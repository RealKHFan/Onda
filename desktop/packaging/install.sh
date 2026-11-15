#!/bin/sh
# Installs Onda for your user only: no root, nothing in the system (fine on Bazzite / Fedora Atomic).
set -e
here="$(cd "$(dirname "$0")" && pwd)"
dest="$HOME/.local/share/onda-app"
rm -rf "$dest"
mkdir -p "$dest" "$HOME/.local/share/applications" "$HOME/.local/share/icons/hicolor/256x256/apps" "$HOME/.local/bin"
cp -r "$here/Onda/." "$dest/"
cp "$dest/lib/Onda.png" "$HOME/.local/share/icons/hicolor/256x256/apps/io.github.realkhfan.Onda.png"
ln -sf "$dest/bin/Onda" "$HOME/.local/bin/onda"
cat > "$HOME/.local/share/applications/io.github.realkhfan.Onda.desktop" <<DESKTOP
[Desktop Entry]
Type=Application
Name=Onda
Comment=Music player without an account or tracking
Exec=$dest/bin/Onda %F
MimeType=audio/mpeg;audio/flac;audio/x-flac;audio/ogg;audio/opus;audio/x-vorbis+ogg;audio/wav;audio/x-wav;audio/mp4;audio/x-m4a;audio/aac;
Icon=io.github.realkhfan.Onda
Categories=AudioVideo;Audio;Player;
StartupWMClass=io-github-realkhfan-onda-desktop-MainKt
Terminal=false
DESKTOP
update-desktop-database "$HOME/.local/share/applications" 2>/dev/null || true
echo "Onda is installed. Find it in your app menu, or run: onda"
echo "To remove it: rm -r ~/.local/share/onda-app ~/.local/bin/onda ~/.local/share/applications/io.github.realkhfan.Onda.desktop"
