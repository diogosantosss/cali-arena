package com.caliarena.ui.match

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.caliarena.R
import com.caliarena.data.AthleteGender
import com.caliarena.data.AthleteOutput
import com.caliarena.data.ErrorCode
import com.caliarena.data.MatchOutput
import com.caliarena.data.MatchProgressOutput
import com.caliarena.data.MatchStatus
import com.caliarena.data.RepSide
import com.caliarena.ui.theme.CaliArenaTheme
import com.caliarena.ui.theme.CaliAthleteBlue
import com.caliarena.ui.theme.CaliAthleteRed
import com.caliarena.ui.theme.CaliFinished
import com.caliarena.ui.theme.CaliGold
import com.caliarena.ui.theme.CaliMuted
import com.caliarena.util.ErrorDescriptions
import com.caliarena.viewmodel.MatchDetailUiState

@Composable
fun MatchScreen(
    matchId: Int,
    uiState: MatchDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onReconnect: () -> Unit,
    onAdjust: (RepSide, Int) -> Unit,
    onFinish: (RepSide) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MatchTopBar(
                matchId = matchId,
                connectionLost = (uiState as? MatchDetailUiState.Ready)?.connectionLost == true,
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
        ) {
            when (uiState) {
                MatchDetailUiState.Connecting -> ConnectingView()

                is MatchDetailUiState.Failed -> FailedView(uiState.code, onRetry = onRetry)

                is MatchDetailUiState.Ready ->
                    ReadyContent(
                        state = uiState,
                        onReconnect = onReconnect,
                        onAdjust = onAdjust,
                        onFinish = onFinish,
                    )
            }
        }
    }
}

@Composable
private fun MatchTopBar(
    matchId: Int,
    connectionLost: Boolean,
    onBack: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(Modifier.width(4.dp))

            Text(
                text = stringResource(R.string.match_number, matchId),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )

            if (connectionLost) {
                Text(
                    text = stringResource(R.string.connection_lost),
                    style = MaterialTheme.typography.labelMedium,
                    color = CaliMuted,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun ConnectingView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.match_connecting),
            style = MaterialTheme.typography.bodyMedium,
            color = CaliMuted,
        )
    }
}

@Composable
private fun FailedView(
    errorCode: ErrorCode,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.error),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(ErrorDescriptions.getErrorDescription(errorCode)),
            style = MaterialTheme.typography.bodyMedium,
            color = CaliMuted,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(20.dp))

        Button(onClick = onRetry) {
            Text(stringResource(R.string.try_again))
        }
    }
}

@Composable
private fun ReadyContent(
    state: MatchDetailUiState.Ready,
    onReconnect: () -> Unit,
    onAdjust: (RepSide, Int) -> Unit,
    onFinish: (RepSide) -> Unit,
) {
    val unknownExercise = stringResource(R.string.exercise_unknown)
    val redLabel = state.exerciseLabels[state.progress.redCurrentExerciseId] ?: unknownExercise
    val blueLabel = state.exerciseLabels[state.progress.blueCurrentExerciseId] ?: unknownExercise

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        if (state.connectionLost) {
            ConnectionLostBanner(onReconnect = onReconnect)
        }

        state.lastActionError?.let { code ->
            ActionErrorBanner(code = code)
        }

        Spacer(Modifier.height(12.dp))

        ScoreSideCard(
            athlete = state.athleteRed,
            exerciseLabel = redLabel,
            reps = state.progress.redCurrentReps,
            finished = state.progress.redFinishedAt != null,
            accentColor = CaliAthleteRed,
            onAdjust = { onAdjust(RepSide.RED, it) },
            onFinish = { onFinish(RepSide.RED) },
        )

        Spacer(Modifier.height(12.dp))

        ScoreSideCard(
            athlete = state.athleteBlue,
            exerciseLabel = blueLabel,
            reps = state.progress.blueCurrentReps,
            finished = state.progress.blueFinishedAt != null,
            accentColor = CaliAthleteBlue,
            onAdjust = { onAdjust(RepSide.BLUE, it) },
            onFinish = { onFinish(RepSide.BLUE) },
        )
    }
}

