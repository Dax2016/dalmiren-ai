package com.darchums.dalmiren.ui.screens.subscription

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.darchums.dalmiren.data.repository.PlayBillingManager
import com.darchums.dalmiren.data.repository.PremiumPlanOption
import com.darchums.dalmiren.ui.theme.ChampagneGold
import com.darchums.dalmiren.ui.theme.Ivory
import com.darchums.dalmiren.ui.theme.MidnightNavy
import com.darchums.dalmiren.ui.theme.RoyalBlue

@Composable
fun PremiumScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val billingManager = remember(context) { PlayBillingManager(context) }
    val billingState by billingManager.state.collectAsState()

    DisposableEffect(billingManager) {
        billingManager.refresh()
        onDispose { billingManager.close() }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Back to Settings",
                color = RoyalBlue,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(vertical = 4.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightNavy)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ChampagneGold
                        )
                        Text(
                            text = "  DALMIREN AI PREMIUM",
                            color = ChampagneGold,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Go deeper in every parenting moment.",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "More insight, stronger continuity and personalised guidance as your family grows.",
                        color = Ivory,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (billingState.tier.equals("PREMIUM", true)) {
                            "Your account: PREMIUM"
                        } else {
                            "Your account: FREE"
                        },
                        color = ChampagneGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.65f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Included with Premium",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MidnightNavy
                    )
                    PremiumFeature("Higher AI guidance usage across all six modules")
                    PremiumFeature("Deeper behaviour and emotional pattern insights")
                    PremiumFeature("Extended Memory Nexus and conversation history")
                    PremiumFeature("Multiple child profiles")
                    PremiumFeature("Advanced progress and pattern reports")
                    PremiumFeature("Personalised action plans and follow-ups")
                    HorizontalDivider(color = ChampagneGold.copy(alpha = 0.5f))
                    Text(
                        text = "Free access remains useful. Safety, safeguarding, crisis support and account security are not Premium-only features.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Choose your plan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (billingState.isLoading && billingState.plans.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = RoyalBlue
                )
                Text(
                    text = "Loading your Google Play offers...",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            billingState.plans.forEach { plan ->
                PremiumPlanCard(
                    plan = plan,
                    enabled = !billingState.isLoading && activity != null,
                    onPurchase = {
                        activity?.let {
                            billingManager.launchPurchase(it, plan.basePlanId)
                        }
                    }
                )
            }

            billingState.message?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            OutlinedButton(
                onClick = { billingManager.restorePurchases() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !billingState.isLoading,
                shape = RoundedCornerShape(14.dp)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null
                )
                Text("  Restore purchases")
            }

            OutlinedButton(
                onClick = { billingManager.refresh() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !billingState.isLoading,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Refresh subscription offers")
            }

            Text(
                text = "Subscriptions are billed through Google Play. The price and any eligible free-trial offer are shown by Google Play before you confirm. Subscriptions renew automatically unless cancelled through Google Play before the renewal date. Trial eligibility is determined by Google Play.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PremiumFeature(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        androidx.compose.material3.Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = RoyalBlue
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun PremiumPlanCard(
    plan: PremiumPlanOption,
    enabled: Boolean,
    onPurchase: () -> Unit
) {
    val isWeekly = plan.basePlanId == "plus-weekly"
    val isMonthly = plan.basePlanId == "plus-monthly"
    val isAnnual = plan.basePlanId == "plus-annual-v1"
    val isFeatured = isAnnual
    val title = when {
        isWeekly -> "Weekly plan"
        isMonthly -> "Monthly plan"
        isAnnual -> "Annual plan"
        else -> "Subscription plan"
    }
    val billingLabel = when {
        isWeekly -> "weekly"
        isMonthly -> "monthly"
        isAnnual -> "annually"
        else -> "per billing period"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isFeatured) ChampagneGold else RoyalBlue.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when {
                        isWeekly -> "WEEKLY"
                        isMonthly -> "MONTHLY"
                        isAnnual -> "YEARLY"
                        else -> ""
                    },
                    color = if (isFeatured) ChampagneGold else RoyalBlue,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = plan.formattedPrice,
                style = MaterialTheme.typography.headlineMedium,
                color = RoyalBlue,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (plan.hasFreeTrial) {
                    "Google Play offer: ${formatTrialPeriod(plan.trialPeriod)} free, then ${plan.formattedPrice} per billing period."
                } else {
                    "Billed $billingLabel through Google Play."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onPurchase,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFeatured) MidnightNavy else RoyalBlue
                )
            ) {
                Text(
                    text = if (plan.hasFreeTrial) {
                        "Start eligible free trial"
                    } else {
                        "Continue with $title"
                    }
                )
            }
        }
    }
}

private fun formatTrialPeriod(period: String?): String {
    if (period.isNullOrBlank()) return "Free trial"
    return when (period.uppercase()) {
        "P7D" -> "7-day"
        "P14D" -> "14-day"
        "P1M" -> "1-month"
        "P3M" -> "3-month"
        else -> period
    }
}
