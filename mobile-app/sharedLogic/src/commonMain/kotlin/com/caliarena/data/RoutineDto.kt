package com.caliarena.data

import kotlinx.serialization.Serializable

@Serializable
data class RoutineOutput(
    val id: Int,
    val name: String,
    val timeCapSeconds: Int?,
    val createdAt: String,
)

@Serializable
data class RoutineOverviewOutput(
    val name: String,
    val timeCapSeconds: Int?,
    val createdAt: String,
    val exercises: List<ExerciseOutput>,
)

@Serializable
data class ExerciseOutput(
    val id: Int,
    val routineId: Int,
    val name: String,
    val targetReps: Int,
    val addedWeight: Double?,
    val exerciseOrder: Int,
    val supersetOrder: Int?,
    val type: ExerciseType,
)

@Serializable
enum class ExerciseType {
    NORMAL,
    UNBROKEN,
    SUPERSET,
}
