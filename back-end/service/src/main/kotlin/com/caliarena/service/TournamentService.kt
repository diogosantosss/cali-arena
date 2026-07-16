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

    data object InvalidStatusTransition : TournamentError()

    data object BracketNotFound : TournamentError()

    data object BracketAlreadyExists : TournamentError()

    data object TournamentStateNotFound : TournamentError()

    data object TournamentStateAlreadyExists : TournamentError()
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

            success(
                repoTournament.createTournament(
                    name = name,
                    location = location,
                    startDate = startDate,
                    endDate = endDate,
                    createdAt = clock.instant(),
                ),
            )
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
        newStatus: TournamentStatus,
    ): Either<TournamentError, Tournament> =
        trx.run {
            val tournament =
                repoTournament.findById(id)
                    ?: return@run failure(TournamentError.TournamentNotFound)

            if (!isValidStatusTransition(tournament.status, newStatus)) {
                return@run failure(TournamentError.InvalidStatusTransition)
            }

            val updated =
                repoTournament.updateStatus(id, newStatus)
                    ?: return@run failure(TournamentError.TournamentNotFound)

            success(updated)
        }

    fun createBracket(
        tournamentId: Int,
        gender: GenderType,
        stage: BracketStage,
    ): Either<TournamentError, Bracket> =
        trx.run {
            repoTournament.findById(tournamentId)
                ?: return@run failure(TournamentError.TournamentNotFound)

            val existing = repoTournament.findBracketsByTournamentIdAndGender(tournamentId, gender)
            if (existing.any { it.stage == stage }) {
                return@run failure(TournamentError.BracketAlreadyExists)
            }

            val bracket =
                repoTournament.createBracket(
                    tournamentId = tournamentId,
                    gender = gender,
                    stage = stage,
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
        gender: GenderType,
    ): Either<TournamentError, List<Bracket>> =
        trx.run {
            repoTournament.findById(tournamentId)
                ?: return@run failure(TournamentError.TournamentNotFound)

            success(repoTournament.findBracketsByTournamentIdAndGender(tournamentId, gender))
        }

    fun getBracketOverview(
        tournamentId: Int,
        gender: GenderType,
    ): Either<TournamentError, List<BracketOverview>> =
        trx.run {
            repoTournament.findById(tournamentId)
                ?: return@run failure(TournamentError.TournamentNotFound)

            val brackets = repoTournament.findBracketsByTournamentIdAndGender(tournamentId, gender)

            val overview =
                brackets.map { bracket ->
                    val matches = repoMatch.findByBracketId(bracket.id)
                    BracketOverview(bracket = bracket, matches = matches)
                }

            success(overview)
        }

    fun initTournamentState(tournamentId: Int): Either<TournamentError, TournamentState> =
        trx.run {
            repoTournament.findById(tournamentId)
                ?: return@run failure(TournamentError.TournamentNotFound)

            if (repoTournament.findStateByTournamentId(tournamentId) != null) {
                return@run failure(TournamentError.TournamentStateAlreadyExists)
            }

            val state =
                repoTournament.createTournamentState(
                    tournamentId = tournamentId,
                    updatedAt = clock.instant(),
                ) ?: return@run failure(TournamentError.TournamentNotFound)

            success(state)
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
        screen: ScreenState,
        currentMatchId: Int?,
    ): Either<TournamentError, TournamentState> =
        trx.run {
            repoTournament.findById(tournamentId)
                ?: return@run failure(TournamentError.TournamentNotFound)

            val updated =
                repoTournament.updateScreen(
                    tournamentId = tournamentId,
                    screen = screen,
                    currentMatchId = currentMatchId,
                    updatedAt = clock.instant(),
                ) ?: return@run failure(TournamentError.TournamentStateNotFound)

            success(updated)
        }

    private fun isValidStatusTransition(
        current: TournamentStatus,
        next: TournamentStatus,
    ): Boolean =
        when (current) {
            TournamentStatus.DRAFT -> next == TournamentStatus.READY
            TournamentStatus.READY -> next == TournamentStatus.LIVE
            TournamentStatus.LIVE -> next == TournamentStatus.FINISHED
            TournamentStatus.FINISHED -> false
        }
}
