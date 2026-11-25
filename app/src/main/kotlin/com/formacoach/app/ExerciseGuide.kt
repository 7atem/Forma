package com.formacoach.app

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.formacoach.coach.TrainingHistory
import com.formacoach.model.*
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Procedural 3D mannequin is hidden so the app shows Videos, Photos, and 2D muscle highlights only. */
private const val ENABLE_PROCEDURAL_3D_MOTION = false

private val thumbnailCache=LruCache<Int,ImageBitmap>(8)

/** Small cached previews decode off the main thread; full resolution is reserved for the guide. */
@Composable
internal fun ExerciseThumbnail(exercise:Exercise,modifier:Modifier=Modifier) {
    val id=guideResources(exercise).image
    val resources=LocalContext.current.resources
    val bitmap by produceState(thumbnailCache.get(id)?.let {id to it},id) {
        value=(thumbnailCache.get(id)?:withContext(Dispatchers.IO) {
            BitmapFactory.decodeResource(resources,id,BitmapFactory.Options().apply {inSampleSize=2}).asImageBitmap().also {thumbnailCache.put(id,it)}
        }).let {id to it}
    }
    val image=bitmap?.takeIf {it.first==id}?.second
    if(image!=null) Image(image,contentDescription=null,contentScale=ContentScale.Fit,modifier=modifier.aspectRatio(1.5f))
    else Surface(modifier.aspectRatio(1.5f),color=MaterialTheme.colorScheme.background,shape=RoundedCornerShape(14.dp)) {}
}

