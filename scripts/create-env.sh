#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ENV_DIR="$ROOT_DIR/.gradle-env"
GRADLE_VERSION="8.7"
GRADLE_ARCHIVE="gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_URL="https://services.gradle.org/distributions/${GRADLE_ARCHIVE}"
GRADLE_HOME="$ENV_DIR/gradle-${GRADLE_VERSION}"

mkdir -p "$ENV_DIR"

if [ -x "$GRADLE_HOME/bin/gradle" ]; then
    echo "Gradle ${GRADLE_VERSION} already installed in ${ENV_DIR}" >&2
    exit 0
fi

TMP_ZIP="$ENV_DIR/${GRADLE_ARCHIVE}"

echo "Downloading Gradle ${GRADLE_VERSION}..." >&2
curl -L "$GRADLE_URL" -o "$TMP_ZIP"

echo "Extracting Gradle..." >&2
unzip -q "$TMP_ZIP" -d "$ENV_DIR"
rm -f "$TMP_ZIP"

echo "Gradle available at ${GRADLE_HOME}" >&2
