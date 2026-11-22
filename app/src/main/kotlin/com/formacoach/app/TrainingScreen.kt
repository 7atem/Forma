package com.formacoach.app

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.formacoach.coach.*
import com.formacoach.model.*
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun TrainContent(state: AppState, today: LocalDate, vm: FitnessViewModel, scrollState: ScrollState, onSetDeleted: (TrainingSet, List<String>) -> Unit = { _, _ -> }) {
    var section by rememberSaveable { mutableIntStateOf(0) }
    var day by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var editorPayload by rememberSaveable { mutableStateOf<String?>(null) }
    val editor = editorPayload?.let { Json.decodeFromString<TrainingProgram>(it) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var justFinished by rememberSaveable { mutableStateOf(false) }
    val active = state.snapshot.sessions.firstOrNull { it.completedAt == null }
    if (section == 0) {
        PageHeading(if (active == null) R.string.training_title else R.string.training_title_active, if (active == null) R.string.training_intro else R.string.session_focus)
    } else {
        PageHeading(if (section == 1) R.string.exercise_library else R.string.training_history)
    }
    val pages = rememberSaveableStateHolder()
    LaunchedEffect(active?.id) { if (active != null) section = 0 }
    LaunchedEffect(section, active?.id) { scrollState.scrollTo(0) }
    SegmentedTabs(
        selectedIndex = section,
        labels = listOf(R.string.my_programs, R.string.exercise_library, R.string.training_history),
        onSelect = { section = it },
        tagPrefix = "training_section",
    )
    pages.SaveableStateProvider(section) {
        when (section) {
            0 -> if (active != null) {
                SessionForm(active, state, vm, onSetDeleted = onSetDeleted) { justFinished = true; section = 2 }
            } else {
                val date = LocalDate.ofEpochDay(day)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DateChoice(date, today, { day = it.toEpochDay() }, "training_date")
                    Spacer(Modifier.weight(1f))
                    FilledTonalButton(
                        onClick = { creating = true },
                        enabled = !state.busy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("new_program"),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.new_program), fontWeight = FontWeight.SemiBold)
                    }
                }
                state.snapshot.programs.forEach { program ->
                    ProgramCard(
                        program.name,
                        program.exercises,
                        "start_${program.id}",
                        state.busy,
                        { vm.start(program.id, date) },
                    ) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { editorPayload = Json.encodeToString(program) }) {
                                Text(stringResource(R.string.edit))
                            }
                            TextButton(
                                onClick = { editorPayload = Json.encodeToString(program.copy(id = java.util.UUID.randomUUID().toString())) },
                                enabled = !state.busy,
                                modifier = Modifier.testTag("duplicate_${program.id}"),
                            ) { Text(stringResource(R.string.duplicate_program)) }
                            ConfirmButton(R.string.remove, stringResource(R.string.delete_program_note), !state.busy) {
                                vm.deleteProgram(program.id)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.starter_programs), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    ScopeInfoButton(R.string.starter_programs, R.string.template_note)
                }
                StarterTemplates.all.forEach { template ->
                    ProgramCard(
                        stringResource(if (template.id == "A") R.string.template_a else R.string.template_b),
                        template.exercises,
                        "start_${template.id}",
                        state.busy,
                        { vm.start(template.id, date) },
                    )
                }
            }
            1 -> ExerciseLibrary(state, vm)
            2 -> TrainingHistoryContent(state, today, vm, showVictory = justFinished, onDismissVictory = { justFinished = false })
        }
    }
    // Plate maths only helps barbell work: show it during a workout only when one is involved.
    val activeExercises = active?.let { session ->
        session.plannedExercises + state.snapshot.sets.filter { it.sessionId == session.id }.map { it.exercise }
    }
    if (section == 0 && (activeExercises == null || activeExercises.any { Equipment.BARBELL in ExerciseCatalog.of(it).equipment })) {
        PlateCalculatorButton()
    }
    if (creating || editor != null) {
        ProgramDialog(editor, state.busy, { creating = false; editorPayload = null }) {
            vm.saveProgram(it) { creating = false; editorPayload = null }
        }
    }
}

