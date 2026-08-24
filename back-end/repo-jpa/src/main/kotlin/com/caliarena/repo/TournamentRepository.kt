package com.caliarena.repo

import com.caliarena.domain.tournament.TournamentStatus
import com.caliarena.repo.entities.tournament.TournamentEntity
import org.springframework.data.repository.CrudRepository

interface TournamentRepository : CrudRepository<TournamentEntity, Int> {
    fun findByStatus(status: TournamentStatus): List<TournamentEntity>

    fun findByName(tournamentName: String): TournamentEntity?
}
