package com.caliarena.domain.bracket

data class RaceResult(
    val athleteName: String,
    val durationMs: Long,
    val matchId: Int,
)

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

fun buildLeaderboard(bracket: Bracket, results: List<RaceResult>): BracketLeaderboard {
    val entries =
        results
            .groupBy(RaceResult::athleteName)
            .map { (_, athleteResults) -> athleteResults.minBy(RaceResult::durationMs) }
            .sortedBy(RaceResult::durationMs)
            .map { result ->
                LeaderboardEntry(
                    athleteName = result.athleteName,
                    duration = formatDuration(result.durationMs),
                    matchId = result.matchId,
                )
            }

    return BracketLeaderboard(bracket.id, bracket.division, bracket.stage, entries)
}

private fun formatDuration(totalMs: Long): String {
    val minutes = totalMs / 60000
    val seconds = (totalMs % 60000) / 1000
    val ms = totalMs % 1000

    return "%d:%02d.%03d".format(minutes, seconds, ms)
}