@Composable
private fun ProgramCard(
    name: String,
    exercises: List<Exercise>,
    tag: String,
    busy: Boolean,
    onStart: () -> Unit,
    actions: @Composable () -> Unit = {},
) {
    InfoCard(Modifier.testTag("card_$tag")) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (exercises.isNotEmpty()) {
                ExerciseThumbnail(
                    exercises.first(),
                    Modifier.width(104.dp).clip(RoundedCornerShape(14.dp)),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                PillBadge(stringResource(R.string.program_exercise_count, number(exercises.size.toDouble(), 0)))
            }
        }
        Text(
            exercises.map { stringResource(exerciseLabel(it)) }.joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onStart,
            enabled = !busy,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag(tag),
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.start_session), fontWeight = FontWeight.Bold)
        }
        actions()
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun SessionForm(session: Session, state: AppState, vm: FitnessViewModel, onSetDeleted: (TrainingSet, List<String>) -> Unit = { _, _ -> }, onFinished: () -> Unit) {
    val choices = (session.plannedExercises + Exercise.entries).distinct()
    val sets = state.snapshot.sets.filter { it.sessionId == session.id }
    val workingCounts = remember(session, sets) { WorkoutFlow.counts(session, sets) }
    var exercise by rememberSaveable(session.id) {
        mutableStateOf(
            sets.lastOrNull()?.let { WorkoutFlow.afterSet(session, sets, it.exercise, it.kind).exercise ?: it.exercise }
                ?: choices.first(),
        )
    }
    var reps by rememberSaveable(session.id) { mutableStateOf("") }
    var weight by rememberSaveable(session.id) { mutableStateOf("") }
    var warmup by rememberSaveable(session.id) { mutableStateOf(false) }
    var showRpe by rememberSaveable(session.id) { mutableStateOf(false) }
    var rpe by rememberSaveable(session.id) { mutableStateOf("") }
    var guide by rememberSaveable(session.id) { mutableStateOf(false) }
    var allSets by rememberSaveable(session.id) { mutableStateOf(false) }
    var deadline by rememberSaveable(session.id) { mutableLongStateOf(0) }
    var timerDuration by rememberSaveable(session.id) { mutableIntStateOf(session.restSeconds) }
    var flowNotice by rememberSaveable(session.id) { mutableStateOf<Int?>(null) }
    var elapsed by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(deadline) {
        while (deadline > SystemClock.elapsedRealtime()) {
            elapsed = SystemClock.elapsedRealtime()
            delay(1000)
        }
        elapsed = SystemClock.elapsedRealtime()
    }
    val unit = LocalWeightUnit.current
    val unitLabel = weightUnitLabel()
    val locale = LocalConfiguration.current.locales[0]
    var inputUnit by rememberSaveable(session.id) { mutableStateOf(unit) }
    LaunchedEffect(unit) {
        if (inputUnit != unit) {
            NumericInput.decimal(weight)?.let { parsed ->
                weight = formatInputNumber(WeightUnits.convert(parsed, inputUnit, unit), locale, 2)
            }
            inputUnit = unit
        }
    }
    // Start from this exercise's last recorded working set (this session first, then history).
    // Values are only a convenience: nothing is saved until the user taps Save set.
    LaunchedEffect(session.id, exercise) {
        if (reps.isBlank() && weight.isBlank()) {
            val source = sets.lastOrNull { it.exercise == exercise && it.kind == SetKind.WORKING }
                ?: state.snapshot.sessions
                    .filter { it.completedAt != null && it.date <= session.date }
                    .sortedWith(compareByDescending<Session> { it.date }.thenByDescending { it.completedAt })
                    .firstNotNullOfOrNull { record ->
                        state.snapshot.sets.lastOrNull { it.sessionId == record.id && it.exercise == exercise && it.kind == SetKind.WORKING }
                    }
            source?.let {
                reps = formatInputNumber(it.reps.toDouble(), locale, 0)
                weight = formatInputNumber(WeightUnits.fromKg(it.weightKg, unit), locale, 2)
                inputUnit = unit
            }
        }
    }
    val remaining = RestTimer.remainingSeconds(deadline, elapsed, timerDuration)
    val candidate = runCatching {
        TrainingSet(
            sessionId = session.id,
            exercise = exercise,
            reps = requireNotNull(NumericInput.integer(reps)),
            weightKg = WeightUnits.toKg(requireNotNull(NumericInput.decimal(weight)), unit),
            kind = if (warmup) SetKind.WARM_UP else SetKind.WORKING,
            rpe = if (rpe.isBlank()) null else requireNotNull(NumericInput.decimal(rpe)),
        )
    }.getOrNull()

    fun selectExercise(next: Exercise) {
        if (next != exercise) {
            exercise = next
            reps = ""
            weight = ""
            rpe = ""
            warmup = false
            flowNotice = null
        }
    }

    // Active Session Header Banner
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.active_session, sessionTitle(session, state.snapshot.programs)),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    stringResource(R.string.session_status, number(sets.size.toDouble(), 0), dateLabel(session.date)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                val tonnage = TrainingHistory.sessionTonnage(sets)
                if (tonnage > 0.0) {
                    Text(
                        stringResource(R.string.session_tonnage, formatWeight(tonnage, 0)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    )
                }
            }
            if (deadline > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LivePulseDot(MaterialTheme.colorScheme.primary)
                    PillBadge(
                        text = "${number(remaining.toDouble(), 0)}s",
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }

    // 1-Tap Horizontal Planned Exercise Strip
    if (session.plannedExercises.isNotEmpty()) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            session.plannedExercises.forEach { planned ->
                val spec = session.prescriptions.firstOrNull { it.exercise == planned }
                val done = workingCounts[planned] ?: 0
                val target = spec?.workingSets
                val isComplete = target != null && done >= target
                val isCurrent = planned == exercise
                FilterChip(
                    selected = isCurrent,
                    onClick = { selectExercise(planned) },
                    leadingIcon = when {
                        isComplete -> ({ Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) })
                        spec?.supersetGroup != null -> ({ Icon(Icons.Default.Link, null, modifier = Modifier.size(16.dp)) })
                        else -> null
                    },
                    label = {
                        val badge = if (target != null) " $done/$target" else if (done > 0) " ($done)" else ""
                        Text(
                            stringResource(exerciseLabel(planned)) + badge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        )
                    },
                )
            }
        }
    }

    // Main Set Logging Card
    InfoCard {
        ExercisePicker(exercise, ::selectExercise, choices)
        flowNotice?.let {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("workout_flow_notice"),
            ) {
                Text(
                    stringResource(it),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
        session.prescriptions.firstOrNull { it.exercise == exercise }?.let { spec ->
            spec.workingSets?.let { target ->
                Text(
                    stringResource(
                        R.string.exercise_set_progress,
                        number((workingCounts[exercise] ?: 0).toDouble(), 0),
                        number(target.toDouble(), 0),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("exercise_plan_progress"),
                )
            }
            PrescriptionSummary(spec.copy(workingSets = null))
        }
        if (session.prescriptions.any { it.supersetGroup != null }) {
            Text(stringResource(R.string.linked_timing_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Compact Form Guide Banner
        Surface(
            onClick = { guide = true },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.7f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth().testTag("session_form_guide"),
        ) {
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExerciseThumbnail(exercise, Modifier.width(96.dp).clip(RoundedCornerShape(10.dp)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.view_form), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(guideResources(exercise).variant),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Previous Set Quick Copy
        val previous = state.snapshot.sessions
            .filter { it.completedAt != null && it.date <= session.date }
            .sortedWith(compareByDescending<Session> { it.date }.thenByDescending { it.completedAt })
            .firstNotNullOfOrNull { record -> state.snapshot.sets.lastOrNull { it.sessionId == record.id && it.exercise == exercise } }
        previous?.let { last ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        stringResource(R.string.previous_set, number(last.reps.toDouble(), 0), formatLoad(last.weightKg, last.exercise)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            reps = formatInputNumber(last.reps.toDouble(), locale, 0)
                            weight = formatInputNumber(WeightUnits.fromKg(last.weightKg, unit), locale, 2)
                            inputUnit = unit
                            warmup = last.kind == SetKind.WARM_UP
                            rpe = last.rpe?.let { formatInputNumber(it, locale, 1) }.orEmpty()
                            showRpe = last.rpe != null
                        },
                    ) { Text(stringResource(R.string.copy_previous_set), fontWeight = FontWeight.SemiBold) }
                }
            }
        }

        // Reps & Weight with Gym-Ready Steppers + Ghost Previous-Set Hints
        FormFieldPair(
            first = {
                StepperInput(
                    label = R.string.reps,
                    value = reps,
                    onChange = { reps = it },
                    tag = "set_reps",
                    step = 1.0,
                    min = 1.0,
                    max = 1000.0,
                    integer = true,
                    error = reps.isNotBlank() && NumericInput.integer(reps)?.let { it in 1..1000 } != true,
                    placeholder = previous?.reps?.toString(),
                )
            },
            second = {
                StepperInput(
                    label = if (unit == WeightUnit.KG) R.string.weight_kg else R.string.weight_lb,
                    value = weight,
                    onChange = { weight = it },
                    tag = "set_weight",
                    step = WeightUnits.step(unit),
                    min = 0.0,
                    max = if (unit == WeightUnit.KG) 1000.0 else 2200.0,
                    integer = false,
                    error = weight.isNotBlank() && NumericInput.decimal(weight)?.let { WeightUnits.toKg(it, unit) in 0.0..1000.0 } != true,
                    placeholder = previous?.let { formatWeight(it.weightKg) },
                )
            },
        )

        // Quick Rep & Weight Preset Chips + Live Barbell Preview
        val haptics = LocalHapticFeedback.current
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(5, 8, 10, 12).forEach { preset ->
                AssistChip(
                    onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); reps = preset.toString() },
                    label = { Text("${number(preset.toDouble(), 0)}×", fontWeight = FontWeight.SemiBold) },
                )
            }
            WeightUnits.quickBumps(unit).forEach { bump ->
                AssistChip(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val cur = NumericInput.decimal(weight) ?: 0.0
                        val cap = if (unit == WeightUnit.KG) 1000.0 else 2200.0
                        val next = (cur + bump).coerceAtMost(cap)
                        weight = formatInputNumber(next, locale, 2)
                    },
                    label = { Text("+${number(bump)} $unitLabel", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) },
                )
            }
        }
        val parsedWeight = NumericInput.decimal(weight)?.let { WeightUnits.toKg(it, unit) }
        val parsedReps = NumericInput.integer(reps)
        if (!warmup && parsedWeight != null && TrainingHistory.isNewPr(state.snapshot, exercise, parsedWeight, parsedReps, session.date)) {
            PillBadge(
                text = "★ " + stringResource(R.string.new_pr_badge),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.secondary,
            )
        }
        if (Equipment.BARBELL in ExerciseCatalog.of(exercise).equipment && parsedWeight != null && parsedWeight >= 20.0) {
            val stdPairs = mapOf(25.0 to 4, 20.0 to 4, 15.0 to 4, 10.0 to 4, 5.0 to 4, 2.5 to 4, 1.25 to 4)
            val preview = remember(parsedWeight) { runCatching { PlateCalculator.calculate(parsedWeight, 20.0, stdPairs) }.getOrNull() }
            preview?.let { BarbellVisualizer(it.perSide) }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(
                selected = warmup,
                onClick = { warmup = !warmup },
                label = { Text(stringResource(R.string.warmup_set)) },
                modifier = Modifier.testTag("warmup_set"),
            )
            FilterChip(
                selected = showRpe,
                onClick = { showRpe = !showRpe; if (!showRpe) rpe = "" },
                label = { Text(stringResource(R.string.effort_toggle)) },
                modifier = Modifier.testTag("toggle_rpe"),
            )
        }
        if (showRpe) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("7" to "3 RIR", "8" to "2 RIR", "9" to "1 RIR", "10" to "0 RIR").forEach { (valStr, rir) ->
                    FilterChip(
                        selected = rpe == valStr,
                        onClick = { rpe = valStr },
                        label = { Text("RPE $valStr · $rir") },
                    )
                }
            }
            Input(
                R.string.rpe_optional,
                rpe,
                { rpe = it },
                "set_rpe",
                error = rpe.isNotBlank() && NumericInput.decimal(rpe)?.let { it in 0.0..10.0 } != true,
            )
            Text(stringResource(R.string.rpe_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Button(
            onClick = {
                if (vm.state.value.busy) return@Button
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                candidate?.let { value ->
                    vm.logSet(value) { saved ->
                        val next = WorkoutFlow.afterSet(session, saved, value.exercise, value.kind)
                        flowNotice = when {
                            next.exercise == null -> R.string.user_plan_done
                            next.exercise != value.exercise && next.withinGroup -> R.string.next_in_linked_group
                            next.exercise != value.exercise -> R.string.next_in_plan
                            else -> null
                        }
                        val following = next.exercise
                        if (following != null && following != exercise) {
                            exercise = following
                            reps = ""
                            weight = ""
                            warmup = false
                        }
                        rpe = ""
                        elapsed = SystemClock.elapsedRealtime()
                        if (next.restSeconds > 0) {
                            timerDuration = next.restSeconds
                            deadline = elapsed + timerDuration * 1000L
                        } else {
                            deadline = 0
                        }
                    }
                }
            },
            enabled = candidate != null && !state.busy,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).testTag("add_set"),
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.add_set), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        // Sleek Radial Rest Timer Card
        if (deadline > 0) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    RestCountdownRing(remainingSeconds = remaining, totalSeconds = timerDuration)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.rest_countdown, number(remaining.toDouble(), 0), number(timerDuration.toDouble(), 0)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    deadline = (deadline - 15_000L).coerceAtLeast(SystemClock.elapsedRealtime())
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                            ) { Text("-15s", fontWeight = FontWeight.Bold) }
                            FilledTonalButton(
                                onClick = {
                                    timerDuration += 15
                                    deadline += 15_000L
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                            ) { Text("+15s", fontWeight = FontWeight.Bold) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { elapsed = SystemClock.elapsedRealtime(); deadline = elapsed + timerDuration * 1000L }) {
                                Text(stringResource(R.string.restart_timer))
                            }
                            TextButton(onClick = { deadline = 0 }) {
                                Text(stringResource(R.string.stop_timer))
                            }
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val convention = if (Equipment.DUMBBELLS in ExerciseCatalog.of(exercise).equipment) R.string.dumbbell_load_note else R.string.load_convention_note
            Text(stringResource(convention), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            if (ExerciseCatalog.of(exercise).perSide) {
                Text(stringResource(R.string.per_side_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            }
        }
    }

    if (sets.isNotEmpty()) {
        InfoCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.logged_sets), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                PillBadge(number(sets.size.toDouble(), 0))
            }
            (if (allSets) sets else sets.takeLast(4)).forEach { setItem ->
                SetCard(setItem, state.busy, true, vm, onRemoved = { deleted -> onSetDeleted(deleted, sets.map { it.id }) })
            }
            if (sets.size > 4) {
                TextButton(onClick = { allSets = !allSets }) {
                    Text(stringResource(if (allSets) R.string.show_less else R.string.show_more))
                }
            }
        }
    }

    Button(
        onClick = { vm.finish(session.id, onFinished) },
        enabled = sets.isNotEmpty() && !state.busy,
        shape = RoundedCornerShape(16.dp),
        // Tonal, so the repeated "Save set" stays the single strongest action on screen.
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("finish_session"),
    ) {
        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.finish_session), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        ConfirmButton(R.string.cancel_session, stringResource(R.string.delete_session_note), !state.busy) {
            vm.deleteSession(session.id)
        }
    }
    if (guide) ExerciseGuideSheet(exercise) { guide = false }
}

