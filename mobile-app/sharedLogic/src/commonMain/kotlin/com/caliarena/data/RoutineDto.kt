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
) {
    val exerciseGroups: List<List<ExerciseOutput>>
        get() =
            exercises
                .groupBy { it.exerciseOrder }
                .map { it.value.sortedBy { exercise -> exercise.supersetOrder ?: 0 } }
                .sortedBy { it.first().exerciseOrder }

    val exerciseGroupsSummary: List<String>
        get() =
            exerciseGroups.map { group ->
                group.joinToString(" - ") { exercise -> exercise.summaryLabel }
            }
}

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
) {
    val abbreviation: String
        get() =
            name
                .split(' ', '-')
                .filter { it.isNotBlank() }
                .let { words ->
                    if (words.size > 1) {
                        words.joinToString("") { it.take(1) }.uppercase()
                    } else {
                        words.first().take(2).uppercase()
                    }
                }

    val addedWeightLabel: String?
        get() = addedWeight?.let { "+${it.removeTrailingZeros()} kg" }

    val summaryLabel: String
        get() {
            val weight = addedWeightLabel?.let { " $it" }.orEmpty()
            val unbroken = if (type == ExerciseType.UNBROKEN) " unbroken" else ""
            val movement = if (type == ExerciseType.SUPERSET) abbreviation else name
            return "$targetReps $movement$weight$unbroken"
        }
}

private fun Double.removeTrailingZeros(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()

@Serializable
enum class ExerciseType {
    NORMAL,
    UNBROKEN,
    SUPERSET,
}
