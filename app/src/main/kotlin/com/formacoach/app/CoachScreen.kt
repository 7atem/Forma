package com.formacoach.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.formacoach.coach.*
import com.formacoach.model.*
import java.time.LocalDate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
internal fun CoachContent(state:AppState,today:LocalDate,vm:FitnessViewModel) {
    var query by rememberSaveable {mutableStateOf("")}
    var question by rememberSaveable {mutableStateOf(CoachQuestion.WEEK)}
    var available by rememberSaveable {mutableIntStateOf(state.snapshot.profile?.sessionsPerWeek?:3)}
    var complete by rememberSaveable {mutableStateOf(false)}
    var notes by rememberSaveable {mutableStateOf("")}
    PageHeading(R.string.coach_title)
    Text(stringResource(R.string.coach_scope),style=MaterialTheme.typography.bodyMedium)
    LocalAiPanel(state,today,vm)
    InfoCard {
        Input(R.string.coach_question,query,{query=it},"coach_question",numeric=false)
        Button(onClick={question=CoachQuestions.resolve(query)},modifier=Modifier.testTag("ask_coach")) {Text(stringResource(R.string.ask))}
        listOf(CoachQuestion.WEEK,CoachQuestion.TRAINING,CoachQuestion.FOOD,CoachQuestion.WEIGHT).forEach {value ->
            FilterChip(selected=question==value,onClick={question=value},label={Text(stringResource(questionLabel(value)))})
        }
    }
    if(question==CoachQuestion.UNSUPPORTED) Text(stringResource(R.string.unsupported_coach)) else {
        if(question==CoachQuestion.WEIGHT) Text(stringResource(R.string.weight_uncertainty))
        WeeklyReviewCard(state.snapshot,today,vm)
        val review=vm.reviewEngine.review(state.snapshot,today)
        InfoCard {
            Text(stringResource(R.string.review_evidence),style=MaterialTheme.typography.titleLarge)
            if(question==CoachQuestion.WEEK||question==CoachQuestion.TRAINING) state.snapshot.sessions.filter {it.id in review.sessionEvidence}.forEach {session ->
                Text(stringResource(R.string.session_evidence,dateLabel(session.date),session.label.ifBlank {session.templateId},number(state.snapshot.sets.count {it.sessionId==session.id}.toDouble(),0)))
            }
            if(question==CoachQuestion.WEEK||question==CoachQuestion.FOOD) state.snapshot.meals.filter {it.id in review.mealEvidence}.forEach {meal ->
                Text(stringResource(R.string.meal_evidence,dateLabel(meal.date),meal.name,number(meal.calories,0)))
            }
            if(question==CoachQuestion.WEEK||question==CoachQuestion.WEIGHT) state.snapshot.measurements.filter {it.date in review.measurementEvidence+review.comparisonMeasurementEvidence}.forEach {measurement ->
                measurement.weightKg?.let {Text(stringResource(R.string.measurement_evidence,dateLabel(measurement.date),number(it)))}
            }
            if(review.sessionEvidence.isEmpty()&&review.mealEvidence.isEmpty()&&review.measurementEvidence.isEmpty()) Text(stringResource(R.string.no_evidence))
        }
    }
    InfoCard {
        Text(stringResource(R.string.check_in),style=MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.check_in_note))
        Row {(2..4).forEach {count ->FilterChip(selected=available==count,onClick={available=count},label={Text(number(count.toDouble(),0))},modifier=Modifier.testTag("available_sessions_$count"))}}
        Row {Checkbox(checked=complete,onCheckedChange={complete=it});Text(stringResource(R.string.complete_food_log))}
        Input(R.string.check_in_notes,notes,{notes=it},"check_in_notes",numeric=false)
        Button(onClick={vm.checkIn(WeeklyCheckIn(date=today,availableSessions=available,foodLogComplete=complete,notes=notes)){notes=""}},enabled=!state.busy&&notes.length<=2000,modifier=Modifier.testTag("save_check_in")) {Text(stringResource(R.string.save_check_in))}
    }
    state.snapshot.proposals.filter {it.status==ProposalStatus.PENDING}.forEach {proposal -> InfoCard {
        Text(stringResource(R.string.pending_plan_change),style=MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.plan_change,number(proposal.baseSessions.toDouble(),0),number(proposal.proposedSessions.toDouble(),0)))
        val source=state.snapshot.checkIns.firstOrNull {it.id==proposal.checkInId}
        source?.let {Text(stringResource(R.string.proposal_source,dateLabel(it.date)))}
        Text(stringResource(R.string.proposal_scope),style=MaterialTheme.typography.bodySmall)
        Button(onClick={vm.acceptProposal(proposal.id)},enabled=!state.busy,modifier=Modifier.testTag("accept_proposal")) {Text(stringResource(R.string.confirm))}
        OutlinedButton(onClick={vm.dismissProposal(proposal.id)},enabled=!state.busy,modifier=Modifier.testTag("dismiss_proposal")) {Text(stringResource(R.string.cancel))}
    }}
    Text(stringResource(R.string.check_in_history),style=MaterialTheme.typography.titleLarge)
    state.snapshot.checkIns.take(10).forEach {checkIn -> InfoCard {
        Text(dateLabel(checkIn.date))
        Text(stringResource(R.string.availability_value,number(checkIn.availableSessions.toDouble(),0)))
        Text(stringResource(if(checkIn.foodLogComplete) R.string.food_complete_reported else R.string.food_incomplete_reported))
        if(checkIn.notes.isNotBlank()) Text(checkIn.notes)
    }}
}

