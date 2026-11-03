package com.formacoach.app

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.formacoach.coach.RestTimer
import com.formacoach.coach.TrainingHistory
import com.formacoach.model.*
import kotlinx.coroutines.delay
import java.time.LocalDate

@Composable
internal fun TrainContent(state:AppState,today:LocalDate,vm:FitnessViewModel) {
    var day by rememberSaveable {mutableLongStateOf(today.toEpochDay())}
    val date=LocalDate.ofEpochDay(day)
    var programEditor by remember {mutableStateOf<TrainingProgram?>(null)}
    var newProgram by rememberSaveable {mutableStateOf(false)}
    var search by rememberSaveable {mutableStateOf("")}
    var limit by rememberSaveable {mutableIntStateOf(10)}
    var historyExercise by rememberSaveable {mutableStateOf(Exercise.ROW)}
    val active=state.snapshot.sessions.firstOrNull {it.completedAt==null}
    Text(stringResource(R.string.training_title),style=MaterialTheme.typography.headlineLarge)
    Text(stringResource(R.string.template_note),color=MaterialTheme.colorScheme.onSurfaceVariant)
    if(active==null) {
        DateChoice(date,today,{day=it.toEpochDay()},"training_date")
        state.snapshot.programs.forEach {program -> InfoCard {
            Text(program.name,style=MaterialTheme.typography.titleLarge)
            program.exercises.forEach {Text(stringResource(exerciseLabel(it)))}
            Button(onClick={vm.start(program.id,date)},enabled=!state.busy,modifier=Modifier.fillMaxWidth()) {Text(stringResource(R.string.start_session))}
            TextButton(onClick={programEditor=program}) {Text(stringResource(R.string.edit))}
            ConfirmButton(R.string.remove,stringResource(R.string.delete_program_note),!state.busy) {vm.deleteProgram(program.id)}
        }}
        OutlinedButton(onClick={newProgram=true},modifier=Modifier.fillMaxWidth().testTag("new_program")) {Text(stringResource(R.string.new_program))}
        StarterTemplates.all.forEach {template -> InfoCard {
            Text(stringResource(if(template.id=="A") R.string.template_a else R.string.template_b),style=MaterialTheme.typography.titleLarge)
            template.exercises.forEach {Text(stringResource(exerciseLabel(it)))}
            Button(onClick={vm.start(template.id,date)},enabled=!state.busy,modifier=Modifier.fillMaxWidth().testTag("start_${template.id}")) {Text(stringResource(R.string.start_session))}
        }}
    } else SessionForm(active,state,vm)
    if(newProgram || programEditor!=null) ProgramDialog(programEditor,state.busy,onDismiss={newProgram=false;programEditor=null},onSave={
        vm.saveProgram(it) {newProgram=false;programEditor=null}
    })

    InfoCard {
        Text(stringResource(R.string.load_history),style=MaterialTheme.typography.titleLarge)
        ExercisePicker(historyExercise,{historyExercise=it})
        val points=TrainingHistory.loads(state.snapshot,historyExercise,today).takeLast(30)
        HistoryChart(points,stringResource(R.string.kg))
        Text(stringResource(R.string.external_load_note),style=MaterialTheme.typography.bodySmall)
    }
    Text(stringResource(R.string.recent_sessions),style=MaterialTheme.typography.titleLarge)
    Input(R.string.search_history,search,{search=it;limit=10},"session_search",numeric=false)
    val names=Exercise.entries.associateWith {stringResource(exerciseLabel(it))}
    val sessions=state.snapshot.sessions.filter {session -> session.completedAt!=null &&
        (search.isBlank() || session.label.contains(search,true) || session.date.toString().contains(search) || state.snapshot.sets.any {it.sessionId==session.id && names.getValue(it.exercise).contains(search,true)})}
    if(sessions.isEmpty()) Text(stringResource(R.string.no_sessions))
    sessions.take(limit).forEach {session -> key(session.id) {SessionHistoryCard(session,state,vm,today)}}
    if(sessions.size>limit) TextButton(onClick={limit+=10}) {Text(stringResource(R.string.show_more))}
}

