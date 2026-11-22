package com.formacoach.app

import android.graphics.BitmapFactory
import android.provider.Settings
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
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
import com.formacoach.model.*
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
internal fun ExerciseIllustration(exercise:Exercise,onOpen:(()->Unit)?=null) {
    val guide=guideResources(exercise)
    val description=stringResource(R.string.exercise_visual_description,stringResource(guide.variant))
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).then(if(onOpen!=null) Modifier.clickable(onClick=onOpen).testTag("open_illustration") else Modifier)) {
        Image(painterResource(guide.image),contentDescription=description,contentScale=ContentScale.Fit,
            modifier=Modifier.fillMaxWidth().aspectRatio(1.5f).testTag("exercise_art_${exercise.name}"))
        if(onOpen!=null) Icon(Icons.Default.OpenInFull,contentDescription=stringResource(R.string.enlarge_image),modifier=Modifier.align(Alignment.BottomEnd).padding(12.dp).size(20.dp),tint=MaterialTheme.colorScheme.primary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExerciseGuideSheet(exercise:Exercise,tools:@Composable ()->Unit={},onDismiss:()->Unit) {
    var tab by rememberSaveable(exercise) {mutableIntStateOf(0)}
    var enlarged by rememberSaveable(exercise) {mutableStateOf(false)}
    val guide=guideResources(exercise)
    val uri=LocalUriHandler.current
    val guideScroll=rememberScrollState()
    LaunchedEffect(exercise) {guideScroll.scrollTo(0)}
    ModalBottomSheet(onDismissRequest=onDismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().heightIn(max=850.dp).verticalScroll(guideScroll).padding(horizontal=20.dp).navigationBarsPadding().padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(exerciseLabel(exercise)),style=MaterialTheme.typography.headlineSmall)
                    Text(stringResource(guide.variant),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick=onDismiss,modifier=Modifier.testTag("guide_close")) {Icon(Icons.Default.Close,stringResource(R.string.close))}
            }
            TabRow(selectedTabIndex=tab,containerColor=MaterialTheme.colorScheme.background) {
                listOf(R.string.technique,R.string.muscles_working).forEachIndexed {index,label ->
                    Tab(selected=tab==index,onClick={tab=index},text={Text(stringResource(label))},modifier=Modifier.testTag(if(index==0) "guide_tab_technique" else "guide_tab_muscles"))
                }
            }
            if(tab==0) {
                val context = LocalContext.current
                val reduceMotion = remember {
                    runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false)
                }
                var animating by rememberSaveable(exercise) { mutableStateOf(false) }
                if (animating && !reduceMotion) {
                    AnimatedExerciseIllustration(exercise) { enlarged = true }
                } else {
                    ExerciseIllustration(exercise) { enlarged = true }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    if (!animating || reduceMotion) {
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
private fun AnimatedExerciseIllustration(exercise: Exercise, onOpen: () -> Unit) {
    val guide = guideResources(exercise)
    val bitmap = ImageBitmap.imageResource(guide.image)
    var showSecond by remember(exercise) { mutableStateOf(false) }
    LaunchedEffect(exercise) {
        while (true) {
            kotlinx.coroutines.delay(1500)
            showSecond = !showSecond
        }
    }
    val alphaSecond by animateFloatAsState(if (showSecond) 1f else 0f, tween(550), label = "phase_crossfade")
    val description = stringResource(R.string.exercise_visual_description, stringResource(guide.variant))
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onOpen).testTag("open_illustration")) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1.25f).semantics { contentDescription = description }.testTag("exercise_art_${exercise.name}")) {
            val halfW = bitmap.width / 2
            val factor = minOf(size.width / halfW, size.height / bitmap.height)
            val w = (halfW * factor).roundToInt()
            val h = (bitmap.height * factor).roundToInt()
            val dx = ((size.width - w) / 2).roundToInt()
            val dy = ((size.height - h) / 2).roundToInt()
            drawImage(bitmap, srcOffset = IntOffset(0, 0), srcSize = IntSize(halfW, bitmap.height), dstOffset = IntOffset(dx, dy), dstSize = IntSize(w, h), alpha = 1f - alphaSecond)
            drawImage(bitmap, srcOffset = IntOffset(halfW, 0), srcSize = IntSize(halfW, bitmap.height), dstOffset = IntOffset(dx, dy), dstSize = IntSize(w, h), alpha = alphaSecond)
        }
        PillBadge(
            text = stringResource(if (showSecond) R.string.phase_movement else R.string.phase_start),
            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
        )
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
internal fun ExerciseLibrary(state:AppState,vm:FitnessViewModel) {
    val columns=if(LocalConfiguration.current.screenWidthDp>=840) 3 else 2
    var search by rememberSaveable {mutableStateOf("")}
    var opened by rememberSaveable {mutableStateOf<Exercise?>(null)}
    var equipment by rememberSaveable {mutableStateOf<Equipment?>(null)}
    var muscle by rememberSaveable {mutableStateOf<Muscle?>(null)}
    var favoritesOnly by rememberSaveable {mutableStateOf(false)}
    var recentOnly by rememberSaveable {mutableStateOf(false)}
    var limit by rememberSaveable {mutableIntStateOf(12)}
    LaunchedEffect(search,equipment,muscle,favoritesOnly,recentOnly) {limit=12}
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
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        FilterChip(favoritesOnly,{favoritesOnly=!favoritesOnly},label={Text(stringResource(R.string.favorites))},modifier=Modifier.testTag("favorites_filter"))
        FilterChip(recentOnly,{recentOnly=!recentOnly},label={Text(stringResource(R.string.recent_exercises))},modifier=Modifier.testTag("recent_filter"))
        if(search.isNotBlank()||equipment!=null||muscle!=null||favoritesOnly||recentOnly) TextButton(onClick={search="";equipment=null;muscle=null;favoritesOnly=false;recentOnly=false},modifier=Modifier.testTag("clear_exercise_filters")) {Text(stringResource(R.string.clear_filters))}
    }
    val mainMuscles=remember {Muscle.entries.filter {m ->Exercise.entries.any {ExerciseMuscles.of(it).primary.first()==m}}}
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        FilterChip(muscle==null,{muscle=null},label={Text(stringResource(R.string.all_muscles))})
        mainMuscles.forEach {m ->
            val count=Exercise.entries.count {ExerciseMuscles.of(it).primary.first()==m}
            FilterChip(muscle==m,{muscle=if(muscle==m) null else m},label={Text("${stringResource(muscleLabel(m))} ($count)")})
        }
    }
    val favorites=state.snapshot.exercisePreferences.filter {it.favorite}.map {it.exercise}.toSet()
    val completed=state.snapshot.sessions.filter {it.completedAt!=null}.map {it.id}.toSet()
    val recent=state.snapshot.sets.filter {it.sessionId in completed}.map {it.exercise}.toSet()
    val names=Exercise.entries.associateWith {stringResource(exerciseLabel(it))+" "+stringResource(guideResources(it).variant)+" "+ExerciseMuscles.of(it).primary.map {m ->stringResource(muscleLabel(m))}.joinToString()+" "+ExerciseCatalog.of(it).equipment.map {e->stringResource(equipmentLabel(e))}.joinToString()}
    val matches=Exercise.entries.filter {(!favoritesOnly||it in favorites)&&(!recentOnly||it in recent)&&(equipment==null||equipment in ExerciseCatalog.of(it).equipment)&&(muscle==null||muscle in ExerciseMuscles.of(it).primary)&&com.formacoach.coach.ExerciseSearch.matches(search,names.getValue(it)+" "+ExerciseCatalog.searchText(it))}
    Text(stringResource(R.string.exercise_results,number(matches.size.toDouble(),0),number(Exercise.entries.size.toDouble(),0)),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
    if(matches.isEmpty()) EmptyState(R.string.no_matching_exercises,R.string.exercise_search_hint)
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
                    Surface(
                        shape=CircleShape,
                        color=MaterialTheme.colorScheme.background.copy(alpha=0.72f),
                        modifier=Modifier.align(Alignment.TopEnd).padding(8.dp).size(34.dp),
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
                    PillBadge(stringResource(muscleLabel(ExerciseMuscles.of(exercise).primary.first())))
                    Text(
                        ExerciseCatalog.of(exercise).equipment.map {stringResource(equipmentLabel(it))}.joinToString(" · "),
                        style=MaterialTheme.typography.labelSmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines=1,
                        overflow=TextOverflow.Ellipsis,
                    )
                }
            }
        }
        repeat(columns-row.size) {Spacer(Modifier.weight(1f))}
    }}
    }
    if(matches.size>limit) TextButton(onClick={limit+=12},modifier=Modifier.testTag("more_exercises")) {Text(stringResource(R.string.show_more))}
    opened?.let {exercise ->ExerciseGuideSheet(exercise,tools={ExerciseTools(exercise,state,vm,onSimilar={opened=it})},onDismiss={opened=null})}
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
