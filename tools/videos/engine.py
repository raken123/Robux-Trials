"""Tiny scene-based 2D animation engine that renders MP4 files with PIL + ffmpeg.

A video is a list of Scene objects. Each scene draws its content for a local time t
(seconds) and progress p (0..1). The engine adds the branded intro/outro, scene
fades, the caption bar and the header, supersamples for anti-aliasing and pipes
frames into ffmpeg.
"""
import json
import math
import os
import subprocess
from dataclasses import dataclass, field
from functools import lru_cache
from multiprocessing import get_context
from typing import Callable, Optional

from PIL import Image, ImageDraw, ImageFilter, ImageFont

W, H = 1280, 720
S = 2  # supersampling factor
FPS = 30
FADE = 0.45

NAVY = (26, 35, 126)
NAVY_DARK = (15, 20, 80)
GOLD = (196, 179, 87)
GOLD_LIGHT = (230, 218, 160)
WHITE = (255, 255, 255)
CREAM = (250, 248, 240)
SKY = (120, 170, 230)

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
LOGO = os.path.join(ROOT, "app/src/main/res/drawable-nodpi/logo_glyph.png")

FONTS = {
    "sans": "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    "bold": "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
    "ar": "/usr/share/fonts/truetype/freefont/FreeSerif.ttf",
    "arbold": "/usr/share/fonts/truetype/freefont/FreeSerifBold.ttf",
    "emoji": "/usr/share/fonts/truetype/noto/NotoColorEmoji.ttf",
}


@lru_cache(maxsize=None)
def font(kind: str, size: float) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(FONTS[kind], int(size * S), layout_engine=ImageFont.Layout.RAQM)


@lru_cache(maxsize=None)
def emoji_img(ch: str, size: int) -> Image.Image:
    f = ImageFont.truetype(FONTS["emoji"], 109, layout_engine=ImageFont.Layout.RAQM)
    im = Image.new("RGBA", (160, 160), (0, 0, 0, 0))
    ImageDraw.Draw(im).text((80, 80), ch, font=f, embedded_color=True, anchor="mm")
    bbox = im.getbbox() or (0, 0, 160, 160)
    im = im.crop(bbox)
    scale = size * S / max(im.size)
    return im.resize((max(1, int(im.width * scale)), max(1, int(im.height * scale))), Image.LANCZOS)


@lru_cache(maxsize=None)
def logo_img(size: int) -> Image.Image:
    return Image.open(LOGO).convert("RGBA").resize((size * S, size * S), Image.LANCZOS)


def lerp(a, b, t):
    return a + (b - a) * t


def clamp(x, lo=0.0, hi=1.0):
    return max(lo, min(hi, x))


def ease(t):
    t = clamp(t)
    return t * t * (3 - 2 * t)


def ease_out(t):
    t = clamp(t)
    return 1 - (1 - t) ** 3


def seg(p, a, b):
    """Progress inside the sub-range [a, b] of p, eased."""
    return ease((p - a) / (b - a)) if b > a else float(p >= a)


def mix(c1, c2, t):
    t = clamp(t)
    return tuple(int(lerp(a, b, t)) for a, b in zip(c1, c2))


def alpha(c, a):
    return (c[0], c[1], c[2], int(255 * clamp(a)))


