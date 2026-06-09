#!/usr/bin/env bash
set -euo pipefail

# Install JDK 11 via SDKMAN if not already available
if ! command -v java &>/dev/null || ! java -version 2>&1 | grep -q 'version "1[1-9]\|version "[2-9][0-9]'; then
  if ! command -v sdk &>/dev/null; then
    export SDKMAN_DIR="$HOME/.sdkman"
    curl -s "https://get.sdkman.io" | bash
    source "$HOME/.sdkman/bin/sdkman-init.sh"
  else
    source "$HOME/.sdkman/bin/sdkman-init.sh"
  fi
  sdk install java 11.0.23-tem
fi

# Ensure JAVA_HOME is set for future scripts
if command -v sdk &>/dev/null; then
  source "$HOME/.sdkman/bin/sdkman-init.sh"
fi

java -version
echo "Toolchain setup complete."
