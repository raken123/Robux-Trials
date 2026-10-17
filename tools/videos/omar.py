"""Short film: "Omar – vägen tillbaka". A teenager drifts into haram habits and finds his way back through tawba."""
import math
import random

from engine import GOLD, GOLD_LIGHT, H, NAVY, NAVY_DARK, WHITE, W, alpha, clamp, ease, lerp, mix, seg

SKIN = (214, 168, 128)
SKIN_MOTHER = (226, 182, 140)
SKIN_IMAM = (196, 150, 110)
HAIR = (35, 28, 24)
INK = (25, 22, 30)


def sc(caption, draw, min_dur=5.0):
    from videos import sc as base
    return base(caption, draw, min_dur)


# ---------------------------------------------------------------------------
# Characters (front view, feet at y)
# ---------------------------------------------------------------------------

def face(c, x, y, r, mood, look=0.0, blink=False):
    ex = r * 0.38
    ey = y - r * 0.08
    lx = look * r * 0.12
    for s in (-1, 1):
        if blink:
            c.line([(x + s * ex - 6, ey), (x + s * ex + 6, ey)], INK, 3)
        else:
            c.circle(x + s * ex + lx, ey, r * 0.11, fill=INK)
        # eyebrows
        tilt = {"angry": -6, "sad": 6, "shocked": 0, "worried": 5}.get(mood, 0) * s
        by = ey - r * 0.32 - (4 if mood == "shocked" else 0)
        c.line([(x + s * ex - 9, by - tilt), (x + s * ex + 9, by + tilt)], INK, 3)
    my = y + r * 0.42
    if mood in ("happy", "proud"):
        c.arc(x, my - r * 0.18, r * 0.36, 20, 160, INK, 4)
    elif mood in ("sad", "worried"):
        c.arc(x, my + r * 0.22, r * 0.3, 205, 335, INK, 4)
    elif mood == "shocked":
        c.ellipse(x - r * 0.13, my - r * 0.12, x + r * 0.13, my + r * 0.16, fill=INK)
    elif mood == "angry":
        c.line([(x - r * 0.25, my + 2), (x + r * 0.25, my - 2)], INK, 4)
    elif mood == "sleep":
        c.line([(x - r * 0.15, my), (x + r * 0.15, my)], INK, 3)
    else:
        c.line([(x - r * 0.2, my), (x + r * 0.2, my)], INK, 4)


