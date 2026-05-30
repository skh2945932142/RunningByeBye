#!/bin/bash
set -e

# RunningByeBye release APK build script for Linux/macOS.
# App-only mode: if mobile/mobile.go is missing, reuse android/app/libs/mobile.aar.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "[1/5] Checking environment..."
command -v java >/dev/null || { echo "ERROR: Java not found"; exit 1; }
if [ -z "$ANDROID_HOME" ] && [ -f android/local.properties ]; then
    ANDROID_HOME="$(sed -n 's/^sdk.dir=//p' android/local.properties | head -n 1)"
    ANDROID_HOME="${ANDROID_HOME//\\:/:}"
    ANDROID_HOME="${ANDROID_HOME//\\//}"
    export ANDROID_HOME
fi
[ -z "$ANDROID_HOME" ] && { echo "ERROR: ANDROID_HOME is not set and android/local.properties has no sdk.dir"; exit 1; }
[ ! -d "$ANDROID_HOME" ] && { echo "ERROR: Android SDK directory does not exist: $ANDROID_HOME"; exit 1; }

echo "[2/5] Preparing mobile AAR..."
if [ -f mobile/mobile.go ]; then
    command -v go >/dev/null || { echo "ERROR: Go source exists but Go was not found"; exit 1; }
    go install golang.org/x/mobile/cmd/gomobile@latest
    go install golang.org/x/mobile/cmd/gobind@latest
    export PATH="$PATH:$(go env GOPATH)/bin"
    gomobile init

    HAS_NDK=""
    if [ -f "$ANDROID_HOME/ndk-bundle/meta/platforms.json" ]; then
        HAS_NDK="1"
    elif [ -d "$ANDROID_HOME/ndk" ]; then
        for ndk_dir in "$ANDROID_HOME"/ndk/*; do
            if [ -f "$ndk_dir/meta/platforms.json" ]; then
                HAS_NDK="1"
                break
            fi
        done
    fi
    if [ -z "$HAS_NDK" ]; then
        echo "ERROR: Android NDK not found under $ANDROID_HOME"
        echo 'Install it in Android Studio SDK Manager or run: sdkmanager "ndk;27.2.12479018"'
        exit 1
    fi

    mkdir -p android/app/libs
    BOOTCLASSPATH="$ANDROID_HOME/platforms/android-35/android.jar"
    gomobile bind -androidapi 24 -bootclasspath "$BOOTCLASSPATH" -target=android -o android/app/libs/mobile.aar RunningByeBye/mobile
    echo "mobile.aar generated from Go source"
else
    if [ ! -f android/app/libs/mobile.aar ]; then
        echo "ERROR: mobile/mobile.go is missing and android/app/libs/mobile.aar was not found"
        exit 1
    fi
    echo "mobile/mobile.go not found; using existing android/app/libs/mobile.aar"
fi

echo "[3/5] Refreshing point assets..."
[ ! -d data/points ] && { echo "ERROR: data/points was not found"; exit 1; }
rm -rf android/app/src/main/assets/points
mkdir -p android/app/src/main/assets/points
cp -r data/points/* android/app/src/main/assets/points/

echo "[4/5] Running Android unit tests..."
cd android
chmod +x gradlew
./gradlew :app:testDebugUnitTest

echo "[5/5] Building release APK..."
./gradlew :app:assembleRelease

echo ""
echo "============================================="
echo "Build succeeded."
echo "APK path: android/app/build/outputs/apk/release/app-release.apk"
echo "============================================="
