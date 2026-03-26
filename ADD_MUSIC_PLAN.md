# 向模拟器添加测试音乐文件

## Context
用户已经通过 `watch-and-deploy.sh` 在模拟器中启动了 SimpleTag app，但模拟器中没有音乐文件，无法进行编辑测试。需要向模拟器添加测试音频文件。

## Current State
- 模拟器已连接 (emulator-5554)
- `/sdcard/Music/` 目录为空（只有 .thumbnails 文件夹）
- MediaStore 中没有音频文件记录
- SimpleTag app 通过 MediaStore 加载音乐文件

## Implementation Plan

### 方案：创建音乐文件推送脚本

创建便捷脚本 `add-music-to-emulator.sh`，从网络下载免费的测试音乐文件并推送到模拟器。

#### 脚本功能
1. **下载示例音乐文件**：从公开来源下载免费的测试音频文件
   - 使用标准的音频测试样例文件
   - 下载多种格式（MP3, FLAC, OGG）以测试不同格式
   - 文件大小控制在 1-3MB 以加快传输

2. **推送到模拟器**：使用 `adb push` 将文件复制到模拟器
   - 目标目录：`/sdcard/Music/`
   - 创建子目录结构（TestMusic/）便于测试

3. **触发 MediaStore 扫描**：确保新文件被系统索引
   - 使用 `adb shell am broadcast` 发送媒体扫描广播
   - 验证文件是否被正确索引

#### 脚本实现
脚本将：
- 检查模拟器连接状态
- 创建临时下载目录
- 下载 3-5 个不同格式的测试音频文件
- 推送到模拟器的 `/sdcard/Music/TestMusic/` 目录
- 触发媒体扫描
- 验证文件已添加
- 清理临时文件

### 验证步骤

1. **检查文件是否成功推送**
   ```bash
   adb shell ls -la /sdcard/Music/
   ```

2. **检查 MediaStore 索引**
   ```bash
   adb shell content query --uri content://media/external/audio/media
   ```

3. **在 SimpleTag app 中验证**
   - 打开 app
   - 应该能看到音乐列表
   - 可以浏览、选择和编辑标签

4. **测试编辑功能**
   - 选择一个音乐文件
   - 编辑标签（标题、艺术家、专辑等）
   - 保存并验证修改

## Critical Files

- **新建脚本**: `/Users/liudong320/workspace/SimpleTag/add-music-to-emulator.sh`
- **相关脚本**: `emulator.sh`, `watch-and-deploy.sh`
- **音乐加载逻辑**: `app/src/main/java/dev/secam/simpletag/data/media/MediaRepo.kt`

## Notes

- 推荐使用小的测试文件（1-3MB）以加快传输速度
- 支持的格式：MP3, FLAC, OGG, Opus, WAV, M4A 等
- 可以创建多个子文件夹测试文件夹过滤功能
- 确保 MediaStore 正确索引文件是关键步骤
