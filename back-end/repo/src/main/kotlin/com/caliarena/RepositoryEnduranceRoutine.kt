package com.caliarena

import com.caliarena.domain.routine.EnduranceRoutine
import com.caliarena.domain.routine.Exercise
import com.caliarena.domain.routine.ExerciseType
import java.math.BigDecimal
import java.time.Instant

interface RepositoryEnduranceRoutine : Repository<EnduranceRoutine> {

    fun createRoutine(
        name: String,
        timeCapSeconds: Int?,
        createdAt: Instant
    ): EnduranceRoutine

    fun findByName(name: String) : EnduranceRoutine?

    // Exercise
    fun createExercise(
        routineId: Int,
        name: String,
        targetReps: Int,
        addedWeight: BigDecimal?,
        exerciseOrder: Int,
        supersetOrder: Int?,
        type: ExerciseType,
    ): Exercise?

    fun findExercisesByRoutineId(routineId: Int) : List<Exercise>

    fun shiftExerciseOrders(routineId: Int, fromOrder: Int)

    fun existsByRoutineIdAndExerciseOrder(routineId: Int, exerciseOrder: Int): Boolean
}