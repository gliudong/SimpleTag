
package dev.secam.simpletag

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import dev.secam.simpletag.data.coil.myImageLoader

@HiltAndroidApp
class SimpleTag : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader = myImageLoader(context)
}