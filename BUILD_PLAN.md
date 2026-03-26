# SimpleTag APK 构建执行计划

## 概述

本计划用于从 SimpleTag Android 项目源码生成 APK 安装包。

**项目信息:**
- 应用名称: SimpleTag (音频标签编辑器)
- 包名: `dev.secam.simpletag`
- 当前版本: `0.3.1-beta` (versionCode: 10)
- 构建系统: Gradle 8.14.3 + Kotlin DSL
- 目标 SDK: 36, 最低 SDK: 26
- JVM 版本: Java 11

**构建变体:**
- **Debug**: `dev.secam.simpletag.debug` - 开发调试版本
- **Release**: `dev.secam.simpletag` - 生产发布版本（带代码混淆）

---

## 环境准备

### 1. 安装 Java JDK

```bash
# 检查 Java 是否已安装
java -version

# 如果未安装，使用 Homebrew 安装 JDK 17
brew install openjdk@17

# 设置 JAVA_HOME 环境变量
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 17)' >> ~/.zshrc
source ~/.zshrc

# 验证安装
java -version
```

**要求:** Java 11 或更高版本（推荐 JDK 17）

### 2. 安装 Android SDK Command-line Tools

**✅ 采用方案：仅安装命令行工具（推荐用于 CI/CD 和服务器环境）**

#### 步骤 2.1: 下载 Command-line Tools

```bash
# 创建下载目录
mkdir -p ~/Downloads/android-tools
cd ~/Downloads/android-tools

# 访问 https://developer.android.com/studio#command-tools
# 下载 macOS 版本的 Command-line Tools
# 文件名类似: commandlinetools-mac-11076708_latest.zip

# 或使用 curl 直接下载（版本号可能变化）
curl https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip -o commandlinetools-mac.zip
```

#### 步骤 2.2: 解压并安装到 SDK 目录

```bash
# 创建 SDK 目录结构
mkdir -p ~/Library/Android/sdk/cmdline-tools/latest

# 解压命令行工具
unzip ~/Downloads/android-tools/commandlinetools-mac.zip -d ~/Library/Android/sdk/cmdline-tools/

# 将解压的内容移动到 latest 目录
mv ~/Library/Android/sdk/cmdline-tools/cmdline-tools/* ~/Library/Android/sdk/cmdline-tools/latest/

# 验证目录结构
ls -la ~/Library/Android/sdk/cmdline-tools/latest/
```

**预期看到的内容:**
```
total 80
drwxr-xr-x  ... .
drwxr-xr-x  ... ..
-rwxr-xr-x  ... bin
-rw-r--r--  ... lib
...
```

#### 步骤 2.3: 设置环境变量

```bash
# 设置 ANDROID_HOME 和 PATH
echo 'export ANDROID_HOME=$HOME/Library/Android/sdk' >> ~/.zshrc
echo 'export ANDROID_SDK_ROOT=$ANDROID_HOME' >> ~/.zshrc
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/36.0.0' >> ~/.zshrc

# 重新加载 shell 配置
source ~/.zshrc

# 验证环境变量
echo $ANDROID_HOME
```

**预期输出:**
```
/Users/liudong320/Library/Android/sdk
```

#### 步骤 2.4: 使用 sdkmanager 安装必需组件

```bash
# 接受许可协议（首次使用时）
sdkmanager --licenses
# 按提示输入 'y' 接受所有许可

# 安装必需的 SDK 组件
sdkmanager "platform-tools"
sdkmanager "platforms;android-36"
sdkmanager "build-tools;36.0.0"

# 验证安装的组件
sdkmanager --list_installed
```

**预期看到已安装的组件:**
```
Installed packages:
  build-tools;36.0.0
  platform-tools
  platforms;android-36
```

#### 步骤 2.5: 验证安装

