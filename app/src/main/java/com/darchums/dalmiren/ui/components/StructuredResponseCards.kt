package com.darchums.dalmiren.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.ShieldMoon
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darchums.dalmiren.domain.model.ChatMessage
import com.darchums.dalmiren.ui.theme.ChampagneGold
import com.darchums.dalmiren.ui.theme.Ivory
import com.darchums.dalmiren.ui.theme.MidnightNavy
import com.darchums.dalmiren.ui.theme.RoyalBlue
import com.darchums.dalmiren.ui.theme.SoftBlueContainer
import com.darchums.dalmiren.ui.theme.SoftBlueOnContainer
import com.darchums.dalmiren.ui.theme.SoftGoldContainer
import com.darchums.dalmiren.ui.theme.SoftGoldOnContainer

@Composable
fun StructuredResponseCardsContainer(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Empathy & Summary Header Card (Midnight Navy)
        if (message.empathyStatement.isNotBlank() || message.responseSummary.isNotBlank()) {
            UnderstandingChildCard(
                summary = message.responseSummary,
                empathy = message.empathyStatement,
                understanding = message.understanding
            )
        }

        // 2. What Might Be Happening Card
        if (message.possibleContributingFactors.isNotEmpty() || message.developmentalContext.isNotBlank()) {
            ContributingFactorsCard(
                factors = message.possibleContributingFactors,
                contextText = message.developmentalContext
            )
        }

        // 3. What to Say / Communication Scripts Card (Champagne Gold with Copy)
        if (message.communicationScripts.isNotEmpty()) {
            CommunicationScriptsCard(
                scripts = message.communicationScripts,
                onCopyScript = { script ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("DALMIREN Script", script))
                    Toast.makeText(context, "Script copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 4. Practical Action Plan Card
        if (message.practicalSteps.isNotEmpty() || message.actionPlan.isNotEmpty()) {
            PracticalActionPlanCard(
                steps = message.practicalSteps,
                actionPlan = message.actionPlan
            )
        }

        // 5. Positive Discipline & Boundaries Card
        if (message.positiveDisciplineGuidance.isNotBlank()) {
            PositiveDisciplineCard(guidance = message.positiveDisciplineGuidance)
        }

        // 6. What to Avoid Card
        if (message.whatToAvoid.isNotEmpty()) {
            WhatToAvoidCard(items = message.whatToAvoid)
        }

        // 7. Reflection & Follow-Up Card
        if (message.reflectionQuestions.isNotEmpty() || message.followUpQuestion.isNotBlank()) {
            ReflectionFollowUpCard(
                questions = message.reflectionQuestions,
                followUp = message.followUpQuestion
            )
        }

        // 8. Professional Support Guidance
        if (!message.professionalSupportGuidance.isNullOrBlank()) {
            ProfessionalSupportCard(guidance = message.professionalSupportGuidance)
        }
    }
}

@Composable
fun UnderstandingChildCard(
    summary: String,
    empathy: String,
    understanding: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MidnightNavy),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = ChampagneGold,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Understanding Your Child",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = ChampagneGold
                    )
                )
            }
            if (summary.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Ivory
                    )
                )
            }
            if (empathy.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = empathy,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Ivory.copy(alpha = 0.9f),
                        lineHeight = 20.sp
                    )
                )
            }
            if (understanding.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = understanding,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Ivory.copy(alpha = 0.8f),
                        lineHeight = 18.sp
                    )
                )
            }
        }
    }
}

@Composable
fun ContributingFactorsCard(
    factors: List<String>,
    contextText: String
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        colors = CardDefaults.cardColors(containerColor = SoftBlueContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = RoyalBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "What Might Be Happening",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SoftBlueOnContainer
                        )
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = RoyalBlue
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    factors.forEach { factor ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "• ",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalBlue
                                )
                            )
                            Text(
                                text = factor,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = SoftBlueOnContainer,
                                    lineHeight = 20.sp
                                )
                            )
                        }
                    }
                    if (contextText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Developmental Context: $contextText",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SoftBlueOnContainer.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommunicationScriptsCard(
    scripts: List<String>,
    onCopyScript: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SoftGoldContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = SoftGoldOnContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "What to Say (Communication Scripts)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = SoftGoldOnContainer
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            scripts.forEach { script ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = script,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 20.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onCopyScript(script) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Script",
                                tint = RoyalBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PracticalActionPlanCard(
    steps: List<String>,
    actionPlan: List<String>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = RoyalBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Practical Action Plan",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            (steps + actionPlan).distinct().forEachIndexed { index, step ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(RoyalBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Ivory,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun PositiveDisciplineCard(guidance: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SoftBlueContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ShieldMoon,
                    contentDescription = null,
                    tint = RoyalBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Positive Discipline & Boundaries",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = SoftBlueOnContainer
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = guidance,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = SoftBlueOnContainer,
                    lineHeight = 20.sp
                )
            )
        }
    }
}

@Composable
fun WhatToAvoidCard(items: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "What to Avoid",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            items.forEach { item ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "✕ ",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    )
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            lineHeight = 20.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ReflectionFollowUpCard(
    questions: List<String>,
    followUp: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MidnightNavy),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FormatQuote,
                    contentDescription = null,
                    tint = ChampagneGold,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Reflection & Follow-Up",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = ChampagneGold
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            questions.forEach { q ->
                Text(
                    text = "• $q",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Ivory.copy(alpha = 0.9f),
                        lineHeight = 20.sp
                    ),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            if (followUp.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Next Question to Consider: $followUp",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = ChampagneGold,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
fun ProfessionalSupportCard(guidance: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SoftBlueContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = RoyalBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Professional Support Guidance",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = SoftBlueOnContainer
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = guidance,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = SoftBlueOnContainer,
                    lineHeight = 20.sp
                )
            )
        }
    }
}
