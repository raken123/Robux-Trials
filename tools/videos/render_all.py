"""Render every video into the Android app: MP4 in res/raw, thumbnail in drawable-nodpi, metadata in assets."""
import json
import os
import sys

from engine import ROOT, cues, render
from videos import all_videos

RAW = os.path.join(ROOT, "app/src/main/res/raw")
THUMBS = os.path.join(ROOT, "app/src/main/res/drawable-nodpi")
ASSETS = os.path.join(ROOT, "app/src/main/assets")

only = set(sys.argv[1:])
os.makedirs(RAW, exist_ok=True)
os.makedirs(ASSETS, exist_ok=True)
catalogue = []
for v in all_videos():
    cue_list, total = cues(v)
    catalogue.append({
        "slug": v.slug, "title": v.title, "subtitle": v.subtitle, "premium": v.premium,
        "duration": round(total, 1), "cues": cue_list,
    })
    if only and v.slug not in only:
        continue
    out = os.path.join(RAW, f"video_{v.slug}.mp4")
    render(v, out, thumb=os.path.join(THUMBS, f"thumb_{v.slug}.jpg"))
    print(f"{v.slug}: {total:.1f}s, {os.path.getsize(out) / 1e6:.1f} MB", flush=True)

with open(os.path.join(ASSETS, "videos.json"), "w", encoding="utf-8") as f:
    json.dump(catalogue, f, ensure_ascii=False, indent=1)
print("done")