```bash
# 验证 sdkmanager 可用
sdkmanager --version

# 验证 adb 可用（platform-tools 的一部分）
adb version

# 验证构建工具可用
ls -la $ANDROID_HOME/build-tools/36.0.0/

# 验证平台 SDK 可用
ls -la $ANDROID_HOME/platforms/android-36/
```

**命令行工具包含的构建工具:**
- ✅ `sdkmanager` - SDK 包管理器
- ✅ `avdmanager` - 模拟器管理器（可选）
- ✅ 构建所需的全部工具链

**优势:**
- 📦 安装包小（~100-200 MB vs Android Studio 的 1GB+）
- ⚡ 安装快速，仅需几分钟
- 💾 占用磁盘空间少
- 🔄 完全满足构建 APK 的需求

### 3. 创建 local.properties

```bash
cd /Users/liudong320/workspace/SimpleTag

# 创建 local.properties 文件并指定 SDK 路径
echo "sdk.dir=$ANDROID_HOME" > local.properties

# 验证文件内容
cat local.properties
```

**预期输出:**
```
sdk.dir=/Users/liudong320/Library/Android/sdk
```

---

## 命令行工具方案说明

### 为什么选择命令行工具？

**完全满足构建 APK 的需求:**
- ✅ Gradle 构建系统通过命令行运行
- ✅ 所有编译、打包工具通过命令行调用
- ✅ 无需 GUI 界面即可完成构建
- ✅ 适合自动化构建和 CI/CD

**对比完整 Android Studio:**

| 特性 | 命令行工具 | Android Studio |
|------|-----------|----------------|
| 安装包大小 | ~100-200 MB | ~1 GB+ |
| 磁盘占用 | ~500 MB | ~3-5 GB |
| 安装时间 | 2-5 分钟 | 15-30 分钟 |
| 构建 APK | ✅ 完全支持 | ✅ 完全支持 |
| GUI 设计工具 | ❌ 无 | ✅ 有 |
| 模拟器管理 | ❌ 需额外配置 | ✅ 内置 |
| 性能分析工具 | ❌ 无 | ✅ 有 |
| 适用场景 | 构建部署 | 开发调试 |

### 命令行工具已包含的构建组件

通过 `sdkmanager` 安装后，你将拥有：

```
~/Library/Android/sdk/
├── cmdline-tools/latest/    # 命令行工具
│   ├── bin/
│   │   ├── sdkmanager       # SDK 包管理器
│   │   └── avdmanager       # 模拟器管理器
│   └── lib/
├── platform-tools/          # 平台工具（包含 adb）
│   └── adb                  # Android Debug Bridge
├── build-tools/36.0.0/      # 构建工具
│   ├── aapt                 # Android Asset Packaging Tool
│   ├── aapt2                # AAPT2（新版）
│   ├── zipalign             # ZIP 对齐工具
│   ├── apksigner            # APK 签名工具
│   └── ...                  # 其他构建工具
└── platforms/android-36/    # Android SDK 平台
    └── android.jar          # Android API 库
```

### 首次构建时间估算

- **首次构建**: 5-15 分钟（下载依赖）
- **后续构建**: 1-3 分钟（使用缓存）
- **增量构建**: 10-30 秒（仅构建修改部分）

---

## 构建前检查清单

在开始构建前，请确认以下各项：

- [ ] **Java JDK 已安装**
  ```bash
  java -version  # 应显示 Java 11 或更高版本
  ```

- [ ] **Android SDK 命令行工具已安装**
  ```bash
  echo $ANDROID_HOME  # 应显示: /Users/liudong320/Library/Android/sdk
  sdkmanager --version  # 应显示版本号
  ```

- [ ] **必需的 SDK 组件已安装**
  ```bash
  sdkmanager --list_installed | grep -E "platform-tools|android-36|build-tools;36"
  ```

- [ ] **local.properties 文件已创建**
  ```bash
  cat local.properties  # 应包含: sdk.dir=/Users/liudong320/Library/Android/sdk
  ```

