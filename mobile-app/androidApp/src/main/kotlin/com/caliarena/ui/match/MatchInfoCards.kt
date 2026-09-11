package com.caliarena.ui.match

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.caliarena.R
import com.caliarena.data.AthleteOutput
import com.caliarena.data.ExerciseOutput
import com.caliarena.data.MatchStatus
import com.caliarena.data.RepSide
import com.caliarena.data.RoutineOverviewOutput
import com.caliarena.ui.theme.CaliArenaTheme
import com.caliarena.ui.theme.CaliFinished
import com.caliarena.ui.theme.CaliGold
import com.caliarena.ui.theme.CaliMuted
import com.caliarena.viewmodel.MatchDetailUiState

@Composable
internal fun AthleteCard(
    athlete: AthleteOutput,
    side: RepSide,
    clubName: String?,
    accentColor: Color,
    finished: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = accentColor.copy(alpha = 0.16f),
                    contentColor = accentColor,
                ) {
                    Text(
                        text = sideLabel(side),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(Modifier.weight(1f))

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

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                AthleteAvatar(name = athlete.name, accentColor = accentColor, size = 48.dp)

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(
                        text = athlete.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Text(
                        text = clubName ?: stringResource(R.string.match_side_club_missing),
                        style = MaterialTheme.typography.bodySmall,
                        color = CaliMuted,
                    )
                }
            }
        }
    }
}

@Composable
internal fun RoutineCard(
    routine: RoutineOverviewOutput,
    currentGroupIndex: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val groups = groupedExercises(routine)
    if (groups.isEmpty()) return

    var showRoutine by remember { mutableStateOf(false) }

    val currentLabel =
        groups
            .getOrNull(currentGroupIndex)
            ?.joinToString(" / ") { it.summaryLabel }

    Card(
        onClick = { showRoutine = true },
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.routine_label),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = CaliMuted,
                )

                Spacer(Modifier.weight(1f))

                Text(
                    text = routine.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                groups.forEachIndexed { index, _ ->
                    val segmentColor =
                        when {
                            index < currentGroupIndex -> accentColor.copy(alpha = 0.4f)
                            index == currentGroupIndex -> accentColor
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }

                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(segmentColor),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            currentLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    if (showRoutine) {
        Dialog(onDismissRequest = { showRoutine = false }) {
            RoutineDetailCard(routine = routine, onClose = { showRoutine = false })
        }
    }
}

@Composable
private fun RoutineDetailCard(
    routine: RoutineOverviewOutput,
    onClose: () -> Unit,
) {
    val groups = groupedExercises(routine)

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp),
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.routine_label),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = CaliMuted,
                    )

                    Text(
                        text = routine.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = CaliMuted,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            routine.timeCapSeconds?.let { timeCap ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = CaliGold.copy(alpha = 0.14f),
                    contentColor = CaliGold,
                ) {
                    Text(
                        text = "${formatSeconds(timeCap)} ${stringResource(R.string.time_cap_label)}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            groups.forEachIndexed { index, group ->
                RoutineDetailRow(index = index, group = group)

                if (index != groups.lastIndex) {
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun RoutineDetailRow(
    index: Int,
    group: List<ExerciseOutput>,
) {
    val names = group.joinToString("  +  ") { it.summaryLabel }

    Row(Modifier.fillMaxWidth()) {
        Text(
            text = (index + 1).toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CaliMuted,
            modifier = Modifier.width(28.dp),
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = names,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
internal fun TimerRow(
    state: MatchDetailUiState.Ready,
    side: RepSide,
    modifier: Modifier = Modifier,
) {
    var elapsedMs by
        remember(state.match.startedAt, state.progress.timerStartedAt, state.finishedAtOf(side)) {
            mutableLongStateOf(0L)
        }

    LaunchedEffect(state.match.startedAt, state.progress.timerStartedAt, state.finishedAtOf(side)) {
        val finishedAt = state.finishedAtOf(side)
        when {
            finishedAt != null ->
                elapsedMs = state.finishedElapsedMillis(side) ?: state.elapsedMatchMillis()

            state.match.status == MatchStatus.FINISHED -> elapsedMs = state.elapsedMatchMillis()

            else ->
                while (true) {
                    elapsedMs = state.elapsedMatchMillis()
                    withFrameNanos {}
                }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.match_time_label),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = CaliMuted,
                )

                Text(
                    text = formatElapsed(elapsedMs),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            val timeCapSeconds = state.routine?.timeCapSeconds
            if (timeCapSeconds != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.time_cap_label),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = CaliMuted,
                    )

                    Text(
                        text = formatSeconds(timeCapSeconds),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = CaliGold,
                    )
                }
            }
        }
    }
}

@Composable
internal fun AthleteAvatar(
    name: String?,
    accentColor: Color,
    size: Dp = 34.dp,
    modifier: Modifier = Modifier,
) {
    val assigned = name != null
    val initial = name?.take(1)?.uppercase()

    Box(
        modifier =
            modifier
                .size(size)
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

@Preview(showBackground = true)
@Composable
private fun AthleteCardPreview() {
    CaliArenaTheme {
        AthleteCard(
            athlete = previewAthleteRed,
            side = RepSide.RED,
            clubName = "Team Norte",
            accentColor = sideColor(RepSide.RED),
            finished = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RoutineCardPreview() {
    CaliArenaTheme {
        RoutineCard(
            routine = previewRoutine,
            currentGroupIndex = 0,
            accentColor = sideColor(RepSide.RED),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimerRowPreview() {
    CaliArenaTheme {
        TimerRow(state = matchReadyState, side = RepSide.RED)
    }
}

@Preview(showBackground = true)
@Composable
private fun AthleteAvatarAssignedPreview() {
    CaliArenaTheme {
        AthleteAvatar(name = "João", accentColor = sideColor(RepSide.RED), size = 52.dp)
    }
}

@Preview(showBackground = true)
@Composable
private fun AthleteAvatarEmptyPreview() {
    CaliArenaTheme {
        AthleteAvatar(name = null, accentColor = sideColor(RepSide.RED), size = 52.dp)
    }
}
