#!/usr/bin/env bash
set -euo pipefail

# Android Gradle plugin 3.1.0 requires JDK <= 11; default JBR 25 is incompatible.
# Install Temurin JDK 11 from Adoptium into ~/jdk11.
JDK_DIR="$HOME/jdk11"

if [ ! -d "$JDK_DIR" ]; then
  echo "Downloading Temurin JDK 11..."
  TMPFILE="$(mktemp /tmp/jdk11.XXXXXX.tar.gz)"
  curl -L --proxy "${HTTPS_PROXY:-}" \
    "https://api.adoptium.net/v3/binary/latest/11/ga/linux/x64/jdk/hotspot/normal/eclipse" \
    -o "$TMPFILE"
  mkdir -p "$JDK_DIR"
  tar -xzf "$TMPFILE" -C "$JDK_DIR" --strip-components=1
  rm -f "$TMPFILE"
fi

export JAVA_HOME="$JDK_DIR"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
echo "JDK 11 ready at $JDK_DIR"
echo "Toolchain setup complete."
