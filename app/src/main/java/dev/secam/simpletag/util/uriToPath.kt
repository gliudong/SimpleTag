
package dev.secam.simpletag.util

import android.net.Uri

fun uriToPath(uri: Uri): String {
    return uri.path?.replace("/tree/primary:", "/storage/emulated/0/") ?: ""
}