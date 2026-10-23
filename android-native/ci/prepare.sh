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
  flame snowflake bell settings droplets calendar-days chart-line users plus plus:2.5 undo-2 sparkles trending-up lock x chevron-left chevron-right copy user-plus shield message-circle trash-2 send arrow-left crown star check check-check camera log-out user-minus target
