package com.caliarena.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

actual fun initKoin(
    baseUrl: String,
    appDeclaration: KoinAppDeclaration,
): KoinApplication =
    startKoin {
        appDeclaration()
        modules(appModule(baseUrl))
    }