- [ ] **Gradle wrapper 可执行**
  ```bash
  ls -la gradlew  # 应显示有执行权限 (rwxr-xr-x)
  ```

- [ ] **磁盘空间充足**（至少 2 GB 可用空间）

- [ ] **网络连接正常**（首次构建需下载依赖）

---

## 构建 Debug APK

### 步骤 1: 清理之前的构建（可选）

```bash
cd /Users/liudong320/workspace/SimpleTag
./gradlew clean
```

### 步骤 2: 构建 Debug APK

```bash
# 构建 debug 版本
./gradlew assembleDebug

# 或查看详细输出
./gradlew assembleDebug --info
```

**预期结果:**
- Gradle 下载依赖（首次构建时）
- KSP 注解处理
- Kotlin 编译
- 资源合并和处理
- DEX 文件生成
- APK 打包

**成功标志:**
```
BUILD SUCCESSFUL in [时间]
```

### 步骤 3: 查找生成的 APK

```bash
# Debug APK 位置
ls -lh app/build/outputs/apk/debug/SimpleTag_0.3.1-beta-debug.apk

# 或查找所有 APK
find app/build/outputs/apk -name "*.apk" -type f
```

**输出文件:**
- **路径**: `app/build/outputs/apk/debug/SimpleTag_0.3.1-beta-debug.apk`
- **包名**: `dev.secam.simpletag.debug`
- **版本**: `0.3.1-beta-debug`
- **签名**: Debug 签名（自动生成）
- **大小**: 约 5-15 MB

---

## 构建 Release APK

### 步骤 1: 生成签名密钥库

**选项 A: 生成测试密钥库（仅用于测试）**

```bash
keytool -genkey -v -keystore app/debug.keystore -alias androiddebugkey \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass android -keypass android \
  -dname "CN=Android Debug,O=Android,C=US"
```

**选项 B: 生成发布密钥库（用于生产）**

```bash
keytool -genkey -v -keystore app/release.keystore -alias simpletag \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass YOUR_STORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD \
  -dname "CN=Sergio Camacho,O=SimpleTag,C=US"

# 重要：安全存储这些密码！
# 永远不要将密钥库文件提交到版本控制
```

### 步骤 2: 配置签名

需要在 `app/build.gradle.kts` 中添加签名配置。由于这是只读计划，以下是需要添加的配置：

```kotlin
// 在 android { } 块内添加，位于 buildTypes 之前
signingConfigs {
    create("release") {
        storeFile = file("release.keystore")
        storePassword = "YOUR_STORE_PASSWORD"
        keyAlias = "simpletag"
        keyPassword = "YOUR_KEY_PASSWORD"
    }
}

// 修改 release buildType 以使用签名
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
        signingConfig = signingConfigs.getByName("release")  // 添加此行
    }
    debug {
        // ... 现有 debug 配置
    }
}
```

**推荐方式：使用环境变量**

```kotlin
signingConfigs {
    create("release") {
        storeFile = file(System.getenv("KEYSTORE_FILE") ?: "release.keystore")
        storePassword = System.getenv("KEYSTORE_STORE_PASSWORD")
        keyAlias = System.getenv("KEYSTORE_KEY_ALIAS") ?: "simpletag"
        keyPassword = System.getenv("KEYSTORE_KEY_PASSWORD")
    }
}
```

构建前设置环境变量：
```bash
export KEYSTORE_FILE=app/release.keystore
export KEYSTORE_STORE_PASSWORD=your_store_password
export KEYSTORE_KEY_ALIAS=simpletag
export KEYSTORE_KEY_PASSWORD=your_key_password
```

### 步骤 3: 构建 Release APK

```bash
cd /Users/liudong320/workspace/SimpleTag

# 构建 release APK
./gradlew assembleRelease

# 或查看详细输出
./gradlew assembleRelease --info
```

**预期过程:**
- 所有 debug 构建的步骤，加上：
- ProGuard/R8 代码混淆和优化
- 资源压缩
- APK 签名
- ZIP 对齐