@Composable
private fun TrainingHistoryContent(state: AppState, today: LocalDate, vm: FitnessViewModel, showVictory: Boolean = false, onDismissVictory: () -> Unit = {}) {
    var exercise by rememberSaveable { mutableStateOf(Exercise.ROW) }
    var guide by rememberSaveable { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    var limit by rememberSaveable { mutableIntStateOf(10) }
    var filterDay by rememberSaveable { mutableStateOf<Long?>(null) }
    if (showVictory) {
        val latest = state.snapshot.sessions.filter { it.completedAt != null }.maxByOrNull { it.completedAt ?: 0L }
        val latestSets = latest?.let { s -> state.snapshot.sets.filter { it.sessionId == s.id } }.orEmpty()
        val tonnage = TrainingHistory.sessionTonnage(latestSets)
        val sessionMuscles = remember(latestSets) {
            val map = mutableMapOf<Muscle, Int>()
            latestSets.filter { it.kind == SetKind.WORKING }.forEach { set ->
                val act = ExerciseMuscles.of(set.exercise)
                (act.primary + act.secondary).forEach { m -> map[m] = (map[m] ?: 0) + 1 }
            }
            map
        }
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(R.string.workout_complete_banner), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        if (tonnage > 0.0) {
                            Text(stringResource(R.string.session_tonnage, formatWeight(tonnage, 0)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = onDismissVictory) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.dismiss))
                    }
                }
                if (sessionMuscles.isNotEmpty()) {
                    WeeklyMuscleHeatMap(sessionMuscles)
                }
            }
        }
    }
    TrainingExport(state, vm)
    WorkoutCalendarPanel(state.snapshot, today, filterDay?.let(LocalDate::ofEpochDay)) { filterDay = it?.toEpochDay(); limit = 10 }
    if (filterDay == null) {
        InfoCard {
            var chartMode by rememberSaveable { mutableIntStateOf(0) }
            Text(stringResource(R.string.load_history), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            ExercisePicker(exercise, { exercise = it })
            SegmentedTabs(
                selectedIndex = chartMode,
                labels = listOf(R.string.chart_mode_load, R.string.chart_mode_e1rm),
                onSelect = { chartMode = it },
                tagPrefix = "chart_mode",
            )
            val unit = LocalWeightUnit.current
            val unitLabel = weightUnitLabel()
            if (chartMode == 0) {
                MuscleTraining.best(state.snapshot, exercise, today)?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(R.string.best_recorded_load, formatWeight(it.weightKg), number(it.repsAtWeight.toDouble(), 0), dateLabel(it.date)),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
                HistoryChart(
                    TrainingHistory.loads(state.snapshot, exercise, today).takeLast(30).map { it.copy(value = WeightUnits.fromKg(it.value, unit)) },
                    unitLabel,
                )
            } else {
                TrainingHistory.bestE1rm(state.snapshot, exercise, today)?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(R.string.best_e1rm_value, formatWeight(it.value, 1), dateLabel(it.date)),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
                HistoryChart(
                    TrainingHistory.e1rm(state.snapshot, exercise, today).takeLast(30).map { it.copy(value = WeightUnits.fromKg(it.value, unit)) },
                    unitLabel,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.external_load_note), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { guide = true }) { Text(stringResource(R.string.view_form)) }
            }
        }
        InfoCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.weekly_muscles), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                ScopeInfoButton(R.string.weekly_muscles, R.string.weekly_muscles_note)
            }
            val counts = MuscleTraining.weekly(state.snapshot, today).filter { it.primarySets + it.secondarySets > 0 }
            if (counts.isEmpty()) Text(stringResource(R.string.no_sessions), color = MaterialTheme.colorScheme.onSurfaceVariant)
            val maxTotal = counts.maxOfOrNull { it.primarySets + it.secondarySets } ?: 1
            counts.forEach {
                MuscleVolumeBar(
                    muscleLabelText = stringResource(muscleLabel(it.muscle)),
                    primarySets = it.primarySets,
                    secondarySets = it.secondarySets,
                    maxSets = maxTotal,
                    detailText = stringResource(
                        R.string.muscle_set_count,
                        stringResource(muscleLabel(it.muscle)),
                        number(it.primarySets.toDouble(), 0),
                        number(it.secondarySets.toDouble(), 0),
                    ),
                )
            }
        }
    }
    Text(stringResource(R.string.recent_sessions), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Input(R.string.search_history, search, { search = it; limit = 10 }, "session_search", numeric = false)
    val names = Exercise.entries.associateWith { stringResource(exerciseLabel(it)) }
    val sessions = state.snapshot.sessions.filter {
        it.completedAt != null &&
            (filterDay == null || it.date.toEpochDay() == filterDay) &&
            (search.isBlank() || it.label.contains(search, true) || it.date.toString().contains(search) ||
                state.snapshot.sets.any { set -> set.sessionId == it.id && ExerciseSearch.matches(search, names.getValue(set.exercise) + " " + ExerciseCatalog.searchText(set.exercise)) })
    }
    if (sessions.isEmpty()) EmptyState(R.string.no_sessions, R.string.history_empty_hint)
    sessions.take(limit).forEach { session -> key(session.id) { SessionHistoryCard(session, state, vm, today) } }
    if (sessions.size > limit) TextButton(onClick = { limit += 10 }) { Text(stringResource(R.string.show_more)) }
    if (guide) ExerciseGuideSheet(exercise) { guide = false }
}

