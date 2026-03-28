
package dev.secam.simpletag.ui.editor

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.secam.simpletag.data.enums.SimpleTagField
import dev.secam.simpletag.data.media.MediaRepo
import dev.secam.simpletag.data.media.MusicData
import dev.secam.simpletag.data.musicbrainz.CoverArtRepository
import dev.secam.simpletag.data.musicbrainz.MusicBrainzMapper
import dev.secam.simpletag.data.musicbrainz.MusicBrainzRepository
import dev.secam.simpletag.data.musicbrainz.models.CoverArtInfo
import dev.secam.simpletag.data.musicbrainz.models.CoverArtResult
import dev.secam.simpletag.data.musicbrainz.models.MusicBrainzResult
import dev.secam.simpletag.data.musicbrainz.models.MusicBrainzRecording
import dev.secam.simpletag.data.preferences.PreferencesRepo
import dev.secam.simpletag.util.FileNameParser
import dev.secam.simpletag.util.FileNameParseResult
import dev.secam.simpletag.util.getMimeType
import dev.secam.simpletag.util.tag.oggFileWriter
import dev.secam.simpletag.util.tag.setArtworkField
import dev.secam.simpletag.util.tag.simpleFileReader
import dev.secam.simpletag.util.tag.simpleFileWriter
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withTimeout
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.asf.AsfTag
import org.jaudiotagger.tag.flac.FlacTag
import org.jaudiotagger.tag.id3.ID3v24Tag
import org.jaudiotagger.tag.images.AndroidArtwork.createArtworkFromFile
import org.jaudiotagger.tag.images.Artwork
import org.jaudiotagger.tag.mp4.Mp4Tag
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag
import java.io.File
import java.nio.file.AccessDeniedException
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.ArrayList
import javax.inject.Inject

