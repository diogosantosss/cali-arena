package com.caliarena.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.caliarena.R
import com.caliarena.data.ErrorCode
import com.caliarena.data.MatchStatus
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
    onMatchClick: (MatchCardItem) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    loggingOut: Boolean = false,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var statusFilter by rememberSaveable { mutableStateOf(MatchStatusFilter.ALL) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                username = username,
                role = role,
                loggingOut = loggingOut,
                onLogout = onLogout,
                onRefresh = onRefresh,
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
                        NoMatchesView(message = stringResource(R.string.no_matches))
                    } else {
                        val filtered = filteredMatches(matchesUiState.matches, statusFilter)
                        MatchListView(
                            matches = filtered,
                            selectedFilter = statusFilter,
                            onFilterChange = { statusFilter = it },
                            onMatchClick = onMatchClick,
                            emptyMessage =
                                if (filtered.isEmpty()) {
                                    stringResource(R.string.matches_filter_empty)
                                } else {
                                    null
                                },
                        )
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
    selectedFilter: MatchStatusFilter,
    onFilterChange: (MatchStatusFilter) -> Unit,
    onMatchClick: (MatchCardItem) -> Unit,
    emptyMessage: String?,
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

        item {
            MatchStatusFilterBar(
                selected = selectedFilter,
                onSelect = onFilterChange,
            )
        }

        if (emptyMessage != null) {
            item {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CaliMuted,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                )
            }
        } else {
            items(matches, key = { it.match.id }) { match ->
                JudgeMatchCard(
                    item = match,
                    onClick = { onMatchClick(match) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun MatchStatusFilterBar(
    selected: MatchStatusFilter,
    onSelect: (MatchStatusFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MatchStatusFilter.values().forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(stringResource(option.labelRes())) },
            )
        }
    }
}

private enum class MatchStatusFilter(
    val status: MatchStatus?,
) {
    ALL(null),
    PENDING(MatchStatus.PENDING),
    RUNNING(MatchStatus.RUNNING),
    FINISHED(MatchStatus.FINISHED),
}

private fun MatchStatusFilter.labelRes(): Int =
    when (this) {
        MatchStatusFilter.ALL -> R.string.matches_filter_all
        MatchStatusFilter.PENDING -> R.string.status_pending
        MatchStatusFilter.RUNNING -> R.string.status_running
        MatchStatusFilter.FINISHED -> R.string.status_finished
    }

private fun filteredMatches(
    matches: List<MatchCardItem>,
    filter: MatchStatusFilter,
): List<MatchCardItem> {
    val status = filter.status ?: return matches
    return matches.filter { it.match.status == status }
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
private fun NoMatchesView(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
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
            onMatchClick = {},
            onRetry = {},
            onRefresh = {},
            onLogout = {},
        )
    }
}
