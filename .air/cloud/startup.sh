#!/usr/bin/env bash
set -euo pipefail

# Load SDKMAN if available (toolchain may have installed JDK via it)
if [ -f "$HOME/.sdkman/bin/sdkman-init.sh" ]; then
  source "$HOME/.sdkman/bin/sdkman-init.sh"
fi

# Gradle requires sdk.dir or ANDROID_HOME even just to configure the :app project.
# Create a minimal stub so configuration succeeds; actual Android builds need a real SDK.
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
