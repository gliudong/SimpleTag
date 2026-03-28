# PRD: 基于文件名的 Auto Edit 功能

## Context

当前 Auto Edit 功能从音频文件的**已有标签元数据**（title、artist、album）中提取关键词去 MusicBrainz 查询。但对于未标记的音乐文件（tag 全空），这个功能完全无法使用。

本需求的目标是：**仅通过音乐文件的文件名**，解析出艺术家和标题等信息，去 MusicBrainz 查询匹配到完整的元数据（标题、艺术家、专辑、年份、曲目号、封面等），并填充到编辑器中。

## 功能范围

### Phase 1（本次实现）：单曲匹配

**入口**：替换现有 Auto Edit 按钮。仅单个文件编辑时可用。

### Phase 2（后续迭代）：批量匹配

通过工具栏按钮触发，自动为每个文件用文件名匹配并直接填充（需后续设计）。

---

## Phase 1 详细设计

### 1. 文件名解析

**来源**：从 `MusicData.path` 提取文件名部分（`substringAfterLast("/")`），再去掉扩展名。

**支持的分隔符**：`-`（横杠）、`_`（下划线）、曲目号前缀（如 `01-`、`01.`）

**解析优先级**（从高到低）：
1. `曲目号分隔符 艺术家分隔符 标题` — 如 `01 - Artist - Title.flac`、`01_Artist_Title.mp3`
2. `艺术家分隔符 标题` — 如 `周杰伦-美人鱼.flac`、`Artist_Title.mp3`
3. `仅标题` — 无法拆分时，整个文件名作为标题，艺术家留空

**解析输出**：`FileNameParseResult(artist: String?, title: String)`

**搜索前确认**：解析后先弹出对话框，展示解析出的艺术家和标题，允许用户修改后再搜索。解析失败（仅标题）时也展示，允许用户手动补充艺术家。

### 2. MusicBrainz 查询

**策略**：用解析出的 artist + title 构建查询字符串（空格拼接），调用现有 `MusicBrainzRepository.searchReleases()`。

**与现有逻辑的区别**：
- 不使用编辑器中的 tag 字段值，而是使用文件名解析结果
- 查询后需要对 release 中的 track 列表做过滤和排序

### 3. 搜索结果展示

**UI 样式**：Material 3 `ModalBottomSheet`（参考现有 `LyricsEditorSheet` 的模式）

**展示内容**：track 级别的匹配结果列表

**每个 track 列表项显示**：
- 封面缩略图（使用 `release.coverArtUrl`，通过 Coil AsyncImage 加载）
- 曲目号 + 标题
- 艺术家
- 来自哪个 release（专辑名 + 年份）

**过滤 + 排序逻辑**：
- 用文件名解析出的 `title` 与每个 track 的 `title` 做模糊匹配（`contains`，忽略大小写）
- 匹配的 track 排在前面，不匹配的排在后面（全部展示，不丢弃）
- 如果有 track 精确匹配（equals ignore case），排在最前

### 4. 应用结果

用户选中某个 track 后：
- 将该 track 的 title、artist、track number 填入编辑器
- 将该 track 所属 release 的 album、year、country、label 等元数据填入编辑器
- **封面处理**：track 列表中嵌入缩略图（已通过 Coil 预加载），选中后自动下载完整封面并应用
- 调用 `MusicBrainzMapper.mapToFieldStates()` 映射（需要扩展以支持指定 track）

### 5. 错误处理

- 文件名为空 → 不触发搜索
- 搜索无结果 → BottomSheet 显示空状态提示
- 网络错误 → 显示错误信息和重试按钮
- 封面下载失败 → 不影响标签填充，仅跳过封面

---

## 实现计划

### Step 1: 创建文件名解析器

**新文件**：`app/src/main/java/dev/secam/simpletag/util/FileNameParser.kt`

```kotlin
data class FileNameParseResult(
    val artist: String?,
    val title: String
)

object FileNameParser {
    fun parse(fileName: String): FileNameParseResult
}
```

解析逻辑：
1. 去除文件扩展名
2. 尝试匹配 `曲目号分隔符艺术家分隔符标题` 模式（正则：`^(\d+)[.\s\-_]+(.+?)[.\s\-_]+(.+)$`）
3. 尝试匹配 `艺术家分隔符标题` 模式（按 `-` 或 `_` 分割为 2 部分）
4. 兜底：整个文件名作为 title

### Step 2: 创建解析确认对话框

**新文件**：`app/src/main/java/dev/secam/simpletag/ui/editor/dialogs/FileNameConfirmDialog.kt`

