#!/usr/bin/env bash
set -euo pipefail

# Ensure JAVA_HOME and JDK 8 are available (pre-installed in base image)
java -version 2>&1

# Ensure Gradle wrapper is executable
chmod +x /workspaces/gitcards/gradlew

echo "Development environment ready."
echo "  - Build console JAR:  ./gradlew :console:jar"
echo "  - Run console:        java -jar console.jar"
