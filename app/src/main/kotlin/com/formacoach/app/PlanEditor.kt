package com.formacoach.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.formacoach.model.*

@Composable
internal fun PrescriptionSummary(value:ExercisePrescription) {
    value.workingSets?.let {Text(stringResource(R.string.planned_sets_value,number(it.toDouble(),0)))}
    value.repMin?.let {Text(stringResource(R.string.rep_range_value,number(it.toDouble(),0),number(checkNotNull(value.repMax).toDouble(),0)))}
    value.restSeconds?.let {Text(stringResource(R.string.exercise_rest_value,number(it.toDouble(),0)))}
    value.supersetGroup?.let {Text(stringResource(R.string.linked_group_value,number(it.toDouble(),0)),color=MaterialTheme.colorScheme.primary)}
    if(value.notes.isNotBlank()) Text(value.notes,style=MaterialTheme.typography.bodySmall)
}

@Composable
internal fun PrescriptionEditor(initial:ExercisePrescription,busy:Boolean,onDismiss:()->Unit,onSave:(ExercisePrescription)->Unit) {
    var sets by rememberSaveable(initial.exercise) {mutableStateOf(initial.workingSets?.toString().orEmpty())}
    var min by rememberSaveable(initial.exercise) {mutableStateOf(initial.repMin?.toString().orEmpty())}
    var max by rememberSaveable(initial.exercise) {mutableStateOf(initial.repMax?.toString().orEmpty())}
    var rest by rememberSaveable(initial.exercise) {mutableStateOf(initial.restSeconds?.toString().orEmpty())}
    var notes by rememberSaveable(initial.exercise) {mutableStateOf(initial.notes)}
    fun optional(value:String)=if(value.isBlank()) null else requireNotNull(NumericInput.integer(value))
    val candidate=runCatching {initial.copy(workingSets=optional(sets),repMin=optional(min),repMax=optional(max),restSeconds=optional(rest),notes=notes)}.getOrNull()
    AlertDialog(onDismissRequest=onDismiss,title={Text(stringResource(exerciseLabel(initial.exercise)))},text={Column(Modifier.heightIn(max=500.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.user_plan_targets_note),style=MaterialTheme.typography.bodySmall)
        Input(R.string.planned_working_sets,sets,{sets=it},"plan_sets")
        FormFieldPair(first={Input(R.string.rep_min,min,{min=it},"plan_rep_min")},second={Input(R.string.rep_max,max,{max=it},"plan_rep_max")})
        Text(stringResource(R.string.rep_range_note),style=MaterialTheme.typography.bodySmall)
        Input(R.string.exercise_rest_optional,rest,{rest=it},"plan_exercise_rest")
        Input(R.string.program_exercise_notes,notes,{if(it.length<=1000) notes=it},"plan_exercise_notes",numeric=false)
    }},confirmButton={TextButton(onClick={candidate?.let(onSave)},enabled=candidate!=null&&!busy,modifier=Modifier.testTag("save_plan_details")) {Text(stringResource(R.string.save))}},dismissButton={TextButton(onClick=onDismiss) {Text(stringResource(R.string.cancel))}})
}

@Composable
internal fun GroupLink(linked:Boolean,onChange:(Boolean)->Unit,exercise:Exercise) {
    Row(verticalAlignment=Alignment.CenterVertically) {
        Checkbox(linked,onCheckedChange=onChange,modifier=Modifier.testTag("link_previous_${exercise.name}"))
        Text(stringResource(R.string.link_previous_exercise),Modifier.weight(1f),style=MaterialTheme.typography.bodySmall)
    }
}
