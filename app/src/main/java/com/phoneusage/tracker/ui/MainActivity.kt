package com.phoneusage.tracker.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.phoneusage.tracker.data.AppPrefsRepository
import com.phoneusage.tracker.data.UsageStatsRepository
import com.phoneusage.tracker.databinding.ActivityMainBinding
import com.phoneusage.tracker.util.formatDuration
import com.phoneusage.tracker.widget.UsageWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var usageStatsRepository: UsageStatsRepository
    private lateinit var appPrefsRepository: AppPrefsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        usageStatsRepository = UsageStatsRepository(this)
        appPrefsRepository = AppPrefsRepository(this)

        binding.grantAccessButton.setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:$packageName"))
            )
        }
        binding.selectAppsButton.setOnClickListener {
            startActivity(Intent(this, AppSelectionActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        lifecycleScope.launch {
            val hasAccess = withContext(Dispatchers.IO) { usageStatsRepository.hasUsageAccess() }
            binding.permissionCard.isVisible = !hasAccess
            binding.usageContainer.isVisible = hasAccess
            binding.selectAppsButton.isVisible = hasAccess

            if (!hasAccess) return@launch

            val totalMillis = withContext(Dispatchers.IO) {
                val excluded = appPrefsRepository.getExcludedPackagesOnce()
                usageStatsRepository.getTotalUsageMillisToday(excluded)
            }
            binding.totalTimeValue.text = formatDuration(totalMillis)
            UsageWidgetProvider.requestUpdate(this@MainActivity)
        }
    }
}
