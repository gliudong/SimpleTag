
package dev.secam.simpletag.util

import java.io.File

fun File.getMimeType(): String{
    return when(extension.lowercase()) {
        "mp3" ->
            "audio/mpeg"
        "mp4", "m4a", "m4p" ->
            "audio/mp4"
        "wav", "wave"->
            "audio/wav"
        "dsf" ->
            "audio/x-dsf"
        "aiff", "aif", "aifc" ->
            "audio/aiff"
        "wma" ->
            "audio/x-ms-wma"
        "ogg" ->
            "audio/ogg"
        "flac" ->
            "audio/flac"
        "aac" ->
            "audio/aac"
        else -> "audio/mpeg"
    }
}