@Composable
private fun ConnectionLostBanner(
    onReconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CaliGold.copy(alpha = 0.14f),
        contentColor = CaliGold,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.connection_lost),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = stringResource(ErrorDescriptions.getErrorDescription(ErrorCode.NO_CONNECTION)),
                    style = MaterialTheme.typography.bodySmall,
                    color = CaliMuted,
                )
            }

            Spacer(Modifier.width(12.dp))

            Button(
                onClick = onReconnect,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = CaliGold,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Text(stringResource(R.string.reconnect))
            }
        }
    }
}

@Composable
private fun ActionErrorBanner(
    code: ErrorCode,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Text(
            text = stringResource(ErrorDescriptions.getErrorDescription(code)),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ScoreSideCard(
    athlete: AthleteOutput?,
    exerciseLabel: String,
    reps: Int,
    finished: Boolean,
    accentColor: Color,
    onAdjust: (Int) -> Unit,
    onFinish: () -> Unit,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AthleteAvatar(name = athlete?.name, accentColor = accentColor)

                Spacer(Modifier.width(10.dp))

                Text(
                    text = athlete?.name ?: stringResource(R.string.athlete_unassigned),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (athlete != null) MaterialTheme.colorScheme.onSurface else CaliMuted,
                    modifier = Modifier.weight(1f),
                )

                if (finished) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = CaliFinished.copy(alpha = 0.14f),
                        contentColor = CaliFinished,
                    ) {
                        Text(
                            text = stringResource(R.string.match_finished),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = exerciseLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = CaliMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RepButton(
                    content = {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.reps_increase),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    enabled = !finished,
                    onClick = { onAdjust(reps + 1) },
                )

                Spacer(Modifier.weight(1f))

                Text(
                    text = reps.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.weight(1f))

                RepButton(
                    content = {
                        Text(
                            text = "−",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    enabled = !finished,
                    onClick = { onAdjust(reps - 1) },
                )
            }

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = onFinish,
                enabled = !finished,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Text(stringResource(R.string.finish_side))
            }
        }
    }
}

@Composable
private fun RepButton(
    content: @Composable () -> Unit,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled) {
        content()
    }
}

@Composable
private fun AthleteAvatar(
    name: String?,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val assigned = name != null
    val initial = name?.take(1)?.uppercase()

    Box(
        modifier =
            modifier
                .size(34.dp)
                .background(accentColor.copy(alpha = if (assigned) 0.18f else 0f), CircleShape)
                .border(
                    width = 2.dp,
                    color = if (assigned) accentColor else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial ?: "—",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (assigned) accentColor else CaliMuted,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MatchScreenPreview() {
    CaliArenaTheme {
        MatchScreen(
            matchId = 12,
            uiState =
                MatchDetailUiState.Ready(
                    match =
                        MatchOutput(
                            id = 12,
                            bracketId = 1,
                            routineId = 3,
                            judgeId = 2,
                            athleteRedId = 1,
                            athleteBlueId = 2,
                            winnerAthleteId = null,
                            status = MatchStatus.RUNNING,
                            startedAt = "2026-08-09T10:00:00Z",
                            finishedAt = null,
                            createdAt = "2026-08-09T09:00:00Z",
                        ),
                    progress =
                        MatchProgressOutput(
                            id = 1,
                            matchId = 12,
                            redCurrentExerciseId = 1,
                            blueCurrentExerciseId = 2,
                            redCurrentReps = 23,
                            blueCurrentReps = 18,
                            updatedAt = "2026-08-09T10:05:00Z",
                        ),
                    athleteRed = AthleteOutput(1, "João", AthleteGender.MALE, 1, "2026-01-01T00:00:00Z"),
                    athleteBlue = AthleteOutput(2, "Maria", AthleteGender.FEMALE, 1, "2026-01-01T00:00:00Z"),
                    exerciseLabels = mapOf(1 to "Muscle-Ups", 2 to "Pull-Ups"),
                ),
            onBack = {},
            onRetry = {},
            onReconnect = {},
            onAdjust = { _, _ -> },
            onFinish = {},
        )
    }
}
