package org.vlog.app.player

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.vlog.app.player.databinding.ActivityPlayerBinding
import java.io.File

@SuppressLint("UnsafeOptInUsageError")
class PlayerActivity : AppCompatActivity() {

    lateinit var binding: ActivityPlayerBinding
    private var player: Player? = null
    private var dataUri: Uri? = null
    private var extras: Bundle? = null
    private var isPlaybackFinished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        // The window is always allowed to extend into the DisplayCutout areas on the short edges of the screen
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataUri = intent.data
        extras = intent.extras

        val backButton =
            binding.playerView.findViewById<ImageButton>(R.id.back_button)
        val videoTitleTextView =
            binding.playerView.findViewById<TextView>(R.id.video_name)
        val audioTrackButton =
            binding.playerView.findViewById<ImageButton>(R.id.btn_audio_track)
        val subtitleTrackButton =
            binding.playerView.findViewById<ImageButton>(R.id.btn_subtitle_track)
        val videoZoomButton =
            binding.playerView.findViewById<ImageButton>(R.id.btn_video_zoom)
        val nextButton =
            binding.playerView.findViewById<ImageButton>(androidx.media3.ui.R.id.exo_next)
        val prevButton =
            binding.playerView.findViewById<ImageButton>(androidx.media3.ui.R.id.exo_prev)
        val lockControlsButton =
            binding.playerView.findViewById<ImageButton>(R.id.btn_lock_controls)
        val unlockControlsButton =
            binding.playerView.findViewById<ImageButton>(R.id.btn_unlock_controls)
        val playerControls =
            binding.playerView.findViewById<FrameLayout>(R.id.player_controls)

        if (extras?.containsKey(API_TITLE) == true) {
            videoTitleTextView.text = extras?.getString(API_TITLE)
        } else {
            videoTitleTextView.text = dataUri?.let { File(dataUri.toString()).name }
        }

        nextButton.setOnClickListener {
            player?.currentPosition?.let {  }
            player?.seekToNext()
        }
        prevButton.setOnClickListener {
            player?.currentPosition?.let {  }
            player?.seekToPrevious()
        }
        videoZoomButton.setOnClickListener {
            binding.playerView.resizeMode =
                if (binding.playerView.resizeMode != AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
        }
        lockControlsButton.setOnClickListener {
            playerControls.visibility = View.INVISIBLE
            unlockControlsButton.visibility = View.VISIBLE
        }
        unlockControlsButton.setOnClickListener {
            unlockControlsButton.visibility = View.INVISIBLE
            playerControls.visibility = View.VISIBLE
        }
        backButton.setOnClickListener { finish() }
    }

    override fun onStart() {
        initializePlayer()
        super.onStart()
    }

    override fun onStop() {
        releasePlayer()
        super.onStop()
    }

    private fun initializePlayer() {

        player = ExoPlayer.Builder(applicationContext)
            //.setRenderersFactory(renderersFactory)
            //.setTrackSelector(trackSelector)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true
            ).build()
            .also { player ->
                binding.playerView.player = player
                binding.playerView.setControllerVisibilityListener(
                    PlayerView.ControllerVisibilityListener { visibility ->

                    }
                )


                val exoPlayerMediaItems: MutableList<MediaItem> = mutableListOf()
                exoPlayerMediaItems.add(
                    MediaItem.Builder().apply {
                        setUri(dataUri)
                        setMediaMetadata(MediaMetadata.Builder().setTitle("StorageMedia").build())
                        setMimeType(MimeTypes.APPLICATION_SUBRIP)
                    }.build()
                )
                player.setMediaItems(exoPlayerMediaItems)

                player.setHandleAudioBecomingNoisy(true)
                player.prepare()
            }
    }

    private fun releasePlayer() {
        player = null
    }

    private fun playbackStateListener() = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            binding.playerView.keepScreenOn = isPlaying

            super.onIsPlayingChanged(isPlaying)
        }

        @SuppressLint("SourceLockedOrientationActivity")
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            super.onVideoSizeChanged(videoSize)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
        }

        @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                oldPosition.mediaItem?.let {  }
            }
            super.onPositionDiscontinuity(oldPosition, newPosition, reason)
        }

        override fun onPlayerError(error: PlaybackException) {
            val alertDialog = MaterialAlertDialogBuilder(this@PlayerActivity)
                .setTitle("error_playing_video")
                .setMessage(error.message ?: "unknown_error")
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                .setOnDismissListener {
                    if (player?.hasNextMediaItem() == true) player?.seekToNext() else finish()
                }
                .create()

            alertDialog.show()
            super.onPlayerError(error)
        }

        override fun onTracksChanged(tracks: Tracks) {
            super.onTracksChanged(tracks)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> {
                    isPlaybackFinished = true
                    if (player?.hasNextMediaItem() == true) {
                        player?.seekToNext()
                    } else {
                        finish()
                    }
                }

                Player.STATE_READY -> {
                }

                Player.STATE_BUFFERING -> {
                }

                Player.STATE_IDLE -> {
                }
            }
            super.onPlaybackStateChanged(playbackState)
        }
    }

    override fun finish() {
        if (extras != null && extras!!.containsKey(API_RETURN_RESULT)) {
            val result = Intent("com.mxtech.intent.result.VIEW")
            result.putExtra(API_END_BY, if (isPlaybackFinished) "playback_completion" else "user")
            if (!isPlaybackFinished) {
                player?.also {
                    if (it.duration != C.TIME_UNSET) {
                        result.putExtra(API_DURATION, it.duration.toInt())
                    }
                    result.putExtra(API_POSITION, it.currentPosition.toInt())
                }
            }
            setResult(Activity.RESULT_OK, result)
        }

        super.finish()
    }

    companion object {
        const val API_TITLE = "title"
        const val API_POSITION = "position"
        const val API_DURATION = "duration"
        const val API_RETURN_RESULT = "return_result"
        const val API_END_BY = "end_by"
        const val API_SUBS = "subs"
        const val API_SUBS_ENABLE = "subs.enable"
        const val API_SUBS_NAME = "subs.name"
    }
}
