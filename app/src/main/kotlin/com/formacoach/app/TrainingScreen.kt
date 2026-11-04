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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun TrainContent(state:AppState,today:LocalDate,vm:FitnessViewModel,scrollState:ScrollState) {
    var section by rememberSaveable {mutableIntStateOf(0)}
    var day by rememberSaveable {mutableLongStateOf(today.toEpochDay())}
    var editorPayload by rememberSaveable {mutableStateOf<String?>(null)}
    val editor=editorPayload?.let {Json.decodeFromString<TrainingProgram>(it)}
    var creating by rememberSaveable {mutableStateOf(false)}
    val active=state.snapshot.sessions.firstOrNull {it.completedAt==null}
    if(section==0) PageHeading(R.string.training_title,if(active==null) R.string.training_intro else R.string.session_focus)
    else Text(stringResource(if(section==1) R.string.exercise_library else R.string.training_history),style=MaterialTheme.typography.headlineSmall)
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
                        FlowRow {
                            TextButton(onClick={editorPayload=Json.encodeToString(program)}) {Text(stringResource(R.string.edit))}
                            TextButton(onClick={editorPayload=Json.encodeToString(program.copy(id=java.util.UUID.randomUUID().toString()))},enabled=!state.busy,modifier=Modifier.testTag("duplicate_${program.id}")) {Text(stringResource(R.string.duplicate_program))}
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
            1 -> ExerciseLibrary(state,vm)
            2 -> TrainingHistoryContent(state,today,vm)
        }
    }
    PlateCalculatorButton()
    if(creating||editor!=null) ProgramDialog(editor,state.busy,{creating=false;editorPayload=null}) {vm.saveProgram(it){creating=false;editorPayload=null}}
}

