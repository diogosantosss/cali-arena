package com.caliarena.ui.match

import androidx.compose.ui.graphics.Color
import com.caliarena.data.AthleteGender
import com.caliarena.data.AthleteOutput
import com.caliarena.data.ExerciseOutput
import com.caliarena.data.ExerciseType
import com.caliarena.data.MatchOutput
import com.caliarena.data.MatchProgressOutput
import com.caliarena.data.MatchStatus
import com.caliarena.data.RepSide
import com.caliarena.data.RoutineOverviewOutput
import com.caliarena.ui.theme.CaliAthleteBlue
import com.caliarena.ui.theme.CaliAthleteRed
import com.caliarena.viewmodel.MatchDetailUiState
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

internal fun sideColor(side: RepSide): Color =
    when (side) {
        RepSide.RED -> CaliAthleteRed
        RepSide.BLUE -> CaliAthleteBlue
    }

internal fun sideLabel(side: RepSide): String =
    when (side) {
        RepSide.RED -> "RED"
        RepSide.BLUE -> "BLUE"
    }

internal fun groupedExercises(routine: RoutineOverviewOutput?): List<List<ExerciseOutput>> =
    routine
        ?.exercises
        ?.groupBy { it.exerciseOrder }
        ?.map { it.value.sortedBy { exercise -> exercise.supersetOrder ?: 0 } }
        ?.sortedBy { it.first().exerciseOrder }
        .orEmpty()

internal fun exerciseLabel(exercise: ExerciseOutput): String = exercise.summaryLabel

internal fun MatchDetailUiState.Ready.currentRepsOf(side: RepSide): Int =
    if (side == RepSide.RED) progress.redCurrentReps else progress.blueCurrentReps

internal fun MatchDetailUiState.Ready.currentExerciseIdOf(side: RepSide): Int? =
    if (side == RepSide.RED) progress.redCurrentExerciseId else progress.blueCurrentExerciseId

internal fun MatchDetailUiState.Ready.currentGroupIndex(side: RepSide): Int {
    val id = currentExerciseIdOf(side) ?: return 0
    val groups = groupedExercises(routine)
    return groups.indexOfFirst { group -> group.any { it.id == id } }.coerceAtLeast(0)
}

internal fun MatchDetailUiState.Ready.isSideFinished(side: RepSide): Boolean =
    if (side == RepSide.RED) progress.redFinishedAt != null else progress.blueFinishedAt != null

internal fun MatchDetailUiState.Ready.finishedAtOf(side: RepSide): String? =
    if (side == RepSide.RED) progress.redFinishedAt else progress.blueFinishedAt

internal fun MatchDetailUiState.Ready.elapsedMatchMillis(): Long {
    val started =
        runCatching { Instant.parse(match.startedAt ?: progress.timerStartedAt ?: return 0L) }
            .getOrNull()
            ?: return 0L
    val nowMs = Clock.System.now().toEpochMilliseconds()
    val startedMs = started.toEpochMilliseconds()
    return (nowMs - startedMs).coerceAtLeast(0L)
}

internal fun MatchDetailUiState.Ready.finishedElapsedMillis(side: RepSide): Long? {
    val finishIso = finishedAtOf(side) ?: return null
    val startIso = match.startedAt ?: progress.timerStartedAt ?: return null
    val startMs = runCatching { Instant.parse(startIso).toEpochMilliseconds() }.getOrNull() ?: return null
    val finishMs = runCatching { Instant.parse(finishIso).toEpochMilliseconds() }.getOrNull() ?: return null
    return (finishMs - startMs).coerceAtLeast(0L)
}

internal fun formatElapsed(elapsedMs: Long): String {
    val totalSeconds = elapsedMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = elapsedMs % 1000
    return "%02d:%02d.%03d".format(minutes, seconds, millis)
}

internal fun formatSeconds(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

internal val previewAthleteRed: AthleteOutput =
    AthleteOutput(1, "João", AthleteGender.MALE, 1, "2026-01-01T00:00:00Z")

internal val previewAthleteBlue: AthleteOutput =
    AthleteOutput(2, "Maria", AthleteGender.FEMALE, 1, "2026-01-01T00:00:00Z")

internal val previewRoutine: RoutineOverviewOutput =
    RoutineOverviewOutput(
        name = "Quarterfinals (MEN) ELITE",
        timeCapSeconds = 720,
        createdAt = "2026-01-01T00:00:00Z",
        exercises =
            listOf(
                ExerciseOutput(
                    id = 1,
                    routineId = 3,
                    name = "Muscle-Ups",
                    targetReps = 1,
                    addedWeight = null,
                    exerciseOrder = 1,
                    supersetOrder = 0,
                    type = ExerciseType.SUPERSET,
                ),
                ExerciseOutput(
                    id = 2,
                    routineId = 3,
                    name = "Pull-Ups",
                    targetReps = 10,
                    addedWeight = null,
                    exerciseOrder = 1,
                    supersetOrder = 1,
                    type = ExerciseType.SUPERSET,
                ),
                ExerciseOutput(
                    id = 3,
                    routineId = 3,
                    name = "Low-Bar Push-Ups",
                    targetReps = 20,
                    addedWeight = null,
                    exerciseOrder = 2,
                    supersetOrder = null,
                    type = ExerciseType.NORMAL,
                ),
                ExerciseOutput(
                    id = 4,
                    routineId = 3,
                    name = "Squats",
                    targetReps = 20,
                    addedWeight = 20.0,
                    exerciseOrder = 3,
                    supersetOrder = null,
                    type = ExerciseType.NORMAL,
                ),
            ),
    )

internal fun previewMatchState(
    status: MatchStatus,
    startedAt: String? = "2026-08-09T10:00:00Z",
    redCurrentExerciseId: Int = 1,
    blueCurrentExerciseId: Int = 2,
    redCurrentReps: Int = 23,
    blueCurrentReps: Int = 18,
    redFinishedAt: String? = null,
): MatchDetailUiState.Ready =
    MatchDetailUiState.Ready(
        match =
            MatchOutput(
                id = 12,
                bracketId = 1,
                routineId = 3,
                athleteRedId = 1,
                athleteBlueId = 2,
                winnerAthleteId = null,
                status = status,
                startedAt = startedAt,
                finishedAt = null,
                createdAt = "2026-08-09T09:00:00Z",
            ),
        progress =
            MatchProgressOutput(
                id = 1,
                matchId = 12,
                redCurrentExerciseId = redCurrentExerciseId,
                blueCurrentExerciseId = blueCurrentExerciseId,
                redCurrentReps = redCurrentReps,
                blueCurrentReps = blueCurrentReps,
                updatedAt = "2026-08-09T10:05:00Z",
                redFinishedAt = redFinishedAt,
            ),
        athleteRed = previewAthleteRed,
        athleteBlue = previewAthleteBlue,
        clubRedName = "Team Norte",
        clubBlueName = "Iron Coast",
        bracketDivision = "MEN ELITE • FINALS",
        routine = previewRoutine,
    )

internal val matchReadyState: MatchDetailUiState.Ready =
    previewMatchState(status = MatchStatus.RUNNING)

internal val matchPendingState: MatchDetailUiState.Ready =
    previewMatchState(status = MatchStatus.PENDING, startedAt = null)

internal val matchFinishedSideState: MatchDetailUiState.Ready =
    previewMatchState(
        status = MatchStatus.RUNNING,
        redFinishedAt = "2026-08-09T10:05:07.500Z",
    )