@Composable
internal fun ExerciseIllustration(exercise:Exercise,onOpen:(()->Unit)?=null,showMuscleHighlights:Boolean=true) {
    val guide=guideResources(exercise)
    val description=stringResource(R.string.exercise_visual_description,stringResource(guide.variant))
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).then(if(onOpen!=null) Modifier.clickable(onClick=onOpen).testTag("open_illustration") else Modifier)) {
        Image(painterResource(guide.image),contentDescription=description,contentScale=ContentScale.Fit,
            modifier=Modifier.fillMaxWidth().aspectRatio(1.5f).testTag("exercise_art_${exercise.name}"))
        if(showMuscleHighlights) CompactMuscleOverlay(exercise, pulsePeak = false, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
        if(onOpen!=null) Icon(Icons.Default.OpenInFull,contentDescription=stringResource(R.string.enlarge_image),modifier=Modifier.align(Alignment.BottomEnd).padding(12.dp).size(20.dp),tint=MaterialTheme.colorScheme.primary)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ExerciseGuideSheet(
    exercise: Exercise,
    snapshot: FitnessSnapshot? = null,
    onRecord: (() -> Unit)? = null,
    tools: @Composable () -> Unit = {},
    onDismiss: () -> Unit,
) {
    var tab by rememberSaveable(exercise) {mutableIntStateOf(0)}
    var enlarged by rememberSaveable(exercise) {mutableStateOf(false)}
    val guide=guideResources(exercise)
    val activation=ExerciseMuscles.of(exercise)
    val uri=LocalUriHandler.current
    val guideScroll=rememberScrollState()
    LaunchedEffect(exercise) {guideScroll.scrollTo(0)}
    ModalBottomSheet(onDismissRequest=onDismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().heightIn(max=850.dp).verticalScroll(guideScroll).padding(horizontal=20.dp).navigationBarsPadding().padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(exerciseLabel(exercise)),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
                    Text(stringResource(guide.variant),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SectionInfoButton(InfoTopic.EXERCISE_GUIDE)
                IconButton(onClick=onDismiss,modifier=Modifier.testTag("guide_close")) {Icon(Icons.Default.Close,stringResource(R.string.close))}
            }
            if (onRecord != null) {
                Button(
                    onClick = onRecord,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("guide_record_set"),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.record_set_for, stringResource(exerciseLabel(exercise))), fontWeight = FontWeight.Bold)
                }
            }
            TabRow(selectedTabIndex=tab,containerColor=MaterialTheme.colorScheme.background) {
                listOf(R.string.technique,R.string.muscles_working).forEachIndexed {index,label ->
                    Tab(selected=tab==index,onClick={tab=index},text={Text(stringResource(label))},modifier=Modifier.testTag(if(index==0) "guide_tab_technique" else "guide_tab_muscles"))
                }
            }
            if(tab==0) {
                val reduceMotion = rememberReduceMotion()
                var animating by rememberSaveable(exercise) { mutableStateOf(!reduceMotion) }
                val context = LocalContext.current
                val hasMotion = remember(exercise) { ENABLE_PROCEDURAL_3D_MOTION && ExerciseMotionCatalog.of(exercise) != null }
                val hasVideo = remember(exercise) { exerciseVideoResource(context, exercise) != 0 }
                // 0 = 3D motion (hidden behind flag), 1 = video, 2 = photos. Only offer what exists for this exercise.
                val modes = remember(exercise) { listOfNotNull(0.takeIf { hasMotion }, 1.takeIf { hasVideo }, 2) }
                var mode by rememberSaveable(exercise) { mutableIntStateOf(modes.first()) }
                var showMuscleHighlights by rememberSaveable(exercise) { mutableStateOf(true) }
                if (modes.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        modes.forEach { option ->
                            FilterChip(
                                selected = mode == option,
                                onClick = { mode = option },
                                label = { Text(stringResource(when (option) { 0 -> R.string.motion_3d; 1 -> R.string.motion_video; else -> R.string.motion_photos })) },
                                modifier = Modifier.testTag(when (option) { 0 -> "guide_mode_3d"; 1 -> "guide_mode_video"; else -> "guide_mode_photos" }),
                            )
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("muscle_highlights_toggle").toggleable(
                        value = showMuscleHighlights,
                        role = androidx.compose.ui.semantics.Role.Switch,
                        onValueChange = { showMuscleHighlights = it },
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.muscle_highlights), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    Switch(checked = showMuscleHighlights, onCheckedChange = null)
                }
                val showMotion = mode != 2
                if (mode == 0) {
                    ExerciseMotion3D(exercise, playing = animating, showMuscleHighlights = showMuscleHighlights)
                    Text(
                        stringResource(R.string.motion_drag_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (mode == 1) {
                    ExerciseVideo(exercise, playing = animating, showMuscleHighlights = showMuscleHighlights)
                    Text(
                        stringResource(R.string.motion_video_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (animating && !reduceMotion) {
                    AnimatedExerciseIllustration(exercise, showMuscleHighlights = showMuscleHighlights) { enlarged = true }
                } else {
                    ExerciseIllustration(exercise, onOpen = { enlarged = true }, showMuscleHighlights = showMuscleHighlights)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    if ((!animating || reduceMotion) && !showMotion) {
                        Box(Modifier.weight(1f)) { PhaseLabels() }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    if (!reduceMotion) {
                        FilterChip(
                            selected = animating,
                            onClick = { animating = !animating },
                            leadingIcon = { Icon(if (animating) Icons.Default.Pause else Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp)) },
                            label = { Text(stringResource(if (animating) R.string.pause_demo else R.string.play_demo)) },
                        )
                    }
                }
                MuscleDemoLegend(exercise)
                // Per-Exercise Rep-Max Ladder
                if (snapshot != null) {
                    val ladder = remember(snapshot, exercise) { TrainingHistory.repMaxLadder(snapshot, exercise) }
                    if (ladder.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.rep_max_ladder), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ladder.forEach { (repBucket, bestKg) ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    ) {
                                        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("${number(repBucket.toDouble(), 0)}RM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                            Text(formatWeight(bestKg, 1), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                guide.cues.forEachIndexed {index,cue -> Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.Top) {
                    Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.primaryContainer) {Text(number((index+1).toDouble(),0),Modifier.padding(horizontal=11.dp,vertical=7.dp),color=MaterialTheme.colorScheme.onPrimaryContainer)}
                    Text(stringResource(cue),Modifier.weight(1f).padding(top=4.dp),style=MaterialTheme.typography.bodyLarge)
                }}
                Text(stringResource(R.string.guide_scope),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick={runCatching {uri.openUri(guide.reference)}}) {Text(stringResource(R.string.exercise_reference))}
            } else MuscleMap(exercise)
            tools()
        }
    }
    if(enlarged) ExerciseImageViewer(exercise){enlarged=false}
}

@Composable
private fun AnimatedExerciseIllustration(exercise: Exercise, showMuscleHighlights: Boolean, onOpen: () -> Unit) {
    val guide = guideResources(exercise)
    val bitmap = ImageBitmap.imageResource(guide.image)
    var showSecond by remember(exercise) { mutableStateOf(false) }
    LaunchedEffect(exercise) {
        while (true) {
            kotlinx.coroutines.delay(1300)
            showSecond = !showSecond
        }
    }
    val progress by animateFloatAsState(if (showSecond) 1f else 0f, tween(620), label = "phase_crossfade")
    val description = stringResource(R.string.exercise_visual_description, stringResource(guide.variant))
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onOpen).testTag("open_illustration")) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1.22f).semantics { contentDescription = description }.testTag("exercise_art_${exercise.name}")) {
            val halfW = bitmap.width / 2
            val baseFactor = minOf(size.width / halfW, size.height / bitmap.height)
            // Subtle breathing scale (1.0 -> 1.035) gives lifelike motion between start and peak phases
            val scaleMotion = 1f + 0.035f * progress
            val factor = baseFactor * scaleMotion
            val w = (halfW * factor).roundToInt()
            val h = (bitmap.height * factor).roundToInt()
            val dx = ((size.width - w) / 2).roundToInt()
            val dy = ((size.height - h) / 2).roundToInt()
            drawImage(bitmap, srcOffset = IntOffset(0, 0), srcSize = IntSize(halfW, bitmap.height), dstOffset = IntOffset(dx, dy), dstSize = IntSize(w, h), alpha = 1f - progress)
            drawImage(bitmap, srcOffset = IntOffset(halfW, 0), srcSize = IntSize(halfW, bitmap.height), dstOffset = IntOffset(dx, dy), dstSize = IntSize(w, h), alpha = progress)
        }
        if (showMuscleHighlights) CompactMuscleOverlay(
            exercise = exercise,
            pulsePeak = showSecond,
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
        )
        Column(
            Modifier.align(Alignment.BottomStart).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PillBadge(
                text = stringResource(if (showSecond) R.string.phase_movement else R.string.phase_start),
                containerColor = if (showSecond) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                contentColor = if (showSecond) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.width(84.dp).height(4.dp).clip(RoundedCornerShape(99.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
            )
        }
        Icon(Icons.Default.OpenInFull, contentDescription = stringResource(R.string.enlarge_image), modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).size(20.dp), tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun PhaseLabels() {
    // Art is intentionally not mirrored in RTL: its left/right phases keep their physical labels.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceAround) {
            Text(stringResource(R.string.phase_start),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.phase_movement),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ExerciseImageViewer(exercise:Exercise,onDismiss:()->Unit) {
    var phase by rememberSaveable(exercise) {mutableIntStateOf(0)}
    val guide=guideResources(exercise)
    val bitmap=ImageBitmap.imageResource(guide.image)
    val description=stringResource(guide.variant)+" · "+stringResource(when(phase) {1->R.string.phase_start;2->R.string.phase_movement;else->R.string.both_phases})
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp).padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text(stringResource(guide.variant),Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
                    IconButton(onClick=onDismiss,modifier=Modifier.testTag("image_viewer_close")) {Icon(Icons.Default.Close,stringResource(R.string.close))}
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf(R.string.both_phases,R.string.phase_start,R.string.phase_movement).forEachIndexed {index,label ->
                        FilterChip(selected=phase==index,onClick={phase=index},label={Text(stringResource(label))},modifier=Modifier.testTag("image_phase_$index"))
                    }
                }
                Canvas(Modifier.fillMaxWidth().weight(1f).semantics {contentDescription=description}.testTag("image_viewer")) {
                    val sourceWidth=if(phase==0) bitmap.width else bitmap.width/2
                    val factor=minOf(size.width/sourceWidth,size.height/bitmap.height)
                    val width=(sourceWidth*factor).roundToInt();val height=(bitmap.height*factor).roundToInt()
                    drawImage(bitmap,srcOffset=IntOffset(if(phase==2) bitmap.width/2 else 0,0),srcSize=IntSize(sourceWidth,bitmap.height),
                        dstOffset=IntOffset(((size.width-width)/2).roundToInt(),((size.height-height)/2).roundToInt()),dstSize=IntSize(width,height))
                }
                Text(stringResource(R.string.viewer_note),style=MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun ExerciseLibrary(state:AppState,vm:FitnessViewModel,today:LocalDate=LocalDate.now(vm.clock),onSetRecorded:()->Unit={}) {
    val columns=if(LocalConfiguration.current.screenWidthDp>=840) 3 else 2
    val context=LocalContext.current
    val prefs=remember(context) {context.getSharedPreferences("forma_ui_prefs",android.content.Context.MODE_PRIVATE)}
    var search by rememberSaveable {mutableStateOf("")}
    var opened by rememberSaveable {mutableStateOf<Exercise?>(null)}
    var recording by rememberSaveable {mutableStateOf<Exercise?>(null)}
    var equipment by rememberSaveable {mutableStateOf<Equipment?>(null)}
    var muscle by rememberSaveable {mutableStateOf<Muscle?>(null)}
    var homeOnly by rememberSaveable {mutableStateOf(false)}
    var favoritesOnly by rememberSaveable {mutableStateOf(false)}
    var recentOnly by rememberSaveable {mutableStateOf(false)}
    var discomfortEncoded by rememberSaveable {mutableStateOf(prefs.getString(ExerciseDiscomfortSelection.STORAGE_KEY,"")?:"")}
    val discomfortExcluded=remember(discomfortEncoded) {ExerciseDiscomfortSelection.decode(discomfortEncoded)}
    var showDiscomfortDialog by rememberSaveable {mutableStateOf(false)}
    fun saveDiscomfort(regions:Set<ExerciseDiscomfortRegion>) {
        val encoded=ExerciseDiscomfortSelection.encode(regions)
        discomfortEncoded=encoded
        prefs.edit().putString(ExerciseDiscomfortSelection.STORAGE_KEY,encoded).apply()
    }
    var limit by rememberSaveable {mutableIntStateOf(12)}
    LaunchedEffect(search,equipment,muscle,homeOnly,favoritesOnly,recentOnly,discomfortEncoded) {limit=12}
    Input(R.string.search_exercises,search,{search=it},"exercise_search",numeric=false)
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f)) {
            var expanded by remember {mutableStateOf(false)}
            OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth().testTag("equipment_filter")) {Text(stringResource(equipment?.let(::equipmentLabel)?:R.string.all_equipment))}
            DropdownMenu(expanded,onDismissRequest={expanded=false}) {
                DropdownMenuItem(text={Text(stringResource(R.string.all_equipment))},onClick={equipment=null;expanded=false})
                Equipment.entries.forEach {value ->DropdownMenuItem(text={Text(stringResource(equipmentLabel(value)))},onClick={equipment=value;expanded=false},modifier=Modifier.testTag("filter_equipment_${value.name}"))}
            }
        }
        Box(Modifier.weight(1f)) {
            var expanded by remember {mutableStateOf(false)}
            OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth().testTag("muscle_filter")) {Text(stringResource(muscle?.let(::muscleLabel)?:R.string.all_muscles))}
            DropdownMenu(expanded,onDismissRequest={expanded=false}) {
                DropdownMenuItem(text={Text(stringResource(R.string.all_muscles))},onClick={muscle=null;expanded=false})
                Muscle.entries.forEach {value ->DropdownMenuItem(text={Text(stringResource(muscleLabel(value)))},onClick={muscle=value;expanded=false},modifier=Modifier.testTag("filter_muscle_${value.name}"))}
            }
        }
    }
    val hasActiveFilter=search.isNotBlank()||equipment!=null||muscle!=null||homeOnly||favoritesOnly||recentOnly||discomfortExcluded.isNotEmpty()
    Row(Modifier.fillMaxWidth().bleedToPageEdges().horizontalScroll(rememberScrollState()).padding(horizontal=PageGutter),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        FilterChip(homeOnly,{homeOnly=!homeOnly;if(homeOnly) equipment=null},label={Text(stringResource(R.string.preset_home_filter))},modifier=Modifier.testTag("home_filter"))
        FilterChip(!homeOnly && equipment==null,{homeOnly=false;equipment=null},label={Text(stringResource(R.string.preset_gym_filter))},modifier=Modifier.testTag("gym_filter"))
        FilterChip(favoritesOnly,{favoritesOnly=!favoritesOnly},label={Text(stringResource(R.string.favorites))},modifier=Modifier.testTag("favorites_filter"))
        FilterChip(recentOnly,{recentOnly=!recentOnly},label={Text(stringResource(R.string.recent_exercises))},modifier=Modifier.testTag("recent_filter"))
        FilterChip(
            selected=discomfortExcluded.isNotEmpty(),
            onClick={showDiscomfortDialog=true},
            label={
                val base=stringResource(R.string.discomfort_filters)
                Text(if(discomfortExcluded.isEmpty()) base else "$base (${number(discomfortExcluded.size.toDouble(),0)})")
            },
            modifier=Modifier.testTag("discomfort_filter_chip"),
        )
        if(hasActiveFilter) TextButton(onClick={search="";equipment=null;muscle=null;homeOnly=false;favoritesOnly=false;recentOnly=false;saveDiscomfort(emptySet())},modifier=Modifier.testTag("clear_exercise_filters")) {Text(stringResource(R.string.clear_filters))}
    }
    if(showDiscomfortDialog) {
        AlertDialog(
            onDismissRequest={showDiscomfortDialog=false},
            title={Text(stringResource(R.string.discomfort_filters),fontWeight=FontWeight.Bold)},
            text={
                Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.discomfort_filter_explainer),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    ExerciseDiscomfortRegion.entries.forEach {region ->
                        val checked=region in discomfortExcluded
                        val labelRes=when(region) {
                            ExerciseDiscomfortRegion.NECK_UPPER_BACK -> R.string.discomfort_region_neck_upper_back
                            ExerciseDiscomfortRegion.SHOULDERS -> R.string.discomfort_region_shoulders
                            ExerciseDiscomfortRegion.LOWER_BACK -> R.string.discomfort_region_lower_back
                        }
                        val hiddenCount=remember(region) {Exercise.entries.count {!ExerciseDiscomfortSelection.includes(it,setOf(region))}}
                        val muscleText=region.directlyTargetedMuscles.map {stringResource(muscleLabel(it))}.sorted().joinToString(" · ")
                        Surface(
                            shape=RoundedCornerShape(14.dp),
                            color=if(checked) MaterialTheme.colorScheme.secondaryContainer.copy(alpha=0.55f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border=BorderStroke(if(checked) 1.5.dp else 1.dp, if(checked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant),
                            modifier=Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .toggleable(value=checked,role=androidx.compose.ui.semantics.Role.Checkbox) {
                                    saveDiscomfort(if(it) discomfortExcluded+region else discomfortExcluded-region)
                                }
                                .testTag("discomfort_region_${region.name.lowercase()}"),
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=10.dp),
                                verticalAlignment=Alignment.CenterVertically,
                                horizontalArrangement=Arrangement.spacedBy(10.dp),
                            ) {
                                Checkbox(checked=checked,onCheckedChange=null)
                                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                                    Text(stringResource(labelRes),style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold)
                                    Text(muscleText,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                PillBadge(
                                    text="-${number(hiddenCount.toDouble(),0)}",
                                    containerColor=if(checked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
                                    contentColor=if(checked) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Surface(
                        shape=RoundedCornerShape(12.dp),
                        color=MaterialTheme.colorScheme.surfaceContainerLow,
                        border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.6f)),
                    ) {
                        Text(
                            stringResource(R.string.discomfort_filter_limits),
                            Modifier.padding(10.dp),
                            style=MaterialTheme.typography.bodySmall,
                            color=MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton={TextButton(onClick={showDiscomfortDialog=false}) {Text(stringResource(R.string.close))}},
            dismissButton={
                if(discomfortExcluded.isNotEmpty()) {
                    TextButton(onClick={saveDiscomfort(emptySet())},modifier=Modifier.testTag("clear_discomfort_filters")) {
                        Text(stringResource(R.string.clear_discomfort_filters))
                    }
                }
            },
        )
    }
    // Muscle filtering lives in the dropdown above; a second chip row for the same filter was removed.
    val favorites=state.snapshot.exercisePreferences.filter {it.favorite}.map {it.exercise}.toSet()
    val completed=state.snapshot.sessions.filter {it.completedAt!=null}.map {it.id}.toSet()
    val recent=state.snapshot.sets.filter {it.sessionId in completed}.map {it.exercise}.toSet()
    val names=Exercise.entries.associateWith {stringResource(exerciseLabel(it))+" "+stringResource(guideResources(it).variant)+" "+ExerciseMuscles.of(it).primary.map {m ->stringResource(muscleLabel(m))}.joinToString()+" "+ExerciseMuscles.of(it).primaryDetail.map {d ->stringResource(muscleDetailLabel(d))}.joinToString()+" "+ExerciseCatalog.of(it).equipment.map {e->stringResource(equipmentLabel(e))}.joinToString()}
    val homeEquipment = remember { setOf(Equipment.BODYWEIGHT, Equipment.DUMBBELLS, Equipment.BENCH, Equipment.BOX, Equipment.PULL_UP_BAR) }
    val matches=Exercise.entries.filter {ExerciseDiscomfortSelection.includes(it,discomfortExcluded)&&(!homeOnly||ExerciseCatalog.of(it).equipment.all {eq -> eq in homeEquipment})&&(!favoritesOnly||it in favorites)&&(!recentOnly||it in recent)&&(equipment==null||equipment in ExerciseCatalog.of(it).equipment)&&(muscle==null||muscle in ExerciseMuscles.of(it).primary)&&com.formacoach.coach.ExerciseSearch.matches(search,names.getValue(it)+" "+ExerciseCatalog.searchText(it))}
    CustomExercisesSection(state,vm,today)
    Text(stringResource(R.string.exercise_results,number(matches.size.toDouble(),0),number(Exercise.entries.size.toDouble(),0)),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
    if(matches.isEmpty()) {
        Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            EmptyState(R.string.no_matching_exercises,R.string.exercise_search_hint)
            if(hasActiveFilter) {
                OutlinedButton(
                    onClick={search="";equipment=null;muscle=null;homeOnly=false;favoritesOnly=false;recentOnly=false;saveDiscomfort(emptySet())},
                    modifier=Modifier.heightIn(min=48.dp).testTag("empty_clear_exercise_filters"),
                ) {Text(stringResource(R.string.clear_filters))}
            }
        }
    }
    val visible=matches.take(limit)
    val grouped=visible.groupBy {ExerciseMuscles.of(it).primary.first()}
    grouped.forEach {(groupMuscle,groupItems) ->
        Row(Modifier.fillMaxWidth().padding(top=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Surface(color=MaterialTheme.colorScheme.primary,shape=RoundedCornerShape(4.dp),modifier=Modifier.width(4.dp).height(18.dp)) {}
            Text(stringResource(muscleLabel(groupMuscle)),Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
            PillBadge(number(groupItems.size.toDouble(),0))
        }
        groupItems.chunked(columns).forEach {row -> Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        row.forEach {exercise ->
            val isFavorite = exercise in favorites
            Card(
                onClick={opened=exercise},
                modifier=Modifier.weight(1f).testTag("library_${exercise.name}"),
                colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),
                border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.65f)),
                shape=RoundedCornerShape(20.dp),
            ) {
                Box(Modifier.fillMaxWidth()) {
                    ExerciseThumbnail(exercise,Modifier.fillMaxWidth())
                    val hasVideoClip = remember(exercise) { exerciseVideoResource(context, exercise) != 0 }
                    val hasMotionClip = remember(exercise) { ENABLE_PROCEDURAL_3D_MOTION && ExerciseMotionCatalog.of(exercise) != null }
                    if (hasVideoClip || hasMotionClip) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.background.copy(alpha = 0.78f),
                            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                        ) {
                            Text(
                                text = stringResource(if (hasVideoClip) R.string.badge_video_short else R.string.badge_3d_short),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Surface(
                        shape=CircleShape,
                        color=MaterialTheme.colorScheme.background.copy(alpha=0.72f),
                        modifier=Modifier.align(Alignment.TopEnd).padding(4.dp).size(48.dp),
                    ) {
                        IconButton(
                            onClick={vm.favorite(exercise,!isFavorite)},
                            enabled=!state.busy,
                            modifier=Modifier.testTag("favorite_${exercise.name}"),
                        ) {
                            Icon(
                                if(isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription=stringResource(if(isFavorite) R.string.remove_favorite else R.string.add_favorite),
                                tint=if(isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier=Modifier.size(18.dp),
                            )
                        }
                    }
                }
                Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(exerciseLabel(exercise)),
                        style=MaterialTheme.typography.titleMedium,
                        fontWeight=FontWeight.Bold,
                        maxLines=2,
                        overflow=TextOverflow.Ellipsis,
                    )
                    PillBadge(stringResource(muscleDetailLabel(ExerciseMuscles.of(exercise).primaryDetail.first())))
                    Text(
                        ExerciseCatalog.of(exercise).equipment.map {stringResource(equipmentLabel(it))}.joinToString(" · "),
                        style=MaterialTheme.typography.labelSmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines=1,
                        overflow=TextOverflow.Ellipsis,
                    )
                    FilledTonalButton(
                        onClick = { recording = exercise },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.fillMaxWidth().testTag("quick_record_${exercise.name}"),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.record_exercise), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        repeat(columns-row.size) {Spacer(Modifier.weight(1f))}
    }}
    }
    if(matches.size>limit) TextButton(onClick={limit+=12},modifier=Modifier.testTag("more_exercises")) {Text(stringResource(R.string.show_more))}
    opened?.let { exercise ->
        ExerciseGuideSheet(
            exercise = exercise,
            snapshot = state.snapshot,
            onRecord = { recording = exercise },
            tools = { ExerciseTools(exercise, state, vm, onSimilar = { opened = it }) },
            onDismiss = { opened = null },
        )
    }
    recording?.let { exercise ->
        QuickRecordExerciseSheet(
            exercise = exercise,
            state = state,
            today = today,
            vm = vm,
            onSaved = {
                recording = null
                onSetRecorded()
            },
            onDismiss = { recording = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickRecordExerciseSheet(
    exercise: Exercise,
    state: AppState,
    today: LocalDate,
    vm: FitnessViewModel,
    onSaved: () -> Unit,
    onDismiss: () -> Unit,
) {
    val unit = LocalWeightUnit.current
    val unitLabel = weightUnitLabel()
    val locale = LocalConfiguration.current.locales[0]
    val haptics = LocalHapticFeedback.current
    val previous = remember(state.snapshot, exercise) {
        state.snapshot.sets.lastOrNull { it.exercise == exercise }
    }
    var reps by rememberSaveable(exercise) {
        mutableStateOf(previous?.reps?.let { formatInputNumber(it.toDouble(), locale, 0) } ?: "8")
    }
    var weight by rememberSaveable(exercise) {
        mutableStateOf(previous?.weightKg?.let { formatInputNumber(WeightUnits.fromKg(it, unit), locale, 2) } ?: "0")
    }
    var inputUnit by rememberSaveable(exercise) { mutableStateOf(unit) }
    LaunchedEffect(unit) {
        if (inputUnit != unit) {
            NumericInput.decimal(weight)?.let { parsed ->
                weight = formatInputNumber(WeightUnits.convert(parsed, inputUnit, unit), locale, 2)
            }
            inputUnit = unit
        }
    }
    var warmup by rememberSaveable(exercise) { mutableStateOf(false) }
    var rpe by rememberSaveable(exercise) { mutableStateOf("") }
    val valid = runCatching {
        val r = requireNotNull(NumericInput.integer(reps))
        val w = WeightUnits.toKg(requireNotNull(NumericInput.decimal(weight)), unit)
        val e = if (rpe.isBlank()) null else requireNotNull(NumericInput.decimal(rpe))
        require(r in 1..1000 && w in 0.0..1000.0 && (e == null || e in 0.0..10.0))
        Triple(r, w, e)
    }.getOrNull()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExerciseThumbnail(exercise, Modifier.width(68.dp).clip(RoundedCornerShape(10.dp)))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(exerciseLabel(exercise)), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    PillBadge(stringResource(muscleDetailLabel(ExerciseMuscles.of(exercise).primaryDetail.first())))
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, stringResource(R.string.close)) }
            }
            previous?.let { last ->
                Text(
                    stringResource(R.string.previous_set, number(last.reps.toDouble(), 0), formatLoad(last.weightKg, last.exercise)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FormFieldPair(
                first = {
                    StepperInput(
                        label = R.string.reps,
                        value = reps,
                        onChange = { reps = it },
                        tag = "quick_reps",
                        step = 1.0,
                        min = 1.0,
                        max = 1000.0,
                        integer = true,
                    )
                },
                second = {
                    StepperInput(
                        label = if (unit == WeightUnit.KG) R.string.weight_kg else R.string.weight_lb,
                        value = weight,
                        onChange = { weight = it },
                        tag = "quick_weight",
                        step = WeightUnits.step(unit),
                        min = 0.0,
                        max = if (unit == WeightUnit.KG) 1000.0 else 2200.0,
                        integer = false,
                    )
                },
            )
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = warmup, onClick = { warmup = !warmup }, label = { Text(stringResource(R.string.warmup_set)) })
                WeightUnits.quickBumps(unit).forEach { bump ->
                    AssistChip(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val cur = NumericInput.decimal(weight) ?: 0.0
                            val cap = if (unit == WeightUnit.KG) 1000.0 else 2200.0
                            weight = formatInputNumber((cur + bump).coerceAtMost(cap), locale, 2)
                        },
                        label = { Text("+${number(bump)} $unitLabel", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) },
                    )
                }
            }
            Button(
                onClick = {
                    if (vm.state.value.busy) return@Button
                    valid?.let { (r, w, e) ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.quickLogExerciseSet(
                            exercise = exercise,
                            reps = r,
                            kg = w,
                            kind = if (warmup) SetKind.WARM_UP else SetKind.WORKING,
                            rpe = e,
                            date = today,
                            done = onSaved,
                        )
                    }
                },
                enabled = valid != null && !state.busy,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("quick_save_set"),
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.add_set), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ExerciseTools(exercise:Exercise,state:AppState,vm:FitnessViewModel,onSimilar:(Exercise)->Unit) {
    val preference=state.snapshot.exercisePreferences.firstOrNull {it.exercise==exercise}
    var editing by rememberSaveable(exercise) {mutableStateOf(false)}
    var similar by rememberSaveable(exercise) {mutableStateOf(false)}
    val details=ExerciseCatalog.of(exercise)
    Text(details.equipment.map {stringResource(equipmentLabel(it))}.joinToString(" · "),style=MaterialTheme.typography.labelLarge)
    if(details.perSide) Text(stringResource(R.string.per_side_note),style=MaterialTheme.typography.bodySmall)
    if(!preference?.notes.isNullOrBlank()) Text(preference!!.notes,style=MaterialTheme.typography.bodyMedium)
    FlowRow {
        TextButton(onClick={vm.favorite(exercise,preference?.favorite!=true)},enabled=!state.busy) {Text(stringResource(if(preference?.favorite==true) R.string.remove_favorite else R.string.add_favorite))}
        TextButton(onClick={editing=true},modifier=Modifier.testTag("exercise_notes")) {Text(stringResource(R.string.personal_notes))}
    }
    TextButton(onClick={similar=!similar},modifier=Modifier.testTag("similar_exercises")) {Text(stringResource(R.string.similar_focus))}
    if(similar) {
        Text(stringResource(R.string.similar_note),style=MaterialTheme.typography.bodySmall)
        ExerciseCatalog.similar(exercise).forEach {other ->TextButton(onClick={onSimilar(other)}) {Text(stringResource(exerciseLabel(other)))}}
    }
    if(editing) {
        var notes by remember(exercise) {mutableStateOf(preference?.notes.orEmpty())}
        AlertDialog(onDismissRequest={editing=false},title={Text(stringResource(R.string.personal_notes))},text={
            OutlinedTextField(notes,{if(it.length<=2000) notes=it},modifier=Modifier.fillMaxWidth().testTag("exercise_notes_input"),minLines=3,supportingText={Text(stringResource(R.string.notes_limit,number(notes.length.toDouble(),0)))})
        },confirmButton={TextButton(onClick={vm.exerciseNotes(exercise,notes){editing=false}},enabled=!state.busy,modifier=Modifier.testTag("save_exercise_notes")) {Text(stringResource(R.string.save))}},dismissButton={TextButton(onClick={editing=false}) {Text(stringResource(R.string.cancel))}})
    }
}
