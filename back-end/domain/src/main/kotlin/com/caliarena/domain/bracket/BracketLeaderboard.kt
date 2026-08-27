package com.caliarena.domain.bracket

import com.caliarena.domain.athlete.GenderType

data class LeaderboardEntry(
    val athleteName: String,
    val duration: String,
    val matchId: Int,
)

data class BracketLeaderboard(
    val bracketId: Int,
    val gender: GenderType,
    val stage: BracketStage,
    val entries: List<LeaderboardEntry>,
)