#!/usr/bin/env bash
set -euo pipefail

QEMU_REF="${QEMU_REF:-v10.2.0}"

echo "======================================"
echo " ibaut ARM64 QEMU build"
echo "======================================"

rm -rf qemu-src

echo "Cloning QEMU ${QEMU_REF}..."

git clone \
  --depth 1 \
  --branch "$QEMU_REF" \
  https://gitlab.com/qemu-project/qemu.git \
  qemu-src

cd qemu-src

echo "Configuring QEMU..."

./configure \
  --target-list=aarch64-softmmu \
  --disable-werror \
  --enable-pie

echo "Building QEMU..."

ninja -C build qemu-system-aarch64

echo "Installing QEMU..."

mkdir -p ../app/src/main/jniLibs/arm64-v8a

cp \
  build/qemu-system-aarch64 \
  ../app/src/main/jniLibs/arm64-v8a/qemu-system-aarch64

echo "======================================"
echo " QEMU build completed"
echo "======================================"

file ../app/src/main/jniLibs/arm64-v8a/qemu-system-aarch64
ls -lh ../app/src/main/jniLibs/arm64-v8a/qemu-system-aarch64
