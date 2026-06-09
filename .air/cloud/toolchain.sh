#!/usr/bin/env bash
set -euo pipefail

# Android Gradle plugin 3.1.0 requires JDK 8 or 11; JDK 25 (JBR default) is incompatible.
# Install JDK 11 via SDKMAN so startup.sh can use it.
export SDKMAN_DIR="$HOME/.sdkman"

if [ ! -f "$SDKMAN_DIR/bin/sdkman-init.sh" ]; then
  curl -s "https://get.sdkman.io" | bash
fi

source "$SDKMAN_DIR/bin/sdkman-init.sh"

if ! sdk list java 2>/dev/null | grep -q "11.*tem.*installed"; then
  sdk install java 11.0.23-tem < /dev/null
fi

sdk default java 11.0.23-tem
java -version
echo "Toolchain setup complete."
