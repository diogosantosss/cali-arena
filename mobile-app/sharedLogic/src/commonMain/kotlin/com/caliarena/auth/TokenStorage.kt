package com.caliarena.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.caliarena.data.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class StoredSession(
    val token: String,
    val username: String,
    val role: UserRole?,
)

class TokenStorage(
    private val dataStore: DataStore<Preferences>,
) {
    val session: Flow<StoredSession?> = dataStore.data.map(::toSession)

    suspend fun readSession(): StoredSession? = toSession(dataStore.data.first())

    suspend fun save(session: StoredSession) {
        dataStore.edit {
            it[KEY_TOKEN] = session.token
            it[KEY_USERNAME] = session.username
            if (session.role != null) {
                it[KEY_ROLE] = session.role.name
            } else {
                it.remove(KEY_ROLE)
            }
        }
    }

    suspend fun clear() {
        dataStore.edit {
            it.remove(KEY_TOKEN)
            it.remove(KEY_USERNAME)
            it.remove(KEY_ROLE)
        }
    }

    private fun toSession(prefs: Preferences): StoredSession? {
        val token = prefs[KEY_TOKEN] ?: return null
        return StoredSession(
            token = token,
            username = prefs[KEY_USERNAME].orEmpty(),
            role = prefs[KEY_ROLE]?.let { toRole(it) },
        )
    }

    private fun toRole(value: String): UserRole? = runCatching { UserRole.valueOf(value) }.getOrNull()

    companion object {
        private val KEY_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_USERNAME = stringPreferencesKey("auth_username")
        private val KEY_ROLE = stringPreferencesKey("auth_role")
    }
}

expect fun createTokenDataStore(): DataStore<Preferences>
