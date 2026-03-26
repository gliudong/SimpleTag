#!/bin/bash

# SimpleTag 实时预览环境一键设置脚本
# 用法：./setup-live-preview.sh

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

ANDROID_SDK="$HOME/Library/Android/sdk"

echo -e "${BLUE}🔧 SimpleTag Live Preview Setup${NC}"
echo "======================================"

# 检查 Android SDK
if [ ! -d "$ANDROID_SDK" ]; then
    echo -e "${YELLOW}⚠️  Android SDK not found at $ANDROID_SDK${NC}"
    echo "Please install Android SDK first"
    exit 1
fi

echo -e "${GREEN}✅ Android SDK found${NC}"

# 安装 emulator
echo -e "${BLUE}📦 Installing emulator...${NC}"
$ANDROID_SDK/cmdline-tools/latest/bin/sdkmanager --install "emulator"

# 安装系统镜像（选择 Android 34）
echo -e "${BLUE}📦 Installing system image (Android 34)...${NC}"
$ANDROID_SDK/cmdline-tools/latest/bin/sdkmanager --install "system-images;android-34;google_apis;x86_64" || {
    echo -e "${YELLOW}⚠️  Android 34 not available, trying Android 31...${NC}"
    $ANDROID_SDK/cmdline-tools/latest/bin/sdkmanager --install "system-images;android-31;google_apis;x86_64"
    IMAGE_VERSION="31"
}

# 创建 AVD
echo -e "${BLUE}📱 Creating AVD...${NC}"
$ANDROID_SDK/cmdline-tools/latest/bin/avdmanager create avd \
    -n SimpleTag_Emulator \
    -k "system-images;android-${IMAGE_VERSION:-34};google_apis;x86_64" \
    -d 1 \
    -f

# 安装文件监听工具
echo -e "${BLUE}📦 Installing file watcher...${NC}"
if ! command -v fswatch &> /dev/null; then
    brew install fswatch
else
    echo -e "${GREEN}✅ fswatch already installed${NC}"
fi

# 赋予脚本执行权限
echo -e "${BLUE}🔑 Setting script permissions...${NC}"
chmod +x emulator.sh
chmod +x watch-and-deploy.sh
chmod +x quick-deploy.sh

echo ""
echo -e "${GREEN}✅ Setup complete!${NC}"
echo ""
echo "Next steps:"
echo "  1. Start emulator: ./emulator.sh start"
echo "  2. Wait for boot (30-60s)"
echo "  3. Start watching: ./watch-and-deploy.sh"
echo ""
