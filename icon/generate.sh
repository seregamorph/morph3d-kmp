#!/bin/sh
# Regenerates the application icons of all assemblies from IconGenerator.java.
# Run from anywhere: ./icon/generate.sh
set -e
cd "$(dirname "$0")/.."

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
java icon/IconGenerator.java "$tmp"

# iOS: a single full bleed 1024x1024 image, the system applies the mask
cp "$tmp/full.png" iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/AppIcon.png

# macOS: window/dock icon at runtime and .icns for the packaged app
cp "$tmp/macos.png" desktopApp/src/main/resources/icon.png
iconset="$tmp/Morph3D.iconset"
mkdir "$iconset"
for s in 16 32 128 256 512; do
    sips -z $s $s "$tmp/macos.png" --out "$iconset/icon_${s}x${s}.png" > /dev/null
    sips -z $((s * 2)) $((s * 2)) "$tmp/macos.png" --out "$iconset/icon_${s}x${s}@2x.png" > /dev/null
done
iconutil -c icns "$iconset" -o desktopApp/icons/Morph3D.icns
