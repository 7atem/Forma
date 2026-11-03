package com.formacoach.app

import android.os.SystemClock
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.formacoach.coach.*
import com.formacoach.model.*
import kotlinx.coroutines.delay
import java.time.LocalDate

@Composable
internal fun TrainContent(state:AppState,today:LocalDate,vm:FitnessViewModel,scrollState:ScrollState) {
    var section by rememberSaveable {mutableIntStateOf(0)}
    var day by rememberSaveable {mutableLongStateOf(today.toEpochDay())}
    var editor by remember {mutableStateOf<TrainingProgram?>(null)}
    var creating by rememberSaveable {mutableStateOf(false)}
    val active=state.snapshot.sessions.firstOrNull {it.completedAt==null}
    PageHeading(R.string.training_title,if(active==null) R.string.training_intro else R.string.session_focus)
    val pages=rememberSaveableStateHolder()
    LaunchedEffect(active?.id) {if(active!=null) section=0}
    LaunchedEffect(section,active?.id) {scrollState.scrollTo(0)}
    TabRow(selectedTabIndex=section,containerColor=MaterialTheme.colorScheme.background) {
            listOf(R.string.my_programs,R.string.exercise_library,R.string.training_history).forEachIndexed {index,label ->
                Tab(selected=section==index,onClick={section=index},text={Text(stringResource(label))},modifier=Modifier.testTag("training_section_$index"))
            }
        }
    pages.SaveableStateProvider(section) {
        when(section) {
            0 -> if(active!=null) SessionForm(active,state,vm){section=2} else {
                val date=LocalDate.ofEpochDay(day)
                DateChoice(date,today,{day=it.toEpochDay()},"training_date")
                state.snapshot.programs.forEach {program ->
                    ProgramCard(program.name,program.exercises,"start_${program.id}",state.busy,{vm.start(program.id,date)}) {
                        Row {
                            TextButton(onClick={editor=program}) {Text(stringResource(R.string.edit))}
                            ConfirmButton(R.string.remove,stringResource(R.string.delete_program_note),!state.busy) {vm.deleteProgram(program.id)}
                        }
                    }
                }
                OutlinedButton(onClick={creating=true},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("new_program")) {Text(stringResource(R.string.new_program))}
                Text(stringResource(R.string.starter_programs),style=MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.template_note),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                StarterTemplates.all.forEach {template ->
                    ProgramCard(stringResource(if(template.id=="A") R.string.template_a else R.string.template_b),template.exercises,"start_${template.id}",state.busy,{vm.start(template.id,date)})
                }
            }
            1 -> ExerciseLibrary()
            2 -> TrainingHistoryContent(state,today,vm)
        }
    }
    if(creating||editor!=null) ProgramDialog(editor,state.busy,{creating=false;editor=null}) {vm.saveProgram(it){creating=false;editor=null}}
}

@Composable
private fun ProgramCard(name:String,exercises:List<Exercise>,tag:String,busy:Boolean,onStart:()->Unit,actions:@Composable ()->Unit={}) {
    InfoCard {
        Row(horizontalArrangement=Arrangement.spacedBy(14.dp),verticalAlignment=Alignment.CenterVertically) {
            ExerciseThumbnail(exercises.first(),Modifier.width(118.dp).clip(RoundedCornerShape(14.dp)))
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(name,style=MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.program_exercise_count,number(exercises.size.toDouble(),0)),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
            }
        }
        Text(exercises.map {stringResource(exerciseLabel(it))}.joinToString(" · "),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick=onStart,enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=50.dp).testTag(tag)) {Text(stringResource(R.string.start_session))}
        actions()
    }
}

