#!/usr/bin/env bash
# Compile the atom and pack dist/ant/ — the directory tools/publish-atom.sh expects.
set -euo pipefail
cd "$(dirname "$0")"

if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
  JAVAC="$JAVA_HOME/bin/javac"
  JAR="$JAVA_HOME/bin/jar"
else
  JAVAC=javac
  JAR=jar
fi

classes="$(mktemp -d)"
trap 'rm -rf "$classes"' EXIT
"$JAVAC" -encoding UTF-8 -d "$classes" src/AntAtom.java

rm -rf dist/ant
mkdir -p dist/ant
"$JAR" --create --file dist/ant/ant.jar -C "$classes" .
cp task.json dist/ant/
[ -f meta.json ] && cp meta.json dist/ant/

echo "built dist/ant: $(ls dist/ant | tr '\n' ' ')"
