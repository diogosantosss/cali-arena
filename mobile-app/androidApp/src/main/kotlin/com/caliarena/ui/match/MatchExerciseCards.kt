package com.caliarena.ui.match

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caliarena.R
import com.caliarena.data.RepSide
import com.caliarena.ui.theme.CaliArenaTheme
import com.caliarena.ui.theme.CaliFinished
import com.caliarena.ui.theme.CaliGold
import com.caliarena.ui.theme.CaliMuted

@Composable
internal fun CurrentExerciseCard(
    label: String,
    reps: Int,
    accentColor: Color,
    enabled: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.current_exercise_label),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = CaliMuted,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RepControlButton(
                    content = {
                        Text(
                            text = "−",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    },
                    enabled = enabled,
                    onClick = onDecrement,
                    accentColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                Spacer(Modifier.weight(1f))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = reps.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        lineHeight = 72.sp,
                    )

                    Text(
                        text = stringResource(R.string.reps_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = CaliMuted,
                    )
                }

                Spacer(Modifier.weight(1f))

                RepControlButton(
                    content = {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.reps_increase),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp),
                        )
                    },
                    enabled = enabled,
                    onClick = onIncrement,
                    accentColor = accentColor,
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RepControlButton(
    content: @Composable () -> Unit,
    enabled: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by
        animateFloatAsState(
            targetValue = if (pressed) 0.84f else 1f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
            label = "repButtonScale",
        )

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        interactionSource = interactionSource,
        modifier =
            Modifier
                .size(64.dp)
                .scale(scale),
        contentPadding = ButtonDefaults.ContentPadding,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    ) {
        content()
    }
}

@Composable
internal fun NextExerciseCard(
    nextLabel: String,
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
        Text(
            text = stringResource(R.string.next_exercise_label).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = CaliGold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp),
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = nextLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = CaliMuted,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
        )
    }
}

@Composable
internal fun PendingStartCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = CaliGold.copy(alpha = 0.10f),
                contentColor = CaliGold,
            ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.match_pending_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.match_pending_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = CaliMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun FinishedCard(
    elapsedMs: Long?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = CaliFinished.copy(alpha = 0.08f),
                contentColor = CaliFinished,
            ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.match_finished),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            if (elapsedMs != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = formatElapsed(elapsedMs),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.final_time_label),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = CaliMuted,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CurrentExerciseCardPreview() {
    CaliArenaTheme {
        CurrentExerciseCard(
            label = "Muscle-Ups / Pull-Ups",
            reps = 23,
            accentColor = sideColor(RepSide.RED),
            enabled = true,
            onIncrement = {},
            onDecrement = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NextExerciseCardPreview() {
    CaliArenaTheme {
        NextExerciseCard(nextLabel = "Low-Bar Push-Ups")
    }
}

@Preview(showBackground = true)
@Composable
private fun PendingStartCardPreview() {
    CaliArenaTheme {
        PendingStartCard()
    }
}

@Preview(showBackground = true)
@Composable
private fun FinishedCardPreview() {
    CaliArenaTheme {
        FinishedCard(elapsedMs = 475_000L)
    }
}
