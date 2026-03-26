.PHONY: install deploy clean watch log help

# 默认目标
help:
	@echo "SimpleTag 实时预览快捷命令"
	@echo ""
	@echo "可用命令:"
	@echo "  make install   - 安装到设备"
	@echo "  make deploy    - 快速部署"
	@echo "  make watch     - 启动实时监听"
	@echo "  make clean     - 清理构建"
	@echo "  make log       - 查看应用日志"
	@echo ""

# 安装到设备
install:
	./gradlew installDebug

# 快速部署
deploy:
	./quick-deploy.sh

# 实时监听
watch:
	./watch-and-deploy.sh

# 清理构建
clean:
	./gradlew clean

# 查看日志
log:
	adb logcat -s SimpleTag:* AndroidRuntime:E
