package com.formacoach.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.formacoach.coach.*
import com.formacoach.model.*
import java.time.LocalDate

@Composable
internal fun CoachContent(state: AppState, today: LocalDate, vm: FitnessViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var question by rememberSaveable { mutableStateOf(CoachQuestion.WEEK) }
    var available by rememberSaveable { mutableIntStateOf(state.snapshot.profile?.sessionsPerWeek ?: 3) }
    var complete by rememberSaveable { mutableStateOf(false) }
    var notes by rememberSaveable { mutableStateOf("") }

    PageHeading(R.string.coach_title, R.string.coach_scope)
    LocalAiPanel(state, today, vm)

    InfoCard {
        Input(R.string.coach_question, query, { query = it }, "coach_question", numeric = false)
        Button(
            onClick = { question = CoachQuestions.resolve(query) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("ask_coach"),
        ) { Text(stringResource(R.string.ask), fontWeight = FontWeight.Bold) }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(CoachQuestion.WEEK, CoachQuestion.TRAINING, CoachQuestion.FOOD, CoachQuestion.WEIGHT).forEach { value ->
                FilterChip(
                    selected = question == value,
                    onClick = { question = value },
                    label = { Text(stringResource(questionLabel(value))) },
                )
            }
        }
    }

    if (question == CoachQuestion.UNSUPPORTED) {
        InfoCard { Text(stringResource(R.string.unsupported_coach)) }
    } else {
        if (question == CoachQuestion.WEIGHT) {
            Text(stringResource(R.string.weight_uncertainty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        WeeklyReviewCard(state.snapshot, today, vm)
        val review = vm.reviewEngine.review(state.snapshot, today)
        InfoCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.review_evidence), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                SectionInfoButton(InfoTopic.REVIEW_EVIDENCE)
            }
            if (question == CoachQuestion.WEEK || question == CoachQuestion.TRAINING) {
                state.snapshot.sessions.filter { it.id in review.sessionEvidence }.forEach { session ->
                    Text(
                        stringResource(
                            R.string.session_evidence,
                            dateLabel(session.date),
                            session.label.ifBlank { session.templateId },
                            number(state.snapshot.sets.count { it.sessionId == session.id }.toDouble(), 0),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (question == CoachQuestion.WEEK || question == CoachQuestion.FOOD) {
                state.snapshot.meals.filter { it.id in review.mealEvidence }.forEach { meal ->
                    Text(
                        stringResource(R.string.meal_evidence, dateLabel(meal.date), meal.name, number(meal.calories, 0)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (question == CoachQuestion.WEEK || question == CoachQuestion.WEIGHT) {
                state.snapshot.measurements.filter { it.date in review.measurementEvidence + review.comparisonMeasurementEvidence }.forEach { measurement ->
                    measurement.weightKg?.let {
                        Text(stringResource(R.string.measurement_evidence, dateLabel(measurement.date), formatWeight(it)), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (review.sessionEvidence.isEmpty() && review.mealEvidence.isEmpty() && review.measurementEvidence.isEmpty()) {
                Text(stringResource(R.string.no_evidence), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    InfoCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.check_in), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            SectionInfoButton(InfoTopic.CHECK_IN)
        }
        Text(stringResource(R.string.check_in_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            (2..4).forEach { count ->
                FilterChip(
                    selected = available == count,
                    onClick = { available = count },
                    label = { Text(number(count.toDouble(), 0), fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("available_sessions_$count"),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Checkbox(checked = complete, onCheckedChange = { complete = it })
            Text(stringResource(R.string.complete_food_log), style = MaterialTheme.typography.bodyMedium)
        }
        Input(R.string.check_in_notes, notes, { notes = it }, "check_in_notes", numeric = false)
        Button(
            onClick = { vm.checkIn(WeeklyCheckIn(date = today, availableSessions = available, foodLogComplete = complete, notes = notes)) { notes = "" } },
            enabled = !state.busy && notes.length <= 2000,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("save_check_in"),
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.save_check_in), fontWeight = FontWeight.Bold)
        }
    }

    state.snapshot.proposals.filter { it.status == ProposalStatus.PENDING }.forEach { proposal ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
            shape = RoundedCornerShape(22.dp),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.pending_plan_change), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    SectionInfoButton(InfoTopic.PLAN_PROPOSAL)
                }
                Text(
                    stringResource(R.string.plan_change, number(proposal.baseSessions.toDouble(), 0), number(proposal.proposedSessions.toDouble(), 0)),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                val source = state.snapshot.checkIns.firstOrNull { it.id == proposal.checkInId }
                source?.let { Text(stringResource(R.string.proposal_source, dateLabel(it.date)), style = MaterialTheme.typography.bodySmall) }
                Text(stringResource(R.string.proposal_scope), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { vm.acceptProposal(proposal.id) },
                        enabled = !state.busy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).testTag("accept_proposal"),
                    ) { Text(stringResource(R.string.confirm), fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = { vm.dismissProposal(proposal.id) },
                        enabled = !state.busy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).testTag("dismiss_proposal"),
                    ) { Text(stringResource(R.string.cancel)) }
                }
            }
        }
    }

    if (state.snapshot.checkIns.isNotEmpty()) {
        Text(stringResource(R.string.check_in_history), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        state.snapshot.checkIns.take(10).forEach { checkIn ->
            InfoCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(dateLabel(checkIn.date), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    PillBadge(stringResource(R.string.availability_value, number(checkIn.availableSessions.toDouble(), 0)))
                }
                Text(
                    stringResource(if (checkIn.foodLogComplete) R.string.food_complete_reported else R.string.food_incomplete_reported),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (checkIn.notes.isNotBlank()) Text(checkIn.notes, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun LocalAiPanel(state: AppState, today: LocalDate, vm: FitnessViewModel) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) vm.cancelAi() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); vm.cancelAi() }
    }
    InfoCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.local_ai), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            SectionInfoButton(InfoTopic.LOCAL_AI)
        }
        Text(stringResource(R.string.local_ai_scope), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PillBadge(stringResource(modelStatusLabel(state.modelAvailability)), modifier = Modifier.testTag("model_status"))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { vm.checkModel() },
                enabled = !state.aiBusy,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("check_local_model"),
            ) { Text(stringResource(R.string.check_local_model)) }
            if (state.modelAvailability == ModelAvailability.READY) {
                Button(
                    onClick = { vm.selectHighlights { owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) } },
                    enabled = !state.aiBusy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("select_highlights"),
                ) { Text(stringResource(R.string.select_highlights)) }
            }
        }
        if (state.modelAvailability == ModelAvailability.DOWNLOADABLE) {
            Text(stringResource(R.string.model_download_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { vm.downloadModel() }, enabled = !state.aiBusy, shape = RoundedCornerShape(12.dp)) {
                Text(stringResource(R.string.download_device_model))
            }
        }
        if (state.aiBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (state.aiFailed) Text(stringResource(R.string.ai_fallback), color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("ai_fallback"))
        val review = vm.reviewEngine.review(state.snapshot, today)
        state.highlights.forEach { id ->
            when (id) {
                "training" -> Text(stringResource(R.string.review_sessions, number(review.completedSessions.toDouble(), 0)))
                "food" -> Text(stringResource(R.string.review_meal_days, number(review.mealDays.toDouble(), 0)))
                "weight" -> review.meanWeight?.let { Text(stringResource(R.string.review_weight, formatWeight(it, 1), number(review.weightObservations.toDouble(), 0))) }
                "coverage" -> Text(stringResource(R.string.logged_only))
            }
        }
    }
}

private fun modelStatusLabel(status: ModelAvailability) = when (status) {
    ModelAvailability.NOT_CHECKED -> R.string.model_not_checked
    ModelAvailability.UNAVAILABLE -> R.string.model_unavailable
    ModelAvailability.DOWNLOADABLE -> R.string.model_downloadable
    ModelAvailability.DOWNLOADING -> R.string.model_downloading
    ModelAvailability.READY -> R.string.model_ready
    ModelAvailability.ERROR -> R.string.model_error
}

private fun questionLabel(question: CoachQuestion) = when (question) {
    CoachQuestion.WEEK -> R.string.coach_week
    CoachQuestion.TRAINING -> R.string.coach_training
    CoachQuestion.FOOD -> R.string.coach_food
    CoachQuestion.WEIGHT -> R.string.coach_weight
    CoachQuestion.UNSUPPORTED -> R.string.unsupported_coach
}
