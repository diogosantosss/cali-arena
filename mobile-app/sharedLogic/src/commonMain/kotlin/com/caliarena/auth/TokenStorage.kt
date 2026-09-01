package com.caliarena.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class TokenStorage(
    private val dataStore: DataStore<Preferences>,
) {
    val token: Flow<String?> = dataStore.data.map { it[KEY_TOKEN] }

    val username: Flow<String?> = dataStore.data.map { it[KEY_USERNAME] }

    suspend fun readToken(): String? = dataStore.data.first()[KEY_TOKEN]

    suspend fun readUsername(): String? = dataStore.data.first()[KEY_USERNAME]

    suspend fun saveSession(
        token: String,
        username: String,
    ) {
        dataStore.edit {
            it[KEY_TOKEN] = token
            it[KEY_USERNAME] = username
        }
    }

    suspend fun clear() {
        dataStore.edit {
            it.remove(KEY_TOKEN)
            it.remove(KEY_USERNAME)
        }
    }

    companion object {
        private val KEY_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_USERNAME = stringPreferencesKey("auth_username")
    }
}

expect fun createTokenDataStore(): DataStore<Preferences>
