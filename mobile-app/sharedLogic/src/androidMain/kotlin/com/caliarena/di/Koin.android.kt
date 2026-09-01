package com.caliarena.di

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

private lateinit var appContext: Context

fun initKoinAndroid(context: Context) {
    appContext = context.applicationContext
}

actual fun initKoin(
    baseUrl: String,
    appDeclaration: KoinAppDeclaration,
): KoinApplication =
    startKoin {
        androidContext(appContext)
        appDeclaration()
        modules(appModule(baseUrl))
    }
