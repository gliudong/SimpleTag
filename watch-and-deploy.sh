#!/bin/bash

# SimpleTag 实时预览脚本
# 监听代码变化并自动部署到设备

set -e

# 颜色输出
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Android SDK 路径
ANDROID_SDK="$HOME/Library/Android/sdk"
ADB="$ANDROID_SDK/platform-tools/adb"

# 应用信息
PACKAGE_NAME="dev.secam.simpletag.debug"
ACTIVITY="dev.secam.simpletag.MainActivity"

echo -e "${BLUE}🚀 SimpleTag Live Preview${NC}"
echo "======================================"

# 检查设备连接
check_device() {
    if ! $ADB devices | grep -q "device$"; then
        echo -e "${RED}❌ No Android device found!${NC}"
        echo "Please connect a device or start an emulator:"
        echo "  $ANDROID_SDK/emulator/emulator -avd <avd_name>"
        echo "  or use: ./emulator.sh start"
        exit 1
    fi
    echo -e "${GREEN}✅ Device connected${NC}"
}

# 首次安装
initial_install() {
    echo -e "${BLUE}📦 Installing initial build...${NC}"
    ./gradlew installDebug
    $ADB shell am start -n "$PACKAGE_NAME/.$ACTIVITY"
    echo -e "${GREEN}✅ Initial install complete${NC}"
}

# 监听并构建
watch_and_build() {
    echo -e "${BLUE}👀 Watching for changes...${NC}"
    echo "Press Ctrl+C to stop"
    echo ""

    # 使用 fswatch 监听文件变化
    # 如果没有安装，使用 poll 模式
    if command -v fswatch &> /dev/null; then
        fswatch -o app/src/main -r | while read; do
            build_and_deploy
        done
    elif command -v entr &> /dev/null; then
        find app/src/main -name "*.kt" | entr -rd sh -c 'build_and_deploy'
    else
        echo -e "${RED}❌ fswatch or entr not found!${NC}"
        echo "Install with: brew install fswatch"
        echo "Or: brew install entr"
        exit 1
    fi
}

# 构建并部署
build_and_deploy() {
    echo -e "\n${BLUE}🔄 Change detected, rebuilding...${NC}"
    START_TIME=$(date +%s)

    # 增量构建
    if ./gradlew assembleDebug --build-cache; then
        # 安装到设备
        APK_PATH="app/build/outputs/apk/debug/SimpleTag_*.apk"
        if $ADB install -r $APK_PATH 2>/dev/null; then
            # 启动应用
            $ADB shell am start -n "$PACKAGE_NAME/.$ACTIVITY" >/dev/null 2>&1

            END_TIME=$(date +%s)
            ELAPSED=$((END_TIME - START_TIME))
            echo -e "${GREEN}✅ Deployed in ${ELAPSED}s${NC}"
        else
            echo -e "${RED}❌ Install failed${NC}"
        fi
    else
        echo -e "${RED}❌ Build failed${NC}"
    fi

    echo -e "${BLUE}⏳ Waiting for changes...${NC}"
}

# 主流程
check_device
initial_install
watch_and_build
