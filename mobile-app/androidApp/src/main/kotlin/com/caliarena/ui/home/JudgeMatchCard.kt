package com.caliarena.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caliarena.R
import com.caliarena.data.AthleteGender
import com.caliarena.data.AthleteOutput
import com.caliarena.data.ExerciseOutput
import com.caliarena.data.ExerciseType
import com.caliarena.data.MatchOutput
import com.caliarena.data.MatchProgressOutput
import com.caliarena.data.MatchStatus
import com.caliarena.data.RoutineOverviewOutput
import com.caliarena.ui.theme.CaliArenaTheme
import com.caliarena.ui.theme.CaliAthleteBlue
import com.caliarena.ui.theme.CaliAthleteRed
import com.caliarena.ui.theme.CaliFinished
import com.caliarena.ui.theme.CaliGold
import com.caliarena.ui.theme.CaliMuted
import com.caliarena.viewmodel.MatchCardItem
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun JudgeMatchCard(
    item: MatchCardItem,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var routineExpanded by rememberSaveable { mutableStateOf(false) }
    val routine = item.routine
    val enterAction = if (item.match.status != MatchStatus.FINISHED) onClick else null

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (enterAction != null) {
                        Modifier.clickable(onClick = enterAction)
                    } else {
                        Modifier
                    },
                ),
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(statusColor(item.match.status)),
            )

            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.match_number, item.match.id).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            color = CaliMuted,
                        )

                        Spacer(Modifier.height(6.dp))

                        when (routine) {
                            null ->
                                Text(
                                    text = stringResource(R.string.match_routine_unavailable),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CaliMuted,
                                )

                            else ->
                                RoutineHeader(
                                    routine = routine,
                                    expanded = routineExpanded,
                                    onToggle = { routineExpanded = !routineExpanded },
                                )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MatchStatusBadge(item.match.status)

                        if (enterAction != null) {
                            Spacer(Modifier.width(2.dp))

                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.match_open),
                                tint = CaliMuted,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                if (routine != null) {
                    AnimatedVisibility(visible = routineExpanded) {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(12.dp))
                            RoutineExercisesList(routine)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                if (item.match.status == MatchStatus.FINISHED) {
                    MatchResultRow(
                        redName = item.athleteRed?.name,
                        blueName = item.athleteBlue?.name,
                        redIsWinner = item.match.athleteRedId == item.match.winnerAthleteId,
                        blueIsWinner = item.match.athleteBlueId == item.match.winnerAthleteId,
                        redElapsedMs = finishElapsedMillis(item.match.startedAt, item.progress?.redFinishedAt),
                        blueElapsedMs = finishElapsedMillis(item.match.startedAt, item.progress?.blueFinishedAt),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AthleteSide(
                            name = item.athleteRed?.name,
                            accentColor = CaliAthleteRed,
                            modifier = Modifier.weight(1f),
                        )

                        Text(
                            text = stringResource(R.string.vs),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = CaliMuted,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )

                        AthleteSide(
                            name = item.athleteBlue?.name,
                            accentColor = CaliAthleteBlue,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = CaliMuted,
                        modifier = Modifier.size(14.dp),
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        text = stringResource(R.string.match_created_on, formatCreatedDate(item.match.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = CaliMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutineHeader(
    routine: RoutineOverviewOutput,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val rotation by
        animateFloatAsState(
            targetValue = if (expanded) 180f else 0f,
            animationSpec = tween(durationMillis = 220),
            label = "routineChevronRotation",
        )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = routine.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = stringResource(R.string.exercises_count, routine.exercises.size),
                style = MaterialTheme.typography.bodySmall,
                color = CaliMuted,
            )
        }

        Spacer(Modifier.width(8.dp))

        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(if (expanded) R.string.routine_hide else R.string.routine_view),
                tint = CaliGold,
                modifier = Modifier.graphicsLayer { rotationZ = rotation },
            )
        }
    }
}

@Composable
private fun RoutineExercisesList(
    routine: RoutineOverviewOutput,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        routine.exerciseGroupsSummary.forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun AthleteSide(
    name: String?,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        AthleteAvatar(name = name, accentColor = accentColor)

        Spacer(Modifier.width(8.dp))

        Text(
            text = name ?: stringResource(R.string.athlete_unassigned),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (name != null) MaterialTheme.colorScheme.onSurface else CaliMuted,
        )
    }
}

@Composable
private fun MatchResultRow(
    redName: String?,
    blueName: String?,
    redIsWinner: Boolean,
    blueIsWinner: Boolean,
    redElapsedMs: Long?,
    blueElapsedMs: Long?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ResultSide(
            name = redName,
            accentColor = CaliAthleteRed,
            elapsedMs = redElapsedMs,
            isWinner = redIsWinner,
            modifier = Modifier.weight(1f),
        )

        ResultSide(
            name = blueName,
            accentColor = CaliAthleteBlue,
            elapsedMs = blueElapsedMs,
            isWinner = blueIsWinner,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ResultSide(
    name: String?,
    accentColor: Color,
    elapsedMs: Long?,
    isWinner: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AthleteAvatar(name = name, accentColor = accentColor)

            Spacer(Modifier.width(8.dp))

            Column {
                Text(
                    text = name ?: stringResource(R.string.athlete_unassigned),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (name != null) MaterialTheme.colorScheme.onSurface else CaliMuted,
                )

                Text(
                    text = elapsedMs?.let { formatElapsedMs(it) } ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isWinner) CaliGold else CaliMuted,
                )
            }
        }

        if (isWinner) {
            Spacer(Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = CaliGold.copy(alpha = 0.16f),
                contentColor = CaliGold,
                modifier = Modifier.align(Alignment.Start),
            ) {
                Text(
                    text = stringResource(R.string.match_result_winner),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private fun finishElapsedMillis(
    startedAt: String?,
    finishedAt: String?,
): Long? {
    if (startedAt == null || finishedAt == null) return null
    val start = runCatching { Instant.parse(startedAt).toEpochMilliseconds() }.getOrNull() ?: return null
    val end = runCatching { Instant.parse(finishedAt).toEpochMilliseconds() }.getOrNull() ?: return null
    return (end - start).coerceAtLeast(0L)
}

private fun formatElapsedMs(ms: Long): String = "%02d:%02d.%03d".format(ms / 60000, (ms / 1000) % 60, ms % 1000)

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
                .size(30.dp)
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

@Composable
private fun MatchStatusBadge(
    status: MatchStatus,
    modifier: Modifier = Modifier,
) {
    val label: String
    val background: Color
    val content: Color

    when (status) {
        MatchStatus.PENDING -> {
            label = stringResource(R.string.status_pending)
            background = MaterialTheme.colorScheme.surfaceVariant
            content = MaterialTheme.colorScheme.onSurfaceVariant
        }

        MatchStatus.RUNNING -> {
            label = stringResource(R.string.status_running)
            background = CaliGold.copy(alpha = 0.16f)
            content = CaliGold
        }

        MatchStatus.FINISHED -> {
            label = stringResource(R.string.status_finished)
            background = CaliFinished.copy(alpha = 0.14f)
            content = CaliFinished
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = background,
        contentColor = content,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun statusColor(status: MatchStatus): Color =
    when (status) {
        MatchStatus.PENDING -> MaterialTheme.colorScheme.outlineVariant
        MatchStatus.RUNNING -> CaliGold
        MatchStatus.FINISHED -> CaliFinished
    }

private fun formatCreatedDate(iso: String): String {
    val local =
        runCatching { Instant.parse(iso) }
            .getOrNull()
            ?.toLocalDateTime(TimeZone.currentSystemDefault())
            ?: return iso
    val day = local.dayOfMonth.toString().padStart(2, '0')
    val month = local.monthNumber.toString().padStart(2, '0')
    val hour = local.hour.toString().padStart(2, '0')
    val minute = local.minute.toString().padStart(2, '0')
    return "$day/$month/${local.year} $hour:$minute"
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun JudgeMatchCardPreview() {
    CaliArenaTheme {
        JudgeMatchCard(
            item =
                MatchCardItem(
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
                    athleteRed = AthleteOutput(1, "João", AthleteGender.MALE, 1, "2026-01-01T00:00:00Z"),
                    athleteBlue = AthleteOutput(2, "Maria", AthleteGender.FEMALE, 1, "2026-01-01T00:00:00Z"),
                    routine =
                        RoutineOverviewOutput(
                            name = "Back & Biceps",
                            timeCapSeconds = 240,
                            createdAt = "2026-01-01T00:00:00Z",
                            exercises =
                                listOf(
                                    ExerciseOutput(1, 3, "Muscle-Ups", 1, null, 1, 0, ExerciseType.SUPERSET),
                                    ExerciseOutput(2, 3, "Straight-Bar-Dips", 10, null, 1, 1, ExerciseType.SUPERSET),
                                    ExerciseOutput(3, 3, "Pull-Ups", 10, null, 1, 2, ExerciseType.SUPERSET),
                                    ExerciseOutput(4, 3, "Low-Bar Push-Ups", 20, null, 2, null, ExerciseType.NORMAL),
                                    ExerciseOutput(5, 3, "Squats", 20, 20.0, 3, null, ExerciseType.NORMAL),
                                    ExerciseOutput(6, 3, "Muscle-Ups", 5, null, 4, null, ExerciseType.UNBROKEN),
                                ),
                        ),
                ),
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun JudgeMatchCardFinishedPreview() {
    CaliArenaTheme {
        JudgeMatchCard(
            item =
                MatchCardItem(
                    match =
                        MatchOutput(
                            id = 13,
                            bracketId = 1,
                            routineId = 3,
                            judgeId = 2,
                            athleteRedId = 1,
                            athleteBlueId = 2,
                            winnerAthleteId = 1,
                            status = MatchStatus.FINISHED,
                            startedAt = "2026-08-09T10:00:00Z",
                            finishedAt = "2026-08-09T10:15:00Z",
                            createdAt = "2026-08-09T09:00:00Z",
                        ),
                    athleteRed = AthleteOutput(1, "João", AthleteGender.MALE, 1, "2026-01-01T00:00:00Z"),
                    athleteBlue = AthleteOutput(2, "Maria", AthleteGender.FEMALE, 1, "2026-01-01T00:00:00Z"),
                    routine =
                        RoutineOverviewOutput(
                            name = "Back & Biceps",
                            timeCapSeconds = 240,
                            createdAt = "2026-01-01T00:00:00Z",
                            exercises =
                                listOf(
                                    ExerciseOutput(1, 3, "Muscle-Ups", 1, null, 1, 0, ExerciseType.SUPERSET),
                                    ExerciseOutput(2, 3, "Straight-Bar-Dips", 10, null, 1, 1, ExerciseType.SUPERSET),
                                ),
                        ),
                    progress =
                        MatchProgressOutput(
                            id = 1,
                            matchId = 13,
                            redCurrentReps = 0,
                            blueCurrentReps = 0,
                            redFinishedAt = "2026-08-09T10:07:12.850Z",
                            blueFinishedAt = "2026-08-09T10:09:40.300Z",
                            updatedAt = "2026-08-09T10:15:00Z",
                        ),
                ),
        )
    }
}
