#!/usr/bin/env bash
# Удаляет то, что поставил install.sh. Настройки и резервные копии базы остаются:
#   ~/.local/share/linary/backups, ~/.java/.userPrefs/linary
set -euo pipefail

name=linary
data="${XDG_DATA_HOME:-$HOME/.local/share}"

rm -rf "$HOME/.local/opt/$name"
rm -f "$HOME/.local/bin/$name" "$data/applications/$name.desktop"
for size in 16 24 32 48 64 128 256 512; do
    rm -f "$data/icons/hicolor/${size}x${size}/apps/$name.png"
done
command -v update-desktop-database >/dev/null && update-desktop-database "$data/applications" || true
echo "Удалено."
