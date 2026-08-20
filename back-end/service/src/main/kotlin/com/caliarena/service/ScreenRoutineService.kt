package com.caliarena.service

import com.caliarena.TransactionManager
import com.caliarena.domain.routine.Exercise
import com.caliarena.domain.routine.RoutineOverview
import com.caliarena.domain.routine.ScreenRoutine
import com.caliarena.service.sse.ScreenRoutineDeletedEvent
import com.caliarena.service.sse.ScreenRoutinesEvent
import com.caliarena.service.sse.SpectatorAction
import com.caliarena.service.sse.SpectatorPublisher
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
    private val publisher: SpectatorPublisher,
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
            val routine =
                repoEnduranceRoutine.findById(routineId)
                    ?: return@run failure(ScreenRoutineError.RoutineNotFound)

            val screenRoutine: ScreenRoutine =
                repoScreenRoutine.create(tournamentId, routineId, displayOrder, label, clock.instant())

            val routineOverview: RoutineOverview =
                repoEnduranceRoutine
                    .findExercisesByRoutineId(routine.id)
                    .sortedBy(Exercise::exerciseOrder)
                    .let { RoutineOverview(routine.name, routine.timeCapSeconds, routine.createdAt, it) }

            publisher.publish(
                ScreenRoutinesEvent(
                    tournamentId,
                    SpectatorAction.SCREEN_ROUTINES_CREATED,
                    screenRoutine,
                    routineOverview,
                ),
            )

            success(screenRoutine)
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

            val routine =
                repoEnduranceRoutine.findById(result.routineId)
                    ?: return@run failure(ScreenRoutineError.RoutineNotFound)

            val routineOverview =
                repoEnduranceRoutine
                    .findExercisesByRoutineId(routine.id)
                    .sortedBy(Exercise::exerciseOrder)
                    .let { exercises -> RoutineOverview(routine.name, routine.timeCapSeconds, routine.createdAt, exercises) }

            publisher.publish(
                ScreenRoutinesEvent(
                    tournamentId,
                    SpectatorAction.SCREEN_ROUTINES_UPDATED,
                    result,
                    routineOverview,
                ),
            )
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

            publisher.publish(
                ScreenRoutineDeletedEvent(
                    tournamentId,
                    screenRoutineId = id,
                ),
            )

            success(Unit)
        }
}
