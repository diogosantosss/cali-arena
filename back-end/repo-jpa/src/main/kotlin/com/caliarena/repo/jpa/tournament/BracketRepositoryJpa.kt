package com.caliarena.repo.jpa.tournament

import com.caliarena.domain.athlete.GenderType
import com.caliarena.repo.entities.tournament.BracketEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BracketRepositoryJpa : JpaRepository<BracketEntity, Int> {
    fun findByTournamentId(tournamentId: Int): List<BracketEntity>

    fun findByTournamentIdAndGender(
        tournamentId: Int,
        gender: GenderType,
    ): List<BracketEntity>
}
