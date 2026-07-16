package com.koshield.appstore

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.widget.ImageView
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Tiny image loader for app icons. No third-party dependency required.
 * Caches decoded bitmaps in memory and guards against RecyclerView view reuse
 * by tagging the target ImageView with the URL it is currently loading.
 */
object IconLoader {

    private val io = Executors.newFixedThreadPool(3)
    private val main = Handler(Looper.getMainLooper())

    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 8).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun load(url: String?, into: ImageView, fallbackRes: Int) {
        into.setImageResource(fallbackRes)
        if (url.isNullOrBlank()) {
            into.setTag(R.id.icon_url_tag, null)
            return
        }
        into.setTag(R.id.icon_url_tag, url)

        cache.get(url)?.let {
            into.setImageBitmap(it)
            return
        }

        io.execute {
            val bmp = runCatching { download(url) }.getOrNull()
            if (bmp != null) cache.put(url, bmp)
            main.post {
                // Only apply if this view still wants the same url.
                if (into.getTag(R.id.icon_url_tag) == url && bmp != null) {
                    into.setImageBitmap(bmp)
                }
            }
        }
    }

    private fun download(urlString: String): Bitmap? {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 12000
            instanceFollowRedirects = true
        }
        try {
            if (conn.responseCode !in 200..299) return null
            return conn.inputStream.use { BitmapFactory.decodeStream(it) }
        } finally {
            conn.disconnect()
        }
    }
}
