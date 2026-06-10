#!/usr/bin/env bash
set -euo pipefail

# Gradle 4.10 requires JDK ≤ 11; the Air workspace ships JBR 25 which is incompatible.
# Install JDK 11 (Temurin) via SDKMAN in userspace so gradlew can run.

export SDKMAN_DIR="$HOME/.sdkman"

if [ ! -f "$SDKMAN_DIR/bin/sdkman-init.sh" ]; then
  echo "Installing SDKMAN..."
  curl -s "https://get.sdkman.io" | bash
fi

# shellcheck disable=SC1091
source "$SDKMAN_DIR/bin/sdkman-init.sh"

if ! sdk current java 2>/dev/null | grep -q "11\."; then
  echo "Installing JDK 11 (Temurin)..."
  sdk install java 11.0.23-tem
  sdk default java 11.0.23-tem
fi

echo "Toolchain ready: $(java -version 2>&1 | head -1)"
