package com.koshield.appstore

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.preference.PreferenceManager
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Fetches and parses the remote catalog.json.
 *
 * Expected JSON shape:
 * {
 *   "store": { "name": "KoShield App Store" },
 *   "apps": [ { "id": "...", "name": "...", "apkUrl": "..." }, ... ]
 * }
 */
object CatalogRepository {

    // Change this default, or set it in the app's Settings screen at runtime.
    const val DEFAULT_CATALOG_URL =
        "https://raw.githubusercontent.com/YOUR-USERNAME/koshield-catalog/main/catalog.json"

    const val PREF_CATALOG_URL = "catalog_url"

    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun catalogUrl(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val saved = prefs.getString(PREF_CATALOG_URL, null)
        return if (saved.isNullOrBlank()) DEFAULT_CATALOG_URL else saved.trim()
    }

    /** Loads the catalog off the main thread and delivers the result on the main thread. */
    fun load(context: Context, onResult: (Result<List<AppItem>>) -> Unit) {
        val url = catalogUrl(context)
        io.execute {
            val result = runCatching { fetch(url) }
            main.post { onResult(result) }
        }
    }

    private fun fetch(urlString: String): List<AppItem> {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 15000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            instanceFollowRedirects = true
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                throw RuntimeException("Server returned HTTP $code")
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(text)
            val appsArray = root.optJSONArray("apps") ?: return emptyList()
            val list = ArrayList<AppItem>(appsArray.length())
            for (i in 0 until appsArray.length()) {
                val obj = appsArray.optJSONObject(i) ?: continue
                val item = AppItem.fromJson(obj)
                if (item.apkUrl.isNotBlank() && item.id.isNotBlank()) list.add(item)
            }
            return list
        } finally {
            conn.disconnect()
        }
    }
}
