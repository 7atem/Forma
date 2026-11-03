package com.formacoach.app

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    val bitmap by produceState(thumbnailCache.get(id),id) {
        if(value==null) value=withContext(Dispatchers.IO) {
            BitmapFactory.decodeResource(resources,id,BitmapFactory.Options().apply {inSampleSize=2}).asImageBitmap().also {thumbnailCache.put(id,it)}
        }
    }
    val image=bitmap
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
internal fun ExerciseGuideSheet(exercise:Exercise,onDismiss:()->Unit) {
    var tab by rememberSaveable(exercise) {mutableIntStateOf(0)}
    var enlarged by rememberSaveable(exercise) {mutableStateOf(false)}
    val guide=guideResources(exercise)
    val uri=LocalUriHandler.current
    ModalBottomSheet(onDismissRequest=onDismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().heightIn(max=850.dp).verticalScroll(rememberScrollState()).padding(horizontal=20.dp).navigationBarsPadding().padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
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
                ExerciseIllustration(exercise){enlarged=true}
                PhaseLabels()
                guide.cues.forEachIndexed {index,cue -> Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.Top) {
                    Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.primaryContainer) {Text(number((index+1).toDouble(),0),Modifier.padding(horizontal=11.dp,vertical=7.dp),color=MaterialTheme.colorScheme.onPrimaryContainer)}
                    Text(stringResource(cue),Modifier.weight(1f).padding(top=4.dp),style=MaterialTheme.typography.bodyLarge)
                }}
                Text(stringResource(R.string.guide_scope),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick={runCatching {uri.openUri(guide.reference)}}) {Text(stringResource(R.string.exercise_reference))}
            } else MuscleMap(exercise)
        }
    }
    if(enlarged) ExerciseImageViewer(exercise){enlarged=false}
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
internal fun ExerciseLibrary() {
    var search by rememberSaveable {mutableStateOf("")}
    var opened by rememberSaveable {mutableStateOf<Exercise?>(null)}
    Input(R.string.search_exercises,search,{search=it},"exercise_search",numeric=false)
    val names=Exercise.entries.associateWith {stringResource(exerciseLabel(it))+" "+stringResource(guideResources(it).variant)+" "+ExerciseMuscles.of(it).primary.map {muscle ->stringResource(muscleLabel(muscle))}.joinToString()}
    val matches=Exercise.entries.filter {search.isBlank() || names.getValue(it).contains(search,true)}
    if(matches.isEmpty()) EmptyState(R.string.no_matching_exercises,R.string.exercise_search_hint)
    matches.chunked(2).forEach {row -> Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        row.forEach {exercise ->
            Card(onClick={opened=exercise},modifier=Modifier.weight(1f).testTag("library_${exercise.name}"),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),shape=RoundedCornerShape(20.dp)) {
                ExerciseThumbnail(exercise,Modifier.fillMaxWidth())
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(exerciseLabel(exercise)),style=MaterialTheme.typography.titleMedium)
                    Text(stringResource(muscleLabel(ExerciseMuscles.of(exercise).primary.first())),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.view_exercise),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if(row.size==1) Spacer(Modifier.weight(1f))
    }}
    opened?.let {ExerciseGuideSheet(it){opened=null}}
}
