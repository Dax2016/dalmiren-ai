package com.darchums.dalmiren.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.darchums.dalmiren.ui.components.DalMirenButton
import com.darchums.dalmiren.ui.components.DalMirenCard
import com.darchums.dalmiren.ui.components.DalMirenHeader
import com.darchums.dalmiren.ui.navigation.AuthSessionState
import com.darchums.dalmiren.ui.theme.ChampagneGold
import com.darchums.dalmiren.ui.theme.RoyalBlue

@Composable
fun AuthScreen(
    state: AuthSessionState,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String?) -> Unit,
    onRetrySession: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isRegisterMode by rememberSaveable { mutableStateOf(false) }
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val busy = state.isLoading || state.isCheckingSession

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DalMirenHeader(
                subtitle = "Understand. Guide. Nurture."
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isRegisterMode) "Create your parent account" else "Welcome back",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )

                Text(
                    text = if (isRegisterMode) {
                        "Create a secure account to begin your personalised parenting journey."
                    } else {
                        "Sign in securely to continue your parenting journey with DALMIREN AI."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (state.isCheckingSession) {
                    DalMirenCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = RoyalBlue)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Verifying your secure session...")
                        }
                    }
                } else {
                    DalMirenCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (isRegisterMode) {
                                OutlinedTextField(
                                    value = fullName,
                                    onValueChange = {
                                        fullName = it
                                        validationError = null
                                        onClearError()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Full name (optional)") },
                                    singleLine = true,
                                    enabled = !busy
                                )
                            }

                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    validationError = null
                                    onClearError()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Email address") },
                                singleLine = true,
                                enabled = !busy,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email
                                )
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    validationError = null
                                    onClearError()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Password") },
                                singleLine = true,
                                enabled = !busy,
                                visualTransformation = if (passwordVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password
                                ),
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            passwordVisible = !passwordVisible
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (passwordVisible) {
                                                Icons.Default.VisibilityOff
                                            } else {
                                                Icons.Default.Visibility
                                            },
                                            contentDescription = if (passwordVisible) {
                                                "Hide password"
                                            } else {
                                                "Show password"
                                            }
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null
                                    )
                                }
                            )

                            if (isRegisterMode) {
                                Text(
                                    text = "Use at least 12 characters for your password.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            val displayedError = validationError ?: state.error

                            if (!displayedError.isNullOrBlank()) {
                                Text(
                                    text = displayedError,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            DalMirenButton(
                                text = if (busy) {
                                    "Please wait..."
                                } else if (isRegisterMode) {
                                    "Create Account"
                                } else {
                                    "Sign In"
                                },
                                onClick = {
                                    val cleanEmail = email.trim()

                                    when {
                                        !android.util.Patterns.EMAIL_ADDRESS
                                            .matcher(cleanEmail).matches() -> {
                                            validationError =
                                                "Enter a valid email address."
                                        }

                                        password.isBlank() -> {
                                            validationError =
                                                "Enter your password."
                                        }

                                        isRegisterMode && password.length < 12 -> {
                                            validationError =
                                                "Your password must contain at least 12 characters."
                                        }

                                        else -> {
                                            validationError = null

                                            if (isRegisterMode) {
                                                onRegister(
                                                    cleanEmail,
                                                    password,
                                                    fullName.trim().takeIf { it.isNotEmpty() }
                                                )
                                            } else {
                                                onLogin(cleanEmail, password)
                                            }
                                        }
                                    }
                                },
                                enabled = !busy
                            )

                            if (busy && state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                    color = RoyalBlue
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            isRegisterMode = !isRegisterMode
                            validationError = null
                            onClearError()
                        },
                        enabled = !busy
                    ) {
                        Text(
                            text = if (isRegisterMode) {
                                "Already have an account? Sign in"
                            } else {
                                "New to DALMIREN? Create an account"
                            },
                            color = RoyalBlue
                        )
                    }

                    if (!state.error.isNullOrBlank() && !state.isLoading) {
                        DalMirenButton(
                            text = "Retry Session Verification",
                            onClick = onRetrySession,
                            isSecondary = true,
                            enabled = !busy
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Your account is protected by secure authentication. Cloud AI processing requires separate, explicit consent.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "DALMIREN AI  •  BY DARCHUMS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = ChampagneGold
                    )
                )
            }
        }
    }
}
