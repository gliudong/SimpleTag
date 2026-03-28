
package dev.secam.simpletag.data.coil

import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import dev.secam.simpletag.data.media.MusicData
import dev.secam.simpletag.util.tag.simpleFileReader
import javax.inject.Inject

class MusicDataFetcher(private val musicData: MusicData): Fetcher {
    override suspend fun fetch(): FetchResult? {
        val artwork = simpleFileReader(musicData.path)?.tag?.firstArtwork?.binaryData

        return if(artwork == null) {
            null
        } else {
            val bitmap = BitmapFactory.decodeByteArray(artwork, 0, artwork.size)
            ImageFetchResult(
                image = bitmap.scale(120, 120, false).asImage(),
                isSampled = false,
                dataSource = DataSource.DISK
            )
        }
    }

    class Factory @Inject constructor() : Fetcher.Factory<MusicData> {
        override fun create(data: MusicData, options: Options, imageLoader: ImageLoader): Fetcher? {
            return MusicDataFetcher(data)
        }
    }
}

