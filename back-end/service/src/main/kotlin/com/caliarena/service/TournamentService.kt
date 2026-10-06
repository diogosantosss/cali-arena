package com.caliarena.service

import com.caliarena.domain.bracket.BracketLeaderboard
import com.caliarena.domain.bracket.TournamentBracketsResponse
import com.caliarena.domain.tournament.ScreenState
import com.caliarena.domain.tournament.ScreenState.WAITING
import com.caliarena.domain.tournament.Tournament
import com.caliarena.domain.tournament.TournamentState
import com.caliarena.domain.tournament.TournamentStatus
import com.caliarena.domain.user.User
import com.caliarena.repo.entities.tournament.TournamentEntity
import com.caliarena.repo.entities.tournament.TournamentStateEntity
import com.caliarena.repo.trx.Transaction
import com.caliarena.repo.trx.TransactionManager
import com.caliarena.service.screen.ScreenEffect
import com.caliarena.service.screen.ScreenEffectHandler
import com.caliarena.service.sse.SpectatorPublisher
import com.caliarena.service.sse.TournamentStateUpdatedEvent
import jakarta.inject.Named
import org.springframework.data.repository.findByIdOrNull
import java.time.Clock
import java.time.Instant

@Named
class TournamentService(
    private val trx: TransactionManager,
    private val clock: Clock,
    private val publisher: SpectatorPublisher,
    private val screenHandlers: List<ScreenEffectHandler>,
) {
    fun createTournament(
        name: String,
        location: String?,
        startDate: Instant?,
        endDate: Instant?,
    ): Either<ApiError, Tournament> =
        trx.run {
            if (tournaments.findByName(name) != null) {
                return@run failure(ApiError.TOURNAMENT_ALREADY_EXISTS)
            }

            val entity =
                tournaments.save(
                    TournamentEntity(
                        name = name,
                        location = location,
                        startDate = startDate?.epochSecond,
                        endDate = endDate?.epochSecond,
                        status = TournamentStatus.DRAFT,
                        createdAt = clock.instant().epochSecond,
                    ),
                )

            tournamentStates.save(
                TournamentStateEntity(
                    tournament = entity,
                    currentScreen = WAITING,
                    updatedAt = clock.instant().epochSecond,
                ),
            )

            success(entity.toDomain())
        }

    fun getTournamentById(id: Int): Either<ApiError, Tournament> =
        trx.run {
            val tournament =
                tournaments.findByIdOrNull(id)?.toDomain()
                    ?: return@run failure(ApiError.TOURNAMENT_NOT_FOUND)

            success(tournament)
        }

    fun getAllTournaments(user: User): List<Tournament> =
        trx.run {
            val visible =
                visibleTournamentIds(user)
                    ?: return@run tournaments.findAll().map { it.toDomain() }

            tournaments.findAllById(visible).map { it.toDomain() }
        }

    fun getTournamentsByStatus(status: TournamentStatus): List<Tournament> =
        trx.run {
            tournaments.findByStatus(status).map { it.toDomain() }
        }

    fun updateTournamentStatus(
        user: User,
        id: Int,
        newStatus: String,
    ): Either<ApiError, Tournament> =
        trx.run {
            val existing =
                tournaments.findByIdOrNull(id)
                    ?: return@run failure(ApiError.TOURNAMENT_NOT_FOUND)

            if (!canManageTournament(user, id)) {
                return@run failure(ApiError.NOT_AUTHORIZED)
            }

            val status =
                TournamentStatus.entries.find { it.name.equals(newStatus, true) }
                    ?: return@run failure(ApiError.INVALID_TOURNAMENT_STATUS)

            existing.status = status

            success(tournaments.save(existing).toDomain())
        }

    /**
     * Removes a tournament.
     *
     * Everything hanging off it — `tournament_state`, `brackets`, `matches`,
     * `match_progress`, `tournament_judges` and `screen_routines` — is declared
     * `ON DELETE CASCADE`, so the database removes the whole tree with this single
     * row. `endurance_routines`, `exercises`, `athletes` and `clubs` are global and
     * stay untouched.
     */
    fun deleteTournament(id: Int): Either<ApiError, Unit> =
        trx.run {
            val tournament =
                tournaments.findByIdOrNull(id)
                    ?: return@run failure(ApiError.TOURNAMENT_NOT_FOUND)

            tournaments.delete(tournament)

            success(Unit)
        }

    fun getTournamentState(tournamentId: Int): Either<ApiError, TournamentState> =
        trx.run {
            tournaments.findByIdOrNull(tournamentId)?.toDomain()
                ?: return@run failure(ApiError.TOURNAMENT_NOT_FOUND)

            val state =
                tournamentStates.findByTournamentId(tournamentId)
                    ?: return@run failure(ApiError.TOURNAMENT_STATE_NOT_FOUND)

            success(state.toDomain())
        }

    fun updateScreen(
        user: User,
        tournamentId: Int,
        screen: String,
        currentMatchId: Int?,
        currentBracketId: Int?,
        currentDivision: String?,
    ): Either<ApiError, TournamentState> =
        trx.run {
            tournaments.findByIdOrNull(tournamentId)?.toDomain()
                ?: return@run failure(ApiError.TOURNAMENT_NOT_FOUND)

            if (!canManageTournament(user, tournamentId)) {
                return@run failure(ApiError.NOT_AUTHORIZED)
            }

            val screenState =
                ScreenState.entries.find { it.name.equals(screen, true) }
                    ?: return@run failure(ApiError.INVALID_SCREEN_STATE)

            val bracket =
                currentBracketId?.let { bracketId ->
                    brackets
                        .findByIdOrNull(bracketId)
                        ?.takeIf { it.tournament.id == tournamentId }
                        ?: return@run failure(ApiError.BRACKET_NOT_FOUND)
                }

            val division =
                currentDivision?.let { raw ->
                    resolveDivision(tournamentId, raw)
                        ?: return@run failure(ApiError.INVALID_BRACKET_DIVISION)
                }

            val state =
                tournamentStates.findByTournamentId(tournamentId)
                    ?: return@run failure(ApiError.TOURNAMENT_STATE_NOT_FOUND)

            state.currentScreen = screenState
            state.currentMatch = currentMatchId?.let { matches.findByIdOrNull(it) }
            state.currentBracket = bracket
            state.currentDivision = division
            state.updatedAt = clock.instant().epochSecond

            val updated = tournamentStates.save(state).toDomain()

            val effect =
                screenHandlers
                    .firstOrNull { it.supports(screenState) }
                    ?.handle(this, tournamentId, updated)
                    ?: ScreenEffect.None

            publish(tournamentId, updated, currentMatchId, effect)

            success(updated)
        }

    private fun Transaction.resolveDivision(
        tournamentId: Int,
        raw: String?,
    ): String? {
        val name =
            raw?.trim().takeUnless { it.isNullOrEmpty() }
                ?: return null

        val hasBracket =
            brackets
                .findByTournamentIdAndDivision(tournamentId, name)
                .isNotEmpty()

        return name.takeIf { hasBracket }
    }

    private fun publish(
        tournamentId: Int,
        state: TournamentState,
        currentMatchId: Int?,
        effect: ScreenEffect,
    ) {
        var leaderboard: BracketLeaderboard? = null
        var brackets: TournamentBracketsResponse? = null

        when (effect) {
            is ScreenEffect.Leaderboard -> leaderboard = effect.leaderboard
            is ScreenEffect.Brackets -> brackets = effect.summary
            is ScreenEffect.Routines -> effect.events.forEach(publisher::publish)
            ScreenEffect.None -> Unit
        }

        publisher.publish(
            TournamentStateUpdatedEvent(
                tournamentId = tournamentId,
                state = state,
                currentMatchId = currentMatchId,
                leaderboard = leaderboard,
                bracketSummary = brackets,
            ),
        )
    }
}
