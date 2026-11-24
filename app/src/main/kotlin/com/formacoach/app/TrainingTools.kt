package com.formacoach.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.formacoach.coach.*
import com.formacoach.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun TrainingExport(state:AppState,vm:FitnessViewModel) {
    val context=LocalContext.current
    var preview by remember {mutableStateOf(false)}
    var pending by remember {mutableStateOf<String?>(null)}
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) {uri ->
        val text=pending;pending=null
        if(uri!=null) vm.write {checkNotNull(text);withContext(Dispatchers.IO) {checkNotNull(context.contentResolver.openOutputStream(uri,"wt")).use {it.write(text.toByteArray(Charsets.UTF_8))}}}
    }
    OutlinedButton(onClick={preview=true},enabled=!state.busy&&state.snapshot.sessions.any {it.completedAt!=null},modifier=Modifier.fillMaxWidth().testTag("export_training_csv")) {Text(stringResource(R.string.export_training_csv))}
    if(preview) AlertDialog(onDismissRequest={preview=false},title={Text(stringResource(R.string.export_training_csv))},text={Text(stringResource(R.string.csv_export_note))},confirmButton={TextButton(onClick={vm.write {pending=withContext(Dispatchers.Default){TrainingCsv.export(state.snapshot)};preview=false;launcher.launch("forma-training.csv")}},enabled=!state.busy) {Text(stringResource(R.string.save))}},dismissButton={TextButton(onClick={preview=false}) {Text(stringResource(R.string.cancel))}})
}

@Composable
internal fun NutritionAndMeasurementsExport(state: AppState, vm: FitnessViewModel) {
    val context = LocalContext.current
    var pendingText by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val text = pendingText; pendingText = null
        if (uri != null) vm.write {
            checkNotNull(text)
            withContext(Dispatchers.IO) {
                checkNotNull(context.contentResolver.openOutputStream(uri, "wt")).use { it.write(text.toByteArray(Charsets.UTF_8)) }
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = {
                vm.write {
                    pendingText = withContext(Dispatchers.Default) { NutritionCsv.export(state.snapshot) }
                    launcher.launch("forma-nutrition.csv")
                }
            },
            enabled = !state.busy && state.snapshot.meals.isNotEmpty(),
            modifier = Modifier.weight(1f).testTag("export_nutrition_csv"),
        ) { Text(stringResource(R.string.export_nutrition_csv)) }
        OutlinedButton(
            onClick = {
                vm.write {
                    pendingText = withContext(Dispatchers.Default) { MeasurementsCsv.export(state.snapshot) }
                    launcher.launch("forma-measurements.csv")
                }
            },
            enabled = !state.busy && (state.snapshot.measurements.isNotEmpty() || state.snapshot.hydration.isNotEmpty()),
            modifier = Modifier.weight(1f).testTag("export_measurements_csv"),
        ) { Text(stringResource(R.string.export_measurements_csv)) }
    }
}

@Composable
internal fun PlateCalculatorButton() {
    var show by remember {mutableStateOf(false)}
    OutlinedButton(onClick={show=true},modifier=Modifier.fillMaxWidth().testTag("plate_calculator")) {Text(stringResource(R.string.plate_calculator))}
    if(show) PlateDialog {show=false}
}

@Composable
private fun PlateDialog(onDismiss:()->Unit) {
    val scroll=rememberScrollState()
    val unit = LocalWeightUnit.current
    val unitLabel = weightUnitLabel()
    var bar by remember(unit) {mutableStateOf(if (unit == WeightUnit.KG) "20" else "45")}
    var target by remember(unit) {mutableStateOf(if (unit == WeightUnit.KG) "100" else "225")}
    val sizes = if (unit == WeightUnit.KG) listOf(25.0,20.0,15.0,10.0,5.0,2.5,1.25,0.5) else listOf(45.0,35.0,25.0,10.0,5.0,2.5)
    val counts=remember(unit) {mutableStateListOf(*Array(sizes.size){"2"})}
    var result by remember(unit) {mutableStateOf<PlateCalculator.Result?>(null)}
    var calculated by remember(unit) {mutableStateOf(false)}
    LaunchedEffect(calculated) {if(calculated) {withFrameNanos {};scroll.animateScrollTo(scroll.maxValue)}}
    fun reset() {calculated=false;result=null}
    val inputs=runCatching {
        val b=requireNotNull(NumericInput.decimal(bar));val t=requireNotNull(NumericInput.decimal(target))
        val cap = if (unit == WeightUnit.KG) 1000.0 else 2200.0
        require(b in 0.0..cap && t in b..cap)
        val pairs=sizes.mapIndexed {i,size ->size to requireNotNull(NumericInput.integer(counts[i])).also {require(it in 0..20)}}.toMap()
        Triple(t,b,pairs)
    }.getOrNull()
    AlertDialog(onDismissRequest=onDismiss,title={Text(stringResource(R.string.plate_calculator))},text={Column(Modifier.heightIn(max=500.dp).verticalScroll(scroll),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.plate_note),style=MaterialTheme.typography.bodySmall)
        Input(R.string.bar_weight,bar,{bar=it;reset()},"plate_bar")
        Input(R.string.target_weight,target,{target=it;reset()},"plate_target")
        Text(stringResource(R.string.available_pairs),style=MaterialTheme.typography.titleSmall)
        sizes.forEachIndexed {i,size ->OutlinedTextField(counts[i],{counts[i]=it;reset()},label={Text(stringResource(R.string.plate_pair_count,"${number(size)} $unitLabel"))},singleLine=true,modifier=Modifier.fillMaxWidth().testTag("plate_count_$i"))}
        if(calculated) {
            val value=result
            if(value==null) Text(stringResource(R.string.plates_unavailable),color=MaterialTheme.colorScheme.error)
            else {
                BarbellVisualizer(value.perSide)
                Text(stringResource(R.string.plates_per_side,if(value.perSide.isEmpty()) stringResource(R.string.empty_bar) else value.perSide.map { "${number(it)} $unitLabel" }.joinToString(" + ")),style=MaterialTheme.typography.titleMedium,modifier=Modifier.testTag("plate_result"))
                val ramp = inputs?.let { WarmupRamp.steps(it.first, it.second) }.orEmpty()
                if (ramp.isNotEmpty()) {
                    HorizontalDivider()
                    Text(stringResource(R.string.warmup_ramp_title), style = MaterialTheme.typography.titleSmall)
                    ramp.forEach { step ->
                        Text(
                            "${number(step.weightKg)} $unitLabel × ${number(step.reps.toDouble(), 0)}" +
                                if (step.percent > 0) " (${step.percent}%)" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }},confirmButton={TextButton(onClick={inputs?.let {result=runCatching {PlateCalculator.calculate(it.first,it.second,it.third)}.getOrNull();calculated=true}},enabled=inputs!=null,modifier=Modifier.testTag("calculate_plates")) {Text(stringResource(R.string.calculate))}},dismissButton={TextButton(onClick=onDismiss) {Text(stringResource(R.string.close))}})
}
