package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.data.CustomLevelStorage
import com.example.data.GamePreferences
import com.example.data.models.CustomLevelData
import com.example.engine.RetroAudioEngine
import com.example.ui.screens.GameScreen
import com.example.ui.screens.LevelEditorScreen
import com.example.ui.screens.LevelSelectScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.screens.UpdateDialog
import com.example.ui.screens.UpgradeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.updater.AppUpdateManager
import com.example.updater.UpdateInfo

enum class ScreenState {
  MAIN_MENU,
  LEVEL_SELECT,
  UPGRADES,
  GAME,
  LEVEL_EDITOR,
  CUSTOM_GAME
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val prefs = GamePreferences(this)
    val storage = CustomLevelStorage(this)
    val audio = RetroAudioEngine(this).apply {
      soundEnabled = prefs.soundEnabled
      hapticsEnabled = prefs.hapticsEnabled
    }
    val updateManager = AppUpdateManager(this, prefs)

    setContent {
      MyApplicationTheme(darkTheme = true) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = Color(0xFF030712)
        ) {
          PixelRiftApp(
            prefs = prefs,
            storage = storage,
            audio = audio,
            updateManager = updateManager
          )
        }
      }
    }
  }
}

@Composable
fun PixelRiftApp(
  prefs: GamePreferences,
  storage: CustomLevelStorage,
  audio: RetroAudioEngine,
  updateManager: AppUpdateManager
) {
  var currentScreen by remember { mutableStateOf(ScreenState.MAIN_MENU) }
  var selectedLevelId by remember { mutableIntStateOf(1) }
  var activeCustomLevel by remember { mutableStateOf<CustomLevelData?>(null) }
  var editingLevelId by remember { mutableStateOf<Int?>(null) }
  var showSettingsDialog by remember { mutableStateOf(false) }

  // Updater States
  var activeUpdateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
  var isDownloadingUpdate by remember { mutableStateOf(false) }
  var downloadProgress by remember { mutableFloatStateOf(0f) }
  var downloadedBytes by remember { mutableLongStateOf(0L) }
  var totalDownloadBytes by remember { mutableLongStateOf(0L) }
  var updateErrorMessage by remember { mutableStateOf<String?>(null) }

  // Auto-check on startup if enabled
  LaunchedEffect(Unit) {
    if (prefs.autoCheckUpdates) {
      updateManager.checkForUpdate(
        isManual = false,
        onResult = { info ->
          if (info != null) {
            activeUpdateInfo = info
          }
        }
      )
    }
  }

  val checkUpdateAction: (isManual: Boolean) -> Unit = { isManual ->
    updateErrorMessage = null
    updateManager.checkForUpdate(
      isManual = isManual,
      onResult = { info ->
        if (info != null) {
          activeUpdateInfo = info
        } else if (isManual) {
          // Fallback test update for instant preview and user convenience
          activeUpdateInfo = updateManager.getTestUpdateInfo()
        }
      },
      onError = { err ->
        updateErrorMessage = err
        if (isManual) {
          // Open dialog with test info and error so user can review link or retry
          activeUpdateInfo = updateManager.getTestUpdateInfo()
        }
      }
    )
  }

  val testUpdateAction: () -> Unit = {
    updateErrorMessage = null
    isDownloadingUpdate = false
    downloadProgress = 0f
    downloadedBytes = 0L
    totalDownloadBytes = 0L
    activeUpdateInfo = updateManager.getTestUpdateInfo()
  }

  val startDownloadAction: (UpdateInfo) -> Unit = { info ->
    isDownloadingUpdate = true
    downloadProgress = 0f
    downloadedBytes = 0L
    totalDownloadBytes = 0L
    updateErrorMessage = null

    updateManager.downloadAndInstall(
      updateInfo = info,
      onProgress = { prog, loaded, total ->
        downloadProgress = prog
        downloadedBytes = loaded
        totalDownloadBytes = total
      },
      onSuccess = {
        isDownloadingUpdate = false
      },
      onError = { err ->
        isDownloadingUpdate = false
        updateErrorMessage = err
      }
    )
  }

  when (currentScreen) {
    ScreenState.MAIN_MENU -> {
      MainMenuScreen(
        prefs = prefs,
        audio = audio,
        onPlayClick = {
          selectedLevelId = prefs.unlockedLevelMax.coerceIn(1, 5)
          activeCustomLevel = null
          currentScreen = ScreenState.GAME
        },
        onLevelSelectClick = {
          currentScreen = ScreenState.LEVEL_SELECT
        },
        onUpgradesClick = {
          currentScreen = ScreenState.UPGRADES
        },
        onEditorClick = {
          editingLevelId = null
          currentScreen = ScreenState.LEVEL_EDITOR
        },
        onSettingsClick = {
          showSettingsDialog = true
        },
        onUpdateClick = {
          checkUpdateAction(true)
        }
      )
    }

    ScreenState.LEVEL_SELECT -> {
      LevelSelectScreen(
        prefs = prefs,
        storage = storage,
        audio = audio,
        onSelectLevel = { levelId ->
          selectedLevelId = levelId
          activeCustomLevel = null
          currentScreen = ScreenState.GAME
        },
        onSelectCustomLevel = { customLevel ->
          activeCustomLevel = customLevel
          currentScreen = ScreenState.CUSTOM_GAME
        },
        onCreateNewLevel = {
          editingLevelId = null
          currentScreen = ScreenState.LEVEL_EDITOR
        },
        onEditCustomLevel = { levelId ->
          editingLevelId = levelId
          currentScreen = ScreenState.LEVEL_EDITOR
        },
        onBackClick = {
          currentScreen = ScreenState.MAIN_MENU
        }
      )
    }

    ScreenState.UPGRADES -> {
      UpgradeScreen(
        prefs = prefs,
        audio = audio,
        onBackClick = {
          currentScreen = ScreenState.MAIN_MENU
        }
      )
    }

    ScreenState.GAME -> {
      GameScreen(
        levelId = selectedLevelId,
        customLevel = null,
        prefs = prefs,
        audio = audio,
        onLevelComplete = { completedLevelId ->
          if (completedLevelId < 5) {
            selectedLevelId = completedLevelId + 1
          } else {
            currentScreen = ScreenState.LEVEL_SELECT
          }
        },
        onUpgradesClick = {
          currentScreen = ScreenState.UPGRADES
        },
        onLevelSelectClick = {
          currentScreen = ScreenState.LEVEL_SELECT
        },
        onMainMenuClick = {
          currentScreen = ScreenState.MAIN_MENU
        }
      )
    }

    ScreenState.CUSTOM_GAME -> {
      GameScreen(
        levelId = 999,
        customLevel = activeCustomLevel,
        prefs = prefs,
        audio = audio,
        onLevelComplete = {
          currentScreen = ScreenState.LEVEL_SELECT
        },
        onUpgradesClick = {
          currentScreen = ScreenState.UPGRADES
        },
        onLevelSelectClick = {
          currentScreen = ScreenState.LEVEL_SELECT
        },
        onMainMenuClick = {
          currentScreen = ScreenState.MAIN_MENU
        }
      )
    }

    ScreenState.LEVEL_EDITOR -> {
      LevelEditorScreen(
        levelId = editingLevelId,
        storage = storage,
        prefs = prefs,
        audio = audio,
        onTestPlay = { testLevel ->
          activeCustomLevel = testLevel
          currentScreen = ScreenState.CUSTOM_GAME
        },
        onBackClick = {
          currentScreen = ScreenState.LEVEL_SELECT
        }
      )
    }
  }

  if (showSettingsDialog) {
    SettingsDialog(
      prefs = prefs,
      audio = audio,
      updateManager = updateManager,
      onCheckUpdate = { checkUpdateAction(true) },
      onTestUpdate = { testUpdateAction() },
      onDismiss = { showSettingsDialog = false }
    )
  }

  // In-Game Update Dialog: "Эй, я нашёл новую версию, скачаем?"
  activeUpdateInfo?.let { updateInfo ->
    UpdateDialog(
      updateInfo = updateInfo,
      currentVersionName = updateManager.currentVersionName,
      isDownloading = isDownloadingUpdate,
      progress = downloadProgress,
      downloadedBytes = downloadedBytes,
      totalBytes = totalDownloadBytes,
      errorMessage = updateErrorMessage,
      audio = audio,
      onDownloadClick = { startDownloadAction(updateInfo) },
      onOpenBrowserClick = { updateManager.openInBrowser(updateInfo.downloadUrl) },
      onDismiss = {
        activeUpdateInfo = null
        isDownloadingUpdate = false
      }
    )
  }
}
