#!/usr/bin/env bash
# Compiles the 1998 sources and the applet runner with a modern JDK, targeting
# Java 8 so that CheerpJ can run the result in the browser.
#
# The sources use `import zInterract;` (a single-name import from the unnamed
# package), which javac rejects since Java 1.4, so those lines are stripped.
# Then patches/*.patch are applied: Restart used Thread.stop(), which throws
# since JDK 20 and doesn't work in CheerpJ.
set -euo pipefail
cd "$(dirname "$0")"
rm -rf build
mkdir -p build/src build/classes
for f in *.java; do
  grep -v '^import zInterract;' "$f" > "build/src/$f"
done
for p in patches/*.patch; do
  patch -s --no-backup-if-mismatch -d build/src -p1 < "$p"
done
javac -nowarn --release 8 -encoding ISO-8859-1 -d build/classes build/src/*.java runner/AppletRunner.java demo/DemoRecorder.java 2>&1 | grep -v '^Note:\|bootstrap class path\|^1 warning' || true
test -f build/classes/zballs.class
echo "Built into build/classes."
