package com.caliarena.service.mapper

import com.caliarena.domain.bracket.BracketLeaderboard
import com.caliarena.domain.bracket.RaceResult
import com.caliarena.domain.bracket.buildLeaderboard
import com.caliarena.repo.entities.athlete.AthleteEntity
import com.caliarena.repo.entities.match.MatchEntity
import com.caliarena.repo.trx.Transaction
import jakarta.inject.Named
import org.springframework.data.repository.findByIdOrNull

@Named
class BracketLeaderboardMapper {
    fun build(
        trx: Transaction,
        bracketId: Int,
    ): BracketLeaderboard? {
        val bracket = trx.brackets.findByIdOrNull(bracketId) ?: return null

        val results =
            trx.matches
                .findByBracketId(bracketId)
                .flatMap { match -> finishedRaceResults(trx, match) }

        return buildLeaderboard(bracket = bracket.toDomain(), results = results)
    }

    private fun finishedRaceResults(
        trx: Transaction,
        match: MatchEntity,
    ): List<RaceResult> {
        val startedAt = match.startedAt ?: return emptyList()
        val progress = trx.matchProgresses.findByMatchId(match.id) ?: return emptyList()

        return listOfNotNull(
            raceResult(match.id, startedAt, match.athleteRed, progress.redFinishedAt),
            raceResult(match.id, startedAt, match.athleteBlue, progress.blueFinishedAt),
        )
    }

    private fun raceResult(
        matchId: Int,
        startedAt: Long,
        athlete: AthleteEntity?,
        finishedAt: Long?,
    ): RaceResult? {
        if (athlete == null || finishedAt == null) return null
        return RaceResult(athlete.name, finishedAt - startedAt, matchId)
    }
}
