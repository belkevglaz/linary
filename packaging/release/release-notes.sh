#!/usr/bin/env bash
# Печатает раздел «## <версия>» из CHANGES.md — текст релиза на GitHub.
# Ошибка, если раздела нет или он пустой: релиз без описания изменений не выпускается.
#   packaging/release/release-notes.sh 1.0.1
set -euo pipefail

version="${1:?usage: release-notes.sh <version>}"
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

notes=$(awk -v version="$version" '
    /^## / { if (found) exit; if ($2 == version) { found = 1; next } }
    found { print }
' "$root/CHANGES.md" | sed -e '/./,$!d')   # без пустых строк в начале

if [ -z "${notes//[[:space:]]/}" ]; then
    echo "CHANGES.md: no section '## $version' (or it is empty)" >&2
    exit 1
fi
printf '%s\n' "$notes"