@Composable
private fun LocalAiPanel(state:AppState,today:LocalDate,vm:FitnessViewModel) {
    val owner=LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer=LifecycleEventObserver {_,event ->if(event==Lifecycle.Event.ON_STOP) vm.cancelAi()}
        owner.lifecycle.addObserver(observer)
        onDispose {owner.lifecycle.removeObserver(observer);vm.cancelAi()}
    }
    InfoCard {
        Text(stringResource(R.string.local_ai),style=MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.local_ai_scope),style=MaterialTheme.typography.bodySmall)
        Text(stringResource(modelStatusLabel(state.modelAvailability)),modifier=Modifier.testTag("model_status"))
        OutlinedButton(onClick={vm.checkModel()},enabled=!state.aiBusy,modifier=Modifier.testTag("check_local_model")) {Text(stringResource(R.string.check_local_model))}
        if(state.modelAvailability==ModelAvailability.DOWNLOADABLE) {
            Text(stringResource(R.string.model_download_note))
            Button(onClick={vm.downloadModel()},enabled=!state.aiBusy) {Text(stringResource(R.string.download_device_model))}
        }
        if(state.modelAvailability==ModelAvailability.READY) Button(onClick={vm.selectHighlights {owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)}},enabled=!state.aiBusy,modifier=Modifier.testTag("select_highlights")) {Text(stringResource(R.string.select_highlights))}
        if(state.aiBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if(state.aiFailed) Text(stringResource(R.string.ai_fallback),modifier=Modifier.testTag("ai_fallback"))
        val review=vm.reviewEngine.review(state.snapshot,today)
        state.highlights.forEach {id ->when(id) {
            "training" -> Text(stringResource(R.string.review_sessions,number(review.completedSessions.toDouble(),0)))
            "food" -> Text(stringResource(R.string.review_meal_days,number(review.mealDays.toDouble(),0)))
            "weight" -> review.meanWeight?.let {Text(stringResource(R.string.review_weight,number(it),number(review.weightObservations.toDouble(),0)))}
            "coverage" -> Text(stringResource(R.string.logged_only))
        }}
    }
}

private fun modelStatusLabel(status:ModelAvailability)=when(status) {
    ModelAvailability.NOT_CHECKED -> R.string.model_not_checked
    ModelAvailability.UNAVAILABLE -> R.string.model_unavailable
    ModelAvailability.DOWNLOADABLE -> R.string.model_downloadable
    ModelAvailability.DOWNLOADING -> R.string.model_downloading
    ModelAvailability.READY -> R.string.model_ready
    ModelAvailability.ERROR -> R.string.model_error
}

private fun questionLabel(question:CoachQuestion)=when(question) {
    CoachQuestion.WEEK -> R.string.coach_week
    CoachQuestion.TRAINING -> R.string.coach_training
    CoachQuestion.FOOD -> R.string.coach_food
    CoachQuestion.WEIGHT -> R.string.coach_weight
    CoachQuestion.UNSUPPORTED -> R.string.unsupported_coach
}
