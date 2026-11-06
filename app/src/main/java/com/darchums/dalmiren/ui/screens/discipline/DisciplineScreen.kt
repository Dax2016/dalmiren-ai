package com.darchums.dalmiren.ui.screens.discipline

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
import androidx.compose.material.icons.filled.ShieldMoon
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
fun DisciplineScreen(
    viewModel: DisciplineViewModel,
    modifier: Modifier = Modifier
) {
    val selectedAgeGroup by viewModel.selectedAgeGroup.collectAsState()
    val conflictType by viewModel.conflictType.collectAsState()
    val result by viewModel.guidanceResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        DalMirenHeader(subtitle = "Positive Discipline & Constructive Boundaries")

        Column(modifier = Modifier.padding(16.dp)) {
            SafetyDisclaimerBanner()
            Spacer(modifier = Modifier.height(16.dp))

            DalMirenCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Constructive Boundaries & De-escalation",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Set firm, warm limits and logical consequences without shouting, humiliation, or threats.",
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
                        options = AgeGroup.entries.map { it.label },
                        selectedOption = selectedAgeGroup.label,
                        onOptionSelected = { label ->
                            val match = AgeGroup.entries.find { it.label == label }
                            if (match != null) viewModel.selectedAgeGroup.value = match
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Conflict Type / Challenge (Optional)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = RoyalBlue
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = conflictType,
                        onValueChange = { viewModel.conflictType.value = it },
                        placeholder = { Text("e.g. Throwing toys, refusal to do homework, curfew disputes...") },
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
                        text = if (isLoading) "Generating Strategy..." else "Get Discipline Strategy",
                        onClick = { viewModel.fetchDisciplineStrategy() },
                        enabled = !isLoading,
                        icon = Icons.Default.ShieldMoon
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