@Composable
private fun SessionHistoryCard(session: Session, state: AppState, vm: FitnessViewModel, today: LocalDate) {
    var expanded by rememberSaveable(session.id) { mutableStateOf(false) }
    val sets = state.snapshot.sets.filter { it.sessionId == session.id }
    InfoCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    sessionTitle(session, state.snapshot.programs),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.session_status, number(sets.size.toDouble(), 0), dateLabel(session.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val tonnage = TrainingHistory.sessionTonnage(sets)
                if (tonnage > 0.0) {
                    PillBadge(stringResource(R.string.session_tonnage, formatWeight(tonnage, 0)))
                }
            }
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.testTag("history_expand_${session.id}")) {
                Text(stringResource(if (expanded) R.string.show_less else R.string.view_session_details))
            }
        }
        if (expanded) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            val repeatFits = (session.plannedExercises + sets.map { it.exercise }).distinct().size in 1..20
            if (!repeatFits) Text(stringResource(R.string.repeat_limit_note), style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { vm.repeatSession(session.id) },
                    enabled = repeatFits && !state.busy && state.snapshot.sessions.none { it.completedAt == null },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("repeat_session_${session.id}"),
                ) { Text(stringResource(R.string.repeat_session)) }
                DateChoice(session.date, today, { vm.moveSession(session.id, it) }, "session_date_${session.id}")
            }
            if (session.source == SessionSource.APPLE_WATCH_IMPORT) {
                PillBadge(stringResource(R.string.watch_record_source))
                session.averageHeartRateBpm?.let { Text(stringResource(R.string.watch_heart_rate, number(it, 0))) }
                session.activeEnergyKcal?.let { Text(stringResource(R.string.watch_energy, number(it, 0))) }
            }
            session.prescriptions.forEach {
                Text(stringResource(exerciseLabel(it.exercise)), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                PrescriptionSummary(it)
            }
            sets.forEach { SetCard(it, state.busy, sets.size > 1, vm) }
            if (sets.size == 1) Text(stringResource(R.string.last_set_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ConfirmButton(R.string.delete_session, stringResource(R.string.delete_session_note), !state.busy) { vm.deleteSession(session.id) }
        }
    }
}