@Composable
private fun SessionForm(session:Session,state:AppState,vm:FitnessViewModel,onFinished:()->Unit) {
    val choices=(session.plannedExercises+Exercise.entries).distinct()
    var exercise by rememberSaveable(session.id) {mutableStateOf(choices.first())}
    var reps by rememberSaveable(session.id) {mutableStateOf("")}
    var weight by rememberSaveable(session.id) {mutableStateOf("")}
    var warmup by rememberSaveable(session.id) {mutableStateOf(false)}
    var showRpe by rememberSaveable(session.id) {mutableStateOf(false)}
    var rpe by rememberSaveable(session.id) {mutableStateOf("")}
    var guide by rememberSaveable(session.id) {mutableStateOf(false)}
    var allSets by rememberSaveable(session.id) {mutableStateOf(false)}
    var deadline by rememberSaveable(session.id) {mutableLongStateOf(0)}
    var elapsed by remember {mutableLongStateOf(SystemClock.elapsedRealtime())}
    LaunchedEffect(deadline) {while(deadline>SystemClock.elapsedRealtime()) {elapsed=SystemClock.elapsedRealtime();delay(1000)};elapsed=SystemClock.elapsedRealtime()}
    val remaining=RestTimer.remainingSeconds(deadline,elapsed,session.restSeconds)
    val sets=state.snapshot.sets.filter {it.sessionId==session.id}
    val candidate=runCatching {TrainingSet(sessionId=session.id,exercise=exercise,reps=requireNotNull(NumericInput.integer(reps)),weightKg=requireNotNull(NumericInput.decimal(weight)),kind=if(warmup) SetKind.WARM_UP else SetKind.WORKING,rpe=if(rpe.isBlank()) null else requireNotNull(NumericInput.decimal(rpe)))}.getOrNull()
    InfoCard {
        Text(session.label.ifBlank {stringResource(R.string.active_session,session.templateId)},style=MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.session_status,number(sets.size.toDouble(),0),dateLabel(session.date)),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
        ExercisePicker(exercise,{exercise=it},choices)
        OutlinedCard(onClick={guide=true},modifier=Modifier.fillMaxWidth().testTag("session_form_guide"),colors=CardDefaults.outlinedCardColors(containerColor=MaterialTheme.colorScheme.background)) {
            Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                ExerciseThumbnail(exercise,Modifier.width(110.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.view_form),style=MaterialTheme.typography.titleSmall)
                    Text(stringResource(guideResources(exercise).variant),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        val previous=state.snapshot.sessions.filter {it.completedAt!=null&&it.date<=session.date}.sortedWith(compareByDescending<Session>{it.date}.thenByDescending {it.completedAt})
            .firstNotNullOfOrNull {record ->state.snapshot.sets.lastOrNull {it.sessionId==record.id&&it.exercise==exercise}}
        previous?.let {last ->
            Text(stringResource(R.string.previous_set,number(last.reps.toDouble(),0),number(last.weightKg)),style=MaterialTheme.typography.bodySmall)
            TextButton(onClick={reps=last.reps.toString();weight=last.weightKg.toString();warmup=last.kind==SetKind.WARM_UP;rpe=last.rpe?.toString().orEmpty();showRpe=last.rpe!=null}) {Text(stringResource(R.string.copy_previous_set))}
        }
        FormFieldPair(first={Input(R.string.reps,reps,{reps=it},"set_reps",error=reps.isNotBlank()&&NumericInput.integer(reps)?.let {it in 1..1000}!=true)},
            second={Input(R.string.weight_kg,weight,{weight=it},"set_weight",error=weight.isNotBlank()&&NumericInput.decimal(weight)?.let {it in 0.0..1000.0}!=true)})
        Text(stringResource(R.string.zero_external_load),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            FilterChip(selected=warmup,onClick={warmup=!warmup},label={Text(stringResource(R.string.warmup_set))},modifier=Modifier.testTag("warmup_set"))
            TextButton(onClick={showRpe=!showRpe;if(!showRpe) rpe=""},modifier=Modifier.testTag("toggle_rpe")) {Text(stringResource(R.string.effort_toggle))}
        }
        if(showRpe) {
            Input(R.string.rpe_optional,rpe,{rpe=it},"set_rpe",error=rpe.isNotBlank()&&NumericInput.decimal(rpe)?.let {it in 0.0..10.0}!=true)
            Text(stringResource(R.string.rpe_note),style=MaterialTheme.typography.bodySmall)
        }
        Button(onClick={candidate?.let {vm.addSet(session.id,it.exercise,it.reps,it.weightKg,it.kind,it.rpe) {
            // Keep the user's last entered load/reps for the next set, never prescribe a progression.
            rpe="";elapsed=SystemClock.elapsedRealtime();deadline=elapsed+session.restSeconds*1000L
        }}},enabled=candidate!=null&&!state.busy,modifier=Modifier.fillMaxWidth().heightIn(min=54.dp).testTag("add_set")) {Text(stringResource(R.string.add_set))}
        if(deadline>0) Surface(color=MaterialTheme.colorScheme.primaryContainer,shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.rest_countdown,number(remaining.toDouble(),0),number(session.restSeconds.toDouble(),0)),style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.onPrimaryContainer)
                LinearProgressIndicator(progress={remaining.toFloat()/session.restSeconds},modifier=Modifier.fillMaxWidth(),drawStopIndicator={})
                Row {
                    TextButton(onClick={elapsed=SystemClock.elapsedRealtime();deadline=elapsed+session.restSeconds*1000L}) {Text(stringResource(R.string.restart_timer))}
                    TextButton(onClick={deadline=0}) {Text(stringResource(R.string.stop_timer))}
                }
            }
        }
        Text(stringResource(R.string.timer_note),style=MaterialTheme.typography.bodySmall)
    }
    if(sets.isNotEmpty()) InfoCard {
        Text(stringResource(R.string.logged_sets),style=MaterialTheme.typography.titleMedium)
        (if(allSets) sets else sets.takeLast(3)).forEach {SetCard(it,state.busy,true,vm)}
        if(sets.size>3) TextButton(onClick={allSets=!allSets}) {Text(stringResource(if(allSets) R.string.show_less else R.string.show_more))}
    }
    Button(onClick={vm.finish(session.id,onFinished)},enabled=sets.isNotEmpty()&&!state.busy,modifier=Modifier.fillMaxWidth().heightIn(min=50.dp).testTag("finish_session")) {Text(stringResource(R.string.finish_session))}
    ConfirmButton(R.string.cancel_session,stringResource(R.string.delete_session_note),!state.busy) {vm.deleteSession(session.id)}
    if(guide) ExerciseGuideSheet(exercise){guide=false}
}