def person(c, x, y, who="omar", mood="neutral", arms="down", walk=0.0, scale=1.0, shirt=None, look=0.0, blink=False):
    s = scale
    skin = {"mother": SKIN_MOTHER, "imam": SKIN_IMAM}.get(who, SKIN)
    shirt = shirt or {"omar": (200, 60, 60), "mother": (70, 110, 90), "imam": (240, 238, 228),
                      "friend": (70, 70, 80), "friend2": (90, 60, 110)}.get(who, (120, 120, 120))
    pants = {"imam": (240, 238, 228), "mother": (70, 110, 90)}.get(who, (45, 55, 90))
    leg = 92 * s
    body = 112 * s
    r = 36 * s
    hip = y - leg
    sh = hip - body
    head_y = sh - r - 4 * s
    sw = math.sin(walk * math.pi * 2) * 18 * s if walk else 0
    # legs / long dress
    if who in ("mother", "imam"):
        c.poly([(x - 34 * s, sh + 20 * s), (x + 34 * s, sh + 20 * s), (x + 50 * s, y), (x - 50 * s, y)], fill=pants)
        c.ellipse(x - 34 * s, y - 8 * s, x - 6 * s, y + 4 * s, fill=INK)
        c.ellipse(x + 6 * s, y - 8 * s, x + 34 * s, y + 4 * s, fill=INK)
    else:
        for side, d in ((-1, sw), (1, -sw)):
            c.line([(x + side * 15 * s, hip), (x + side * 15 * s + d, y - 6 * s)], pants, 24 * s)
            c.ellipse(x + side * 15 * s + d - 16 * s, y - 12 * s, x + side * 15 * s + d + 18 * s, y + 2 * s, fill=INK)
    # torso
    c.rect(x - 40 * s, sh, x + 40 * s, hip + 12 * s, fill=shirt, radius=22 * s)
    if who == "omar":  # hoodie strings
        c.line([(x - 8 * s, sh + 8 * s), (x - 8 * s, sh + 38 * s)], (240, 240, 240), 3)
        c.line([(x + 8 * s, sh + 8 * s), (x + 8 * s, sh + 38 * s)], (240, 240, 240), 3)
    # arms
    lsh, rsh = (x - 40 * s, sh + 16 * s), (x + 40 * s, sh + 16 * s)
    hands = {
        "down": ((x - 52 * s, hip + 10 * s), (x + 52 * s, hip + 10 * s)),
        "phone": ((x - 52 * s, hip + 10 * s), (x + 16 * s, sh + 30 * s)),
        "wave": ((x - 52 * s, hip + 10 * s), (x + 78 * s, sh - 70 * s)),
        "open": ((x - 90 * s, sh + 20 * s), (x + 90 * s, sh + 20 * s)),
        "qiyam": ((x + 14 * s, sh + 50 * s), (x - 14 * s, sh + 46 * s)),
        "takbir": ((x - 44 * s, head_y), (x + 44 * s, head_y)),
        "dua": ((x - 30 * s, sh - 10 * s), (x + 30 * s, sh - 10 * s)),
        "hug": ((x + 70 * s, sh + 30 * s), (x + 110 * s, sh + 30 * s)),
        "face": ((x - 18 * s, head_y + 6 * s), (x + 18 * s, head_y + 6 * s)),
        "carry": ((x - 62 * s, hip - 10 * s), (x + 62 * s, hip - 10 * s)),
    }[arms]
    for shoulder, hand in ((lsh, hands[0]), (rsh, hands[1])):
        elbow = ((shoulder[0] + hand[0]) / 2 + (shoulder[0] - x) * 0.25, (shoulder[1] + hand[1]) / 2 + 12 * s)
        c.line([shoulder, elbow, hand], shirt, 18 * s)
        c.circle(hand[0], hand[1], 10 * s, fill=skin)
    if arms == "phone":
        hx, hy = hands[1]
        c.rect(hx - 12 * s, hy - 34 * s, hx + 12 * s, hy + 4 * s, fill=(20, 20, 25), radius=4)
        c.rect(hx - 9 * s, hy - 31 * s, hx + 9 * s, hy + 1 * s, fill=(120, 190, 255))
    if arms == "carry":
        for hx in (x - 62 * s, x + 62 * s):
            c.rect(hx - 18 * s, hip - 8 * s, hx + 18 * s, hip + 40 * s, fill=(230, 220, 190), radius=6)
    # neck + head + hair / headwear
    c.rect(x - 10 * s, sh - 12 * s, x + 10 * s, sh + 4 * s, fill=skin)
    if who == "mother":
        c.circle(x, head_y + 4 * s, r * 1.28, fill=(60, 90, 140))
        c.poly([(x - r * 1.25, head_y + 10 * s), (x + r * 1.25, head_y + 10 * s), (x + 46 * s, sh + 34 * s), (x - 46 * s, sh + 34 * s)],
               fill=(60, 90, 140))
        c.circle(x, head_y + 4 * s, r * 0.92, fill=skin)
        face(c, x, head_y + 6 * s, r * 0.92, mood, look, blink)
        return
    c.circle(x, head_y, r, fill=skin)
    c.circle(x - r, head_y + 4 * s, 7 * s, fill=skin)
    c.circle(x + r, head_y + 4 * s, 7 * s, fill=skin)
    if who == "imam":
        c.ellipse(x - r * 0.95, head_y + r * 0.15, x + r * 0.95, head_y + r * 1.65, fill=(225, 225, 225))
        c.ellipse(x - r * 0.9, head_y - r * 1.1, x + r * 0.9, head_y - r * 0.35, fill=WHITE)
        face(c, x, head_y - 2 * s, r * 0.95, mood, look, blink)
        return
    if who in ("friend", "friend2"):
        c.arc(x, head_y, r + 2, 180, 360, (30, 30, 35), int(16 * s))
        c.rect(x - r, head_y - r * 0.45, x + r + 22 * s, head_y - r * 0.25, fill=(30, 30, 35), radius=4)
    else:
        c.ellipse(x - r * 1.02, head_y - r * 1.08, x + r * 1.02, head_y - r * 0.05, fill=HAIR)
        c.rect(x - r * 1.02, head_y - r * 0.6, x - r * 0.75, head_y + r * 0.1, fill=HAIR)
        c.rect(x + r * 0.75, head_y - r * 0.6, x + r * 1.02, head_y + r * 0.1, fill=HAIR)
    face(c, x, head_y, r, mood, look, blink)


