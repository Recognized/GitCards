#!/usr/bin/env bash
# Startup script for Air cloud workspaces: installs tmux.
set -euo pipefail

if command -v tmux >/dev/null 2>&1; then
    echo "tmux already installed: $(tmux -V)"
    exit 0
fi

SUDO=""
if [ "$(id -u)" -ne 0 ]; then
    if command -v sudo >/dev/null 2>&1; then
        SUDO="sudo"
    else
        echo "Need root privileges to install tmux, but sudo is unavailable." >&2
        exit 1
    fi
fi

echo "Installing tmux..."
export DEBIAN_FRONTEND=noninteractive
$SUDO apt-get update -y
$SUDO apt-get install -y --no-install-recommends tmux

echo "Installed: $(tmux -V)"
