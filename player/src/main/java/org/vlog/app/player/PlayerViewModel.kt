package org.vlog.app.player

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.apply
import kotlin.ranges.coerceAtLeast

enum class DesiredScreenOrientation {
    PORTRAIT,
    LANDSCAPE,
    SYSTEM_DEFAULT
}

data class PlayerUiState(
    val isLoading: Boolean = false,
    val isPlaying: Boolean = false,
    val isFullscreen: Boolean = false,
    val desiredOrientation: DesiredScreenOrientation = DesiredScreenOrientation.SYSTEM_DEFAULT, // Target orientation for the activity
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val bufferedPosition: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val error: String? = null,
    val videoId: String? = null,
    val title: String? = null,
    var remarks: String? = null,
    var coverUrl: String? = null,
    val isOrientationFullscreen: Boolean = false
)

data class PlaylistState(
    val currentGatherIndex: Int = 0,
    val currentPlayIndex: Int = 0,
    val gatherId: String? = null,
    val gatherName: String? = null,
    val playerUrl: String? = null,
    val playerTitle: String? = null
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private var _exoPlayer: ExoPlayer? = null
    val exoPlayer: ExoPlayer?
        get() = _exoPlayer

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _playlistState = MutableStateFlow(PlaylistState())
    val playlistState: StateFlow<PlaylistState> = _playlistState.asStateFlow()

    private var playbackStateJob: Job? = null
    private var positionToRestore: Long = -1L
    private var playWhenReadyToRestore: Boolean = false
    private var handleAudioFocus: Boolean = true

    init {
        initializePlayerInternal()
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayerInternal() {
        if (_exoPlayer == null) {
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            _exoPlayer = ExoPlayer.Builder(context)
                .setSeekBackIncrementMs(10000L)
                .setSeekForwardIncrementMs(10000L)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AUDIO_CONTENT_TYPE_MOVIE)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    handleAudioFocus,
                )
                .build()
                .apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _uiState.update { it.copy(isPlaying = isPlaying) }
                        if (isPlaying) {
                            startPlaybackStateUpdate()
                        } else {
                            playbackStateJob?.cancel()
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        _uiState.update {
                            it.copy(
                                isLoading = playbackState == Player.STATE_BUFFERING,
                                duration = _exoPlayer?.duration?.coerceAtLeast(0L) ?: it.duration // Ensure duration is not negative
                            )
                        }
                        when (playbackState) {
                            Player.STATE_READY -> {
                                _uiState.update { it.copy(duration = _exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L) }
                                if (positionToRestore >= 0) {
                                    _exoPlayer?.seekTo(positionToRestore)
                                    positionToRestore = -1L // Reset after restoring position
                                }

                                // ADDED LOGIC FOR PLAYWHENREADY RESTORATION
                                if (playWhenReadyToRestore) {
                                    _exoPlayer?.play()
                                }
                                playWhenReadyToRestore = false // Reset flag
                            }
                            Player.STATE_ENDED -> {
                                _uiState.update { it.copy(isPlaying = false /*, currentPosition = 0L // or keep at end */) }
                                playbackStateJob?.cancel()
                                // Potentially play next item here if auto-play is enabled
                                // playNext()
                            }
                            Player.STATE_IDLE -> {
                                // Player is idle, possibly stopped or failed.
                                playbackStateJob?.cancel()
                            }
                            else -> {}
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        _uiState.update { it.copy(error = error.message ?: "Unknown player error", isLoading = false) }
                        playbackStateJob?.cancel()
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        // This is called when the player transitions to a new media item.
                        // You might want to update UI based on the new mediaItem's metadata.
                        // For example, if your MediaItem has tag with title:
                        // val newTitle = mediaItem?.mediaMetadata?.title?.toString()
                        // _uiState.update { it.copy(title = newTitle ?: "Loading...") }
                        // Ensure playback state job is running if player is playing
                        if (_exoPlayer?.isPlaying == true) {
                            startPlaybackStateUpdate()
                        }
                    }

                    override fun onPositionDiscontinuity(
                        oldPosition: Player.PositionInfo,
                        newPosition: Player.PositionInfo,
                        reason: Int
                    ) {
                        _uiState.update { it.copy(currentPosition = newPosition.positionMs) }
                    }
                })
                playWhenReady = true
            }
            _uiState.update { it.copy(isLoading = false) } // Initial load done
        }
    }

    private fun setMediaItem(url: String, title: String? = null) {
        if (_exoPlayer == null) initializePlayerInternal()
        val mediaItem = MediaItem.fromUri(url)
        _exoPlayer?.setMediaItem(mediaItem)
        _exoPlayer?.prepare()
        // Update UI with the title of the current media item being prepared
        _uiState.update { it.copy(title = title ?: it.title) } // Use new title or keep existing video title
    }

    fun play() {
        _exoPlayer?.play()
    }

    fun pause() {
        _exoPlayer?.pause()
    }

    fun seekTo(positionMs: Long) {
        _exoPlayer?.seekTo(positionMs)
    }

    private fun releasePlayer() {
        playbackStateJob?.cancel()
        if (_exoPlayer != null) {
            try {
                positionToRestore = _exoPlayer!!.currentPosition // Save position before release
                playWhenReadyToRestore = _exoPlayer!!.playWhenReady // CAPTURE playWhenReady STATE
            } catch (e: Exception) {
                Log.w("VideoPlayerViewModel", "Error getting state on release: ${e.message}")
                // positionToRestore remains as is, or _uiState.value.currentPosition could be a fallback
            }
            _exoPlayer!!.release()
            _exoPlayer = null
        }
        _uiState.update {
            it.copy(
                isPlaying = false,
                duration = 0L,
                bufferedPosition = 0L,
                isLoading = true,
                error = null
                // currentPosition is NOT changed here
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }


    fun hasPrevious(): Boolean {
        val current = _playlistState.value
        return current.currentPlayIndex > 0 || current.currentGatherIndex > 0
    }



    fun hasNext(): Boolean {
        val current = _playlistState.value
        return false
    }

    fun toggleFullscreen() {
        _uiState.update { it.copy(isFullscreen = !it.isFullscreen) }
    }



    fun toggleOrientationFullscreen() {
        _uiState.update { it.copy(isOrientationFullscreen = !it.isOrientationFullscreen) }
    }

    fun setError(error: String?) { // Should ideally be handled by Player.Listener
        _uiState.update { it.copy(error = error) }
    }

    fun clearError() {
        setError(null)
    }

    private fun startPlaybackStateUpdate() {
        playbackStateJob?.cancel()
        playbackStateJob = viewModelScope.launch {
            while (true) {
                _uiState.update {
                    it.copy(
                        currentPosition = _exoPlayer?.currentPosition ?: 0L,
                        duration = _exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L,
                        bufferedPosition = _exoPlayer?.bufferedPosition ?: 0L
                    )
                }
                delay(1000) // Update every second
            }
        }
    }
}