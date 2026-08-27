package com.caliarena.repo

import com.caliarena.repo.entities.tournament.TournamentStateEntity
import org.springframework.data.repository.CrudRepository

interface TournamentStateRepository : CrudRepository<TournamentStateEntity, Int> {
    fun findByTournamentId(tournamentId: Int): TournamentStateEntity?
}
