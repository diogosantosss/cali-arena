package com.caliarena.service

import com.caliarena.domain.athlete.GenderType
import com.caliarena.domain.bracket.Bracket
import com.caliarena.domain.bracket.BracketLeaderboard
import com.caliarena.domain.bracket.BracketMatchSummary
import com.caliarena.domain.bracket.BracketOverview
import com.caliarena.domain.bracket.BracketStage
import com.caliarena.domain.bracket.BracketSummary
import com.caliarena.domain.bracket.LeaderboardEntry
import com.caliarena.domain.bracket.TournamentBracketsResponse
import com.caliarena.domain.match.MatchStatus
import com.caliarena.repo.entities.match.MatchEntity
import com.caliarena.repo.entities.tournament.BracketEntity
import com.caliarena.repo.trx.TransactionManager
import jakarta.inject.Named
import org.springframework.data.repository.findByIdOrNull
import java.time.Clock
import java.time.Duration
import java.time.Instant

sealed class BracketError {
    data object BracketNotFound : BracketError()

    data object BracketAlreadyExists : BracketError()

    data object InvalidBracketStage : BracketError()

    data object InvalidGender : BracketError()

    data object TournamentNotFound : BracketError()

    data object MatchNotFinished : BracketError()

    data object InvalidTournamentStatus : BracketError()
}

@Named
class BracketService(
    private val trx: TransactionManager,
    private val clock: Clock,
) {
    fun createBracket(
        tournamentId: Int,
        gender: String,
        stage: String,
    ): Either<BracketError, Bracket> =
        trx.run {
            val tournament =
                tournaments.findByIdOrNull(tournamentId)
                    ?: return@run failure(BracketError.TournamentNotFound)

            val genderType =
                GenderType.entries.find { it.name.equals(gender, true) }
                    ?: return@run failure(BracketError.InvalidGender)

            val bracketStage =
                BracketStage.entries.find { it.name.equals(stage, true) }
                    ?: return@run failure(BracketError.InvalidBracketStage)

            val existing = brackets.findByTournamentIdAndGender(tournamentId, genderType)
            if (existing.any { it.stage == bracketStage }) {
                return@run failure(BracketError.BracketAlreadyExists)
            }

            val now = clock.instant()

            val bracket =
                brackets
                    .save(
                        BracketEntity(
                            tournament = tournament,
                            gender = genderType,
                            stage = bracketStage,
                            createdAt = now.epochSecond,
                        ),
                    ).toDomain()

            success(bracket)
        }

    fun getBracketsByTournament(tournamentId: Int): Either<BracketError, List<Bracket>> =
        trx.run {
            tournaments.findByIdOrNull(tournamentId)?.toDomain()
                ?: return@run failure(BracketError.TournamentNotFound)

            success(brackets.findByTournamentId(tournamentId).map(BracketEntity::toDomain))
        }

    fun getBracketsByTournamentAndGender(
        tournamentId: Int,
        gender: String,
    ): Either<BracketError, List<Bracket>> =
        trx.run {
            tournaments.findByIdOrNull(tournamentId)?.toDomain()
                ?: return@run failure(BracketError.TournamentNotFound)

            val genderType =
                GenderType.entries.find { it.name.equals(gender, true) }
                    ?: return@run failure(BracketError.InvalidGender)

            success(brackets.findByTournamentIdAndGender(tournamentId, genderType).map(BracketEntity::toDomain))
        }

    fun getBracketOverview(
        tournamentId: Int,
        gender: String,
    ): Either<BracketError, List<BracketOverview>> =
        trx.run {
            tournaments.findByIdOrNull(tournamentId)?.toDomain()
                ?: return@run failure(BracketError.TournamentNotFound)

            val genderType =
                GenderType.entries.find { it.name.equals(gender, true) }
                    ?: return@run failure(BracketError.InvalidGender)

            val bracketList = brackets.findByTournamentIdAndGender(tournamentId, genderType)

            val overview =
                bracketList.map { bracket ->
                    val matches = matches.findByBracketId(bracket.id).map(MatchEntity::toDomain)
                    BracketOverview(bracket = bracket.toDomain(), matches = matches)
                }

            success(overview)
        }

    fun getBracketLeaderboard(bracketId: Int): Either<BracketError, BracketLeaderboard> =
        trx.run {
            val bracket =
                brackets.findByIdOrNull(bracketId)
                    ?: return@run failure(BracketError.BracketNotFound)

            val bracketMatches = matches.findByBracketId(bracketId)

            if (bracketMatches.isEmpty()) {
                return@run success(BracketLeaderboard(bracket.id, bracket.gender, bracket.stage, emptyList()))
            }

            if (bracketMatches.any { it.status != MatchStatus.FINISHED }) {
                return@run failure(BracketError.MatchNotFinished)
            }

            val entries = mutableListOf<LeaderboardEntry>()

            for (match in bracketMatches) {
                val progress = matchProgresses.findByMatchId(match.id)

                match.athleteRed?.let {
                    entries +=
                        LeaderboardEntry(
                            athleteName = it.name,
                            duration =
                                formatMatchTime(
                                    match.startedAt!!,
                                    progress?.redFinishedAt!!,
                                ),
                            matchId = match.id,
                        )
                }

                match.athleteBlue?.let {
                    entries +=
                        LeaderboardEntry(
                            athleteName = it.name,
                            duration =
                                formatMatchTime(
                                    match.startedAt!!,
                                    progress?.blueFinishedAt!!,
                                ),
                            matchId = match.id,
                        )
                }
            }

            entries.sortBy(LeaderboardEntry::duration)

            success(
                BracketLeaderboard(bracket.id, bracket.gender, bracket.stage, entries),
            )
        }

    fun getTournamentBracketsSummary(
        tournamentId: Int,
        gender: String,
    ): Either<BracketError, TournamentBracketsResponse> =
        trx.run {
            val validGender =
                GenderType.entries.find { it.name.equals(gender, true) }
                    ?: return@run failure(BracketError.InvalidGender)

            tournaments.findByIdOrNull(tournamentId)
                ?: return@run failure(BracketError.TournamentNotFound)

            val tournamentBrackets = brackets.findByTournamentIdAndGender(tournamentId, validGender)

            if (tournamentBrackets.isEmpty()) {
                return@run success(TournamentBracketsResponse(tournamentId, validGender, emptyList()))
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

            success(TournamentBracketsResponse(tournamentId, validGender, summaries))
        }

    private fun formatMatchTime(
        startedAt: Long,
        finishedAt: Long,
    ): String {
        val totalMs =
            Duration
                .between(
                    Instant.ofEpochMilli(startedAt),
                    Instant.ofEpochMilli(finishedAt),
                ).toMillis()

        val minutes = totalMs / 60000
        val seconds = (totalMs % 60000) / 1000
        val ms = totalMs % 1000

        return "%d:%02d.%03d".format(minutes, seconds, ms)
    }
}
