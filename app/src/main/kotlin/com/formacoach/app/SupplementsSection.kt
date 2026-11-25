package com.formacoach.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.formacoach.model.Supplement
import java.time.LocalDate

/** Name-only shortcuts; Forma never suggests a dose. Users may type their own dose label. */
private data class SupplementPreset(val nameEn: String, val nameAr: String)

private val defaultSupplementPresets = listOf(
    SupplementPreset("Creatine", "كرياتين"),
    SupplementPreset("Vitamin D3", "فيتامين د٣"),
    SupplementPreset("Omega-3", "أوميجا ٣"),
    SupplementPreset("Magnesium", "ماغنيسيوم"),
    SupplementPreset("Multivitamin", "فيتامينات متعددة"),
    SupplementPreset("Electrolytes", "إلكترولايت"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SupplementsVitaminsCard(app: AppState, vm: FitnessViewModel, date: LocalDate) {
    val arabic = LocalConfiguration.current.locales[0].language == "ar"
    // Stored as typed records in the encrypted database and included in backup.
    val items = app.snapshot.supplements
    var expanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    val takenToday = app.snapshot.supplementIntakes.filter { it.date == date }.map { it.supplementId }.toSet()
    val takenCount = items.count { it.id in takenToday }

    InfoCard(Modifier.testTag("supplements_card")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Medication, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            Text(
                stringResource(R.string.supplements_title),
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            SectionInfoButton(InfoTopic.SUPPLEMENTS)
            if (items.isNotEmpty()) {
                PillBadge(
                    text = "${number(takenCount.toDouble(), 0)} / ${number(items.size.toDouble(), 0)}",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            TextButton(onClick = { expanded = !expanded }) {
                Text(stringResource(if (expanded) R.string.supplements_hide else R.string.supplements_manage))
            }
        }

        if (!expanded && items.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items.forEach { item ->
                    val checked = item.id in takenToday
                    FilterChip(
                        selected = checked,
                        onClick = { vm.setSupplementTaken(date, item.id, !checked) },
                        enabled = !app.busy,
                        leadingIcon = {
                            Icon(
                                if (checked) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        label = { Text(if (item.dose.isBlank()) item.name else "${item.name} · ${item.dose}") },
                    )
                }
            }
        } else if (expanded && items.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { item ->
                    val checked = item.id in takenToday
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (checked) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, if (checked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .toggleable(value = checked, enabled = !app.busy, role = Role.Checkbox) { nowChecked ->
                                vm.setSupplementTaken(date, item.id, nowChecked)
                            },
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                if (checked) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                contentDescription = null,
                                tint = if (checked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                if (item.dose.isNotBlank()) {
                                    Text(item.dose, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            ConfirmButton(
                                R.string.remove,
                                stringResource(R.string.supplement_delete_note),
                                !app.busy,
                                tag = "remove_supplement_${item.id}",
                            ) { vm.deleteSupplement(item.id) }
                        }
                    }
                }
            }
        }

        if (expanded) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                defaultSupplementPresets.forEach { preset ->
                    val label = if (arabic) preset.nameAr else preset.nameEn
                    val alreadyAdded = items.any { it.name.equals(label, ignoreCase = true) }
                    if (!alreadyAdded) {
                        AssistChip(
                            onClick = { vm.saveSupplement(Supplement(name = label)) },
                            enabled = !app.busy && items.size < MAX_SUPPLEMENTS,
                            leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) },
                            label = { Text(label) },
                        )
                    }
                }
                FilledTonalButton(
                    onClick = { showAddDialog = true },
                    enabled = !app.busy && items.size < MAX_SUPPLEMENTS,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.supplements_add_custom))
                }
            }
            Text(
                stringResource(R.string.supplements_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var dose by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.supplements_add_custom)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.supplements_name_label)) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = dose,
                        onValueChange = { dose = it },
                        label = { Text(stringResource(R.string.supplements_dose_label)) },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = name.trim().isNotEmpty() && !app.busy,
                    onClick = {
                        vm.saveSupplement(Supplement(name = name.trim().take(60), dose = dose.trim().take(30))) { showAddDialog = false }
                    },
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

/** Keeps the check-off list manageable; nothing already saved is ever trimmed. */
private const val MAX_SUPPLEMENTS = 40
