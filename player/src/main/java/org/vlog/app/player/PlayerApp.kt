package org.vlog.app.player

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.media3.common.MediaMetadata
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.analytics.AnalyticsListener
import org.vlog.app.player.compose.ResizeMode
import org.vlog.app.player.compose.RepeatMode
import org.vlog.app.player.compose.VideoPlayer
import org.vlog.app.player.compose.controller.VideoPlayerControllerConfig
import org.vlog.app.player.compose.toRepeatMode
import org.vlog.app.player.compose.uri.VideoPlayerMediaItem


const val MIME_TYPE_DASH = MimeTypes.APPLICATION_MPD
const val MIME_TYPE_HLS = MimeTypes.APPLICATION_M3U8
const val MIME_TYPE_VIDEO_MP4 = MimeTypes.VIDEO_MP4

class PlayerActivity : ComponentActivity() {

    val samplePlayList: List<VideoPlayerMediaItem> = listOf(

        VideoPlayerMediaItem.NetworkMediaItem(
            id = "abcde1234",
            url = "https://krevonix.com/20250321/V4JwLqbI/index.m3u8",
            mediaMetadata = MediaMetadata.Builder().setTitle("Clear HLS: Angel one").build(),
            mimeType = MIME_TYPE_HLS,
        ),
        VideoPlayerMediaItem.NetworkMediaItem(
            id = "abcde9876",
            url = "https://v6.longshengtea.com/yyv6/202502/04/BXMskxxhvy19/video/index.m3u8",
            mediaMetadata = MediaMetadata.Builder().setTitle("Clear HLS: Angel one").build(),
            mimeType = MIME_TYPE_HLS,
        ),
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = LocalContext.current
        var repeatMode by remember { mutableStateOf(RepeatMode.NONE) }
        VideoPlayer(
            mediaItems = samplePlayList,
            autoPlay = false,
            usePlayerController = true,
            controllerConfig = VideoPlayerControllerConfig.Default.copy(
                showSpeedAndPitchOverlay = true,
                showCurrentTimeAndTotalTime = true,
                showBufferingProgress = true,
                controllerShowTimeMilliSeconds = 5_000,
                showSubtitleButton = true,
                showNextTrackButton = true,
                showBackTrackButton = true,
                showBackwardIncrementButton = true,
                showForwardIncrementButton = true,
                showRepeatModeButton = true,
                showFullScreenButton = true,
                controllerAutoShow = true,
            ),
            repeatMode = repeatMode,
            resizeMode = ResizeMode.FIT,
            onCurrentTimeChanged = {
                Log.e("CurrentTime", it.toString())
            },
            playerInstance = {
                Log.e("VOLUME", volume.toString())
                addAnalyticsListener(object : AnalyticsListener {
                    @SuppressLint("UnsafeOptInUsageError")
                    override fun onRepeatModeChanged(
                        eventTime: AnalyticsListener.EventTime,
                        rMode: Int,
                    ) {
                        repeatMode = rMode.toRepeatMode()
                        Toast.makeText(
                            context,
                            "RepeatMode changed = ${rMode.toRepeatMode()}",
                            Toast.LENGTH_LONG,
                        ).show()
                    }

                    @SuppressLint("UnsafeOptInUsageError")
                    override fun onPlayWhenReadyChanged(
                        eventTime: AnalyticsListener.EventTime,
                        playWhenReady: Boolean,
                        reason: Int,
                    ) {
                        Toast.makeText(
                            context,
                            "isPlaying = $playWhenReady",
                            Toast.LENGTH_LONG,
                        ).show()
                    }

                    @SuppressLint("UnsafeOptInUsageError")
                    override fun onVolumeChanged(
                        eventTime: AnalyticsListener.EventTime,
                        volume: Float,
                    ) {
                        Toast.makeText(
                            context,
                            "Player volume changed = $volume",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                })
            },
            modifier = Modifier
                .fillMaxSize(),
        )

    }


}