def bubble(c, x, y, text, tail, size=28, thought=False, color=WHITE):
    w = max(c.text_w(ln, size) for ln in text.split("\n")) + 40
    lines = text.split("\n")
    h = size * 1.3 * len(lines) + 26
    if thought:
        c.ellipse(x - w / 2 - 10, y - h / 2 - 8, x + w / 2 + 10, y + h / 2 + 8, fill=color)
        tx, ty = tail
        for k, rr in ((0.35, 12), (0.65, 8), (0.9, 5)):
            c.circle(lerp(x, tx, k), lerp(y + h / 2, ty, k), rr, fill=color)
    else:
        c.poly([(x - 16, y + h / 2 - 4), (x + 16, y + h / 2 - 4), tail], fill=color)
        c.rect(x - w / 2, y - h / 2, x + w / 2, y + h / 2, fill=color, radius=22)
    for i, ln in enumerate(lines):
        c.text(x, y - h / 2 + 13 + size * 1.3 * (i + 0.5), ln, size, INK, "bold")


# ---------------------------------------------------------------------------
# Sets
# ---------------------------------------------------------------------------

def bedroom(c, t, night=True):
    wall = (40, 46, 90) if night else (215, 205, 185)
    c.rect(0, 0, W, H, fill=wall)
    c.rect(0, 560, W, H, fill=(70, 55, 50) if night else (160, 120, 90))
    # window
    c.rect(880, 130, 1120, 340, fill=(10, 14, 40) if night else (150, 200, 245), outline=(200, 190, 160), width=8)
    c.line([(1000, 130), (1000, 340)], (200, 190, 160), 6, round_caps=False)
    if night:
        c.circle(1060, 180, 24, fill=GOLD_LIGHT)
        c.circle(1048, 172, 22, fill=(10, 14, 40))
    # bed
    c.rect(120, 470, 560, 560, fill=(110, 80, 70), radius=10)
    c.rect(130, 440, 550, 490, fill=(200, 200, 215), radius=16)
    c.rect(140, 420, 240, 460, fill=WHITE, radius=14)


def clock(c, x, y, txt, ring=False, t=0.0):
    dx = math.sin(t * 40) * 4 if ring else 0
    c.rect(x - 70 + dx, y - 30, x + 70 + dx, y + 30, fill=(20, 20, 25), radius=10)
    c.text(x + dx, y, txt, 36, (255, 90, 80) if not ring else GOLD, "bold")


def kitchen(c):
    c.rect(0, 0, W, H, fill=(225, 210, 185))
    c.rect(0, 560, W, H, fill=(150, 110, 80))
    c.rect(60, 330, 460, 560, fill=(180, 140, 100))
    c.rect(60, 320, 460, 340, fill=(120, 90, 70))
    for k in range(3):
        c.rect(80 + k * 130, 360, 180 + k * 130, 540, outline=(130, 95, 70), width=4, radius=6)
    c.rect(860, 120, 1120, 300, fill=(150, 200, 245), outline=WHITE, width=8)


