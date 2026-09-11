package com.caliarena.ui.match

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.caliarena.R
import com.caliarena.data.AthleteOutput
import com.caliarena.data.RepSide
import com.caliarena.ui.theme.CaliArenaTheme
import com.caliarena.ui.theme.CaliMuted
import com.caliarena.viewmodel.MatchDetailUiState

@Composable
internal fun SideChooser(
    state: MatchDetailUiState.Ready,
    onSelectSide: (RepSide) -> Unit,
) {
    val hasAssigned = state.athleteRed != null || state.athleteBlue != null

    if (!hasAssigned) {
        UnassignedAthletesCard()
        return
    }

    Text(
        text = stringResource(R.string.match_choose_side_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
    )

    Spacer(Modifier.height(4.dp))

    Text(
        text = stringResource(R.string.match_choose_side_subtitle),
        style = MaterialTheme.typography.bodySmall,
        color = CaliMuted,
    )

    Spacer(Modifier.height(16.dp))

    ChoiceSideCard(
        label = RepSide.RED,
        athlete = state.athleteRed,
        clubName = state.clubRedName,
        onSelect = { onSelectSide(RepSide.RED) },
    )

    Spacer(Modifier.height(12.dp))

    ChoiceSideCard(
        label = RepSide.BLUE,
        athlete = state.athleteBlue,
        clubName = state.clubBlueName,
        onSelect = { onSelectSide(RepSide.BLUE) },
    )
}

@Composable
private fun ChoiceSideCard(
    label: RepSide,
    athlete: AthleteOutput?,
    clubName: String?,
    onSelect: () -> Unit,
) {
    val accent = sideColor(label)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = accent.copy(alpha = 0.16f),
                    contentColor = accent,
                ) {
                    Text(
                        text = sideLabel(label),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(Modifier.weight(1f))

                Box(
                    modifier =
                        Modifier
                            .size(24.dp)
                            .border(2.dp, accent, CircleShape),
                )
            }

            Spacer(Modifier.height(10.dp))

            AthleteAvatar(name = athlete?.name, accentColor = accent, size = 52.dp)

            Spacer(Modifier.height(10.dp))

            Text(
                text = athlete?.name ?: stringResource(R.string.athlete_unassigned),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (athlete != null) MaterialTheme.colorScheme.onSurface else CaliMuted,
            )

            if (athlete != null) {
                Text(
                    text = clubName ?: stringResource(R.string.match_side_club_missing),
                    style = MaterialTheme.typography.bodySmall,
                    color = CaliMuted,
                )
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onSelect,
                enabled = athlete != null,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Text(stringResource(R.string.match_judge_side))
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SideChooserPreview() {
    CaliArenaTheme {
        SideChooser(state = matchReadyState, onSelectSide = {})
    }
}
