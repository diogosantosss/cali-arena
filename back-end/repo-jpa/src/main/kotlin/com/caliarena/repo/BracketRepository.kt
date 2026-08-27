package com.caliarena.repo

import com.caliarena.domain.athlete.GenderType
import com.caliarena.repo.entities.tournament.BracketEntity
import org.springframework.data.repository.CrudRepository

interface BracketRepository : CrudRepository<BracketEntity, Int> {
    fun findByTournamentId(tournamentId: Int): List<BracketEntity>

    fun findByTournamentIdAndGender(
        tournamentId: Int,
        gender: GenderType,
    ): List<BracketEntity>
}
