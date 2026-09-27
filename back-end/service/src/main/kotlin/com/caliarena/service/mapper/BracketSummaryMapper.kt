package com.caliarena.service.mapper

import com.caliarena.domain.bracket.BracketMatchSummary
import com.caliarena.domain.bracket.BracketSummary
import com.caliarena.domain.bracket.TournamentBracketsResponse
import com.caliarena.repo.entities.match.MatchEntity
import com.caliarena.repo.trx.Transaction
import jakarta.inject.Named
import java.time.Instant

@Named
class BracketSummaryMapper {
    fun build(
        trx: Transaction,
        tournamentId: Int,
        division: String,
    ): TournamentBracketsResponse {
        val tournamentBrackets = trx.brackets.findByTournamentIdAndDivision(tournamentId, division)

        if (tournamentBrackets.isEmpty()) {
            return TournamentBracketsResponse(tournamentId, division, emptyList())
        }

        val summaries =
            tournamentBrackets.map { bracket ->
                BracketSummary(
                    stage = bracket.stage,
                    matches = trx.matches.findByBracketId(bracket.id).map(::toSummary),
                )
            }

        return TournamentBracketsResponse(tournamentId, division, summaries)
    }

    private fun toSummary(match: MatchEntity) =
        BracketMatchSummary(
            matchId = match.id,
            startedAt = match.startedAt?.let(Instant::ofEpochMilli),
            athleteRed = match.athleteRed?.name ?: "Unknown",
            athleteBlue = match.athleteBlue?.name ?: "Unknown",
            winner = match.winnerAthlete?.name ?: "—",
        )
}
