
package dev.secam.simpletag.data.preferences

import dev.secam.simpletag.data.enums.AppColorScheme
import dev.secam.simpletag.data.enums.AppTheme
import dev.secam.simpletag.data.enums.FolderSelectMode
import dev.secam.simpletag.data.enums.SortDirection
import dev.secam.simpletag.data.enums.SortOrder

data class UserPreferences(
    val theme: AppTheme = AppTheme.System,
    val colorScheme: AppColorScheme = AppColorScheme.Dynamic,
    val pureBlack: Boolean = false,
    val simpleEditor: Boolean = false,
    val roundCovers: Boolean = true,
    val systemFont: Boolean = false,
    val optionalPermissionsSkipped: Boolean? = null,
    val rememberSort: Boolean = false,
    val sortOrder: SortOrder = SortOrder.Title,
    val sortDirection: SortDirection = SortDirection.Ascending,
    val selectMode: FolderSelectMode = FolderSelectMode.Include,
    val selectedList: Set<String> = setOf()
)