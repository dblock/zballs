#!/usr/bin/env bash
# Runs zBalls in a window. Pass --original to run the 1998 .class files as shipped.
set -euo pipefail
cd "$(dirname "$0")"
if [[ "${1:-}" == "--original" ]]; then
  mkdir -p build/runner
  javac -nowarn -d build/runner runner/AppletRunner.java 2>&1 | grep -v '^Note:' || true
  exec java -cp .:build/runner AppletRunner zballs 400 500 .
fi
[[ -f build/classes/zballs.class ]] || ./build.sh
exec java -cp build/classes AppletRunner zballs 400 500 .
