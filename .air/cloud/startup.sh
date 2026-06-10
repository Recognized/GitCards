#!/usr/bin/env bash
set -euo pipefail

echo "Java: $(java -version 2>&1 | head -1)"

# Ensure gradlew is executable after a fresh clone.
chmod +x gradlew

# Create a minimal Android SDK placeholder so the :app project can configure
# if it needs to be evaluated (fallback when --configure-on-demand is not enough).
ANDROID_SDK_DIR="$HOME/.android-sdk"
mkdir -p "$ANDROID_SDK_DIR"
echo "sdk.dir=$ANDROID_SDK_DIR" > local.properties

# Download Gradle distribution and resolve console module dependencies.
# --configure-on-demand limits project configuration to :console, so :app is not
# evaluated and the full Android SDK installation is not required.
./gradlew :console:dependencies --configure-on-demand --no-daemon