def classroom(c):
    c.rect(0, 0, W, H, fill=(205, 215, 200))
    c.rect(0, 580, W, H, fill=(140, 110, 80))
    c.rect(330, 80, 950, 260, fill=(40, 70, 55), outline=(120, 90, 60), width=10)
    c.text(640, 140, "PROV – Matematik", 34, WHITE, "bold")
    c.text(640, 200, "x² + 3x – 10 = 0", 30, (220, 230, 220))


def street(c, t, night=True, police=False):
    c.rect(0, 0, W, H, fill=(12, 16, 40) if night else (120, 170, 230))
    rnd = random.Random(4)
    for i in range(8):
        bx = i * 170 - 20
        bh = rnd.randint(200, 360)
        c.rect(bx, 560 - bh, bx + 150, 560, fill=(28, 32, 60))
        for wy in range(560 - bh + 20, 540, 46):
            for wx in range(bx + 18, bx + 140, 38):
                if rnd.random() < 0.45:
                    c.rect(wx, wy, wx + 20, wy + 26, fill=(250, 210, 120))
    c.rect(0, 560, W, H, fill=(40, 40, 48))
    c.rect(0, 556, W, 566, fill=(90, 90, 100))
    # lamp
    c.line([(1120, 560), (1120, 250), (1060, 250)], (90, 90, 100), 8)
    c.circle(1060, 262, 16, fill=(255, 230, 150))
    if police:
        on = int(t * 6) % 2
        c.rect(0, 0, W, H, fill=(40, 80, 255, 70) if on else (255, 40, 40, 50))


def park_dawn(c, t, k=0.0):
    sky = mix((20, 24, 60), (240, 170, 120), k)
    c.rect(0, 0, W, H, fill=sky)
    c.rect(0, 540, W, H, fill=mix((20, 40, 30), (60, 110, 70), k))
    # mosque silhouette with minaret
    gc = (25, 30, 70)
    c.rect(820, 400, 1060, 540, fill=gc)
    c.ellipse(860, 320, 1020, 460, fill=gc)
    c.rect(1090, 250, 1120, 540, fill=gc)
    c.poly([(1086, 250), (1124, 250), (1105, 205)], fill=gc)
    # bench
    c.rect(180, 470, 520, 490, fill=(110, 80, 60), radius=4)
    c.rect(190, 490, 210, 545, fill=(80, 60, 45))
    c.rect(490, 490, 510, 545, fill=(80, 60, 45))
    c.rect(180, 420, 520, 440, fill=(110, 80, 60), radius=4)


def adhan_waves(c, t, x=1105, y=230):
    for k in range(4):
        ph = (t * 0.6 + k * 0.25) % 1
        c.arc(x, y, 30 + ph * 220, 180, 270, alpha(GOLD_LIGHT, 1 - ph), 4)


def mosque_inside(c, t):
    c.rect(0, 0, W, H, fill=(240, 232, 210))
    for k in range(5):
        x = 140 + k * 250
        c.rect(x - 60, 120, x + 60, 470, fill=(225, 214, 186))
        c.ellipse(x - 60, 60, x + 60, 180, fill=(225, 214, 186))
    c.rect(0, 540, W, H, fill=(150, 30, 45))
    for k in range(10):
        c.line([(k * 140, 545), (k * 140, H)], (130, 25, 38), 4, round_caps=False)
    # mihrab
    c.rect(560, 160, 720, 540, fill=(200, 175, 110))
    c.ellipse(560, 90, 720, 240, fill=(200, 175, 110))


def light_rays(c, t, x, y, a=1.0):
    for k in range(12):
        ang = math.radians(k * 30 + t * 8)
        c.poly([(x, y), (x + 900 * math.cos(ang - 0.06), y + 900 * math.sin(ang - 0.06)),
                (x + 900 * math.cos(ang + 0.06), y + 900 * math.sin(ang + 0.06))], fill=alpha(GOLD_LIGHT, 0.18 * a))