@Composable
private fun SetCard(set: TrainingSet, busy: Boolean, canRemove: Boolean, vm: FitnessViewModel, onRemoved: ((TrainingSet) -> Unit)? = null) {
    var editing by remember { mutableStateOf(false) }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.set_summary, stringResource(exerciseLabel(set.exercise)), number(set.reps.toDouble(), 0), formatLoad(set.weightKg, set.exercise)),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { editing = true }, enabled = !busy, modifier = Modifier.testTag("edit_set")) {
                    Icon(Icons.Default.Edit, stringResource(R.string.edit), modifier = Modifier.size(18.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillBadge(
                    text = stringResource(if (set.kind == SetKind.WARM_UP) R.string.warmup_set else R.string.working_set),
                    containerColor = if (set.kind == SetKind.WARM_UP) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (set.kind == SetKind.WARM_UP) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                )
                set.rpe?.let {
                    PillBadge(
                        text = stringResource(R.string.rpe_value, number(it, 1)),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.tertiary,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (onRemoved != null) {
                    TextButton(
                        onClick = { vm.removeSet(set.id); onRemoved(set) },
                        enabled = !busy && canRemove,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)),
                    ) { Text(stringResource(R.string.remove)) }
                } else {
                    ConfirmButton(R.string.remove, stringResource(R.string.delete_set_note), !busy && canRemove) { vm.removeSet(set.id) }
                }
            }
        }
    }
    if (editing) SetDialog(set, busy, { editing = false }) { vm.correctSet(it) { editing = false } }
}

