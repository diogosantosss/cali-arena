package com.caliarena.ui.login

import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.caliarena.ui.theme.CaliArenaTheme
import com.caliarena.ui.theme.CaliDanger
import com.caliarena.ui.theme.CaliMuted

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
    state: LoginState,
    onEvent: (LoginEvent) -> Unit,
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

            LoginFormView(
                state = state,
                onUsernameChange = { onEvent(LoginEvent.UsernameChanged(it)) },
                onPasswordChange = { onEvent(LoginEvent.PasswordChanged(it)) },
                onTogglePasswordVisibility = { onEvent(LoginEvent.TogglePasswordVisibility) },
                onSubmit = { onEvent(LoginEvent.LoginSubmitted) },
            )

            if (state.errorMessage != null) {
                Spacer(Modifier.height(16.dp))
                LoginErrorView(message = state.errorMessage)
            }

            Spacer(Modifier.height(12.dp))

            LoginFooterView(
                onContactAdministrator = { onEvent(LoginEvent.ContactAdministrator) },
            )
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
fun LoginScreenError(
    modifier: Modifier = Modifier,
    @StringRes message: Int = R.string.error_login,
    onRetry: () -> Unit = {},
) {
    LoginScreenStructure(modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.error),
                style = MaterialTheme.typography.headlineSmall,
                color = CaliDanger,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.try_again), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
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
    state: LoginState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = state.username,
            onValueChange = onUsernameChange,
            label = { Text(stringResource(R.string.username)) },
            singleLine = true,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(R.string.password)) },
            singleLine = true,
            enabled = !state.isLoading,
            visualTransformation =
                if (state.passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Text(
                        text =
                            stringResource(
                                if (state.passwordVisible) R.string.password_hide else R.string.password_show,
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
            enabled = state.canSubmit,
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
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = stringResource(R.string.sign_in),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun LoginErrorView(
    @StringRes message: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(message),
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
        LoginScreenView(state = LoginState(), onEvent = {})
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenLoadingPreview() {
    CaliArenaTheme {
        LoginScreenLoading()
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenErrorPreview() {
    CaliArenaTheme {
        LoginScreenError(message = R.string.error_login_invalid)
    }
}

// ------- Dark theme previews -------

@Preview(name = "Login - Dark", showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenViewDarkPreview() {
    CaliArenaTheme(darkTheme = true) {
        LoginScreenView(state = LoginState(), onEvent = {})
    }
}

@Preview(name = "Login Loading - Dark", showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenLoadingDarkPreview() {
    CaliArenaTheme(darkTheme = true) {
        LoginScreenLoading()
    }
}

@Preview(name = "Login Error - Dark", showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenErrorDarkPreview() {
    CaliArenaTheme(darkTheme = true) {
        LoginScreenError(message = R.string.error_login_invalid)
    }
}
