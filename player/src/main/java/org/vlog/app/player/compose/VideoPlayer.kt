package org.vlog.app.player.compose

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.FloatRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.util.RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL
import androidx.media3.common.util.RepeatModeUtil.REPEAT_TOGGLE_MODE_NONE
import androidx.media3.common.util.RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerView
import org.vlog.app.player.compose.controller.VideoPlayerControllerConfig
import org.vlog.app.player.compose.controller.applyToExoPlayerView
import org.vlog.app.player.compose.uri.VideoPlayerMediaItem
import org.vlog.app.player.compose.uri.toUri
import org.vlog.app.player.compose.util.findActivity
import org.vlog.app.player.compose.util.setFullScreen
import kotlinx.coroutines.Dispatchers
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * [VideoPlayer] is UI component that can play video in Jetpack Compose. It works based on ExoPlayer.
 * You can play local (e.g. asset files, resource files) files and all video files in the network environment.
 * For all video formats supported by the [VideoPlayer] component, see the ExoPlayer link below.
 *
 * If you rotate the screen, the default action is to reset the player state.
 * To prevent this happening, put the following options in the `android:configChanges` option of your app's AndroidManifest.xml to keep the settings.
 * ```
 * keyboard|keyboardHidden|orientation|screenSize|screenLayout|smallestScreenSize|uiMode
 * ```
 *
 * This component is linked with Compose [androidx.compose.runtime.DisposableEffect].
 * This means that it move out of the Composable Scope, the ExoPlayer instance will automatically be destroyed as well.
 *
 * @see <a href="https://exoplayer.dev/supported-formats.html">Exoplayer Support Formats</a>
 *
 * @param modifier Modifier to apply to this layout node.
 * @param mediaItems [VideoPlayerMediaItem] to be played by the video player. The reason for receiving media items as an array is to configure multi-track. If it's a single track, provide a single list (e.g. listOf(mediaItem)).
 * @param autoPlay Autoplay when media item prepared. Default is true.
 * @param usePlayerController Using player controller. Default is true.
 * @param controllerConfig Player controller config. You can customize the Video Player Controller UI.
 * @param seekBeforeMilliSeconds The seek back increment, in milliseconds. Default is 10sec (10000ms). Read-only props (Changes in values do not take effect.)
 * @param seekAfterMilliSeconds The seek forward increment, in milliseconds. Default is 10sec (10000ms). Read-only props (Changes in values do not take effect.)
 * @param repeatMode Sets the content repeat mode.
 * @param volume Sets thie player volume. It's possible from 0.0 to 1.0.
 * @param onCurrentTimeChanged A callback that returned once every second for player current time when the player is playing.
 * @param fullScreenSecurePolicy Windows security settings to apply when full screen. Default is off. (For example, avoid screenshots that are not DRM-applied.)
 * @param onFullScreenEnter A callback that occurs when the player is full screen. (The [VideoPlayerControllerConfig.showFullScreenButton] must be true to trigger a callback.)
 * @param onFullScreenExit A callback that occurs when the full screen is turned off. (The [VideoPlayerControllerConfig.showFullScreenButton] must be true to trigger a callback.)
 * @param handleAudioFocus Set whether to handle the video playback control automatically when it is playing in PIP mode and media is played in another app. Default is true.
 * @param playerBuilder Return exoplayer builder. This instance allows you to customise the ExoPlayer while in the Building process. Used to add various components like other RenderFactories.
 * @param playerInstance Return exoplayer instance. This instance allows you to add [androidx.media3.exoplayer.analytics.AnalyticsListener] to receive various events from the player.
 */
