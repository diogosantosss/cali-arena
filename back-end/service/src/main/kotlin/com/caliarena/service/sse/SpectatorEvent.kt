package com.caliarena.service.sse

import com.caliarena.domain.routine.RoutineOverview
import com.caliarena.domain.routine.ScreenRoutine
import com.caliarena.domain.tournament.TournamentState
import java.time.Instant

sealed interface SpectatorEvent {
    val tournamentId: Int
}

data class TournamentStateUpdatedEvent(
    override val tournamentId: Int,
    val state: TournamentState,
) : SpectatorEvent

data class ScreenRoutinesUpdatedEvent(
    override val tournamentId: Int,
    val screenRoutine: ScreenRoutine,
    val routineOverview: RoutineOverview,
) : SpectatorEvent

data class KeepAliveEvent(
    val timestamp: Instant,
)
