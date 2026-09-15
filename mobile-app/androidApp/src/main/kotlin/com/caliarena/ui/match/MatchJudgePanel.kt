package com.caliarena.ui.match

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.caliarena.R
import com.caliarena.data.MatchStatus
import com.caliarena.data.RepSide
import com.caliarena.ui.theme.CaliArenaTheme
import com.caliarena.viewmodel.MatchDetailUiState
import kotlin.math.max

@Composable
internal fun JudgePanel(
    state: MatchDetailUiState.Ready,
    side: RepSide,
    onAdjust: (RepSide, Int) -> Unit,
    onFinish: (RepSide) -> Unit,
) {
    val athlete = if (side == RepSide.RED) state.athleteRed else state.athleteBlue
    val clubName = if (side == RepSide.RED) state.clubRedName else state.clubBlueName
    val currentGroupIndex = state.currentGroupIndex(side)
    val finished = state.isSideFinished(side)
    var showFinishConfirm by remember { mutableStateOf(false) }

    if (athlete == null) {
        UnassignedAthletesCard()
        return
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AthleteCard(
            athlete = athlete,
            side = side,
            clubName = clubName,
            accentColor = sideColor(side),
            finished = finished,
        )

        state.routine?.let { routine ->
            RoutineCard(
                routine = routine,
                currentGroupIndex = currentGroupIndex,
                accentColor = sideColor(side),
            )
        }

        TimerRow(state = state, side = side)

        AnimatedContent(
            targetState =
                when {
                    state.match.status == MatchStatus.PENDING -> JudgePhase.PENDING
                    finished -> JudgePhase.FINISHED
                    else -> JudgePhase.SCORING
                },
            modifier = Modifier.fillMaxWidth(),
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                ) togetherWith fadeOut(animationSpec = tween(durationMillis = 140))
            },
            label = "judgeBody",
        ) { phase ->
            when (phase) {
                JudgePhase.PENDING ->
                    PendingStartCard(modifier = Modifier.fillMaxWidth())

                JudgePhase.FINISHED ->
                    FinishedCard(
                        elapsedMs = state.finishedElapsedMillis(side),
                        modifier = Modifier.fillMaxWidth(),
                    )

                JudgePhase.SCORING ->
                    ScoringBody(
                        state = state,
                        side = side,
                        currentGroupIndex = currentGroupIndex,
                        onAdjust = onAdjust,
                        modifier = Modifier.fillMaxWidth(),
                    )
            }
        }

        if (state.match.status != MatchStatus.FINISHED) {
            Button(
                onClick = { showFinishConfirm = true },
                enabled = !finished && state.match.status == MatchStatus.RUNNING,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = sideColor(side),
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Text(stringResource(R.string.finish_side))
            }
        }
    }

    if (showFinishConfirm) {
        AlertDialog(
            onDismissRequest = { showFinishConfirm = false },
            title = { Text(stringResource(R.string.finish_confirm_title)) },
            text = { Text(stringResource(R.string.finish_confirm_message, athlete.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        showFinishConfirm = false
                        onFinish(side)
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = sideColor(side),
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                ) {
                    Text(stringResource(R.string.finish_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ScoringBody(
    state: MatchDetailUiState.Ready,
    side: RepSide,
    currentGroupIndex: Int,
    onAdjust: (RepSide, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups = groupedExercises(state.routine)
    val currentExerciseId = state.currentExerciseIdOf(side)
    val currentGroup = groups.getOrNull(currentGroupIndex)
    val currentExercise = currentGroup?.firstOrNull { it.id == currentExerciseId }
    val label =
        currentExercise?.let { exerciseLabel(it) }
            ?: currentGroup?.joinToString(" / ") { it.summaryLabel }
            ?: stringResource(R.string.exercise_unknown)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CurrentExerciseCard(
            label = label,
            reps = state.currentRepsOf(side),
            accentColor = sideColor(side),
            enabled = state.match.status == MatchStatus.RUNNING,
            onIncrement = { onAdjust(side, state.currentRepsOf(side) + 1) },
            onDecrement = { onAdjust(side, max(0, state.currentRepsOf(side) - 1)) },
            modifier = Modifier.fillMaxWidth(),
        )

        val nextGroup = groups.getOrNull(currentGroupIndex + 1)
        if (nextGroup != null) {
            NextExerciseCard(
                nextLabel = nextGroup.joinToString(" - ") { it.summaryLabel },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private enum class JudgePhase { PENDING, SCORING, FINISHED }

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun JudgePanelScoringPreview() {
    CaliArenaTheme {
        JudgePanel(
            state = matchReadyState,
            side = RepSide.RED,
            onAdjust = { _, _ -> },
            onFinish = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun JudgePanelPendingPreview() {
    CaliArenaTheme {
        JudgePanel(
            state = matchPendingState,
            side = RepSide.RED,
            onAdjust = { _, _ -> },
            onFinish = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun JudgePanelFinishedPreview() {
    CaliArenaTheme {
        JudgePanel(
            state = matchFinishedSideState,
            side = RepSide.RED,
            onAdjust = { _, _ -> },
            onFinish = {},
        )
    }
}
