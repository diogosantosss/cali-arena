package com.caliarena.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caliarena.data.AthleteOutput
import com.caliarena.data.ErrorCode
import com.caliarena.data.JudgeErrorEvent
import com.caliarena.data.JudgeFinishedEvent
import com.caliarena.data.JudgeRepsEvent
import com.caliarena.data.JudgeStartedEvent
import com.caliarena.data.JudgeWsEvent
import com.caliarena.data.MatchConnectionLost
import com.caliarena.data.MatchOutput
import com.caliarena.data.MatchProgressOutput
import com.caliarena.data.MatchStatus
import com.caliarena.data.RepSide
import com.caliarena.data.RoutineOverviewOutput
import com.caliarena.data.toErrorCode
import com.caliarena.network.CaliApiException
import com.caliarena.network.MatchWsClient
import com.caliarena.network.MatchWsSession
import com.caliarena.repository.MatchRepository
import com.caliarena.repository.NotAuthenticatedException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface MatchDetailUiState {
    data object Connecting : MatchDetailUiState

    data class Ready(
        val match: MatchOutput,
        val progress: MatchProgressOutput,
        val athleteRed: AthleteOutput?,
        val athleteBlue: AthleteOutput?,
        val clubRedName: String?,
        val clubBlueName: String?,
        val bracketDivision: String?,
        val routine: RoutineOverviewOutput?,
        val connectionLost: Boolean = false,
        val lastActionError: ErrorCode? = null,
    ) : MatchDetailUiState

    data class Failed(
        val code: ErrorCode,
    ) : MatchDetailUiState
}

class MatchViewModel(
    private val matchId: Int,
    private val repository: MatchRepository,
    private val wsClient: MatchWsClient,
) : ViewModel() {
    private val _uiState = MutableStateFlow<MatchDetailUiState>(MatchDetailUiState.Connecting)
    val uiState: StateFlow<MatchDetailUiState> = _uiState.asStateFlow()

    private val _selectedSide = MutableStateFlow<RepSide?>(null)
    val selectedSide: StateFlow<RepSide?> = _selectedSide.asStateFlow()

    private var session: MatchWsSession? = null
    private var eventsJob: Job? = null

    init {
        load()
    }

    fun selectSide(side: RepSide) {
        _selectedSide.value = side
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = MatchDetailUiState.Connecting
            val match =
                repository.getMatch(matchId).getOrElse { error ->
                    fail(error.toErrorCode())
                    return@launch
                }
            val progress =
                repository.getMatchProgress(matchId).getOrElse { error ->
                    if (match.status == MatchStatus.PENDING) {
                        MatchProgressOutput(
                            id = 0,
                            matchId = match.id,
                            redCurrentReps = 0,
                            blueCurrentReps = 0,
                            updatedAt = match.createdAt,
                        )
                    } else {
                        fail(error.toErrorCode())
                        return@launch
                    }
                }
            val bracketDivision =
                repository
                    .getBracketLeaderboard(match.bracketId)
                    .getOrNull()
                    ?.division
            val clubRed =
                match.athleteRedId
                    ?.let { repository.getAthlete(it).getOrNull()?.clubId }
                    ?.let { repository.getClub(it).getOrNull() }
            val clubBlue =
                match.athleteBlueId
                    ?.let { repository.getAthlete(it).getOrNull()?.clubId }
                    ?.let { repository.getClub(it).getOrNull() }
            val routine = routineOverview(match)
            val red = match.athleteRedId?.let { repository.getAthlete(it).getOrNull() }
            val blue = match.athleteBlueId?.let { repository.getAthlete(it).getOrNull() }
            _uiState.value =
                MatchDetailUiState.Ready(
                    match = match,
                    progress = progress,
                    athleteRed = red,
                    athleteBlue = blue,
                    clubRedName = clubRed?.name,
                    clubBlueName = clubBlue?.name,
                    bracketDivision = bracketDivision,
                    routine = routine,
                )
            openSession()
        }
    }

    fun reconnect() {
        viewModelScope.launch {
            _uiState.update { state ->
                if (state is MatchDetailUiState.Ready) {
                    state.copy(connectionLost = false)
                } else {
                    state
                }
            }
            openSession()
        }
    }

    fun adjust(
        side: RepSide,
        reps: Int,
    ) {
        launchWsAction {
            it.adjustSide(side, reps)
        }
    }

    fun finish(side: RepSide) {
        launchWsAction {
            it.finishSide(side)
        }
    }

    private suspend fun routineOverview(match: MatchOutput): RoutineOverviewOutput? {
        val name =
            repository
                .getRoutines()
                .getOrNull()
                ?.firstOrNull { it.id == match.routineId }
                ?.name
                ?: return null
        return repository.getRoutineOverview(name).getOrNull()
    }

    private suspend fun openSession() {
        eventsJob?.cancel()
        val previous = session
        session = null
        previous?.close()

        val newSession =
            wsClient.open(matchId).getOrElse { error ->
                _uiState.update { state ->
                    if (state is MatchDetailUiState.Ready) {
                        state.copy(connectionLost = true, lastActionError = error.toErrorCode())
                    } else {
                        state
                    }
                }
                return
            }
        session = newSession
        eventsJob =
            viewModelScope.launch {
                newSession
                    .events
                    .collect(::onEvent)
            }
    }

    private fun onEvent(event: JudgeWsEvent) {
        _uiState.update { state ->
            when {
                state !is MatchDetailUiState.Ready -> state

                event is JudgeStartedEvent ->
                    state.copy(match = event.match, progress = event.progress)

                event is JudgeRepsEvent ->
                    state.copy(progress = state.progress.applyReps(event.side, event.reps, event.exerciseId))

                event is JudgeFinishedEvent ->
                    state.copy(
                        progress =
                            if (event.side == RepSide.RED) {
                                state.progress.copy(redFinishedAt = state.progress.redFinishedAt ?: event.finishedAt)
                            } else {
                                state.progress.copy(blueFinishedAt = state.progress.blueFinishedAt ?: event.finishedAt)
                            },
                    )

                event is JudgeErrorEvent -> state.copy(lastActionError = event.toErrorCode())

                event == MatchConnectionLost -> state.copy(connectionLost = true)

                else -> state
            }
        }
    }

    private fun launchWsAction(block: suspend (MatchWsSession) -> Result<Unit>) {
        val active = session ?: return
        viewModelScope.launch {
            block(active).fold(
                onSuccess = {
                    _uiState.update { state ->
                        if (state is MatchDetailUiState.Ready) {
                            state.copy(lastActionError = null, connectionLost = false)
                        } else {
                            state
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        if (state is MatchDetailUiState.Ready) {
                            state.copy(lastActionError = error.toErrorCode())
                        } else {
                            state
                        }
                    }
                },
            )
        }
    }

    private fun MatchProgressOutput.applyReps(
        side: RepSide,
        reps: Int,
        exerciseId: Int?,
    ): MatchProgressOutput =
        when (side) {
            RepSide.RED -> copy(redCurrentReps = reps, redCurrentExerciseId = exerciseId ?: redCurrentExerciseId)
            RepSide.BLUE -> copy(blueCurrentReps = reps, blueCurrentExerciseId = exerciseId ?: blueCurrentExerciseId)
        }

    private fun fail(code: ErrorCode) {
        _uiState.value = MatchDetailUiState.Failed(code)
    }

    private fun Throwable.toErrorCode(): ErrorCode =
        when (this) {
            is NotAuthenticatedException -> ErrorCode.SESSION_INVALID
            is CaliApiException -> code
            else -> ErrorCode.UNKNOWN_ERROR
        }
}
