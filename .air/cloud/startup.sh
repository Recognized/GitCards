#!/usr/bin/env bash
set -euo pipefail

# Use JDK 11 installed by toolchain.sh (Android Gradle plugin 3.1.0 needs Java <= 11)
export SDKMAN_DIR="$HOME/.sdkman"
if [ -f "$SDKMAN_DIR/bin/sdkman-init.sh" ]; then
  source "$SDKMAN_DIR/bin/sdkman-init.sh"
  sdk use java 11.0.23-tem
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
