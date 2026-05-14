#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

export GRADLE_USER_HOME="$PWD/.gradle-user-home"
if [[ -z "${ANDROID_HOME:-}" && -n "${LOCALAPPDATA:-}" && -d "$LOCALAPPDATA/Android/Sdk" ]]; then
  export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
fi
if [[ -z "${JAVA_HOME:-}" && -n "${PROGRAMFILES:-}" && -d "$PROGRAMFILES/Android/Android Studio/jbr" ]]; then
  export JAVA_HOME="$PROGRAMFILES/Android/Android Studio/jbr"
fi
if [[ -n "${JAVA_HOME:-}" ]]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi

step() {
  printf '\n==> %s\n' "$1"
}

step "Environment"
if [[ -z "${JAVA_HOME:-}" ]]; then
  echo "JAVA_HOME is not set. Set JAVA_HOME to a valid JDK before running checks." >&2
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  echo "java was not found on PATH. Add JAVA_HOME/bin to PATH before running checks." >&2
  exit 1
fi

java -version

if [[ -z "${ANDROID_HOME:-}" ]]; then
  echo "ANDROID_HOME is not set. Set ANDROID_HOME or create local.properties with sdk.dir." >&2
  exit 1
fi

if [[ ! -d "$ANDROID_HOME" ]]; then
  echo "ANDROID_HOME points to a missing directory: $ANDROID_HOME" >&2
  exit 1
fi

step "Unit tests"
./gradlew test

step "Lint debug"
./gradlew lintDebug

step "Assemble debug"
./gradlew assembleDebug

step "Assemble debug Android tests"
./gradlew assembleDebugAndroidTest

if [[ "${RUN_CONNECTED_TESTS:-}" == "1" ]]; then
  step "Connected debug Android tests"
  ./gradlew connectedDebugAndroidTest
fi

printf '\nAll local checks passed.\n'
