package com.caliarena.domain.match

import java.time.Instant

data class MatchProgress(
    val id: Int,
    val matchId: Int,
    val redCurrentExerciseId: Int?,
    val blueCurrentExerciseId: Int?,
    val redCurrentReps: Int,
    val blueCurrentReps: Int,
    val redFinishedAt: Instant?,
    val blueFinishedAt: Instant?,
    val timerStartedAt: Instant?,
    val timerRemainingSeconds: Int?,
    val updatedAt: Instant,
)
