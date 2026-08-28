package com.caliarena.domain.bracket

data class LeaderboardEntry(
    val athleteName: String,
    val duration: String,
    val matchId: Int,
)

data class BracketLeaderboard(
    val bracketId: Int,
    val division: String,
    val stage: BracketStage,
    val entries: List<LeaderboardEntry>,
)