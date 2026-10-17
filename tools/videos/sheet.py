"""Tile preview PNGs into one contact sheet: python3 sheet.py <slug> <out.png>"""
import glob, sys
from PIL import Image
files = sorted(glob.glob(f"/tmp/claude-0/prev/{sys.argv[1]}_*.png"), key=lambda f: int(f.rsplit("_", 1)[1][:-4]))
ims = [Image.open(f).resize((640, 360)) for f in files]
cols = 2
rows = (len(ims) + cols - 1) // cols
sheet = Image.new("RGB", (cols * 640, rows * 360), "white")
for i, im in enumerate(ims):
    sheet.paste(im, ((i % cols) * 640, (i // cols) * 360))
sheet.save(sys.argv[2])
