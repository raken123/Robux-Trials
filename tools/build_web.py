"""Assemble the web version into dist/web and dist/IslamiskaForbundet-web.zip."""
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
