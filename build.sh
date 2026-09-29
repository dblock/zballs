#!/usr/bin/env bash
# Compiles the 1998 sources and the applet runner with a modern JDK.
# The sources use `import zInterract;` (a single-name import from the unnamed
# package), which javac rejects since Java 1.4, so those lines are stripped.
set -euo pipefail
cd "$(dirname "$0")"
rm -rf build
mkdir -p build/src build/classes
for f in *.java; do
  grep -v '^import zInterract;' "$f" > "build/src/$f"
done
javac -nowarn -encoding ISO-8859-1 -d build/classes build/src/*.java runner/AppletRunner.java demo/DemoRecorder.java 2>&1 | grep -v '^Note:' || true
test -f build/classes/zballs.class
echo "Built into build/classes."
