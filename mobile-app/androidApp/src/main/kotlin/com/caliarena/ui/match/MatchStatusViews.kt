package com.caliarena.ui.match

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.caliarena.R
import com.caliarena.data.ErrorCode
import com.caliarena.ui.theme.CaliArenaTheme
import com.caliarena.ui.theme.CaliMuted
import com.caliarena.util.ErrorDescriptions

@Composable
internal fun ConnectingView(modifier: Modifier = Modifier) {
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
internal fun FailedView(
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
internal fun UnassignedAthletesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Text(
            text = stringResource(R.string.match_athletes_unassigned),
            style = MaterialTheme.typography.bodyMedium,
            color = CaliMuted,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConnectingViewPreview() {
    CaliArenaTheme {
        ConnectingView()
    }
}

@Preview(showBackground = true)
@Composable
private fun FailedViewPreview() {
    CaliArenaTheme {
        FailedView(errorCode = ErrorCode.NO_CONNECTION, onRetry = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun UnassignedAthletesCardPreview() {
    CaliArenaTheme {
        UnassignedAthletesCard()
    }
}
