package com.darchums.dalmiren.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darchums.dalmiren.ui.components.DalMirenButton
import com.darchums.dalmiren.ui.components.DalMirenCard
import com.darchums.dalmiren.ui.theme.ChampagneGold
import com.darchums.dalmiren.ui.theme.Ivory
import com.darchums.dalmiren.ui.theme.MidnightNavy
import com.darchums.dalmiren.ui.theme.RoyalBlue

@Composable
fun OnboardingScreen(
    onGetStartedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MidnightNavy
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(36.dp))
                Box(
                    modifier = Modifier
                        .size(90.dp)
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
                        contentDescription = "DALMIREN Logo",
                        tint = Ivory,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "DALMIREN AI",
                    style = MaterialTheme.typography.displayMedium.copy(
                        color = Ivory,
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 2.sp
                    )
                )

                Text(
                    text = "BY DARCHUMS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = ChampagneGold,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Understand. Guide. Nurture.",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Ivory.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Empowering parents with non-judgmental, evidence-informed behavioral and emotional guidance for every stage of child growth.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Ivory.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Core Principles Cards
                PrincipleItem(
                    icon = Icons.Default.Favorite,
                    title = "Empathy & Co-regulation",
                    description = "Focus on connection before correction. Understand your child's feelings and emotional needs."
                )
                Spacer(modifier = Modifier.height(12.dp))
                PrincipleItem(
                    icon = Icons.Default.Shield,
                    title = "Positive Discipline",
                    description = "Constructive boundaries and alternatives to threats, shouting, or forced obedience."
                )
                Spacer(modifier = Modifier.height(12.dp))
                PrincipleItem(
                    icon = Icons.Default.Lock,
                    title = "Privacy & Safety First",
                    description = "Non-diagnostic guidance stored securely with full user data controls."
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp)
            ) {
                DalMirenButton(
                    text = "Get Started",
                    onClick = onGetStartedClick,
                    icon = Icons.Default.ArrowForward
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "By continuing, you acknowledge that DALMIREN AI provides educational support and is not a clinical diagnostic service.",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Ivory.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PrincipleItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    DalMirenCard(
        backgroundColor = MidnightNavy.copy(alpha = 0.8f),
        borderColor = ChampagneGold.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ChampagneGold.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ChampagneGold,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Ivory,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Ivory.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                )
            }
        }
    }
}
