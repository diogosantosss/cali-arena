package com.caliarena.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caliarena.R
import com.caliarena.data.ErrorCode
import com.caliarena.ui.components.AppTextField
import com.caliarena.ui.theme.CaliArenaTheme
import com.caliarena.ui.theme.CaliDanger
import com.caliarena.ui.theme.CaliMuted
import com.caliarena.util.ErrorDescriptions

@Composable
fun LoginScreenStructure(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@Composable
fun LoginScreenView(
    username: String,
    password: String,
    errorCode: ErrorCode?,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onContactAdministrator: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LoginScreenStructure(modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(120.dp))

            LoginHeaderView()

            Spacer(Modifier.height(40.dp))

            var passwordVisible by rememberSaveable { mutableStateOf(false) }

            LoginFormView(
                username = username,
                password = password,
                passwordVisible = passwordVisible,
                onUsernameChange = onUsernameChange,
                onPasswordChange = onPasswordChange,
                onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                onSubmit = onSubmit,
                canSubmit = username.isNotBlank() && password.isNotBlank(),
            )

            if (errorCode != null) {
                Spacer(Modifier.height(16.dp))
                LoginErrorView(errorCode = errorCode)
            }

            Spacer(Modifier.height(12.dp))

            LoginFooterView(onContactAdministrator = onContactAdministrator)
        }
    }
}

@Composable
fun LoginScreenLoading(modifier: Modifier = Modifier) {
    LoginScreenStructure(modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(strokeWidth = 4.dp)

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.signing_in),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun LoginHeaderView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.login_title),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.login_subtitle),
            color = CaliMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LoginFormView(
    username: String,
    password: String,
    passwordVisible: Boolean,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
    canSubmit: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        AppTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = R.string.username,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        AppTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = R.string.password,
            keyboardType = KeyboardType.Password,
            visualTransformation =
                if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Text(
                        text =
                            stringResource(
                                if (passwordVisible) R.string.password_hide else R.string.password_show,
                            ),
                        color = CaliMuted,
                        fontSize = 12.sp,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onSubmit,
            enabled = canSubmit,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        ) {
            Text(
                text = stringResource(R.string.sign_in),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun LoginErrorView(
    errorCode: ErrorCode,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(ErrorDescriptions.getErrorDescription(errorCode)),
        color = CaliDanger,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun LoginFooterView(
    onContactAdministrator: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(onClick = onContactAdministrator, modifier = modifier) {
        Text(stringResource(R.string.contact_administrator), color = CaliMuted)
    }
}

// ------- Previews -------

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenViewPreview() {
    CaliArenaTheme {
        LoginScreenView(
            username = "",
            password = "",
            errorCode = null,
            onUsernameChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onContactAdministrator = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenLoadingPreview() {
    CaliArenaTheme {
        LoginScreenLoading()
    }
}
