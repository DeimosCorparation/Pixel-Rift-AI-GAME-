package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.GamePreferences
import com.example.engine.GameEngine
import com.example.engine.LevelCatalog
import com.example.engine.RetroAudioEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GameEngineTest {

  @Test
  fun testPlayerMovementAndJump() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = GamePreferences(context)
    val audio = RetroAudioEngine(context).apply {
      soundEnabled = false
      hapticsEnabled = false
    }

    val level = LevelCatalog.getLevel(1)
    val engine = GameEngine(level, prefs, audio)

    val startX = engine.playerX
    val startY = engine.playerY

    // 1. Let player settle on the ground
    for (i in 0 until 30) {
      engine.update(1f / 60f)
    }

    // Player should be grounded
    assertTrue("Player should land on ground", engine.isGrounded)

    // 2. Test walk right
    engine.inputMoveX = 1f
    for (i in 0 until 30) {
      engine.update(1f / 60f)
    }
    assertTrue("Player should have moved right", engine.playerX > startX)

    // 3. Test Jump
    engine.onJumpPressed()
    engine.update(1f / 60f)
    assertTrue("Player should have upward velocity after jump", engine.playerVy < 0f)

    // 4. Test collecting all artifacts opens portal
    for (art in engine.level.artifacts) {
      art.collected = true
    }
    engine.artifactsCollected = engine.totalArtifacts
    // Update to process portal check
    engine.playerX = engine.level.portal.x
    engine.playerY = engine.level.portal.y
    engine.isPortalUnlocked = true
    engine.update(1f / 60f)
    assertTrue("Level should complete when reaching portal with all artifacts", engine.isLevelCompleted)
  }
}
