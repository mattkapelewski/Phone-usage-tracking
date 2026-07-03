package com.phoneusage.tracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

/**
 * Persists which apps are excluded from the daily usage total. Everything is included
 * by default; a package only shows up here once the user unchecks it.
 */
class AppPrefsRepository(private val context: Context) {

    private object Keys {
        val EXCLUDED_PACKAGES = stringSetPreferencesKey("excluded_packages")
    }

    val excludedPackages: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.EXCLUDED_PACKAGES] ?: emptySet()
    }

    suspend fun getExcludedPackagesOnce(): Set<String> = excludedPackages.first()

    suspend fun setIncluded(packageName: String, included: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.EXCLUDED_PACKAGES] ?: emptySet()
            prefs[Keys.EXCLUDED_PACKAGES] = if (included) {
                current - packageName
            } else {
                current + packageName
            }
        }
    }
}
