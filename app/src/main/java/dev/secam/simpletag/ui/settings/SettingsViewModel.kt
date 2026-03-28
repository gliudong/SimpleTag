
package dev.secam.simpletag.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.secam.simpletag.R
import dev.secam.simpletag.data.enums.AppColorScheme
import dev.secam.simpletag.data.enums.AppTheme
import dev.secam.simpletag.data.enums.FolderSelectMode
import dev.secam.simpletag.data.media.MediaRepo
import dev.secam.simpletag.data.preferences.PreferencesRepo
import dev.secam.simpletag.data.preferences.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepo: PreferencesRepo,
    private val mediaRepo: MediaRepo
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val prefState = preferencesRepo.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserPreferences()
    )
    val uiState = _uiState.asStateFlow()

    /*      Preferences     */
    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            preferencesRepo.saveThemePref(theme)
        }
    }

    fun setColorScheme(colorScheme: AppColorScheme) {
        viewModelScope.launch {
            preferencesRepo.saveColorSchemePref(colorScheme)
        }
    }

    fun setPureBlack(pureBlack: Boolean) {
        viewModelScope.launch {
            preferencesRepo.savePureBlackPref(pureBlack)
        }
    }

    fun setSimpleEditor(simpleEditor: Boolean) {
        viewModelScope.launch {
            preferencesRepo.saveSimpleEditorPref(simpleEditor)
        }
    }

    fun setRoundCovers(roundCovers: Boolean) {
        viewModelScope.launch {
            preferencesRepo.saveRoundCoversPref(roundCovers)
        }
    }
    fun setSystemFont(systemFont: Boolean) {
        viewModelScope.launch {
            preferencesRepo.saveSystemFontPref(systemFont)
        }
    }
    fun setRememberSort(rememberSort: Boolean) {
        viewModelScope.launch {
            preferencesRepo.saveRememberSort(rememberSort)
        }
    }

    fun setSelectMode(selectMode: FolderSelectMode) {
        viewModelScope.launch {
            preferencesRepo.saveSelectMode(selectMode)
        }
    }
    fun setSelectedList(selectedList: Set<String>) {
        viewModelScope.launch {
            preferencesRepo.saveSelectedList(selectedList)
        }
    }
    fun updateFilters(selectedList: Set<String>, selectMode: FolderSelectMode){
        viewModelScope.launch {
            mediaRepo.updatePathFilter(selectedList, selectMode)
            mediaRepo.updateVersion()
        }
    }



    /*      UI State     */
    fun setShowThemeDialog(showThemeDialog: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                showThemeDialog = showThemeDialog
            )
        }
    }
    fun setShowColorSchemeDialog(showColorSchemeDialog: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                showColorSchemeDialog = showColorSchemeDialog
            )
        }
    }

    fun setShowFolderPickerDialog(showFolderPickerDialog: Boolean){
        _uiState.update { currentState ->
            currentState.copy(
                showFolderPickerDialog = showFolderPickerDialog
            )
        }
    }



    @Composable
    fun getThemeIcon(theme: AppTheme): Painter {
        return when (theme) {
            AppTheme.System -> painterResource(R.drawable.ic_system_theme_24px)
            AppTheme.Dark -> painterResource(R.drawable.ic_dark_mode_24px)
            AppTheme.Light -> painterResource(R.drawable.ic_light_mode_24px)
        }
    }
}

data class SettingsUiState(
    val showThemeDialog: Boolean = false,
    val showColorSchemeDialog: Boolean = false,
    val showFolderPickerDialog: Boolean = false
)
