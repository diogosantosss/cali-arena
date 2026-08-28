package com.caliarena.domain.bracket

import java.time.Instant


data class BracketMatchSummary(
    val matchId: Int,
    val startedAt: Instant?,
    val athleteRed: String,
    val athleteBlue: String,
    val winner: String,
)

data class BracketSummary(
    val stage: BracketStage,
    val matches: List<BracketMatchSummary>,
)

data class TournamentBracketsResponse(
    val tournamentId: Int,
    val division: String,
    val brackets: List<BracketSummary>,
)