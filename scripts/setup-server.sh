#!/usr/bin/env bash
# Downloads the latest Paper 1.21.8 build into run/ and prepares a local test server.
set -euo pipefail

VERSION="1.21.8"
RUN_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/run"
ACCEPT_EULA=false
USER_AGENT="brockenreich-land-setup-script (+https://github.com/Jedidiah1926/brockenreich-land)"

for arg in "$@"; do
  case "$arg" in
    --accept-eula) ACCEPT_EULA=true ;;
  esac
done

mkdir -p "$RUN_DIR/plugins"
cd "$RUN_DIR"

echo "Fetching Paper $VERSION builds..."
TMP_JSON="$(mktemp)"
trap 'rm -f "$TMP_JSON"' EXIT
curl -sS -A "$USER_AGENT" "https://fill.papermc.io/v3/projects/paper/versions/$VERSION/builds" > "$TMP_JSON"

JQ_FILTER='(map(select(.channel == "STABLE")) as $stable | if ($stable | length) > 0 then $stable else . end) | max_by(.id)'

if command -v jq >/dev/null 2>&1; then
  BUILD_ID=$(jq -r "$JQ_FILTER | .id" "$TMP_JSON")
  DOWNLOAD_URL=$(jq -r "$JQ_FILTER | .downloads.\"server:default\".url" "$TMP_JSON")
  JAR_NAME=$(jq -r "$JQ_FILTER | .downloads.\"server:default\".name // empty" "$TMP_JSON")
elif command -v python3 >/dev/null 2>&1; then
  read -r BUILD_ID DOWNLOAD_URL JAR_NAME < <(python3 - "$TMP_JSON" <<'PYEOF'
import json, sys
with open(sys.argv[1]) as f:
    builds = json.load(f)
stable = [b for b in builds if b.get("channel") == "STABLE"]
candidates = stable if stable else builds
latest = max(candidates, key=lambda b: b["id"])
d = latest["downloads"]["server:default"]
print(latest["id"], d["url"], d.get("name") or "-")
PYEOF
)
else
  echo "Need jq or python3 installed to parse the Paper download API response." >&2
  exit 1
fi

if [ -z "$JAR_NAME" ] || [ "$JAR_NAME" = "null" ] || [ "$JAR_NAME" = "-" ]; then
  JAR_NAME="paper-$VERSION-$BUILD_ID.jar"
fi

echo "Latest build: $BUILD_ID"

if [ ! -f "$JAR_NAME" ]; then
  echo "Downloading $JAR_NAME..."
  curl -sS -A "$USER_AGENT" -o "$JAR_NAME" "$DOWNLOAD_URL"
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
echo "Next: ./gradlew build && ./gradlew deployToRunServer && ./scripts/run-server.sh"
