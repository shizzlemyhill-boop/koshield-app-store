package com.koshield.appstore

import org.json.JSONObject

/**
 * A single app listing from the remote catalog.
 * `id` must equal the app's real package name (applicationId) so the store
 * can detect whether it is already installed and offer Update / Open.
 */
data class AppItem(
    val id: String,
    val name: String,
    val version: String,
    val versionCode: Long,
    val description: String,
    val iconUrl: String?,
    val apkUrl: String,
    val category: String?,
    val size: String?
) {
    companion object {
        fun fromJson(o: JSONObject): AppItem {
            return AppItem(
                id = o.optString("id").ifBlank { o.optString("packageName") },
                name = o.optString("name", "Untitled"),
                version = o.optString("version", ""),
                versionCode = o.optLong("versionCode", 0L),
                description = o.optString("description", ""),
                iconUrl = o.optString("iconUrl", "").ifBlank { null },
                apkUrl = o.optString("apkUrl", ""),
                category = o.optString("category", "").ifBlank { null },
                size = o.optString("size", "").ifBlank { null }
            )
        }
    }
}
