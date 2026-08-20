package com.caliarena.service

import com.caliarena.TransactionManager
import com.caliarena.domain.routine.EnduranceRoutine
import com.caliarena.domain.routine.Exercise
import com.caliarena.domain.routine.ExerciseType
import com.caliarena.domain.routine.RoutineOverview
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Clock

sealed class RoutineError {
    data object RoutineAlreadyExists : RoutineError()

    data object RoutineNotFound : RoutineError()

    data object ExerciseTypeNotFound : RoutineError()
}

@Service
class RoutineService(
    private val trx: TransactionManager,
    private val clock: Clock,
) {
    fun createRoutine(
        name: String,
        timeCapSeconds: Int?,
    ): Either<RoutineError, EnduranceRoutine> =
        trx.run {
            repoEnduranceRoutine
                .findByName(name)
                ?.let { return@run failure(RoutineError.RoutineAlreadyExists) }

            val enduranceRoutine =
                repoEnduranceRoutine.createRoutine(
                    name,
                    timeCapSeconds,
                    clock.instant(),
                )

            success(enduranceRoutine)
        }

    /**
     * Creates an exercise for a specific routine.
     *
     * Example usage:
     * name: "Push-Ups"
     * targetReps: 20
     * addedWeight: null
     * exerciseOrder: 1
     * supersetOrder: null
     * type: "NORMAL"
     *
     * The Type can Either be [ExerciseType.NORMAL], [ExerciseType.UNBROKEN], or [ExerciseType.SUPERSET].
     */
    fun createExercise(
        routineId: Int,
        name: String,
        targetReps: Int,
        addedWeight: BigDecimal?,
        exerciseOrder: Int,
        supersetOrder: Int?,
        type: String, // ExerciseType
    ): Either<RoutineError, Exercise> =
        trx.run {
            repoEnduranceRoutine.findById(routineId)
                ?: return@run failure(RoutineError.RoutineNotFound)

            val exerciseType =
                ExerciseType.entries.find { it.name == type }
                    ?: return@run failure(RoutineError.ExerciseTypeNotFound)

            val exists = repoEnduranceRoutine.existsByRoutineIdAndExerciseOrder(routineId, exerciseOrder)

            if (exists && exerciseType != ExerciseType.SUPERSET) {
                repoEnduranceRoutine.shiftExerciseOrders(routineId, exerciseOrder)
            }

            val exercise =
                repoEnduranceRoutine.createExercise(
                    routineId,
                    name,
                    targetReps,
                    addedWeight,
                    exerciseOrder,
                    supersetOrder,
                    exerciseType,
                ) ?: return@run failure(RoutineError.RoutineNotFound)

            success(exercise)
        }

    fun getRoutineOverview(routineName: String): Either<RoutineError, RoutineOverview> =
        trx.run {
            val routine =
                repoEnduranceRoutine.findByName(routineName)
                    ?: return@run failure(RoutineError.RoutineNotFound)

            val exercises = repoEnduranceRoutine.findExercisesByRoutineId(routine.id)

            success(
                RoutineOverview(
                    name = routine.name,
                    timeCapSeconds = routine.timeCapSeconds,
                    createdAt = routine.createdAt,
                    exercises = exercises.sortedBy(Exercise::exerciseOrder),
                ),
            )
        }

    fun getRoutines(): List<EnduranceRoutine> = trx.run { repoEnduranceRoutine.findAll() }
}
