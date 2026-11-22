package com.formacoach.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.formacoach.coach.HistoryPoint
import com.formacoach.model.Exercise
import java.time.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateChoice(date: LocalDate, today: LocalDate, onChange: (LocalDate) -> Unit, tag: String = "date_choice") {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = { open = true },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.testTag(tag),
    ) {
        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.selected_date, dateLabel(date)), fontWeight = FontWeight.Medium)
    }
    if (open) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            yearRange = 1970..today.year,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate() <= today
            },
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        picker.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                        open = false
                    },
                    enabled = picker.selectedDateMillis != null,
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(picker) }
    }
}

@Composable
internal fun ConfirmButton(label: Int, description: String, enabled: Boolean = true, tag: String = "", onConfirm: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    TextButton(
        onClick = { open = true },
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)),
        modifier = Modifier.testTag(tag),
    ) { Text(stringResource(label)) }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(stringResource(label), fontWeight = FontWeight.Bold) },
            text = { Text(description) },
            confirmButton = {
                TextButton(onClick = { open = false; onConfirm() }) {
                    Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
internal fun ExercisePicker(selected: Exercise, onChange: (Exercise) -> Unit, choices: List<Exercise> = Exercise.entries) {
    var expanded by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = { expanded = true },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("exercise_picker"),
    ) {
        Text(
            stringResource(exerciseLabel(selected)),
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
    }
    if (expanded) {
        var search by remember { mutableStateOf("") }
        val labels = choices.associateWith { stringResource(exerciseLabel(it)) }
        AlertDialog(
            onDismissRequest = { expanded = false },
            title = { Text(stringResource(R.string.exercise_library), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Input(R.string.search_exercises, search, { search = it }, "picker_exercise_search", numeric = false)
                    Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        choices.filter {
                            com.formacoach.coach.ExerciseSearch.matches(search, labels.getValue(it) + " " + com.formacoach.model.ExerciseCatalog.searchText(it))
                        }.forEach { value ->
                            val isSelected = value == selected
                            Surface(
                                onClick = { onChange(value); expanded = false },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                modifier = Modifier.fillMaxWidth().testTag("exercise_${value.name}"),
                            ) {
                                Text(
                                    labels.getValue(value),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { expanded = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
internal fun HistoryChart(points: List<HistoryPoint>, unit: String) {
    if (points.isEmpty()) {
        Text(stringResource(R.string.no_chart_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val min = points.minOf { it.value }
    val max = points.maxOf { it.value }
    val latest = points.last().value
    val start = points.first().date
    val end = points.last().date
    val description = stringResource(
        R.string.chart_description,
        number(points.size.toDouble(), 0),
        dateLabel(start),
        dateLabel(end),
        number(min),
        number(max),
        unit,
    )
    val lineColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                stringResource(R.string.chart_range, number(min), number(max), unit),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PillBadge("${number(latest)} $unit")
                ScopeInfoButton(R.string.progress_title, R.string.chart_note)
            }
        }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Canvas(Modifier.fillMaxWidth().height(168.dp).padding(horizontal = 6.dp, vertical = 8.dp).semantics { contentDescription = description }) {
                val pad = 14.dp.toPx()
                val width = (size.width - 2 * pad).coerceAtLeast(1f)
                val height = (size.height - 2 * pad).coerceAtLeast(1f)
                // Horizontal reference gridlines
                listOf(0f, 0.5f, 1f).forEach { frac ->
                    val y = pad + height * frac
                    drawLine(gridColor, Offset(pad, y), Offset(pad + width, y), 1.dp.toPx())
                }
                val days = (end.toEpochDay() - start.toEpochDay()).coerceAtLeast(1)
                val span = (max - min).takeIf { it > 0 } ?: 1.0
                val coords = points.map { point ->
                    Offset(
                        pad + ((point.date.toEpochDay() - start.toEpochDay()).toDouble() / days * width).toFloat(),
                        pad + height - ((if (max == min) 0.5 else (point.value - min) / span) * height).toFloat(),
                    )
                }
                if (coords.size >= 2) {
                    val areaPath = Path().apply {
                        moveTo(coords.first().x, pad + height)
                        coords.forEach { lineTo(it.x, it.y) }
                        lineTo(coords.last().x, pad + height)
                        close()
                    }
                    drawPath(
                        areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.0f)),
                            startY = pad,
                            endY = pad + height,
                        ),
                    )
                    val linePath = Path().apply {
                        coords.forEachIndexed { index, offset ->
                            if (index == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
                        }
                    }
                    drawPath(
                        linePath,
                        color = lineColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
                coords.forEachIndexed { index, offset ->
                    val isLast = index == coords.lastIndex
                    if (isLast) {
                        drawCircle(lineColor.copy(alpha = 0.25f), 9.dp.toPx(), offset)
                    }
                    drawCircle(surfaceColor, 5.dp.toPx(), offset)
                    drawCircle(lineColor, if (isLast) 4.5.dp.toPx() else 3.5.dp.toPx(), offset)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(dateLabel(start), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(dateLabel(end), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
