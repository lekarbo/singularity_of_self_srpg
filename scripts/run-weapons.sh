#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo "Launching SRPG prototype for weapon menu testing..."
echo "Tip: press V in-game to open the weapon loadout menu and Enter to equip."

"$ROOT_DIR/scripts/gradle.sh" run
