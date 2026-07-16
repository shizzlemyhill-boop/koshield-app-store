package com.koshield.appstore

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.koshield.appstore.databinding.ItemAppBinding

/**
 * Renders the list of catalog apps. The button label adapts to installed state:
 * Install (not installed), Update (newer version available), or Open (up to date).
 */
class AppAdapter(
    private val onInstallOrUpdate: (AppItem) -> Unit,
    private val onOpen: (AppItem) -> Unit,
    private val installedVersionCode: (String) -> Long?
) : RecyclerView.Adapter<AppAdapter.VH>() {

    private val items = mutableListOf<AppItem>()
    private val downloading = mutableSetOf<String>()

    fun submit(newItems: List<AppItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun setDownloading(appId: String, isDownloading: Boolean) {
        val changed = if (isDownloading) downloading.add(appId) else downloading.remove(appId)
        if (changed) {
            val idx = items.indexOfFirst { it.id == appId }
            if (idx >= 0) notifyItemChanged(idx)
        }
    }

    /** Call after (un)installs so button labels refresh. */
    fun refreshStates() = notifyDataSetChanged()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemAppBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    inner class VH(private val b: ItemAppBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(app: AppItem) {
            b.appName.text = app.name
            b.appDescription.text = app.description

            val metaParts = mutableListOf<String>()
            if (app.version.isNotBlank()) metaParts.add("v${app.version}")
            app.category?.let { metaParts.add(it) }
            app.size?.let { metaParts.add(it) }
            b.appMeta.text = metaParts.joinToString("  •  ")

            IconLoader.load(app.iconUrl, b.appIcon, R.drawable.placeholder_app)

            val installedVc = installedVersionCode(app.id)
            val isDownloading = downloading.contains(app.id)

            b.itemProgress.visibility = if (isDownloading) android.view.View.VISIBLE else android.view.View.GONE
            b.installButton.isEnabled = !isDownloading

            when {
                installedVc == null -> {
                    b.installButton.text = b.root.context.getString(R.string.install)
                    b.installButton.setOnClickListener { onInstallOrUpdate(app) }
                }
                app.versionCode > installedVc -> {
                    b.installButton.text = b.root.context.getString(R.string.update)
                    b.installButton.setOnClickListener { onInstallOrUpdate(app) }
                }
                else -> {
                    b.installButton.text = b.root.context.getString(R.string.open)
                    b.installButton.setOnClickListener { onOpen(app) }
                }
            }
        }
    }
}
