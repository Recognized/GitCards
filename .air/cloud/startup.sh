#!/usr/bin/env bash
set -euo pipefail

# Source SDKMAN so JDK 11 (installed by toolchain.sh) takes precedence over JBR 25.
if [ -f "$HOME/.sdkman/bin/sdkman-init.sh" ]; then
  # shellcheck disable=SC1091
  source "$HOME/.sdkman/bin/sdkman-init.sh"
fi

echo "Java: $(java -version 2>&1 | head -1)"

# Ensure gradlew is executable after a fresh clone.
chmod +x gradlew

# Download Gradle distribution and fetch console module dependencies.
./gradlew :console:dependencies --no-daemon -q
