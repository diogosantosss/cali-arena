package com.caliarena.data

import kotlinx.serialization.Serializable

@Serializable
data class MatchOutput(
    val id: Int,
    val bracketId: Int,
    val routineId: Int,
    val judgeId: Int,
    val athleteRedId: Int?,
    val athleteBlueId: Int?,
    val winnerAthleteId: Int?,
    val status: MatchStatus,
    val startedAt: String?,
    val finishedAt: String?,
    val createdAt: String,
)

@Serializable
enum class MatchStatus {
    PENDING,
    RUNNING,
    FINISHED,
}
