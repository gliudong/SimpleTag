
package dev.secam.simpletag.data.coil

import coil3.key.Keyer
import coil3.request.Options
import dev.secam.simpletag.data.media.MusicData

object MusicDataKeyer : Keyer<MusicData> {
    override fun key(data: MusicData, options: Options): String {
        return data.id.toString()
    }
}