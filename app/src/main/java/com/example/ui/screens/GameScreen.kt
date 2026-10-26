package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import com.example.data.GamePreferences
import com.example.data.models.CustomLevelData
import com.example.data.models.toLevelDef
import com.example.engine.GameEngine
import com.example.engine.LevelCatalog
import com.example.engine.RetroAudioEngine
import com.example.ui.components.GameCanvas
import com.example.ui.components.GameHud
import com.example.ui.components.TouchControls

@Composable
fun GameScreen(
  levelId: Int,
  customLevel: CustomLevelData? = null,
  prefs: GamePreferences,
  audio: RetroAudioEngine,
  onLevelComplete: (Int) -> Unit,
  onUpgradesClick: () -> Unit,
  onLevelSelectClick: () -> Unit,
  onMainMenuClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  var currentLevel by remember { mutableIntStateOf(levelId) }
  var restartTrigger by remember { mutableIntStateOf(0) }
  var isPaused by remember { mutableStateOf(false) }
  var frameTick by remember { mutableLongStateOf(0L) }

  val focusRequester = remember { FocusRequester() }

  // Game Engine instance
  val engine = remember(currentLevel, customLevel, restartTrigger) {
    val levelDef = customLevel?.toLevelDef() ?: LevelCatalog.getLevel(currentLevel)
    GameEngine(levelDef, prefs, audio)
  }

  BackHandler {
    if (!isPaused && !engine.isLevelCompleted) {
      isPaused = true
    } else {
      onMainMenuClick()
    }
  }

  // Auto-focus for keyboard support in emulator/web preview
  LaunchedEffect(Unit) {
    try {
      focusRequester.requestFocus()
    } catch (_: Exception) {}
  }

  // 60 FPS Game Loop
  LaunchedEffect(isPaused, engine) {
    if (isPaused) return@LaunchedEffect

    var lastTimeNanos = 0L
    while (!engine.isLevelCompleted) {
      withFrameNanos { nowNanos ->
        if (lastTimeNanos != 0L) {
          val dt = (nowNanos - lastTimeNanos) / 1_000_000_000f
          engine.update(dt)
          // Updating frameTick forces recomposition of GameCanvas and GameHud every frame!
          frameTick = nowNanos
        }
        lastTimeNanos = nowNanos
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black)
      .focusRequester(focusRequester)
      .focusable()
      .onKeyEvent { event ->
        val isDown = event.type == KeyEventType.KeyDown
        val isUp = event.type == KeyEventType.KeyUp

        when (event.key) {
          Key.DirectionLeft, Key.A -> {
            if (isDown) {
              engine.inputMoveX = -1f
            } else if (isUp) {
              if (engine.inputMoveX < 0f) engine.inputMoveX = 0f
            }
            true
          }
          Key.DirectionRight, Key.D -> {
            if (isDown) {
              engine.inputMoveX = 1f
            } else if (isUp) {
              if (engine.inputMoveX > 0f) engine.inputMoveX = 0f
            }
            true
          }
          Key.DirectionUp, Key.W, Key.Spacebar -> {
            if (isDown) {
              engine.onJumpPressed()
            } else if (isUp) {
              engine.onJumpReleased()
            }
            true
          }
          Key.ShiftLeft, Key.ShiftRight, Key.J -> {
            if (isDown) {
              engine.onDashPressed()
            }
            true
          }
          Key.E, Key.K -> {
            if (isDown) {
              engine.onChronoPressed()
            }
            true
          }
          Key.T, Key.Q -> {
            if (isDown) {
              engine.onTeleportPressed()
            }
            true
          }
          Key.Escape, Key.P -> {
            if (isDown) {
              isPaused = !isPaused
            }
            true
          }
          else -> false
        }
      }
  ) {
    // 1. Rendering Canvas
    GameCanvas(
      engine = engine,
      modifier = Modifier.fillMaxSize()
    )

    // 2. Heads-Up Display
    GameHud(
      engine = engine,
      onPauseClick = { isPaused = true },
      modifier = Modifier.align(Alignment.TopCenter)
    )

    // 3. Touch Controls (D-Pad, Jump, Dash, Chrono)
    TouchControls(
      engine = engine,
      modifier = Modifier.align(Alignment.BottomCenter)
    )

    // Pause Dialog
    if (isPaused) {
      PauseDialog(
        audio = audio,
        onResume = { isPaused = false },
        onRestartCheckpoint = {
          engine.respawn()
          isPaused = false
        },
        onRestartLevel = {
          restartTrigger++
          isPaused = false
        },
        onLevelSelect = {
          isPaused = false
          onLevelSelectClick()
        },
        onMainMenu = {
          isPaused = false
          onMainMenuClick()
        }
      )
    }

    // Victory Dialog
    if (engine.isLevelCompleted) {
      VictoryDialog(
        levelId = currentLevel,
        levelTimeSeconds = engine.levelTimeSeconds,
        artifactsCount = engine.totalArtifacts,
        dustEarned = engine.collectedDust,
        hasNextLevel = customLevel == null && currentLevel < LevelCatalog.totalLevels,
        audio = audio,
        onNextLevel = {
          if (customLevel == null && currentLevel < LevelCatalog.totalLevels) {
            currentLevel++
          } else {
            onLevelSelectClick()
          }
        },
        onUpgrades = onUpgradesClick,
        onMainMenu = onMainMenuClick
      )
    }
  }
}
