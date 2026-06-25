#!/usr/bin/env bash
set -euo pipefail

# Install JDK 8 (Adoptium Temurin) in userspace — sudo is not available
JDK_DIR="$HOME/.jdk"
if [ ! -d "$JDK_DIR" ]; then
  echo "Downloading JDK 8 (Adoptium Temurin)..."
  JDK_URL="https://api.adoptium.net/v3/binary/latest/8/ga/linux/x64/jdk/hotspot/normal/eclipse"
  mkdir -p "$JDK_DIR"
  curl -fSL "$JDK_URL" | tar -xz -C "$JDK_DIR" --strip-components=1
  echo "JDK installed at $JDK_DIR"
else
  echo "JDK already installed at $JDK_DIR"
fi

export JAVA_HOME="$JDK_DIR"
export PATH="$JAVA_HOME/bin:$PATH"
java -version

# Ensure Gradle wrapper is executable
chmod +x gradlew

# Pre-fetch Gradle wrapper distribution and console module dependencies
echo "Resolving Gradle dependencies for console module..."
./gradlew :console:dependencies --no-daemon -q

echo "Building console module..."
./gradlew :console:build --no-daemon

echo "Startup complete."
