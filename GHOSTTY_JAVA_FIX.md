# Ghostty "Unable to locate a Java Runtime" 修复记录

## 报错信息

```
The operation couldn't be completed. Unable to locate a Java Runtime.
Please visit http://www.java.com for information on installing Java.
```

打开 Ghostty 终端时触发。

## 原因

macOS 的 `/usr/libexec/java_home` 只会查找 `/Library/Java/JavaVirtualMachines/` 下的 JDK。

JDK 是通过 Homebrew 安装的（`openjdk@17`），实际路径在：
```
/opt/homebrew/Cellar/openjdk@17/17.0.18/libexec/openjdk.jdk
```

macOS 不会自动扫描 Homebrew 的 Cellar 目录，所以系统认为没有安装 Java。

## 修复步骤

### 1. 创建符号链接

```bash
sudo ln -sfn /opt/homebrew/Cellar/openjdk@17/17.0.18/libexec/openjdk.jdk \
  /Library/Java/JavaVirtualMachines/openjdk.jdk
```

### 2. 注意版本路径

首次修复时使用了 `brew --prefix openjdk`，但实际安装的是 `openjdk@17`（不是 `openjdk`），导致链接指向了不存在的路径。

确认实际安装的包：
```bash
brew list | grep jdk
# 输出: openjdk@17
```

## 后续 JDK 升级

Homebrew 升级 JDK 后小版本号可能变化（如 `17.0.18` → `17.0.19`），需要重新创建链接：

```bash
sudo ln -sfn $(brew --prefix openjdk@17)/libexec/openjdk.jdk \
  /Library/Java/JavaVirtualMachines/openjdk.jdk
```

使用 `brew --prefix openjdk@17` 会自动解析到最新版本路径，无需手动修改版本号。

## 日期

2026-03-27
