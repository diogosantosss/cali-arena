package com.caliarena.ui.match

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.caliarena.data.ErrorCode
import com.caliarena.ui.theme.CaliArenaTheme
import com.caliarena.ui.theme.CaliGold
import com.caliarena.ui.theme.CaliMuted
import com.caliarena.util.ErrorDescriptions

@Composable
internal fun ConnectionLostBanner(
    onReconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CaliGold.copy(alpha = 0.14f),
        contentColor = CaliGold,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.connection_lost),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = stringResource(ErrorDescriptions.getErrorDescription(ErrorCode.NO_CONNECTION)),
                    style = MaterialTheme.typography.bodySmall,
                    color = CaliMuted,
                )
            }

            Spacer(Modifier.width(12.dp))

            Button(
                onClick = onReconnect,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = CaliGold,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Text(stringResource(R.string.reconnect))
            }
        }
    }
}

@Composable
internal fun ActionErrorBanner(
    code: ErrorCode,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Text(
            text = stringResource(ErrorDescriptions.getErrorDescription(code)),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConnectionLostBannerPreview() {
    CaliArenaTheme {
        ConnectionLostBanner(onReconnect = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ActionErrorBannerPreview() {
    CaliArenaTheme {
        ActionErrorBanner(code = ErrorCode.NO_CONNECTION)
    }
}
