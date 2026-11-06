package com.darchums.dalmiren.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.ShieldMoon
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darchums.dalmiren.domain.model.ParentReflection
import com.darchums.dalmiren.ui.components.DalMirenCard
import com.darchums.dalmiren.ui.components.DalMirenHeader
import com.darchums.dalmiren.ui.components.QuickActionCard
import com.darchums.dalmiren.ui.components.SafetyDisclaimerBanner
import com.darchums.dalmiren.ui.theme.ChampagneGold
import com.darchums.dalmiren.ui.theme.MidnightNavy
import com.darchums.dalmiren.ui.theme.RoyalBlue
import com.darchums.dalmiren.ui.theme.SoftGoldContainer
import com.darchums.dalmiren.ui.theme.SoftGoldOnContainer

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToCompanion: () -> Unit,
    onNavigateToBehaviour: () -> Unit,
    onNavigateToEmotional: () -> Unit,
    onNavigateToCommunication: () -> Unit,
    onNavigateToDiscipline: () -> Unit,
    onNavigateToLearning: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recentReflections by viewModel.recentReflections.collectAsState()
    val modules by viewModel.modules.collectAsState()
    val modulesById = modules.associateBy { it.id.uppercase() }

    fun moduleTitle(id: String, fallback: String): String =
        modulesById[id]?.name?.takeIf { it.isNotBlank() } ?: fallback

    fun moduleSubtitle(id: String, fallback: String): String {
        val module = modulesById[id] ?: return fallback
        return listOf(module.role, module.mission)
            .filter { it.isNotBlank() }
            .joinToString(" — ")
            .ifBlank { fallback }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        DalMirenHeader(subtitle = "Understand. Guide. Nurture.")

        Column(modifier = Modifier.padding(16.dp)) {
            // Welcome Card
            Text(
                text = "Welcome to DALMIREN AI",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = "Empathetic parenting guidance for your family's daily moments",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Daily Reflection Tip
            Surface(
                color = SoftGoldContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = null,
                        tint = SoftGoldOnContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Daily Parenting Reflection",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = SoftGoldOnContainer
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = viewModel.dailyTip,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = SoftGoldOnContainer,
                                lineHeight = 20.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // AI Companion Feature Card
            QuickActionCard(
                title = "AI Parenting Companion",
                subtitle = "Ask any parenting question in real-time conversation",
                icon = Icons.Default.ChatBubbleOutline,
                onClick = onNavigateToCompanion,
                badgeText = "Interactive"
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Parenting Intelligence Modules",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(10.dp))

            // 5 Intelligence Modules
            QuickActionCard(
                title = moduleTitle("BEHAVIOUR", "Behaviour Intelligence"),
                subtitle = moduleSubtitle("BEHAVIOUR", "Understand triggers, underlying needs, and calm responses"),
                icon = Icons.Default.Psychology,
                onClick = onNavigateToBehaviour
            )
            Spacer(modifier = Modifier.height(10.dp))

            QuickActionCard(
                title = moduleTitle("EMOTIONAL", "Emotional Intelligence"),
                subtitle = moduleSubtitle("EMOTIONAL", "Age-aware emotional coaching & co-regulation techniques"),
                icon = Icons.Default.FavoriteBorder,
                onClick = onNavigateToEmotional
            )
            Spacer(modifier = Modifier.height(10.dp))

            QuickActionCard(
                title = moduleTitle("COMMUNICATION", "Communication Guidance"),
                subtitle = moduleSubtitle("COMMUNICATION", "Active listening, example scripts & conflict repair"),
                icon = Icons.Default.RecordVoiceOver,
                onClick = onNavigateToCommunication
            )
            Spacer(modifier = Modifier.height(10.dp))

            QuickActionCard(
                title = moduleTitle("DISCIPLINE", "Positive Discipline"),
                subtitle = moduleSubtitle("DISCIPLINE", "Constructive boundaries without shouting or threats"),
                icon = Icons.Default.ShieldMoon,
                onClick = onNavigateToDiscipline
            )
            Spacer(modifier = Modifier.height(10.dp))

            QuickActionCard(
                title = moduleTitle("LEARNING", "Learning Support"),
                subtitle = moduleSubtitle("LEARNING", "Encourage curiosity, motivation & healthy routines"),
                icon = Icons.Default.MenuBook,
                onClick = onNavigateToLearning
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Guidance History / Reflections
            Text(
                text = "Recent Guidance History",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (recentReflections.isEmpty()) {
                DalMirenCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = null,
                            tint = ChampagneGold,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No guidance history recorded yet",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Use any intelligence module above to receive structured parenting insights.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            } else {
                recentReflections.take(2).forEach { ref ->
                    ReflectionSummaryCard(reflection = ref)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SafetyDisclaimerBanner()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ReflectionSummaryCard(reflection: ParentReflection) {
    DalMirenCard {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reflection.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = reflection.category.title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = RoyalBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = reflection.notes,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
