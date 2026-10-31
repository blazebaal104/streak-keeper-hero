#!/usr/bin/env bash
# Fetches the exact assets the website uses, so the native app matches it:
# Inter + Space Grotesk (Google Fonts) and the Lucide icons (converted to Kotlin).
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p app/src/main/res/font
curl -fsSL "https://github.com/google/fonts/raw/main/ofl/inter/Inter%5Bopsz%2Cwght%5D.ttf" -o app/src/main/res/font/inter.ttf
curl -fsSL "https://github.com/google/fonts/raw/main/ofl/spacegrotesk/SpaceGrotesk%5Bwght%5D.ttf" -o app/src/main/res/font/space_grotesk.ttf
tmp=$(mktemp -d)
(cd "$tmp" && npm init -y >/dev/null && npm i lucide-static@0.575.0 >/dev/null)
python3 ci/lucide2kt.py "$tmp/node_modules/lucide-static/icons" app/src/main/java/com/sbo/app/ui/Lucide.kt \
  flame snowflake bell settings droplets calendar-days chart-line users plus plus:2.5 undo-2 sparkles trending-up lock x chevron-left chevron-right copy user-plus shield message-circle trash-2 send arrow-left crown star check check-check camera log-out user-minus target dumbbell moon maximize-2 check:3

# NNN protections: real CC0 photo textures from Poly Haven (2K diffuse, re-encoded to WebP for the APK)
nnn="app/src/main/res/drawable-nodpi"
python3 -c "import PIL" 2>/dev/null || python3 -m pip install --quiet --break-system-packages pillow || python3 -m pip install --quiet pillow
for id in rough_wood plywood metal_plate metal_plate_02 rusty_metal_02 blue_metal_plate painted_metal_shutter hessian_230 brown_leather red_brick rusty_metal_grid old_planks_02; do
  out="$nnn/tex_${id}.webp"
  [ -f "$out" ] && continue
  curl -fsSL "https://dl.polyhaven.org/file/ph-assets/Textures/jpg/2k/${id}/${id}_diff_2k.jpg" -o "$tmp/${id}.jpg"
  python3 - "$tmp/${id}.jpg" "$out" <<'PY'
import sys
from PIL import Image
im = Image.open(sys.argv[1]).convert("RGB")
im = im.resize((1536, 1536), Image.LANCZOS)
im.save(sys.argv[2], "WEBP", quality=82, method=6)
PY
done
