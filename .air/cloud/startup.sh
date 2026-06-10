#!/usr/bin/env bash
set -euo pipefail

echo "Java: $(java -version 2>&1 | head -1)"

# Ensure gradlew is executable after a fresh clone.
chmod +x gradlew

# Create a minimal Android SDK placeholder so the :app project can configure.
ANDROID_SDK_DIR="$HOME/.android-sdk"
mkdir -p "$ANDROID_SDK_DIR"
echo "sdk.dir=$ANDROID_SDK_DIR" > local.properties

# Android Gradle plugin 3.1.0 bundles JAXB which uses ClassLoader.defineClass via
# reflection. Java 9+ module system blocks this without --add-opens. Apply globally
# via JAVA_TOOL_OPTIONS so it reaches any forked Gradle JVM as well.
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} --add-opens java.base/java.lang=ALL-UNNAMED"

# Download Gradle distribution and resolve console module dependencies.
# --configure-on-demand limits project configuration to :console, avoiding the need
# for a full Android SDK installation that :app would require.
./gradlew :console:dependencies --configure-on-demand --no-daemon
