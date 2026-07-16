package com.caliarena.repo.jpa.tournament

import com.caliarena.domain.tournament.TournamentStatus
import com.caliarena.repo.entities.tournament.TournamentEntity
import org.springframework.data.jpa.repository.JpaRepository

interface TournamentRepositoryJpa : JpaRepository<TournamentEntity, Int> {
    fun findByStatus(status: TournamentStatus): List<TournamentEntity>

    fun findByName(tournamentName: String): TournamentEntity?
}
