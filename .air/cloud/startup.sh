#!/usr/bin/env bash
set -euo pipefail

# Use JDK 11 installed by toolchain.sh (Android Gradle plugin 3.1.0 needs Java <= 11)
JDK_DIR="$HOME/jdk11"
if [ -d "$JDK_DIR" ]; then
  export JAVA_HOME="$JDK_DIR"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

# Gradle requires sdk.dir or ANDROID_HOME even just to configure the :app project.
# Create a minimal stub so Gradle configuration succeeds; actual Android builds need a real SDK.
if [ -z "${ANDROID_HOME:-}" ] && [ ! -f local.properties ]; then
  ANDROID_STUB="$HOME/.android-sdk-stub"
  mkdir -p "$ANDROID_STUB"
  echo "sdk.dir=$ANDROID_STUB" > local.properties
fi

# Ensure gradlew is executable
chmod +x gradlew

# Fetch console module dependencies (Gradle downloads the distribution on first run)
./gradlew :console:dependencies --no-daemon -q

echo "Startup complete."
