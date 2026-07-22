package com.caliarena.repo.jpa.routine

import com.caliarena.repo.entities.routine.ExerciseEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ExerciseRepositoryJpa : JpaRepository<ExerciseEntity, Int> {
    fun findExercisesByRoutineId(routineId: Int): List<ExerciseEntity>
}
