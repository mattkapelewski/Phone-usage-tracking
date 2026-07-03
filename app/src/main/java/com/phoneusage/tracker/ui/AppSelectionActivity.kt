package com.phoneusage.tracker.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.phoneusage.tracker.data.AppPrefsRepository
import com.phoneusage.tracker.data.AppUsageItem
import com.phoneusage.tracker.data.UsageStatsRepository
import com.phoneusage.tracker.databinding.ActivityAppSelectionBinding
import com.phoneusage.tracker.widget.UsageWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Lets the user choose which installed apps count toward the daily total. */
class AppSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppSelectionBinding
    private lateinit var usageStatsRepository: UsageStatsRepository
    private lateinit var appPrefsRepository: AppPrefsRepository
    private lateinit var adapter: AppSelectionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        usageStatsRepository = UsageStatsRepository(this)
        appPrefsRepository = AppPrefsRepository(this)

        adapter = AppSelectionAdapter { item, included ->
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    appPrefsRepository.setIncluded(item.app.packageName, included)
                }
                UsageWidgetProvider.requestUpdate(this@AppSelectionActivity)
            }
        }

        binding.appList.layoutManager = LinearLayoutManager(this)
        binding.appList.adapter = adapter

        loadApps()
    }

    private fun loadApps() {
        binding.emptyState.isVisible = true
        lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                val apps = usageStatsRepository.getLaunchableApps()
                val usageByPackage = usageStatsRepository.getUsageTodayByPackage()
                val excluded = appPrefsRepository.getExcludedPackagesOnce()
                apps.map { app ->
                    AppUsageItem(
                        app = app,
                        usageMillisToday = usageByPackage[app.packageName] ?: 0L,
                        isIncluded = app.packageName !in excluded
                    )
                }.sortedByDescending { it.usageMillisToday }
            }
            binding.emptyState.isVisible = false
            adapter.submitList(items)
        }
    }
}
