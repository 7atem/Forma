package com.formacoach.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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
    val unit = LocalWeightUnit.current
    val unitStr = weightUnitLabel()
    var limit by rememberSaveable { mutableIntStateOf(14) }

    // Day being entered or edited in the measurement sheet; null when it is closed.
    var measuringDay by rememberSaveable { mutableStateOf<Long?>(null) }

    PageHeading(R.string.progress_title)

    ProgressHighlightsCard(state.snapshot, today, onLogMeasurement = { measuringDay = today.toEpochDay() })

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
                Text(stringResource(R.string.personal_records_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                SectionInfoButton(InfoTopic.PERSONAL_RECORDS)
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
                                "${if (best.weightKg == 0.0) stringResource(R.string.bodyweight_load) else formatWeight(best.weightKg)} × ${number(best.repsAtWeight.toDouble(), 0)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            e1rm?.let {
                                PillBadge(
                                    "${stringResource(R.string.chart_mode_e1rm)} ${formatWeight(it.value, 1)}",
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

    InfoCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.weight_chart), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            SectionInfoButton(InfoTopic.WEIGHT_CHART)
        }
        val weightPoints = remember(state.snapshot, today, unit) {
            TrainingHistory.weights(state.snapshot, today).map { it.copy(value = WeightUnits.fromKg(it.value, unit)) }
        }
        val trendPoints = remember(weightPoints) {
            TrainingHistory.rollingWeightTrend(weightPoints, windowDays = 7)
        }
        HistoryChart(weightPoints, unitStr, trendPoints = trendPoints)
    }

    TrainingTrendCard(state.snapshot, today)
    NutritionTrendCard(state.snapshot, today)

    // 7-Day Body Muscle Heat Map
    val weeklyCounts = remember(state.snapshot, today) { MuscleTraining.weekly(state.snapshot, today) }
    val weeklyMuscleMap = remember(weeklyCounts) { weeklyCounts.associate { it.muscle to (it.primarySets + it.secondarySets) } }
    InfoCard {
        WeeklyMuscleHeatMap(weeklyMuscleMap)
        // The shading combines roles; main and supporting set counts stay listed separately.
        WeeklyMuscleSplit(weeklyCounts)
    }

    // 12-Week GitHub-Style Training Consistency Grid + Streak
    val countsByDate = remember(state.snapshot.sessions) {
        state.snapshot.sessions.filter { it.completedAt != null }.groupingBy { it.date }.eachCount()
    }
    val weekStreak = remember(state.snapshot, today) { TrainingHistory.activeWeekStreak(state.snapshot, today) }
    InfoCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.activity_heatmap_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            SectionInfoButton(InfoTopic.ACTIVITY_GRID)
        }
        if (weekStreak > 0) {
            PillBadge(stringResource(R.string.week_streak_badge, number(weekStreak.toDouble(), 0)))
        }
        ActivityHeatmapGrid(today = today, countsByDate = countsByDate)
    }

    BmiCard(state.snapshot)
    SleepCard(vm, today)


    WeeklyReviewCard(state.snapshot, today, vm)

    Text(stringResource(R.string.measurement_history), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    if (state.snapshot.measurements.isEmpty()) Text(stringResource(R.string.no_measurements), color = MaterialTheme.colorScheme.onSurfaceVariant)
    state.snapshot.measurements.take(limit).forEach { value ->
        InfoCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(dateLabel(value.date), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        value.weightKg?.let { PillBadge(stringResource(R.string.body_weight_value, formatWeight(it))) }
                        value.waistCm?.let {
                            PillBadge(
                                stringResource(R.string.waist_value, number(it)),
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                    }
                }
                TextButton(onClick = { measuringDay = value.date.toEpochDay() }) { Text(stringResource(R.string.edit)) }
                ConfirmButton(R.string.remove, stringResource(R.string.delete_measurement_note), !state.busy) {
                    vm.removeMeasurement(value.date)
                }
            }
        }
    }
    if (state.snapshot.measurements.size > limit) {
        TextButton(onClick = { limit += 14 }) { Text(stringResource(R.string.show_more)) }
    }
    NutritionAndMeasurementsExport(state, vm)
    measuringDay?.let { initial ->
        MeasurementSheet(state, today, LocalDate.ofEpochDay(initial), vm) { measuringDay = null }
    }
}