class Ctx:
    """Drawing context in 1280x720 coordinates on a supersampled RGBA layer.

    PIL replaces pixels when drawing on RGBA images, so any translucent colour is
    drawn on a small temporary layer and alpha-composited to blend correctly.
    """

    def __init__(self, img: Image.Image):
        self.img = img
        self.d = ImageDraw.Draw(img)

    def _blend(self, colors, box, fn):
        translucent = any(c is not None and len(c) == 4 and c[3] < 255 for c in colors)
        if not translucent:
            fn(self.d, 0, 0)
            return
        pad = 8 * S
        x0 = max(0, int(box[0]) - pad)
        y0 = max(0, int(box[1]) - pad)
        x1 = min(self.img.width, int(box[2]) + pad)
        y1 = min(self.img.height, int(box[3]) + pad)
        if x1 <= x0 or y1 <= y0:
            return
        tmp = Image.new("RGBA", (x1 - x0, y1 - y0), (0, 0, 0, 0))
        fn(ImageDraw.Draw(tmp), -x0, -y0)
        self.img.alpha_composite(tmp, (x0, y0))

    @staticmethod
    def _bounds(pts, grow=0.0):
        xs = [x for x, _ in pts]
        ys = [y for _, y in pts]
        return ((min(xs) - grow) * S, (min(ys) - grow) * S, (max(xs) + grow) * S, (max(ys) + grow) * S)

    def line(self, pts, color, width=4, round_caps=True):
        def fn(d, ox, oy):
            p = [(x * S + ox, y * S + oy) for x, y in pts]
            d.line(p, fill=color, width=int(width * S), joint="curve")
            if round_caps:
                r = width / 2 * S
                for x, y in (p[0], p[-1]):
                    d.ellipse([x - r, y - r, x + r, y + r], fill=color)
        self._blend([color], self._bounds(pts, width), fn)

    def circle(self, x, y, r, fill=None, outline=None, width=2):
        self.ellipse(x - r, y - r, x + r, y + r, fill, outline, width)

    def ellipse(self, x0, y0, x1, y1, fill=None, outline=None, width=2):
        def fn(d, ox, oy):
            d.ellipse([x0 * S + ox, y0 * S + oy, x1 * S + ox, y1 * S + oy], fill=fill, outline=outline,
                      width=int(width * S))
        self._blend([fill, outline], (x0 * S, y0 * S, x1 * S, y1 * S), fn)

    def arc(self, x, y, r, start, end, color, width=4):
        def fn(d, ox, oy):
            d.arc([(x - r) * S + ox, (y - r) * S + oy, (x + r) * S + ox, (y + r) * S + oy], start, end, fill=color,
                  width=int(width * S))
        self._blend([color], ((x - r) * S, (y - r) * S, (x + r) * S, (y + r) * S), fn)

    def pie(self, x, y, r, start, end, fill):
        def fn(d, ox, oy):
            d.pieslice([(x - r) * S + ox, (y - r) * S + oy, (x + r) * S + ox, (y + r) * S + oy], start, end, fill=fill)
        self._blend([fill], ((x - r) * S, (y - r) * S, (x + r) * S, (y + r) * S), fn)

    def rect(self, x0, y0, x1, y1, fill=None, outline=None, width=2, radius=0):
        def fn(d, ox, oy):
            box = [x0 * S + ox, y0 * S + oy, x1 * S + ox, y1 * S + oy]
            if radius:
                d.rounded_rectangle(box, radius=radius * S, fill=fill, outline=outline, width=int(width * S))
            else:
                d.rectangle(box, fill=fill, outline=outline, width=int(width * S))
        self._blend([fill, outline], (x0 * S, y0 * S, x1 * S, y1 * S), fn)

    def poly(self, pts, fill=None, outline=None, width=2):
        def fn(d, ox, oy):
            d.polygon([(x * S + ox, y * S + oy) for x, y in pts], fill=fill, outline=outline, width=int(width * S))
        self._blend([fill, outline], self._bounds(pts), fn)

    def text(self, x, y, s, size=32, color=WHITE, kind="sans", anchor="mm", rtl=False):
        kw = {"direction": "rtl"} if rtl else {}
        f = font(kind, size)
        box = self.d.textbbox((x * S, y * S), s, font=f, anchor=anchor, **kw)

        def fn(d, ox, oy):
            d.text((x * S + ox, y * S + oy), s, font=f, fill=color, anchor=anchor, **kw)
        self._blend([color], box, fn)

    def fit(self, s, size, max_w, kind="sans"):
        """Largest font size <= size that fits s into max_w."""
        while size > 12 and self.text_w(s, size, kind) > max_w:
            size -= 2
        return size

    def text_w(self, s, size, kind="sans"):
        return self.d.textlength(s, font=font(kind, size)) / S

    def wrap(self, s, size, max_w, kind="sans"):
        lines, cur = [], ""
        for word in s.split():
            test = (cur + " " + word).strip()
            if self.text_w(test, size, kind) <= max_w or not cur:
                cur = test
            else:
                lines.append(cur)
                cur = word
        if cur:
            lines.append(cur)
        return lines

    def paragraph(self, x, y, s, size=30, color=WHITE, max_w=900, kind="sans", spacing=1.3, anchor="mm"):
        lines = self.wrap(s, size, max_w, kind)
        lh = size * spacing
        y0 = y - lh * (len(lines) - 1) / 2
        for i, ln in enumerate(lines):
            self.text(x, y0 + i * lh, ln, size, color, kind, anchor)

    def emoji(self, x, y, ch, size=80, a=1.0, rot=0):
        im = emoji_img(ch, int(size))
        if rot:
            im = im.rotate(rot, resample=Image.BICUBIC, expand=True)
        if a < 1:
            im = im.copy()
            im.putalpha(im.getchannel("A").point(lambda v: int(v * clamp(a))))
        self.img.alpha_composite(im, (int(x * S - im.width / 2), int(y * S - im.height / 2)))

    def logo(self, x, y, size, a=1.0):
        im = logo_img(int(size))
        if a < 1:
            im = im.copy()
            im.putalpha(im.getchannel("A").point(lambda v: int(v * clamp(a))))
        self.img.alpha_composite(im, (int(x * S - im.width / 2), int(y * S - im.height / 2)))


