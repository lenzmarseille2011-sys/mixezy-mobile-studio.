#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
command -v java >/dev/null || { echo "JDK 17 requis" >&2; exit 1; }
command -v gradle >/dev/null || { echo "Gradle 8.9 requis (ou utilise GitHub Actions)" >&2; exit 1; }
python3 "$ROOT/tools/validate_android_project.py"
if [[ -n "${MIXEZY_SERVER_URL:-}" ]]; then
  case "$MIXEZY_SERVER_URL" in https://*) ;; *) echo "MIXEZY_SERVER_URL doit utiliser HTTPS" >&2; exit 1;; esac
  gradle -p "$ROOT/android" --no-daemon -PMIXEZY_SERVER_URL="$MIXEZY_SERVER_URL" assembleDebug
else
  gradle -p "$ROOT/android" --no-daemon assembleDebug
fi
APK="$ROOT/android/app/build/outputs/apk/debug/app-debug.apk"
test -s "$APK"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[[ -n "$SDK" ]] || { echo "ANDROID_HOME/ANDROID_SDK_ROOT doit être défini pour vérifier l'APK" >&2; exit 1; }
APKSIGNER="$(find "$SDK/build-tools" -type f -name apksigner | sort -V | tail -n 1)"
test -x "$APKSIGNER"
"$APKSIGNER" verify --verbose "$APK"
OUT="$ROOT/dist"
mkdir -p "$OUT"
cp "$APK" "$OUT/Mixezy-Mobile-v0.9.3-debug.apk"
(cd "$OUT" && sha256sum Mixezy-Mobile-v0.9.3-debug.apk > Mixezy-Mobile-v0.9.3-debug.apk.sha256)
echo "APK créé: $OUT/Mixezy-Mobile-v0.9.3-debug.apk"
