#!/usr/bin/env bash
# Runs the local Paper test server set up by setup-server.sh.
set -euo pipefail

RUN_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/run"
cd "$RUN_DIR"

if [ ! -f server.jar ]; then
  echo "run/server.jar not found. Run scripts/setup-server.sh first." >&2
  exit 1
fi

if ! grep -q "eula=true" eula.txt 2>/dev/null; then
  echo "EULA not accepted (run/eula.txt). See https://aka.ms/MinecraftEULA" >&2
  exit 1
fi

java -Xms1G -Xmx2G -jar server.jar --nogui
