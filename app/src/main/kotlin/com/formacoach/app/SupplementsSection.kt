package com.formacoach.app

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.UUID

@Serializable
internal data class SupplementEntry(
    val id: String,
    val name: String,
    val dose: String = "",
)

@Serializable
internal data class SupplementsState(
    val items: List<SupplementEntry> = emptyList(),
    val takenByDate: Map<String, List<String>> = emptyMap(),
)

internal object SupplementsStore {
    private const val PREFS = "forma_supplements_prefs"
    private const val KEY = "state_v1"
    private val json = Json { ignoreUnknownKeys = true }

    fun load(context: Context): SupplementsState {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return SupplementsState()
        return runCatching { json.decodeFromString<SupplementsState>(raw) }.getOrDefault(SupplementsState())
    }

    fun save(context: Context, state: SupplementsState) {
        // Keep last 90 days of check-offs bounded
        val boundedDates = state.takenByDate.entries.sortedByDescending { it.key }.take(90).associate { it.key to it.value }
        val clean = state.copy(items = state.items.take(40), takenByDate = boundedDates)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, json.encodeToString(clean))
            .apply()
    }
}

private data class SupplementPreset(val nameEn: String, val nameAr: String, val dose: String)

private val defaultSupplementPresets = listOf(
    SupplementPreset("Creatine", "كرياتين", "5 g"),
    SupplementPreset("Vitamin D3", "فيتامين د٣", "2000 IU"),
    SupplementPreset("Omega-3", "أوميجا ٣", "1000 mg"),
    SupplementPreset("Magnesium", "ماغنيسيوم", "300 mg"),
    SupplementPreset("Multivitamin", "فيتامينات متعددة", "1 cap"),
    SupplementPreset("Electrolytes", "إلكترولايت", "1 scoop"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SupplementsVitaminsCard(date: LocalDate) {
    val context = LocalContext.current
    val arabic = LocalConfiguration.current.locales[0].language == "ar"
    val dateKey = date.toString()
    var state by remember { mutableStateOf(SupplementsStore.load(context)) }
    var expanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    fun update(next: SupplementsState) {
        state = next
        SupplementsStore.save(context, next)
    }

    val takenToday = state.takenByDate[dateKey].orEmpty().toSet()
    val takenCount = state.items.count { it.id in takenToday }

    InfoCard(Modifier.testTag("supplements_card")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Medication, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            Text(
                stringResource(R.string.supplements_title),
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (state.items.isNotEmpty()) {
                PillBadge(
                    text = "${number(takenCount.toDouble(), 0)} / ${number(state.items.size.toDouble(), 0)}",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            TextButton(onClick = { expanded = !expanded }) {
                Text(stringResource(if (expanded) R.string.supplements_hide else R.string.supplements_manage))
            }
        }

        if (!expanded && state.items.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.items.forEach { item ->
                    val checked = item.id in takenToday
                    FilterChip(
                        selected = checked,
                        onClick = {
                            val nextSet = if (!checked) takenToday + item.id else takenToday - item.id
                            update(state.copy(takenByDate = state.takenByDate + (dateKey to nextSet.toList())))
                        },
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
        } else if (expanded && state.items.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.items.forEach { item ->
                    val checked = item.id in takenToday
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (checked) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, if (checked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .toggleable(value = checked, role = Role.Checkbox) { nowChecked ->
                                val nextSet = if (nowChecked) takenToday + item.id else takenToday - item.id
                                update(state.copy(takenByDate = state.takenByDate + (dateKey to nextSet.toList())))
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
                            IconButton(
                                onClick = {
                                    val nextItems = state.items.filterNot { it.id == item.id }
                                    val nextDates = state.takenByDate.mapValues { (_, list) -> list.filterNot { it == item.id } }
                                    update(state.copy(items = nextItems, takenByDate = nextDates))
                                },
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        if (expanded) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                defaultSupplementPresets.forEach { preset ->
                    val label = if (arabic) preset.nameAr else preset.nameEn
                    val alreadyAdded = state.items.any { it.name.equals(label, ignoreCase = true) }
                    if (!alreadyAdded) {
                        AssistChip(
                            onClick = {
                                update(state.copy(items = state.items + SupplementEntry(UUID.randomUUID().toString(), label, preset.dose)))
                            },
                            leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) },
                            label = { Text(label) },
                        )
                    }
                }
                FilledTonalButton(
                    onClick = { showAddDialog = true },
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
                    enabled = name.trim().isNotEmpty(),
                    onClick = {
                        update(state.copy(items = state.items + SupplementEntry(UUID.randomUUID().toString(), name.trim().take(60), dose.trim().take(30))))
                        showAddDialog = false
                    },
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