@dataclass
class Scene:
    dur: float
    caption: str
    draw: Callable[[Ctx, float, float], None]
    narration: Optional[str] = None
    background: Optional[Callable[[Ctx, float, float], None]] = None


@dataclass
class Video:
    slug: str
    title: str
    subtitle: str
    scenes: list
    premium: bool = False
    thumb_at: float = 0.5  # which scene (fraction of scenes) to use for the thumbnail
    timeline: list = field(default_factory=list)


INTRO = 3.0
OUTRO = 3.5


@lru_cache(maxsize=4)
def base_background() -> Image.Image:
    """Navy gradient with a faint eight-point star lattice."""
    w, h = W * S, H * S
    bg = Image.new("RGBA", (w, h))
    px = ImageDraw.Draw(bg)
    for y in range(h):
        px.line([(0, y), (w, y)], fill=mix(NAVY, NAVY_DARK, y / h) + (255,))
    pat = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    pd = ImageDraw.Draw(pat)
    step = 120 * S
    for gy in range(-1, h // step + 2):
        for gx in range(-1, w // step + 2):
            cx, cy = gx * step + (step // 2 if gy % 2 else 0), gy * step
            r = 26 * S
            for k in range(2):
                pts = [(cx + r * math.cos(math.radians(a + 45 * k)), cy + r * math.sin(math.radians(a + 45 * k)))
                       for a in (0, 90, 180, 270)]
                pd.polygon(pts, outline=GOLD + (22,), width=2 * S)
    bg.alpha_composite(pat)
    return bg


def header(c: Ctx, title: str):
    w = c.text_w(title, 22, "bold")
    c.rect(16, 16, 92 + w, 64, fill=(10, 13, 50, 170), radius=24)
    c.logo(44, 40, 40)
    c.text(76, 40, title, 22, GOLD_LIGHT, "bold", anchor="lm")


def caption_bar(c: Ctx, text: str, a: float):
    if not text or a <= 0:
        return
    size = 30
    lines = c.wrap(text, size, 1060)
    lh = size * 1.32
    h = lh * len(lines) + 28
    y1 = H - 26
    y0 = y1 - h
    c.rect(90, y0, W - 90, y1, fill=(10, 13, 50, int(200 * a)), radius=18)
    c.rect(90, y0, 96, y1, fill=alpha(GOLD, a), radius=3)
    for i, ln in enumerate(lines):
        c.text(W / 2 + 3, y0 + 14 + lh * (i + 0.5), ln, size, alpha(WHITE, a), "sans")


def intro(c: Ctx, v: Video, t: float):
    p = t / INTRO
    a = seg(p, 0, 0.3)
    c.logo(W / 2, 270 - 20 * (1 - a), 170, a)
    c.text(W / 2, 410, "Islamiska Förbundet", 26, alpha(GOLD_LIGHT, seg(p, 0.2, 0.45)), "bold")
    b = seg(p, 0.3, 0.6)
    c.text(W / 2, 480 + 12 * (1 - b), v.title, 54, alpha(WHITE, b), "bold")
    c.text(W / 2, 545, v.subtitle, 28, alpha(GOLD, seg(p, 0.45, 0.75)))
    out = 1 - seg(p, 0.88, 1.0)
    if out < 1:
        c.rect(0, 0, W, H, fill=(15, 20, 80, int(255 * (1 - out))))


def outro(c: Ctx, v: Video, t: float):
    p = t / OUTRO
    a = seg(p, 0, 0.25)
    c.logo(W / 2, 290, 150, a)
    c.text(W / 2, 420, "Islamiska Förbundet", 40, alpha(WHITE, a), "bold")
    c.text(W / 2, 475, "Må Allah välsigna dig", 28, alpha(GOLD, seg(p, 0.2, 0.45)))
    c.text(W / 2, 520, "islamiskaforbundet.se", 22, alpha(GOLD_LIGHT, seg(p, 0.3, 0.55)))
    fade = seg(p, 0.85, 1.0)
    if fade > 0:
        c.rect(0, 0, W, H, fill=(15, 20, 80, int(255 * fade)))


def build_timeline(v: Video):
    tl = [("intro", 0.0, INTRO, None)]
    t = INTRO
    for s in v.scenes:
        tl.append(("scene", t, s.dur, s))
        t += s.dur
    tl.append(("outro", t, OUTRO, None))
    return tl, t + OUTRO


def render_frame(args):
    v, i = args
    tl, total = build_timeline(v)
    ts = i / FPS
    img = base_background().copy()
    c = Ctx(img)
    for kind, start, dur, sc in tl:
        if start <= ts < start + dur or (kind == "outro" and ts >= start):
            lt = ts - start
            if kind == "intro":
                intro(c, v, lt)
            elif kind == "outro":
                outro(c, v, min(lt, dur))
            else:
                p = clamp(lt / dur)
                fade_in = seg(lt, 0, FADE)
                fade_out = 1 - seg(lt, dur - FADE, dur)
                a = min(fade_in, fade_out)
                if sc.background:
                    sc.background(c, lt, p)
                layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
                lc = Ctx(layer)
                sc.draw(lc, lt, p)
                if a < 1:
                    layer.putalpha(layer.getchannel("A").point(lambda x, a=a: int(x * a)))
                img.alpha_composite(layer)
                header(c, v.title)
                caption_bar(c, sc.caption, a)
            break
    return img.convert("RGB").resize((W, H), Image.LANCZOS).tobytes()


_CURRENT: Optional[Video] = None


def _render_index(i):
    # Worker processes are forked after _CURRENT is set, so scenes (with lambdas) need no pickling.
    return render_frame((_CURRENT, i))


def render(v: Video, out_mp4: str, thumb: Optional[str] = None, workers: int = 4, preview_times=None):
    tl, total = build_timeline(v)
    n = int(round(total * FPS))
    if preview_times is not None:
        os.makedirs(os.path.dirname(out_mp4), exist_ok=True)
        for k, ts in enumerate(preview_times):
            raw = render_frame((v, int(ts * FPS)))
            Image.frombytes("RGB", (W, H), raw).save(out_mp4.replace(".mp4", f"_{k}.png"))
        return
    ff = subprocess.Popen([
        "ffmpeg", "-y", "-loglevel", "error", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{W}x{H}",
        "-r", str(FPS), "-i", "-", "-c:v", "libx264", "-preset", "slow", "-crf", "26", "-tune", "animation",
        "-pix_fmt", "yuv420p", "-movflags", "+faststart", out_mp4,
    ], stdin=subprocess.PIPE)
    global _CURRENT
    _CURRENT = v
    with get_context("fork").Pool(workers) as pool:
        for raw in pool.imap(_render_index, range(n), chunksize=6):
            ff.stdin.write(raw)
    ff.stdin.close()
    ff.wait()
    if thumb:
        idx = min(len(v.scenes) - 1, int(len(v.scenes) * v.thumb_at))
        start = [s for k, s, d, sc in tl if k == "scene"][idx]
        ts = start + v.scenes[idx].dur * 0.75
        Image.frombytes("RGB", (W, H), render_frame((v, int(ts * FPS)))).resize((640, 360), Image.LANCZOS) \
            .save(thumb, quality=82)
    return total


def cues(v: Video):
    tl, total = build_timeline(v)
    out = [{"t": 0.3, "text": v.title}]
    for kind, start, dur, sc in tl:
        if kind == "scene":
            out.append({"t": round(start + 0.3, 2), "text": sc.narration or sc.caption})
    return out, total
