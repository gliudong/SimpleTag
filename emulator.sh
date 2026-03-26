#!/bin/bash

# SimpleTag 模拟器管理脚本
# 用法：./emulator.sh {start|stop|status|restart}

ANDROID_SDK="$HOME/Library/Android/sdk"
AVD_NAME="SimpleTag_Emulator"

start_emulator() {
    echo "🚀 Starting emulator..."
    $ANDROID_SDK/emulator/emulator \
        -avd $AVD_NAME \
        -gpu host \
        -no-snapshot \
        -no-audio &
    echo "⏳ Waiting for emulator to be ready..."
    $ANDROID_SDK/platform-tools/adb wait-for-device
    echo "✅ Emulator is ready!"
}

stop_emulator() {
    echo "🛑 Stopping emulator..."
    $ANDROID_SDK/platform-tools/adb shell reboot -p || echo "Emulator already stopped"
}

status_emulator() {
    echo "📊 Emulator status:"
    $ANDROID_SDK/platform-tools/adb devices
}

case "$1" in
    start)
        start_emulator
        ;;
    stop)
        stop_emulator
        ;;
    status)
        status_emulator
        ;;
    restart)
        stop_emulator
        sleep 2
        start_emulator
        ;;
    *)
        echo "Usage: $0 {start|stop|status|restart}"
        exit 1
        ;;
esac
