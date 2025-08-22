@file:OptIn(ExperimentalPermissionsApi::class)

package org.vlog.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.vlog.app.data.model.VideoItem
import org.vlog.app.ui.theme.PlayerApplicationTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import org.vlog.app.player.PlayerActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlayerApplicationTheme {
                val playerActivityLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult(),
                    onResult = {}
                )

                RequestPermissionAndDisplayContent {
                    MainScreenWithBottomNavigation(
                        onVideoItemClick = { videoItem ->
                            val playerIntent = Intent(this@MainActivity, PlayerActivity::class.java).apply{
                                data = videoItem.uri
                            }
                            playerActivityLauncher.launch(playerIntent)

                        }
                    )
                }
            }
        }
    }
}



@Composable
private fun RequestPermissionAndDisplayContent(
    appContent: @Composable () -> Unit,
) {

    val readVideoPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(
            android.Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        rememberPermissionState(
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    fun requestPermissions(){
        readVideoPermissionState.launchPermissionRequest()
    }

    LaunchedEffect(key1 = Unit){
        if(!readVideoPermissionState.status.isGranted){
            requestPermissions()
        }
    }

    if (readVideoPermissionState.status.isGranted) {

        appContent()

    } else {

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ){

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "warning ambe",
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                "no permission",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            if(readVideoPermissionState.status.shouldShowRationale){
                Spacer(modifier = Modifier.size(8.dp))
                OutlinedButton(
                    onClick = { requestPermissions() },
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        "request_again",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenWithBottomNavigation(
    onVideoItemClick: (VideoItem) -> Unit
){

    var bottomNavigationScreen by rememberSaveable {
        mutableStateOf( BottomNavigationScreens.VideosView )
    }

    val mainViewModel: MainViewModel = viewModel(factory = MainViewModel.factory)

    val videosViewStateFlow by mainViewModel.videoItemsStateFlow.collectAsState()
    val foldersViewStateFlow by mainViewModel.folderItemStateFlow.collectAsState()

    Scaffold(
        topBar = {

            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                //Todo Add Nested Scroll To Animate TopBar And Background
            )


        },
        bottomBar = {

            NavigationBar(
                tonalElevation = 12.dp
            ){

                NavigationBarItem(
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    selected = bottomNavigationScreen == BottomNavigationScreens.VideosView,
                    label = { Text(text = "videos") },
                    onClick = {
                        bottomNavigationScreen = BottomNavigationScreens.VideosView
                    },
                    icon = {

                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "videos"
                        )
                    }
                )

                NavigationBarItem(
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    selected = bottomNavigationScreen == BottomNavigationScreens.FoldersView,
                    label = { Text(text = "folders") },
                    onClick = {
                        bottomNavigationScreen = BottomNavigationScreens.FoldersView
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "folders"
                        )
                    }
                )
            }
        }
    ) { paddingValues ->

        AnimatedContent(
            targetState = bottomNavigationScreen ,
            label = "",
            transitionSpec = {
                when(this.targetState){
                    BottomNavigationScreens.VideosView -> slideInHorizontally(){-it}.togetherWith(slideOutHorizontally(){it})

                    BottomNavigationScreens.FoldersView -> slideInHorizontally(){it}.togetherWith(slideOutHorizontally(){-it})
                }
            }
        ) { navScreen ->

            when(navScreen){

                BottomNavigationScreens.VideosView -> {

                    VideoItemGridLayout(
                        contentPadding = paddingValues,
                        videoList = videosViewStateFlow,
                        onVideoItemClick = onVideoItemClick,
                    )

                }

                BottomNavigationScreens.FoldersView -> {

                    var foldersVideosNavigation by rememberSaveable{
                        mutableStateOf(FoldersVideosNavigation.FoldersScreen)
                    }

                    Crossfade(
                        targetState = foldersVideosNavigation, label = "",
                        animationSpec = tween(300, easing = LinearEasing)
                    ) { foldersAndVideosNav ->

                        when(foldersAndVideosNav){

                            FoldersVideosNavigation.FoldersScreen -> {

                                FolderItemGridLayout(
                                    foldersList = foldersViewStateFlow,
                                    onFolderItemClick = {
                                        mainViewModel.updateCurrentSelectedFolderItem(it)
                                        foldersVideosNavigation = FoldersVideosNavigation.VideosScreen
                                    },
                                    contentPadding = paddingValues
                                )

                            }

                            FoldersVideosNavigation.VideosScreen -> {

                                BackHandler(true) {
                                    foldersVideosNavigation = FoldersVideosNavigation.FoldersScreen
                                }

                                VideoItemGridLayout(
                                    contentPadding = paddingValues,
                                    videoList = mainViewModel.currentSelectedFolder.videoItems,
                                    onVideoItemClick = onVideoItemClick,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class BottomNavigationScreens{
    VideosView,
    FoldersView
}

private enum class FoldersVideosNavigation{
    FoldersScreen,
    VideosScreen
}