@Composable
private fun SessionForm(session:Session,state:AppState,vm:FitnessViewModel) {
    val choices=(session.plannedExercises+Exercise.entries).distinct()
    var exercise by rememberSaveable(session.id) {mutableStateOf(choices.first())}
    var reps by rememberSaveable(session.id) {mutableStateOf("")}
    var weight by rememberSaveable(session.id) {mutableStateOf("")}
    var deadline by rememberSaveable(session.id) {mutableLongStateOf(0)}
    var elapsed by remember {mutableLongStateOf(SystemClock.elapsedRealtime())}
    LaunchedEffect(deadline) {while(deadline>SystemClock.elapsedRealtime()) {elapsed=SystemClock.elapsedRealtime();delay(1000)};elapsed=SystemClock.elapsedRealtime()}
    val remaining=RestTimer.remainingSeconds(deadline,elapsed,session.restSeconds)
    val sets=state.snapshot.sets.filter {it.sessionId==session.id}
    val candidate=runCatching {TrainingSet(sessionId=session.id,exercise=exercise,reps=requireNotNull(NumericInput.integer(reps)),weightKg=requireNotNull(NumericInput.decimal(weight)))}.getOrNull()
    InfoCard {
        Text(if(session.label.isBlank()) stringResource(R.string.active_session,session.templateId) else session.label,style=MaterialTheme.typography.titleLarge)
        Text(dateLabel(session.date))
        ExercisePicker(exercise,{exercise=it},choices)
        val previous=state.snapshot.sets.lastOrNull {set -> set.exercise==exercise && state.snapshot.sessions.any {it.id==set.sessionId && it.completedAt!=null && it.date<=session.date}}
        previous?.let {Text(stringResource(R.string.previous_set,number(it.reps.toDouble(),0),number(it.weightKg)))}
        Text(stringResource(R.string.exercise_record_note),style=MaterialTheme.typography.bodySmall)
        Input(R.string.reps,reps,{reps=it},"set_reps")
        Input(R.string.weight_kg,weight,{weight=it},"set_weight")
        Text(stringResource(R.string.zero_external_load),style=MaterialTheme.typography.bodySmall)
        Button(onClick={candidate?.let {vm.addSet(session.id,it.exercise,it.reps,it.weightKg) {
            reps="";weight="";elapsed=SystemClock.elapsedRealtime();deadline=elapsed+session.restSeconds*1000L
        }}},enabled=candidate!=null&&!state.busy,modifier=Modifier.fillMaxWidth().testTag("add_set")) {Text(stringResource(R.string.add_set))}
        Text(stringResource(R.string.rest_countdown,number(remaining.toDouble(),0),number(session.restSeconds.toDouble(),0)),style=MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.timer_note),style=MaterialTheme.typography.bodySmall)
        Row {
            TextButton(onClick={elapsed=SystemClock.elapsedRealtime();deadline=elapsed+session.restSeconds*1000L}) {Text(stringResource(R.string.restart_timer))}
            TextButton(onClick={deadline=0}) {Text(stringResource(R.string.stop_timer))}
        }
        sets.forEach {SetCard(it,state.busy,canRemove=true,vm=vm)}
        OutlinedButton(onClick={vm.finish(session.id)},enabled=sets.isNotEmpty()&&!state.busy,modifier=Modifier.fillMaxWidth().testTag("finish_session")) {Text(stringResource(R.string.finish_session))}
        ConfirmButton(R.string.cancel_session,stringResource(R.string.delete_session_note),!state.busy) {vm.deleteSession(session.id)}
    }
}

@Composable
private fun SessionHistoryCard(session:Session,state:AppState,vm:FitnessViewModel,today:LocalDate) {
    val sets=state.snapshot.sets.filter {it.sessionId==session.id}
    InfoCard {
        Text(if(session.label.isNotBlank()) session.label else stringResource(R.string.session_summary,session.templateId,number(sets.size.toDouble(),0)),style=MaterialTheme.typography.titleMedium)
        DateChoice(session.date,today,{vm.moveSession(session.id,it)},"session_date_${session.id}")
        sets.forEach {SetCard(it,state.busy,canRemove=sets.size>1,vm=vm)}
        if(sets.size==1) Text(stringResource(R.string.last_set_note),style=MaterialTheme.typography.bodySmall)
        ConfirmButton(R.string.delete_session,stringResource(R.string.delete_session_note),!state.busy) {vm.deleteSession(session.id)}
    }
}

