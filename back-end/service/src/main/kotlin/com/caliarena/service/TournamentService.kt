package com.caliarena.service

import com.caliarena.domain.athlete.GenderType
import com.caliarena.domain.tournament.ScreenState
import com.caliarena.domain.tournament.Tournament
import com.caliarena.domain.tournament.TournamentState
import com.caliarena.domain.tournament.TournamentStatus
import com.caliarena.repo.entities.tournament.TournamentEntity
import com.caliarena.repo.entities.tournament.TournamentStateEntity
import com.caliarena.repo.trx.TransactionManager
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

            val tournament =
                tournaments
                    .save(
                        TournamentEntity(
                            name = name,
                            location = location,
                            startDate = startDate?.epochSecond,
                            endDate = endDate?.epochSecond,
                            status = TournamentStatus.DRAFT,
                            createdAt = clock.instant().epochSecond,
                        ),
                    ).toDomain()

            val tournamentEntity =
                tournaments.findByIdOrNull(tournament.id)
                    ?: return@run failure(ApiError.TOURNAMENT_NOT_FOUND)

            tournamentStates.save(
                TournamentStateEntity(
                    tournament = tournamentEntity,
                    currentScreen = ScreenState.WAITING,
                    updatedAt = clock.instant().epochSecond,
                ),
            )

            success(tournament)
        }

    fun getTournamentById(id: Int): Either<ApiError, Tournament> =
        trx.run {
            val tournament =
                tournaments.findByIdOrNull(id)?.toDomain()
                    ?: return@run failure(ApiError.TOURNAMENT_NOT_FOUND)

            success(tournament)
        }

    fun getAllTournaments(): List<Tournament> =
        trx.run {
            tournaments.findAll().map { it.toDomain() }
        }

    fun getTournamentsByStatus(status: TournamentStatus): List<Tournament> =
        trx.run {
            tournaments.findByStatus(status).map { it.toDomain() }
        }

    fun updateTournamentStatus(
        id: Int,
        newStatus: String,
    ): Either<ApiError, Tournament> =
        trx.run {
            val existing =
                tournaments.findByIdOrNull(id)
                    ?: return@run failure(ApiError.TOURNAMENT_NOT_FOUND)

            val status =
                TournamentStatus.entries.find { it.name.equals(newStatus, true) }
                    ?: return@run failure(ApiError.INVALID_TOURNAMENT_STATUS)

            existing.status = status

            success(tournaments.save(existing).toDomain())
        }

    fun getTournamentState(tournamentId: Int): Either<ApiError, TournamentState> =
        trx.run {
            tournaments.findByIdOrNull(tournamentId)?.toDomain()
                ?: return@run failure(ApiError.TOURNAMENT_NOT_FOUND)

            val state =
                tournamentStates.findByTournamentId(tournamentId)?.toDomain()
                    ?: return@run failure(ApiError.TOURNAMENT_STATE_NOT_FOUND)

            success(state)
        }

    fun updateScreen(
        tournamentId: Int,
        screen: String,
        currentMatchId: Int?,
        currentBracketId: Int?,
        gender: String?,
    ): Either<ApiError, TournamentState> =
        trx.run {
            tournaments.findByIdOrNull(tournamentId)?.toDomain()
                ?: return@run failure(ApiError.TOURNAMENT_NOT_FOUND)

            val screenState =
                ScreenState.entries.find { it.name.equals(screen, true) }
                    ?: return@run failure(ApiError.INVALID_SCREEN_STATE)

            val bracket =
                currentBracketId?.let { id ->
                    val found = brackets.findByIdOrNull(id) ?: return@run failure(ApiError.BRACKET_NOT_FOUND)
                    if (found.tournament.id != tournamentId) return@run failure(ApiError.BRACKET_NOT_FOUND)
                    found
                }

            val currentGender =
                gender?.let {
                    GenderType.entries.find { g -> g.name.equals(it, true) }
                        ?: return@run failure(ApiError.INVALID_GENDER)
                }

            val state =
                tournamentStates.findByTournamentId(tournamentId)
                    ?: return@run failure(ApiError.TOURNAMENT_STATE_NOT_FOUND)

            state.currentScreen = screenState
            state.currentMatch = currentMatchId?.let { matches.findByIdOrNull(it) }
            state.currentBracket = bracket
            state.currentGender = currentGender
            state.updatedAt = clock.instant().epochSecond

            val updated = tournamentStates.save(state).toDomain()

            TournamentStateUpdatedEvent(
                tournamentId = tournamentId,
                state = updated,
                currentMatchId = currentMatchId,
            ).let { publisher.publish(it) }

            success(updated)
        }
}
