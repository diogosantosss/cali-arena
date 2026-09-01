package com.caliarena.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences

private lateinit var appContext: Context

fun initDataStoreContext(context: Context) {
    appContext = context.applicationContext
}

actual fun createTokenDataStore(): DataStore<Preferences> =
    PreferenceDataStoreFactory.create {
        appContext.filesDir.resolve("cali_arena.preferences_pb")
    }
