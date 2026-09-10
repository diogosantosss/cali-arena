package com.caliarena.data

import kotlinx.serialization.Serializable

@Serializable
data class MatchProgressOutput(
    val id: Int,
    val matchId: Int,
    val redCurrentExerciseId: Int? = null,
    val blueCurrentExerciseId: Int? = null,
    val redCurrentReps: Int,
    val blueCurrentReps: Int,
    val redFinishedAt: String? = null,
    val blueFinishedAt: String? = null,
    val timerStartedAt: String? = null,
    val timerRemainingSeconds: Int? = null,
    val updatedAt: String,
)
