#!/bin/bash

set -e

# 配置
ADB="$HOME/Library/Android/sdk/platform-tools/adb"
MUSIC_DIR="/sdcard/Music/TestMusic"
TEMP_DIR="./temp_music"

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== 向模拟器添加测试音乐文件 ===${NC}"

# 1. 检查模拟器连接
echo -e "\n${YELLOW}1. 检查模拟器连接状态...${NC}"
if ! $ADB devices | grep -q "device$"; then
    echo -e "${RED}错误: 没有检测到连接的设备/模拟器${NC}"
    echo "请确保模拟器正在运行"
    exit 1
fi
echo -e "${GREEN}✓ 模拟器已连接${NC}"

# 2. 检查临时目录
echo -e "\n${YELLOW}2. 检查本地缓存...${NC}"
mkdir -p "$TEMP_DIR"
cd "$TEMP_DIR"

DOWNLOADED_COUNT=$(ls -1 *.mp3 *.flac *.ogg 2>/dev/null | wc -l)
if [ "$DOWNLOADED_COUNT" -gt 0 ]; then
    echo -e "${GREEN}✓ 本地已有 $DOWNLOADED_COUNT 个音频文件，跳过下载${NC}"
else
    # 3. 下载测试音乐文件
    echo -e "\n${YELLOW}3. 下载测试音乐文件...${NC}"
    echo "  下载测试音频文件 (来自免费音频源)..."

    # 文件1: Happy Whistle
    echo "  下载 happy_whistle.mp3..."
    curl -L -o "happy_whistle.mp3" "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Simon_Panrucker/Happy_TV_Songs/Simon_Panrucker_-_01_-_Whistle.mp3" --max-time 60 2>/dev/null && echo -e "    ${GREEN}✓ 下载成功${NC}" || echo -e "    ${RED}✗ 下载失败${NC}"

    # 文件2: Sentinel
    echo "  下载 sentinel.mp3..."
    curl -L -o "sentinel.mp3" "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/ccCommunity/Kai_Engel/Satin/Kai_Engel_-_04_-_Sentinel.mp3" --max-time 60 2>/dev/null && echo -e "    ${GREEN}✓ 下载成功${NC}" || echo -e "    ${RED}✗ 下载失败${NC}"

    # 文件3: 尝试从其他源下载
    echo "  下载 test_audio.mp3..."
    curl -L -o "test_audio.mp3" "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3" --max-time 60 2>/dev/null && echo -e "    ${GREEN}✓ 下载成功${NC}" || echo -e "    ${RED}✗ 下载失败${NC}"

    # 检查是否有文件下载成功
    DOWNLOADED_COUNT=$(ls -1 *.mp3 *.flac *.ogg 2>/dev/null | wc -l)
    if [ "$DOWNLOADED_COUNT" -eq 0 ]; then
        echo -e "\n${RED}错误: 没有成功下载任何音频文件${NC}"
        cd ..
        rm -rf "$TEMP_DIR"
        exit 1
    fi

    echo -e "${GREEN}✓ 成功下载 $DOWNLOADED_COUNT 个音频文件${NC}"
fi

# 4. 在模拟器中创建目录
echo -e "\n${YELLOW}4. 在模拟器中创建音乐目录...${NC}"
$ADB shell "mkdir -p $MUSIC_DIR"
echo -e "${GREEN}✓ 目录已创建: $MUSIC_DIR${NC}"

# 5. 推送文件到模拟器
echo -e "\n${YELLOW}5. 推送音频文件到模拟器...${NC}"
PUSHED_COUNT=0
for file in *.mp3 *.flac *.ogg *.wav *.m4a; do
    if [ -f "$file" ]; then
        echo "  推送 $file..."
        if $ADB push "$file" "$MUSIC_DIR/" > /dev/null 2>&1; then
            echo -e "    ${GREEN}✓ 成功${NC}"
            PUSHED_COUNT=$((PUSHED_COUNT + 1))
        else
            echo -e "    ${RED}✗ 失败${NC}"
        fi
    fi
done 2>/dev/null
echo -e "${GREEN}✓ 成功推送 $PUSHED_COUNT 个文件${NC}"

# 6. 返回上级目录
cd ..

# 7. 触发媒体扫描
echo -e "\n${YELLOW}6. 触发 MediaStore 扫描...${NC}"
$ADB shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d "file://$MUSIC_DIR" > /dev/null 2>&1
$ADB shell am broadcast -a android.intent.action.MEDIA_MOUNTED -d "file://$MUSIC_DIR" > /dev/null 2>&1
echo -e "${GREEN}✓ 媒体扫描已触发${NC}"

# 8. 等待 MediaStore 索引
echo -e "\n${YELLOW}7. 等待 MediaStore 索引完成...${NC}"
sleep 3

# 9. 验证文件
echo -e "\n${YELLOW}8. 验证文件已添加...${NC}"
MUSIC_COUNT=$($ADB shell content query --uri content://media/external/audio/media | grep -c "row=" || echo "0")
echo -e "${GREEN}✓ MediaStore 中现在有 $MUSIC_COUNT 个音频文件${NC}"

# 10. 显示文件列表
echo -e "\n${YELLOW}模拟器中的音乐文件:${NC}"
$ADB shell ls -lh "$MUSIC_DIR/" 2>/dev/null || echo "  (无法列出文件)"

# 11. 保留临时文件供下次复用
echo -e "\n${YELLOW}9. 本地缓存保留在 $TEMP_DIR/ 供下次复用${NC}"

# 完成
echo -e "\n${GREEN}=== 完成! ===${NC}"
echo -e "${GREEN}现在可以在 SimpleTag app 中看到并编辑音乐文件了${NC}"
echo ""
echo "提示: 如果在 SimpleTag 中还看不到文件，可以尝试:"
echo "  1. 重启 SimpleTag app"
echo "  2. 检查 app 的文件夹过滤设置"
echo "  3. 等待几秒让 MediaStore 完成索引"
