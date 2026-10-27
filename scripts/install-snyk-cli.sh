#!/usr/bin/env bash
# Downloads the Snyk CLI binary matching the runner's OS and CPU architecture (the official snyk/actions setup action
# fetches the x64 build, which doesn't run on ARM64 runners), verifies its published SHA-256 checksum, and prints the
# path of the installed executable.
#
# It is installed where snyk-maven-plugin looks for (and, if missing or stale, downloads) its own CLI, so the plugin
# reuses it without any POM configuration. That location is ~/.local/share/snyk/snyk-linux on Linux and
# ~/Library/Application Support/Snyk/snyk-macos on macOS (honouring XDG_DATA_HOME on Linux).
#
# Usage: scripts/install-snyk-cli.sh
set -euo pipefail

case "$(uname -s)-$(uname -m)" in
  Linux-x86_64) binary=snyk-linux; dest="${XDG_DATA_HOME:-$HOME/.local/share}/snyk"; target=snyk-linux ;;
  Linux-aarch64 | Linux-arm64) binary=snyk-linux-arm64; dest="${XDG_DATA_HOME:-$HOME/.local/share}/snyk"; target=snyk-linux ;;
  Darwin-x86_64) binary=snyk-macos; dest="$HOME/Library/Application Support/Snyk"; target=snyk-macos ;;
  Darwin-arm64) binary=snyk-macos-arm64; dest="$HOME/Library/Application Support/Snyk"; target=snyk-macos ;;
  *) echo "Unsupported platform: $(uname -s)-$(uname -m)" >&2; exit 1 ;;
esac

base=https://downloads.snyk.io/cli/stable
mkdir -p "$dest"
curl -fsSL --retry 3 -o "$dest/$binary" "$base/$binary"
expected=$(curl -fsSL --retry 3 "$base/$binary.sha256" | awk '{print $1}')
actual=$(shasum -a 256 "$dest/$binary" | awk '{print $1}')
if [ "$expected" != "$actual" ]; then
  echo "Checksum mismatch for $binary: expected $expected, got $actual" >&2
  exit 1
fi
mv "$dest/$binary" "$dest/$target"
chmod +x "$dest/$target"
echo "$dest/$target"
