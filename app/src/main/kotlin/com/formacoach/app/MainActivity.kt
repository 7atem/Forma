package com.formacoach.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.formacoach.coach.NextAction
import com.formacoach.model.*
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(AppLocaleHelper.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FormaApp() }
    }
}

internal val LocalWeightUnit = compositionLocalOf { WeightUnit.KG }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormaApp(vm: FitnessViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("forma_ui_prefs", android.content.Context.MODE_PRIVATE) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var darkTheme by rememberSaveable { mutableStateOf(prefs.getBoolean("dark_theme", true)) }
    var weightUnit by rememberSaveable { mutableStateOf(if (prefs.getString("weight_unit", "KG") == "LB") WeightUnit.LB else WeightUnit.KG) }
    var showQuickLog by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var editingProfile by rememberSaveable { mutableStateOf(false) }
    var showWatch by rememberSaveable { mutableStateOf(false) }
    var showBackup by rememberSaveable { mutableStateOf(false) }
    var language by remember { mutableStateOf(AppLocaleHelper.current(context)) }
    var generation by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val setRemovedMsg = stringResource(R.string.set_removed_undo)
    val undoLabel = stringResource(R.string.undo)
    val weightSavedMsg = stringResource(R.string.quick_weight_saved)
    val pageState = key(generation) { rememberSaveableStateHolder() }
    // Re-evaluate calendar totals if the app remains open over local midnight.
    var today by remember { mutableStateOf(LocalDate.now(vm.clock)) }
    LaunchedEffect(Unit) {
        while (true) {
            today = LocalDate.now(vm.clock)
            kotlinx.coroutines.delay(30_000)
        }
    }
    LaunchedEffect(today) { vm.onDayChanged() }
    LaunchedEffect(state.snapshot, today) {
        if (!state.loading && !state.loadFailed) {
            FormaWidgetProvider.updateSummary(context, state.snapshot, today)
        }
    }
    // Numbers and dates use Western digits in every language so they match the app's text resources.
    val baseConfiguration = LocalConfiguration.current
    val displayConfiguration = remember(baseConfiguration) {
        android.content.res.Configuration(baseConfiguration).apply {
            setLocales(android.os.LocaleList(withWesternDigits(baseConfiguration.locales[0])))
        }
    }
    CompositionLocalProvider(LocalWeightUnit provides weightUnit, LocalConfiguration provides displayConfiguration) {
    MaterialTheme(
        colorScheme = if (darkTheme) FormaColors else FormaLightColors,
        typography = FormaTypography,
        shapes = FormaShapes,
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (state.loadFailed) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.load_failed))
                }
            } else if (state.snapshot.profile == null || editingProfile) {
                Box(Modifier.fillMaxSize().systemBarsPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
                    ProfileForm(
                        state.snapshot.profile,
                        state.busy,
                        state.error,
                        onSave = { vm.saveProfile(it) { editingProfile = false } },
                        onCancel = if (state.snapshot.profile == null) null else ({ editingProfile = false }),
                        onBackup = { showBackup = true },
                        weightUnit = weightUnit,
                        onWeightUnit = {
                            weightUnit = it
                            prefs.edit().putString("weight_unit", it.name).apply()
                        },
                    )
                }
            } else {
                FormaScaffold(
                    tab = tab,
                    onTab = { tab = it },
                    today = today,
                    snackbarHostState = snackbarHostState,
                    onQuickLog = { showQuickLog = true },
                    onProfile = { editingProfile = true },
                    onSettings = { showSettings = true },
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
                        pageState.SaveableStateProvider(tab) {
                            val scrollState = rememberScrollState()
                            Column(
                                Modifier
                                    .widthIn(max = 880.dp)
                                    .fillMaxWidth()
                                    .verticalScroll(scrollState)
                                    .padding(horizontal = PageGutter, vertical = 18.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                if (state.error) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                stringResource(R.string.save_failed),
                                                Modifier.weight(1f),
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                            )
                                            TextButton(onClick = vm::clearError) {
                                                Text(stringResource(R.string.dismiss), color = MaterialTheme.colorScheme.onErrorContainer)
                                            }
                                        }
                                    }
                                }
                                val liveSession = state.snapshot.sessions.firstOrNull { it.completedAt == null }
                                if (liveSession != null && tab != 1) {
                                    val liveSets = state.snapshot.sets.filter { it.sessionId == liveSession.id }
                                    val lastEx = liveSets.lastOrNull()?.exercise
                                    Surface(
                                        onClick = { tab = 1 },
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            LivePulseDot(MaterialTheme.colorScheme.primary)
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    stringResource(R.string.active_session, sessionTitle(liveSession, state.snapshot.programs)),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                )
                                                Text(
                                                    lastEx?.let { stringResource(exerciseLabel(it)) } ?: stringResource(R.string.resume_session),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                            PillBadge(number(liveSets.size.toDouble(), 0))
                                        }
                                    }
                                }
                                var progressSection by rememberSaveable { mutableIntStateOf(0) }
                                when (tab) {
                                    0 -> {
                                        TodayContent(
                                            state.snapshot,
                                            today,
                                            vm,
                                            busy = state.busy,
                                            onTrain = { tab = 1 },
                                            onEat = { tab = 2 },
                                            onStartProgram = { id -> vm.start(id, today); tab = 1 },
                                        )
                                        MilestonesCard(state.snapshot)
                                    }
                                    1 -> TrainContent(state, today, vm, scrollState) { deletedSet, originalIds ->
                                        scope.launch {
                                            val res = snackbarHostState.showSnackbar(
                                                message = setRemovedMsg,
                                                actionLabel = undoLabel,
                                                duration = SnackbarDuration.Short,
                                            )
                                            if (res == SnackbarResult.ActionPerformed) {
                                                vm.restoreSet(deletedSet, originalIds)
                                            }
                                        }
                                    }
                                    2 -> EatContent(state, today, vm)
                                    3 -> {
                                        SegmentedTabs(
                                            selectedIndex = progressSection,
                                            labels = listOf(R.string.stats_section, R.string.coach),
                                            onSelect = { progressSection = it },
                                            tagPrefix = "progress_section",
                                        )
                                        when (progressSection) {
                                            0 -> ProgressContent(state, today, vm)
                                            else -> CoachContent(state, today, vm)
                                        }
                                    }
                                }
                                // Room for the floating quick-log button so it never hides the last item.
                                Spacer(Modifier.height(88.dp))
                            }
                        }
                    }
                }
            }
            if (showQuickLog) {
                ModalBottomSheet(
                    onDismissRequest = { showQuickLog = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    val todayMeasure = state.snapshot.measurements.firstOrNull { it.date == today }
                    var quickWeight by rememberSaveable(today) {
                        mutableStateOf(todayMeasure?.weightKg?.let { formatInputNumber(WeightUnits.fromKg(it, weightUnit), displayConfiguration.locales[0], 2) }.orEmpty())
                    }
                    val parsedQuickWeight = NumericInput.decimal(quickWeight)?.let { WeightUnits.toKg(it, weightUnit) }?.takeIf { it in 20.0..400.0 }
                    val recentMeals = state.snapshot.meals.distinctBy { listOf(it.name, it.calories, it.protein, it.carbs, it.fat, it.source) }.take(4)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp)
                            .navigationBarsPadding()
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(stringResource(R.string.quick_log), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = { showQuickLog = false; tab = 1 },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                        ) {
                            Icon(Icons.Default.FitnessCenter, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(if (state.snapshot.sessions.any { it.completedAt == null }) R.string.resume_session else R.string.open_training), fontWeight = FontWeight.Bold)
                        }
                        // Inline Quick Weight Log
                        Text(stringResource(R.string.quick_log_weight), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                Input(
                                    if (weightUnit == WeightUnit.KG) R.string.body_weight else R.string.body_weight_lb,
                                    quickWeight,
                                    { quickWeight = it },
                                    "quick_weight_input",
                                )
                            }
                            FilledTonalButton(
                                onClick = {
                                    parsedQuickWeight?.let { kg ->
                                        vm.measure(Measurement(today, kg, todayMeasure?.waistCm)) {
                                            showQuickLog = false
                                            scope.launch { snackbarHostState.showSnackbar(weightSavedMsg, duration = SnackbarDuration.Short) }
                                        }
                                    }
                                },
                                enabled = parsedQuickWeight != null && !state.busy,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.heightIn(min = 52.dp).testTag("quick_weight_save"),
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                            }
                        }
                        if (recentMeals.isNotEmpty()) {
                            Text(stringResource(R.string.quick_repeat_meal), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                recentMeals.forEach { recent ->
                                    AssistChip(
                                        onClick = {
                                            vm.addMeal(recent.copy(id = java.util.UUID.randomUUID().toString(), date = today)) {
                                                showQuickLog = false
                                                tab = 2
                                            }
                                        },
                                        enabled = !state.busy,
                                        leadingIcon = { Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(15.dp)) },
                                        label = { Text("${recent.name} · ${number(recent.calories, 0)}", maxLines = 1) },
                                    )
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = { showQuickLog = false; tab = 2 },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Icon(Icons.Default.Restaurant, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.add_meal), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            if (showSettings) {
                SettingsSheet(
                    language = language,
                    onLanguage = { next ->
                        language = next
                        showSettings = false
                        AppLocaleHelper.apply(context, next)
                    },
                    darkTheme = darkTheme,
                    onDarkTheme = {
                        darkTheme = it
                        prefs.edit().putBoolean("dark_theme", it).apply()
                    },
                    weightUnit = weightUnit,
                    onWeightUnit = {
                        weightUnit = it
                        prefs.edit().putString("weight_unit", it.name).apply()
                    },
                    onProfile = { editingProfile = true },
                    onWatch = { showWatch = true },
                    onBackup = { showBackup = true },
                    onDismiss = { showSettings = false },
                )
            }
            if (showWatch) WatchTransferDialog(vm, state) { showWatch = false }
            if (showBackup) {
                BackupDialog(
                    vm,
                    state,
                    onDismiss = { showBackup = false },
                    onRestored = {
                        showBackup = false
                        editingProfile = false
                        tab = 0
                        generation++
                    },
                )
            }
        }
    }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileForm(
    profile: Profile?,
    busy: Boolean,
    error: Boolean,
    onSave: (Profile) -> Unit,
    onCancel: (() -> Unit)?,
    onBackup: () -> Unit,
    weightUnit: WeightUnit,
    onWeightUnit: (WeightUnit) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(profile?.name.orEmpty()) }
    var goal by rememberSaveable { mutableStateOf(profile?.goal ?: Goal.FAT_LOSS) }
    var sessions by rememberSaveable { mutableIntStateOf(profile?.sessionsPerWeek ?: 3) }
    var calories by rememberSaveable { mutableStateOf(profile?.caloriesTarget?.toString().orEmpty()) }
    var protein by rememberSaveable { mutableStateOf(profile?.proteinTarget?.toString().orEmpty()) }
    val candidate = runCatching {
        Profile(
            name.trim(),
            goal,
            sessions,
            if (calories.isBlank()) null else requireNotNull(NumericInput.integer(calories)),
            if (protein.isBlank()) null else requireNotNull(NumericInput.decimal(protein)),
        )
    }.getOrNull()
    Column(
        Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(52.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.FitnessCenter, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.welcome), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Text(stringResource(R.string.welcome_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        InfoCard {
            Input(R.string.name, name, { name = it }, "profile_name", numeric = false)
            Text(stringResource(R.string.your_goal), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Goal.entries.forEach { value ->
                    FilterChip(
                        selected = goal == value,
                        onClick = { goal = value },
                        label = { Text(stringResource(goalLabel(value))) },
                    )
                }
            }
            Text(stringResource(R.string.weight_unit_question), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WeightUnit.entries.forEach { value ->
                    FilterChip(
                        selected = weightUnit == value,
                        onClick = { onWeightUnit(value) },
                        label = { Text(stringResource(if (value == WeightUnit.KG) R.string.kg else R.string.lb), fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("weight_unit_${value.name}"),
                    )
                }
            }
            Text(stringResource(R.string.sessions_per_week), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                (2..4).forEach { count ->
                    FilterChip(
                        selected = sessions == count,
                        onClick = { sessions = count },
                        label = { Text(number(count.toDouble(), 0), fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("target_sessions_$count"),
                    )
                }
            }
        }
        InfoCard {
            Text(stringResource(R.string.manual_targets), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.manual_targets_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FormFieldPair(
                first = { Input(R.string.calorie_target, calories, { calories = it }, "profile_calories") },
                second = { Input(R.string.protein_target, protein, { protein = it }, "profile_protein") },
            )
        }
        if (error) Text(stringResource(R.string.save_failed), color = MaterialTheme.colorScheme.error)
        Button(
            onClick = { candidate?.let(onSave) },
            enabled = candidate != null && !busy,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).testTag("save_profile"),
        ) {
            Text(
                stringResource(if (profile == null) R.string.start_journey else R.string.save),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            onCancel?.let { TextButton(onClick = it) { Text(stringResource(R.string.cancel)) } }
            TextButton(onClick = onBackup) { Text(stringResource(R.string.backup)) }
        }
        Text(stringResource(R.string.local_privacy), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TodayContent(
    snapshot: FitnessSnapshot,
    today: LocalDate,
    vm: FitnessViewModel,
    busy: Boolean,
    onTrain: () -> Unit,
    onEat: () -> Unit,
    onStartProgram: (String) -> Unit,
) {
    val profile = checkNotNull(snapshot.profile)
    val meals = snapshot.meals.filter { it.date == today }
    val review = vm.reviewEngine.review(snapshot, today)
    val activeDates = remember(snapshot.sessions) {
        snapshot.sessions.filter { it.completedAt != null }.map { it.date }.toSet()
    }
    val hasActiveSession = snapshot.sessions.any { it.completedAt == null }
    val weeklyVol = com.formacoach.coach.TrainingHistory.weeklyTonnage(snapshot, today)
    val latestWeight = snapshot.measurements.firstOrNull { it.weightKg != null }?.weightKg
    val unit = LocalWeightUnit.current
    val unitStr = stringResource(if (unit == WeightUnit.KG) R.string.kg else R.string.lb)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            if (profile.name.isBlank()) stringResource(R.string.greeting_anonymous) else stringResource(R.string.greeting, profile.name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            stringResource(R.string.today_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    // Warm, honest daily narrative reflection (deterministic, never shames rest days)
    val todayCompletedIds = snapshot.sessions.filter { it.completedAt != null && it.date == today }.map { it.id }.toSet()
    val todayWorkingSets = snapshot.sets.count { it.sessionId in todayCompletedIds && it.kind == SetKind.WORKING }
    val todayKcal = meals.sumOf { it.calories }
    val storyText = when {
        todayWorkingSets > 0 && todayKcal > 0 -> stringResource(
            R.string.daily_story_complete,
            number(todayWorkingSets.toDouble(), 0),
            number(todayKcal, 0),
        )
        todayWorkingSets > 0 -> stringResource(R.string.daily_story_trained, number(todayWorkingSets.toDouble(), 0))
        todayKcal > 0 -> stringResource(R.string.daily_story_fueled, number(todayKcal, 0))
        else -> stringResource(R.string.daily_story_fresh)
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth().testTag("daily_narrative_card"),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            Text(storyText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }

    WeeklyActivityStrip(today = today, activeDates = activeDates)

    // Hero Weekly Training Card
    Surface(
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    ),
                )
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PillBadge(stringResource(R.string.your_week))
                        Text(
                            stringResource(
                                R.string.workout_count,
                                number(review.completedSessions.toDouble(), 0),
                                number(profile.sessionsPerWeek.toDouble(), 0),
                            ),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            stringResource(R.string.completed_sessions),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    CircularProgressRing(
                        progress = review.completedSessions.toFloat() / profile.sessionsPerWeek.coerceAtLeast(1),
                        valueText = "\u2066${number(review.completedSessions.toDouble(), 0)}/${number(profile.sessionsPerWeek.toDouble(), 0)}\u2069",
                        labelText = "",
                        size = 86.dp,
                        strokeWidth = 9.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Button(
                    onClick = onTrain,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) {
                    Icon(
                        if (hasActiveSession) Icons.Default.PlayArrow else Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(if (hasActiveSession) R.string.resume_session else R.string.open_training),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }

    // 1-Tap Quick Start Programs Row (when no active session)
    if (weeklyVol > 0.0 || latestWeight != null) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (weeklyVol > 0.0) {
                MetricTile(
                    "${number(WeightUnits.fromKg(weeklyVol, unit), 0)} $unitStr",
                    stringResource(R.string.weekly_volume_label),
                    Modifier.weight(1f),
                    accent = MaterialTheme.colorScheme.primary,
                )
            }
            latestWeight?.let {
                MetricTile(
                    "${number(WeightUnits.fromKg(it, unit), 1)} $unitStr",
                    stringResource(R.string.body_weight),
                    Modifier.weight(1f),
                    accent = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }

    if (!hasActiveSession) {
        val quickItems = if (snapshot.programs.isNotEmpty()) {
            snapshot.programs.map { Triple(it.id, it.name, it.exercises) }
        } else {
            StarterTemplates.all.map {
                Triple(it.id, stringResource(starterTemplateLabel(it.id)), it.exercises)
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            quickItems.take(5).forEach { (id, name, exercises) ->
                Surface(
                    onClick = { if (!busy) onStartProgram(id) },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
                    modifier = Modifier.width(210.dp),
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (exercises.isNotEmpty()) {
                            ExerciseThumbnail(
                                exercises.first(),
                                Modifier.width(58.dp).clip(RoundedCornerShape(10.dp)),
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                stringResource(R.string.program_exercise_count, number(exercises.size.toDouble(), 0)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }

    // Daily Nutrition Overview Card
    val totalKcal = meals.sumOf { it.calories }
    val totalProtein = meals.sumOf { it.protein }
    val totalCarbs = meals.sumOf { it.carbs }
    val totalFat = meals.sumOf { it.fat }
    InfoCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.food_today), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            ScopeInfoButton(R.string.food_today, R.string.logged_only)
            Icon(Icons.Default.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
        }
        if (profile.caloriesTarget != null || profile.proteinTarget != null) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                profile.caloriesTarget?.let { target ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CircularProgressRing(
                            progress = (totalKcal / target).toFloat(),
                            valueText = number(totalKcal, 0),
                            labelText = "/ ${number(target.toDouble(), 0)}",
                            size = 98.dp,
                            strokeWidth = 9.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(stringResource(R.string.calories_logged), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } ?: MetricTile(number(totalKcal, 0), stringResource(R.string.calories_logged), Modifier.weight(1f), accent = MaterialTheme.colorScheme.primary)
                profile.proteinTarget?.let { target ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CircularProgressRing(
                            progress = (totalProtein / target).toFloat(),
                            valueText = number(totalProtein, 0),
                            labelText = "/ ${number(target, 0)}g",
                            size = 98.dp,
                            strokeWidth = 9.dp,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Text(stringResource(R.string.protein_logged), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } ?: MetricTile(number(totalProtein), stringResource(R.string.protein_logged), Modifier.weight(1f), accent = MaterialTheme.colorScheme.secondary)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricTile(number(totalKcal, 0), stringResource(R.string.calories_logged), Modifier.weight(1f), accent = MaterialTheme.colorScheme.primary)
                MetricTile(number(totalProtein), stringResource(R.string.protein_logged), Modifier.weight(1f), accent = MaterialTheme.colorScheme.secondary)
            }
        }
        if (totalKcal > 0) {
            MacroBreakdownBar(protein = totalProtein, carbs = totalCarbs, fat = totalFat)
        }
        OutlinedButton(
            onClick = onEat,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.add_meal), fontWeight = FontWeight.SemiBold)
        }
    }

    // Compact Next Step Banner
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.next_step), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                Text(stringResource(actionLabel(review.nextAction)), style = MaterialTheme.typography.bodyMedium)
            }
            ScopeInfoButton(R.string.app_name, R.string.alpha_note)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MilestonesCard(snapshot: FitnessSnapshot) {
    val completedSessions = snapshot.sessions.count { it.completedAt != null }
    val workingSets = snapshot.sets.count { it.kind == SetKind.WORKING }
    if (completedSessions == 0 && workingSets == 0) return
    val setMilestones = listOf(10, 50, 100, 250, 500, 1000).filter { workingSets >= it }
    InfoCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.milestones_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (completedSessions >= 1) {
                PillBadge(
                    text = "★ " + stringResource(R.string.milestone_first_session),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                )
            }
            setMilestones.forEach { count ->
                PillBadge(
                    text = "★ " + stringResource(R.string.milestone_sets_count, number(count.toDouble(), 0)),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
internal fun InfoCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
internal fun Input(label: Int, value: String, onChange: (String) -> Unit, tag: String, numeric: Boolean = true, error: Boolean = false, placeholder: String? = null) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        placeholder = placeholder?.let { hint -> ({ Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)) }) },
        singleLine = true,
        isError = error,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text,
            imeAction = ImeAction.Done,
        ),
        modifier = Modifier.fillMaxWidth().testTag(tag),
    )
}

internal fun withWesternDigits(locale: java.util.Locale): java.util.Locale =
    java.util.Locale.Builder().setLocale(locale).setUnicodeLocaleKeyword("nu", "latn").build()

@Composable
internal fun number(value: Double, digits: Int = 2): String {
    val locale = LocalConfiguration.current.locales[0]
    return NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = digits }.format(value)
}

@Composable
internal fun weightUnitLabel(): String =
    stringResource(if (LocalWeightUnit.current == WeightUnit.KG) R.string.kg else R.string.lb)

@Composable
internal fun formatWeight(kg: Double, digits: Int = 2): String =
    "${number(WeightUnits.fromKg(kg, LocalWeightUnit.current), digits)} ${weightUnitLabel()}"

/** Bodyweight exercises recorded with no external load read as "bodyweight", not "0 kg". */
@Composable
internal fun formatLoad(kg: Double, exercise: Exercise): String =
    if (kg == 0.0 && Equipment.BODYWEIGHT in ExerciseCatalog.of(exercise).equipment) stringResource(R.string.bodyweight_load) else formatWeight(kg)

internal fun formatInputNumber(value: Double, locale: java.util.Locale, digits: Int = 2): String =
    NumberFormat.getNumberInstance(locale).apply {
        isGroupingUsed = false
        maximumFractionDigits = digits
    }.format(value)

@Composable
internal fun formatInputNumber(value: Double, digits: Int = 2): String =
    formatInputNumber(value, LocalConfiguration.current.locales[0], digits)

@Composable
internal fun formatWeightInput(kg: Double): String =
    formatInputNumber(WeightUnits.fromKg(kg, LocalWeightUnit.current), 2)

@Composable
internal fun dateLabel(date: LocalDate): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(LocalConfiguration.current.locales[0]))

/** Display name for a built-in starter template; every new starter ID must be listed here. */
internal fun starterTemplateLabel(id: String): Int = when (id) {
    "A" -> R.string.template_a
    "B" -> R.string.template_b
    "HOME_EXPRESS" -> R.string.template_home_express
    else -> R.string.workout_generic
}

internal fun goalLabel(goal: Goal): Int = when (goal) {
    Goal.FAT_LOSS -> R.string.goal_fat_loss
    Goal.BUILD_STRENGTH -> R.string.goal_strength
    Goal.BUILD_HABIT -> R.string.goal_habit
}

internal fun actionLabel(action: NextAction): Int = when (action) {
    NextAction.LOG_FIRST_SESSION -> R.string.action_first_session
    NextAction.KEEP_TRAINING_LOG -> R.string.action_keep_training
    NextAction.ADD_MEAL_CONTEXT -> R.string.action_meals
    NextAction.ADD_MEASUREMENTS -> R.string.action_measurements
    NextAction.REVIEW_WITH_CONTEXT -> R.string.action_review
}
