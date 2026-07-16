package com.koshield.appstore

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.koshield.appstore.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: AppAdapter
    private lateinit var installer: ApkInstaller

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_refresh -> { loadCatalog(); true }
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java)); true
                }
                else -> false
            }
        }

        installer = ApkInstaller(this) { appId, downloading ->
            adapter.setDownloading(appId, downloading)
        }

        adapter = AppAdapter(
            onInstallOrUpdate = { installer.startInstall(it) },
            onOpen = { openApp(it) },
            installedVersionCode = { installedVersionCode(it) }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.swipeRefresh.setColorSchemeResources(R.color.ko_primary, R.color.ko_accent)
        binding.swipeRefresh.setOnRefreshListener { loadCatalog() }

        loadCatalog()
    }

    override fun onResume() {
        super.onResume()
        installer.register()
        // Installed state may have changed while we were away.
        if (::adapter.isInitialized) adapter.refreshStates()
    }

    override fun onPause() {
        super.onPause()
        installer.unregister()
    }

    private fun loadCatalog() {
        setLoading(true)
        CatalogRepository.load(this) { result ->
            setLoading(false)
            binding.swipeRefresh.isRefreshing = false
            result.onSuccess { apps ->
                adapter.submit(apps)
                binding.emptyView.visibility =
                    if (apps.isEmpty()) View.VISIBLE else View.GONE
            }.onFailure {
                binding.emptyView.visibility = View.VISIBLE
                binding.emptyView.text = getString(R.string.loading_error)
                Toast.makeText(this, R.string.loading_error, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        // Only show the centered spinner when there is nothing on screen yet.
        val showSpinner = loading && adapter.itemCount == 0 && !binding.swipeRefresh.isRefreshing
        binding.progressBar.visibility = if (showSpinner) View.VISIBLE else View.GONE
        if (loading) binding.emptyView.visibility = View.GONE
    }

    private fun openApp(app: AppItem) {
        val launch = packageManager.getLaunchIntentForPackage(app.id)
        if (launch != null) {
            startActivity(launch)
        } else {
            Toast.makeText(this, "${app.name} is installed but has no launcher.", Toast.LENGTH_SHORT).show()
        }
    }

    /** Returns the installed versionCode for a package, or null if not installed. */
    private fun installedVersionCode(packageName: String): Long? {
        return try {
            val info = packageManager.getPackageInfo(packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
}
