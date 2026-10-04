package com.example.marvel.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.ImageView
import com.example.marvel.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI

enum class ArchiveImageState { LOADING, READY, MISSING, FAILED }

/** Bounded image cache and downsampling keep long archive lists within mobile memory limits. */
class ArchiveImages {
    private val cache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    fun load(view: ImageView, url: String, description: String, scope: CoroutineScope, onState: (ArchiveImageState) -> Unit = {}) {
        val request = Any()
        view.setTag(R.id.archive_image_request, request)
        view.contentDescription = if (url.isBlank()) "$description. Image unavailable" else description
        view.setImageResource(R.drawable.ic_dossier)
        if (url.isBlank()) { onState(ArchiveImageState.MISSING); return }
        cache.get(url)?.let { view.setImageBitmap(it); onState(ArchiveImageState.READY); return }
        onState(ArchiveImageState.LOADING)
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    val uri = URI(url)
                    require(uri.scheme == "https")
                    val conn = uri.toURL().openConnection() as HttpURLConnection
                    try {
                        conn.connectTimeout = 10_000; conn.readTimeout = 15_000
                        conn.setRequestProperty("User-Agent", "SHIELDArchivesAndroid/1.0")
                        val bytes = conn.inputStream.use { input ->
                            val out = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
                            var count = input.read(buffer)
                            while(count != -1) { check(out.size() + count <= 8 * 1024 * 1024); out.write(buffer, 0, count); count = input.read(buffer) }
                            out.toByteArray()
                        }
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = 1
                            while(bounds.outWidth / inSampleSize > 1024 || bounds.outHeight / inSampleSize > 1024) inSampleSize *= 2
                        }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.also { cache.put(url, it) }
                    } finally { conn.disconnect() }
                }.getOrNull()
            }
            if(view.getTag(R.id.archive_image_request) !== request) return@launch
            if (bitmap != null) {
                view.setImageBitmap(bitmap); onState(ArchiveImageState.READY)
            } else {
                view.contentDescription = "$description. Image unavailable"
                onState(ArchiveImageState.FAILED)
            }
        }
    }
}
