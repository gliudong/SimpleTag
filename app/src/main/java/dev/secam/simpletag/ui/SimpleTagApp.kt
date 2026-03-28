
package dev.secam.simpletag.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.TransformOrigin
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.secam.simpletag.data.media.MusicData
import dev.secam.simpletag.ui.editor.BatchAutoEditScreen
import dev.secam.simpletag.ui.editor.EditorScreen
import dev.secam.simpletag.ui.selector.SelectorScreen
import dev.secam.simpletag.ui.settings.AboutScreen
import dev.secam.simpletag.ui.settings.SettingsScreen
import kotlinx.serialization.json.Json

@Composable
fun SimpleTagApp(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Selector,
        popExitTransition = {
            scaleOut(
                targetScale = 0.9f,
                transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0.5f)
            )
        },
        popEnterTransition = {
            EnterTransition.None
        },
    ) {
        val duration = 300
        composable<Selector>(
            enterTransition = {
                return@composable fadeIn(tween(1000))
            }, exitTransition = {
                return@composable slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start, tween(duration)
                )
            }, popEnterTransition = {
                return@composable slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End, tween(duration)
                )
            }
        ) {
            SelectorScreen(
                onNavigateToSettings = { navController.navigate(Settings) },
                onNavigateToEditor = { musicList: List<MusicData> ->
                    navController.navigate(Editor(Json.encodeToString(musicList)))
                },
                onNavigateToBatchAutoEdit = { musicList: List<MusicData> ->
                    navController.navigate(BatchAutoEdit(Json.encodeToString(musicList)))
                }
            )
        }

        composable<Editor> (
            //typeMap = mapOf(typeOf<List<MusicData>>() to navTypeOf<List<MusicData>>()),
            enterTransition = {
                return@composable slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start, tween(duration)
                )
            },
            exitTransition = {
                return@composable slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start, tween(duration)
                )
            },
            popExitTransition = {
                return@composable slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End, tween(duration)
                )
            },
            popEnterTransition = {
                return@composable slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End, tween(duration)
                )
            }
        ){ backStackEntry ->
            val editor: Editor = backStackEntry.toRoute()
            EditorScreen(
                musicList = Json.decodeFromString(editor.musicList),
                onNavigateBack = {navController.navigateUp()}
            )
        }

        composable<BatchAutoEdit> (
            enterTransition = {
                return@composable slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start, tween(duration)
                )
            },
            exitTransition = {
                return@composable slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start, tween(duration)
                )
            },
            popExitTransition = {
                return@composable slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End, tween(duration)
                )
            },
            popEnterTransition = {
                return@composable slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End, tween(duration)
                )
            }
        ){ backStackEntry ->
            val batchAutoEdit: BatchAutoEdit = backStackEntry.toRoute()
            BatchAutoEditScreen(
                musicList = Json.decodeFromString(batchAutoEdit.musicList),
                onNavigateBack = {navController.navigateUp()}
            )
        }

        composable<Settings> (
            enterTransition = {
                return@composable slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start, tween(duration)
                )
            },
            exitTransition = {
                return@composable slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start, tween(duration)
                )
            },
            popExitTransition = {
                return@composable slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End, tween(duration)
                )
            },
            popEnterTransition = {
                return@composable slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End, tween(duration)
                )
            }
        ){
            SettingsScreen(
                onNavigateBack = {navController.navigateUp()},
                onNavigateToAbout = {navController.navigate(About)}
            )
        }
        composable<About> (
            enterTransition = {
                return@composable slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start, tween(duration)
                )
            },
            exitTransition = {
                return@composable slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start, tween(duration)
                )
            },
            popExitTransition = {
                return@composable slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End, tween(duration)
                )
            },
            popEnterTransition = {
                return@composable slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End, tween(duration)
                )
            }
        ){
            AboutScreen(
                onNavigateBack = {navController.navigateUp()}
            )
        }
    }
}
