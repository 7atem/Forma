package com.formacoach.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.formacoach.coach.ProgressHighlights
import com.formacoach.model.FitnessSnapshot
import com.formacoach.model.WeightUnits
import java.time.LocalDate

/**
 * Top of Progress: what the last 7 days of logs show, next to the person's own targets. Unlogged days are unknown,
 * so food uses logged days only and nothing here judges adherence or recommends changes.
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun ProgressHighlightsCard(snapshot: FitnessSnapshot, today: LocalDate, onLogMeasurement: () -> Unit) {
    val h = remember(snapshot, today) { ProgressHighlights.of(snapshot, today) }
    val unit = LocalWeightUnit.current
    val unitStr = weightUnitLabel()
    Surface(
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth().testTag("progress_highlights"),
    ) {
        Column(
            Modifier
                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f), MaterialTheme.colorScheme.surface)))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.highlights_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                SectionInfoButton(InfoTopic.PROGRESS_HIGHLIGHTS)
            }
            if (h.isEmpty) {
                Text(stringResource(R.string.highlights_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val tiles = buildList<@Composable (Modifier) -> Unit> {
                    add { m ->
                        HighlightTile(
                            Icons.Default.FitnessCenter,
                            h.plannedSessions?.let { plan -> ltr("${number(h.workouts.toDouble(), 0)} / ${number(plan.toDouble(), 0)}") } ?: number(h.workouts.toDouble(), 0),
                            stringResource(R.string.highlights_workouts),
                            m,
                        )
                    }
                    add { m ->
                        HighlightTile(
                            Icons.AutoMirrored.Filled.DirectionsRun,
                            if (h.cardioEntries == 0) "—" else durationText(h.cardioSeconds),
                            stringResource(R.string.highlights_cardio),
                            m,
                        )
                    }
                    add { m ->
                        val avg = h.averageKcalOnLoggedDays
                        HighlightTile(
                            Icons.Default.Restaurant,
                            avg?.let { number(it, 0) } ?: "—",
                            if (avg == null) stringResource(R.string.highlights_food_none)
                            else h.calorieTarget?.let { target -> stringResource(R.string.highlights_food_target, number(h.foodDays.toDouble(), 0), number(target.toDouble(), 0)) }
                            ?: stringResource(R.string.highlights_food, number(h.foodDays.toDouble(), 0)),
                            m,
                        )
                    }
                    add { m ->
                        val change = h.weightChange30Kg
                        val latest = h.latestWeightKg
                        HighlightTile(
                            Icons.Default.MonitorWeight,
                            when {
                                change != null -> ltr("${if (change > 0) "+" else ""}${number(WeightUnits.fromKg(change, unit), 1)} $unitStr")
                                latest != null -> "${number(WeightUnits.fromKg(latest, unit), 1)} $unitStr"
                                else -> "—"
                            },
                            stringResource(if (change != null) R.string.highlights_weight_change else R.string.highlights_weight_latest),
                            m,
                        )
                    }
                }
                tiles.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { tile -> tile(Modifier.weight(1f).fillMaxHeight()) }
                    }
                }
                if (h.newRecords.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.testTag("highlights_records")) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text(
                                stringResource(R.string.highlights_records, h.newRecords.take(3).map { stringResource(exerciseLabel(it)) }.joinToString(", ")) +
                                    if (h.newRecords.size > 3) " " + stringResource(R.string.highlights_records_more, number((h.newRecords.size - 3).toDouble(), 0)) else "",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }
            FilledTonalButton(
                onClick = onLogMeasurement,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("log_measurement"),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.log_measurement), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun HighlightTile(icon: ImageVector, value: String, label: String, modifier: Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.clearAndSetSemantics { contentDescription = "$label: $value" },
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Keeps "3 / 4" and "+1.5 kg" in reading order inside right-to-left text (left-to-right isolate). */
private fun ltr(text: String): String = Char(0x2066) + text + Char(0x2069)
