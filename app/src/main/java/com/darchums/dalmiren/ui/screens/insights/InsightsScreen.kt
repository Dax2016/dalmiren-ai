package com.darchums.dalmiren.ui.screens.insights

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darchums.dalmiren.domain.model.ParentReflection
import com.darchums.dalmiren.domain.model.ParentingCategory
import com.darchums.dalmiren.ui.components.ChipSelector
import com.darchums.dalmiren.ui.components.DalMirenButton
import com.darchums.dalmiren.ui.components.DalMirenCard
import com.darchums.dalmiren.ui.components.DalMirenHeader
import com.darchums.dalmiren.ui.components.SafetyDisclaimerBanner
import com.darchums.dalmiren.ui.theme.ChampagneGold
import com.darchums.dalmiren.ui.theme.RoyalBlue

@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel,
    modifier: Modifier = Modifier
) {
    val reflections by viewModel.reflections.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        DalMirenHeader(subtitle = "Progress Insights & Parent Reflections")

        Column(modifier = Modifier.padding(16.dp)) {
            SafetyDisclaimerBanner(text = "Insights reflect strictly your recorded reflections and guidance history. DALMIREN AI makes no medical or clinical assumptions about a child.")
            Spacer(modifier = Modifier.height(16.dp))

            // Action & Stats Summary
            DalMirenCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Parent Reflection Log",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "${reflections.size} Recorded Moments",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        if (reflections.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearHistory() }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear Reflections",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    DalMirenButton(
                        text = "Add Parent Reflection",
                        onClick = { showAddDialog = true },
                        icon = Icons.Default.Add
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Reflection List
            if (reflections.isEmpty()) {
                DalMirenCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = null,
                            tint = ChampagneGold,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Reflections Added Yet",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Add Parent Reflection' above to log your daily parenting observations, wins, or feelings.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            } else {
                reflections.forEach { reflection ->
                    ReflectionCardItem(
                        reflection = reflection,
                        onDelete = { viewModel.deleteReflection(reflection.id) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showAddDialog) {
        AddReflectionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { category, title, notes, rating ->
                viewModel.addReflection(category, title, notes, rating)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ReflectionCardItem(
    reflection: ParentReflection,
    onDelete: () -> Unit
) {
    DalMirenCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reflection.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = reflection.category.title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = RoyalBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = reflection.notes,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Connection Rating: ",
                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                    text = "★".repeat(reflection.feelingRating) + "☆".repeat(5 - reflection.feelingRating),
                    style = MaterialTheme.typography.labelLarge.copy(color = ChampagneGold)
                )
            }
        }
    }
}

@Composable
private fun AddReflectionDialog(
    onDismiss: () -> Unit,
    onSave: (ParentingCategory, String, String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ParentingCategory.BEHAVIOUR) }
    var rating by remember { mutableFloatStateOf(4f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Parent Reflection",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = RoyalBlue)
                )
                Spacer(modifier = Modifier.height(4.dp))
                ChipSelector(
                    options = ParentingCategory.entries.map { it.title },
                    selectedOption = selectedCategory.title,
                    onOptionSelected = { label ->
                        val cat = ParentingCategory.entries.find { it.title == label }
                        if (cat != null) selectedCategory = cat
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Reflection Title") },
                    placeholder = { Text("e.g. Bedtime co-regulation success") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Observations") },
                    placeholder = { Text("What strategy did you try? How did your child respond?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Parent-Child Connection Rating: ${rating.toInt()}/5",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Slider(
                    value = rating,
                    onValueChange = { rating = it },
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = RoyalBlue,
                        activeTrackColor = RoyalBlue
                    )
                )
            }
        },
        confirmButton = {
            DalMirenButton(
                text = "Save Reflection",
                onClick = {
                    if (title.isNotBlank() && notes.isNotBlank()) {
                        onSave(selectedCategory, title, notes, rating.toInt())
                    }
                },
                enabled = title.isNotBlank() && notes.isNotBlank(),
                modifier = Modifier.width(160.dp)
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
