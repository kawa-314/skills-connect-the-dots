#!/usr/bin/env bash
# コーヒー帝国 (Coffee Empire) APK ビルドスクリプト
#
# Android SDK 不要。以下のツールだけで APK を組み立てます:
#   - apktool          : マニフェスト/リソースのコンパイルと APK パッケージング
#   - dalvik-dx        : .class -> classes.dex 変換
#   - uber-apk-signer  : zipalign + v1/v2/v3 署名 (デバッグ鍵)
#   - android (stub)   : javac 用のコンパイル時クラスパス
#
# 使い方: TOOLS_DIR=/path/to/tools ./build.sh
set -euo pipefail
cd "$(dirname "$0")"

TOOLS_DIR="${TOOLS_DIR:-./tools}"
APKTOOL="$TOOLS_DIR/apktool.jar"
DX="$TOOLS_DIR/dx.jar"
SIGNER="$TOOLS_DIR/uber-apk-signer.jar"
ANDROID_JAR="$TOOLS_DIR/android-stub.jar"

mkdir -p "$TOOLS_DIR"
dl() { [ -f "$2" ] || curl -sSL -o "$2" "$1"; }
dl "https://github.com/iBotPeaches/Apktool/releases/download/v2.10.0/apktool_2.10.0.jar" "$APKTOOL"
dl "https://github.com/patrickfav/uber-apk-signer/releases/download/v1.3.0/uber-apk-signer-1.3.0.jar" "$SIGNER"
dl "https://repo1.maven.org/maven2/com/jakewharton/android/repackaged/dalvik-dx/9.0.0_r3/dalvik-dx-9.0.0_r3.jar" "$DX"
dl "https://repo1.maven.org/maven2/com/google/android/android/4.1.1.4/android-4.1.1.4.jar" "$ANDROID_JAR"

BUILD=./build-tmp
rm -rf "$BUILD" && mkdir -p "$BUILD/classes" dist

echo "==> javac"
javac --release 8 -encoding UTF-8 \
      -classpath "$ANDROID_JAR" \
      -d "$BUILD/classes" src/MainActivity.java

echo "==> dx (classes.dex)"
java -cp "$DX" com.android.dx.command.Main --dex \
     --output="app/classes.dex" "$BUILD/classes"

echo "==> apktool build"
java -jar "$APKTOOL" b app -o "$BUILD/coffee-empire-unsigned.apk"

echo "==> sign (debug key)"
java -jar "$SIGNER" -a "$BUILD/coffee-empire-unsigned.apk" --allowResign -o "$BUILD/signed"
cp "$BUILD/signed/"*.apk dist/CoffeeEmpire-debug.apk

echo "==> done: dist/CoffeeEmpire-debug.apk"
