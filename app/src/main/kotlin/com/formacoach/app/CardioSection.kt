package com.formacoach.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.formacoach.model.*
import java.time.LocalDate

private const val METERS_PER_MILE = 1609.344

/**
 * Timed activities (runs, rides, rows, holds) the user enters by hand. Totals and pace are arithmetic on
 * those entries only; Forma estimates no calories or intensity and treats unlogged days as unknown.
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun CardioSection(app: AppState, vm: FitnessViewModel, date: LocalDate, today: LocalDate) {
    val miles = LocalWeightUnit.current == WeightUnit.LB
    val entries = app.snapshot.cardio
    var logging by remember { mutableStateOf(false) }
    val week = entries.filter { it.date in today.minusDays(6)..today }
    val recent = entries.sortedWith(compareByDescending<CardioEntry> { it.date }).take(5)

    InfoCard(Modifier.testTag("cardio_card")) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.cardio_title), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            SectionInfoButton(InfoTopic.CARDIO)
        }
        if (week.isNotEmpty()) {
            Text(
                stringResource(R.string.cardio_week_summary, number(week.size.toDouble(), 0), durationText(week.sumOf { it.durationSeconds })),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("cardio_week_summary"),
            )
        }
        if (recent.isEmpty()) {
            Text(stringResource(R.string.cardio_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recent.forEach { entry -> CardioRow(entry, miles, app.busy) { vm.removeCardio(entry.id) } }
            }
        }
        FilledTonalButton(
            onClick = { logging = true },
            enabled = !app.busy,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("log_cardio"),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.cardio_log), fontWeight = FontWeight.SemiBold)
        }
    }

    if (logging) CardioDialog(date, miles, app.busy, onDismiss = { logging = false }) { vm.saveCardio(it) { logging = false } }
}

@Composable
private fun CardioRow(entry: CardioEntry, miles: Boolean, busy: Boolean, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    entry.note.ifBlank { stringResource(cardioLabel(entry.activity)) },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                val parts = buildList {
                    add(dateLabel(entry.date))
                    if (entry.note.isNotBlank()) add(stringResource(cardioLabel(entry.activity)))
                    add(durationText(entry.durationSeconds))
                    entry.distanceMeters?.let { add(distanceText(it, miles)) }
                    paceText(entry, miles)?.let { add(it) }
                }
                Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ConfirmButton(R.string.remove, stringResource(R.string.cardio_remove_note), !busy, tag = "remove_cardio_${entry.id}", onConfirm = onRemove)
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun CardioDialog(date: LocalDate, miles: Boolean, busy: Boolean, onDismiss: () -> Unit, onSave: (CardioEntry) -> Unit) {
    var activity by remember { mutableStateOf(CardioActivity.RUN) }
    var minutesText by remember { mutableStateOf("") }
    var secondsText by remember { mutableStateOf("") }
    var distanceText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val minutes = if (minutesText.isBlank()) 0 else NumericInput.integer(minutesText)?.takeIf { it in 0..1440 }
    val seconds = if (secondsText.isBlank()) 0 else NumericInput.integer(secondsText)?.takeIf { it in 0..59 }
    val total = if (minutes != null && seconds != null) minutes * 60 + seconds else null
    val durationOk = total != null && total in 1..86_400
    val distanceAllowed = activity != CardioActivity.HOLD
    val distance = if (!distanceAllowed || distanceText.isBlank()) null else NumericInput.decimal(distanceText)
    val meters = distance?.let { if (miles) it * METERS_PER_MILE else it * 1000 }
    val distanceOk = !distanceAllowed || distanceText.isBlank() || (meters != null && meters > 0 && meters <= 1_000_000)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cardio_log), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.cardio_date, dateLabel(date)), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CardioActivity.entries.forEach { option ->
                        FilterChip(
                            selected = activity == option,
                            onClick = { activity = option },
                            label = { Text(stringResource(cardioLabel(option))) },
                            modifier = Modifier.testTag("cardio_activity_${option.name}"),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { minutesText = it.take(4) },
                        label = { Text(stringResource(R.string.cardio_minutes)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = minutes == null,
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("cardio_minutes"),
                    )
                    OutlinedTextField(
                        value = secondsText,
                        onValueChange = { secondsText = it.take(2) },
                        label = { Text(stringResource(R.string.cardio_seconds)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = seconds == null,
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("cardio_seconds"),
                    )
                }
                if (distanceAllowed) {
                    OutlinedTextField(
                        value = distanceText,
                        onValueChange = { distanceText = it.take(8) },
                        label = { Text(stringResource(if (miles) R.string.cardio_distance_mi else R.string.cardio_distance_km)) },
                        isError = !distanceOk,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("cardio_distance"),
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(60) },
                    label = { Text(stringResource(R.string.cardio_note)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("cardio_note"),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = durationOk && distanceOk && !busy,
                onClick = {
                    if (total != null) onSave(CardioEntry(date = date, activity = activity, durationSeconds = total, distanceMeters = meters, note = note.trim()))
                },
                modifier = Modifier.testTag("save_cardio"),
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

internal fun cardioLabel(activity: CardioActivity): Int = when (activity) {
    CardioActivity.WALK -> R.string.cardio_walk
    CardioActivity.RUN -> R.string.cardio_run
    CardioActivity.CYCLE -> R.string.cardio_cycle
    CardioActivity.ROW -> R.string.cardio_row
    CardioActivity.SWIM -> R.string.cardio_swim
    CardioActivity.ELLIPTICAL -> R.string.cardio_elliptical
    CardioActivity.STAIRS -> R.string.cardio_stairs
    CardioActivity.JUMP_ROPE -> R.string.cardio_jump_rope
    CardioActivity.HOLD -> R.string.cardio_hold
    CardioActivity.OTHER -> R.string.cardio_other
}

@Composable
internal fun durationText(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val h = number(hours.toDouble(), 0)
    val m = number(minutes.toDouble(), 0)
    val s = number(seconds.toDouble(), 0)
    return when {
        hours > 0 -> stringResource(R.string.cardio_duration_hm, h, m)
        seconds == 0 -> stringResource(R.string.cardio_duration_m, m)
        minutes == 0 -> stringResource(R.string.cardio_duration_s, s)
        else -> stringResource(R.string.cardio_duration_ms, m, s)
    }
}

@Composable
private fun distanceText(meters: Double, miles: Boolean): String =
    stringResource(if (miles) R.string.cardio_mi else R.string.cardio_km, number(if (miles) meters / METERS_PER_MILE else meters / 1000, 2))

/** Pace is plain division of the user's own time by distance, shown only for walks and runs. */
@Composable
private fun paceText(entry: CardioEntry, miles: Boolean): String? {
    val meters = entry.distanceMeters ?: return null
    if (entry.activity != CardioActivity.RUN && entry.activity != CardioActivity.WALK) return null
    val units = if (miles) meters / METERS_PER_MILE else meters / 1000
    if (units < 0.1) return null
    val perUnit = (entry.durationSeconds / units).toInt()
    if (perUnit !in 1..7200) return null
    val pace = "${number((perUnit / 60).toDouble(), 0)}:${number((perUnit % 60).toDouble(), 0).padStart(2, number(0.0, 0).first())}"
    return stringResource(if (miles) R.string.cardio_pace_mi else R.string.cardio_pace_km, pace)
}
