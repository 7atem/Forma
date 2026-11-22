package com.formacoach.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.formacoach.coach.MuscleTraining
import com.formacoach.coach.TrainingHistory
import com.formacoach.model.*
import java.time.LocalDate

@Composable
internal fun ProgressContent(state: AppState, today: LocalDate, vm: FitnessViewModel) {
    var day by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    val date = LocalDate.ofEpochDay(day)
    val existing = state.snapshot.measurements.firstOrNull { it.date == date }
    var weight by rememberSaveable(day, existing?.weightKg) { mutableStateOf(existing?.weightKg?.toString().orEmpty()) }
    var waist by rememberSaveable(day, existing?.waistCm) { mutableStateOf(existing?.waistCm?.toString().orEmpty()) }
    val measure = runCatching {
        Measurement(
            date,
            if (weight.isBlank()) null else requireNotNull(NumericInput.decimal(weight)),
            if (waist.isBlank()) null else requireNotNull(NumericInput.decimal(waist)),
        )
    }.getOrNull()
    var limit by rememberSaveable { mutableIntStateOf(14) }
    val latestWeight = state.snapshot.measurements.firstOrNull { it.weightKg != null }
    val latestWaist = state.snapshot.measurements.firstOrNull { it.waistCm != null }
    val weeklyVolume = TrainingHistory.weeklyTonnage(state.snapshot, today)
    val unit = LocalWeightUnit.current
    val unitStr = stringResource(if (unit == WeightUnit.KG) R.string.kg else R.string.lb)

    PageHeading(R.string.progress_title)

    if (latestWeight != null || latestWaist != null || weeklyVolume > 0.0) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            latestWeight?.weightKg?.let {
                MetricTile(
                    "${number(WeightUnits.fromKg(it, unit), 1)} $unitStr",
                    stringResource(R.string.body_weight),
                    Modifier.weight(1f),
                    accent = MaterialTheme.colorScheme.primary,
                )
            }
            latestWaist?.waistCm?.let {
                MetricTile(
                    number(it),
                    stringResource(R.string.waist),
                    Modifier.weight(1f),
                    accent = MaterialTheme.colorScheme.tertiary,
                )
            }
            if (weeklyVolume > 0.0) {
                MetricTile(
                    "${number(WeightUnits.fromKg(weeklyVolume, unit), 0)} $unitStr",
                    stringResource(R.string.weekly_volume_label),
                    Modifier.weight(1f),
                    accent = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }

    DateChoice(date, today, { day = it.toEpochDay() }, "measurement_date")

    InfoCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.measure_for_date, dateLabel(date)), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            ScopeInfoButton(R.string.progress_title, R.string.measure_note)
        }
        FormFieldPair(
            first = { Input(R.string.body_weight, weight, { weight = it }, "body_weight") },
            second = { Input(R.string.waist, waist, { waist = it }, "waist") },
        )
        Button(
            onClick = { measure?.let { vm.measure(it) {} } },
            enabled = measure != null && !state.busy,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("save_measurement"),
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.save_measurement), fontWeight = FontWeight.Bold)
        }
    }

    InfoCard {
        Text(stringResource(R.string.weight_chart), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        HistoryChart(
            TrainingHistory.weights(state.snapshot, today).map { it.copy(value = WeightUnits.fromKg(it.value, unit)) },
            unitStr,
        )
    }

    // 7-Day Body Muscle Heat Map
    val weeklyMuscleMap = remember(state.snapshot, today) {
        MuscleTraining.weekly(state.snapshot, today).associate { it.muscle to (it.primarySets + it.secondarySets) }
    }
    InfoCard {
        WeeklyMuscleHeatMap(weeklyMuscleMap)
    }

    // 12-Week GitHub-Style Training Consistency Grid + Streak
    val countsByDate = remember(state.snapshot.sessions) {
        state.snapshot.sessions.filter { it.completedAt != null }.groupingBy { it.date }.eachCount()
    }
    val weekStreak = remember(state.snapshot, today) { TrainingHistory.activeWeekStreak(state.snapshot, today) }
    InfoCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.activity_heatmap_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (weekStreak > 0) {
                PillBadge(stringResource(R.string.week_streak_badge, number(weekStreak.toDouble(), 0)))
            }
        }
        ActivityHeatmapGrid(today = today, countsByDate = countsByDate)
    }

    // Personal Records (PR) Trophy Wall
    val records = remember(state.snapshot, today) {
        Exercise.entries.mapNotNull { ex ->
            MuscleTraining.best(state.snapshot, ex, today)?.let { best ->
                Triple(ex, best, TrainingHistory.bestE1rm(state.snapshot, ex, today))
            }
        }.sortedByDescending { it.second.weightKg }.take(8)
    }
    if (records.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
                Text(stringResource(R.string.personal_records_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                records.forEach { (ex, best, e1rm) ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
                        modifier = Modifier.width(172.dp),
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                stringResource(exerciseLabel(ex)),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${number(best.weightKg)} ${stringResource(R.string.kg)} × ${number(best.repsAtWeight.toDouble(), 0)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            e1rm?.let {
                                PillBadge(
                                    "${stringResource(R.string.chart_mode_e1rm)} ${number(it.value, 1)}",
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    WeeklyReviewCard(state.snapshot, date, vm)

    Text(stringResource(R.string.measurement_history), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    if (state.snapshot.measurements.isEmpty()) Text(stringResource(R.string.no_measurements), color = MaterialTheme.colorScheme.onSurfaceVariant)
    state.snapshot.measurements.take(limit).forEach { value ->
        InfoCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(dateLabel(value.date), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        value.weightKg?.let { PillBadge(stringResource(R.string.body_weight_value, number(it))) }
                        value.waistCm?.let {
                            PillBadge(
                                stringResource(R.string.waist_value, number(it)),
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                    }
                }
                TextButton(onClick = { day = value.date.toEpochDay() }) { Text(stringResource(R.string.edit)) }
                ConfirmButton(R.string.remove, stringResource(R.string.delete_measurement_note), !state.busy) {
                    vm.removeMeasurement(value.date)
                }
            }
        }
    }
    if (state.snapshot.measurements.size > limit) {
        TextButton(onClick = { limit += 14 }) { Text(stringResource(R.string.show_more)) }
    }
}

@Composable
internal fun WeeklyReviewCard(snapshot: FitnessSnapshot, date: LocalDate, vm: FitnessViewModel) {
    val review = vm.reviewEngine.review(snapshot, date)
    InfoCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.weekly_review), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            PillBadge(stringResource(R.string.date_range, dateLabel(review.start), dateLabel(review.end)))
            ScopeInfoButton(R.string.weekly_review, R.string.logged_only, R.string.no_auto_change)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(
                number(review.completedSessions.toDouble(), 0),
                stringResource(R.string.review_sessions, number(review.completedSessions.toDouble(), 0)),
                Modifier.weight(1f),
                accent = MaterialTheme.colorScheme.primary,
            )
            MetricTile(
                number(review.mealDays.toDouble(), 0),
                stringResource(R.string.review_meal_days, number(review.mealDays.toDouble(), 0)),
                Modifier.weight(1f),
                accent = MaterialTheme.colorScheme.secondary,
            )
        }
        if (review.meanLoggedCalories != null || review.meanWeight != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                review.meanLoggedCalories?.let {
                    MetricTile(
                        number(it, 0),
                        stringResource(R.string.review_calories, number(it, 0)),
                        Modifier.weight(1f),
                    )
                }
                review.meanWeight?.let {
                    MetricTile(
                        "${number(it)} ${stringResource(R.string.kg)}",
                        stringResource(R.string.review_weight, number(it), number(review.weightObservations.toDouble(), 0)),
                        Modifier.weight(1f),
                        accent = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
        review.meanWaist?.let { Text(stringResource(R.string.review_waist, number(it)), style = MaterialTheme.typography.bodyMedium) }
        val delta = review.weightDelta
        if (delta != null) {
            PillBadge(
                stringResource(R.string.review_delta, number(delta)),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                stringResource(
                    R.string.comparison_range,
                    dateLabel(review.start.minusDays(7)),
                    dateLabel(review.start.minusDays(1)),
                    number(review.previousWeightObservations.toDouble(), 0),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(stringResource(R.string.not_enough_weight), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Text(stringResource(actionLabel(review.nextAction)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
