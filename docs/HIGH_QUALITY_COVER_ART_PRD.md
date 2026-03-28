# PRD: Auto Edit 获取高质量专辑封面

## Context

在 auto edit 功能中，当前通过 Cover Art Archive 获取封面图时只使用简单的重定向 URL (`/release/{mbid}/front`)。

### 发现：MusicBrainz API 已包含封面信息

通过验证 MusicBrainz Release API，发现完整响应中包含 `cover-art-archive` 字段：

```json
"cover-art-archive": {
    "darkened": false,
    "count": 0,       // 封面数量
    "artwork": false, // 是否有任何封面
    "back": false,    // 是否有背面封面
    "front": false    // 是否有正面封面
}
```

**这允许在调用 Cover Art Archive API 之前预先判断是否有封面可用，避免无效请求。**

### 优化方案

利用 Cover Art Archive API 的元数据能力：
- **封面元数据 API** (`/release/{mbid}/`) - 返回所有可用封面图及其缩略图信息
- **多种缩略图尺寸** - 250px, 500px, 1200px
- **预检查优化** - 使用 MusicBrainz 的 `cover-art-archive` 字段提前判断

本需求旨在利用这些 API 能力，在 auto edit 时优先获取**高质量封面图**（优先 500px），提供更好的用户体验。

---

## 实现方案

### 架构概览

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  EditorViewModel │ ──▶ │ CoverArtRepository │ ──▶ │ CoverArtArchive │
│                 │     │                   │     │     API        │
│  fetchAndApply  │     │  - Cache (LRU)    │     │  /release/{id}/ │
│  CoverArt()     │     │  - Rate Limit     │     │  Returns JSON   │
└─────────────────┘     │  - Quality Select │     └─────────────────┘
                        └──────────────────┘
                                 │
                                 ▼
                        ┌─────────────────┐
                        │ CoverArtResult  │
                        │  - Success      │
                        │  - PartialSuccess│
                        │  - NoCoverArt   │
                        │  - Error        │
                        └─────────────────┘
```

### 核心逻辑：智能质量选择（含预检查优化）

```
0. 【新增】预检查：读取 MusicBrainz 响应中的 cover-art-archive.front
   ├─ 如果 front = false → 跳过，直接尝试 fallback
   └─ 如果 front = true → 继续下一步
   ↓
1. 调用 /release/{mbid}/ 获取封面元数据
   ↓
2. 解析 JSON，提取所有 images
   ↓
3. 筛选 Front 类型的图片
   ↓
4. 质量优先级选择：
   - 优先：500px 缩略图 (large)
   - 其次：250px 缩略图 (small)
   - 最后：原图
   ↓
5. 如果没有 Front，选择其他 approved 图片
   ↓
6. 下载选中的图片并应用
```

### 多级 Fallback 策略

```
Release 500px
    ↓ 失败
Release 250px
    ↓ 失败
Release Original
    ↓ 失败
Release Group 500px
    ↓ 失败
Release Group 250px
    ↓ 失败
显示错误提示
```

---

## 关键文件修改

### 1. 新建文件

#### `app/src/main/java/dev/secam/simpletag/data/musicbrainz/models/CoverArtModels.kt`
```kotlin
// Cover Art Archive API 响应模型
data class CoverArtArchiveResponse(val images: List<CoverArtImage>)

data class CoverArtImage(
    val types: List<String>,
    val front: Boolean,
    val back: Boolean,
    val image: String,
    val thumbnails: CoverArtThumbnails?,
    val approved: Boolean,
    val id: String
)

data class CoverArtThumbnails(
    val small: String?,    // 250px
    val large: String?,    // 500px
    val `1200`: String?    // 1200px
)

// 【新增】MusicBrainz API 响应中的封面信息预检查
data class CoverArtInfo(
    val front: Boolean,    // 是否有正面封面
    val back: Boolean,     // 是否有背面封面
    val count: Int,        // 封面总数
    val artwork: Boolean   // 是否有任何封面
)

enum class CoverArtQuality {
    MEDIUM,  // 500px (优先)
    LOW,     // 250px
    ORIGINAL // 原图
}

sealed class CoverArtResult {
    data class Success(val coverArt: SelectedCoverArt) : CoverArtResult()
    data class PartialSuccess(val coverArt: SelectedCoverArt, val fallbackMessage: String) : CoverArtResult()
    data class Error(val message: String, val cause: Throwable?) : CoverArtResult()
    object NoCoverArt : CoverArtResult()
}
```

#### `app/src/main/java/dev/secam/simpletag/data/musicbrainz/CoverArtArchiveApiService.kt`
```kotlin
interface CoverArtArchiveApiService {
    @GET("release/{mbid}/")
    suspend fun getReleaseCoverArt(mbid: String): CoverArtArchiveResponse

