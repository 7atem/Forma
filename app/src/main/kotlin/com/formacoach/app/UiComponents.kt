package com.formacoach.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.formacoach.coach.HistoryPoint
import com.formacoach.model.Exercise
import java.time.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateChoice(date:LocalDate,today:LocalDate,onChange:(LocalDate)->Unit,tag:String="date_choice") {
    var open by remember {mutableStateOf(false)}
    OutlinedButton(onClick={open=true},modifier=Modifier.testTag(tag)) {Text(stringResource(R.string.selected_date,dateLabel(date)))}
    if(open) {
        val picker=rememberDatePickerState(initialSelectedDateMillis=date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            yearRange=1970..today.year,selectableDates=object:SelectableDates {
                override fun isSelectableDate(utcTimeMillis:Long)=Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()<=today
            })
        DatePickerDialog(onDismissRequest={open=false},confirmButton={TextButton(onClick={
            picker.selectedDateMillis?.let {onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())};open=false
        },enabled=picker.selectedDateMillis!=null) {Text(stringResource(R.string.save))}},dismissButton={TextButton(onClick={open=false}) {Text(stringResource(R.string.cancel))}}) {DatePicker(picker)}
    }
}

@Composable
internal fun ConfirmButton(label:Int,description:String,enabled:Boolean=true,tag:String="",onConfirm:()->Unit) {
    var open by remember {mutableStateOf(false)}
    TextButton(onClick={open=true},enabled=enabled,modifier=Modifier.testTag(tag)) {Text(stringResource(label))}
    if(open) AlertDialog(onDismissRequest={open=false},title={Text(stringResource(label))},text={Text(description)},
        confirmButton={TextButton(onClick={open=false;onConfirm()}) {Text(stringResource(R.string.confirm))}},
        dismissButton={TextButton(onClick={open=false}) {Text(stringResource(R.string.cancel))}})
}

@Composable
internal fun ExercisePicker(selected:Exercise,onChange:(Exercise)->Unit,choices:List<Exercise> = Exercise.entries) {
    var expanded by remember {mutableStateOf(false)}
    OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth().testTag("exercise_picker")) {Text(stringResource(exerciseLabel(selected)))}
    if(expanded) {
        var search by remember {mutableStateOf("")}
        val labels=choices.associateWith {stringResource(exerciseLabel(it))}
        AlertDialog(onDismissRequest={expanded=false},title={Text(stringResource(R.string.exercise_library))},text={
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Input(R.string.search_exercises,search,{search=it},"picker_exercise_search",numeric=false)
                Column(Modifier.heightIn(max=350.dp).verticalScroll(rememberScrollState())) {
                    choices.filter {com.formacoach.coach.ExerciseSearch.matches(search,labels.getValue(it)+" "+com.formacoach.model.ExerciseCatalog.searchText(it))}.forEach {value ->
                        TextButton(onClick={onChange(value);expanded=false},modifier=Modifier.fillMaxWidth().testTag("exercise_${value.name}")) {Text(labels.getValue(value))}
                    }
                }
            }
        },confirmButton={},dismissButton={TextButton(onClick={expanded=false}) {Text(stringResource(R.string.cancel))}})
    }
}

@Composable
internal fun HistoryChart(points:List<HistoryPoint>,unit:String) {
    if(points.isEmpty()) {Text(stringResource(R.string.no_chart_data));return}
    val min=points.minOf {it.value};val max=points.maxOf {it.value}
    val start=points.first().date;val end=points.last().date
    val description=stringResource(R.string.chart_description,number(points.size.toDouble(),0),dateLabel(start),dateLabel(end),number(min),number(max),unit)
    val color=MaterialTheme.colorScheme.primary
    val axis=MaterialTheme.colorScheme.outline
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.chart_range,number(min),number(max),unit))
        Canvas(Modifier.fillMaxWidth().height(150.dp).semantics {contentDescription=description}) {
            val pad=12.dp.toPx();val width=(size.width-2*pad).coerceAtLeast(1f);val height=(size.height-2*pad).coerceAtLeast(1f)
            drawLine(axis,Offset(pad,pad+height),Offset(pad+width,pad+height),1.dp.toPx())
            val days=(end.toEpochDay()-start.toEpochDay()).coerceAtLeast(1)
            val span=(max-min).takeIf {it>0}?:1.0
            val coords=points.map {point -> Offset(pad+((point.date.toEpochDay()-start.toEpochDay()).toDouble()/days*width).toFloat(),
                pad+height-((if(max==min) 0.5 else (point.value-min)/span)*height).toFloat())}
            val path=Path();coords.forEachIndexed {index,offset ->if(index==0) path.moveTo(offset.x,offset.y) else path.lineTo(offset.x,offset.y)}
            drawPath(path,color,style=androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
            coords.forEach {drawCircle(color,4.dp.toPx(),it)}
        }
        Text(stringResource(R.string.date_range,dateLabel(start),dateLabel(end)),style=MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.chart_note),style=MaterialTheme.typography.bodySmall)
    }
}
