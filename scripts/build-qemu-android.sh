#!/usr/bin/env bash

set -euo pipefail

echo "=========================================="
echo " ibaut S10+ ARM64 QEMU Android Builder"
echo "=========================================="

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

QEMU_DIR="${ROOT_DIR}/.qemu"
BUILD_DIR="${ROOT_DIR}/.qemu-build"

OUTPUT_DIR="${ROOT_DIR}/app/src/main/assets/qemu"

NDK_VERSION="29.0.14206865"

QEMU_REPOSITORY="https://github.com/aarch64-android-emulator/aarch64-qemu.git"

QEMU_REF="master"

echo
echo "[1/8] Checking Android NDK..."

if [ -z "${ANDROID_NDK_HOME:-}" ]; then
    if [ -n "${ANDROID_NDK_ROOT:-}" ]; then
        ANDROID_NDK_HOME="$ANDROID_NDK_ROOT"
    else
        ANDROID_NDK_HOME="${ANDROID_SDK_ROOT}/ndk/${NDK_VERSION}"
    fi
fi

if [ ! -d "$ANDROID_NDK_HOME" ]; then
    echo "ERROR: Android NDK not found:"
    echo "$ANDROID_NDK_HOME"
    exit 1
fi

echo "NDK: $ANDROID_NDK_HOME"

echo
echo "[2/8] Installing build dependencies..."

sudo apt-get update

sudo apt-get install -y \
    git \
    ninja-build \
    cmake \
    meson \
    python3 \
    python3-pip \
    pkg-config \
    libglib2.0-dev \
    libpixman-1-dev \
    libslirp-dev \
    libcap-ng-dev \
    libattr1-dev \
    libaio-dev \
    libusb-1.0-0-dev \
    libpulse-dev \
    libasound2-dev \
    libx11-dev \
    libxext-dev \
    libxrender-dev \
    libxcursor-dev \
    libxi-dev \
    libxrandr-dev \
    libxfixes-dev \
    libxinerama-dev \
    libegl1-mesa-dev \
    libgles2-mesa-dev

echo
echo "[3/8] Downloading Android QEMU source..."

if [ ! -d "$QEMU_DIR/.git" ]; then

    rm -rf "$QEMU_DIR"

    git clone \
        --depth=1 \
        --branch "$QEMU_REF" \
        "$QEMU_REPOSITORY" \
        "$QEMU_DIR"

else

    cd "$QEMU_DIR"

    git fetch --depth=1 origin "$QEMU_REF"

    git reset --hard FETCH_HEAD
fi

echo
echo "[4/8] Preparing build directory..."

rm -rf "$BUILD_DIR"

mkdir -p "$BUILD_DIR"
mkdir -p "$OUTPUT_DIR"

echo
echo "[5/8] Configuring QEMU..."

cd "$QEMU_DIR"

export CC="${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android35-clang"
export CXX="${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android35-clang++"

export AR="${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-ar"
export RANLIB="${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-ranlib"
export STRIP="${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip"

echo "Compiler:"
echo "$CC"

echo
echo "[6/8] Running QEMU configure..."

./configure \
    --target-list=aarch64-softmmu \
    --enable-system \
    --disable-werror \
    --disable-docs \
    --disable-gtk \
    --disable-sdl \
    --disable-vnc \
    --disable-plugins \
    --disable-debug-tcg \
    --disable-tcg-interpreter \
    --enable-fdt \
    --enable-slirp \
    --enable-tools \
    --prefix="$BUILD_DIR/install"

echo
echo "[7/8] Building qemu-system-aarch64..."

ninja -C build \
    qemu-system-aarch64

echo
echo "[8/8] Installing QEMU into Android assets..."

QEMU_BINARY="build/qemu-system-aarch64"

if [ ! -f "$QEMU_BINARY" ]; then

    echo "ERROR: qemu-system-aarch64 was not produced."

    find build -name "qemu-system-aarch64*" -type f || true

    exit 1
fi

rm -rf "$OUTPUT_DIR"

mkdir -p "$OUTPUT_DIR"

cp "$QEMU_BINARY" \
    "$OUTPUT_DIR/qemu-system-aarch64"

chmod 755 \
    "$OUTPUT_DIR/qemu-system-aarch64"

echo
echo "=========================================="
echo " QEMU BUILD COMPLETE"
echo "=========================================="

file "$OUTPUT_DIR/qemu-system-aarch64"

ls -lh "$OUTPUT_DIR/qemu-system-aarch64"

echo
echo "Output:"
echo "$OUTPUT_DIR/qemu-system-aarch64"