@SuppressLint("SourceLockedOrientationActivity", "UnsafeOptInUsageError")
@Composable
fun VideoPlayer(
    modifier: Modifier = Modifier,
    mediaItems: List<VideoPlayerMediaItem>,
    autoPlay: Boolean = true,
    usePlayerController: Boolean = true,
    controllerConfig: VideoPlayerControllerConfig = VideoPlayerControllerConfig.Default,
    seekBeforeMilliSeconds: Long = 5000L,
    seekAfterMilliSeconds: Long = 5000L,
    repeatMode: RepeatMode = RepeatMode.NONE,
    resizeMode: ResizeMode = ResizeMode.FIT,
    @FloatRange(from = 0.0, to = 1.0) volume: Float = 1f,
    onCurrentTimeChanged: (Long) -> Unit = {},
    fullScreenSecurePolicy: SecureFlagPolicy = SecureFlagPolicy.Inherit,
    onFullScreenEnter: () -> Unit = {},
    onFullScreenExit: () -> Unit = {},
    defaultFullScreen: Boolean = false,
    handleAudioFocus: Boolean = true,
    playerBuilder: ExoPlayer.Builder.() -> ExoPlayer.Builder = { this },
    playerInstance: ExoPlayer.() -> Unit = {},
) {
    val context = LocalContext.current
    var currentTime by remember { mutableLongStateOf(0L) }

    var mediaSession = remember<MediaSession?> { null }

    val player = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()

        ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(seekBeforeMilliSeconds)
            .setSeekForwardIncrementMs(seekAfterMilliSeconds)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                handleAudioFocus,
            )
            .apply {
                val cache = PlayerCacheManager.getCache()
                if (cache != null) {
                    val cacheDataSourceFactory = CacheDataSource.Factory()
                        .setCache(cache)
                        .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context, httpDataSourceFactory))
                    setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
                }
            }
            .playerBuilder()
            .build()
            .also(playerInstance)
    }

    val defaultPlayerView = remember {
        PlayerView(context)
    }




    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)

            if (currentTime != player.currentPosition) {
                onCurrentTimeChanged(currentTime)
            }

            currentTime = player.currentPosition
        }
    }

    LaunchedEffect(usePlayerController) {
        defaultPlayerView.useController = usePlayerController
    }

    LaunchedEffect(player) {
        defaultPlayerView.player = player
    }

    LaunchedEffect(mediaItems, player) {
        mediaSession?.release()
        mediaSession = MediaSession.Builder(context, ForwardingPlayer(player))
            .setId("VideoPlayerMediaSession_${UUID.randomUUID().toString().lowercase().split("-").first()}")
            .build()
        val exoPlayerMediaItems = withContext(Dispatchers.IO) {
            mediaItems.map {
                val uri = it.toUri(context)

                MediaItem.Builder().apply {
                    setUri(uri)
                    setMediaMetadata(it.mediaMetadata)
                    setMimeType(it.mimeType)
                    setDrmConfiguration(
                        if (it is VideoPlayerMediaItem.NetworkMediaItem) {
                            it.drmConfiguration
                        } else {
                            null
                        },
                    )
                }.build()
            }
        }

        player.setMediaItems(exoPlayerMediaItems)
        player.prepare()

        if (autoPlay) {
            player.play()
        }
    }

    var isFullScreenModeEntered by remember { mutableStateOf(defaultFullScreen) }

    LaunchedEffect(controllerConfig) {
        controllerConfig.applyToExoPlayerView(defaultPlayerView) {
            isFullScreenModeEntered = it

            if (it) {
                onFullScreenEnter()
            }
        }
    }

    LaunchedEffect(controllerConfig, repeatMode) {
        defaultPlayerView.setRepeatToggleModes(
            if (controllerConfig.showRepeatModeButton) {
                REPEAT_TOGGLE_MODE_ALL or REPEAT_TOGGLE_MODE_ONE
            } else {
                REPEAT_TOGGLE_MODE_NONE
            },
        )
        player.repeatMode = repeatMode.toExoPlayerRepeatMode()
    }

    LaunchedEffect(volume) {
        player.volume = volume
    }

    VideoPlayerSurface(
        modifier = modifier,
        defaultPlayerView = defaultPlayerView,
        player = player,
        usePlayerController = usePlayerController,
        surfaceResizeMode = resizeMode
    )

    if (isFullScreenModeEntered) {
        var fullScreenPlayerView by remember { mutableStateOf<PlayerView?>(null) }

        VideoPlayerFullScreenDialog(
            player = player,
            currentPlayerView = defaultPlayerView,
            controllerConfig = controllerConfig,
            repeatMode = repeatMode,
            resizeMode = resizeMode,
            onDismissRequest = {
                fullScreenPlayerView?.let {
                    PlayerView.switchTargetView(player, it, defaultPlayerView)
                    defaultPlayerView.findViewById<ImageButton>(androidx.media3.ui.R.id.exo_fullscreen)
                        .performClick()
                    val currentActivity = context.findActivity()
                    currentActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    currentActivity.setFullScreen(false)
                    onFullScreenExit()
                }
                isFullScreenModeEntered = false
            },
            securePolicy = fullScreenSecurePolicy,
            fullScreenPlayerView = {
                fullScreenPlayerView = this
            },
        )
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
internal fun VideoPlayerSurface(
    modifier: Modifier = Modifier,
    defaultPlayerView: PlayerView,
    player: ExoPlayer,
    usePlayerController: Boolean,
    surfaceResizeMode: ResizeMode,
    autoDispose: Boolean = true,
) {
    val lifecycleOwner = rememberUpdatedState(androidx.lifecycle.compose.LocalLifecycleOwner.current)

    DisposableEffect(
        AndroidView(
            modifier = modifier,
            factory = {
                defaultPlayerView.apply {
                    useController = usePlayerController
                    resizeMode = surfaceResizeMode.toPlayerViewResizeMode()
                    keepScreenOn = false
                    setBackgroundColor(Color.BLACK)
                }
            },
            update = {
                it.apply {
                    resizeMode = surfaceResizeMode.toPlayerViewResizeMode()
                    keepScreenOn = false
                }
            }
        ),
    ) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    player.pause()
                }

                Lifecycle.Event.ON_RESUME -> {
                    player.play()
                }

                Lifecycle.Event.ON_STOP -> {
                    player.stop()
                }

                else -> {}
            }
        }
        val lifecycle = lifecycleOwner.value.lifecycle
        lifecycle.addObserver(observer)

        onDispose {
            if (autoDispose) {
                player.release()
                lifecycle.removeObserver(observer)
            }
        }
    }
}
