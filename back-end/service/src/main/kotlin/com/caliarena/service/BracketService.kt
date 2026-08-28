package com.caliarena.service

import com.caliarena.domain.bracket.Bracket
import com.caliarena.domain.bracket.BracketLeaderboard
import com.caliarena.domain.bracket.BracketMatchSummary
import com.caliarena.domain.bracket.BracketOverview
import com.caliarena.domain.bracket.BracketStage
import com.caliarena.domain.bracket.BracketSummary
import com.caliarena.domain.bracket.LeaderboardEntry
import com.caliarena.domain.bracket.TournamentBracketsResponse
import com.caliarena.repo.entities.match.MatchEntity
import com.caliarena.repo.entities.tournament.BracketEntity
import com.caliarena.repo.trx.TransactionManager
import jakarta.inject.Named
import org.springframework.data.repository.findByIdOrNull
import java.time.Clock
import java.time.Instant

@Named
class BracketService(
    private val trx: TransactionManager,
    private val clock: Clock,
) {
    fun createBracket(
        tournamentId: Int,
        division: String,
        stage: String,
    ): Either<ApiError, Bracket> =
        trx.run {
            val tournament =
                tournaments.findByIdOrNull(tournamentId)
                    ?: return@run failure(ApiError.TOURNAMENT_NOT_FOUND)

            val divisionName = division.trim()
            if (divisionName.isEmpty()) return@run failure(ApiError.INVALID_BRACKET_DIVISION)

            val bracketStage =
                BracketStage.entries.find { it.name.equals(stage, true) }
                    ?: return@run failure(ApiError.INVALID_BRACKET_STAGE)

            val existing = brackets.findByTournamentIdAndDivision(tournamentId, divisionName)
            if (existing.any { it.stage == bracketStage }) {
                return@run failure(ApiError.BRACKET_ALREADY_EXISTS)
            }

            val now = clock.instant()

            val bracket =
                brackets
                    .save(
                        BracketEntity(
                            tournament = tournament,
                            division = divisionName,
                            stage = bracketStage,
                            createdAt = now.epochSecond,
                        ),
                    ).toDomain()

            success(bracket)
        }

    fun getBracketsByTournament(tournamentId: Int): Either<ApiError, List<Bracket>> =
        trx.run {
            tournaments.findByIdOrNull(tournamentId)?.toDomain()
                ?: return@run failure(ApiError.TOURNAMENT_NOT_FOUND)

            success(brackets.findByTournamentId(tournamentId).map(BracketEntity::toDomain))
        }

    fun getBracketsByTournamentAndDivision(
        tournamentId: Int,
        division: String,
    ): Either<ApiError, List<Bracket>> =
        trx.run {
            tournaments.findByIdOrNull(tournamentId)?.toDomain()
                ?: return@run failure(ApiError.TOURNAMENT_NOT_FOUND)

            val divisionName = division.trim()
            if (divisionName.isEmpty()) return@run failure(ApiError.INVALID_BRACKET_DIVISION)

            success(brackets.findByTournamentIdAndDivision(tournamentId, divisionName).map(BracketEntity::toDomain))
        }

    fun getBracketOverview(
        tournamentId: Int,
        division: String,
    ): Either<ApiError, List<BracketOverview>> =
        trx.run {
            tournaments.findByIdOrNull(tournamentId)?.toDomain()
                ?: return@run failure(ApiError.TOURNAMENT_NOT_FOUND)

            val divisionName = division.trim()
            if (divisionName.isEmpty()) return@run failure(ApiError.INVALID_BRACKET_DIVISION)

            val bracketList = brackets.findByTournamentIdAndDivision(tournamentId, divisionName)

            val overview =
                bracketList.map { bracket ->
                    val matches = matches.findByBracketId(bracket.id).map(MatchEntity::toDomain)
                    BracketOverview(bracket = bracket.toDomain(), matches = matches)
                }

            success(overview)
        }

    fun getBracketLeaderboard(bracketId: Int): Either<ApiError, BracketLeaderboard> =
        trx.run {
            val bracket =
                brackets.findByIdOrNull(bracketId)
                    ?: return@run failure(ApiError.BRACKET_NOT_FOUND)

            val times = mutableListOf<Triple<String, Long, Int>>()

            for (match in matches.findByBracketId(bracketId)) {
                val startedAt = match.startedAt ?: continue
                val progress = matchProgresses.findByMatchId(match.id)

                match.athleteRed?.let { athlete ->
                    progress?.redFinishedAt?.let { finishedAt ->
                        times += Triple(athlete.name, (finishedAt - startedAt), match.id)
                    }
                }

                match.athleteBlue?.let { athlete ->
                    progress?.blueFinishedAt?.let { finishedAt ->
                        times += Triple(athlete.name, (finishedAt - startedAt), match.id)
                    }
                }
            }

            val entries =
                times
                    .groupBy { it.first }
                    .map { (name, results) ->
                        val best = results.minBy { it.second }
                        Triple(name, best.second, best.third)
                    }.sortedBy { it.second }
                    .map { (name, millis, matchId) ->
                        LeaderboardEntry(
                            athleteName = name,
                            duration = formatDuration(millis),
                            matchId = matchId,
                        )
                    }

            success(
                BracketLeaderboard(bracket.id, bracket.division, bracket.stage, entries),
            )
        }

    fun getTournamentBracketsSummary(
        tournamentId: Int,
        division: String,
    ): Either<ApiError, TournamentBracketsResponse> =
        trx.run {
            val divisionName = division.trim()
            if (divisionName.isEmpty()) return@run failure(ApiError.INVALID_BRACKET_DIVISION)

            tournaments.findByIdOrNull(tournamentId)
                ?: return@run failure(ApiError.TOURNAMENT_NOT_FOUND)

            val tournamentBrackets = brackets.findByTournamentIdAndDivision(tournamentId, divisionName)

            if (tournamentBrackets.isEmpty()) {
                return@run success(TournamentBracketsResponse(tournamentId, divisionName, emptyList()))
            }

            val summaries =
                tournamentBrackets.map { bracket: BracketEntity ->
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

            success(TournamentBracketsResponse(tournamentId, divisionName, summaries))
        }

    private fun formatDuration(totalMs: Long): String {
        val minutes = totalMs / 60000
        val seconds = (totalMs % 60000) / 1000
        val ms = totalMs % 1000

        return "%d:%02d.%03d".format(minutes, seconds, ms)
    }
}