# ---------------------------------------------------------------------------
# Scenes
# ---------------------------------------------------------------------------

def s_intro(c, t, p):
    bedroom(c, t)
    c.circle(800, 300, 160, fill=(100, 160, 255, 40))
    person(c, 760, 575, "omar", "happy", "phone", look=0.6)
    clock(c, 330, 250, "03:12")
    c.text(1000, 420, "Omar, 17 år", 34, GOLD, "bold")


def s_fajr(c, t, p):
    bedroom(c, t)
    c.rect(150, 400, 540, 470, fill=(200, 60, 60), radius=30)  # Omar under the blanket
    c.circle(200, 418, 34, fill=SKIN)
    c.ellipse(166, 384, 234, 418, fill=HAIR)
    ring = p < 0.45
    clock(c, 760, 300, "05:14", ring, t)
    if ring:
        bubble(c, 760, 190, "Fajr – dags att be!", (760, 260), 26)
    else:
        for k in range(3):
            ph = (t * 0.8 + k / 3) % 1
            c.text(260 + ph * 80, 380 - ph * 120, "Z", 26 + k * 6, alpha(WHITE, 1 - ph), "bold")
        c.text(760, 360, "SNOOZE", 26, (200, 200, 210), "bold")


def s_lie(c, t, p):
    kitchen(c)
    person(c, 380, 575, "mother", "worried", "down", blink=(t % 3) < 0.12)
    person(c, 860, 575, "omar", "neutral", "down", look=-0.6)
    if p < 0.45:
        bubble(c, 420, 150, "Omar, har du bett Fajr?", (400, 250))
    else:
        bubble(c, 850, 140, "Ja, mamma…", (850, 230))
        bubble(c, 1100, 300, "(Det har jag inte)", (920, 330), 22, thought=True)


def s_cheat(c, t, p):
    classroom(c)
    for x in (420, 860):
        c.rect(x - 150, 405, x + 150, 435, fill=(170, 130, 90))
        c.rect(x - 140, 435, x - 120, 580, fill=(120, 90, 60))
        c.rect(x + 120, 435, x + 140, 580, fill=(120, 90, 60))
    person(c, 420, 575, "friend2", "neutral", "down", scale=0.9, shirt=(70, 120, 160))
    person(c, 860, 575, "omar", "worried", "down", scale=0.9, look=-1)
    for x in (420, 860):
        c.rect(x - 60, 387, x + 60, 405, fill=WHITE)
    c.line([(790, 265), (500, 390)], alpha((255, 80, 80), 0.6), 4)
    c.text(650, 300, "fusk", 28, (220, 50, 50), "bold")


def s_friends(c, t, p):
    street(c, t)
    person(c, 380, 575, "friend", "angry", "down", walk=t * 0.6)
    person(c, 620, 575, "omar", "neutral", "down", walk=t * 0.6)
    person(c, 860, 575, "friend2", "happy", "wave", walk=t * 0.6)
    bubble(c, 860, 150, "#@!%&?!", (860, 260), 30)
    c.text(620, 120, "Fel vänner, fel ställen", 30, GOLD_LIGHT, "bold")


def s_mother_waits(c, t, p):
    c.rect(0, 0, W, H, fill=(30, 34, 70))
    c.rect(0, 560, W, H, fill=(50, 40, 40))
    c.rect(700, 140, 1000, 380, fill=(10, 14, 40), outline=(150, 140, 120), width=8)
    c.circle(940, 190, 20, fill=GOLD_LIGHT)
    clock(c, 260, 200, "01:40")
    person(c, 560, 575, "mother", "sad", "face")
    c.text(560, 130, "Mamma väntar…", 30, GOLD_LIGHT, "bold")


