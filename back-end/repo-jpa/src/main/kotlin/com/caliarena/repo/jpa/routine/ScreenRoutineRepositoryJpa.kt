package com.caliarena.repo.jpa.routine

import com.caliarena.repo.entities.routine.ScreenRoutineEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ScreenRoutineRepositoryJpa : JpaRepository<ScreenRoutineEntity, Int> {
    fun findByTournamentIdOrderByDisplayOrder(tournamentId: Int): List<ScreenRoutineEntity>
}
