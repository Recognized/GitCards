#!/usr/bin/env bash
set -euo pipefail

# Load SDKMAN if available
if [ -f "$HOME/.sdkman/bin/sdkman-init.sh" ]; then
  source "$HOME/.sdkman/bin/sdkman-init.sh"
fi

# Ensure gradlew is executable
chmod +x gradlew

# Download Gradle and fetch console module dependencies
./gradlew :console:dependencies --no-daemon -q

echo "Startup complete."