def s_police(c, t, p):
    street(c, t, police=True)
    run = seg(p, 0.15, 1.0)
    person(c, lerp(560, 1150, run), 650, "omar", "shocked", "down", walk=t * 2.2)
    person(c, 300, 575, "friend", "shocked", "down")
    person(c, 160, 575, "friend2", "shocked", "down")
    c.rect(20, 470, 120, 520, fill=(230, 230, 240), radius=10)
    c.rect(40, 455, 100, 472, fill=(40, 80, 255) if int(t * 6) % 2 else (255, 40, 40), radius=4)


def s_bench(c, t, p):
    k = seg(p, 0.2, 1.0) * 0.6
    park_dawn(c, t, k)
    person(c, 350, 560, "omar", "sad", "face" if p < 0.5 else "down", look=0.8 if p > 0.5 else 0)
    if p > 0.35:
        adhan_waves(c, t)
        c.text(820, 150, "حي على الصلاة", 46, alpha(GOLD_LIGHT, seg(p, 0.35, 0.55)), "ar")
        c.text(820, 205, "Kom till bönen", 26, alpha(WHITE, seg(p, 0.4, 0.6)), "bold")


def s_imam(c, t, p):
    mosque_inside(c, t)
    person(c, 400, 575, "omar", "sad", "down", look=0.5)
    person(c, 880, 575, "imam", "happy", "open" if p > 0.5 else "down")
    if p < 0.5:
        bubble(c, 420, 150, "Jag har gjort så mycket fel…", (420, 250), 26)
    else:
        bubble(c, 880, 140, "Välkommen, min son.", (880, 240), 26)


def s_verse(c, t, p):
    c.rect(0, 0, W, H, fill=NAVY_DARK)
    light_rays(c, t, W / 2, 330, seg(p, 0, 0.4))
    c.text(W / 2, 230, "لَا تَقْنَطُوا مِن رَّحْمَةِ ٱللَّهِ", 64, GOLD, "arbold")
    c.paragraph(W / 2, 360, "Misströsta inte om Allahs nåd. Allah förlåter alla synder.", 34, WHITE, 1000, "bold")
    c.text(W / 2, 450, "Az-Zumar 39:53", 26, GOLD_LIGHT)


def s_tawba(c, t, p):
    mosque_inside(c, t)
    light_rays(c, t, 640, 120, seg(p, 0.3, 0.9))
    c.rect(440, 555, 840, 590, fill=(30, 90, 70), radius=6)
    c.rect(440, 555, 840, 562, fill=GOLD)
    arms = "takbir" if p < 0.25 else ("qiyam" if p < 0.7 else "dua")
    person(c, 640, 575, "omar", "neutral" if p < 0.7 else "happy", arms, blink=0.3 < p < 0.7)
    if p < 0.25:
        c.text(640, 120, "الله أكبر", 48, NAVY, "arbold")


def s_sorry(c, t, p):
    kitchen(c)
    k = seg(p, 0.0, 0.45)
    person(c, 460, 575, "mother", "sad" if p < 0.5 else "happy", "down" if p < 0.5 else "open")
    person(c, lerp(980, 575, k), 640, "omar", "sad" if p < 0.5 else "happy", "down", walk=t if k < 1 else 0)
    if p < 0.5:
        bubble(c, 900, 150, "Förlåt mig, mamma.", (900, 240), 26)
    else:
        for i in range(5):
            ph = (t * 0.5 + i / 5) % 1
            c.text(550 + i * 25, 260 - ph * 120, "♥", 30, alpha((230, 80, 100), 1 - ph), "bold")


