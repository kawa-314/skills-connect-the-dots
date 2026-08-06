#!/usr/bin/env bash
# ネイティブ版(Java + Canvas)のAPKビルド。Android SDK 不要。
set -euo pipefail
cd "$(dirname "$0")"

TOOLS_DIR="${TOOLS_DIR:-./tools}"
APKTOOL="$TOOLS_DIR/apktool.jar"
DX="$TOOLS_DIR/dx.jar"
SIGNER="$TOOLS_DIR/uber-apk-signer.jar"
ANDROID_JAR="${ANDROID_JAR:-$TOOLS_DIR/android-stub.jar}"

mkdir -p "$TOOLS_DIR" dist
dl() { [ -f "$2" ] || curl -sSL -o "$2" "$1"; }
dl "https://github.com/iBotPeaches/Apktool/releases/download/v2.10.0/apktool_2.10.0.jar" "$APKTOOL"
dl "https://github.com/patrickfav/uber-apk-signer/releases/download/v1.3.0/uber-apk-signer-1.3.0.jar" "$SIGNER"
dl "https://repo1.maven.org/maven2/com/jakewharton/android/repackaged/dalvik-dx/9.0.0_r3/dalvik-dx-9.0.0_r3.jar" "$DX"
dl "https://repo1.maven.org/maven2/com/google/android/android/4.1.1.4/android-4.1.1.4.jar" "$ANDROID_JAR"

BUILD=./build-apk
rm -rf "$BUILD"; mkdir -p "$BUILD/classes" "$BUILD/pkg"

echo "==> javac (core + android)"
javac --release 8 -encoding UTF-8 -nowarn \
      -classpath "$ANDROID_JAR" \
      -d "$BUILD/classes" $(find core android -name "*.java")

echo "==> dx"
java -cp "$DX" com.android.dx.command.Main --dex --min-sdk-version=26 --output="$BUILD/pkg/classes.dex" "$BUILD/classes"

echo "==> パッケージ構成"
cp app-manifest.xml "$BUILD/pkg/AndroidManifest.xml"
cp apktool.yml "$BUILD/pkg/apktool.yml"
cp -r res "$BUILD/pkg/res"

echo "==> apktool build"
java -jar "$APKTOOL" b "$BUILD/pkg" -o "$BUILD/unsigned.apk"

echo "==> sign"
java -jar "$SIGNER" -a "$BUILD/unsigned.apk" --allowResign -o "$BUILD/signed"
cp "$BUILD/signed/"*.apk dist/CoffeeIncNative-debug.apk
echo "==> done: dist/CoffeeIncNative-debug.apk"
