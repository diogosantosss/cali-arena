package com.caliarena.domain.match

import java.time.Instant

data class Match(
    val id: Int,
    val bracketId: Int,
    val routineId: Int,
    val judgeId: Int,

    val athleteRedId: Int?,
    val athleteBlueId: Int?,

    val winnerAthleteId: Int?,

    val status: MatchStatus,
    val startedAt: Instant?,
    val finishedAt: Instant?,
    val createdAt: Instant,
)
