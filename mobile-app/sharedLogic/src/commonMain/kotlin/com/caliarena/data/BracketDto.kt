package com.caliarena.data

import kotlinx.serialization.Serializable

@Serializable
data class BracketLeaderboardOutput(
    val bracketId: Int,
    val division: String,
    val stage: BracketStage,
    val entries: List<BracketLeaderboardEntryOutput> = emptyList(),
)

@Serializable
data class BracketLeaderboardEntryOutput(
    val athleteName: String,
    val duration: String,
    val matchId: Int,
)

@Serializable
enum class BracketStage {
    QUALIFIERS,
    QUARTERFINALS,
    SEMIFINALS,
    FINALS,
}