@Composable
private fun SetDialog(set: TrainingSet, busy: Boolean, onDismiss: () -> Unit, onSave: (TrainingSet) -> Unit) {
    val unit = LocalWeightUnit.current
    val locale = LocalConfiguration.current.locales[0]
    var exercise by remember { mutableStateOf(set.exercise) }
    var reps by remember { mutableStateOf(formatInputNumber(set.reps.toDouble(), locale, 0)) }
    var weight by remember { mutableStateOf(formatInputNumber(WeightUnits.fromKg(set.weightKg, unit), locale, 2)) }
    var inputUnit by remember { mutableStateOf(unit) }
    LaunchedEffect(unit) {
        if (inputUnit != unit) {
            NumericInput.decimal(weight)?.let { parsed ->
                weight = formatInputNumber(WeightUnits.convert(parsed, inputUnit, unit), locale, 2)
            }
            inputUnit = unit
        }
    }
    var warmup by remember { mutableStateOf(set.kind == SetKind.WARM_UP) }
    var rpe by remember { mutableStateOf(set.rpe?.let { formatInputNumber(it, locale, 1) }.orEmpty()) }
    val value = runCatching {
        set.copy(
            exercise = exercise,
            reps = requireNotNull(NumericInput.integer(reps)),
            weightKg = WeightUnits.toKg(requireNotNull(NumericInput.decimal(weight)), unit),
            kind = if (warmup) SetKind.WARM_UP else SetKind.WORKING,
            rpe = if (rpe.isBlank()) null else requireNotNull(NumericInput.decimal(rpe)),
        )
    }.getOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_set), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(selected = warmup, onClick = { warmup = !warmup }, label = { Text(stringResource(R.string.warmup_set)) })
                Input(R.string.rpe_optional, rpe, { rpe = it }, "edit_rpe")
                ExercisePicker(exercise, { exercise = it })
                Input(R.string.reps, reps, { reps = it }, "edit_reps")
                Input(if (unit == WeightUnit.KG) R.string.weight_kg else R.string.weight_lb, weight, { weight = it }, "edit_weight")
            }
        },
        confirmButton = {
            TextButton(onClick = { value?.let(onSave) }, enabled = value != null && !busy, modifier = Modifier.testTag("save_set_correction")) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun ProgramDialog(program: TrainingProgram?, busy: Boolean, onDismiss: () -> Unit, onSave: (TrainingProgram) -> Unit) {
    var name by rememberSaveable(program?.id) { mutableStateOf(program?.name.orEmpty()) }
    var selected by rememberSaveable(program?.id) { mutableStateOf(program?.exercises ?: emptyList()) }
    var rest by rememberSaveable(program?.id) { mutableStateOf((program?.restSeconds ?: 90).toString()) }
    var detailsPayload by rememberSaveable(program?.id) { mutableStateOf(Json.encodeToString(program?.prescriptions ?: emptyList<ExercisePrescription>())) }
    val details = Json.decodeFromString<List<ExercisePrescription>>(detailsPayload)
    var detailExercise by rememberSaveable(program?.id) { mutableStateOf<Exercise?>(null) }
    fun orderChanged(order: List<Exercise>) {
        detailsPayload = Json.encodeToString(PlanEditing.normalize(order, details))
        selected = order
    }
    var search by remember { mutableStateOf("") }
    var limit by remember { mutableIntStateOf(12) }
    val candidate = runCatching {
        TrainingProgram(
            id = program?.id ?: java.util.UUID.randomUUID().toString(),
            name = name.trim(),
            exercises = selected,
            restSeconds = requireNotNull(NumericInput.integer(rest)),
            prescriptions = details,
        )
    }.getOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.saved_program), fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Input(R.string.program_name, name, { name = it }, "program_name", numeric = false)
                Input(R.string.rest_seconds, rest, { rest = it }, "program_rest")
                Text(stringResource(R.string.program_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                selected.forEachIndexed { index, exercise ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(exerciseLabel(exercise)), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            details.firstOrNull { it.exercise == exercise }?.let { PrescriptionSummary(it) }
                            TextButton(onClick = { detailExercise = exercise }, modifier = Modifier.testTag("plan_details_${exercise.name}")) {
                                Text(stringResource(R.string.edit_plan_details))
                            }
                            if (index > 0) {
                                val group = details.firstOrNull { it.exercise == exercise }?.supersetGroup
                                val previous = details.firstOrNull { it.exercise == selected[index - 1] }?.supersetGroup
                                GroupLink(
                                    group != null && group == previous,
                                    { linked -> detailsPayload = Json.encodeToString(PlanEditing.linkPrevious(selected, details, exercise, linked)) },
                                    exercise,
                                )
                            }
                            Row {
                                TextButton(
                                    onClick = { orderChanged(selected.toMutableList().apply { java.util.Collections.swap(this, index, index - 1) }) },
                                    enabled = index > 0,
                                ) { Text(stringResource(R.string.move_up)) }
                                TextButton(
                                    onClick = { orderChanged(selected.toMutableList().apply { java.util.Collections.swap(this, index, index + 1) }) },
                                    enabled = index < selected.lastIndex,
                                ) { Text(stringResource(R.string.move_down)) }
                                TextButton(onClick = { orderChanged(selected - exercise) }) { Text(stringResource(R.string.remove)) }
                            }
                        }
                    }
                }
                Input(R.string.search_exercises, search, { search = it; limit = 12 }, "program_exercise_search", numeric = false)
                val names = Exercise.entries.associateWith { stringResource(exerciseLabel(it)) }
                val matches = Exercise.entries.filter { ExerciseSearch.matches(search, names.getValue(it) + " " + ExerciseCatalog.searchText(it)) }
                matches.take(limit).forEach { exercise ->
                    FilterChip(
                        selected = exercise in selected,
                        onClick = { orderChanged(if (exercise in selected) selected - exercise else selected + exercise) },
                        enabled = exercise in selected || selected.size < 20,
                        label = { Text(stringResource(exerciseLabel(exercise))) },
                        modifier = Modifier.testTag("program_exercise_${exercise.name}"),
                    )
                }
                if (matches.size > limit) TextButton(onClick = { limit += 12 }) { Text(stringResource(R.string.show_more)) }
            }
        },
        confirmButton = {
            TextButton(onClick = { candidate?.let(onSave) }, enabled = candidate != null && !busy, modifier = Modifier.testTag("save_program")) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
    detailExercise?.let { exercise ->
        PrescriptionEditor(
            details.firstOrNull { it.exercise == exercise } ?: ExercisePrescription(exercise),
            busy,
            { detailExercise = null },
        ) { value ->
            detailsPayload = Json.encodeToString(details.filter { it.exercise != exercise } + value)
            detailExercise = null
        }
    }
}

/** User-facing workout name; internal template IDs are never shown. */
@Composable
internal fun sessionTitle(session: Session, programs: List<TrainingProgram>): String = session.label.ifBlank {
    when (session.templateId) {
        "A" -> stringResource(R.string.template_a)
        "B" -> stringResource(R.string.template_b)
        "quick" -> stringResource(R.string.quick_workout)
        "repeat" -> stringResource(R.string.repeated_workout)
        else -> programs.firstOrNull { it.id == session.templateId }?.name ?: stringResource(R.string.workout_generic)
    }
}
