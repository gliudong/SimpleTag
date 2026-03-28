
package dev.secam.simpletag.ui

import kotlinx.serialization.Serializable


@Serializable
object Selector

@Serializable
object Settings

@Serializable
object About

@Serializable
data class Editor(
    val musicList: String
)

@Serializable
data class BatchAutoEdit(
    val musicList: String
)


