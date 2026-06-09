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

# Install Android SDK command-line tools so sdkmanager can install platform-26 + build-tools-27.0.3
ANDROID_SDK="$HOME/android-sdk"
CMDLINE_TOOLS_DIR="$ANDROID_SDK/cmdline-tools/latest"

if [ ! -d "$CMDLINE_TOOLS_DIR" ]; then
  echo "Downloading Android command-line tools..."
  CMDZIP="$(mktemp /tmp/cmdtools.XXXXXX.zip)"
  curl -L --proxy "${HTTPS_PROXY:-}" \
    "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" \
    -o "$CMDZIP"
  mkdir -p "$CMDLINE_TOOLS_DIR"
  python3 -c "
import zipfile, sys
with zipfile.ZipFile('$CMDZIP') as z:
    for m in z.infolist():
        name = m.filename
        # strip leading 'cmdline-tools/' prefix
        stripped = name[len('cmdline-tools/'):] if name.startswith('cmdline-tools/') else name
        if not stripped:
            continue
        import os
        dest = os.path.join('$CMDLINE_TOOLS_DIR', stripped)
        if m.is_dir():
            os.makedirs(dest, exist_ok=True)
        else:
            os.makedirs(os.path.dirname(dest), exist_ok=True)
            with z.open(m) as src, open(dest, 'wb') as dst:
                dst.write(src.read())
            # preserve executable bit
            if m.external_attr >> 16 & 0o111:
                os.chmod(dest, os.stat(dest).st_mode | 0o111)
"
  rm -f "$CMDZIP"
fi

export ANDROID_HOME="$ANDROID_SDK"
export PATH="$ANDROID_SDK/cmdline-tools/latest/bin:$ANDROID_SDK/platform-tools:$PATH"

# Accept licenses
mkdir -p "$ANDROID_SDK/licenses"
printf '\nd56f5187479451eabf01fb78af6dfcb131a6481e\n24333f8a63b6825ea9c5514f83c2829b004d1fee' \
  > "$ANDROID_SDK/licenses/android-sdk-license"
printf '\n84831b9409646a918e30573bab4c9c91346d8abd' \
  > "$ANDROID_SDK/licenses/android-sdk-preview-license"

# Install required Android SDK packages if not already present
if [ ! -d "$ANDROID_SDK/platforms/android-26" ] || [ ! -d "$ANDROID_SDK/build-tools/27.0.3" ]; then
  echo "Installing Android SDK packages..."
  yes | sdkmanager --sdk_root="$ANDROID_SDK" "platforms;android-26" "build-tools;27.0.3" 2>&1 || true
fi

echo "Android SDK ready at $ANDROID_SDK"
echo "Toolchain setup complete."