    @GET("release-group/{mbid}/")
    suspend fun getReleaseGroupCoverArt(mbid: String): CoverArtArchiveResponse
}
```

#### `app/src/main/java/dev/secam/simpletag/data/musicbrainz/CoverArtRepository.kt`
```kotlin
class CoverArtRepository(
    private val apiService: CoverArtArchiveApiService,
    private val musicBrainzRepo: MusicBrainzRepository
) {
    private val cache = LruCache<String, CoverArtArchiveResponse>(30)

    /**
     * 【新增】预检查：根据 MusicBrainz 的 cover-art-archive 信息判断是否有封面
     */
    fun shouldFetchCoverArt(coverArtInfo: CoverArtInfo?): Boolean {
        return coverArtInfo?.front == true
    }

    suspend fun getReleaseCoverArtWithFallback(
        releaseId: String,
        releaseGroupId: String? = null,
        coverArtInfo: CoverArtInfo? = null  // 【新增】预检查参数
    ): CoverArtResult {
        // 【新增】预检查：如果没有正面封面，直接跳过
        if (!shouldFetchCoverArt(coverArtInfo)) {
            Log.d(TAG, "No front cover available (cover-art-archive.front=false), skipping fetch")
            // 直接尝试 fallback
            if (!releaseGroupId.isNullOrBlank()) {
                return getReleaseGroupCoverArt(releaseGroupId)
            }
            return CoverArtResult.NoCoverArt
        }

        // 1. Try release
        val releaseResult = getReleaseCoverArt(releaseId)
        if (releaseResult is CoverArtResult.Success ||
            releaseResult is CoverArtResult.PartialSuccess) {
            return releaseResult
        }

        // 2. Fallback to release group
        if (!releaseGroupId.isNullOrBlank()) {
            return getReleaseGroupCoverArt(releaseGroupId)
        }

        return releaseResult
    }

    private fun selectBestCoverArt(response: CoverArtArchiveResponse): CoverArtResult {
        // Smart quality selection logic
        // Priority: large(500px) > small(250px) > original
    }
}
```

### 2. 修改文件

#### `app/src/main/java/dev/secam/simpletag/data/musicbrainz/models/MusicBrainzModels.kt`

```kotlin
data class MusicBrainzRelease(
    val id: String,
    val title: String,
    // ... 其他字段 ...
    val coverArtUrl: String? = null,
    val coverArtInfo: CoverArtInfo? = null,  // 【新增】封面可用性信息
    val releaseGroupId: String? = null
)
```

#### `app/src/main/java/dev/secam/simpletag/data/musicbrainz/MusicBrainzRepository.kt`

```kotlin
private fun parseRelease(obj: JSONObject): MusicBrainzRelease {
    val id = obj.getString("id")

    // ... 现有解析逻辑 ...

    // 【新增】解析 cover-art-archive 信息
    val coverArtArchive = obj.optJSONObject("cover-art-archive")
    val coverArtInfo = if (coverArtArchive != null) {
        CoverArtInfo(
            front = coverArtArchive.optBoolean("front", false),
            back = coverArtArchive.optBoolean("back", false),
            count = coverArtArchive.optInt("count", 0),
            artwork = coverArtArchive.optBoolean("artwork", false)
        )
    } else {
        // 兼容旧响应，默认尝试获取
        CoverArtInfo(front = true, back = false, count = 0, artwork = true)
    }

    return MusicBrainzRelease(
        id = id,
        // ...
        coverArtInfo = coverArtInfo  // 【新增】
    )
}