### 步骤 4: 查找生成的 APK

```bash
# Release APK 位置
ls -lh app/build/outputs/apk/release/SimpleTag_0.3.1-beta.apk
```

**输出文件:**
- **路径**: `app/build/outputs/apk/release/SimpleTag_0.3.1-beta.apk`
- **包名**: `dev.secam.simpletag`
- **版本**: `0.3.1-beta`
- **签名**: 你的发布签名
- **混淆**: 是（ProGuard/R8）
- **大小**: 约 3-8 MB（更小，因为混淆）

---

## 验证 APK

### 1. 验证 APK 完整性

```bash
# 检查 APK 文件
ls -lh app/build/outputs/apk/debug/SimpleTag_0.3.1-beta-debug.apk

# 验证 APK 是有效的 ZIP 文件
unzip -l app/build/outputs/apk/debug/SimpleTag_0.3.1-beta-debug.apk | head -20
```

### 2. 验证 APK 签名（仅 Release）

```bash
# 使用 apksigner 验证
$ANDROID_HOME/build-tools/36.0.0/apksigner verify \
  --print-certs app/build/outputs/apk/release/SimpleTag_0.3.1-beta.apk
```

### 3. 检查 APK 包信息

```bash
# 使用 aapt 获取包信息
$ANDROID_HOME/build-tools/36.0.0/aapt dump badging \
  app/build/outputs/apk/debug/SimpleTag_0.3.1-beta-debug.apk
```

**预期输出包括:**
```
package: name='dev.secam.simpletag.debug' versionCode='10' versionName='0.3.1-beta-debug'
sdkVersion:'26' targetSdkVersion:'36'
```

### 4. 安装测试（可选）

```bash
# 安装到连接的设备/模拟器
adb install app/build/outputs/apk/debug/SimpleTag_0.3.1-beta-debug.apk

# 验证安装
adb shell pm list packages | grep simpletag
```

---

## 常用命令快速参考

```bash
# 构建 Debug APK（最快）
./gradlew assembleDebug

# 构建 Release APK（需要配置签名）
./gradlew assembleRelease

# 清理并重新构建
./gradlew clean assembleDebug

# 构建所有变体
./gradlew assemble

# 直接安装到设备
./gradlew installDebug

# 详细输出构建
./gradlew assembleDebug --stacktrace --info
```

---

## 关键文件

1. **`app/build.gradle.kts`** - 构建配置文件，定义构建变体、签名、版本信息和 APK 输出命名

2. **`local.properties`** - 必须创建，指向 Android SDK 位置

3. **`gradle/wrapper/gradle-wrapper.properties`** - 定义 Gradle 版本（8.14.3）

4. **`gradle.properties`** - JVM 参数和构建设置

5. **`app/proguard-rules.pro`** - ProGuard 配置，用于 Release 版本代码混淆

---

## 故障排除

### 问题: "SDK location not found"

```bash
# 创建 local.properties 文件
echo "sdk.dir=$ANDROID_HOME" > local.properties
```

### 问题: "Java Runtime not found"

```bash
# 安装 JDK
brew install openjdk@17

# 设置 JAVA_HOME
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

### 问题: "Permission denied" for gradlew

```bash
chmod +x gradlew
```

### 问题: "Out of memory" during build

```bash
# 在 gradle.properties 中增加 Gradle 堆大小
echo "org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8" >> gradle.properties
```

---

## 输出位置总结

### Debug APK
- **路径**: `app/build/outputs/apk/debug/SimpleTag_0.3.1-beta-debug.apk`
- **包名**: `dev.secam.simpletag.debug`
- **版本**: `0.3.1-beta-debug`

### Release APK
- **路径**: `app/build/outputs/apk/release/SimpleTag_0.3.1-beta.apk`
- **包名**: `dev.secam.simpletag`
- **版本**: `0.3.1-beta`
