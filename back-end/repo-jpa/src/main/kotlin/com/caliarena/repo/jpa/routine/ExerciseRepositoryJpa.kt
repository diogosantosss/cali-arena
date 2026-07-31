package com.caliarena.repo.jpa.routine

import com.caliarena.repo.entities.routine.ExerciseEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ExerciseRepositoryJpa : JpaRepository<ExerciseEntity, Int> {
    fun findExercisesByRoutineId(routineId: Int): List<ExerciseEntity>

    @Modifying
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
