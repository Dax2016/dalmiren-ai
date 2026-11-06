package com.darchums.dalmiren.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darchums.dalmiren.domain.model.AgeGroup
import com.darchums.dalmiren.ui.components.ChipSelector
import com.darchums.dalmiren.ui.components.DalMirenButton
import com.darchums.dalmiren.ui.components.DalMirenCard
import com.darchums.dalmiren.ui.components.DalMirenHeader
import com.darchums.dalmiren.ui.theme.ChampagneGold
import com.darchums.dalmiren.ui.theme.MidnightNavy
import com.darchums.dalmiren.ui.theme.RoyalBlue
import com.darchums.dalmiren.ui.navigation.ConsentViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onResetOnboarding: () -> Unit,
    accountEmail: String?,
    isLoggingOut: Boolean,
    onLogout: () -> Unit,
    consentViewModel: ConsentViewModel,
    modifier: Modifier = Modifier
) {
    val prefs by viewModel.userPreferences.collectAsState()
    val consentState by consentViewModel.state.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        DalMirenHeader(subtitle = "Settings & Privacy Governance")

        Column(modifier = Modifier.padding(16.dp)) {
            // User Preferences Section
            DalMirenCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "User Preferences",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Default Age Group",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = RoyalBlue
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ChipSelector(
                        options = AgeGroup.entries.map { it.label },
                        selectedOption = prefs.defaultAgeGroup.label,
                        onOptionSelected = { label ->
                            val match = AgeGroup.entries.find { it.label == label }
                            if (match != null) viewModel.setDefaultAgeGroup(match)
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dark Theme",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Enable deep midnight navy dark mode",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        Switch(
                            checked = prefs.darkTheme == true,
                            onCheckedChange = { checked ->
                                viewModel.setDarkTheme(checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ChampagneGold,
                                checkedTrackColor = MidnightNavy
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Privacy and Cloud Processing Consent
            DalMirenCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = RoyalBlue
                        )
                        Spacer(modifier = Modifier.padding(start = 8.dp))
                        Text(
                            text = "Privacy & Data Protection",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "DALMIREN AI uses local-first processing where available. " +
                            "Cloud AI processing is optional and disabled unless you explicitly enable it.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "If you enable OpenAI cloud processing, information included " +
                            "in your parenting AI requests may be sent to OpenAI for processing. " +
                            "Do not include unnecessary identifying details such as a child's " +
                            "full name, home address, or school.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "When cloud processing is disabled, DALMIREN will not use " +
                            "OpenAI for your requests. Local AI is attempted when available; " +
                            "if it is unavailable, AI-generated guidance may not be available.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Allow OpenAI Cloud Processing",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            Text(
                                text = when {
                                    consentState.isLoading -> "Checking your saved preference..."
                                    consentState.isSaving -> "Saving your choice..."
                                    !consentState.hasLoaded -> "Consent status could not be confirmed."
                                    consentState.cloudProcessingGranted -> "Enabled â€” OpenAI processing is permitted."
                                    else -> "Disabled â€” local processing only."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = consentState.cloudProcessingGranted,
                            enabled = consentState.canChangeConsent,
                            onCheckedChange = { granted ->
                                consentViewModel.setCloudProcessingConsent(granted)
                            }
                        )
                    }

                    if (!consentState.hasLoaded && !consentState.isLoading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { consentViewModel.loadConsent() }
                        ) {
                            Text("Retry consent status")
                        }
                    }

                    if (consentState.error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = consentState.error.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "You can withdraw consent at any time. Your choice is " +
                            "saved to your authenticated account. DALMIREN provides " +
                            "non-diagnostic, educational parenting guidance.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Account and Authentication
            DalMirenCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Account & Security",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = accountEmail ?: "Authenticated parent account",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    DalMirenButton(
                        text = if (isLoggingOut) "Signing out..." else "Sign Out",
                        onClick = { showLogoutDialog = true },
                        isSecondary = true,
                        enabled = !isLoggingOut
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Data Controls
            DalMirenCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Data Management",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Clear all local preferences, reflection logs, and reset onboarding.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    DalMirenButton(
                        text = "Clear All Local Data",
                        onClick = { showClearDialog = true },
                        isSecondary = true,
                        icon = Icons.Default.DeleteForever
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // About DARCHUMS
            DalMirenCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = ChampagneGold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "DALMIREN AI",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "BY DARCHUMS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = ChampagneGold,
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Version 1.0.0 Ã¢â‚¬Â¢ Premium Parenting Intelligence",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isLoggingOut) showLogoutDialog = false
            },
            title = {
                Text("Sign out of DALMIREN AI?")
            },
            text = {
                Text(
                    "You will need to sign in again to access your parenting workspace. Your saved local preferences and reflections will not be cleared."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isLoggingOut,
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Sign Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isLoggingOut,
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All User Data?") },
            text = { Text("This will permanently remove your stored preferences, reflections, and return to onboarding. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllUserData {
                            showClearDialog = false
                            onResetOnboarding()
                        }
                    }
                ) {
                    Text("Clear Data", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
