package com.caliarena.service

import com.caliarena.TransactionManager
import com.caliarena.domain.athlete.GenderType
import com.caliarena.domain.bracket.Bracket
import com.caliarena.domain.bracket.BracketOverview
import com.caliarena.domain.bracket.BracketStage
import com.caliarena.domain.tournament.ScreenState
import com.caliarena.domain.tournament.Tournament
import com.caliarena.domain.tournament.TournamentState
import com.caliarena.domain.tournament.TournamentStatus
import jakarta.inject.Named
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
) {
    fun createTournament(
        name: String,
        location: String?,
        startDate: Instant?,
        endDate: Instant?,
    ): Either<TournamentError, Tournament> =
        trx.run {
            if (repoTournament.findByName(name) != null) {
                return@run failure(TournamentError.TournamentAlreadyExists)
            }

            val tournament =
                repoTournament.createTournament(
                    name = name,
                    location = location,
                    startDate = startDate,
                    endDate = endDate,
                    createdAt = clock.instant(),
                )

            repoTournament.createTournamentState(
                tournamentId = tournament.id,
                updatedAt = clock.instant(),
            ) ?: return@run failure(TournamentError.TournamentNotFound)

            success(tournament)
        }

    fun getTournamentById(id: Int): Either<TournamentError, Tournament> =
        trx.run {
            val tournament =
                repoTournament.findById(id)
                    ?: return@run failure(TournamentError.TournamentNotFound)

            success(tournament)
        }

    fun getAllTournaments(): List<Tournament> =
        trx.run {
            repoTournament.findAll()
        }

    fun getTournamentsByStatus(status: TournamentStatus): List<Tournament> =
        trx.run {
            repoTournament.findByStatus(status)
        }

    fun updateTournamentStatus(
        id: Int,
        newStatus: String,
    ): Either<TournamentError, Tournament> =
        trx.run {
            repoTournament.findById(id)
                ?: return@run failure(TournamentError.TournamentNotFound)

            val status =
                TournamentStatus.entries.find { it.name.equals(newStatus, true) }
                    ?: return@run failure(TournamentError.InvalidTournamentStatus)

            val updated =
                repoTournament.updateStatus(id, status)
                    ?: return@run failure(TournamentError.TournamentNotFound)

            success(updated)
        }

    fun createBracket(
        tournamentId: Int,
        gender: String,
        stage: String,
    ): Either<TournamentError, Bracket> =
        trx.run {
            repoTournament.findById(tournamentId)
                ?: return@run failure(TournamentError.TournamentNotFound)

            val genderType =
                GenderType.entries.find { it.name.equals(gender, true) }
                    ?: return@run failure(TournamentError.InvalidGender)

            val bracketStage =
                BracketStage.entries.find { it.name.equals(stage, true) }
                    ?: return@run failure(TournamentError.InvalidBracketStage)

            val existing = repoTournament.findBracketsByTournamentIdAndGender(tournamentId, genderType)
            if (existing.any { it.stage == bracketStage }) {
                return@run failure(TournamentError.BracketAlreadyExists)
            }

            val bracket =
                repoTournament.createBracket(
                    tournamentId = tournamentId,
                    gender = genderType,
                    stage = bracketStage,
                    createdAt = clock.instant(),
                ) ?: return@run failure(TournamentError.TournamentNotFound)

            success(bracket)
        }

    fun getBracketsByTournament(tournamentId: Int): Either<TournamentError, List<Bracket>> =
        trx.run {
            repoTournament.findById(tournamentId)
                ?: return@run failure(TournamentError.TournamentNotFound)

            success(repoTournament.findBracketsByTournamentId(tournamentId))
        }

    fun getBracketsByTournamentAndGender(
        tournamentId: Int,
        gender: String,
    ): Either<TournamentError, List<Bracket>> =
        trx.run {
            repoTournament.findById(tournamentId)
                ?: return@run failure(TournamentError.TournamentNotFound)

            val genderType =
                GenderType.entries.find { it.name.equals(gender, true) }
                    ?: return@run failure(TournamentError.InvalidGender)

            success(repoTournament.findBracketsByTournamentIdAndGender(tournamentId, genderType))
        }

    fun getBracketOverview(
        tournamentId: Int,
        gender: String,
    ): Either<TournamentError, List<BracketOverview>> =
        trx.run {
            repoTournament.findById(tournamentId)
                ?: return@run failure(TournamentError.TournamentNotFound)

            val genderType =
                GenderType.entries.find { it.name.equals(gender, true) }
                    ?: return@run failure(TournamentError.InvalidGender)

            val brackets = repoTournament.findBracketsByTournamentIdAndGender(tournamentId, genderType)

            val overview =
                brackets.map { bracket ->
                    val matches = repoMatch.findByBracketId(bracket.id)
                    BracketOverview(bracket = bracket, matches = matches)
                }

            success(overview)
        }

    fun getTournamentState(tournamentId: Int): Either<TournamentError, TournamentState> =
        trx.run {
            repoTournament.findById(tournamentId)
                ?: return@run failure(TournamentError.TournamentNotFound)

            val state =
                repoTournament.findStateByTournamentId(tournamentId)
                    ?: return@run failure(TournamentError.TournamentStateNotFound)

            success(state)
        }

    fun updateScreen(
        tournamentId: Int,
        screen: String,
        currentMatchId: Int?,
    ): Either<TournamentError, TournamentState> =
        trx.run {
            repoTournament.findById(tournamentId)
                ?: return@run failure(TournamentError.TournamentNotFound)

            val screenState =
                ScreenState.entries.find { it.name.equals(screen, true) }
                    ?: return@run failure(TournamentError.InvalidScreenState)

            val updated =
                repoTournament.updateScreen(
                    tournamentId = tournamentId,
                    screen = screenState,
                    currentMatchId = currentMatchId,
                    updatedAt = clock.instant(),
                ) ?: return@run failure(TournamentError.TournamentStateNotFound)

            success(updated)
        }
}