// 添加包级可见方法供 CoverArtRepository 共享速率限制
suspend fun waitForRateLimit() {
    rateLimitMutex.withLock {
        // ... existing rate limit logic
    }
}
```

#### `app/src/main/java/dev/secam/simpletag/di/AppModule.kt`
```kotlin
@Singleton
@Provides
fun provideCoverArtArchiveRetrofit(okHttpClient: OkHttpClient): Retrofit {
    return Retrofit.Builder()
        .baseUrl("https://coverartarchive.org/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
}

@Singleton
@Provides
fun provideCoverArtArchiveApiService(retrofit: Retrofit): CoverArtArchiveApiService {
    return retrofit.create(CoverArtArchiveApiService::class.java)
}

@Singleton
@Provides
fun provideCoverArtRepository(
    apiService: CoverArtArchiveApiService,
    musicBrainzRepo: MusicBrainzRepository
): CoverArtRepository {
    return CoverArtRepository(apiService, musicBrainzRepo)
}
```

#### `app/src/main/java/dev/secam/simpletag/ui/editor/EditorViewModel.kt`
```kotlin
@HiltViewModel
class EditorViewModel @Inject constructor(
    // ... existing dependencies ...
    private val coverArtRepository: CoverArtRepository,
    private val okHttpClient: OkHttpClient
) : ViewModel() {

    fun fetchAndApplyCoverArt(
        releaseId: String? = null,
        releaseGroupId: String? = null,
        coverArtInfo: CoverArtInfo? = null,  // 【新增】预检查信息
        directUrl: String? = null  // Legacy support
    ) {
        backgroundScope.launch {
            // Legacy support
            if (!directUrl.isNullOrBlank() && releaseId.isNullOrBlank()) {
                tryDownloadCoverArt(directUrl)
                return@launch
            }

            when (val result = coverArtRepository.getReleaseCoverArtWithFallback(
                releaseId = releaseId ?: return@launch,
                releaseGroupId = releaseGroupId,
                coverArtInfo = coverArtInfo  // 【新增】传递预检查信息
            )) {
                is CoverArtResult.Success -> {
                    downloadAndApplyCoverArt(result.coverArt)
                }
                is CoverArtResult.PartialSuccess -> {
                    downloadAndApplyCoverArt(result.coverArt)
                    _coverArtError.value = result.fallbackMessage
                }
                is CoverArtResult.NoCoverArt -> {
                    _coverArtError.value = "No cover art available"
                }
                is CoverArtResult.Error -> {
                    _coverArtError.value = "Failed: ${result.message}"
                }
            }
        }
    }

    private suspend fun downloadAndApplyCoverArt(coverArt: SelectedCoverArt): Boolean {
        // Download from URL and apply to editor
    }
}
```

### 3. 依赖添加

#### `app/build.gradle`
```gradle
// For Cover Art Archive JSON parsing
implementation("com.squareup.moshi:moshi:1.15.0")
implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.0")
```

---

## 实现步骤

1. **创建数据模型** - `CoverArtModels.kt`
2. **修改 MusicBrainz 模型** - 添加 `CoverArtInfo` 到 `MusicBrainzRelease`
3. **修改 MusicBrainz Repository** - 解析 `cover-art-archive` 字段
4. **创建 API 服务** - `CoverArtArchiveApiService.kt`
5. **更新 DI 配置** - `AppModule.kt`
6. **创建 Repository** - `CoverArtRepository.kt`
7. **更新 ViewModel** - `EditorViewModel.kt`
8. **测试验证**

---

## 向后兼容性

- 保留 `coverArtUrl` 字段
- 保留 `tryDownloadCoverArt()` 方法
- `fetchAndApplyCoverArt()` 支持新旧两种调用方式
- 如果新 API 失败，自动降级到旧方式
- `CoverArtInfo` 缺失时默认尝试获取封面

---

## 测试验证

1. **正常流程**：有 500px 封面的发行 → 验证下载 500px 图片
2. **降级流程**：只有 250px/原图的发行 → 验证选择可用最高质量
3. **Fallback 流程**：Release 失败后尝试 Release Group
4. **【新增】预检查流程**：`cover-art-archive.front=false` → 跳过 API 调用，直接尝试 fallback
5. **错误处理**：网络错误、404、503 等情况的用户提示

---

## 关键文件路径

| 文件 | 操作 | 说明 |
|------|------|------|
| `data/musicbrainz/models/CoverArtModels.kt` | 新建 | 封面数据模型 + CoverArtInfo |
| `data/musicbrainz/models/MusicBrainzModels.kt` | **修改** | MusicBrainzRelease 添加 coverArtInfo 字段 |
| `data/musicbrainz/CoverArtArchiveApiService.kt` | 新建 | Retrofit API |
| `data/musicbrainz/CoverArtRepository.kt` | 新建 | 封面仓库 + 预检查逻辑 |
| `di/AppModule.kt` | 修改 | DI 配置 |
| `ui/editor/EditorViewModel.kt` | 修改 | 封面获取逻辑 + 传递 coverArtInfo |
| `data/musicbrainz/MusicBrainzRepository.kt` | **修改** | 解析 cover-art-archive 字段 |
| `app/build.gradle` | 修改 | 添加 Moshi 依赖 |
