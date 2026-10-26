#!/usr/bin/env bash
set -euo pipefail

REPO_URL="https://github.com/Lytiy74/payment-engine.git"
REPO_DIR="~projects/payment-engine"

sudo dnf install -y java-21-openjdk-devel git tmux
java -version
uname -m
if [ -d "$REPO_DIR/.git" ]; then
  git -C "$REPO_DIR" pull --ff-only
else
  git clone "$REPO_URL" "$REPO_DIR"
fi
cd "$REPO_DIR"
./mvnw -B verify
