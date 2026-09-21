#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "$0")" && pwd)

if [ -z "${JAVA_HOME:-}" ]; then
    for candidate in \
        /opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home \
        /usr/local/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home; do
        if [ -x "$candidate/bin/java" ]; then
            JAVA_HOME=$candidate
            break
        fi
    done
fi

if [ -z "${JAVA_HOME:-}" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
    echo "Java 25 was not found. Set JAVA_HOME to a JDK 25 installation." >&2
    exit 1
fi

export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

if ! command -v mvn >/dev/null 2>&1; then
    echo "Maven was not found. Install Maven or add it to PATH." >&2
    exit 1
fi

cd "$ROOT_DIR"
VERSION=${1:-$(mvn -q -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive exec:exec)}
shift $(( $# > 0 ? 1 : 0 ))
JAR_PATH="$ROOT_DIR/owlplug-client/target/owlplug-client-$VERSION.jar"

if [ ! -f "$JAR_PATH" ]; then
    echo "Runnable JAR not found: $JAR_PATH" >&2
    echo "Run ./build-mac.sh first." >&2
    exit 1
fi

exec "$JAVA_HOME/bin/java" -jar "$JAR_PATH" "$@"