def s_new_life(c, t, p):
    c.rect(0, 0, W, H, fill=(245, 240, 225))
    panels = [("Fajr i tid", "05:14 ✓"), ("Pluggar ärligt", "A"), ("Hjälper mamma", ""), ("Goda vänner", "")]
    for i, (label, extra) in enumerate(panels):
        a = seg(p, 0.05 + i * 0.18, 0.2 + i * 0.18)
        if a <= 0:
            continue
        x0 = 40 + i * 305
        c.rect(x0, 90, x0 + 285, 560, fill=alpha((225, 232, 245), a), radius=24)
        if i == 0:
            c.rect(x0 + 60, 405, x0 + 225, 435, fill=alpha((30, 90, 70), a), radius=6)
            person(c, x0 + 142, 425, "omar", "happy", "qiyam", scale=0.7)
        elif i == 1:
            c.rect(x0 + 40, 335, x0 + 245, 365, fill=alpha((170, 130, 90), a))
            person(c, x0 + 142, 425, "omar", "happy", "down", scale=0.7)
            c.rect(x0 + 100, 315, x0 + 180, 335, fill=alpha(WHITE, a))
            c.text(x0 + 230, 170, "A", 60, alpha((40, 140, 70), a), "bold")
        elif i == 2:
            person(c, x0 + 90, 425, "mother", "happy", "down", scale=0.6)
            person(c, x0 + 195, 425, "omar", "happy", "carry", scale=0.6)
        else:
            person(c, x0 + 80, 425, "friend", "happy", "down", scale=0.55, shirt=(60, 120, 90))
            person(c, x0 + 142, 425, "omar", "happy", "wave", scale=0.6)
            person(c, x0 + 205, 425, "friend2", "happy", "down", scale=0.55, shirt=(60, 90, 150))
        c.text(x0 + 142, 530, label, 26, alpha(NAVY, a), "bold")
        c.circle(x0 + 250, 135, 22, fill=alpha((40, 140, 70), a))
        c.text(x0 + 250, 136, "✓", 26, alpha(WHITE, a), "bold")


def s_end(c, t, p):
    park_dawn(c, t, 1.0)
    light_rays(c, t, 640, 560, 0.8)
    c.circle(640, 545, 60, fill=(255, 214, 90))
    c.rect(0, 540, W, H, fill=(60, 110, 70))
    person(c, 400, 585, "omar", "proud", "down")
    person(c, 900, 585, "mother", "happy", "down")
    c.text(640, 140, "Det är aldrig för sent", 50, NAVY_DARK, "bold")
    c.text(640, 205, "att vända tillbaka till Allah", 36, NAVY, "bold")


def omar_scenes():
    return [
        sc("Det här är Omar. Han är sjutton år och bor i Stockholm. Nätterna går åt till mobilen och spel.", s_intro, 7),
        sc("När väckarklockan ringer till Fajr stänger han av den och somnar om. Igen.", s_fajr, 7),
        sc("Mamma frågar om han har bett. Omar ljuger och säger ja.", s_lie, 8),
        sc("I skolan fuskar han på provet i stället för att plugga.", s_cheat, 6),
        sc("På kvällarna hänger han med fel vänner. De svär, bråkar och gör sådant som Omar vet är haram.", s_friends, 8),
        sc("Hemma sitter mamma uppe sent och väntar, orolig och ledsen.", s_mother_waits, 6),
        sc("En natt går allt för långt. Blåljusen lyser upp gatan, och Omar springer därifrån med hjärtat i halsen.", s_police, 8),
        sc("Ensam på en parkbänk i gryningen hör han azan från moskén: Hayya ala as-salah, kom till bönen.", s_bench, 9),
        sc("Han går in i moskén. Jag har gjort så mycket fel, säger Omar. Välkommen, min son, svarar imamen.", s_imam, 9),
        sc("Imamen läser ur Koranen: Misströsta inte om Allahs nåd. Allah förlåter alla synder.", s_verse, 8),
        sc("Omar gör tawba. Han tvättar sig, ställer sig på bönemattan och ber för första gången på länge.", s_tawba, 9),
        sc("Han går hem och ber sin mamma om förlåtelse. Hon kramar honom med tårar i ögonen.", s_sorry, 8),
        sc("Nu går Omar upp till Fajr, pluggar ärligt, hjälper sin mamma och umgås med vänner som drar honom mot det goda.", s_new_life, 10),
        sc("Det är aldrig för sent att vända tillbaka till Allah. Dörren till tawba står öppen så länge du lever.", s_end, 8),
    ]
