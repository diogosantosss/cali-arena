package com.caliarena.service

import com.caliarena.domain.routine.EnduranceRoutine
import com.caliarena.domain.routine.Exercise
import com.caliarena.domain.routine.ExerciseType
import com.caliarena.domain.routine.RoutineOverview
import com.caliarena.repo.entities.routine.EnduranceRoutineEntity
import com.caliarena.repo.entities.routine.ExerciseEntity
import com.caliarena.repo.trx.TransactionManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant

@Service
class RoutineService(
    private val trx: TransactionManager,
    private val clock: Clock,
) {
    fun createRoutine(
        name: String,
        timeCapSeconds: Int?,
    ): Either<ApiError, EnduranceRoutine> =
        trx.run {
            routines
                .findByName(name)
                ?.let { return@run failure(ApiError.ROUTINE_ALREADY_EXISTS) }

            val enduranceRoutine =
                routines.save(
                    EnduranceRoutineEntity(
                        name = name,
                        timeCapSeconds = timeCapSeconds,
                        createdAt = clock.instant().epochSecond,
                    ),
                )

            success(enduranceRoutine.toDomain())
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
    ): Either<ApiError, Exercise> =
        trx.run {
            val routine =
                routines.findByIdOrNull(routineId)
                    ?: return@run failure(ApiError.ROUTINE_NOT_FOUND)

            val exerciseType =
                ExerciseType.entries.find { it.name == type }
                    ?: return@run failure(ApiError.EXERCISE_TYPE_NOT_FOUND)

            val exists = exercises.existsByRoutineIdAndExerciseOrder(routineId, exerciseOrder)

            if (exists && exerciseType != ExerciseType.SUPERSET) {
                exercises.shiftExerciseOrders(routineId, exerciseOrder)
            }

            val exercise =
                exercises.save(
                    ExerciseEntity(
                        routine = routine,
                        name = name,
                        targetReps = targetReps,
                        addedWeight = addedWeight,
                        exerciseOrder = exerciseOrder,
                        supersetOrder = supersetOrder,
                        type = exerciseType,
                    ),
                )

            success(exercise.toDomain())
        }

    fun getRoutineOverview(routineName: String): Either<ApiError, RoutineOverview> =
        trx.run {
            val routine =
                routines.findByName(routineName)
                    ?: return@run failure(ApiError.ROUTINE_NOT_FOUND)

            val exercises = exercises.findExercisesByRoutineId(routine.id).map(ExerciseEntity::toDomain)

            success(
                RoutineOverview(
                    name = routine.name,
                    timeCapSeconds = routine.timeCapSeconds,
                    createdAt = Instant.ofEpochSecond(routine.createdAt),
                    exercises = exercises.sortedBy(Exercise::exerciseOrder),
                ),
            )
        }

    fun getRoutines(): List<EnduranceRoutine> =
        trx.run {
            routines.findAll().map(EnduranceRoutineEntity::toDomain)
        }
}
