package com.caliarena.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caliarena.data.AthleteOutput
import com.caliarena.data.ErrorCode
import com.caliarena.data.MatchOutput
import com.caliarena.data.RoutineOverviewOutput
import com.caliarena.network.CaliApiException
import com.caliarena.repository.MatchRepository
import com.caliarena.repository.NotAuthenticatedException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MatchesUiState {
    data object Loading : MatchesUiState

    data class Ready(
        val matches: List<MatchCardItem>,
    ) : MatchesUiState

    data class Failed(
        val code: ErrorCode,
    ) : MatchesUiState
}

data class MatchCardItem(
    val match: MatchOutput,
    val athleteRed: AthleteOutput?,
    val athleteBlue: AthleteOutput?,
    val routine: RoutineOverviewOutput?,
)

class MatchesViewModel(
    private val repository: MatchRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<MatchesUiState>(MatchesUiState.Loading)
    val uiState: StateFlow<MatchesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = MatchesUiState.Loading
            val result = repository.getMatches()
            result.fold(
                onSuccess = { matches ->
                    _uiState.value = MatchesUiState.Ready(buildItems(matches))
                },
                onFailure = {
                    _uiState.value = MatchesUiState.Failed(it.toErrorCode())
                },
            )
        }
    }

    private suspend fun buildItems(matches: List<MatchOutput>): List<MatchCardItem> {
        val athleteIds =
            matches.fold(mutableSetOf<Int>()) { ids, match ->
                ids.apply {
                    match.athleteRedId?.let(ids::add)
                    match.athleteBlueId?.let(ids::add)
                }
            }
        val athletes = athleteIds.associateWith { repository.getAthlete(it).getOrNull() }

        val routineNamesById =
            repository
                .getRoutines()
                .getOrNull()
                ?.associate { it.id to it.name }
                .orEmpty()
        val overviewsByRoutineId =
            matches
                .map { it.routineId }
                .distinct()
                .associateWith { routineId ->
                    routineNamesById[routineId]?.let { repository.getRoutineOverview(it).getOrNull() }
                }

        return matches
            .sortedByDescending { it.createdAt }
            .map { match ->
                MatchCardItem(
                    match = match,
                    athleteRed = match.athleteRedId?.let { athletes[it] },
                    athleteBlue = match.athleteBlueId?.let { athletes[it] },
                    routine = overviewsByRoutineId[match.routineId],
                )
            }
    }

    private fun Throwable.toErrorCode(): ErrorCode =
        when (this) {
            is NotAuthenticatedException -> ErrorCode.SESSION_INVALID
            is CaliApiException -> code
            else -> ErrorCode.UNKNOWN_ERROR
        }
}
