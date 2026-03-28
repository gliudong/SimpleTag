
package dev.secam.simpletag.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import dagger.hilt.android.AndroidEntryPoint
import dev.secam.simpletag.data.preferences.PreferencesRepo
import dev.secam.simpletag.ui.theme.SimpleTagTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var preferencesRepo: PreferencesRepo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefState = preferencesRepo.preferencesFlow.collectAsState(null).value
            SimpleTagTheme(
                appTheme = prefState?.theme,
                appColorScheme = prefState?.colorScheme,
                pureBlack = prefState?.pureBlack,
                systemFont = prefState?.systemFont
            ) {
                SimpleTagApp()
            }
        }
    }
}