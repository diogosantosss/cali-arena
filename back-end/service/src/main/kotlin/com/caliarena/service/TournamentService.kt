package com.caliarena.service

import com.caliarena.domain.athlete.GenderType
import com.caliarena.domain.bracket.Bracket
import com.caliarena.domain.bracket.BracketOverview
import com.caliarena.domain.bracket.BracketStage
import com.caliarena.domain.tournament.ScreenState
import com.caliarena.domain.tournament.Tournament
import com.caliarena.domain.tournament.TournamentState
import com.caliarena.domain.tournament.TournamentStatus
import com.caliarena.repo.entities.match.MatchEntity
import com.caliarena.repo.entities.tournament.BracketEntity
import com.caliarena.repo.entities.tournament.TournamentEntity
import com.caliarena.repo.entities.tournament.TournamentStateEntity
import com.caliarena.repo.trx.TransactionManager
import com.caliarena.service.sse.SpectatorPublisher
import com.caliarena.service.sse.TournamentStateUpdatedEvent
import jakarta.inject.Named
import org.springframework.data.repository.findByIdOrNull
import java.time.Clock
import java.time.Instant

sealed class TournamentError {
    data object TournamentNotFound : TournamentError()

    data object TournamentAlreadyExists : TournamentError()

    data object InvalidTournamentStatus : TournamentError()

    data object BracketNotFound : TournamentError()

    data object BracketAlreadyExists : TournamentError()

    data object TournamentStateNotFound : TournamentError()

    data object TournamentStateAlreadyExists : TournamentError()

    data object InvalidBracketStage : TournamentError()

    data object InvalidGender : TournamentError()

    data object InvalidScreenState : TournamentError()
}

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
    ): Either<TournamentError, Tournament> =
        trx.run {
            if (tournaments.findByName(name) != null) {
                return@run failure(TournamentError.TournamentAlreadyExists)
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
                    ?: return@run failure(TournamentError.TournamentNotFound)

            tournamentStates.save(
                TournamentStateEntity(
                    tournament = tournamentEntity,
                    currentScreen = ScreenState.WAITING,
                    updatedAt = clock.instant().epochSecond,
                ),
            )

            success(tournament)
        }

    fun getTournamentById(id: Int): Either<TournamentError, Tournament> =
        trx.run {
            val tournament =
                tournaments.findByIdOrNull(id)?.toDomain()
                    ?: return@run failure(TournamentError.TournamentNotFound)

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
    ): Either<TournamentError, Tournament> =
        trx.run {
            val existing =
                tournaments.findByIdOrNull(id)
                    ?: return@run failure(TournamentError.TournamentNotFound)

            val status =
                TournamentStatus.entries.find { it.name.equals(newStatus, true) }
                    ?: return@run failure(TournamentError.InvalidTournamentStatus)

            existing.status = status

            success(tournaments.save(existing).toDomain())
        }

    fun createBracket(
        tournamentId: Int,
        gender: String,
        stage: String,
    ): Either<TournamentError, Bracket> =
        trx.run {
            tournaments.findByIdOrNull(tournamentId)?.toDomain()
                ?: return@run failure(TournamentError.TournamentNotFound)

            val genderType =
                GenderType.entries.find { it.name.equals(gender, true) }
                    ?: return@run failure(TournamentError.InvalidGender)

            val bracketStage =
                BracketStage.entries.find { it.name.equals(stage, true) }
                    ?: return@run failure(TournamentError.InvalidBracketStage)

            val existing = brackets.findByTournamentIdAndGender(tournamentId, genderType)
            if (existing.any { it.stage == bracketStage }) {
                return@run failure(TournamentError.BracketAlreadyExists)
            }

            val bracket =
                brackets
                    .save(
                        BracketEntity(
                            tournament = tournaments.findByIdOrNull(tournamentId)!!,
                            gender = genderType,
                            stage = bracketStage,
                            createdAt = clock.instant().epochSecond,
                        ),
                    ).toDomain()

            success(bracket)
        }

    fun getBracketsByTournament(tournamentId: Int): Either<TournamentError, List<Bracket>> =
        trx.run {
            tournaments.findByIdOrNull(tournamentId)?.toDomain()
                ?: return@run failure(TournamentError.TournamentNotFound)

            success(brackets.findByTournamentId(tournamentId).map(BracketEntity::toDomain))
        }

    fun getBracketsByTournamentAndGender(
        tournamentId: Int,
        gender: String,
    ): Either<TournamentError, List<Bracket>> =
        trx.run {
            tournaments.findByIdOrNull(tournamentId)?.toDomain()
                ?: return@run failure(TournamentError.TournamentNotFound)

            val genderType =
                GenderType.entries.find { it.name.equals(gender, true) }
                    ?: return@run failure(TournamentError.InvalidGender)

            success(brackets.findByTournamentIdAndGender(tournamentId, genderType).map(BracketEntity::toDomain))
        }

    fun getBracketOverview(
        tournamentId: Int,
        gender: String,
    ): Either<TournamentError, List<BracketOverview>> =
        trx.run {
            tournaments.findByIdOrNull(tournamentId)?.toDomain()
                ?: return@run failure(TournamentError.TournamentNotFound)

            val genderType =
                GenderType.entries.find { it.name.equals(gender, true) }
                    ?: return@run failure(TournamentError.InvalidGender)

            val bracketList = brackets.findByTournamentIdAndGender(tournamentId, genderType)

            val overview =
                bracketList.map { bracket ->
                    val matches = matches.findByBracketId(bracket.id).map(MatchEntity::toDomain)
                    BracketOverview(bracket = bracket.toDomain(), matches = matches)
                }

            success(overview)
        }

    fun getTournamentState(tournamentId: Int): Either<TournamentError, TournamentState> =
        trx.run {
            tournaments.findByIdOrNull(tournamentId)?.toDomain()
                ?: return@run failure(TournamentError.TournamentNotFound)

            val state =
                tournamentStates.findByTournamentId(tournamentId)?.toDomain()
                    ?: return@run failure(TournamentError.TournamentStateNotFound)

            success(state)
        }

    fun updateScreen(
        tournamentId: Int,
        screen: String,
        currentMatchId: Int?,
    ): Either<TournamentError, TournamentState> =
        trx.run {
            tournaments.findByIdOrNull(tournamentId)?.toDomain()
                ?: return@run failure(TournamentError.TournamentNotFound)

            val screenState =
                ScreenState.entries.find { it.name.equals(screen, true) }
                    ?: return@run failure(TournamentError.InvalidScreenState)

            val state =
                tournamentStates.findByTournamentId(tournamentId)
                    ?: return@run failure(TournamentError.TournamentStateNotFound)

            state.currentScreen = screenState
            state.currentMatch = currentMatchId?.let { matches.findByIdOrNull(it) }
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
