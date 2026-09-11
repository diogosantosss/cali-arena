package com.caliarena.ui.match

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.caliarena.data.RepSide
import com.caliarena.ui.theme.CaliArenaTheme
import com.caliarena.viewmodel.MatchDetailUiState

@Composable
fun MatchScreen(
    matchId: Int,
    uiState: MatchDetailUiState,
    selectedSide: RepSide?,
    onSelectSide: (RepSide) -> Unit,
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
                bracketDivision = (uiState as? MatchDetailUiState.Ready)?.bracketDivision,
                status = (uiState as? MatchDetailUiState.Ready)?.match?.status,
                selectedSide = selectedSide,
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
                        selectedSide = selectedSide,
                        onSelectSide = onSelectSide,
                        onReconnect = onReconnect,
                        onAdjust = onAdjust,
                        onFinish = onFinish,
                    )
            }
        }
    }
}

@Composable
internal fun ReadyContent(
    state: MatchDetailUiState.Ready,
    selectedSide: RepSide?,
    onSelectSide: (RepSide) -> Unit,
    onReconnect: () -> Unit,
    onAdjust: (RepSide, Int) -> Unit,
    onFinish: (RepSide) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.connectionLost) {
            ConnectionLostBanner(onReconnect = onReconnect)
        }

        state.lastActionError?.let { code ->
            ActionErrorBanner(code = code)
        }

        if (selectedSide == null) {
            SideChooser(
                state = state,
                onSelectSide = onSelectSide,
            )
        } else {
            JudgePanel(
                state = state,
                side = selectedSide,
                onAdjust = onAdjust,
                onFinish = onFinish,
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MatchScreenPreview() {
    CaliArenaTheme {
        MatchScreen(
            matchId = 12,
            uiState = matchReadyState,
            selectedSide = RepSide.RED,
            onSelectSide = {},
            onBack = {},
            onRetry = {},
            onReconnect = {},
            onAdjust = { _, _ -> },
            onFinish = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ReadyContentChoicePreview() {
    CaliArenaTheme {
        ReadyContent(
            state = matchReadyState,
            selectedSide = null,
            onSelectSide = {},
            onReconnect = {},
            onAdjust = { _, _ -> },
            onFinish = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ReadyContentJudgingPreview() {
    CaliArenaTheme {
        ReadyContent(
            state = matchReadyState,
            selectedSide = RepSide.RED,
            onSelectSide = {},
            onReconnect = {},
            onAdjust = { _, _ -> },
            onFinish = {},
        )
    }
}
