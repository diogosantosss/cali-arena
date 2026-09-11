package com.caliarena.ui.match

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.caliarena.data.MatchStatus
import com.caliarena.data.RepSide
import com.caliarena.ui.theme.CaliArenaTheme
import com.caliarena.ui.theme.CaliFinished
import com.caliarena.ui.theme.CaliGold
import com.caliarena.ui.theme.CaliMuted

@Composable
internal fun MatchTopBar(
    matchId: Int,
    bracketDivision: String?,
    status: MatchStatus?,
    selectedSide: RepSide?,
    connectionLost: Boolean,
    onBack: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 4.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
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

            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.match_number, matchId),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (!bracketDivision.isNullOrBlank()) {
                    Text(
                        text = bracketDivision,
                        style = MaterialTheme.typography.labelSmall,
                        color = CaliMuted,
                        maxLines = 1,
                    )
                }
            }

            status?.let {
                StatusBadge(status = it)

                Spacer(Modifier.width(8.dp))
            }

            if (selectedSide != null) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = sideColor(selectedSide).copy(alpha = 0.16f),
                    contentColor = sideColor(selectedSide),
                ) {
                    Text(
                        text = sideLabel(selectedSide),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            if (connectionLost) {
                Text(
                    text = stringResource(R.string.connection_lost),
                    style = MaterialTheme.typography.labelMedium,
                    color = CaliMuted,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: MatchStatus) {
    val (label, color) =
        when (status) {
            MatchStatus.PENDING -> stringResource(R.string.status_pending) to CaliGold
            MatchStatus.RUNNING -> stringResource(R.string.status_running) to CaliFinished
            MatchStatus.FINISHED -> stringResource(R.string.status_finished) to CaliMuted
        }

    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.16f),
        contentColor = color,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MatchTopBarPreview() {
    CaliArenaTheme {
        MatchTopBar(
            matchId = 12,
            bracketDivision = "MEN ELITE • FINALS",
            status = MatchStatus.RUNNING,
            selectedSide = RepSide.RED,
            connectionLost = false,
            onBack = {},
        )
    }
}
