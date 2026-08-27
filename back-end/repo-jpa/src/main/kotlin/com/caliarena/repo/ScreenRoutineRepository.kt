package com.caliarena.repo

import com.caliarena.repo.entities.routine.ScreenRoutineEntity
import org.springframework.data.repository.CrudRepository

interface ScreenRoutineRepository : CrudRepository<ScreenRoutineEntity, Int> {
    fun findByTournamentIdOrderByDisplayOrder(tournamentId: Int): List<ScreenRoutineEntity>
}
