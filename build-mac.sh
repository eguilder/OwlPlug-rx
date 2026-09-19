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

if ! command -v jpackage >/dev/null 2>&1; then
    echo "jpackage was not found in JAVA_HOME." >&2
    exit 1
fi

cd "$ROOT_DIR"

VERSION=${1:-$(mvn -q -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive exec:exec)}
ARCH=${2:-$(uname -m)}

case "$ARCH" in
    arm64|aarch64)
        TARGET_PLATFORM=osx-arm64
        ;;
    x86_64|amd64)
        TARGET_PLATFORM=osx-x64
        ;;
    *)
        echo "Unsupported macOS architecture: $ARCH" >&2
        exit 1
        ;;
esac

JAR_PATH="$ROOT_DIR/owlplug-client/target/owlplug-client-$VERSION.jar"
INPUT_DIR="$ROOT_DIR/build/input"
OUTPUT_DIR="$ROOT_DIR/build/output"

rm -f "$INPUT_DIR/owlplug.jar" "$INPUT_DIR/LICENSE"
rm -f "$OUTPUT_DIR/OwlPlug-$VERSION.dmg" "$OUTPUT_DIR/OwlPlug-$VERSION-$TARGET_PLATFORM.dmg"
mkdir -p "$INPUT_DIR" "$OUTPUT_DIR"

echo "Building OwlPlug $VERSION for $TARGET_PLATFORM"
mvn -B clean install -DskipTests
(cd owlplug-client && mvn -B install spring-boot:repackage -DskipTests)

if [ ! -f "$JAR_PATH" ]; then
    echo "Expected runnable JAR was not produced: $JAR_PATH" >&2
    exit 1
fi

cp "$ROOT_DIR/LICENSE" "$INPUT_DIR/LICENSE"
cp "$JAR_PATH" "$INPUT_DIR/owlplug.jar"

jpackage \
    --input "$INPUT_DIR" \
    --name OwlPlug \
    --main-class org.springframework.boot.loader.launch.JarLauncher \
    --main-jar owlplug.jar \
    --license-file "$INPUT_DIR/LICENSE" \
    --dest "$OUTPUT_DIR" \
    --app-version "$VERSION" \
    --icon "$ROOT_DIR/build/resources/owlplug.icns" \
    --vendor OwlPlug \
    --mac-package-identifier owlplug \
    --mac-package-name OwlPlug

DMG_PATH="$OUTPUT_DIR/OwlPlug-$VERSION.dmg"
FINAL_DMG_PATH="$OUTPUT_DIR/OwlPlug-$VERSION-$TARGET_PLATFORM.dmg"
mv "$DMG_PATH" "$FINAL_DMG_PATH"

printf '\nCreated: %s\n' "$FINAL_DMG_PATH"
if command -v hdiutil >/dev/null 2>&1; then
    hdiutil imageinfo "$FINAL_DMG_PATH" | sed -n '1,8p'
fi
shasum -a 256 "$FINAL_DMG_PATH"
