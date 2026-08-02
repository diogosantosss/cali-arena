package com.caliarena.service

import com.caliarena.TransactionManager
import com.caliarena.domain.routine.ScreenRoutine
import org.springframework.stereotype.Service
import java.time.Clock

sealed class ScreenRoutineError {
    data object TournamentNotFound : ScreenRoutineError()

    data object RoutineNotFound : ScreenRoutineError()

    data object ScreenRoutineNotFound : ScreenRoutineError()

    data object TournamentMismatch : ScreenRoutineError()

    data object ErrorUpdatingScreenRoutine : ScreenRoutineError()
}

@Service
class ScreenRoutineService(
    private val trx: TransactionManager,
    private val clock: Clock,
) {
    fun getByTournamentId(tournamentId: Int): Either<ScreenRoutineError, List<ScreenRoutine>> =
        trx.run {
            repoTournament.findById(tournamentId)
                ?: return@run failure(ScreenRoutineError.TournamentNotFound)
            success(repoScreenRoutine.findByTournamentId(tournamentId))
        }

    fun create(
        tournamentId: Int,
        routineId: Int,
        displayOrder: Int,
        label: String?,
    ): Either<ScreenRoutineError, ScreenRoutine> =
        trx.run {
            repoTournament.findById(tournamentId)
                ?: return@run failure(ScreenRoutineError.TournamentNotFound)
            repoEnduranceRoutine.findById(routineId)
                ?: return@run failure(ScreenRoutineError.RoutineNotFound)

            success(repoScreenRoutine.create(tournamentId, routineId, displayOrder, label, clock.instant()))
        }

    fun update(
        tournamentId: Int,
        id: Int,
        isVisible: Boolean?,
        displayOrder: Int?,
        label: String?,
    ): Either<ScreenRoutineError, ScreenRoutine> =
        trx.run {
            val existing =
                repoScreenRoutine.findById(id)
                    ?: return@run failure(ScreenRoutineError.ScreenRoutineNotFound)

            if (existing.tournamentId != tournamentId) {
                return@run failure(ScreenRoutineError.TournamentMismatch)
            }

            val result =
                repoScreenRoutine.update(id, isVisible, displayOrder, label, clock.instant())
                    ?: return@run failure(ScreenRoutineError.ErrorUpdatingScreenRoutine)

            success(result)
        }

    fun delete(
        tournamentId: Int,
        id: Int,
    ): Either<ScreenRoutineError, Unit> =
        trx.run {
            val existing =
                repoScreenRoutine.findById(id)
                    ?: return@run failure(ScreenRoutineError.ScreenRoutineNotFound)
            if (existing.tournamentId != tournamentId) {
                return@run failure(ScreenRoutineError.TournamentMismatch)
            }
            repoScreenRoutine.deleteById(id)
            success(Unit)
        }
}