@Composable
private fun SetCard(set:TrainingSet,busy:Boolean,canRemove:Boolean,vm:FitnessViewModel) {
    var editing by remember {mutableStateOf(false)}
    Column {
        Text(stringResource(R.string.set_summary,stringResource(exerciseLabel(set.exercise)),number(set.reps.toDouble(),0),number(set.weightKg)))
        Row {
            TextButton(onClick={editing=true},enabled=!busy,modifier=Modifier.testTag("edit_set")) {Text(stringResource(R.string.edit))}
            ConfirmButton(R.string.remove,stringResource(R.string.delete_set_note),!busy&&canRemove) {vm.removeSet(set.id)}
        }
    }
    if(editing) SetDialog(set,busy,{editing=false}) {vm.correctSet(it){editing=false}}
}

@Composable
private fun SetDialog(set:TrainingSet,busy:Boolean,onDismiss:()->Unit,onSave:(TrainingSet)->Unit) {
    var exercise by remember {mutableStateOf(set.exercise)}
    var reps by remember {mutableStateOf(set.reps.toString())}
    var weight by remember {mutableStateOf(set.weightKg.toString())}
    val value=runCatching {set.copy(exercise=exercise,reps=requireNotNull(NumericInput.integer(reps)),weightKg=requireNotNull(NumericInput.decimal(weight)))}.getOrNull()
    AlertDialog(onDismissRequest=onDismiss,title={Text(stringResource(R.string.edit_set))},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        ExercisePicker(exercise,{exercise=it});Input(R.string.reps,reps,{reps=it},"edit_reps");Input(R.string.weight_kg,weight,{weight=it},"edit_weight")
    }},confirmButton={TextButton(onClick={value?.let(onSave)},enabled=value!=null&&!busy,modifier=Modifier.testTag("save_set_correction")) {Text(stringResource(R.string.save))}},dismissButton={TextButton(onClick=onDismiss) {Text(stringResource(R.string.cancel))}})
}

@Composable
private fun ProgramDialog(program:TrainingProgram?,busy:Boolean,onDismiss:()->Unit,onSave:(TrainingProgram)->Unit) {
    var name by remember {mutableStateOf(program?.name.orEmpty())}
    var selected by remember {mutableStateOf(program?.exercises?:emptyList())}
    var rest by remember {mutableStateOf((program?.restSeconds?:90).toString())}
    val candidate=runCatching {TrainingProgram(id=program?.id?:java.util.UUID.randomUUID().toString(),name=name.trim(),exercises=selected,restSeconds=requireNotNull(NumericInput.integer(rest)))}.getOrNull()
    AlertDialog(onDismissRequest=onDismiss,title={Text(stringResource(R.string.saved_program))},text={Column(Modifier.heightIn(max=480.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Input(R.string.program_name,name,{name=it},"program_name",numeric=false)
        Input(R.string.rest_seconds,rest,{rest=it},"program_rest")
        Text(stringResource(R.string.program_note))
        Exercise.entries.forEach {exercise -> FilterChip(selected=exercise in selected,onClick={selected=if(exercise in selected) selected-exercise else selected+exercise},label={Text(stringResource(exerciseLabel(exercise)))},modifier=Modifier.testTag("program_exercise_${exercise.name}"))}
        selected.forEachIndexed {index,exercise ->
            Text(stringResource(exerciseLabel(exercise)))
            Row {
                TextButton(onClick={selected=selected.toMutableList().apply {java.util.Collections.swap(this,index,index-1)}},enabled=index>0) {Text(stringResource(R.string.move_up))}
                TextButton(onClick={selected=selected.toMutableList().apply {java.util.Collections.swap(this,index,index+1)}},enabled=index<selected.lastIndex) {Text(stringResource(R.string.move_down))}
            }
        }
    }},confirmButton={TextButton(onClick={candidate?.let(onSave)},enabled=candidate!=null&&!busy,modifier=Modifier.testTag("save_program")) {Text(stringResource(R.string.save))}},dismissButton={TextButton(onClick=onDismiss) {Text(stringResource(R.string.cancel))}})
}
