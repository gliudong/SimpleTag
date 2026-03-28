# 音乐列表页批量操作交互优化方案

## 背景

SimpleTag 的音乐列表页（SelectorScreen）当前已实现基础的批量选择功能，但在交互效率、功能完整性和视觉反馈方面仍有优化空间。本方案旨在提升用户批量操作音乐文件的体验。

## 当前实现分析

### 核心文件
- `ui/selector/SelectorScreen.kt` - 主屏幕入口
- `ui/selector/ListScreen.kt` - 音乐列表显示
- `ui/selector/SelectorViewModel.kt` - 状态管理
- `ui/selector/components/MultiSelectTopBar.kt` - 批量选择顶部栏
- `ui/selector/components/SimpleMusicItem.kt` - 单个音乐项组件

### 现有功能
| 功能 | 实现方式 |
|-----|---------|
| 触发批量选择 | 长按任意音乐项/专辑项 |
| 选择/取消选择 | 点击切换 |
| 专辑全选 | 长按专辑项 |
| 视觉反馈 | 背景色变化 + 右下角 CheckCircle |
| 批量操作 | 批量编辑、批量自动编辑 |

### 状态管理
```kotlin
// SelectorUiState 关键字段
data class SelectorUiState(
    val multiSelectEnabled: Boolean = false,
    val selectedItems: Set<MusicData> = emptySet(),
    // ...
)
```

---

## 优化方案

### 1. 进入批量选择的多种方式

**现状：** 仅支持长按触发

**优化：**

```
┌─────────────────────────────────────┐
│  🔍 搜索框              [选择] 按钮 │  ← TopAppBar 增加"选择"入口
├─────────────────────────────────────┤
│  📁 文件夹                          │
│  🎵 歌曲              ☑ 全选        │  ← 筛选区域增加全选
├─────────────────────────────────────┤
│  [长按任意项] → 进入选择模式         │  ← 保留现有方式
└─────────────────────────────────────┘
```

**实现要点：**
- TopAppBar 右上角增加"选择"按钮（选中时显示数量角标）
- 长按列表项触发（保留）
- 筛选区域增加"全选当前结果"快捷操作

---

### 2. 选择操作的效率提升

**现状：** 逐个点击选择，专辑长按全选

**新增操作：**

| 操作 | 手势/交互 | 功能 |
|-----|----------|-----|
| 范围选择 | 长按首项 + 滑动到末项 | 选中范围内所有项目 |
| 快速全选 | TopAppBar 全选按钮 | 全选当前可见列表 |
| 反选 | 操作栏按钮 | 已选↔未选 切换 |
| 清空选择 | 操作栏按钮 | 清除所有选择 |

**实现关键文件：**
- `ui/selector/ListScreen.kt` - 添加滑动选择逻辑
- `ui/selector/components/MultiSelectTopBar.kt` - 添加全选/反选/清空按钮
- `ui/selector/SelectorViewModel.kt` - 添加对应状态和方法

---

### 3. 批量操作功能扩展

**现状：** 编辑、批量自动编辑

**扩展后的操作栏：**

```
┌──────────────────────────────────────────────────────┐
│ ← 批量操作 (12)                    清空选择    完成   │
├──────────────────────────────────────────────────────┤
│  [✏️ 编辑]  [🪄 自动编辑]  [🖼️ 封面]  [🗑️ 删除]      │
│  [📋 复制标签] [▶️ 添加到播放列表] [↗️ 分享]          │
└──────────────────────────────────────────────────────┘
```

**新增功能：**
1. **批量删除** - 带二次确认弹窗
2. **批量封面设置** - 统一设置专辑封面
3. **标签复制** - 将某首歌的标签批量应用到其他选中项
4. **导出播放列表** - 生成 M3U 文件

---

### 4. 视觉反馈优化

**现状：** 背景色变化 + 右下角 CheckCircle

**优化：**

