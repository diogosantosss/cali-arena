package com.caliarena.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.caliarena.data.UserRole
import com.caliarena.ui.components.AppTopBar
import com.caliarena.ui.theme.CaliArenaTheme
import com.caliarena.ui.theme.CaliMuted
import com.caliarena.util.ErrorDescriptions
import com.caliarena.viewmodel.MatchCardItem
import com.caliarena.viewmodel.MatchesUiState

@Composable
fun HomeScreen(
    username: String,
    role: UserRole?,
    matchesUiState: MatchesUiState,
    onRetry: () -> Unit,
    loggingOut: Boolean = false,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                username = username,
                role = role,
                loggingOut = loggingOut,
                onLogout = onLogout,
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
            when (matchesUiState) {
                MatchesUiState.Loading -> MatchListLoading()

                is MatchesUiState.Ready ->
                    if (matchesUiState.matches.isEmpty()) {
                        NoMatchesView()
                    } else {
                        MatchListView(matchesUiState.matches)
                    }

                is MatchesUiState.Failed ->
                    MatchListError(
                        errorCode = matchesUiState.code,
                        onRetry = onRetry,
                    )
            }
        }
    }
}

@Composable
private fun MatchListView(
    matches: List<MatchCardItem>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.matches_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        items(matches, key = { it.match.id }) { match ->
            JudgeMatchCard(item = match)
        }
    }
}

@Composable
private fun MatchListLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.loading_matches),
            style = MaterialTheme.typography.bodyMedium,
            color = CaliMuted,
        )
    }
}

@Composable
private fun MatchListError(
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
            text = stringResource(R.string.matches_load_failed),
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
private fun NoMatchesView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.no_matches),
            style = MaterialTheme.typography.bodyMedium,
            color = CaliMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp),
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    CaliArenaTheme {
        HomeScreen(
            username = "admin",
            role = UserRole.ADMIN,
            matchesUiState = MatchesUiState.Loading,
            onRetry = {},
            onLogout = {},
        )
    }
}
