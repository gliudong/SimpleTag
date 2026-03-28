
package dev.secam.simpletag.util.tag

import org.jaudiotagger.tag.flac.FlacTag
import org.jaudiotagger.tag.images.Artwork
import org.jaudiotagger.tag.reference.PictureTypes
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentFieldKey
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag
import org.jaudiotagger.tag.vorbiscomment.util.Base64Coder

fun FlacTag.setArtworkField(artwork: Artwork) {
    setField(
        createArtworkField(
            /* imageData = */ artwork.binaryData,
            /* pictureType = */ PictureTypes.DEFAULT_ID,
            /* mimeType = */ artwork.mimeType,
            /* description = */ "cover",
            /* width = */ artwork.width,
            /* height = */ artwork.height,
            /* colourDepth = */ 24,
            /* indexedColouredCount = */ 0
        )
    )
}

fun VorbisCommentTag.setArtworkField(artwork: Artwork) {
    val imageData = artwork.binaryData
    val testdata = Base64Coder.encode(imageData)
    val base64image = String(testdata)
    setField(createField(VorbisCommentFieldKey.COVERART,base64image))
    setField(createField(VorbisCommentFieldKey.COVERARTMIME,artwork.mimeType))
}