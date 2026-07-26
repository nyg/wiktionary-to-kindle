#!/usr/bin/env bash
#
# Regenerates the platform icons from assets/icon.svg.
#
# Run on macOS: iconutil (for .icns) ships with Xcode's command line tools, and rsvg-convert or
# ImageMagick is needed to rasterise the SVG. The generated files are committed, so this only needs
# running when the artwork changes — CI never invokes it.
#
#   brew install librsvg   # provides rsvg-convert
#   scripts/generate-icons.sh
set -euo pipefail

readonly PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

readonly SVG="assets/icon.svg"
readonly ICONSET="assets/icon.iconset"

die() { printf 'error: %s\n' "$*" >&2; exit 1; }

[[ -f "$SVG" ]] || die "$SVG not found"

rasterise() {
  local size="$1" out="$2"
  if command -v rsvg-convert >/dev/null 2>&1; then
    rsvg-convert -w "$size" -h "$size" "$SVG" -o "$out"
  elif command -v magick >/dev/null 2>&1; then
    magick -background none -density 600 "$SVG" -resize "${size}x${size}" "$out"
  else
    die "need rsvg-convert or magick — try: brew install librsvg"
  fi
}

# Base PNG, used directly on Linux.
rasterise 512 assets/icon.png

# macOS .icns via an .iconset directory.
rm -rf "$ICONSET"
mkdir -p "$ICONSET"
for size in 16 32 128 256 512; do
  rasterise "$size" "$ICONSET/icon_${size}x${size}.png"
  rasterise "$((size * 2))" "$ICONSET/icon_${size}x${size}@2x.png"
done

if command -v iconutil >/dev/null 2>&1; then
  iconutil -c icns "$ICONSET" -o assets/icon.icns
  echo "wrote assets/icon.icns"
else
  echo "iconutil not available (not macOS?) — skipping .icns" >&2
fi

# Windows .ico, multi-resolution.
if command -v magick >/dev/null 2>&1; then
  magick assets/icon.png -define icon:auto-resize=256,128,64,48,32,16 assets/icon.ico
  echo "wrote assets/icon.ico"
else
  echo "magick not available — skipping .ico (brew install imagemagick)" >&2
fi

echo "wrote assets/icon.png and $ICONSET"
