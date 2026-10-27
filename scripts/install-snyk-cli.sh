#!/usr/bin/env bash
# Downloads the Snyk CLI binary matching the runner's OS and CPU architecture (the official snyk/actions setup action
# fetches the x64 build, which doesn't run on ARM64 runners), verifies its published SHA-256 checksum, and prints the
# path of the installed executable.
#
# Usage: scripts/install-snyk-cli.sh [destination-directory]   (default: $RUNNER_TEMP/snyk, else /tmp/snyk)
set -euo pipefail

dest="${1:-${RUNNER_TEMP:-/tmp}/snyk}"
case "$(uname -s)-$(uname -m)" in
  Linux-x86_64) binary=snyk-linux ;;
  Linux-aarch64 | Linux-arm64) binary=snyk-linux-arm64 ;;
  Darwin-x86_64) binary=snyk-macos ;;
  Darwin-arm64) binary=snyk-macos-arm64 ;;
  *) echo "Unsupported platform: $(uname -s)-$(uname -m)" >&2; exit 1 ;;
esac

base=https://downloads.snyk.io/cli/stable
mkdir -p "$dest"
curl -fsSL --retry 3 -o "$dest/$binary" "$base/$binary"
expected=$(curl -fsSL --retry 3 "$base/$binary.sha256" | awk '{print $1}')
actual=$(sha256sum "$dest/$binary" | awk '{print $1}')
if [ "$expected" != "$actual" ]; then
  echo "Checksum mismatch for $binary: expected $expected, got $actual" >&2
  exit 1
fi
mv "$dest/$binary" "$dest/snyk"
chmod +x "$dest/snyk"
echo "$dest/snyk"
