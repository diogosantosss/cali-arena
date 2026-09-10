package com.caliarena.repository

import com.caliarena.auth.TokenStorage
import com.caliarena.data.AthleteOutput
import com.caliarena.data.MatchOutput
import com.caliarena.data.MatchProgressOutput
import com.caliarena.data.RoutineOutput
import com.caliarena.data.RoutineOverviewOutput
import com.caliarena.network.CaliApiClient

class MatchRepository(
    private val api: CaliApiClient,
    private val tokenStorage: TokenStorage,
) {
    suspend fun getMatches(): Result<List<MatchOutput>> {
        val token =
            tokenStorage.readSession()?.token
                ?: return Result.failure(NotAuthenticatedException())
        return api.getMatchesForJudge(token)
    }

    suspend fun getAthlete(id: Int): Result<AthleteOutput> = api.getAthlete(id)

    suspend fun getMatch(id: Int): Result<MatchOutput> = api.getMatch(id)

    suspend fun getMatchProgress(id: Int): Result<MatchProgressOutput> = api.getMatchProgress(id)

    suspend fun getRoutines(): Result<List<RoutineOutput>> = api.getRoutines()

    suspend fun getRoutineOverview(name: String): Result<RoutineOverviewOutput> = api.getRoutineOverview(name)
}
