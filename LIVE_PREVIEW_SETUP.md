# SimpleTag 实时预览开发环境设置指南

本文档介绍如何配置 SimpleTag 项目的实时预览功能，实现代码修改后自动构建并部署到设备/模拟器。

## 目标

- 🚀 代码修改后 **10-20 秒**内在设备上看到效果
- 🔄 全自动构建→安装→启动流程
- 💰 零成本，使用现有 Gradle 环境
- ⚡ 增量编译，第二次构建更快

## 快速开始

### 系统要求

- macOS / Linux / Windows
- Android SDK 已安装
- **Apple Silicon Mac (M1/M2/M3)** - 自动使用 ARM64 系统镜像
- **Intel Mac** - 自动使用 x86_64 系统镜像

### 一键设置（推荐）

```bash
# 1. 运行一键设置脚本
chmod +x setup-live-preview.sh
./setup-live-preview.sh

# 2. 启动模拟器
./emulator.sh start

# 3. 等待模拟器启动完成（30-60秒）
./emulator.sh status

# 4. 启动实时预览
./watch-and-deploy.sh
```

### 手动设置

如果一键设置脚本不可用，请按以下步骤手动配置：

#### 步骤 1：优化 Gradle 配置

编辑 `gradle.properties`，添加以下内容：

```properties
# 启用 Gradle 缓存和并行构建
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.configureondemand=true

# Kotlin 增量编译
kotlin.incremental=true
kotlin.compiler.execution.strategy=in-process

# 增加 JVM 内存
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8 -XX:+UseParallelGC

# 启用配置缓存（Android Gradle Plugin 8.0+）
org.gradle.configuration-cache=true
```

#### 步骤 2：安装文件监听工具

```bash
brew install fswatch
# 或
brew install entr
```

#### 步骤 3：赋予脚本执行权限

```bash
chmod +x emulator.sh
chmod +x watch-and-deploy.sh
chmod +x quick-deploy.sh
```

## 使用方式

### 方式 1：使用模拟器（推荐）

```bash
# 启动模拟器
./emulator.sh start

# 启动实时预览
./watch-and-deploy.sh

# 修改代码，自动构建部署
# 按 Ctrl+C 停止监听

# 停止模拟器
./emulator.sh stop
```

### 方式 2：使用真机

```bash
# 1. 用 USB 连接手机，启用 USB 调试
# 2. 手机上授权计算机调试权限

# 验证连接
adb devices

# 启动实时预览
./watch-and-deploy.sh
```

### 方式 3：手动快速部署

```bash
./quick-deploy.sh
# 或
make deploy
```

## 脚本说明

### emulator.sh - 模拟器管理

```bash
./emulator.sh start    # 启动模拟器
./emulator.sh stop     # 停止模拟器
./emulator.sh status   # 查看状态
./emulator.sh restart  # 重启模拟器
```

### watch-and-deploy.sh - 实时监听

监听代码变化并自动部署到设备。

**特性：**
- 自动检测设备连接
- 增量构建
- 彩色输出
- 构建时间统计

### quick-deploy.sh - 快速部署

手动触发单次构建和部署。

## Makefile 快捷命令（可选）

```bash
make watch   # 启动实时预览
make deploy  # 手动快速部署
make install # 安装到设备
make clean   # 清理构建
make log     # 查看应用日志
```

## 查看日志

```bash
# 终端 1：运行监听脚本
./watch-and-deploy.sh

# 终端 2：查看日志
adb logcat -s SimpleTag:* AndroidRuntime:E
```

## 验证测试

### 模拟器环境

1. ✅ 运行一键设置脚本：`./setup-live-preview.sh`
2. ✅ 启动模拟器：`./emulator.sh start`
3. ✅ 检查模拟器状态：`./emulator.sh status`（应显示 emulator-5554）
4. ✅ 运行 `./watch-and-deploy.sh`
5. ✅ 修改任意 Kotlin 文件并保存
6. ✅ 10-30 秒内模拟器自动显示更新
7. ✅ 查看日志确认无错误

### 真机环境

1. ✅ 用 USB 连接手机，启用 USB 调试
2. ✅ 手机上授权计算机调试权限
3. ✅ 验证连接：`adb devices`
4. ✅ 运行 `./watch-and-deploy.sh`
5. ✅ 修改代码并保存
6. ✅ 10-30 秒内手机自动显示更新

## 故障排查

### Apple Silicon Mac (M1/M2/M3) 架构问题

如果您遇到以下错误：
```
FATAL | Avd's CPU Architecture 'x86_64' is not supported by the QEMU2 emulator on aarch64 host
```

**解决方案：**
```bash
# 删除旧的 x86_64 AVD
$HOME/Library/Android/sdk/cmdline-tools/latest/bin/avdmanager delete avd -n SimpleTag_Emulator

# 重新运行设置脚本（会自动检测架构）
./setup-live-preview.sh
```

设置脚本会自动检测您的 Mac 架构并安装对应的系统镜像：
- **Apple Silicon**: 使用 `arm64-v8a` 镜像
- **Intel Mac**: 使用 `x86_64` 镜像

### 模拟器启动失败

```bash
# 检查系统镜像
$HOME/Library/Android/sdk/cmdline-tools/latest/bin/sdkmanager --list | grep system-images

# 重新创建 AVD
$HOME/Library/Android/sdk/cmdline-tools/latest/bin/avdmanager delete avd -n SimpleTag_Emulator
./emulator.sh start
```

### adb 找不到设备

```bash
# 重启 adb
$HOME/Library/Android/sdk/platform-tools/adb kill-server
$HOME/Library/Android/sdk/platform-tools/adb start-server
$HOME/Library/Android/sdk/platform-tools/adb devices
```

### 构建失败

```bash
./gradlew clean
./gradlew assembleDebug --stacktrace
```

### 安装失败

```bash
adb uninstall dev.secam.simpletag.debug
./gradlew installDebug
```

### 文件监听不工作

```bash
# 检查是否安装了 fswatch 或 entr
brew list fswatch
brew list entr
```

## 性能优化

1. **首次构建**：运行 `./gradlew build` 建立缓存
2. **增量编译**：Kotlin 编译器默认启用增量编译
3. **构建缓存**：启用 Gradle 构建缓存加速后续构建
4. **SSD 存储**：确保项目在 SSD 上
5. **关闭资源压缩**：Debug 构建已禁用混淆和压缩

## 配置 adb 环境变量（可选）

在 `~/.zshrc` 或 `~/.bash_profile` 添加：

```bash
# Android SDK
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
```

然后重新加载配置：
```bash
source ~/.zshrc
```

## 预期效果

- 🚀 代码修改后 **10-20 秒**内在设备上看到效果
- 🔄 全自动构建→安装→启动流程
- 💰 零成本，使用现有 Gradle 环境
- ⚡ 增量编译，第二次构建更快

## 相关文件

- `gradle.properties` - Gradle 构建优化配置
- `emulator.sh` - 模拟器管理脚本
- `watch-and-deploy.sh` - 实时监听和自动部署脚本
- `quick-deploy.sh` - 手动快速部署脚本
- `setup-live-preview.sh` - 一键设置脚本
- `Makefile` - 命令快捷方式（可选）
