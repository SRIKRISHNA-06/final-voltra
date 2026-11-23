#!/usr/bin/env sh
set -e
MVN_DIR="$HOME/.m2/wrapper/dists/apache-maven-3.9.9-bin/voltra"
MVN_BIN="$MVN_DIR/apache-maven-3.9.9/bin/mvn"
if [ ! -x "$MVN_BIN" ]; then
  mkdir -p "$MVN_DIR"
  curl -fsSL "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip" -o "$MVN_DIR/maven.zip"
  unzip -q "$MVN_DIR/maven.zip" -d "$MVN_DIR"
  rm -f "$MVN_DIR/maven.zip"
fi
exec "$MVN_BIN" "$@"
