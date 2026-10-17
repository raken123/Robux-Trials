"""Render a few still frames of one video for checking: python3 preview.py <slug> [times...]"""
import sys
from engine import INTRO, render, build_timeline
from videos import all_videos

slug = sys.argv[1]
v = next(x for x in all_videos() if x.slug == slug)
tl, total = build_timeline(v)
if len(sys.argv) > 2:
    times = [float(x) for x in sys.argv[2:]]
else:
    times = [1.8] + [s + d * 0.8 for k, s, d, _ in tl if k == "scene"] + [total - 1.5]
print(f"{slug}: {total:.1f}s, {len(times)} frames")
render(v, f"/tmp/claude-0/prev/{slug}.mp4", preview_times=times)
