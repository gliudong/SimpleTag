
package dev.secam.simpletag.data.preferences

import dev.secam.simpletag.data.enums.AppColorScheme
import dev.secam.simpletag.data.enums.AppTheme
import dev.secam.simpletag.data.enums.FolderSelectMode
import dev.secam.simpletag.data.enums.SortDirection
import dev.secam.simpletag.data.enums.SortOrder
import kotlinx.coroutines.flow.Flow

interface PreferencesRepo {
    val preferencesFlow: Flow<UserPreferences>
    suspend fun saveThemePref(theme: AppTheme)
    suspend fun saveColorSchemePref(colorScheme: AppColorScheme)
    suspend fun savePureBlackPref(pureBlack: Boolean)
    suspend fun saveSimpleEditorPref(simpleEditor: Boolean)
    suspend fun saveRoundCoversPref(roundCovers: Boolean)
    suspend fun saveSystemFontPref(systemFont: Boolean)
    suspend fun saveOptionalPermissionsSkipped(optionalPermissionsSkipped: Boolean)
    suspend fun saveRememberSort(rememberSort: Boolean)
    suspend fun saveSortOrder(sortOrder: SortOrder)
    suspend fun saveSortDirection(sortDirection: SortDirection)
    suspend fun saveSelectMode(selectMode: FolderSelectMode)
    suspend fun saveSelectedList(selectedList: Set<String>)
}