package com.caliarena.network

import com.caliarena.data.ErrorCode
import com.caliarena.data.ProblemBody
import com.caliarena.data.UserInfoOutput
import com.caliarena.data.UserLoginInput
import com.caliarena.data.UserLoginOutput
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

class CaliApiClient(
    private val baseUrl: String,
    private val client: HttpClient,
) {
    private val base = baseUrl.trimEnd('/')

    suspend fun login(input: UserLoginInput): Result<UserLoginOutput> =
        execute {
            client.post("$base/api/users/token") {
                contentType(ContentType.Application.Json)
                setBody(input)
            }
        }

    suspend fun register(
        username: String,
        password: String,
    ): Result<UserInfoOutput> =
        execute {
            client.post("$base/api/users") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest(username, password))
            }
        }

    suspend fun me(token: String): Result<UserInfoOutput> =
        execute {
            client.get("$base/api/users/me") {
                bearerAuth(token)
            }
        }

    suspend fun logout(token: String): Result<Unit> =
        execute {
            client.post("$base/api/users/logout") {
                bearerAuth(token)
            }
        }

    private suspend inline fun <reified T> execute(request: () -> HttpResponse): Result<T> =
        try {
            val response = request()
            if (response.status.value !in 200..299) {
                val problem = runCatching { response.body<ProblemBody>() }.getOrNull()
                Result.failure(CaliApiException(response.status.value, problem))
            } else if (T::class == Unit::class) {
                Result.success(Unit as T)
            } else {
                runCatching { response.body<T>() }
            }
        } catch (e: Throwable) {
            Result.failure(CaliApiException(statusCode = null, problem = null, code = ErrorCode.NO_CONNECTION))
        }
}

@Serializable
private data class RegisterRequest(
    val username: String,
    val password: String,
)

class CaliApiException(
    val statusCode: Int?,
    val problem: ProblemBody? = null,
    val code: ErrorCode = ErrorCode.fromType(problem?.type),
    override val message: String? = null,
) : Exception(message ?: code.name)
