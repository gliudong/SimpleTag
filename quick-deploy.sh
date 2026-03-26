#!/bin/bash

# 快速部署脚本（手动触发）
# 用法：./quick-deploy.sh

ANDROID_SDK="$HOME/Library/Android/sdk"
ADB="$ANDROID_SDK/platform-tools/adb"
PACKAGE_NAME="dev.secam.simpletag.debug"
ACTIVITY="dev.secam.simpletag.MainActivity"

echo "🔨 Building and deploying..."

./gradlew assembleDebug &&
$ADB install -r app/build/outputs/apk/debug/SimpleTag_*.apk &&
$ADB shell am start -n "$PACKAGE_NAME/.$ACTIVITY"

echo "✅ Done!"