const val WRITE_TIMEOUT = 1000L
val SUPPORTS_RG = listOf(
    "mp3",
    "wav",
    "wave",
    "dsf",
    "wma",
    "ogg",
    "flac",
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    preferencesRepo: PreferencesRepo,
    private val mediaRepo: MediaRepo,
    private val musicBrainzRepository: MusicBrainzRepository,
    private val coverArtRepository: CoverArtRepository,
    private val okHttpClient: OkHttpClient
): ViewModel() {
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState = _uiState.asStateFlow()
    val prefState = preferencesRepo.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }
    private val backgroundScope =
        viewModelScope.plus(Dispatchers.Default + coroutineExceptionHandler)

    fun initEditor(musicList: List<MusicData>, tagNames: Map<SimpleTagField, String>) {
        backgroundScope.launch {
            var supportsRG = true
            resetFields()
            val simpleEditor = prefState.value?.simpleEditor
            if (simpleEditor != null) {
                _uiState.update { currentState ->
                    currentState.copy(
                        invisibleTags = SimpleTagField.entries.toSet(),
                        tagNames = tagNames
                    )
                }
                // open file
                val firstFile = simpleFileReader(musicList[0].path)
                if (firstFile != null) {
                    // check file replaygain support
                    if (!SUPPORTS_RG.contains(firstFile.ext)) {
                        removeRG()
                        supportsRG = false
                    }
                    // open tag
                    val firstTag = firstFile.tag
                    setEditorMusicList(musicList)
                    if(firstTag != null){
                        // set artwork
                        setArtwork(firstTag.firstArtwork)
                        // add fields to editor display
                        SimpleTagField.entries.forEachIndexed { index, field ->
                            // basic fields
                            if (simpleEditor) {
                                if (index <= SimpleTagField.ADVANCED_CUTOFF) {
                                    addField(field, firstTag.getFirst(field.fieldKey), false)
                                }
                                // advanced fields
                            } else {
                                if(field == SimpleTagField.ReplayGainTrack || field == SimpleTagField.ReplayGainAlbum){
                                    // skip checking replaygain if unsupported
                                    if(supportsRG){
                                        if (!firstTag.getFirst(field.fieldKey).isEmpty()) {
                                            addField(field, firstTag.getFirst(field.fieldKey), false)
                                        }
                                    }
                                } else {
                                    if (!firstTag.getFirst(field.fieldKey).isEmpty()) {
                                        addField(field, firstTag.getFirst(field.fieldKey), false)
                                    }
                                }
                            }
                        }
                        deleteLyricsField()
                        val lyrics = firstTag.getFirst(SimpleTagField.Lyrics.fieldKey)
                        if(lyrics != null){
                            setLyrics(lyrics)
                        }
                    }
                }
                if (musicList.size > 1 && !simpleEditor){
                    for (song in musicList - musicList[0]) {
                        val file = simpleFileReader(song.path)
                        if (file != null){
                            // Check ReplayGain support for all files
                            if (!SUPPORTS_RG.contains(file.ext)) {
                                if (supportsRG) {
                                    removeRG()
                                    supportsRG = false
                                }
                            }
                            // open tag
                            val tag = file.tag
                            if (tag != null) {
                                for (tagField in uiState.value.invisibleTags) {
                                    if (!tag.getFirst(tagField.fieldKey).isEmpty()) {
                                        addField(tagField, tag.getFirst(tagField.fieldKey), false)
                                    }
                                }
                            }
                        }
                    }
                }
                setSavedFields()
                setArtworkChanged(false)
                setInitialized(true)
                onSearch()
            }
        }
    }

    fun deleteLyricsField(){
        _uiState.update { it.copy(
            fieldStates = uiState.value.fieldStates - SimpleTagField.Lyrics,
            invisibleTags = uiState.value.invisibleTags - SimpleTagField.Lyrics,
        ) }
    }
    fun addField(field: SimpleTagField, content: String = "", enabled: Boolean = true){
        _uiState.update {
            it.copy(
                fieldStates = uiState.value.fieldStates + Pair(field,
                    EditorFieldState(
                        textState = TextFieldState(content),
                        enabledState = mutableStateOf(enabled)
                    )
                ),
                invisibleTags = uiState.value.invisibleTags - field,
                deletedFields = uiState.value.deletedFields - field
            )
        }
    }
    fun removeRG() {
        _uiState.update {
            it.copy(
                invisibleTags = uiState.value.invisibleTags - setOf(
                    SimpleTagField.ReplayGainAlbum,
                    SimpleTagField.ReplayGainTrack
                ),
            )
        }
    }
    fun removeField(field: SimpleTagField){
        setChangesMade(true)
        _uiState.update {
            it.copy(
//                fieldStates = uiState.value.fieldStates - field,
                invisibleTags = uiState.value.invisibleTags + field,
                deletedFields = uiState.value.deletedFields + field,

            )
        }
    }

    fun resetFields() {
        _uiState.update {
            it.copy(
                fieldStates = mapOf(),
                invisibleTags = setOf(),
                deletedFields = setOf(),
            )
        }
    }
    fun createTag(ext: String): Tag {
        return when (ext) {
            "flac" -> FlacTag()
            "mp4", "m4a", "m4p", "aac" -> Mp4Tag()
            "ogg" -> VorbisCommentTag()
            "wma" -> AsfTag()
            "mp3", "wav", "wave", "dsf", "aiff", "aif", "aifc" -> ID3v24Tag()
            else -> null
        }!!
    }

    fun getArtworkFromUri(contentResolver: ContentResolver, uri: Uri): Artwork? {
        var path: String? = null
        val projection = arrayOf(
            MediaStore.Images.Media.DATA
        )
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                path = cursor.getString(columnIndex)
            }
        }
        val artwork = createArtworkFromFile(File(path!!))
        _uiState.update { currentState ->
            currentState.copy(
                artwork = artwork
            )
        }
        return artwork
    }
    fun openExternal(context: Context, data: MusicData){
        val tempFile = File.createTempFile("open_external_temp", null,context.cacheDir)
        tempFile.writeBytes(File(data.path).readBytes())
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(FileProvider.getUriForFile(context, context.applicationContext.packageName + ".provider", tempFile), tempFile.getMimeType() )
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        context.startActivity(intent)
    }

    fun clearCache(context: Context) {
        context.cacheDir.delete()
    }
    suspend fun writeTags(context: Context): Boolean {
        _uiState.update { it.copy(log = "") }
        backgroundScope.async {
            var log = ""
            try {
                withTimeout(WRITE_TIMEOUT) {
                    log = "Entered writeTags()\n"
                    val fields = uiState.value.fieldStates - uiState.value.deletedFields
                    val artwork = uiState.value.artwork
                    val lyrics = uiState.value.lyrics
                    val musicList = uiState.value.editorMusicList
                    // single editor
                    if (musicList.size == 1) {
                        log += "Writing single file\n"
                        val file = simpleFileReader(musicList[0].path)
                        if(file != null) {
                            log += "Opened file: ${file.file.path}\n"
                            if (file.tag == null) {
                                log += "No tag. Creating tag\n"
                                file.tag = createTag(file.ext)
                                log += "Tag created\n"
                            }
                            val tag = file.tag
                            log += "Tag opened\n"
                            tag.deleteArtworkField()
                            log += "Deleted old artwork\n"
                            if (artwork != null) {
                                when (tag.javaClass) {
                                    FlacTag().javaClass -> {
                                        (tag as FlacTag).setArtworkField(artwork)
                                    }
                                    VorbisCommentTag().javaClass -> {
                                        (tag as VorbisCommentTag).setArtworkField(artwork)
                                    }
                                    else -> tag.setField(artwork)
                                }
                                log += "Wrote new artwork as ${tag.javaClass}\n"
                            }
                            for(field in uiState.value.deletedFields){
                                tag.deleteField(field.fieldKey)
                            }
                            if (lyrics != null) {
                                tag.setField(SimpleTagField.Lyrics.fieldKey,lyrics)
                            } else {
                                tag.deleteField(SimpleTagField.Lyrics.fieldKey)
                            }
                            for (field in fields) {
                                if (!field.value.textState.text.isEmpty()) {
                                    tag.setField(field.key.fieldKey, field.value.textState.text as String)
                                    log += "Wrote field ${field.key.fieldKey} with content: ${field.value.textState.text}\n"
                                } else {
                                    tag.deleteField(field.key.fieldKey)
                                    log += "Cleared field ${field.key.fieldKey}\n"
                                }
                            }
                            if (file.ext == "ogg" || file.ext == "opus") {
                                log += "Writing file using ogg writer\n"
                                oggFileWriter(file, context)
                                log += "Wrote file: ${file.file.path} \n"
                            } else {
                                log += "Writing file using default writer\n"
                                simpleFileWriter(file)
                                log += "Wrote file: ${file.file.path} \n"
                            }
                        }
                    //  Batch Editor
                    } else {
                        log += "Writing multiple files\n"
                        val enabledFieldStates = fields.filter { it.value.enabledState.value }
                        for(song in musicList){
                            val file = simpleFileReader(song.path)
                            if(file != null){
                                log += "Opened file: ${file.file.path}\n"
                                if (file.tag == null) {
                                    log += "No tag. Creating tag\n"
                                    file.tag = createTag(file.ext)
                                    log += "Tag created\n"
                                }
                                val tag = file.tag
                                log += "Tag opened\n"
                                if(uiState.value.artworkEnabled){
                                    tag.deleteArtworkField()
                                    log += "Deleted old artwork\n"
                                    if (artwork != null) {
                                        when (tag.javaClass) {
                                            FlacTag().javaClass -> {
                                                (tag as FlacTag).setArtworkField(artwork)
                                            }
                                            VorbisCommentTag().javaClass -> {
                                                (tag as VorbisCommentTag).setArtworkField(artwork)
                                            }
                                            else -> tag.setField(artwork)
                                        }
                                        log += "Wrote new artwork as ${tag.javaClass}\n"
                                    }
                                }

                                for(field in uiState.value.deletedFields){
                                    tag.deleteField(field.fieldKey)
                                }
                                for (field in enabledFieldStates) {
                                    if (!field.value.textState.text.isEmpty()) {
                                        tag.setField(field.key.fieldKey, field.value.textState.text as String)
                                        log += "Wrote field ${field.key.fieldKey} with content: ${field.value.textState.text}\n"
                                    } else {
                                        tag.deleteField(field.key.fieldKey)
                                        log += "Cleared field ${field.key.fieldKey}\n"
                                    }
                                }
                                if (file.ext == "ogg" || file.ext== "opus") {
                                    log += "Writing file using ogg writer\n"
                                    oggFileWriter(file, context)
                                    log += "Wrote file: ${file.file.path} \n"
                                } else {
                                    log += "Writing file using default writer\n"
                                    simpleFileWriter(file)
                                    log += "Wrote file: ${file.file.path} \n"
                                }
                            }
                        }
                    }
                    log += "Resetting change tracking\n"
                    //  reset change tracking
                    setArtworkChanged(false)
                    setSavedFields()
                    setChangesMade(false)
                    log += "Change tracking reset\n"
                    //  refresh mediastore to reflect changes
                    log += "Refreshing mediastore\n"
                    mediaRepo.refreshMediaStore(musicList)
                    log += "Mediastore refreshed\n"
                    log += "Finished saving\n"
                    Log.d("EditorVM", log)
                }
            } catch (e: TimeoutCancellationException) {
                _uiState.update { it.copy(log = e.toString() + "\n$log") }
            }
        }.await()
        return uiState.value.log == ""
    }

    fun onSave(
        activity: Activity?,
        context: Context?,
        launcher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>?,
        snackbarHostState: SnackbarHostState,
        onCancelText: String,
        onOkText: String,
        onErrorText: String,
        actionText: String
    ) {
        if (activity != null && context != null) {
            backgroundScope.launch {
                // request permission for api 30+
                if (launcher != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val musicList = uiState.value.editorMusicList
                    val uris = ArrayList(musicList.map{ song ->
                        ContentUris.withAppendedId(
                            MediaStore.Audio.Media.getContentUri("external"),
                            song.id
                        )
                    })
                    val request = IntentSenderRequest.Builder(
                        MediaStore.createWriteRequest(
                            context.contentResolver,
                            uris
                        )
                    ).build()
                    launcher.launch(request)
                }
                // permission request not needed for api 29 and below
                else {
                    try {
                        if (!writeTags(context)) {
                            if (snackbarHostState.showSnackbar(
                                    onErrorText,
                                    actionText
                                ) == SnackbarResult.ActionPerformed
                            )
                                setShowLogDialog(true)
                        } else snackbarHostState.showSnackbar(onOkText)

                    } catch (e: AccessDeniedException) {
                        e.printStackTrace()
                        snackbarHostState.showSnackbar(onCancelText)
                    }
                }
            }
        }
    }

    fun changesMade(): Boolean {
        val currentFields = uiState.value.fieldStates.map { mapEntry ->
            mapEntry.value.textState.text as String
        }
        val lyricsChanged = uiState.value.savedLyrics != (uiState.value.lyrics ?: "")
        return (uiState.value.changesMade || uiState.value.artworkChanged || uiState.value.savedFields != currentFields || lyricsChanged )
    }

    fun onSearch(query: String = "") {
        val list = uiState.value.tagNames .filterKeys { it in uiState.value.invisibleTags }
        if(!query.isEmpty()){
            val list1 = mutableListOf<SimpleTagField>()
            val list2 = mutableListOf<SimpleTagField>()
            for (entry in list) {
                if(entry.key in uiState.value.invisibleTags){
                    if (entry.value.startsWith(query, true)) {
                        list1.add(entry.key)
                    } else if (entry.value.contains(query, true)) {
                        list2.add(entry.key)
                    }
                }
            }
            _uiState.update { it.copy(searchResults = list1 + list2) }
        } else _uiState.update { currentState ->
            currentState.copy(searchResults = list.map { it.key })
        }
    }
    fun toggleSelectAll(){
        backgroundScope.launch {
            val fieldStates = uiState.value.fieldStates
            val artworkEnabled: Boolean
            if(allEnabled()) {
                for(field in fieldStates){
                    field.value.enabledState.value = false
                }
                artworkEnabled = false
            }
            else {
                for(field in fieldStates){
                    field.value.enabledState.value = true
                }
                artworkEnabled = true
            }
            _uiState.update { it.copy(
                fieldStates = fieldStates,
                artworkEnabled = artworkEnabled
            ) }
        }
    }

    fun allEnabled(): Boolean{
        val fieldStates = uiState.value.fieldStates
        return uiState.value.artworkEnabled && fieldStates.map {
            it.value.enabledState.value
        }.all { it }
    }


    /*------- Setters -------*/
    fun setArtwork(artwork: Artwork?) {
        Log.d("AutoEdit", "setArtwork: artwork=${if (artwork != null) "${artwork.binaryData?.size} bytes, mimeType=${artwork.mimeType}" else "null"}")
        _uiState.update { currentState ->
            currentState.copy(
                artwork = artwork,
                artworkChanged = true
            )
        }
    }

    fun setEditorMusicList(editorMusicList: List<MusicData>) {
        _uiState.update { currentState ->
            currentState.copy(
                editorMusicList = editorMusicList
            )
        }
    }

    fun setInitialized(initialized: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                initialized = initialized
            )
        }
    }

    fun setShowBackDialog(showBackDialog: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                showBackDialog = showBackDialog
            )
        }
    }

    fun setShowSaveDialog(showSaveDialog: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                showSaveDialog = showSaveDialog
            )
        }
    }

    fun setSavedFields() {
        _uiState.update {
            it.copy(
                savedFields = uiState.value.fieldStates.map { mapEntry ->
                    mapEntry.value.textState.text as String
                },
                savedLyrics = uiState.value.lyrics ?: ""
            )
        }
    }
    fun setArtworkChanged(artworkChanged: Boolean) {
        _uiState.update { it.copy(artworkChanged = artworkChanged) }
    }

    fun setShowLogDialog(showLogDialog: Boolean) {
        _uiState.update { it.copy(showLogDialog = showLogDialog) }
    }
    fun setShowHelpDialog(showHelpDialog: Boolean) {
        _uiState.update { it.copy(showHelpDialog = showHelpDialog) }
    }

    fun setShowAddFieldDialog(showAddFieldDialog: Boolean){
        _uiState.update { it.copy(showAddFieldDialog = showAddFieldDialog) }
    }
    fun setShowLyricsSheet(showLyricsSheet: Boolean) {
        _uiState.update { it.copy(showLyricsSheet = showLyricsSheet) }
    }
    fun setShowSongSyncMissingDialog(showSongSyncMissingDialog: Boolean) {
        _uiState.update { it.copy(showSongSyncMissingDialog = showSongSyncMissingDialog) }
    }
    fun setChangesMade(changesMade: Boolean) {
        _uiState.update { it.copy(changesMade = changesMade) }
    }
    fun setArtworkEnabled(artworkEnabled: Boolean){
        _uiState.update { it.copy(artworkEnabled = artworkEnabled) }
    }
    fun setLyrics(lyrics: String?){
//        if(lyrics != uiState.value.lyrics) {
//            setChangesMade(true)
//        }
        _uiState.update { it.copy(lyrics = lyrics) }
    }

    /*      Auto Edit Methods     */

    /**
     * Fetch metadata from MusicBrainz based on current tag values
     * @param queryParams Query parameters for MusicBrainz search
     */
    fun fetchAutoEditData(queryParams: AutoEditQueryParams) {
        backgroundScope.launch {
            val queryDesc = "title=${queryParams.title}, artist=${queryParams.artist}, album=${queryParams.album}, track=${queryParams.track}"
            Log.d("AutoEdit", "fetchAutoEditData: $queryDesc")
            _uiState.update { it.copy(autoEditState = AutoEditState.Loading) }

            val result = musicBrainzRepository.searchReleases(
                title = queryParams.title,
                artist = queryParams.artist,
                album = queryParams.album,
                track = queryParams.track
            )

            Log.d("AutoEdit", "Result type: ${result::class.simpleName}")

            when (result) {
                is MusicBrainzResult.Success -> {
                    if (result.data.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                autoEditState = AutoEditState.NoResults,
                                autoEditResults = listOf(),
                                showAutoEditDialog = true
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                autoEditState = AutoEditState.Success(result.data),
                                autoEditResults = result.data,
                                showAutoEditDialog = true
                            )
                        }
                    }
                }
                is MusicBrainzResult.Error -> {
                    Log.e("AutoEdit", "Error: ${result.exception.message}", result.exception)
                    _uiState.update {
                        it.copy(
                            autoEditState = AutoEditState.Error(result.exception.message ?: "Unknown error"),
                            showAutoEditDialog = true
                        )
                    }
                }
                is MusicBrainzResult.NoResults -> {
                    _uiState.update {
                        it.copy(
                            autoEditState = AutoEditState.NoResults,
                            autoEditResults = listOf(),
                            showAutoEditDialog = true
                        )
                    }
                }
            }
        }
    }

    /**
     * Select a specific auto-edit result from the list
     */
    fun selectAutoEditResult(release: dev.secam.simpletag.data.musicbrainz.models.MusicBrainzRelease) {
        _uiState.update { it.copy(selectedAutoEditResult = release) }
    }

    /**
     * Apply the selected auto-edit result to the editor fields
     */
    fun applyAutoEditData(release: dev.secam.simpletag.data.musicbrainz.models.MusicBrainzRelease) {
        val fieldMap = MusicBrainzMapper.mapToFieldStates(release)

        _uiState.update { currentState ->
            val updatedFieldStates = currentState.fieldStates.toMutableMap()
            var updatedInvisibleTags = currentState.invisibleTags.toMutableSet()
            var updatedDeletedFields = currentState.deletedFields.toMutableSet()

            fieldMap.forEach { (field, value) ->
                val existingField = currentState.fieldStates[field]
                if (existingField != null) {
                    updatedFieldStates[field] = existingField.copy(
                        textState = TextFieldState(value)
                    )
                } else {
                    updatedFieldStates[field] = EditorFieldState(
                        textState = TextFieldState(value),
                        enabledState = mutableStateOf(false)
                    )
                    updatedInvisibleTags.remove(field)
                    updatedDeletedFields.remove(field)
                }
            }

            currentState.copy(
                fieldStates = updatedFieldStates,
                invisibleTags = updatedInvisibleTags,
                deletedFields = updatedDeletedFields
            )
        }

        setChangesMade(true)
        setShowAutoEditDialog(false)
    }

    /** Cover art download error message, observed by UI to show snackbar */
    private val _coverArtError = MutableStateFlow<String?>(null)

    /**
     * Download cover art from Cover Art Archive using intelligent quality selection.
     * Uses release ID to fetch metadata and selects best quality (500px > 250px > original).
     * Includes pre-check optimization and fallback to release group.
     *
     * @param releaseId MusicBrainz Release ID
     * @param releaseGroupId MusicBrainz Release Group ID (for fallback)
     * @param coverArtInfo Cover art availability info from MusicBrainz (for pre-check)
     */
    fun fetchAndApplyCoverArt(
        releaseId: String? = null,
        releaseGroupId: String? = null,
        coverArtInfo: CoverArtInfo? = null,
        directUrl: String? = null  // Legacy support for direct URL
    ) {
        // Legacy support: if directUrl is provided and releaseId is not, use old method
        if (!directUrl.isNullOrBlank() && releaseId.isNullOrBlank()) {
            fetchAndApplyCoverArtLegacy(directUrl, releaseGroupId)
            return
        }

        // New method: require releaseId
        if (releaseId.isNullOrBlank()) {
            Log.w("AutoEdit", "fetchAndApplyCoverArt: releaseId is null or blank, skipping")
            return
        }

        backgroundScope.launch {
            Log.d("AutoEdit", "Fetching cover art with quality selection for release=$releaseId")

            when (val result = coverArtRepository.getReleaseCoverArtWithFallback(
                releaseId = releaseId,
                releaseGroupId = releaseGroupId,
                coverArtInfo = coverArtInfo
            )) {
                is CoverArtResult.Success -> {
                    Log.d("AutoEdit", "Cover art success: ${result.coverArt.url} (${result.coverArt.quality})")
                    if (downloadAndApplyCoverArt(result.coverArt)) {
                        _coverArtError.value = null
                    } else {
                        _coverArtError.value = "Failed to download cover art"
                    }
                }
                is CoverArtResult.PartialSuccess -> {
                    Log.d("AutoEdit", "Cover art partial success: ${result.coverArt.url} - ${result.fallbackMessage}")
                    if (downloadAndApplyCoverArt(result.coverArt)) {
                        _coverArtError.value = result.fallbackMessage
                    } else {
                        _coverArtError.value = "Failed to download cover art: ${result.fallbackMessage}"
                    }
                }
                is CoverArtResult.NoCoverArt -> {
                    Log.d("AutoEdit", "No cover art available")
                    _coverArtError.value = "No cover art available"
                }
                is CoverArtResult.Error -> {
                    Log.e("AutoEdit", "Cover art error: ${result.message}", result.cause)
                    _coverArtError.value = "Failed: ${result.message}"
                }
            }
        }
    }

    /**
     * Legacy method for direct URL download (backward compatibility)
     * @deprecated Use fetchAndApplyCoverArt with releaseId instead
     */
    private fun fetchAndApplyCoverArtLegacy(coverArtUrl: String, releaseGroupId: String? = null) {
        backgroundScope.launch {
            if (tryDownloadCoverArt(coverArtUrl)) return@launch
            // First URL failed, notify immediately
            Log.d("AutoEdit", "fetchAndApplyCoverArt: cover art not available")
            _coverArtError.value = "Cover art not available"
            // Still try fallback silently in background
            if (!releaseGroupId.isNullOrBlank()) {
                val groupUrl = "https://coverartarchive.org/release-group/$releaseGroupId/front"
                Log.d("AutoEdit", "fetchAndApplyCoverArt: fallback to release-group URL: $groupUrl")
                tryDownloadCoverArt(groupUrl)
            }
        }
    }

    fun clearCoverArtError() {
        _coverArtError.value = null
    }

    val coverArtError = _coverArtError.asStateFlow()

    /**
     * Download cover art from a selected URL and apply to editor
     */
    private suspend fun downloadAndApplyCoverArt(coverArt: dev.secam.simpletag.data.musicbrainz.models.SelectedCoverArt): Boolean {
        return tryDownloadCoverArt(coverArt.url)
    }

    private fun tryDownloadCoverArt(url: String): Boolean {
        try {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                Log.d("AutoEdit", "fetchAndApplyCoverArt: url=$url, code=${response.code}")
                if (response.isSuccessful) {
                    val body = response.body
                    if (body == null) {
                        Log.e("AutoEdit", "fetchAndApplyCoverArt: response body is null")
                        return false
                    }
                    val bytes = body.bytes()
                    if (bytes.isEmpty()) {
                        Log.e("AutoEdit", "fetchAndApplyCoverArt: downloaded bytes are empty")
                        return false
                    }
                    val mimeType = body.contentType()?.let { "${it.type}/${it.subtype}" } ?: "image/jpeg"
                    val artwork = org.jaudiotagger.tag.images.AndroidArtwork()
                    artwork.binaryData = bytes
                    artwork.mimeType = mimeType
                    artwork.description = ""
                    artwork.pictureType = org.jaudiotagger.tag.reference.PictureTypes.DEFAULT_ID
                    setArtwork(artwork)
                    Log.d("AutoEdit", "fetchAndApplyCoverArt: success, ${bytes.size} bytes, mimeType=$mimeType")
                    return true
                } else {
                    Log.w("AutoEdit", "fetchAndApplyCoverArt: HTTP ${response.code}, not available")
                    return false
                }
            }
        } catch (e: Exception) {
            Log.e("AutoEdit", "fetchAndApplyCoverArt: failed for $url", e)
            return false
        }
    }

    /**
     * Set the visibility of the auto-edit dialog
     */
    fun setShowAutoEditDialog(show: Boolean) {
        _uiState.update { it.copy(showAutoEditDialog = show) }
    }

    /**
     * Reset auto-edit state to idle
     */
    fun resetAutoEditState() {
        _uiState.update {
            it.copy(
                autoEditState = AutoEditState.Idle,
                autoEditResults = listOf(),
                selectedAutoEditResult = null,
                autoEditTrackItems = listOf(),
                autoEditRecordingItems = listOf()
            )
        }
    }

    /*      Filename Auto Edit Methods     */

    /**
     * Parse filename and show confirm dialog for auto edit
     * @param filePath The full path to the audio file
     */
    fun parseAndShowConfirmDialog(filePath: String) {
        val fileName = File(filePath).name
        val parseResult = FileNameParser.parse(fileName)

        Log.d("AutoEdit", "Parsed filename: $fileName -> artist=${parseResult.artist}, title=${parseResult.title}")

        _uiState.update {
            it.copy(
                showFileNameConfirmDialog = true,
                parsedArtist = parseResult.artist,
                parsedTitle = parseResult.title
            )
        }
    }

    /**
     * Show or hide the filename confirm dialog
     */
    fun setShowFileNameConfirmDialog(show: Boolean) {
        _uiState.update { it.copy(showFileNameConfirmDialog = show) }
    }

    /**
     * Fetch auto edit data from parsed filename (artist and title)
     * @param artist The parsed artist name (can be empty/null)
     * @param title The parsed title
     */
    fun fetchAutoEditFromFilename(artist: String?, title: String) {
        backgroundScope.launch {
            val queryArtist = if (artist.isNullOrBlank()) null else artist
            Log.d("AutoEdit", "fetchAutoEditFromFilename: title=$title, artist=$queryArtist")
            _uiState.update { it.copy(autoEditState = AutoEditState.Loading) }

            val result = musicBrainzRepository.searchRecordings(
                title = title,
                artist = queryArtist
            )

            Log.d("AutoEdit", "Result type: ${result::class.simpleName}")

            when (result) {
                is MusicBrainzResult.Success -> {
                    val recordings = result.data
                    if (recordings.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                autoEditState = AutoEditState.NoResults,
                                autoEditTrackItems = listOf(),
                                showAutoEditDialog = true,
                                showFileNameConfirmDialog = false
                            )
                        }
                    } else {
                        val trackItems = buildAutoEditTrackItemsFromRecordings(recordings, title)
                        _uiState.update {
                            it.copy(
                                autoEditState = AutoEditState.Loading, // Keep loading state, we use custom items
                                autoEditRecordingItems = trackItems,
                                showAutoEditDialog = true,
                                showFileNameConfirmDialog = false
                            )
                        }
                    }
                }
                is MusicBrainzResult.Error -> {
                    Log.e("AutoEdit", "Error: ${result.exception.message}", result.exception)
                    _uiState.update {
                        it.copy(
                            autoEditState = AutoEditState.Error(result.exception.message ?: "Unknown error"),
                            showAutoEditDialog = true,
                            showFileNameConfirmDialog = false
                        )
                    }
                }
                is MusicBrainzResult.NoResults -> {
                    _uiState.update {
                        it.copy(
                            autoEditState = AutoEditState.NoResults,
                            autoEditTrackItems = listOf(),
                            showAutoEditDialog = true,
                            showFileNameConfirmDialog = false
                        )
                    }
                }
            }
        }
    }

    /**
     * Apply the selected track from auto-edit results
     * @param trackItem The AutoEditTrackItem containing release and track
     */
    fun applyAutoEditTrack(trackItem: AutoEditTrackItem) {
        val fieldMap = MusicBrainzMapper.mapTrackToFieldStates(trackItem.release, trackItem.track)

        _uiState.update { currentState ->
            val updatedFieldStates = currentState.fieldStates.toMutableMap()
            var updatedInvisibleTags = currentState.invisibleTags.toMutableSet()
            var updatedDeletedFields = currentState.deletedFields.toMutableSet()

            fieldMap.forEach { (field, value) ->
                val existingField = currentState.fieldStates[field]
                if (existingField != null) {
                    updatedFieldStates[field] = existingField.copy(
                        textState = TextFieldState(value)
                    )
                } else {
                    updatedFieldStates[field] = EditorFieldState(
                        textState = TextFieldState(value),
                        enabledState = mutableStateOf(false)
                    )
                    updatedInvisibleTags.remove(field)
                    updatedDeletedFields.remove(field)
                }
            }

            currentState.copy(
                fieldStates = updatedFieldStates,
                invisibleTags = updatedInvisibleTags,
                deletedFields = updatedDeletedFields
            )
        }

        setChangesMade(true)
        setShowAutoEditDialog(false)

        // Fetch cover art using new repository with quality selection
        fetchAndApplyCoverArt(
            releaseId = trackItem.release.id,
            releaseGroupId = trackItem.release.releaseGroupId,
            coverArtInfo = trackItem.release.coverArtInfo
        )
    }

    /**
     * Build a list of AutoEditTrackItems from releases, sorted by relevance to search title
     * @param releases List of MusicBrainz releases
     * @param searchTitle The title used for searching (for sorting relevance)
     * @return Flattened list of all tracks from all releases, sorted by relevance
     */
    private fun buildAutoEditTrackItems(
        releases: List<dev.secam.simpletag.data.musicbrainz.models.MusicBrainzRelease>,
        searchTitle: String
    ): List<AutoEditTrackItem> {
        val allTracks = mutableListOf<AutoEditTrackItem>()

        for (release in releases) {
            for (track in release.tracks) {
                allTracks.add(AutoEditTrackItem(release, track))
            }
        }

        // Sort: exact title match first, then contains match, then rest
        allTracks.sortWith { item1, item2 ->
            val title1 = item1.track.title.lowercase()
            val title2 = item2.track.title.lowercase()
            val searchLower = searchTitle.lowercase()

            val exact1 = title1 == searchLower
            val exact2 = title2 == searchLower
            val contains1 = title1.contains(searchLower)
            val contains2 = title2.contains(searchLower)

            when {
                exact1 && !exact2 -> -1
                !exact1 && exact2 -> 1
                exact1 && exact2 -> 0
                contains1 && !contains2 -> -1
                !contains1 && contains2 -> 1
                contains1 && contains2 -> 0
                else -> 0
            }
        }

        return allTracks
    }

    /**
     * Build a list of AutoEditRecordingItem from recordings, sorted by relevance to search title
     * @param recordings List of MusicBrainzRecording
     * @param searchTitle The title used for searching (for sorting relevance)
     * @return List of recording items sorted by relevance
     */
    private fun buildAutoEditTrackItemsFromRecordings(
        recordings: List<MusicBrainzRecording>,
        searchTitle: String
    ): List<AutoEditRecordingItem> {
        val recordingItems: MutableList<AutoEditRecordingItem> = recordings.map { recording ->
            // Prefer the first release (usually the original)
            val preferredRelease = recording.releases.firstOrNull()
            AutoEditRecordingItem(recording, preferredRelease)
        }.toMutableList()

        // Sort: exact title match first, then contains match, then rest
        recordingItems.sortWith { item1: AutoEditRecordingItem, item2: AutoEditRecordingItem ->
            val title1 = item1.recording.title.lowercase()
            val title2 = item2.recording.title.lowercase()
            val searchLower = searchTitle.lowercase()

            val exact1 = title1 == searchLower
            val exact2 = title2 == searchLower
            val contains1 = title1.contains(searchLower)
            val contains2 = title2.contains(searchLower)

            when {
                exact1 && !exact2 -> -1
                !exact1 && exact2 -> 1
                exact1 && exact2 -> 0
                contains1 && !contains2 -> -1
                !contains1 && contains2 -> 1
                contains1 && contains2 -> 0
                else -> 0
            }
        }

        return recordingItems
    }

    /**
     * Apply the selected recording from auto-edit results
     * @param recordingItem The AutoEditRecordingItem containing recording and optional release
     */
    fun applyAutoEditRecording(recordingItem: AutoEditRecordingItem) {
        val recording = recordingItem.recording
        val release = recordingItem.release

        _uiState.update { currentState ->
            val updatedFieldStates = currentState.fieldStates.toMutableMap()
            var updatedInvisibleTags = currentState.invisibleTags.toMutableSet()
            var updatedDeletedFields = currentState.deletedFields.toMutableSet()

            // Set title from recording
            val titleField = currentState.fieldStates[SimpleTagField.Title]
            if (titleField != null) {
                updatedFieldStates[SimpleTagField.Title] = titleField.copy(
                    textState = TextFieldState(recording.title)
                )
            } else {
                updatedFieldStates[SimpleTagField.Title] = EditorFieldState(
                    textState = TextFieldState(recording.title),
                    enabledState = mutableStateOf(false)
                )
                updatedInvisibleTags.remove(SimpleTagField.Title)
                updatedDeletedFields.remove(SimpleTagField.Title)
            }

            // Set artist from recording
            val artistField = currentState.fieldStates[SimpleTagField.Artist]
            if (artistField != null) {
                updatedFieldStates[SimpleTagField.Artist] = artistField.copy(
                    textState = TextFieldState(recording.artist)
                )
            } else {
                updatedFieldStates[SimpleTagField.Artist] = EditorFieldState(
                    textState = TextFieldState(recording.artist),
                    enabledState = mutableStateOf(false)
                )
                updatedInvisibleTags.remove(SimpleTagField.Artist)
                updatedDeletedFields.remove(SimpleTagField.Artist)
            }

            // Set album from release if available
            if (release != null) {
                val albumField = currentState.fieldStates[SimpleTagField.Album]
                if (albumField != null) {
                    updatedFieldStates[SimpleTagField.Album] = albumField.copy(
                        textState = TextFieldState(release.title)
                    )
                } else {
                    updatedFieldStates[SimpleTagField.Album] = EditorFieldState(
                        textState = TextFieldState(release.title),
                        enabledState = mutableStateOf(false)
                    )
                    updatedInvisibleTags.remove(SimpleTagField.Album)
                    updatedDeletedFields.remove(SimpleTagField.Album)
                }

                // Set year if available
                if (release.date != null) {
                    val year = release.date.substringBefore("-")
                    val yearField = currentState.fieldStates[SimpleTagField.Year]
                    if (yearField != null) {
                        updatedFieldStates[SimpleTagField.Year] = yearField.copy(
                            textState = TextFieldState(year)
                        )
                    } else {
                        updatedFieldStates[SimpleTagField.Year] = EditorFieldState(
                            textState = TextFieldState(year),
                            enabledState = mutableStateOf(false)
                        )
                        updatedInvisibleTags.remove(SimpleTagField.Year)
                        updatedDeletedFields.remove(SimpleTagField.Year)
                    }
                }
            }

            // Set MusicBrainz IDs
            recording.artistId?.let { artistId ->
                val artistIdField = currentState.fieldStates[SimpleTagField.MusicBrainzArtistId]
                if (artistIdField != null) {
                    updatedFieldStates[SimpleTagField.MusicBrainzArtistId] = artistIdField.copy(
                        textState = TextFieldState(artistId)
                    )
                } else {
                    updatedFieldStates[SimpleTagField.MusicBrainzArtistId] = EditorFieldState(
                        textState = TextFieldState(artistId),
                        enabledState = mutableStateOf(false)
                    )
                    updatedInvisibleTags.remove(SimpleTagField.MusicBrainzArtistId)
                    updatedDeletedFields.remove(SimpleTagField.MusicBrainzArtistId)
                }
            }

            currentState.copy(
                fieldStates = updatedFieldStates,
                invisibleTags = updatedInvisibleTags,
                deletedFields = updatedDeletedFields
            )
        }

        setChangesMade(true)
        setShowAutoEditDialog(false)

        // Fetch cover art from recording's release
        // Note: MusicBrainzRecordingRelease has limited fields, use legacy method
        if (release != null) {
            fetchAndApplyCoverArt(
                directUrl = release.coverArtUrl
            )
        }
    }
}

