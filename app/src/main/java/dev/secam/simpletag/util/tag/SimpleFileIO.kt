
package dev.secam.simpletag.util.tag

import android.content.Context
import android.util.Log
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.mp4.Mp4FileReader
import org.jaudiotagger.audio.mp4.Mp4FileWriter
import org.jaudiotagger.audio.ogg.OggFileReader
import org.jaudiotagger.audio.ogg.OggFileWriter
import org.jaudiotagger.audio.opus.OpusFileReader
import org.jaudiotagger.audio.opus.OpusFileWriter
import java.io.File

fun simpleFileReader(path: String): AudioFile? {
    return try {
        when {
            path.endsWith("aac", true) ->
                Mp4FileReader().read(File(path))
            path.endsWith("opus", true) ->
                OpusFileReader().read(File(path))
            else ->
                AudioFileIO.read(File(path))
        }
    } catch (e: CannotReadException) {
        e.printStackTrace()
        null
    }
}

/**
 * for ogg/vorbis use [oggFileWriter] instead
 */
fun simpleFileWriter(file: AudioFile) {
    when {
        file.file.path.endsWith("aac", true) ->
            Mp4FileWriter().write(file)
        else ->
            AudioFileIO.write(file)
    }
}

/**
 * need to use this to write ogg as jaudiotagger tries to create temp files which it doesn't have permissions for
 */
fun oggFileWriter(file: AudioFile, context: Context){
    if(file.ext == "opus"){
        opusFileWriter(file, context)
    }
    else {
        val tempFile = File(context.filesDir, "ogg_temp")
        tempFile.writeBytes(file.file.readBytes())
        val tempAF = OggFileReader().read(tempFile)
        tempAF.tag = file.tag
        Log.d("oggWriter", tempAF.toString())
        OggFileWriter().write(tempAF)
        file.file.writeBytes(tempFile.readBytes())
        tempFile.delete()
    }
}

fun opusFileWriter(file: AudioFile, context: Context){
    val tempFile = File(context.filesDir, "opus_temp")
    tempFile.writeBytes(file.file.readBytes())
    val tempAF = OpusFileReader().read(tempFile)
    tempAF.tag = file.tag
    Log.d("opusWriter", tempAF.toString())
    OpusFileWriter().write(tempAF)
    file.file.writeBytes(tempFile.readBytes())
    tempFile.delete()
}