@Composable
internal fun WeeklyReviewCard(snapshot: FitnessSnapshot, date: LocalDate, vm: FitnessViewModel) {
    val review = vm.reviewEngine.review(snapshot, date)
    InfoCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.weekly_review), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            SectionInfoButton(InfoTopic.WEEKLY_REVIEW)
        }
        PillBadge(stringResource(R.string.date_range, dateLabel(review.start), dateLabel(review.end)))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(
                number(review.completedSessions.toDouble(), 0),
                stringResource(R.string.review_tile_sessions),
                Modifier.weight(1f),
                accent = MaterialTheme.colorScheme.primary,
            )
            MetricTile(
                stringResource(R.string.review_tile_meal_days_value, number(review.mealDays.toDouble(), 0)),
                stringResource(R.string.review_tile_meal_days),
                Modifier.weight(1f),
                accent = MaterialTheme.colorScheme.secondary,
            )
        }
        if (review.meanLoggedCalories != null || review.meanWeight != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                review.meanLoggedCalories?.let {
                    MetricTile(
                        number(it, 0),
                        stringResource(R.string.review_tile_calories),
                        Modifier.weight(1f),
                    )
                }
                review.meanWeight?.let {
                    MetricTile(
                        formatWeight(it, 1),
                        stringResource(R.string.review_tile_weight, number(review.weightObservations.toDouble(), 0)),
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
                stringResource(R.string.review_delta, formatWeight(delta, 2)),
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

/** Measurement entry in a sheet so the Progress tab opens on what changed, not on an empty form. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MeasurementSheet(state: AppState, today: LocalDate, initialDay: LocalDate, vm: FitnessViewModel, onDismiss: () -> Unit) {
    val unit = LocalWeightUnit.current
    val locale = LocalConfiguration.current.locales[0]
    var day by rememberSaveable { mutableLongStateOf(initialDay.toEpochDay()) }
    val date = LocalDate.ofEpochDay(day)
    val existing = state.snapshot.measurements.firstOrNull { it.date == date }
    var weight by rememberSaveable(day, existing?.weightKg) {
        mutableStateOf(existing?.weightKg?.let { formatInputNumber(WeightUnits.fromKg(it, unit), locale, 2) }.orEmpty())
    }
    var inputUnit by rememberSaveable(day, existing?.weightKg) { mutableStateOf(unit) }
    LaunchedEffect(unit) {
        if (inputUnit != unit) {
            NumericInput.decimal(weight)?.let { parsed ->
                weight = formatInputNumber(WeightUnits.convert(parsed, inputUnit, unit), locale, 2)
            }
            inputUnit = unit
        }
    }
    var waist by rememberSaveable(day, existing?.waistCm) {
        mutableStateOf(existing?.waistCm?.let { formatInputNumber(it, locale, 2) }.orEmpty())
    }
    var neck by rememberSaveable(day, existing?.neckCm) {
        mutableStateOf(existing?.neckCm?.let { formatInputNumber(it, locale, 2) }.orEmpty())
    }
    var chest by rememberSaveable(day, existing?.chestCm) {
        mutableStateOf(existing?.chestCm?.let { formatInputNumber(it, locale, 2) }.orEmpty())
    }
    var arms by rememberSaveable(day, existing?.armsCm) {
        mutableStateOf(existing?.armsCm?.let { formatInputNumber(it, locale, 2) }.orEmpty())
    }
    var hips by rememberSaveable(day, existing?.hipsCm) {
        mutableStateOf(existing?.hipsCm?.let { formatInputNumber(it, locale, 2) }.orEmpty())
    }
    var thighs by rememberSaveable(day, existing?.thighsCm) {
        mutableStateOf(existing?.thighsCm?.let { formatInputNumber(it, locale, 2) }.orEmpty())
    }
    var showMoreTape by rememberSaveable { mutableStateOf(false) }
    val weightKg = if (weight.isBlank()) null else NumericInput.decimal(weight)?.let { WeightUnits.toKg(it, unit) }
    val waistValue = if (waist.isBlank()) null else NumericInput.decimal(waist)
    val neckValue = if (neck.isBlank()) null else NumericInput.decimal(neck)
    val chestValue = if (chest.isBlank()) null else NumericInput.decimal(chest)
    val armsValue = if (arms.isBlank()) null else NumericInput.decimal(arms)
    val hipsValue = if (hips.isBlank()) null else NumericInput.decimal(hips)
    val thighsValue = if (thighs.isBlank()) null else NumericInput.decimal(thighs)
    val weightInvalid = weight.isNotBlank() && (weightKg == null || weightKg !in 0.0001..1000.0)
    val waistInvalid = waist.isNotBlank() && (waistValue == null || waistValue !in 0.0001..500.0)
    val measure = runCatching {
        Measurement(
            date = date,
            weightKg = if (weight.isBlank()) null else requireNotNull(weightKg),
            waistCm = if (waist.isBlank()) null else requireNotNull(waistValue),
            neckCm = if (neck.isBlank()) null else requireNotNull(neckValue),
            chestCm = if (chest.isBlank()) null else requireNotNull(chestValue),
            armsCm = if (arms.isBlank()) null else requireNotNull(armsValue),
            hipsCm = if (hips.isBlank()) null else requireNotNull(hipsValue),
            thighsCm = if (thighs.isBlank()) null else requireNotNull(thighsValue),
        )
    }.getOrNull()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DateChoice(date, today, { day = it.toEpochDay() }, "measurement_date")
        InfoCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.measure_for_date, dateLabel(date)), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                SectionInfoButton(InfoTopic.MEASUREMENTS)
            }
            FormFieldPair(
                first = {
                    Input(
                        if (unit == WeightUnit.KG) R.string.body_weight else R.string.body_weight_lb,
                        weight,
                        { weight = it },
                        "body_weight",
                        error = weightInvalid,
                        supportingText = if (weightInvalid) stringResource(R.string.weight_error) else null,
                    )
                },
                second = {
                    Input(
                        R.string.waist,
                        waist,
                        { waist = it },
                        "waist",
                        error = waistInvalid,
                        supportingText = if (waistInvalid) stringResource(R.string.waist_error) else null,
                    )
                },
            )
            FilterChip(
                selected = showMoreTape,
                onClick = { showMoreTape = !showMoreTape },
                label = { Text(stringResource(R.string.more_measurements), fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("toggle_more_tape"),
            )
            if (showMoreTape) {
                FormFieldPair(
                    first = { Input(R.string.neck_cm, neck, { neck = it }, "neck_cm") },
                    second = { Input(R.string.chest_cm, chest, { chest = it }, "chest_cm") },
                )
                FormFieldPair(
                    first = { Input(R.string.arms_cm, arms, { arms = it }, "arms_cm") },
                    second = { Input(R.string.hips_cm, hips, { hips = it }, "hips_cm") },
                )
                Input(R.string.thighs_cm, thighs, { thighs = it }, "thighs_cm")
            }
            Button(
                onClick = { measure?.let { vm.measure(it) { onDismiss() } } },
                enabled = measure != null && !state.busy,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("save_measurement"),
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.save_measurement), fontWeight = FontWeight.Bold)
            }
        }
        }
    }
}
