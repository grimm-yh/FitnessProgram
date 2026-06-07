package com.dhera.fitnessprogram

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dhera.fitnessprogram.data.AppDatabase
import com.dhera.fitnessprogram.data.TrainingRepository
import com.dhera.fitnessprogram.ui.MainViewModel
import com.dhera.fitnessprogram.ui.MainViewModelFactory
import com.dhera.fitnessprogram.ui.screen.AboutScreen
import com.dhera.fitnessprogram.ui.screen.HomeScreen
import com.dhera.fitnessprogram.ui.screen.PlansScreen
import com.dhera.fitnessprogram.ui.screen.SettingsScreen
import com.dhera.fitnessprogram.ui.screen.TimerWindow
import com.dhera.fitnessprogram.ui.theme.FitnessProgramTheme

class MainActivity : ComponentActivity() {
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val repository by lazy { TrainingRepository(database.trainingDao()) }
    private val viewModel: MainViewModel by viewModels { MainViewModelFactory(application, repository) }
    private lateinit var musicManager: MusicManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        musicManager = MusicManager(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Startup music logic
        if (viewModel.playMusicOnStartup.value) {
            musicManager.startBackgroundMusic()
            viewModel.setMusicPlaying(true)
        }

        enableEdgeToEdge()
        setContent {
            val appTheme by viewModel.appTheme.collectAsState()
            FitnessProgramTheme(appTheme = appTheme) {
                FitnessProgramApp(viewModel, musicManager)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        musicManager.release()
    }
}

@Composable
fun FitnessProgramApp(viewModel: MainViewModel, musicManager: MusicManager) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val isMusicPlaying by viewModel.isMusicPlaying.collectAsState()
    
    val activeTimerType by viewModel.activeTimerType.collectAsState()
    val activeTimerTarget by viewModel.activeTimerTarget.collectAsState()
    val timerMinimized by viewModel.timerMinimized.collectAsState()

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label,
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { 
                        currentDestination = it 
                        musicManager.stopAllNotifications()
                    }
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                floatingActionButton = {
                    // Global Music Control Button
                    FloatingActionButton(
                        onClick = {
                            musicManager.stopAllNotifications()
                            if (isMusicPlaying) {
                                musicManager.pauseBackgroundMusic()
                                viewModel.setMusicPlaying(false)
                            } else {
                                musicManager.startBackgroundMusic()
                                viewModel.setMusicPlaying(true)
                            }
                        },
                        modifier = Modifier.padding(bottom = 80.dp),
                        containerColor = if (isMusicPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RectangleShape
                    ) {
                        Icon(
                            imageVector = if (isMusicPlaying) Icons.Default.MusicNote else Icons.Default.MusicOff,
                            contentDescription = "音乐控制"
                        )
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                    when (currentDestination) {
                        AppDestinations.HOME -> HomeScreen(viewModel, musicManager, Modifier.fillMaxSize())
                        AppDestinations.PLANS -> PlansScreen(viewModel, musicManager, Modifier.fillMaxSize())
                        AppDestinations.SETTINGS -> {
                            var showAbout by remember { mutableStateOf(false) }
                            if (showAbout) {
                                AboutScreen(onBack = { showAbout = false }, modifier = Modifier.fillMaxSize())
                            } else {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    musicManager = musicManager,
                                    onAboutClick = { showAbout = true },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    // Global Floating Timer Window
                    if (activeTimerType != null) {
                        TimerWindow(
                            type = activeTimerType!!,
                            targetSeconds = activeTimerTarget,
                            minimized = timerMinimized,
                            onToggleMinimize = {
                                musicManager.stopAllNotifications()
                                viewModel.toggleTimerMinimize()
                            },
                            onClose = {
                                musicManager.stopAllNotifications()
                                viewModel.closeTimer()
                            },
                            musicManager = musicManager,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("今日训练", Icons.Default.Home),
    PLANS("训练计划", Icons.AutoMirrored.Filled.List),
    SETTINGS("设置", Icons.Default.Settings),
}
