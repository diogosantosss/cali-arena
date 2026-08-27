package com.caliarena.service

import com.caliarena.domain.routine.Exercise
import com.caliarena.domain.routine.RoutineOverview
import com.caliarena.domain.routine.ScreenRoutine
import com.caliarena.repo.entities.routine.ExerciseEntity
import com.caliarena.repo.entities.routine.ScreenRoutineEntity
import com.caliarena.repo.trx.TransactionManager
import com.caliarena.service.sse.ScreenRoutineDeletedEvent
import com.caliarena.service.sse.ScreenRoutinesEvent
import com.caliarena.service.sse.SpectatorAction
import com.caliarena.service.sse.SpectatorPublisher
import org.springframework.data.repository.findByIdOrNull
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
            tournaments.findByIdOrNull(tournamentId)?.toDomain()
                ?: return@run failure(ScreenRoutineError.TournamentNotFound)

            success(screenRoutines.findByTournamentIdOrderByDisplayOrder(tournamentId).map(ScreenRoutineEntity::toDomain))
        }

    fun create(
        tournamentId: Int,
        routineId: Int,
        displayOrder: Int,
        label: String?,
    ): Either<ScreenRoutineError, ScreenRoutine> =
        trx.run {
            tournaments.findByIdOrNull(tournamentId)
                ?: return@run failure(ScreenRoutineError.TournamentNotFound)

            val routine =
                routines.findByIdOrNull(routineId)?.toDomain()
                    ?: return@run failure(ScreenRoutineError.RoutineNotFound)

            val now = clock.instant()

            val screenRoutine =
                screenRoutines
                    .save(
                        ScreenRoutineEntity(
                            tournamentId = tournamentId,
                            routineId = routineId,
                            displayOrder = displayOrder,
                            isVisible = true,
                            label = label,
                            createdAt = now.epochSecond,
                            updatedAt = now.epochSecond,
                        ),
                    ).toDomain()

            val routineOverview: RoutineOverview =
                exercises
                    .findExercisesByRoutineId(routine.id)
                    .map(ExerciseEntity::toDomain)
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
                screenRoutines.findByIdOrNull(id)
                    ?: return@run failure(ScreenRoutineError.ScreenRoutineNotFound)

            if (existing.tournamentId != tournamentId) {
                return@run failure(ScreenRoutineError.TournamentMismatch)
            }

            isVisible?.let { existing.isVisible = it }
            displayOrder?.let { existing.displayOrder = it }
            label?.let { existing.label = it }
            existing.updatedAt = clock.instant().epochSecond

            val result = screenRoutines.save(existing).toDomain()

            val routine =
                routines.findByIdOrNull(result.routineId)?.toDomain()
                    ?: return@run failure(ScreenRoutineError.RoutineNotFound)

            val routineOverview =
                exercises
                    .findExercisesByRoutineId(routine.id)
                    .map(ExerciseEntity::toDomain)
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
                screenRoutines.findByIdOrNull(id)
                    ?: return@run failure(ScreenRoutineError.ScreenRoutineNotFound)

            if (existing.tournamentId != tournamentId) {
                return@run failure(ScreenRoutineError.TournamentMismatch)
            }

            screenRoutines.deleteById(id)

            publisher.publish(
                ScreenRoutineDeletedEvent(
                    tournamentId,
                    screenRoutineId = id,
                ),
            )

            success(Unit)
        }
}
