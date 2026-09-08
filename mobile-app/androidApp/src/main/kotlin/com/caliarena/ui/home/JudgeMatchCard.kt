package com.caliarena.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.caliarena.R
import com.caliarena.data.AthleteGender
import com.caliarena.data.AthleteOutput
import com.caliarena.data.MatchOutput
import com.caliarena.data.MatchStatus
import com.caliarena.data.RoutineOverviewOutput
import com.caliarena.ui.theme.CaliArenaTheme
import com.caliarena.ui.theme.CaliAthleteBlue
import com.caliarena.ui.theme.CaliAthleteRed
import com.caliarena.ui.theme.CaliFinished
import com.caliarena.ui.theme.CaliGold
import com.caliarena.ui.theme.CaliMuted
import com.caliarena.viewmodel.MatchCardItem

@Composable
fun JudgeMatchCard(
    item: MatchCardItem,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.match_number, item.match.id),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(Modifier.weight(1f))

                MatchStatusBadge(item.match.status)
            }

            Spacer(Modifier.height(12.dp))

            val routine = item.routine
            if (routine != null) {
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
            } else {
                Text(
                    text = stringResource(R.string.match_routine_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CaliMuted,
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                AthleteSide(
                    name = item.athleteRed?.name,
                    dotColor = CaliAthleteRed,
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
                    dotColor = CaliAthleteBlue,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.match_created_on, formatCreatedDate(item.match.createdAt)),
                style = MaterialTheme.typography.labelMedium,
                color = CaliMuted,
            )
        }
    }
}

@Composable
private fun AthleteSide(
    name: String?,
    dotColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier
                    .size(10.dp)
                    .background(dotColor, CircleShape),
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = if (name != null) name else stringResource(R.string.athlete_unassigned),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (name != null) MaterialTheme.colorScheme.onSurface else CaliMuted,
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

private fun formatCreatedDate(iso: String): String {
    val parts = iso.substringBefore('T').split('-')
    return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else iso
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
                    routine = RoutineOverviewOutput("Back & Biceps", 240, "2026-01-01T00:00:00Z", emptyList()),
                ),
        )
    }
}
