#!/usr/bin/env bash
# Gives the screenshots workflow's captures matching drop shadows and lays them out side by side.
# Usage: scripts/compose-screenshots.sh <dir with macos.png windows.png linux-gnome.png> <output dir>
# Written against ImageMagick 6, which is what Ubuntu ships, so it runs under 7 as well.
set -euo pipefail

in="$1"
out="$2"
font="${FONT:-DejaVu-Sans}"
mkdir -p "$out"

width() { identify -format '%w\n' "$1"; }
height() { identify -format '%h\n' "$1"; }

# Matches the shadow macOS gives its own capture: 56px either side, 38px above and 74px below.
shadow() {
  local w h
  w=$(width "$1")
  h=$(height "$1")
  convert "$1" -alpha set -background none \
    \( -clone 0 -fill black -colorize 100 -channel A -evaluate multiply 0.35 +channel \
       -extent "$((w + 112))x$((h + 112))-56-56" -blur 0x16 \) \
    \( -clone 0 -extent "$((w + 112))x$((h + 112))-56-38" \) \
    -delete 0 -compose over -composite "$2"
}

# The Windows capture's outer pixel is the window border over whatever desktop lay behind it, and its
# corners hold desktop pixels where Windows 11 rounds them by 8px: redraw the border, cut the corners.
w=$(width "$in/windows.png")
h=$(height "$in/windows.png")
convert "$in/windows.png" -alpha set -shave 1x1 -bordercolor '#c6c6c6' -border 1 \
  \( -size "${w}x${h}" xc:none -fill white -draw "roundrectangle 0,0 $((w - 1)),$((h - 1)) 8,8" \) \
  -compose DstIn -composite "$out/windows.png"
shadow "$out/windows.png" "$out/windows.png"
shadow "$in/linux-gnome.png" "$out/linux-gnome.png"
cp "$in/macos.png" "$out/macos.png"

# Equal heights put the three captions on one baseline.
tallest=$(for image in macos windows linux-gnome; do height "$out/$image.png"; done | sort -n | tail -1)
tiles=$(mktemp -d)
for image in macos windows linux-gnome; do
  convert "$out/$image.png" -background none -gravity north -extent "$(width "$out/$image.png")x$tallest" \
    "$tiles/$image.png"
done
montage -background none -fill '#8b949e' -font "$font" -pointsize 40 \
  -label macOS "$tiles/macos.png" -label Windows "$tiles/windows.png" -label 'Linux (GNOME)' "$tiles/linux-gnome.png" \
  -tile 3x1 -geometry +8+0 "$out/desktops.png"
rm -r "$tiles"
