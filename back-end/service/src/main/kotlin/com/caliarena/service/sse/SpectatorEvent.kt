package com.caliarena.service.sse

import com.caliarena.domain.bracket.BracketLeaderboard
import com.caliarena.domain.bracket.TournamentBracketsResponse
import com.caliarena.domain.match.MatchProgress
import com.caliarena.domain.routine.RoutineOverview
import com.caliarena.domain.routine.ScreenRoutine
import com.caliarena.domain.tournament.TournamentState
import java.time.Instant

enum class SpectatorAction {
    TOURNAMENT_STATE_UPDATED,
    SCREEN_ROUTINES_CREATED,
    SCREEN_ROUTINES_UPDATED,
    SCREEN_ROUTINES_DELETED,
    MATCH_UPDATED,
}

sealed interface SpectatorEvent {
    val tournamentId: Int
    val action: SpectatorAction
}

data class TournamentStateUpdatedEvent(
    override val tournamentId: Int,
    override val action: SpectatorAction = SpectatorAction.TOURNAMENT_STATE_UPDATED,
    val state: TournamentState,
    val currentMatchId: Int? = null,
    val leaderboard: BracketLeaderboard? = null,
    val bracketSummary: TournamentBracketsResponse? = null,
) : SpectatorEvent

data class ScreenRoutinesEvent(
    override val tournamentId: Int,
    override val action: SpectatorAction = SpectatorAction.SCREEN_ROUTINES_UPDATED,
    val screenRoutine: ScreenRoutine,
    val routineOverview: RoutineOverview,
) : SpectatorEvent

data class ScreenRoutineDeletedEvent(
    override val tournamentId: Int,
    override val action: SpectatorAction = SpectatorAction.SCREEN_ROUTINES_DELETED,
    val screenRoutineId: Int,
) : SpectatorEvent

data class MatchUpdatedEvent(
    override val tournamentId: Int,
    override val action: SpectatorAction = SpectatorAction.MATCH_UPDATED,
    val matchProgress: MatchProgress,
) : SpectatorEvent

data class KeepAliveEvent(
    val timestamp: Instant,
)
