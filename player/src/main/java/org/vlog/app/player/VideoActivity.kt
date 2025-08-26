package org.vlog.app.player

import android.Manifest
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.common.collect.ImmutableList
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VideoActivity : AppCompatActivity(){


    private val viewModel: VideoViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()
    private var dataUri: Uri? = null
    private var extras: Bundle? = null

    private val ACTION_VIEW: String = "org.vlog.app.player.action.VIEW"
    private val EXTENSION_EXTRA: String = "extension"
    private val DRM_SCHEME_EXTRA: String = "drm_scheme"
    private val DRM_LICENSE_URL_EXTRA: String = "drm_license_url"

    private var playerView: PlayerView? = null
    private var mediaItemDatabase = MediaItemDatabase()

    private var player: ExoPlayer? = null

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.colorMode = ActivityInfo.COLOR_MODE_HDR

        viewModel.toastMessage.observe(this) { newMessage ->
            newMessage?.let {
                viewModel.toastMessage.value = null
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        dataUri = intent.data
        extras = intent.extras

        val playlistHolderList = mutableStateOf<List<PlaylistHolder>>(emptyList())
        lifecycleScope.launch {
            playlistHolderList.value = mutableListOf(
                PlaylistHolder(title = "PlayTitle", mediaItems = listOf(
                    MediaItem.fromUri("https://v14.daayee.com/yyv14/202508/10/q1PhJADdat20/video/index.m3u8"),
                    MediaItem.fromUri("https://v13.daayee.com/yyv13/202508/10/uqpX2URe4D21/video/index.m3u8")
                ))
            )
        }


        setContent {

            val snackbarHostState = remember { SnackbarHostState() }
            val coroutineScope = rememberCoroutineScope()
            val context = LocalContext.current
            val exoPlayer by remember {
                mutableStateOf(ExoPlayer.Builder(context).build().apply { playWhenReady = true })
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            ) { paddingValues ->
                Column(
                    modifier = Modifier.fillMaxWidth().padding(paddingValues),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PlayerScreen(exoPlayer)


                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InputChooser(
                            playlistHolderList.value,
                            onException = { message ->
                                coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                            },
                        ) { mediaItems ->
                            exoPlayer.apply {
                                setMediaItems(mediaItems)
                                prepare()
                            }
                        }
                    }
                }
            }


        }

    }


    @Composable
    private fun PlayerScreen(exoPlayer: ExoPlayer) {
        val context = LocalContext.current
        AndroidView(
            factory = { PlayerView(context).apply { player = exoPlayer } },
            modifier =
                Modifier.height(dimensionResource(id = R.dimen.android_view_height))
                    .padding(all = dimensionResource(id = R.dimen.regular_padding)),
        )
    }


    @Composable
    private fun InputChooser(
        playlistHolderList: List<PlaylistHolder>,
        onException: (String) -> Unit,
        onNewMediaItems: (List<MediaItem>) -> Unit,
    ) {
        var showPresetInputChooser by remember { mutableStateOf(false) }
        var showLocalFileChooser by remember { mutableStateOf(false) }
        Row(
            Modifier.padding(vertical = dimensionResource(id = R.dimen.regular_padding)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.regular_padding)),
        ) {
            Button(onClick = { showPresetInputChooser = true }) {
                Text(text = stringResource(id = R.string.choose_preset_input))
            }
            Button(onClick = { showLocalFileChooser = true }) {
                Text(text = stringResource(id = R.string.choose_local_file))
            }
        }
        if (showPresetInputChooser) {
            if (playlistHolderList.isNotEmpty()) {
                PresetInputChooser(
                    playlistHolderList,
                    onDismissRequest = { showPresetInputChooser = false },
                ) { mediaItems ->
                    onNewMediaItems(mediaItems)
                    showPresetInputChooser = false
                }
            } else {
                onException(stringResource(id = R.string.no_loaded_playlists_error))
                showPresetInputChooser = false
            }
        }
        if (showLocalFileChooser) {
            LocalFileChooser(
                onException = { message ->
                    onException(message)
                    showLocalFileChooser = false
                }
            ) { mediaItems ->
                onNewMediaItems(mediaItems)
                showLocalFileChooser = false
            }
        }
    }

    @Composable
    private fun PresetInputChooser(
        playlistHolderList: List<PlaylistHolder>,
        onDismissRequest: () -> Unit,
        onInputSelected: (List<MediaItem>) -> Unit,
    ) {
        var selectedOption by remember { mutableStateOf(playlistHolderList.first()) }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(stringResource(id = R.string.choose_preset_input)) },
            confirmButton = {
                Button(onClick = { onInputSelected(selectedOption.mediaItems) }) {
                    Text(text = stringResource(id = R.string.ok))
                }
            },
            text = {
                Column {
                    playlistHolderList.forEach { playlistHolder ->
                        Row(
                            Modifier.fillMaxWidth()
                                .selectable(
                                    (playlistHolder == selectedOption),
                                    onClick = { selectedOption = playlistHolder },
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = (playlistHolder == selectedOption),
                                onClick = { selectedOption = playlistHolder },
                            )
                            Text(playlistHolder.title)
                        }
                    }
                }
            },
        )
    }

    @OptIn(UnstableApi::class)
    @Composable
    private fun LocalFileChooser(
        onException: (String) -> Unit,
        onFileSelected: (List<MediaItem>) -> Unit,
    ) {
        val context = LocalContext.current
        val localFileChooserLauncher =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
                onResult = { uri: Uri? ->
                    if (uri != null) {
                        onFileSelected(listOf(MediaItem.fromUri(uri)))
                    } else {
                        onException(getString(R.string.can_not_open_file_error))
                    }
                },
            )
        val permissionLauncher =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { isGranted: Boolean ->
                    if (isGranted) {
                        localFileChooserLauncher.launch(arrayOf("video/*"))
                    } else {
                        onException(getString(R.string.permission_not_granted_error))
                    }
                },
            )
        LaunchedEffect(Unit) {
            val permission =
                if (SDK_INT >= 33) Manifest.permission.READ_MEDIA_VIDEO
                else Manifest.permission.READ_EXTERNAL_STORAGE
            val permissionCheck = ContextCompat.checkSelfPermission(context, permission)
            if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                localFileChooserLauncher.launch(arrayOf("video/*"))
            } else {
                permissionLauncher.launch(permission)
            }
        }
    }

}



