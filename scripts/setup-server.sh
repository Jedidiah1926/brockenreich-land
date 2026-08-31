#!/usr/bin/env bash
# Downloads the latest Paper 1.21.8 build into run/ and prepares a local test server.
set -euo pipefail

VERSION="1.21.8"
RUN_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/run"
ACCEPT_EULA=false

for arg in "$@"; do
  case "$arg" in
    --accept-eula) ACCEPT_EULA=true ;;
  esac
done

mkdir -p "$RUN_DIR/plugins"
cd "$RUN_DIR"

echo "Fetching latest Paper $VERSION build number..."
BUILD=$(curl -sS "https://api.papermc.io/v2/projects/paper/versions/$VERSION" | grep -o '"builds":\[[0-9, ]*\]' | tr -d '[]"builds:' | tr ',' '\n' | tail -1)
if [ -z "$BUILD" ]; then
  echo "Could not determine latest build for Paper $VERSION" >&2
  exit 1
fi
echo "Latest build: $BUILD"

JAR_NAME="paper-$VERSION-$BUILD.jar"
if [ ! -f "$JAR_NAME" ]; then
  echo "Downloading $JAR_NAME..."
  curl -sS -o "$JAR_NAME" \
    "https://api.papermc.io/v2/projects/paper/versions/$VERSION/builds/$BUILD/downloads/$JAR_NAME"
else
  echo "$JAR_NAME already present, skipping download."
fi

ln -sf "$JAR_NAME" server.jar

if [ "$ACCEPT_EULA" = true ]; then
  echo "eula=true" > eula.txt
  echo "Wrote eula.txt (you have accepted the Mojang EULA: https://aka.ms/MinecraftEULA)."
else
  echo "eula=false" > eula.txt
  echo
  echo "NOTE: run/eula.txt was created with eula=false."
  echo "Read https://aka.ms/MinecraftEULA, then either edit run/eula.txt to eula=true"
  echo "or re-run this script with --accept-eula."
fi

echo
echo "Setup complete. Server jar: run/$JAR_NAME (symlinked as run/server.jar)"
echo "Next: ./gradlew build && cp build/libs/*.jar run/plugins/ && ./scripts/run-server.sh"
