package com.caliarena.service

import com.caliarena.domain.bracket.Bracket
import com.caliarena.domain.bracket.BracketLeaderboard
import com.caliarena.domain.bracket.BracketOverview
import com.caliarena.domain.bracket.BracketStage
import com.caliarena.domain.bracket.TournamentBracketsResponse
import com.caliarena.domain.user.User
import com.caliarena.repo.entities.match.MatchEntity
import com.caliarena.repo.entities.tournament.BracketEntity
import com.caliarena.repo.trx.TransactionManager
import com.caliarena.service.mapper.BracketLeaderboardMapper
import com.caliarena.service.mapper.BracketSummaryMapper
import jakarta.inject.Named
import org.springframework.data.repository.findByIdOrNull
import java.time.Clock

@Named
class BracketService(
    private val trx: TransactionManager,
    private val clock: Clock,
    private val leaderboardMapper: BracketLeaderboardMapper,
    private val summaryMapper: BracketSummaryMapper,
) {
    fun createBracket(
        user: User,
        tournamentId: Int,
        division: String,
        stage: String,
    ): Either<ApiError, Bracket> =
        trx.run {
            val tournament =
                tournaments.findByIdOrNull(tournamentId)
                    ?: return@run failure(ApiError.TOURNAMENT_NOT_FOUND)

            if (!canManageTournament(user, tournamentId)) {
                return@run failure(ApiError.NOT_AUTHORIZED)
            }

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

    fun getBracketById(bracketId: Int): Either<ApiError, Bracket> =
        trx.run {
            val bracket =
                brackets.findByIdOrNull(bracketId)
                    ?: return@run failure(ApiError.BRACKET_NOT_FOUND)

            success(bracket.toDomain())
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

            val brackets =
                brackets
                    .findByTournamentIdAndDivision(tournamentId, divisionName)
                    .map(BracketEntity::toDomain)

            success(brackets)
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
                    val matches =
                        matches
                            .findByBracketId(bracket.id)
                            .map(MatchEntity::toDomain)

                    BracketOverview(bracket = bracket.toDomain(), matches = matches)
                }

            success(overview)
        }

    fun getBracketLeaderboard(bracketId: Int): Either<ApiError, BracketLeaderboard> =
        trx.run {
            val leaderboard =
                leaderboardMapper.build(this, bracketId)
                    ?: return@run failure(ApiError.BRACKET_NOT_FOUND)

            success(leaderboard)
        }

    fun getTournamentBracketsSummary(
        tournamentId: Int,
        division: String,
    ): Either<ApiError, TournamentBracketsResponse> =
        trx.run {
            tournaments.findByIdOrNull(tournamentId)
                ?: return@run failure(ApiError.TOURNAMENT_NOT_FOUND)

            val divisionName =
                division.trimOrNull()
                    ?: return@run failure(ApiError.INVALID_BRACKET_DIVISION)

            success(summaryMapper.build(this, tournamentId, divisionName))
        }

    private fun String.trimOrNull(): String? = trim().takeUnless { it.isEmpty() }
}
