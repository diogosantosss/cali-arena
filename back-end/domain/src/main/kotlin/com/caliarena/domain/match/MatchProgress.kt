package com.caliarena.domain.match

import com.caliarena.domain.routine.Exercise
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
) {
    fun advance(
        redReps: Int?,
        blueReps: Int?,
        exercises: List<Exercise>,
        now: Instant,
    ): MatchProgress {
        val red = nextSide(redCurrentExerciseId, redCurrentReps, redReps, redFinishedAt, exercises, now)
        val blue = nextSide(blueCurrentExerciseId, blueCurrentReps, blueReps, blueFinishedAt, exercises, now)

        return copy(
            redCurrentExerciseId = red.exerciseId,
            redCurrentReps = red.reps,
            redFinishedAt = red.finishedAt,
            blueCurrentExerciseId = blue.exerciseId,
            blueCurrentReps = blue.reps,
            blueFinishedAt = blue.finishedAt,
            updatedAt = now,
        )
    }

    private data class SideState(
        val exerciseId: Int?,
        val reps: Int,
        val finishedAt: Instant?,
    )

    private fun nextSide(
        currentExerciseId: Int?,
        currentReps: Int,
        newReps: Int?,
        currentFinishedAt: Instant?,
        exercises: List<Exercise>,
        now: Instant,
    ): SideState {
        // não mexe se não veio reps novas, ou se este lado já terminou a rotina
        if (newReps == null || currentFinishedAt != null || currentExerciseId == null) {
            return SideState(currentExerciseId, currentReps, currentFinishedAt)
        }

        val current = exercises.find { it.id == currentExerciseId }
            ?: return SideState(currentExerciseId, currentReps, currentFinishedAt)

        if (newReps < current.targetReps) {
            return SideState(current.id, newReps, null)
        }

        val next = exercises.filter { it.exerciseOrder > current.exerciseOrder }.minByOrNull { it.exerciseOrder }

        return if (next != null) {
            SideState(next.id, 0, null) // avança de exercício, reseta reps
        } else {
            SideState(null, current.targetReps, now) // acabou a rotina toda -> marca como terminado
        }
    }
}