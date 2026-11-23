package com.formacoach.app

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .testTag("exercise_video")
            .semantics { contentDescription = description },
    ) {
        key(resource) {
            AndroidView(
                factory = { ctx ->
                    SilentLoopTextureView(ctx, Uri.parse("android.resource://${ctx.packageName}/$resource")).apply {
                        importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                        setPlaying(play)
                    }
                },
                update = { view -> view.setPlaying(play) },
                onRelease = { it.releasePlayer() },
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            )
        }
        if (showMuscleHighlights) {
            CompactMuscleOverlay(exercise, pulsePeak = pulse, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
        }
    }
}

/**
 * TextureView-backed silent looping player so Compose rounded-corner clipping, sheet scrolling,
 * and muscle overlays work cleanly without SurfaceView z-order punch-through or black flashes.
 */
private class SilentLoopTextureView(
    context: Context,
    private val uri: Uri,
) : TextureView(context), TextureView.SurfaceTextureListener {
    private var player: MediaPlayer? = null
    private var surface: Surface? = null
    private var prepared = false
    private var desiredPlaying = false

    init {
        isOpaque = false
        surfaceTextureListener = this
    }

    fun setPlaying(play: Boolean) {
        desiredPlaying = play
        val mp = player ?: return
        if (!prepared) return
        if (play && !mp.isPlaying) mp.start() else if (!play && mp.isPlaying) mp.pause()
    }

    fun releasePlayer() {
        prepared = false
        player?.let { mp ->
            runCatching { mp.stop() }
            runCatching { mp.release() }
        }
        player = null
        surface?.release()
        surface = null
    }

    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
        releasePlayer()
        val s = Surface(st)
        surface = s
        val mp = MediaPlayer()
        player = mp
        runCatching {
            mp.setDataSource(context, uri)
            mp.setSurface(s)
            mp.isLooping = true
            mp.setVolume(0f, 0f)
            mp.setOnPreparedListener { ready ->
                prepared = true
                ready.setVolume(0f, 0f)
                if (desiredPlaying) ready.start() else ready.seekTo(1)
            }
            mp.prepareAsync()
        }.onFailure {
            releasePlayer()
        }
    }

    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) = Unit

    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
        releasePlayer()
        return true
    }

    override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
}
