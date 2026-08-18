package com.example.nightguard.data

import android.content.Context
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

    suspend fun saveWhitelist(
        packages: Set<String>
    ) {

        context.dataStore.edit { preferences ->

            preferences[WHITELIST_KEY] =
                packages
        }
    }

    suspend fun getWhitelist(): Set<String> {

        val preferences =
            context.dataStore.data.first()

        return preferences[WHITELIST_KEY]
            ?: emptySet()
    }

    fun whitelistFlow(): Flow<Set<String>> {

        return context.dataStore.data.map { preferences ->

            preferences[WHITELIST_KEY]
                ?: emptySet()
        }
    }
}