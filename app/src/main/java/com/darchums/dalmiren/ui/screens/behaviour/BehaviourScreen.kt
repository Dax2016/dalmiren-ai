package com.darchums.dalmiren.ui.screens.behaviour

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
import androidx.compose.material.icons.filled.Psychology
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
fun BehaviourScreen(
    viewModel: BehaviourViewModel,
    modifier: Modifier = Modifier
) {
    val description by viewModel.description.collectAsState()
    val selectedAgeGroup by viewModel.selectedAgeGroup.collectAsState()
    val triggers by viewModel.triggers.collectAsState()
    val contextDetails by viewModel.contextDetails.collectAsState()
    val result by viewModel.guidanceResult.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        DalMirenHeader(subtitle = "Behaviour Intelligence & Analysis")

        Column(modifier = Modifier.padding(16.dp)) {
            SafetyDisclaimerBanner()
            Spacer(modifier = Modifier.height(16.dp))

            DalMirenCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Record Situation Details",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Child's Age Group",
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
                            if (match != null) {
                                viewModel.selectedAgeGroup.value = match
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Describe What Happened",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = RoyalBlue
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { viewModel.description.value = it },
                        placeholder = { Text("e.g. My child refused to put toys away and threw blocks when asked to get ready for bedtime...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Possible Triggers (Optional)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = triggers,
                        onValueChange = { viewModel.triggers.value = it },
                        placeholder = { Text("e.g. Tiredness, sibling conflict, sudden change in plans...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Context & Environment (Optional)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = contextDetails,
                        onValueChange = { viewModel.contextDetails.value = it },
                        placeholder = { Text("e.g. Public store, bedtime routine, after school...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    DalMirenButton(
                        text = if (isAnalyzing) "Analyzing Situation..." else "Analyze Behaviour",
                        onClick = { viewModel.analyzeSituation() },
                        enabled = description.isNotBlank() && !isAnalyzing,
                        icon = Icons.Default.Psychology
                    )
                }
            }

            if (isAnalyzing) {
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = RoyalBlue)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Formulating hypotheses & calm response strategies...",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
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
