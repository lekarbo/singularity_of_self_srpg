#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
JAVA_ENV_DIR="$ROOT_DIR/.java-env"
JDK_VERSION="17.0.11+9"
JDK_ARCHIVE="OpenJDK17U-jdk_x64_linux_hotspot_17.0.11_9.tar.gz"
JDK_URL="https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.11%2B9/${JDK_ARCHIVE}"
JDK_HOME="$JAVA_ENV_DIR/jdk-17.0.11+9"

mkdir -p "$JAVA_ENV_DIR"

if [ -x "$JDK_HOME/bin/java" ]; then
    echo "JDK ${JDK_VERSION} already installed in ${JAVA_ENV_DIR}" >&2
    exit 0
fi

TMP_ARCHIVE="$JAVA_ENV_DIR/${JDK_ARCHIVE}"

echo "Downloading Temurin ${JDK_VERSION}..." >&2
curl -L "$JDK_URL" -o "$TMP_ARCHIVE"

echo "Extracting JDK..." >&2
tar -xzf "$TMP_ARCHIVE" -C "$JAVA_ENV_DIR"
rm -f "$TMP_ARCHIVE"

echo "JDK available at ${JDK_HOME}" >&2
