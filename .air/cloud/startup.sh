#!/usr/bin/env bash
set -euo pipefail

echo "Java: $(java -version 2>&1 | head -1)"

# Ensure gradlew is executable after a fresh clone.
chmod +x gradlew

# The :app subproject requires an Android SDK location at configuration time even when
# we only build the console module. Create a minimal placeholder so the Android Gradle
# plugin finds an sdk.dir and does not abort project configuration.
ANDROID_SDK_DIR="$HOME/.android-sdk"
mkdir -p "$ANDROID_SDK_DIR"
echo "sdk.dir=$ANDROID_SDK_DIR" > local.properties

# Download Gradle distribution and resolve console module dependencies.
./gradlew :console:dependencies --no-daemon
