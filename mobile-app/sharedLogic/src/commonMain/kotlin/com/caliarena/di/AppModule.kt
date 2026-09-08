package com.caliarena.di

import com.caliarena.auth.TokenStorage
import com.caliarena.auth.createTokenDataStore
import com.caliarena.network.CaliApiClient
import com.caliarena.network.createHttpClientEngine
import com.caliarena.repository.AuthRepository
import com.caliarena.repository.MatchRepository
import com.caliarena.viewmodel.LoginViewModel
import com.caliarena.viewmodel.MatchesViewModel
import com.caliarena.viewmodel.SessionViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

const val DEFAULT_BASE_URL = "http://10.0.2.2:8080"

data class AppConfig(
    val baseUrl: String,
)

fun appModule(baseUrl: String = DEFAULT_BASE_URL): Module =
    module {
        single { AppConfig(baseUrl) }

        single { createTokenDataStore() }
        single { TokenStorage(get()) }
        single { AuthRepository(get(), get()) }
        single { MatchRepository(get(), get()) }

        single {
            HttpClient(createHttpClientEngine()) {
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            explicitNulls = false
                        },
                    )
                }
            }
        }

        single { CaliApiClient(get<AppConfig>().baseUrl, get()) }

        viewModelOf(::LoginViewModel)
        viewModelOf(::SessionViewModel)
        viewModelOf(::MatchesViewModel)
    }