@Composable
private fun ProgramCard(name:String,exercises:List<Exercise>,tag:String,busy:Boolean,onStart:()->Unit,actions:@Composable ()->Unit={}) {
    InfoCard(Modifier.testTag("card_$tag")) {
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
    val sets=state.snapshot.sets.filter {it.sessionId==session.id}
    var exercise by rememberSaveable(session.id) {mutableStateOf(sets.lastOrNull()?.let {WorkoutFlow.afterSet(session,sets,it.exercise,it.kind).exercise?:it.exercise}?:choices.first())}
    var reps by rememberSaveable(session.id) {mutableStateOf("")}
    var weight by rememberSaveable(session.id) {mutableStateOf("")}
    var warmup by rememberSaveable(session.id) {mutableStateOf(false)}
    var showRpe by rememberSaveable(session.id) {mutableStateOf(false)}
    var rpe by rememberSaveable(session.id) {mutableStateOf("")}
    var guide by rememberSaveable(session.id) {mutableStateOf(false)}
    var allSets by rememberSaveable(session.id) {mutableStateOf(false)}
    var deadline by rememberSaveable(session.id) {mutableLongStateOf(0)}
    var timerDuration by rememberSaveable(session.id) {mutableIntStateOf(session.restSeconds)}
    var flowNotice by rememberSaveable(session.id) {mutableStateOf<Int?>(null)}
    var elapsed by remember {mutableLongStateOf(SystemClock.elapsedRealtime())}
    LaunchedEffect(deadline) {while(deadline>SystemClock.elapsedRealtime()) {elapsed=SystemClock.elapsedRealtime();delay(1000)};elapsed=SystemClock.elapsedRealtime()}
    val remaining=RestTimer.remainingSeconds(deadline,elapsed,timerDuration)
    val candidate=runCatching {TrainingSet(sessionId=session.id,exercise=exercise,reps=requireNotNull(NumericInput.integer(reps)),weightKg=requireNotNull(NumericInput.decimal(weight)),kind=if(warmup) SetKind.WARM_UP else SetKind.WORKING,rpe=if(rpe.isBlank()) null else requireNotNull(NumericInput.decimal(rpe)))}.getOrNull()
    InfoCard {
        Text(session.label.ifBlank {stringResource(R.string.active_session,session.templateId)},style=MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.session_status,number(sets.size.toDouble(),0),dateLabel(session.date)),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
        ExercisePicker(exercise,{next ->if(next!=exercise) {exercise=next;reps="";weight="";rpe="";warmup=false;flowNotice=null}},choices)
        flowNotice?.let {Text(stringResource(it),color=MaterialTheme.colorScheme.primary,modifier=Modifier.testTag("workout_flow_notice"))}
        session.prescriptions.firstOrNull {it.exercise==exercise}?.let {spec ->
            spec.workingSets?.let {target ->Text(stringResource(R.string.exercise_set_progress,number((WorkoutFlow.counts(session,sets)[exercise]?:0).toDouble(),0),number(target.toDouble(),0)),style=MaterialTheme.typography.titleMedium,modifier=Modifier.testTag("exercise_plan_progress"))}
            PrescriptionSummary(spec.copy(workingSets=null))
        }
        if(session.prescriptions.any {it.supersetGroup!=null}) Text(stringResource(R.string.linked_timing_note),style=MaterialTheme.typography.bodySmall)
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
        Text(stringResource(if(Equipment.DUMBBELLS in ExerciseCatalog.of(exercise).equipment) R.string.dumbbell_load_note else R.string.load_convention_note),style=MaterialTheme.typography.bodySmall)
        if(ExerciseCatalog.of(exercise).perSide) Text(stringResource(R.string.per_side_note),style=MaterialTheme.typography.bodySmall)
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            FilterChip(selected=warmup,onClick={warmup=!warmup},label={Text(stringResource(R.string.warmup_set))},modifier=Modifier.testTag("warmup_set"))
            TextButton(onClick={showRpe=!showRpe;if(!showRpe) rpe=""},modifier=Modifier.testTag("toggle_rpe")) {Text(stringResource(R.string.effort_toggle))}
        }
        if(showRpe) {
            Input(R.string.rpe_optional,rpe,{rpe=it},"set_rpe",error=rpe.isNotBlank()&&NumericInput.decimal(rpe)?.let {it in 0.0..10.0}!=true)
            Text(stringResource(R.string.rpe_note),style=MaterialTheme.typography.bodySmall)
        }
        Button(onClick={candidate?.let {value ->vm.logSet(value) {saved ->
            val next=WorkoutFlow.afterSet(session,saved,value.exercise,value.kind)
            flowNotice=when {next.exercise==null->R.string.user_plan_done;next.exercise!=value.exercise&&next.withinGroup->R.string.next_in_linked_group;next.exercise!=value.exercise->R.string.next_in_plan;else->null}
            val following=next.exercise
            if(following!=null&&following!=exercise) {exercise=following;reps="";weight="";warmup=false}
            rpe="";elapsed=SystemClock.elapsedRealtime()
            if(next.restSeconds>0) {timerDuration=next.restSeconds;deadline=elapsed+timerDuration*1000L} else deadline=0
        }}},enabled=candidate!=null&&!state.busy,modifier=Modifier.fillMaxWidth().heightIn(min=54.dp).testTag("add_set")) {Text(stringResource(R.string.add_set))}
        if(deadline>0) Surface(color=MaterialTheme.colorScheme.primaryContainer,shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.rest_countdown,number(remaining.toDouble(),0),number(timerDuration.toDouble(),0)),style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.onPrimaryContainer)
                LinearProgressIndicator(progress={remaining.toFloat()/timerDuration},modifier=Modifier.fillMaxWidth(),drawStopIndicator={})
                Row {
                    TextButton(onClick={elapsed=SystemClock.elapsedRealtime();deadline=elapsed+timerDuration*1000L}) {Text(stringResource(R.string.restart_timer))}
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
    var filterDay by rememberSaveable {mutableStateOf<Long?>(null)}
    TrainingExport(state,vm)
    WorkoutCalendarPanel(state.snapshot,today,filterDay?.let(LocalDate::ofEpochDay)) {filterDay=it?.toEpochDay();limit=10}
    if(filterDay==null) {
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
    }
    Text(stringResource(R.string.recent_sessions),style=MaterialTheme.typography.titleLarge)
    Input(R.string.search_history,search,{search=it;limit=10},"session_search",numeric=false)
    val names=Exercise.entries.associateWith {stringResource(exerciseLabel(it))}
    val sessions=state.snapshot.sessions.filter {it.completedAt!=null && (filterDay==null||it.date.toEpochDay()==filterDay) && (search.isBlank()||it.label.contains(search,true)||it.date.toString().contains(search)||state.snapshot.sets.any {set ->set.sessionId==it.id&&ExerciseSearch.matches(search,names.getValue(set.exercise)+" "+ExerciseCatalog.searchText(set.exercise))})}
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
            Text(stringResource(R.string.repeat_session_note),style=MaterialTheme.typography.bodySmall)
            val repeatFits=(session.plannedExercises+sets.map {it.exercise}).distinct().size in 1..20
            if(!repeatFits) Text(stringResource(R.string.repeat_limit_note),style=MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick={vm.repeatSession(session.id)},enabled=repeatFits&&!state.busy&&state.snapshot.sessions.none {it.completedAt==null},modifier=Modifier.testTag("repeat_session_${session.id}")) {Text(stringResource(R.string.repeat_session))}
            if(session.source==SessionSource.APPLE_WATCH_IMPORT) {
                Text(stringResource(R.string.watch_record_source))
                session.averageHeartRateBpm?.let {Text(stringResource(R.string.watch_heart_rate,number(it,0)))}
                session.activeEnergyKcal?.let {Text(stringResource(R.string.watch_energy,number(it,0)))}
            }
            DateChoice(session.date,today,{vm.moveSession(session.id,it)},"session_date_${session.id}")
            session.prescriptions.forEach {Text(stringResource(exerciseLabel(it.exercise)),style=MaterialTheme.typography.titleSmall);PrescriptionSummary(it)}
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
    var name by rememberSaveable(program?.id) {mutableStateOf(program?.name.orEmpty())}
    var selected by rememberSaveable(program?.id) {mutableStateOf(program?.exercises?:emptyList())}
    var rest by rememberSaveable(program?.id) {mutableStateOf((program?.restSeconds?:90).toString())}
    var detailsPayload by rememberSaveable(program?.id) {mutableStateOf(Json.encodeToString(program?.prescriptions?:emptyList<ExercisePrescription>()))}
    val details=Json.decodeFromString<List<ExercisePrescription>>(detailsPayload)
    var detailExercise by rememberSaveable(program?.id) {mutableStateOf<Exercise?>(null)}
    fun orderChanged(order:List<Exercise>) {detailsPayload=Json.encodeToString(PlanEditing.normalize(order,details));selected=order}
    var search by remember {mutableStateOf("")}
    var limit by remember {mutableIntStateOf(12)}
    val candidate=runCatching {TrainingProgram(id=program?.id?:java.util.UUID.randomUUID().toString(),name=name.trim(),exercises=selected,restSeconds=requireNotNull(NumericInput.integer(rest)),prescriptions=details)}.getOrNull()
    AlertDialog(onDismissRequest=onDismiss,title={Text(stringResource(R.string.saved_program))},text={Column(Modifier.heightIn(max=480.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Input(R.string.program_name,name,{name=it},"program_name",numeric=false)
        Input(R.string.rest_seconds,rest,{rest=it},"program_rest")
        Text(stringResource(R.string.program_note))
        Text(stringResource(R.string.plan_group_edit_note),style=MaterialTheme.typography.bodySmall)
        selected.forEachIndexed {index,exercise ->
            Text(stringResource(exerciseLabel(exercise)))
            details.firstOrNull {it.exercise==exercise}?.let {PrescriptionSummary(it)}
            TextButton(onClick={detailExercise=exercise},modifier=Modifier.testTag("plan_details_${exercise.name}")) {Text(stringResource(R.string.edit_plan_details))}
            if(index>0) {
                val group=details.firstOrNull {it.exercise==exercise}?.supersetGroup
                val previous=details.firstOrNull {it.exercise==selected[index-1]}?.supersetGroup
                GroupLink(group!=null&&group==previous,{linked ->detailsPayload=Json.encodeToString(PlanEditing.linkPrevious(selected,details,exercise,linked))},exercise)
            }
            Row {
                TextButton(onClick={orderChanged(selected.toMutableList().apply {java.util.Collections.swap(this,index,index-1)})},enabled=index>0) {Text(stringResource(R.string.move_up))}
                TextButton(onClick={orderChanged(selected.toMutableList().apply {java.util.Collections.swap(this,index,index+1)})},enabled=index<selected.lastIndex) {Text(stringResource(R.string.move_down))}
                TextButton(onClick={orderChanged(selected-exercise)}) {Text(stringResource(R.string.remove))}
            }
        }
        Input(R.string.search_exercises,search,{search=it;limit=12},"program_exercise_search",numeric=false)
        val names=Exercise.entries.associateWith {stringResource(exerciseLabel(it))}
        val matches=Exercise.entries.filter {ExerciseSearch.matches(search,names.getValue(it)+" "+ExerciseCatalog.searchText(it))}
        matches.take(limit).forEach {exercise -> FilterChip(selected=exercise in selected,onClick={orderChanged(if(exercise in selected) selected-exercise else selected+exercise)},enabled=exercise in selected||selected.size<20,label={Text(stringResource(exerciseLabel(exercise)))},modifier=Modifier.testTag("program_exercise_${exercise.name}"))}
        if(matches.size>limit) TextButton(onClick={limit+=12}) {Text(stringResource(R.string.show_more))}
    }},confirmButton={TextButton(onClick={candidate?.let(onSave)},enabled=candidate!=null&&!busy,modifier=Modifier.testTag("save_program")) {Text(stringResource(R.string.save))}},dismissButton={TextButton(onClick=onDismiss) {Text(stringResource(R.string.cancel))}})
    detailExercise?.let {exercise ->PrescriptionEditor(details.firstOrNull {it.exercise==exercise}?:ExercisePrescription(exercise),busy,{detailExercise=null}) {value ->detailsPayload=Json.encodeToString(details.filter {it.exercise!=exercise}+value);detailExercise=null}}
}
