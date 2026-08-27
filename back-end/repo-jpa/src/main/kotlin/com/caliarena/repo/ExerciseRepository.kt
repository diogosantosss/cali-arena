package com.caliarena.repo

import com.caliarena.repo.entities.routine.ExerciseEntity
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface ExerciseRepository : CrudRepository<ExerciseEntity, Int> {
    fun findExercisesByRoutineId(routineId: Int): List<ExerciseEntity>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
            UPDATE ExerciseEntity e
            SET e.exerciseOrder = e.exerciseOrder + 1
            WHERE e.routine.id = :routineId AND e.exerciseOrder >= :fromOrder
        """,
    )
    fun shiftExerciseOrders(
        routineId: Int,
        fromOrder: Int,
    )

    fun existsByRoutineIdAndExerciseOrder(
        routineId: Int,
        exerciseOrder: Int,
    ): Boolean
}
