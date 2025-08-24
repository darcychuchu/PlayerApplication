package org.vlog.app.player

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.vlog.app.ui.theme.PlayerApplicationTheme
import org.vlog.app.ui.theme.spacing
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import org.vlog.app.player.VideoViewModel.Companion.LAYOUT_EXTRA
import kotlin.getValue

class VideoActivity : AppCompatActivity(){
    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.colorMode = ActivityInfo.COLOR_MODE_HDR
        val compositionLayout = intent.getStringExtra(LAYOUT_EXTRA) ?: "layout"
        val viewModel: VideoViewModel by viewModels {
            VideoViewModelFactory(application, compositionLayout)
        }

        viewModel.toastMessage.observe(this) { newMessage ->
            newMessage?.let {
                viewModel.toastMessage.value = null
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        setContent {
            PlayerApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val scope = rememberCoroutineScope()
                    val navigator = rememberSupportingPaneScaffoldNavigator()

                    BackHandler(navigator.canNavigateBack()) { scope.launch { navigator.navigateBack() } }

                    SupportingPaneScaffold(
                        directive = navigator.scaffoldDirective,
                        value = navigator.scaffoldValue,
                        mainPane = {
                        },
                        supportingPane = {
                        },
                        modifier =Modifier.padding(innerPadding).padding(MaterialTheme.spacing.standard, MaterialTheme.spacing.small),
                    )
                }

            }
        }

    }


    @Composable
    fun CompositionPreviewPane(
        shouldShowSupportingPaneButton: Boolean,
        onNavigateToSupportingPane: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        val scrollState = rememberScrollState()
        Column {
            Text(
                text = "layout",
                fontWeight = FontWeight.Bold,
            )

        }
    }

    @Composable
    fun ExportOptionsPane(
        shouldShowBackButton: Boolean,
        onBack: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        var isAudioTypeExpanded by remember { mutableStateOf(false) }
        var isVideoTypeExpanded by remember { mutableStateOf(false) }
        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.mini),
            modifier = modifier,
        ) {
            Text(text = "export_settings", fontWeight = FontWeight.Bold)

        }
    }
}