@Composable
private fun TrainingHistoryContent(state:AppState,today:LocalDate,vm:FitnessViewModel) {
    var exercise by rememberSaveable {mutableStateOf(Exercise.ROW)}
    var guide by rememberSaveable {mutableStateOf(false)}
    var search by rememberSaveable {mutableStateOf("")}
    var limit by rememberSaveable {mutableIntStateOf(10)}
    InfoCard {
        Text(stringResource(R.string.load_history),style=MaterialTheme.typography.titleLarge)
        ExercisePicker(exercise,{exercise=it})
        MuscleTraining.best(state.snapshot,exercise,today)?.let {Text(stringResource(R.string.best_recorded_load,number(it.weightKg),number(it.repsAtWeight.toDouble(),0),dateLabel(it.date)),style=MaterialTheme.typography.bodyMedium)}
        HistoryChart(TrainingHistory.loads(state.snapshot,exercise,today).takeLast(30),stringResource(R.string.kg))
        Text(stringResource(R.string.external_load_note),style=MaterialTheme.typography.bodySmall)
        TextButton(onClick={guide=true}) {Text(stringResource(R.string.view_form))}
    }
    InfoCard {
        Text(stringResource(R.string.weekly_muscles),style=MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.weekly_muscles_note),style=MaterialTheme.typography.bodySmall)
        val counts=MuscleTraining.weekly(state.snapshot,today).filter {it.primarySets+it.secondarySets>0}
        if(counts.isEmpty()) Text(stringResource(R.string.no_sessions))
        counts.forEach {Text(stringResource(R.string.muscle_set_count,stringResource(muscleLabel(it.muscle)),number(it.primarySets.toDouble(),0),number(it.secondarySets.toDouble(),0)),style=MaterialTheme.typography.bodyMedium)}
    }
    Text(stringResource(R.string.recent_sessions),style=MaterialTheme.typography.titleLarge)
    Input(R.string.search_history,search,{search=it;limit=10},"session_search",numeric=false)
    val names=Exercise.entries.associateWith {stringResource(exerciseLabel(it))}
    val sessions=state.snapshot.sessions.filter {it.completedAt!=null && (search.isBlank()||it.label.contains(search,true)||it.date.toString().contains(search)||state.snapshot.sets.any {set ->set.sessionId==it.id&&names.getValue(set.exercise).contains(search,true)})}
    if(sessions.isEmpty()) EmptyState(R.string.no_sessions,R.string.history_empty_hint)
    sessions.take(limit).forEach {session ->key(session.id) {SessionHistoryCard(session,state,vm,today)}}
    if(sessions.size>limit) TextButton(onClick={limit+=10}) {Text(stringResource(R.string.show_more))}
    if(guide) ExerciseGuideSheet(exercise){guide=false}
}

