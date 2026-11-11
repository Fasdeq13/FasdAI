#!/usr/bin/env bash
set -e

mode="${1:-debug}"

if [ -z "$ANDROID_HOME" ]; then
  echo "ANDROID_HOME не задан, запусти через nix develop"
  exit 1
fi

case "$mode" in
  debug)
    gradle :app:assembleDebug
    apk=$(find app/build/outputs/apk/debug -name "*.apk" | head -n1)
    ;;
  release)
    gradle :app:assembleRelease
    apk=$(find app/build/outputs/apk/release -name "*.apk" | head -n1)
    ;;
  clean)
    gradle clean
    exit 0
    ;;
  *)
    echo "Неизвестный режим: $mode (debug | release | clean)"
    exit 1
    ;;
esac

if [ -z "$apk" ]; then
  echo "Сборка прошла, но apk не найден"
  exit 1
fi

mkdir -p out
cp "$apk" out/
echo "Готово: out/$(basename "$apk")"
