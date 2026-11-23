package com.formacoach.app

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.formacoach.model.Exercise
import com.formacoach.model.ExerciseMuscles
import kotlinx.coroutines.delay

/**
 * Bundled, reviewed AI exercise clip (res/raw/exercise_video_<id>.mp4), silent and looping.
 * Working muscles come from the catalog overlay, never from the video. Returns 0 when absent.
 */
@SuppressLint("DiscouragedApi") // Clips are optional per exercise; res/raw/keep.xml keeps them in release.
internal fun exerciseVideoResource(context: Context, exercise: Exercise): Int =
    context.resources.getIdentifier("exercise_video_${exercise.name.lowercase()}", "raw", context.packageName)

@Composable
internal fun ExerciseVideo(exercise: Exercise, playing: Boolean, modifier: Modifier = Modifier, showMuscleHighlights: Boolean = true) {
    val context = LocalContext.current
    val resource = remember(exercise) { exerciseVideoResource(context, exercise) }
    if (resource == 0) return
    val play = playing && !rememberReduceMotion()
    var pulse by remember { mutableStateOf(false) }
    LaunchedEffect(play, showMuscleHighlights) {
        while (play && showMuscleHighlights) {
            delay(1300)
            pulse = !pulse
        }
    }
    val activation = remember(exercise) { ExerciseMuscles.of(exercise) }
    val muscles = (activation.primary + activation.secondary).map { stringResource(muscleLabel(it)) }.joinToString()
    val description = stringResource(R.string.motion_video_description, stringResource(exerciseLabel(exercise)), muscles)
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .testTag("exercise_video")
            .semantics { contentDescription = description },
    ) {
        key(resource) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        // Silent demo: never take audio focus away from the user's music or podcast.
                        setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
                        importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                        setVideoURI(Uri.parse("android.resource://${ctx.packageName}/$resource"))
                        setOnPreparedListener { player ->
                            player.isLooping = true
                            player.setVolume(0f, 0f)
                            if (play) start() else seekTo(1)
                        }
                    }
                },
                update = { view -> if (play && !view.isPlaying) view.start() else if (!play && view.isPlaying) view.pause() },
                onRelease = { it.stopPlayback() },
                modifier = Modifier.matchParentSizeCompat(),
            )
        }
        if (showMuscleHighlights) {
            CompactMuscleOverlay(exercise, pulsePeak = pulse, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
        }
    }
}

private fun Modifier.matchParentSizeCompat(): Modifier = this.then(Modifier.fillMaxWidth().aspectRatio(1f))