```
┌─────────────────────────────────────┐
│  ┌───┐                               │
│  │ ✓ │  Song Title            ✓     │  ← 增加: 左侧勾选框
│  └───┘  Artist Name                 │
│       [选中态: 背景色 + 左侧勾选框] │
└─────────────────────────────────────┘
```

**视觉层级优化：**
1. 左侧固定勾选框（Material Design 标准）
2. TopAppBar 实时显示 "已选 N 首"
3. 专辑部分选中状态（显示 -/12）
4. 触觉反馈（选择时轻微震动，可配置）

**实现关键文件：**
- `ui/selector/components/SimpleMusicItem.kt` - 添加左侧勾选框
- `ui/selector/components/SimpleAlbumItem.kt` - 添加部分选中状态
- `ui/selector/components/MusicThumbnail.kt` - 调整 CheckCircle 位置

---

### 5. 操作流程优化

**问题：** 当前选择列表为空时自动退出，可能误操作

**优化后的退出确认：**

```kotlin
// 退出选择模式时的确认逻辑
if (selectedItems.isNotEmpty()) {
    // 显示确认对话框
    ShowAlertDialog(
        title = "退出选择模式？",
        message = "已选择 ${selectedItems.size} 项，是否放弃？",
        confirmText = "放弃",
        dismissText = "保持选择"
    )
} else {
    // 直接退出
    setMultiSelectEnabled(false)
}
```

---

### 6. 快捷操作手势（可选，P3）

| 手势 | 功能 | 场景 |
|-----|------|-----|
| 长按 + 滑动 | 连续多选 | 快速选择连续歌曲 |
| 双指下滑 | 快速全选 | 一键全选当前页 |
| 双指上滑 | 取消全选 | 快速清空选择 |
| 长按空白处 + 拖框 | 框选模式 | 平板/横屏大屏场景 |

---

## 实现优先级

| 优先级 | 优化项 | 预期收益 | 实现复杂度 |
|-------|-------|---------|-----------|
| **P0** | TopAppBar 增加选择按钮 | 降低发现成本 | 低 |
| **P0** | 选中数量实时显示 | 状态明确 | 低 |
| **P1** | 批量删除功能 | 核心需求补全 | 中 |
| **P1** | 范围选择（滑动选择） | 大幅提升效率 | 中 |
| **P2** | 左侧勾选框设计 | 视觉更标准 | 低 |
| **P2** | 反选/清空快捷操作 | 便捷性提升 | 低 |
| **P3** | 高级手势操作 | 极客场景 | 高 |

---

## 关键文件修改清单

### P0 阶段
1. `ui/selector/components/MultiSelectTopBar.kt`
   - 添加选中数量显示
   - 添加"选择"模式入口按钮

2. `ui/selector/SelectorViewModel.kt`
   - 新增 `selectedCount` 计算属性
   - 新增 `selectAll()`, `clearSelection()`, `invertSelection()` 方法

### P1 阶段
3. `ui/selector/ListScreen.kt`
   - 实现滑动范围选择逻辑
   - 添加退出确认对话框

4. `ui/selector/components/` (新建)
   - `BatchDeleteDialog.kt` - 批量删除确认对话框

### P2 阶段
5. `ui/selector/components/SimpleMusicItem.kt`
   - 重构布局，添加左侧勾选框

6. `ui/selector/components/SimpleAlbumItem.kt`
   - 添加部分选中状态视觉

---

## 验证方式

1. 进入批量选择模式的多种方式均可正常触发
2. 选择/取消选择操作响应流畅（150ms 动画保持）
3. 批量操作功能执行正确，无数据丢失
4. 退出确认在选中非空时正确触发
5. 暗色模式/不同屏幕尺寸下 UI 显示正常
6. TalkBack 无障碍测试通过

---

## 参考资料

- Material Design 3 Selection Guidelines
- Jetpack Compose Animation API
- 现有代码：`app/src/main/java/dev/secam/simpletag/ui/selector/`
