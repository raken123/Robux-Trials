"""Draw the printable "Premium 30 dagar gratis" card around the QR modules in matrix.txt
(produced by QrTool.java in CI from payload.txt). Output: tools/qr/premium-30-dagar.png"""
import math
import os

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
NAVY, NAVY_DARK, GOLD, GOLD_LIGHT, WHITE = (26, 35, 126), (15, 20, 80), (196, 179, 87), (230, 218, 160), (255, 255, 255)
F = lambda kind, size: ImageFont.truetype({
    "sans": "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    "bold": "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"}[kind], size)

rows = [r.strip() for r in open(os.path.join(HERE, "matrix.txt")) if r.strip()]
n = len(rows)
W, H = 1080, 1700
img = Image.new("RGB", (W, H))
d = ImageDraw.Draw(img)
for y in range(H):
    t = y / H
    d.line([(0, y), (W, y)], fill=tuple(int(a + (b - a) * t) for a, b in zip(NAVY, NAVY_DARK)))
# faint star lattice
pat = Image.new("RGBA", (W, H), (0, 0, 0, 0))
pd = ImageDraw.Draw(pat)
for gy in range(-1, H // 120 + 2):
    for gx in range(-1, W // 120 + 2):
        cx, cy = gx * 120 + (60 if gy % 2 else 0), gy * 120
        for k in range(2):
            pts = [(cx + 26 * math.cos(math.radians(a + 45 * k)), cy + 26 * math.sin(math.radians(a + 45 * k))) for a in (0, 90, 180, 270)]
            pd.polygon(pts, outline=GOLD + (26,), width=2)
img = Image.alpha_composite(img.convert("RGBA"), pat)
d = ImageDraw.Draw(img)

logo = Image.open(os.path.join(ROOT, "app/src/main/res/drawable-nodpi/logo_glyph.png")).convert("RGBA")
img.alpha_composite(logo.resize((120, 120), Image.LANCZOS), (W // 2 - 60, 60))
d.text((W / 2, 210), "Islamiska Förbundet", font=F("bold", 34), fill=GOLD_LIGHT, anchor="mm")

d.text((W / 2, 300), "Premium", font=F("bold", 96), fill=WHITE, anchor="mm")
d.rounded_rectangle([W / 2 - 300, 370, W / 2 + 300, 450], radius=40, fill=GOLD)
d.text((W / 2, 410), "30 dagar gratis", font=F("bold", 50), fill=NAVY_DARK, anchor="mm")
d.text((W / 2, 495), "Inget kort behövs", font=F("sans", 34), fill=GOLD_LIGHT, anchor="mm")

# QR with 4-module quiet zone on a white tile
mod = 15
quiet = 4
size = (n + 2 * quiet) * mod
x0, y0 = (W - size) // 2, 550
d.rounded_rectangle([x0 - 12, y0 - 12, x0 + size + 12, y0 + size + 12], radius=36, fill=WHITE)
for y, row in enumerate(rows):
    for x, bit in enumerate(row):
        if bit == "1":
            px, py = x0 + (x + quiet) * mod, y0 + (y + quiet) * mod
            d.rectangle([px, py, px + mod - 1, py + mod - 1], fill=NAVY_DARK)
# small logo badge in the centre (error correction level H covers it)
badge = 7 * mod
cx, cy = x0 + size // 2, y0 + size // 2
d.rounded_rectangle([cx - badge / 2 - 6, cy - badge / 2 - 6, cx + badge / 2 + 6, cy + badge / 2 + 6], radius=18, fill=WHITE)
d.rounded_rectangle([cx - badge / 2, cy - badge / 2, cx + badge / 2, cy + badge / 2], radius=14, fill=NAVY)
img.alpha_composite(logo.resize((badge - 24, badge - 24), Image.LANCZOS), (int(cx - badge / 2 + 12), int(cy - badge / 2 + 12)))

y = y0 + size + 70
steps = ["Öppna appen Islamiska Förbundet", "Gå till Premium", "Tryck på ”Skanna kod” och rikta kameran hit"]
for i, s in enumerate(steps):
    yy = y + i * 62
    d.ellipse([170, yy - 22, 214, yy + 22], fill=GOLD)
    d.text((192, yy + 1), str(i + 1), font=F("bold", 26), fill=NAVY_DARK, anchor="mm")
    d.text((236, yy), s, font=F("sans", 32), fill=WHITE, anchor="lm")

y += 3 * 62 + 40
pills = ["Inga reklamer", "Alla videor", "98 % till zakat"]
widths = [d.textlength(p, font=F("bold", 26)) + 44 for p in pills]
x = (W - sum(widths) - 20 * (len(pills) - 1)) / 2
for p, w in zip(pills, widths):
    d.rounded_rectangle([x, y - 26, x + w, y + 26], radius=26, outline=GOLD, width=3)
    d.text((x + w / 2, y), p, font=F("bold", 26), fill=GOLD_LIGHT, anchor="mm")
    x += w + 20
d.text((W / 2, H - 50), "Gäller en gång per telefon · islamiskaforbundet.se", font=F("sans", 24), fill=GOLD_LIGHT, anchor="mm")
out = os.path.join(HERE, "premium-30-dagar.png")
img.convert("RGB").save(out, optimize=True)
print(out, img.size)
