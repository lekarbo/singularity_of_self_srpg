#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ENV_DIR="$ROOT_DIR/.gradle-env"
JAVA_ENV_DIR="$ROOT_DIR/.java-env"
GRADLE_VERSION="8.7"
GRADLE_HOME="$ENV_DIR/gradle-${GRADLE_VERSION}"
JDK_HOME="$JAVA_ENV_DIR/jdk-17.0.11+9"
GRADLE_TMP="$ROOT_DIR/.gradle-tmp"
GRADLE_USER_HOME="$ROOT_DIR/.gradle-user"

if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
    "$ROOT_DIR/scripts/create-env.sh"
fi

if [ ! -x "$JDK_HOME/bin/java" ]; then
    "$ROOT_DIR/scripts/create-jdk.sh"
fi

export JAVA_HOME="$JDK_HOME"
export PATH="$JAVA_HOME/bin:$PATH"
mkdir -p "$GRADLE_TMP"
mkdir -p "$GRADLE_USER_HOME"
export GRADLE_OPTS="${GRADLE_OPTS:-} -Dorg.gradle.native=false -Dorg.gradle.vfs.watch=false -Djava.io.tmpdir=$GRADLE_TMP -Djava.net.preferIPv4Stack=true -Dorg.gradle.daemon=false"
export TMPDIR="$GRADLE_TMP"
export TEMP="$GRADLE_TMP"
export TMP="$GRADLE_TMP"
export GRADLE_USER_HOME

exec "$GRADLE_HOME/bin/gradle" --no-daemon --project-dir "$ROOT_DIR" "$@"
