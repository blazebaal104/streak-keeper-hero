#!/usr/bin/env bash
# Runs inside the emulator job: installs SBO Dev (native) and screenshots the home screen in each
# button state, a few frames apart, so animations (fire, embers) can be judged.
set -u
OUT=shots
mkdir -p $OUT
adb install -r android-native/app/build/outputs/apk/dev/debug/app-dev-debug.apk
adb shell settings put system screen_off_timeout 600000
for mode in fire:14 fire:2 ice:9 neutral; do
  name=${mode/:/-}
  adb shell am force-stop com.sbo.app.dev
  adb logcat -c
  adb shell am start -n com.sbo.app.dev/com.sbo.app.MainActivity --es preview "$mode"
  sleep 12
  for f in 1 2 3; do
    adb exec-out screencap -p > "$OUT/native-$name-$f.png"
    sleep 0.35
  done
  adb logcat -d -t 400 '*:E' > "$OUT/logcat-$name.txt" || true
done
