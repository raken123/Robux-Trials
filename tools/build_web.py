"""Assemble the web version: dist/web + dist/IslamiskaForbundet-web.zip, and a single-file
dist/IslamiskaForbundet-standalone.html with videos, images and (if present in web/vendor) the QR libraries inlined."""
import base64
import io
import json
import os
import re
import shutil

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT = os.path.join(ROOT, "dist", "web")
RES = os.path.join(ROOT, "app", "src", "main", "res")

config = open(os.path.join(ROOT, "app/src/main/java/se/islamiskaforbundet/app/Config.kt"), encoding="utf-8").read()
key = re.search(r'PREMIUM_CODE_KEY = "([^"]*)"', config).group(1)
videos = json.load(open(os.path.join(ROOT, "app/src/main/assets/videos.json"), encoding="utf-8"))

shutil.rmtree(OUT, ignore_errors=True)
os.makedirs(os.path.join(OUT, "videos"))
os.makedirs(os.path.join(OUT, "thumbs"))
html = open(os.path.join(ROOT, "web", "index.html"), encoding="utf-8").read()
html = html.replace("__PREMIUM_CODE_KEY__", key).replace("/*VIDEOS_JSON*/[]", json.dumps(videos, ensure_ascii=False))
open(os.path.join(OUT, "index.html"), "w", encoding="utf-8").write(html)
shutil.copy(os.path.join(RES, "drawable-nodpi", "logo_glyph.png"), os.path.join(OUT, "logo.png"))
shutil.copy(os.path.join(ROOT, "store", "icon-512.png"), os.path.join(OUT, "icon-512.png"))
for v in videos:
    shutil.copy(os.path.join(RES, "raw", f"video_{v['slug']}.mp4"), os.path.join(OUT, "videos"))
    shutil.copy(os.path.join(RES, "drawable-nodpi", f"thumb_{v['slug']}.jpg"), os.path.join(OUT, "thumbs"))
archive = shutil.make_archive(os.path.join(ROOT, "dist", "IslamiskaForbundet-web"), "zip", OUT)
print(archive, os.path.getsize(archive) // 1024, "KB")

# ---- standalone single file
from PIL import Image  # noqa: E402


def b64(path):
    return base64.b64encode(open(path, "rb").read()).decode()


def png_data_uri(path, size):
    im = Image.open(path).convert("RGBA")
    im.thumbnail((size, size), Image.LANCZOS)
    buf = io.BytesIO()
    im.save(buf, "PNG", optimize=True)
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()


media = {
    "videos": {v["slug"]: b64(os.path.join(RES, "raw", f"video_{v['slug']}.mp4")) for v in videos},
    "thumbs": {v["slug"]: b64(os.path.join(RES, "drawable-nodpi", f"thumb_{v['slug']}.jpg")) for v in videos},
}
single = html.replace("/*MEDIA_JSON*/null", json.dumps(media))
single = single.replace("logo.png", png_data_uri(os.path.join(RES, "drawable-nodpi", "logo_glyph.png"), 192))
single = single.replace("icon-512.png", png_data_uri(os.path.join(ROOT, "store", "icon-512.png"), 192))
vendor = ""
for lib in ("jsQR.js", "qrcode.js"):
    path = os.path.join(ROOT, "web", "vendor", lib)
    if os.path.exists(path):
        vendor += "<script>\n" + open(path, encoding="utf-8").read().replace("</script", "<\\/script") + "\n</script>\n"
    else:
        print(f"warning: web/vendor/{lib} missing - standalone file loads it from the internet when needed")
single = single.replace("<!--VENDOR-->", vendor)
out = os.path.join(ROOT, "dist", "IslamiskaForbundet-standalone.html")
open(out, "w", encoding="utf-8").write(single)
print(out, os.path.getsize(out) // 1024, "KB")
