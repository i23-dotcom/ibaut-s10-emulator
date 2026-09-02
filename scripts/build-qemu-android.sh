#!/usr/bin/env bash
set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORK="$ROOT/.android-qemu"
OUT="$ROOT/app/src/main/assets/qemu"

echo "=== ibaut ARM64 QEMU build ==="

mkdir -p "$WORK"
mkdir -p "$OUT"

cd "$WORK"

if [ ! -d "qemu" ]; then
    git clone --depth 1 \
      https://github.com/aarch64-android-emulator/aarch64-qemu.git \
      qemu
fi

cd qemu

echo "Preparing Android QEMU..."

if [ -f "./android/rebuild.sh" ]; then
    chmod +x ./android/rebuild.sh

    ./android/rebuild.sh \
        --out-dir="$WORK/objs"
else
    echo "ERROR: Android QEMU rebuild.sh was not found."
    exit 1
fi

echo "Searching for qemu-system-aarch64..."

QEMU="$(find "$WORK/objs" \
    -type f \
    -name 'qemu-system-aarch64' \
    | head -n 1)"

if [ -z "$QEMU" ]; then
    echo "ERROR: qemu-system-aarch64 was not built."
    find "$WORK/objs" -type f -name 'qemu-system-*' || true
    exit 1
fi

echo "Found:"
echo "$QEMU"

rm -f "$OUT/qemu-system-aarch64"

cp "$QEMU" "$OUT/qemu-system-aarch64"

chmod 755 "$OUT/qemu-system-aarch64"

echo
echo "QEMU installed into:"
echo "$OUT/qemu-system-aarch64"

ls -lh "$OUT/qemu-system-aarch64"
