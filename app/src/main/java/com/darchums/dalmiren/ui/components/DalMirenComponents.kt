package com.darchums.dalmiren.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darchums.dalmiren.ui.theme.ChampagneGold
import com.darchums.dalmiren.ui.theme.Ivory
import com.darchums.dalmiren.ui.theme.MidnightNavy
import com.darchums.dalmiren.ui.theme.RoyalBlue
import com.darchums.dalmiren.ui.theme.SoftGoldContainer
import com.darchums.dalmiren.ui.theme.SoftGoldOnContainer

@Composable
fun DalMirenHeader(
    modifier: Modifier = Modifier,
    subtitle: String? = "Understand. Guide. Nurture."
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MidnightNavy,
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(ChampagneGold, RoyalBlue)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = Ivory,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "DALMIREN AI",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = Ivory,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "BY DARCHUMS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = ChampagneGold,
                            letterSpacing = 1.5.sp,
                            fontSize = 9.sp
                        )
                    )
                }
            }
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Ivory.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

@Composable
fun DalMirenCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(16.dp),
        content = content
    )
}

@Composable
fun DalMirenButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSecondary: Boolean = false,
    icon: ImageVector? = null
) {
    if (isSecondary) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, RoyalBlue)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, tint = RoyalBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text = text, color = RoyalBlue, fontWeight = FontWeight.SemiBold)
            }
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MidnightNavy,
                contentColor = Ivory
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, tint = ChampagneGold)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    fontWeight = FontWeight.Bold,
                    color = Ivory,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SafetyDisclaimerBanner(
    modifier: Modifier = Modifier,
    text: String = "DALMIREN AI provides non-diagnostic, supportive parenting insights. If you suspect emergency, medical, or safety concerns, please consult a healthcare or safety professional."
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SoftGoldContainer,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Safety note",
                tint = SoftGoldOnContainer,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = SoftGoldOnContainer,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            )
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null
) {
    DalMirenCard(
        modifier = modifier.clickable { onClick() },
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RoyalBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = RoyalBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    if (badgeText != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = ChampagneGold.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MidnightNavy,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipSelector(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            FilterChip(
                selected = isSelected,
                onClick = { onOptionSelected(option) },
                label = {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RoyalBlue,
                    selectedLabelColor = Ivory,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun GuidanceCard(
    title: String,
    hypotheses: List<String>,
    possibleFeelings: List<String>,
    calmResponse: String,
    nextObservations: List<String>,
    modifier: Modifier = Modifier,
    disclaimerText: String = "Hypotheses generated for parenting insight. Not a medical or psychological diagnosis."
) {
    DalMirenCard(
        modifier = modifier,
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
        borderColor = ChampagneGold.copy(alpha = 0.6f)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = ChampagneGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "What might be happening (Hypotheses):",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = RoyalBlue
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            hypotheses.forEach { hyp ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(text = "• ", fontWeight = FontWeight.Bold, color = ChampagneGold)
                    Text(
                        text = hyp,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Child's Possible Emotional Need:",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = RoyalBlue
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            possibleFeelings.forEach { feeling ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(text = "• ", fontWeight = FontWeight.Bold, color = RoyalBlue)
                    Text(
                        text = feeling,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = SoftGoldContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Calm & Constructive Response Today:",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = SoftGoldOnContainer
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = calmResponse,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = SoftGoldOnContainer,
                            lineHeight = 20.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "What to Observe Next:",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            nextObservations.forEach { obs ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = RoyalBlue,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = obs,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = disclaimerText,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            )
        }
    }
}
