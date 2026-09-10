package com.caliarena.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caliarena.R
import com.caliarena.data.UserRole
import com.caliarena.ui.theme.CaliGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    username: String,
    role: UserRole?,
    onLogout: () -> Unit,
    onRefresh: () -> Unit,
    loggingOut: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var showConfirm by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f to CaliGold.copy(alpha = 0.10f),
                        1f to Color.Transparent,
                    ),
                ).drawWithContent {
                    drawContent()
                    drawLine(
                        color = CaliGold.copy(alpha = 0.18f),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                },
    ) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Avatar(username = username)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = username,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                        RoleBadge(role = role)
                    }
                }
            },
            actions = {
                IconButton(
                    onClick = onRefresh,
                    enabled = !loggingOut,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.refresh),
                        tint = CaliGold,
                    )
                }
                TextButton(
                    onClick = { showConfirm = true },
                    enabled = !loggingOut,
                ) {
                    if (loggingOut) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.logout),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
        )
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { if (!loggingOut) showConfirm = false },
            title = { Text(stringResource(R.string.logout_confirm_title)) },
            text = { Text(stringResource(R.string.logout_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirm = false
                        onLogout()
                    },
                    enabled = !loggingOut,
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirm = false },
                    enabled = !loggingOut,
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun Avatar(username: String) {
    val initial = username.firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier =
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(CaliGold.copy(alpha = 0.16f))
                .border(1.dp, CaliGold.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            color = CaliGold,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun RoleBadge(role: UserRole?) {
    Surface(
        color = CaliGold.copy(alpha = 0.08f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, CaliGold.copy(alpha = 0.30f)),
    ) {
        Text(
            text = role.toLabel(),
            style = MaterialTheme.typography.labelSmall,
            color = CaliGold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun UserRole?.toLabel(): String =
    when (this) {
        UserRole.ADMIN -> stringResource(R.string.role_admin)
        UserRole.JUDGE -> stringResource(R.string.role_judge)
        null -> stringResource(R.string.role_unknown)
    }

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AppTopBarPreview() {
    MaterialTheme {
        Scaffold(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                AppTopBar(
                    username = "judge01",
                    role = UserRole.JUDGE,
                    loggingOut = false,
                    onLogout = {},
                    onRefresh = {},
                )
            },
        ) {}
    }
}
