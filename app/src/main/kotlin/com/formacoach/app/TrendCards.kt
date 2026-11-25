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