data class EditorUiState(
    val initialized: Boolean = false,
    val artwork: Artwork? = null,
    val lyrics: String? = null,
    val fieldStates: Map<SimpleTagField, EditorFieldState> = mapOf(),
    val editorMusicList: List<MusicData> = listOf(),
    val artworkChanged: Boolean = false,

    val savedFields: List<String> = listOf(),
    val savedLyrics: String = "",
    val log: String = "",
    val tagNames: Map<SimpleTagField, String> = mapOf(),
    val invisibleTags: Set<SimpleTagField> = setOf(),
    val searchResults: List<SimpleTagField> = listOf(),
    val deletedFields: Set<SimpleTagField> = setOf(),
    val artworkEnabled: Boolean = false,
    val changesMade: Boolean = false,
    /*      Show Dialogs     */
    val showBackDialog: Boolean = false,
    val showSaveDialog: Boolean = false,
    val showLogDialog: Boolean = false,
    val showHelpDialog: Boolean = false,
    val showAddFieldDialog: Boolean = false,
    val showLyricsSheet: Boolean = false,
    val showSongSyncMissingDialog: Boolean = false,

    /*      Auto Edit     */
    val autoEditState: AutoEditState = AutoEditState.Idle,
    val autoEditResults: List<dev.secam.simpletag.data.musicbrainz.models.MusicBrainzRelease> = listOf(),
    val selectedAutoEditResult: dev.secam.simpletag.data.musicbrainz.models.MusicBrainzRelease? = null,
    val showAutoEditDialog: Boolean = false,

    /*      Filename Auto Edit     */
    val showFileNameConfirmDialog: Boolean = false,
    val parsedArtist: String? = null,
    val parsedTitle: String = "",
    val autoEditTrackItems: List<AutoEditTrackItem> = listOf(),
    val autoEditRecordingItems: List<AutoEditRecordingItem> = listOf(),
)

data class EditorFieldState(
    val textState: TextFieldState,
    val enabledState: MutableState<Boolean> //= mutableStateOf(false)
)

