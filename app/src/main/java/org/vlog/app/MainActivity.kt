package org.vlog.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import org.vlog.app.player.PlayerActivity
import org.vlog.app.ui.theme.PlayerApplicationTheme

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
                            val playerIntent = Intent(
                                this@MainActivity,
                                PlayerActivity::class.java
                            ).apply{
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



@OptIn(ExperimentalPermissionsApi::class)
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
