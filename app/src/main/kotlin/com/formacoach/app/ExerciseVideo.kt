package com.formacoach.app

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
internal fun ExerciseVideo(
    exercise: Exercise,
    playing: Boolean,
    onPlayingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showMuscleHighlights: Boolean = true,
) {
    val context = LocalContext.current
    val resource = remember(exercise) { exerciseVideoResource(context, exercise) }
    if (resource == 0) return
    // Slow motion helps study the movement; it is a viewing speed only and never changes the clip.
    var slow by rememberSaveable(exercise) { mutableStateOf(false) }
    var progress by remember(exercise) { mutableFloatStateOf(0f) }
    var player by remember(exercise) { mutableStateOf<SilentLoopTextureView?>(null) }
    var pulse by remember { mutableStateOf(false) }
    LaunchedEffect(playing, showMuscleHighlights) {
        while (playing && showMuscleHighlights) {
            delay(1300)
            pulse = !pulse
        }
    }
    LaunchedEffect(playing, player) {
        while (playing) {
            player?.fraction()?.let { progress = it }
            delay(100)
        }
    }
    val activation = remember(exercise) { ExerciseMuscles.of(exercise) }
    val muscles = (activation.primary + activation.secondary).map { stringResource(muscleLabel(it)) }.joinToString()
    val description = stringResource(R.string.motion_video_description, stringResource(exerciseLabel(exercise)), muscles)
    val toggleLabel = stringResource(if (playing) R.string.pause_video else R.string.play_video)
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
                        setPlaying(playing)
                        setSlow(slow)
                        player = this
                    }
                },
                update = { view -> view.setPlaying(playing); view.setSlow(slow) },
                onRelease = { it.releasePlayer(); player = null },
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            )
        }
        // The whole frame toggles playback, like a standard video player.
        Box(
            Modifier
                .matchParentSize()
                .clickable(onClickLabel = toggleLabel) { onPlayingChange(!playing) }
                .testTag("exercise_video_surface"),
        )
        if (showMuscleHighlights) {
            CompactMuscleOverlay(exercise, pulsePeak = pulse, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
        }
        AnimatedVisibility(!playing, Modifier.align(Alignment.Center), enter = fadeIn(), exit = fadeOut()) {
            Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.55f), contentColor = Color.White) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.padding(14.dp).size(40.dp))
            }
        }
        // Control bar: play/pause and speed, over a soft scrim so it stays readable on light frames.
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onPlayingChange(!playing) }, modifier = Modifier.testTag("exercise_video_play_toggle")) {
                Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = toggleLabel, tint = Color.White)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f),
                drawStopIndicator = {},
                gapSize = 0.dp,
            )
            val speedLabel = stringResource(if (slow) R.string.video_speed_normal else R.string.video_speed_slow)
            TextButton(
                onClick = { slow = !slow },
                modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = speedLabel }.testTag("exercise_video_speed"),
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
            ) { Text(if (slow) "0.5×" else "1×", fontWeight = FontWeight.Bold) }
        }
    }
}

/**
 * TextureView-backed silent looping player so Compose rounded-corner clipping, sheet scrolling,
 * and muscle overlays work cleanly without SurfaceView z-order punch-through or black flashes.
 */
internal class SilentLoopTextureView(
    context: Context,
    private val uri: Uri,
) : TextureView(context), TextureView.SurfaceTextureListener {
    private var player: MediaPlayer? = null
    private var surface: Surface? = null
    private var prepared = false
    private var desiredPlaying = false
    private var slow = false

    /** Position within the loop, 0..1, or null before the clip is ready. */
    fun fraction(): Float? {
        val mp = player ?: return null
        if (!prepared) return null
        return runCatching { mp.currentPosition.toFloat() / mp.duration.coerceAtLeast(1) }.getOrNull()?.coerceIn(0f, 1f)
    }

    fun setSlow(value: Boolean) {
        slow = value
        applySpeed()
    }

    private fun applySpeed() {
        val mp = player ?: return
        if (!prepared) return
        val wasPlaying = mp.isPlaying
        // Setting params on a paused player starts it, so restore the paused state afterwards.
        runCatching { mp.playbackParams = mp.playbackParams.setSpeed(if (slow) 0.5f else 1f) }
        if (!wasPlaying && mp.isPlaying) mp.pause()
    }

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
                if (slow) applySpeed()
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
