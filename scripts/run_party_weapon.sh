#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo "Launching SRPG prototype for party + weapon menu flow..."
echo "Flow: Party menu (P) -> Weapon menu (V) -> Placement."
echo "Shortcuts: P opens party roster, V opens weapon menu, Enter equips."

"$ROOT_DIR/scripts/gradle.sh" run
