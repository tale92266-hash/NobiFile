package com.nobifile.manager.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ThumbnailCache {
    private val cache = object : LruCache<String, Bitmap>(32 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    suspend fun load(context: Context, path: String, file: File?, uri: Uri?): Bitmap? {
        cache.get(path)?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val bmp: Bitmap? = when {
                    uri != null -> {
                        try {
                            if (android.os.Build.VERSION.SDK_INT >= 29) {
                                context.contentResolver.loadThumbnail(uri, android.util.Size(256, 256), null)
                            } else null
                        } catch (e: Exception) { null }
                    }
                    file != null && file.exists() -> {
                        val ext = file.name.substringAfterLast('.', "").lowercase()
                        when (ext) {
                            "mp4", "mkv", "webm", "mov", "avi", "3gp", "m4v" -> videoFrame(file, 256)
                            "jpg", "jpeg", "png", "webp", "gif", "bmp" -> decodeSampled(file, 256)
                            else -> null
                        }
                    }
                    else -> null
                }
                if (bmp != null) cache.put(path, bmp)
                bmp
            } catch (e: Exception) { null }
        }
    }

    /** First video frame as a thumbnail. BitmapFactory cannot decode video,
     * so videos previously never got thumbnails at all. */
    private fun videoFrame(file: File, req: Int): Bitmap? {
        var retriever: android.media.MediaMetadataRetriever? = null
        return try {
            retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val frame = retriever.getFrameAtTime(1_000_000, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: return null
            if (frame.width <= req && frame.height <= req) return frame
            val scale = req.toFloat() / maxOf(frame.width, frame.height)
            val scaled = Bitmap.createScaledBitmap(frame, (frame.width * scale).toInt(), (frame.height * scale).toInt(), true)
            if (scaled !== frame) frame.recycle()
            scaled
        } catch (e: Exception) {
            null
        } finally {
            try { retriever?.release() } catch (e: Exception) { }
        }
    }

    private fun decodeSampled(file: File, req: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        var sample = 1
        while ((opts.outWidth / sample) > req || (opts.outHeight / sample) > req) sample *= 2
        val o2 = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.absolutePath, o2)
    }

    fun clear() { cache.evictAll() }
}
