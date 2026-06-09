#!/usr/bin/env bash
set -euo pipefail

# Use JDK 11 installed by toolchain.sh (Android Gradle plugin 3.1.0 needs Java <= 11)
JDK_DIR="$HOME/jdk11"
if [ -d "$JDK_DIR" ]; then
  export JAVA_HOME="$JDK_DIR"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

# Use Android SDK installed by toolchain.sh
ANDROID_SDK="$HOME/android-sdk"
if [ -d "$ANDROID_SDK" ]; then
  export ANDROID_HOME="$ANDROID_SDK"
  export PATH="$ANDROID_SDK/cmdline-tools/latest/bin:$ANDROID_SDK/platform-tools:$PATH"
fi

# Ensure gradlew is executable
chmod +x gradlew

# Fetch console module dependencies (Gradle downloads the distribution on first run)
./gradlew :console:dependencies --no-daemon -q

echo "Startup complete."
