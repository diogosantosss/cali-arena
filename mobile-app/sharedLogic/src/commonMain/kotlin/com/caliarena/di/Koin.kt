package com.caliarena.di

import org.koin.core.KoinApplication
import org.koin.dsl.KoinAppDeclaration

expect fun initKoin(
    baseUrl: String = DEFAULT_BASE_URL,
    appDeclaration: KoinAppDeclaration = {},
): KoinApplication
