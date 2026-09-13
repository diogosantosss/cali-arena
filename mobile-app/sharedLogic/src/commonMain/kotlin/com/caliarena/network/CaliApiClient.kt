package com.caliarena.network

import com.caliarena.data.AthleteOutput
import com.caliarena.data.BracketLeaderboardOutput
import com.caliarena.data.ClubOutput
import com.caliarena.data.ErrorCode
import com.caliarena.data.MatchOutput
import com.caliarena.data.MatchProgressOutput
import com.caliarena.data.ProblemBody
import com.caliarena.data.RoutineOutput
import com.caliarena.data.RoutineOverviewOutput
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

    suspend fun getMatches(token: String): Result<List<MatchOutput>> =
        execute {
            client.get("$base/api/matches") {
                bearerAuth(token)
            }
        }

    suspend fun getMatch(id: Int): Result<MatchOutput> =
        execute {
            client.get("$base/api/matches/$id")
        }

    suspend fun getMatchProgress(id: Int): Result<MatchProgressOutput> =
        execute {
            client.get("$base/api/matches/$id/progress")
        }

    suspend fun getAthlete(id: Int): Result<AthleteOutput> =
        execute {
            client.get("$base/api/athletes/$id")
        }

    suspend fun getClub(id: Int): Result<ClubOutput> =
        execute {
            client.get("$base/api/clubs/$id")
        }

    suspend fun getBracketLeaderboard(bracketId: Int): Result<BracketLeaderboardOutput> =
        execute {
            client.get("$base/api/brackets/$bracketId/leaderboard")
        }

    suspend fun getRoutines(): Result<List<RoutineOutput>> =
        execute {
            client.get("$base/api/routines")
        }

    suspend fun getRoutineOverview(name: String): Result<RoutineOverviewOutput> =
        execute {
            client.get("$base/api/routines/$name/overview")
        }

    private suspend inline fun <reified T> execute(request: () -> HttpResponse): Result<T> =
        try {
            val response = request()
            if (response.status.value !in 200..299) {
                val problem = runCatching { response.body<ProblemBody>() }.getOrNull()
                Result.failure(CaliApiException(problem))
            } else if (T::class == Unit::class) {
                Result.success(Unit as T)
            } else {
                runCatching { response.body<T>() }
            }
        } catch (e: Throwable) {
            Result.failure(CaliApiException(code = ErrorCode.NO_CONNECTION))
        }
}

class CaliApiException(
    val problem: ProblemBody? = null,
    val code: ErrorCode = ErrorCode.fromType(problem?.type),
    override val message: String? = null,
) : Exception(message ?: code.name)
