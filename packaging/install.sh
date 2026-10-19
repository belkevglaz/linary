#!/usr/bin/env bash
# Установка для текущего пользователя, без root: ~/.local/opt, ~/.local/bin, меню приложений.
#   ./packaging/install.sh            — собрать и установить
#   ./packaging/install.sh --no-build — установить уже собранное
set -euo pipefail

name=linary
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
data="${XDG_DATA_HOME:-$HOME/.local/share}"
opt="$HOME/.local/opt/$name"
bin="$HOME/.local/bin"
app="$root/composeApp/build/compose/binaries/main/app/$name"

if [[ "${1:-}" != "--no-build" ]]; then
    (cd "$root" && ./gradlew :composeApp:createDistributable)
fi
[[ -x "$app/bin/$name" ]] || { echo "Нет сборки: $app" >&2; exit 1; }

rm -rf "$opt"
mkdir -p "$opt" "$bin" "$data/applications"
cp -a "$app/." "$opt/"
ln -sfn "$opt/bin/$name" "$bin/$name"

sed "s|^Exec=.*|Exec=$opt/bin/$name|" "$root/packaging/linux/$name.desktop" > "$data/applications/$name.desktop"
for size in 16 24 32 48 64 128 256 512; do
    install -Dm644 "$root/packaging/icons/$name-$size.png" "$data/icons/hicolor/${size}x${size}/apps/$name.png"
done

command -v update-desktop-database >/dev/null && update-desktop-database "$data/applications" || true
command -v gtk-update-icon-cache >/dev/null && gtk-update-icon-cache -q -t "$data/icons/hicolor" || true

echo "Установлено: $opt"
echo "Запуск: $name (или из меню приложений)"
