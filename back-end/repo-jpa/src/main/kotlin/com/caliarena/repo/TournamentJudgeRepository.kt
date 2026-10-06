package com.caliarena.repo

import com.caliarena.repo.entities.tournament.TournamentJudgeEntity
import com.caliarena.repo.entities.tournament.TournamentJudgeId
import org.springframework.data.repository.CrudRepository

interface TournamentJudgeRepository : CrudRepository<TournamentJudgeEntity, TournamentJudgeId> {
    fun findByTournamentIdOrderByCreatedAt(tournamentId: Int): List<TournamentJudgeEntity>

    fun findByTournamentId(tournamentId: Int): List<TournamentJudgeEntity>

    fun findByUserId(userId: Int): List<TournamentJudgeEntity>

    fun findByTournamentIdIn(tournamentIds: Collection<Int>): List<TournamentJudgeEntity>

    fun existsByTournamentIdAndUserId(
        tournamentId: Int,
        userId: Int,
    ): Boolean

    fun deleteByTournamentIdAndUserId(
        tournamentId: Int,
        userId: Int,
    )
}