@Composable
private fun SessionHistoryCard(session:Session,state:AppState,vm:FitnessViewModel,today:LocalDate) {
    var expanded by rememberSaveable(session.id) {mutableStateOf(false)}
    val sets=state.snapshot.sets.filter {it.sessionId==session.id}
    InfoCard {
        Text(session.label.ifBlank {stringResource(R.string.session_summary,session.templateId,number(sets.size.toDouble(),0))},style=MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.session_status,number(sets.size.toDouble(),0),dateLabel(session.date)),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick={expanded=!expanded},modifier=Modifier.testTag("history_expand_${session.id}")) {Text(stringResource(if(expanded) R.string.show_less else R.string.view_session_details))}
        if(expanded) {
            if(session.source==SessionSource.APPLE_WATCH_IMPORT) {
                Text(stringResource(R.string.watch_record_source))
                session.averageHeartRateBpm?.let {Text(stringResource(R.string.watch_heart_rate,number(it,0)))}
                session.activeEnergyKcal?.let {Text(stringResource(R.string.watch_energy,number(it,0)))}
            }
            DateChoice(session.date,today,{vm.moveSession(session.id,it)},"session_date_${session.id}")
            sets.forEach {SetCard(it,state.busy,sets.size>1,vm)}
            if(sets.size==1) Text(stringResource(R.string.last_set_note),style=MaterialTheme.typography.bodySmall)
            ConfirmButton(R.string.delete_session,stringResource(R.string.delete_session_note),!state.busy) {vm.deleteSession(session.id)}
        }
    }
}

@Composable
private fun SetCard(set:TrainingSet,busy:Boolean,canRemove:Boolean,vm:FitnessViewModel) {
    var editing by remember {mutableStateOf(false)}
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.set_summary,stringResource(exerciseLabel(set.exercise)),number(set.reps.toDouble(),0),number(set.weightKg)),style=MaterialTheme.typography.titleSmall)
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text(stringResource(if(set.kind==SetKind.WARM_UP) R.string.warmup_set else R.string.working_set),Modifier.weight(1f),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            set.rpe?.let {Text(stringResource(R.string.rpe_value,number(it,1)),style=MaterialTheme.typography.labelMedium)}
            IconButton(onClick={editing=true},enabled=!busy,modifier=Modifier.testTag("edit_set")) {Icon(Icons.Default.Edit,stringResource(R.string.edit))}
            ConfirmButton(R.string.remove,stringResource(R.string.delete_set_note),!busy&&canRemove) {vm.removeSet(set.id)}
        }
        HorizontalDivider(color=MaterialTheme.colorScheme.outline.copy(alpha=0.2f))
    }
    if(editing) SetDialog(set,busy,{editing=false}) {vm.correctSet(it){editing=false}}
}

@Composable
private fun SetDialog(set:TrainingSet,busy:Boolean,onDismiss:()->Unit,onSave:(TrainingSet)->Unit) {
    var exercise by remember {mutableStateOf(set.exercise)}
    var reps by remember {mutableStateOf(set.reps.toString())}
    var weight by remember {mutableStateOf(set.weightKg.toString())}
    var warmup by remember {mutableStateOf(set.kind==SetKind.WARM_UP)}
    var rpe by remember {mutableStateOf(set.rpe?.toString().orEmpty())}
    val value=runCatching {set.copy(exercise=exercise,reps=requireNotNull(NumericInput.integer(reps)),weightKg=requireNotNull(NumericInput.decimal(weight)),kind=if(warmup) SetKind.WARM_UP else SetKind.WORKING,rpe=if(rpe.isBlank()) null else requireNotNull(NumericInput.decimal(rpe)))}.getOrNull()
    AlertDialog(onDismissRequest=onDismiss,title={Text(stringResource(R.string.edit_set))},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        FilterChip(selected=warmup,onClick={warmup=!warmup},label={Text(stringResource(R.string.warmup_set))})
        Input(R.string.rpe_optional,rpe,{rpe=it},"edit_rpe")
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
