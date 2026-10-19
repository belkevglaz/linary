#!/usr/bin/env bash
# Собирает AppImage из готовой сборки (./gradlew createDistributable).
#   packaging/appimage/build-appimage.sh <версия> [папка-для-результата]
set -euo pipefail

version="${1:?usage: build-appimage.sh <version> [out-dir]}"
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
out="$(mkdir -p "${2:-.}" && cd "${2:-.}" && pwd)"
name=linary
app="$root/composeApp/build/compose/binaries/main/app/$name"
[[ -x "$app/bin/$name" ]] || { echo "No build: $app (run ./gradlew createDistributable)" >&2; exit 1; }

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
appdir="$work/AppDir"

# Приложение jpackage (bin/ и lib/) — в usr/, ярлык и иконка — в корень AppDir
mkdir -p "$appdir/usr"
cp -a "$app/." "$appdir/usr/"
cp "$root/packaging/linux/$name.desktop" "$appdir/$name.desktop"
cp "$root/packaging/icons/$name-256.png" "$appdir/$name.png"
for size in 16 24 32 48 64 128 256 512; do
    install -Dm644 "$root/packaging/icons/$name-$size.png" "$appdir/usr/share/icons/hicolor/${size}x${size}/apps/$name.png"
done
cat > "$appdir/AppRun" <<'RUN'
#!/bin/sh
HERE="$(dirname "$(readlink -f "$0")")"
exec "$HERE/usr/bin/linary" "$@"
RUN
chmod +x "$appdir/AppRun"

tool="$work/appimagetool"
curl -fsSL -o "$tool" \
    https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-x86_64.AppImage
chmod +x "$tool"
# На сборочных машинах нет FUSE — appimagetool распаковывает себя и запускается так
ARCH=x86_64 APPIMAGE_EXTRACT_AND_RUN=1 "$tool" --no-appstream "$appdir" "$out/$name-$version-x86_64.AppImage"