复用 `SimpleDialog` 组件，包含：
- Artist 输入框（可编辑）
- Title 输入框（可编辑）
- "搜索" 和 "取消" 按钮

### Step 3: 创建 Track 级别搜索结果 BottomSheet

**新文件**：`app/src/main/java/dev/secam/simpletag/ui/editor/dialogs/AutoEditTrackSheet.kt`

参考 `LyricsEditorSheet.kt` 的 ModalBottomSheet 模式：
- 使用 `ModalBottomSheet` + `LazyColumn`
- 每个 item 包含：封面缩略图（AsyncImage）、track 号 + 标题、艺术家、release 信息
- 点击 track → 调用 apply 逻辑并关闭 sheet

### Step 4: 扩展 MusicBrainzMapper

**修改文件**：`app/src/main/java/dev/secam/simpletag/data/musicbrainz/MusicBrainzMapper.kt`

新增方法：
```kotlin
fun mapTrackToFieldStates(release: MusicBrainzRelease, track: MusicBrainzTrack): Map<SimpleTagField, String>
```

将指定的 track 信息 + release 级别的元数据映射为编辑器字段。

### Step 5: 修改 EditorViewModel

**修改文件**：`app/src/main/java/dev/secam/simpletag/ui/editor/EditorViewModel.kt`

新增：
- `parseFileName(fileName: String): FileNameParseResult` — 调用 FileNameParser
- `showFileNameConfirmDialog` 状态
- `autoEditTrackResults: List<AutoEditTrackItem>` 状态（包含 track + release 信息）
- `applyAutoEditTrack(release, track)` — 应用选中的 track 结果

修改：
- `fetchAutoEditData()` — 改为从文件名解析结果构建查询，而非编辑器字段
- 将 `AutoEditState` 扩展以包含 track 级别结果

### Step 6: 修改 EditorUiState

**修改文件**：`app/src/main/java/dev/secam/simpletag/ui/editor/EditorViewModel.kt`（EditorUiState data class）

新增字段：
- `showFileNameConfirmDialog: Boolean`
- `parsedArtist: String?`
- `parsedTitle: String`
- `autoEditTrackResults: List<AutoEditTrackItem>`

### Step 7: 修改 EditorScreen

**修改文件**：`app/src/main/java/dev/secam/simpletag/ui/editor/EditorScreen.kt`

修改 Auto Edit 按钮的 onClick：
1. 从 `MusicData.path` 提取文件名
2. 调用 `FileNameParser.parse()`
3. 更新状态，弹出 `FileNameConfirmDialog`
4. 用户确认后 → 调用 `fetchAutoEditData()`
5. 成功后 → 显示 `AutoEditTrackSheet`

新增对话框/Sheet 的渲染：
- `FileNameConfirmDialog`
- `AutoEditTrackSheet`

---

## 关键文件

| 文件 | 操作 |
|------|------|
| `util/FileNameParser.kt` | **新建** — 文件名解析器 |
| `ui/editor/dialogs/FileNameConfirmDialog.kt` | **新建** — 解析确认对话框 |
| `ui/editor/dialogs/AutoEditTrackSheet.kt` | **新建** — track 结果 BottomSheet |
| `data/musicbrainz/MusicBrainzMapper.kt` | **修改** — 新增 mapTrackToFieldStates() |
| `ui/editor/EditorViewModel.kt` | **修改** — 新增状态和方法 |
| `ui/editor/EditorScreen.kt` | **修改** — Auto Edit 按钮逻辑 + UI |

## 复用的现有组件

- `SimpleDialog`（`ui/components/SimpleDialog.kt`）— 确认对话框
- `ModalBottomSheet`（Material 3）— BottomSheet（参考 `LyricsEditorSheet.kt`）
- `MusicBrainzRepository.searchReleases()` — 查询逻辑不变
- `MusicBrainzApiService` — API 接口不变
- `AsyncImage`（Coil）— 封面缩略图加载

## 验证方式

1. 准备一个无 tag 的音乐文件，文件名为 `周杰伦-美人鱼.flac`
2. 在 SimpleTag 中选择该文件进入编辑器
3. 点击 Auto Edit 按钮
4. 验证弹出确认对话框，显示 Artist=`周杰伦`，Title=`美人鱼`
5. 点击搜索，验证 BottomSheet 显示 track 列表，带有封面缩略图
6. 验证「美人鱼」track 排在列表最前
7. 选中该 track，验证编辑器字段被正确填充（title、artist、album、year、track number、封面）
8. 运行 `./gradlew assembleDebug` 确认编译通过
