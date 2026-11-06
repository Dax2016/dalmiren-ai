package com.darchums.dalmiren.ui.screens.emotional

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.darchums.dalmiren.domain.model.AgeGroup
import com.darchums.dalmiren.ui.components.ChipSelector
import com.darchums.dalmiren.ui.components.DalMirenButton
import com.darchums.dalmiren.ui.components.DalMirenCard
import com.darchums.dalmiren.ui.components.DalMirenHeader
import com.darchums.dalmiren.ui.components.GuidanceCard
import com.darchums.dalmiren.ui.components.SafetyDisclaimerBanner
import com.darchums.dalmiren.ui.theme.RoyalBlue

@Composable
fun EmotionalScreen(
    viewModel: EmotionalViewModel,
    modifier: Modifier = Modifier
) {
    val selectedAgeGroup by viewModel.selectedAgeGroup.collectAsState()
    val emotionalContext by viewModel.emotionalContext.collectAsState()
    val result by viewModel.guidanceResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        DalMirenHeader(subtitle = "Emotional Intelligence & Co-regulation")

        Column(modifier = Modifier.padding(16.dp)) {
            SafetyDisclaimerBanner()
            Spacer(modifier = Modifier.height(16.dp))

            DalMirenCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Emotional Coaching & Needs",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Help your child label feelings, validate emotional distress, and practice peaceful co-regulation.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Select Age Group",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = RoyalBlue
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ChipSelector(
                        options = AgeGroup.values().map { it.label },
                        selectedOption = selectedAgeGroup.label,
                        onOptionSelected = { label ->
                            val match = AgeGroup.values().find { it.label == label }
                            if (match != null) viewModel.selectedAgeGroup.value = match
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Emotional Trigger / Situation (Optional)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = RoyalBlue
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = emotionalContext,
                        onValueChange = { viewModel.emotionalContext.value = it },
                        placeholder = { Text("e.g. Meltdown when leaving the playground, fear of sleeping alone...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    DalMirenButton(
                        text = if (isLoading) "Generating Coaching..." else "Get Emotional Coaching",
                        onClick = { viewModel.fetchEmotionalGuidance() },
                        enabled = !isLoading,
                        icon = Icons.Default.FavoriteBorder
                    )
                }
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = RoyalBlue)
                }
            }

            if (result != null) {
                Spacer(modifier = Modifier.height(24.dp))
                GuidanceCard(
                    title = result!!.title,
                    hypotheses = result!!.hypotheses,
                    possibleFeelings = result!!.emotionalNeeds,
                    calmResponse = result!!.calmResponse,
                    nextObservations = result!!.nextObservations
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
