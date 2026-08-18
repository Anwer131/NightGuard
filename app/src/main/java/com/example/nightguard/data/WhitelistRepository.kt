package com.example.nightguard.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(
    name = "nightguard_preferences"
)

class WhitelistRepository(
    private val context: Context
) {

    companion object {

        private val WHITELIST_KEY =
            stringSetPreferencesKey("night_whitelist")
    }

    /**
     * Apps that should ALWAYS remain accessible,
     * regardless of Night Mode.
     */
    private fun getDefaultWhitelist(): Set<String> {

        val defaults =
            mutableSetOf<String>()

        val packageManager =
            context.packageManager

        // ------------------------------------------------
        // NightGuard itself
        // ------------------------------------------------

        defaults.add(context.packageName)

        // ------------------------------------------------
        // Default Launcher
        // ------------------------------------------------

        val launcherIntent =
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }

        packageManager
            .resolveActivity(
                launcherIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            ?.activityInfo
            ?.packageName
            ?.let { packageName ->

                defaults.add(packageName)
            }

        // ------------------------------------------------
        // Phone / Dialer
        // ------------------------------------------------

        val dialIntent =
            Intent(Intent.ACTION_DIAL)

        packageManager
            .resolveActivity(
                dialIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            ?.activityInfo
            ?.packageName
            ?.let { packageName ->

                defaults.add(packageName)
            }

        // ------------------------------------------------
        // Android System UI
        //
        // Required for:
        // - Notification shade
        // - Quick settings
        // - System navigation
        // ------------------------------------------------

        defaults.add("com.android.systemui")

        return defaults
    }

    /**
     * Save user-selected whitelist.
     *
     * Default apps are automatically added.
     */
    suspend fun saveWhitelist(
        packages: Set<String>
    ) {

        val protectedPackages =
            packages.toMutableSet()

        // NightGuard can never be removed
        protectedPackages.add(
            context.packageName
        )

        context.dataStore.edit { preferences ->

            preferences[WHITELIST_KEY] =
                protectedPackages
        }
    }

    /**
     * Get whitelist including mandatory default apps.
     */
    suspend fun getWhitelist(): Set<String> {

        val preferences =
            context.dataStore.data.first()

        val savedWhitelist =
            preferences[WHITELIST_KEY]
                ?: emptySet()

        return savedWhitelist + getDefaultWhitelist()
    }

    /**
     * Observe whitelist changes.
     */
    fun whitelistFlow(): Flow<Set<String>> {

        return context.dataStore.data.map { preferences ->

            val savedWhitelist =
                preferences[WHITELIST_KEY]
                    ?: emptySet()

            savedWhitelist + getDefaultWhitelist()
        }
    }
}