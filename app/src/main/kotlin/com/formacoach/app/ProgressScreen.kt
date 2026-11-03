package com.formacoach.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.formacoach.coach.TrainingHistory
import com.formacoach.model.*
import java.time.LocalDate

@Composable
internal fun ProgressContent(state:AppState,today:LocalDate,vm:FitnessViewModel) {
    var day by rememberSaveable {mutableLongStateOf(today.toEpochDay())}
    val date=LocalDate.ofEpochDay(day)
    val existing=state.snapshot.measurements.firstOrNull {it.date==date}
    var weight by rememberSaveable(day,existing?.weightKg) {mutableStateOf(existing?.weightKg?.toString().orEmpty())}
    var waist by rememberSaveable(day,existing?.waistCm) {mutableStateOf(existing?.waistCm?.toString().orEmpty())}
    val measure=runCatching {Measurement(date,if(weight.isBlank()) null else requireNotNull(NumericInput.decimal(weight)),if(waist.isBlank()) null else requireNotNull(NumericInput.decimal(waist)))}.getOrNull()
    var limit by rememberSaveable {mutableIntStateOf(14)}
    Text(stringResource(R.string.progress_title),style=MaterialTheme.typography.headlineLarge)
    DateChoice(date,today,{day=it.toEpochDay()},"measurement_date")
    InfoCard {
        Text(stringResource(R.string.measure_for_date,dateLabel(date)),style=MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.measure_note),style=MaterialTheme.typography.bodySmall)
        Input(R.string.body_weight,weight,{weight=it},"body_weight")
        Input(R.string.waist,waist,{waist=it},"waist")
        Button(onClick={measure?.let {vm.measure(it){}}},enabled=measure!=null&&!state.busy,modifier=Modifier.fillMaxWidth().testTag("save_measurement")) {Text(stringResource(R.string.save_measurement))}
    }
    InfoCard {
        Text(stringResource(R.string.weight_chart),style=MaterialTheme.typography.titleLarge)
        HistoryChart(TrainingHistory.weights(state.snapshot,today),stringResource(R.string.kg))
    }
    WeeklyReviewCard(state.snapshot,date,vm)
    Text(stringResource(R.string.measurement_history),style=MaterialTheme.typography.titleLarge)
    if(state.snapshot.measurements.isEmpty()) Text(stringResource(R.string.no_measurements))
    state.snapshot.measurements.take(limit).forEach {value -> InfoCard {
        Text(dateLabel(value.date))
        value.weightKg?.let {Text(stringResource(R.string.body_weight_value,number(it)))}
        value.waistCm?.let {Text(stringResource(R.string.waist_value,number(it)))}
        TextButton(onClick={day=value.date.toEpochDay()}) {Text(stringResource(R.string.edit))}
        ConfirmButton(R.string.remove,stringResource(R.string.delete_measurement_note),!state.busy) {vm.removeMeasurement(value.date)}
    }}
    if(state.snapshot.measurements.size>limit) TextButton(onClick={limit+=14}) {Text(stringResource(R.string.show_more))}
}

@Composable
internal fun WeeklyReviewCard(snapshot:FitnessSnapshot,date:LocalDate,vm:FitnessViewModel) {
    val review=vm.reviewEngine.review(snapshot,date)
    InfoCard {
        Text(stringResource(R.string.weekly_review),style=MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.date_range,dateLabel(review.start),dateLabel(review.end)))
        Text(stringResource(R.string.review_sessions,number(review.completedSessions.toDouble(),0)))
        Text(stringResource(R.string.review_meal_days,number(review.mealDays.toDouble(),0)))
        review.meanLoggedCalories?.let {Text(stringResource(R.string.review_calories,number(it,0)))}
        Text(stringResource(R.string.logged_only),style=MaterialTheme.typography.bodySmall)
        review.meanWeight?.let {Text(stringResource(R.string.review_weight,number(it),number(review.weightObservations.toDouble(),0)))}
        review.meanWaist?.let {Text(stringResource(R.string.review_waist,number(it)))}
        val delta=review.weightDelta
        if(delta!=null) {
            Text(stringResource(R.string.review_delta,number(delta)))
            Text(stringResource(R.string.comparison_range,dateLabel(review.start.minusDays(7)),dateLabel(review.start.minusDays(1)),number(review.previousWeightObservations.toDouble(),0)),style=MaterialTheme.typography.bodySmall)
        } else Text(stringResource(R.string.not_enough_weight),style=MaterialTheme.typography.bodySmall)
        HorizontalDivider()
        Text(stringResource(actionLabel(review.nextAction)))
        Text(stringResource(R.string.no_auto_change),style=MaterialTheme.typography.bodySmall)
    }
}
