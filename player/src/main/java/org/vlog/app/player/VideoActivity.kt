package org.vlog.app.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import dagger.hilt.android.AndroidEntryPoint
import org.vlog.app.player.VideoViewModel.Companion.LAYOUT_EXTRA
import kotlin.getValue

@AndroidEntryPoint
class VideoActivity : AppCompatActivity(){


    private val viewModel: VideoViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()
    private var dataUri: Uri? = null
    private var extras: Bundle? = null

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

        setContent {
            val playerUiState by playerViewModel.uiState.collectAsState()
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (playerUiState.isFullscreen) {
                    PlayerView(
                        isFullscreen = true,
                        isOrientationFullscreen = playerUiState.isOrientationFullscreen,
                        onFullscreenToggle = {
                            playerViewModel.toggleFullscreen()
                        },
                        onPrevious = {  },
                        onNext = {  },
                        hasPrevious = playerViewModel.hasPrevious(),
                        hasNext = playerViewModel.hasNext(),
                        currentTitle = "title",
                        currentGatherTitle = "Gather",
                        currentPlayTitle = "Play",
                        modifier = Modifier.fillMaxSize(),
                        onOrientationToggle = {
                            playerViewModel.toggleOrientationFullscreen()
                            val activity = applicationContext as? Activity
                            activity?.let {
                                it.requestedOrientation = if (it.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                }
                            }
                        }
                    )
                } else {
                    Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .verticalScroll(rememberScrollState())
                        ) {
                            PlayerView(
                                isFullscreen = false,
                                isOrientationFullscreen = playerUiState.isOrientationFullscreen,
                                onFullscreenToggle = {
                                    playerViewModel.toggleFullscreen()
                                },
                                onPrevious = {  },
                                onNext = {  },
                                hasPrevious = playerViewModel.hasPrevious(),
                                hasNext = playerViewModel.hasNext(),
                                currentTitle = "title",
                                currentGatherTitle = "Gather",
                                currentPlayTitle = "Play",
                                modifier = Modifier.fillMaxSize(),
                                onOrientationToggle = {
                                    playerViewModel.toggleOrientationFullscreen()
                                    val activity = applicationContext as? Activity
                                    activity?.let {
                                        it.requestedOrientation = if (it.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                        } else {
                                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                        }
                                    }
                                }
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {

                            // 播放控制区域
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TabRow(
                                    selectedTabIndex = 0,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Tab(
                                        selected = true,
                                        onClick = {  },
                                        text = { Text("详情") }
                                    )
                                    Tab(
                                        selected = false,
                                        onClick = {  },
                                        text = { Text("评论") }
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // 线路和选集整合按钮
                                Button(
                                    onClick = {  },
                                    modifier = Modifier.wrapContentWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.List,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("选集/线路")
                                }
                            }



                            Text(text = "dataUri: ${dataUri}", fontWeight = FontWeight.Bold)
                            Text(text = "extras: ${extras}", fontWeight = FontWeight.Bold)
                        }
                    }


                }
            }
        }

    }


    @Composable
    fun CompositionPreviewPane(
        shouldShowSupportingPaneButton: Boolean,
        onNavigateToSupportingPane: () -> Unit,
        viewModel: VideoViewModel,
        modifier: Modifier = Modifier,
    ) {
        var isFullScreen by remember { mutableStateOf(false) }
        Column(
            modifier = modifier,
        ) {
            Text(text = "Composition Preview Pane", fontWeight = FontWeight.Bold)

        }

    }

    @Composable
    fun ExportOptionsPane(
        viewModel: VideoViewModel,
        shouldShowBackButton: Boolean,
        onBack: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        var isAudioTypeExpanded by remember { mutableStateOf(false) }
        var isVideoTypeExpanded by remember { mutableStateOf(false) }
        Column(
            modifier = modifier,
        ) {
            Text(text = "Export Options Pane", fontWeight = FontWeight.Bold)

        }
    }

}



