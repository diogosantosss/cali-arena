package com.caliarena.service

import com.caliarena.domain.bracket.BracketLeaderboard
import com.caliarena.domain.bracket.BracketMatchSummary
import com.caliarena.domain.bracket.BracketSummary
import com.caliarena.domain.bracket.RaceResult
import com.caliarena.domain.bracket.TournamentBracketsResponse
import com.caliarena.domain.bracket.buildLeaderboard
import com.caliarena.domain.routine.Exercise
import com.caliarena.domain.routine.RoutineOverview
import com.caliarena.repo.entities.athlete.AthleteEntity
import com.caliarena.repo.entities.match.MatchEntity
import com.caliarena.repo.entities.routine.ExerciseEntity
import com.caliarena.repo.trx.Transaction
import com.caliarena.service.sse.ScreenRoutinesEvent
import com.caliarena.service.sse.SpectatorAction
import org.springframework.data.repository.findByIdOrNull
import java.time.Instant

fun Transaction.buildLeaderboard(bracketId: Int): BracketLeaderboard? {
    val bracket = brackets.findByIdOrNull(bracketId) ?: return null

    val results =
        matches
            .findByBracketId(bracketId)
            .flatMap(::finishedRaceResults)

    return buildLeaderboard(bracket = bracket.toDomain(), results = results)
}

private fun Transaction.finishedRaceResults(match: MatchEntity): List<RaceResult> {
    val startedAt = match.startedAt ?: return emptyList()
    val progress = matchProgresses.findByMatchId(match.id) ?: return emptyList()

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

fun Transaction.buildBracketsSummary(
    tournamentId: Int,
    division: String,
): TournamentBracketsResponse {
    val tournamentBrackets = brackets.findByTournamentIdAndDivision(tournamentId, division)

    if (tournamentBrackets.isEmpty()) {
        return TournamentBracketsResponse(tournamentId, division, emptyList())
    }

    val summaries =
        tournamentBrackets.map { bracket ->
            val bracketMatches: List<MatchEntity> =
                matches
                    .findByBracketId(bracket.id)

            val matchSummaries =
                bracketMatches.map { match ->
                    val startedAt = match.startedAt?.let { Instant.ofEpochMilli(it) }
                    val red = match.athleteRed?.name ?: "Unknown"
                    val blue = match.athleteBlue?.name ?: "Unknown"
                    val winner = match.winnerAthlete?.name ?: "—"

                    BracketMatchSummary(match.id, startedAt, red, blue, winner)
                }

            BracketSummary(stage = bracket.stage, matches = matchSummaries)
        }

    return TournamentBracketsResponse(tournamentId, division, summaries)
}

fun Transaction.buildScreenRoutinesSnapshot(tournamentId: Int): List<ScreenRoutinesEvent> =
    screenRoutines
        .findByTournamentIdOrderByDisplayOrder(tournamentId)
        .mapNotNull { screenRoutineEntity ->
            val screenRoutine = screenRoutineEntity.toDomain()
            val routine =
                routines.findByIdOrNull(screenRoutine.routineId)?.toDomain()
                    ?: return@mapNotNull null

            val routineOverview =
                exercises
                    .findExercisesByRoutineId(routine.id)
                    .map(ExerciseEntity::toDomain)
                    .sortedBy(Exercise::exerciseOrder)
                    .let { exercises ->
                        RoutineOverview(routine.name, routine.timeCapSeconds, routine.createdAt, exercises)
                    }

            ScreenRoutinesEvent(
                tournamentId = tournamentId,
                action = SpectatorAction.SCREEN_ROUTINES_CREATED,
                screenRoutine = screenRoutine,
                routineOverview = routineOverview,
            )
        }
