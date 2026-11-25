package com.formacoach.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formacoach.coach.NutritionTrends
import com.formacoach.coach.TrainingTrends
import com.formacoach.model.FitnessSnapshot
import java.time.LocalDate

/**
 * Logged calories and macros over 7 or 30 days next to the targets the user entered.
 * Days without entries are drawn as outlines and excluded from averages: they are unknown, not zero.
 */
@Composable
internal fun NutritionTrendCard(snapshot: FitnessSnapshot, today: LocalDate) {
    var range by rememberSaveable { mutableIntStateOf(0) }
    val length = if (range == 0) 7 else 30
    val trend = remember(snapshot.meals, snapshot.profile, today, length) { NutritionTrends.range(snapshot, today, length) }
    val calorieTarget = snapshot.profile?.caloriesTarget?.toDouble()
    InfoCard(Modifier.testTag("nutrition_trend_card")) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.nutrition_trends_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            ScopeInfoButton(R.string.nutrition_trends_title, R.string.trend_unlogged_note)
        }
        SegmentedTabs(
            selectedIndex = range,
            labels = listOf(R.string.trend_range_7, R.string.trend_range_30),
            onSelect = { range = it },
            tagPrefix = "nutrition_trend_range",
        )
        Text(
            stringResource(R.string.trend_logged_days, number(trend.loggedDays.toDouble(), 0), number(length.toDouble(), 0)),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        if (trend.loggedDays == 0) {
            Text(stringResource(R.string.trend_no_entries), color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@InfoCard
        }
        val description = stringResource(
            R.string.trend_chart_description,
            dateLabel(trend.days.first().date),
            dateLabel(trend.days.last().date),
            number(trend.loggedDays.toDouble(), 0),
            number(trend.meanCalories ?: 0.0, 0),
        )
        val bar = MaterialTheme.colorScheme.primary
        val outline = MaterialTheme.colorScheme.outline
        val targetColor = MaterialTheme.colorScheme.secondary
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        Canvas(
            Modifier.fillMaxWidth().height(140.dp).testTag("nutrition_trend_chart").clearAndSetSemantics { contentDescription = description },
        ) {
            val maxValue = maxOf(trend.days.maxOf { it.calories }, calorieTarget ?: 0.0, 1.0) * 1.1
            val slot = size.width / trend.days.size
            val barWidth = (slot * 0.62f).coerceAtLeast(2f)
            trend.days.forEachIndexed { i, day ->
                // Oldest on the reading-start side, newest on the reading-end side.
                val index = if (rtl) trend.days.lastIndex - i else i
                val left = index * slot + (slot - barWidth) / 2
                if (day.logged) {
                    val h = (day.calories / maxValue * size.height).toFloat().coerceAtLeast(2f)
                    drawRoundRect(bar, Offset(left, size.height - h), Size(barWidth, h), CornerRadius(4f, 4f))
                } else {
                    // Unknown day: a short dashed outline, never a zero-height "miss".
                    val h = size.height * 0.18f
                    drawRoundRect(outline, Offset(left, size.height - h), Size(barWidth, h), CornerRadius(4f, 4f),
                        style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f))))
                }
            }
            calorieTarget?.let { target ->
                val y = size.height - (target / maxValue * size.height).toFloat()
                drawLine(targetColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Text(dateLabel(trend.days.first().date), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(dateLabel(trend.days.last().date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (calorieTarget != null) {
            Text(stringResource(R.string.trend_target_line, number(calorieTarget, 0)), style = MaterialTheme.typography.labelMedium, color = targetColor)
        }
        FormFieldPair(
            first = { MetricTile(number(trend.meanCalories ?: 0.0, 0), stringResource(R.string.trend_avg_kcal), Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.primary) },
            second = { MetricTile(stringResource(R.string.grams_amount, number(trend.meanProtein ?: 0.0, 0)), stringResource(R.string.trend_avg_protein), Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.secondary) },
        )
        FormFieldPair(
            first = { MetricTile(stringResource(R.string.grams_amount, number(trend.meanCarbs ?: 0.0, 0)), stringResource(R.string.trend_avg_carbs), Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.tertiary) },
            second = { MetricTile(stringResource(R.string.grams_amount, number(trend.meanFat ?: 0.0, 0)), stringResource(R.string.trend_avg_fat), Modifier.fillMaxWidth()) },
        )
        val logged = number(trend.loggedDays.toDouble(), 0)
        trend.daysCaloriesNearTarget?.let {
            Text(stringResource(R.string.trend_calorie_near, number(it.toDouble(), 0), logged), style = MaterialTheme.typography.bodyMedium)
        }
        trend.daysProteinAtTarget?.let {
            Text(stringResource(R.string.trend_protein_met, number(it.toDouble(), 0), logged), style = MaterialTheme.typography.bodyMedium)
        }
        if (trend.daysCaloriesNearTarget == null && trend.daysProteinAtTarget == null) {
            Text(stringResource(R.string.trend_no_targets), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Working sets and external-load volume per rolling week for the last 12 weeks. */
@Composable
internal fun TrainingTrendCard(snapshot: FitnessSnapshot, today: LocalDate) {
    val weeks = remember(snapshot.sessions, snapshot.sets, today) { TrainingTrends.weekly(snapshot, today, 12) }
    val active = weeks.filter { it.workingSets > 0 }
    InfoCard(Modifier.testTag("training_trend_card")) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.training_trends_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            ScopeInfoButton(R.string.training_trends_title, R.string.training_trend_note)
        }
        if (active.isEmpty()) {
            Text(stringResource(R.string.training_trend_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@InfoCard
        }
        val average = weeks.sumOf { it.workingSets }.toDouble() / weeks.size
        val latest = weeks.last()
        val description = stringResource(
            R.string.training_chart_description,
            number(weeks.size.toDouble(), 0),
            number(latest.workingSets.toDouble(), 0),
            number(average, 1),
        )
        val bar = MaterialTheme.colorScheme.tertiary
        val current = MaterialTheme.colorScheme.primary
        val track = MaterialTheme.colorScheme.surfaceContainerHigh
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        Text(stringResource(R.string.training_trend_sets), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Canvas(Modifier.fillMaxWidth().height(120.dp).testTag("training_trend_chart").clearAndSetSemantics { contentDescription = description }) {
            val maxSets = weeks.maxOf { it.workingSets }.coerceAtLeast(1)
            val slot = size.width / weeks.size
            val barWidth = slot * 0.6f
            weeks.forEachIndexed { i, week ->
                val index = if (rtl) weeks.lastIndex - i else i
                val left = index * slot + (slot - barWidth) / 2
                drawRoundRect(track, Offset(left, 0f), Size(barWidth, size.height), CornerRadius(6f, 6f))
                if (week.workingSets > 0) {
                    val h = week.workingSets.toFloat() / maxSets * size.height
                    drawRoundRect(if (i == weeks.lastIndex) current else bar, Offset(left, size.height - h), Size(barWidth, h), CornerRadius(6f, 6f))
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Text(dateLabel(weeks.first().start), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(dateLabel(latest.end), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FormFieldPair(
            first = { MetricTile(number(latest.workingSets.toDouble(), 0), stringResource(R.string.training_trend_this_week, number(latest.sessions.toDouble(), 0)), Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.primary) },
            second = { MetricTile(number(average, 1), stringResource(R.string.training_trend_average), Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.tertiary) },
        )
        Text(stringResource(R.string.training_trend_volume, formatWeight(latest.volumeKg, 0)), style = MaterialTheme.typography.bodyMedium)
    }
}

/** BMI from the latest weight the user logged and the height they entered: a number only, no category labels. */
@Composable
internal fun BmiCard(snapshot: FitnessSnapshot) {
    val height = snapshot.profile?.heightCm
    val latest = snapshot.measurements.filter { it.weightKg != null }.maxByOrNull { it.date }
    val bmi = if (height != null && latest?.weightKg != null) com.formacoach.coach.BodyMetrics.bmi(latest.weightKg!!, height) else null
    InfoCard(Modifier.testTag("bmi_card")) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.bmi_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            ScopeInfoButton(R.string.bmi_title, R.string.bmi_note)
        }
        when {
            bmi != null && latest != null && height != null -> {
                Text(number(bmi, 1), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.bmi_from, dateLabel(latest.date), number(height, 0)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            height == null -> Text(stringResource(R.string.bmi_need_height), color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> Text(stringResource(R.string.bmi_need_weight), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(stringResource(R.string.bmi_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Nightly sleep copied read-only from Health Connect: duration and stages only; hidden until something is imported. */
@Composable
internal fun SleepCard(vm: FitnessViewModel, today: LocalDate) {
    val imports by vm.healthImports.collectAsStateWithLifecycle()
    if (imports.sleep.isEmpty()) return
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val byDate = imports.sleep.associateBy { it.date }
    val recorded = days.mapNotNull { byDate[it] }
    val description = stringResource(R.string.sleep_nights_recorded, number(recorded.size.toDouble(), 0))
    val bar = MaterialTheme.colorScheme.tertiary
    val outline = MaterialTheme.colorScheme.outline
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    InfoCard(Modifier.testTag("sleep_card")) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.sleep_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            ScopeInfoButton(R.string.sleep_title, R.string.sleep_note)
        }
        Text(description, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Canvas(Modifier.fillMaxWidth().height(100.dp).clearAndSetSemantics { contentDescription = description }) {
            val maxMinutes = maxOf(recorded.maxOfOrNull { it.asleepMinutes } ?: 0.0, 60.0)
            val slot = size.width / days.size
            val w = slot * 0.6f
            days.forEachIndexed { i, day ->
                val index = if (rtl) days.lastIndex - i else i
                val left = index * slot + (slot - w) / 2
                val night = byDate[day]
                if (night != null) {
                    val h = (night.asleepMinutes / maxMinutes * size.height).toFloat().coerceAtLeast(2f)
                    drawRoundRect(bar, Offset(left, size.height - h), Size(w, h), CornerRadius(6f, 6f))
                } else {
                    val h = size.height * 0.15f
                    drawRoundRect(outline, Offset(left, size.height - h), Size(w, h), CornerRadius(6f, 6f),
                        style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f))))
                }
            }
        }
        if (recorded.isNotEmpty()) {
            Text(stringResource(R.string.sleep_average, sleepDuration(recorded.sumOf { it.asleepMinutes } / recorded.size)), style = MaterialTheme.typography.bodyMedium)
        }
        val last = imports.sleep.maxByOrNull { it.date }
        if (last != null) {
            Text(stringResource(R.string.sleep_last_night, dateLabel(last.date), sleepDuration(last.asleepMinutes)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (last.deepMinutes != null && last.remMinutes != null) {
                Text(
                    stringResource(R.string.sleep_stages, sleepDuration(last.deepMinutes!!), sleepDuration(last.remMinutes!!), sleepDuration(last.lightMinutes ?: 0.0)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(stringResource(R.string.sleep_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun sleepDuration(minutes: Double): String {
    val total = kotlin.math.round(minutes).toInt()
    return stringResource(R.string.duration_hm, number((total / 60).toDouble(), 0), number((total % 60).toDouble(), 0))
}
