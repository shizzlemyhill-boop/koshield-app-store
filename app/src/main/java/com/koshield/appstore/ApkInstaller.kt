package com.koshield.appstore

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

/**
 * Handles downloading an APK via the system DownloadManager and launching the
 * package installer. Requires the REQUEST_INSTALL_PACKAGES permission and that
 * the user has allowed this app to install unknown apps (handled with a prompt).
 */
class ApkInstaller(
    private val activity: AppCompatActivity,
    private val onDownloadStateChange: (appId: String, downloading: Boolean) -> Unit
) {
    private val dm = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val downloadIdToApp = HashMap<Long, AppItem>()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            val app = downloadIdToApp.remove(id) ?: return
            onDownloadStateChange(app.id, false)
            if (isDownloadSuccessful(id)) {
                installApk(app)
            } else {
                Toast.makeText(
                    activity,
                    activity.getString(R.string.download_failed, app.name),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun register() {
        ContextCompat.registerReceiver(
            activity,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    fun unregister() {
        runCatching { activity.unregisterReceiver(receiver) }
    }

    fun startInstall(app: AppItem) {
        if (!canRequestInstalls()) {
            promptEnableUnknownSources()
            return
        }
        val target = apkFile(app)
        target.parentFile?.mkdirs()
        if (target.exists()) target.delete()

        val request = DownloadManager.Request(Uri.parse(app.apkUrl))
            .setTitle(app.name)
            .setDescription(activity.getString(R.string.downloading, app.name))
            .setDestinationInExternalFilesDir(activity, null, "downloads/${app.id}.apk")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val id = dm.enqueue(request)
        downloadIdToApp[id] = app
        onDownloadStateChange(app.id, true)
        Toast.makeText(
            activity,
            activity.getString(R.string.downloading, app.name),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun apkFile(app: AppItem): File =
        File(activity.getExternalFilesDir(null), "downloads/${app.id}.apk")

    private fun isDownloadSuccessful(id: Long): Boolean {
        dm.query(DownloadManager.Query().setFilterById(id)).use { c ->
            if (c != null && c.moveToFirst()) {
                val statusCol = c.getColumnIndex(DownloadManager.COLUMN_STATUS)
                return statusCol >= 0 &&
                        c.getInt(statusCol) == DownloadManager.STATUS_SUCCESSFUL
            }
        }
        return false
    }

    private fun installApk(app: AppItem) {
        val file = apkFile(app)
        if (!file.exists()) {
            Toast.makeText(
                activity,
                activity.getString(R.string.download_failed, app.name),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val uri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }

    private fun canRequestInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    private fun promptEnableUnknownSources() {
        AlertDialog.Builder(activity)
            .setTitle(R.string.app_name)
            .setMessage(R.string.enable_unknown_sources)
            .setPositiveButton("Open settings") { _, _ ->
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${activity.packageName}")
                    )
                } else {
                    Intent(Settings.ACTION_SECURITY_SETTINGS)
                }
                runCatching { activity.startActivity(intent) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
