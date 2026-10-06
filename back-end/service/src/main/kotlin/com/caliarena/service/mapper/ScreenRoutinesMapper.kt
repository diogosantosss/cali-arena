package com.caliarena.service.mapper

import com.caliarena.domain.routine.Exercise
import com.caliarena.domain.routine.RoutineOverview
import com.caliarena.repo.entities.routine.ExerciseEntity
import com.caliarena.repo.entities.routine.ScreenRoutineEntity
import com.caliarena.repo.trx.Transaction
import com.caliarena.service.sse.ScreenRoutinesEvent
import com.caliarena.service.sse.SpectatorAction
import jakarta.inject.Named
import org.springframework.data.repository.findByIdOrNull

@Named
class ScreenRoutinesMapper {
    fun build(
        trx: Transaction,
        tournamentId: Int,
    ): List<ScreenRoutinesEvent> =
        trx.screenRoutines
            .findByTournamentIdOrderByDisplayOrder(tournamentId)
            .mapNotNull { entity -> toEvent(trx, tournamentId, entity) }

    private fun toEvent(
        trx: Transaction,
        tournamentId: Int,
        entity: ScreenRoutineEntity,
    ): ScreenRoutinesEvent? {
        val screenRoutine = entity.toDomain()
        val routine =
            trx.routines.findByIdOrNull(screenRoutine.routineId)?.toDomain()
                ?: return null

        val exercises =
            trx.exercises
                .findExercisesByRoutineId(routine.id)
                .map(ExerciseEntity::toDomain)
                .sortedBy(Exercise::exerciseOrder)

        return ScreenRoutinesEvent(
            tournamentId = tournamentId,
            action = SpectatorAction.SCREEN_ROUTINES_CREATED,
            screenRoutine = screenRoutine,
            routineOverview =
                RoutineOverview(
                    routine.name,
                    routine.timeCapSeconds,
                    routine.createdAt,
                    exercises,
                ),
        )
    }
}
