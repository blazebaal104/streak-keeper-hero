"""PNG renders -> WebP sprites nnn_<name>_<step>.webp in res/drawable-nodpi; tape anchors -> assets/nnn/anchor_<kind>.txt ("x y")."""
import json
import os
import sys

from PIL import Image

src, dst = sys.argv[1], sys.argv[2]
os.makedirs(dst, exist_ok=True)
assets = os.path.join(os.path.dirname(os.path.dirname(dst.rstrip("/"))), "assets", "nnn")
os.makedirs(assets, exist_ok=True)
n = 0
for f in sorted(os.listdir(src)):
    if not f.endswith(".png") or f.startswith("_"):
        continue
    name = f[:-4].replace("-", "_")
    im = Image.open(os.path.join(src, f)).convert("RGBA")
    if im.width != 640:
        im = im.resize((640, 640), Image.LANCZOS)
    im.save(os.path.join(dst, f"nnn_{name}.webp"), "WEBP", quality=86, method=6)
    n += 1
anchors = os.path.join(src, "tape_anchors.json")
if os.path.exists(anchors):
    for k, (x, y) in json.load(open(anchors)).items():
        open(os.path.join(assets, f"anchor_{k}.txt"), "w").write(f"{x} {y}\n")
print("packed", n)
