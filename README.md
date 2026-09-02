# ibaut S10+ Emulator

A GitHub-buildable Android project for a real ARM64 virtual-machine frontend.

## Current stage
- Android application shell
- ISO document picker
- VM profile
- persistent VM directory
- native bridge
- GitHub Actions build
- QEMU integration hook

## Important
This repository does not include Samsung proprietary firmware or a guest OS. It is intended to boot compatible guest images once the QEMU runtime is supplied.

The current APK shell does not yet contain the full QEMU binary; the workflow is deliberately prepared to build/package the native engine in the next stage.

## Build
Use GitHub Actions with `.github/workflows/build-apk.yml`.

NDK is pinned to 29.0.14206865.
