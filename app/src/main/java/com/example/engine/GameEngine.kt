package com.example.engine

import com.example.data.GamePreferences
import com.example.data.models.Checkpoint
import com.example.data.models.CrumblingBlockState
import com.example.data.models.LevelDef
import com.example.data.models.TileType
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class GhostTrail(
  val x: Float,
  val y: Float,
  val facingRight: Boolean,
  var alpha: Float = 0.7f
)

data class GameParticle(
  var x: Float,
  var y: Float,
  var vx: Float,
  var vy: Float,
  var color: Long,
  var size: Float,
  var life: Float,
  val maxLife: Float
)

data class Firefly(
  var x: Float,
  var y: Float,
  var baseAlpha: Float,
  var phase: Float
)

enum class PlayerAnimState {
  IDLE, RUN, JUMP, FALL, WALL_SLIDE, DASH
}

enum class DayPhase(val titleRu: String) {
  DAWN("Рассвет"),
  DAY("День"),
  DUSK("Закат"),
  NIGHT("Ночь")
}

class GameEngine(
  val level: LevelDef,
  val prefs: GamePreferences,
  val audio: RetroAudioEngine
) {
  // Render tick state: notifies Compose DrawScope to redraw every frame
  var renderTick by mutableLongStateOf(0L)

  // Player bounds & position
  val playerWidth = 20f
  val playerHeight = 28f
  var playerX = level.spawnX
  var playerY = level.spawnY
  var playerVx = 0f
  var playerVy = 0f
  var facingRight = true
  var isGrounded = false
  var wallSlideDir = 0 // -1 = left, 1 = right, 0 = none

  // Upgrades stats applied
  val maxHearts = 3 + prefs.shieldRank.coerceIn(0, 2)
  var currentHearts = maxHearts
  var hasBarrierShield = prefs.shieldRank > 0

  private val baseRunSpeed = 160f * (1f + prefs.speedRank * 0.15f)
  private val baseJumpVelocity = -390f * (1f + prefs.jumpRank * 0.12f)
  private val hasDoubleJump = prefs.doubleJumpRank > 0
  private val hasDash = prefs.dashRank > 0
  private val dashDistanceMultiplier = 1f + prefs.dashRank * 0.25f
  private val magnetRadius = if (prefs.magnetRank > 0) 70f + prefs.magnetRank * 40f else 0f
  val hasChrono = prefs.chronoRank > 0
  val hasGlide = prefs.glideRank > 0
  val glideFallSpeed = if (prefs.glideRank >= 2) 90f else 150f
  val dustMultiplier = 1f + prefs.dustMultiplierRank * 0.5f
  val wallJumpMultiplier = 1f + prefs.wallJumpRank * 0.35f
  val hasRadar = prefs.radarRank > 0
  var radarAngle: Float? = null
  var radarDistance: Float? = null
  val hasShieldRecharge = prefs.shieldRechargeRank > 0
  var shieldRechargeTimer = 0f
  val shieldRechargeInterval = if (prefs.shieldRechargeRank >= 2) 10f else 18f
  val hasPhoenix = prefs.phoenixRank > 0
  var phoenixUsedInLevel = false

  // Ray Teleport (Beam Teleport)
  val hasRayTeleport = prefs.rayTeleportRank > 0
  val rayTeleportRank = prefs.rayTeleportRank
  val teleportCooldown = 5.0f
  var teleportCooldownTimer = 0f

  // Jump helpers
  var canDoubleJump = false
  var coyoteTimer = 0f
  var jumpBufferTimer = 0f
  val coyoteDuration = 0.14f
  val jumpBufferDuration = 0.14f

  // Dash state
  var isDashing = false
  var dashTimer = 0f
  var dashCooldownTimer = 0f
  val dashDuration = 0.18f
  val dashCooldown = 0.8f

  // Chrono Shift (Time Dilation)
  var isChronoActive = false
  var chronoTimer = 0f
  var chronoCooldownTimer = 0f
  val chronoDuration = 3.5f
  val chronoCooldown = 9.0f

  // Invulnerability & recoil
  var invulnerabilityTimer = 0f
  var screenShake = 0f

  // Input states
  var inputMoveX = 0f
  var isHoldingJump = false

  // Level Progression
  var artifactsCollected = 0
  val totalArtifacts = level.artifacts.size
  var collectedDust = 0
  var levelTimeSeconds = 0f
  var isPortalUnlocked = false
  var isLevelCompleted = false
  var isGameOver = false
  var activeCheckpoint: Checkpoint? = null
  var portalPromptText: String? = null
  var portalPromptTimer = 0f

  // Day/Night cycle
  var dayTime = 0.25f // Start at bright morning
  var dayPhase: DayPhase = DayPhase.DAY
  var ambientDarkness = 0f // 0f = day, up to 0.75f = night
  val fireflies = mutableListOf<Firefly>()

  // Particles & Visuals
  val particles = mutableListOf<GameParticle>()
  val ghostTrails = mutableListOf<GhostTrail>()
  val crumblingStates = mutableMapOf<Pair<Int, Int>, CrumblingBlockState>()
  var animState = PlayerAnimState.IDLE
  var animTimer = 0f

  init {
    // Initialize fireflies across level
    val levelWidthPx = level.tileCols * level.tileSize
    val levelHeightPx = level.tileRows * level.tileSize
    for (i in 0 until 30) {
      fireflies.add(
        Firefly(
          x = (i * 97f) % levelWidthPx,
          y = 50f + (i * 47f) % (levelHeightPx - 100f),
          baseAlpha = 0.3f + (i % 5) * 0.14f,
          phase = (i * 1.3f) % 6.28f
        )
      )
    }
  }

  fun onJumpPressed() {
    jumpBufferTimer = jumpBufferDuration
  }

  fun onJumpReleased() {
    isHoldingJump = false
    // Variable jump height: cut upward velocity if released early
    if (playerVy < -120f) {
      playerVy *= 0.45f
    }
  }

  fun onDashPressed() {
    if (!hasDash || isDashing || dashCooldownTimer > 0f) return
    isDashing = true
    dashTimer = dashDuration
    dashCooldownTimer = dashCooldown
    invulnerabilityTimer = dashDuration + 0.1f

    val dir = if (inputMoveX != 0f) {
      if (inputMoveX > 0) 1f else -1f
    } else {
      if (facingRight) 1f else -1f
    }

    playerVx = dir * 420f * dashDistanceMultiplier
    playerVy = 0f
    screenShake = 3.5f

    audio.playDash()
    emitDashParticles()
  }

  fun onChronoPressed() {
    if (!hasChrono || isChronoActive || chronoCooldownTimer > 0f) return
    isChronoActive = true
    chronoTimer = chronoDuration
    chronoCooldownTimer = chronoCooldown
    audio.playChronoShift()
    screenShake = 2.0f
  }

  fun onTeleportPressed() {
    if (!hasRayTeleport || teleportCooldownTimer > 0f) return

    val blocks = if (rayTeleportRank >= 2) 2 else 1
    val dir = if (facingRight) 1f else -1f
    val stepDistance = level.tileSize

    val oldX = playerX
    val oldY = playerY

    // Test step by step
    var finalX = playerX
    for (step in 1..blocks) {
      val testX = (playerX + dir * stepDistance * step).coerceIn(0f, level.tileCols * level.tileSize - playerWidth)
      if (!isOverlappingSolid(testX, playerY)) {
        finalX = testX
      } else {
        break
      }
    }

    if (finalX == oldX) {
      // Direct step was blocked: try full distance if spot itself is free
      val candidate = (playerX + dir * stepDistance * blocks).coerceIn(0f, level.tileCols * level.tileSize - playerWidth)
      if (!isOverlappingSolid(candidate, playerY)) {
        finalX = candidate
      }
    }

    playerX = finalX
    playerVx = 0f
    teleportCooldownTimer = teleportCooldown
    screenShake = 3.5f
    audio.playDash()
    emitRayBeam(oldX + playerWidth / 2f, oldY + playerHeight / 2f, playerX + playerWidth / 2f, playerY + playerHeight / 2f)
  }

  private fun isOverlappingSolid(px: Float, py: Float): Boolean {
    val minTileX = (px / level.tileSize).toInt().coerceAtLeast(0)
    val maxTileX = ((px + playerWidth - 1f) / level.tileSize).toInt().coerceAtMost(level.tileCols - 1)
    val minTileY = (py / level.tileSize).toInt().coerceAtLeast(0)
    val maxTileY = ((py + playerHeight - 1f) / level.tileSize).toInt().coerceAtMost(level.tileRows - 1)

    for (ty in minTileY..maxTileY) {
      for (tx in minTileX..maxTileX) {
        if (isTileSolidAt(tx, ty, false)) {
          return true
        }
      }
    }
    return false
  }

  private fun emitRayBeam(startX: Float, startY: Float, endX: Float, endY: Float) {
    val count = 22
    for (i in 0..count) {
      val t = i.toFloat() / count.toFloat()
      val bx = startX + (endX - startX) * t
      val by = startY + (endY - startY) * t
      particles.add(
        GameParticle(
          x = bx + (-3f + Math.random().toFloat() * 6f),
          y = by + (-3f + Math.random().toFloat() * 6f),
          vx = (-12f + Math.random().toFloat() * 24f),
          vy = (-12f + Math.random().toFloat() * 24f),
          color = if (i % 2 == 0) 0xFF00FFFF else 0xFF2DD4BF,
          size = 3.8f,
          life = 0.25f,
          maxLife = 0.25f
        )
      )
    }
    ghostTrails.add(GhostTrail(startX - playerWidth / 2f, startY - playerHeight / 2f, facingRight, 0.75f))
    ghostTrails.add(GhostTrail(endX - playerWidth / 2f, endY - playerHeight / 2f, facingRight, 0.85f))
  }

  fun update(rawDt: Float) {
    if (isLevelCompleted || isGameOver) return

    val dt = rawDt.coerceIn(0.001f, 0.05f)
    val worldTimeScale = if (isChronoActive) 0.5f else 1.0f

    levelTimeSeconds += dt

    // Day/Night cycle continuous progress
    val cycleSpeed = prefs.cycleSpeed
    dayTime = (dayTime + (dt / 80f) * cycleSpeed) % 1.0f
    updateDayNightState(dt)

    // Chrono shift countdown
    if (isChronoActive) {
      chronoTimer -= dt
      if (chronoTimer <= 0f) {
        isChronoActive = false
      }
    }
    if (chronoCooldownTimer > 0f) {
      chronoCooldownTimer -= dt
    }

    // Portal prompt toast timer
    if (portalPromptTimer > 0f) {
      portalPromptTimer -= dt
      if (portalPromptTimer <= 0f) {
        portalPromptText = null
      }
    }

    // Timers
    if (invulnerabilityTimer > 0f) invulnerabilityTimer -= dt
    if (dashCooldownTimer > 0f) dashCooldownTimer -= dt
    if (teleportCooldownTimer > 0f) teleportCooldownTimer -= dt
    if (screenShake > 0f) screenShake = (screenShake - dt * 14f).coerceAtLeast(0f)
    if (coyoteTimer > 0f) coyoteTimer -= dt
    if (jumpBufferTimer > 0f) jumpBufferTimer -= dt

    // Update moving platforms
    updateMovingPlatforms(dt * worldTimeScale)

    // Update crumbling blocks
    updateCrumblingBlocks(dt * worldTimeScale)

    // Player physics
    if (isDashing) {
      updateDashPhysics(dt)
    } else {
      updateNormalPhysics(dt)
    }

    // Magnet mechanics for artifacts and star dust
    updateMagnet(dt)
    updateRadar()
    updateShieldRecharge(dt)

    // Collectibles & Hazards check
    checkCollectibles()
    checkHazards()
    checkCheckpoints()
    checkPortal()

    // Particles & Visual effects
    updateParticles(dt)
    updateGhostTrails(dt)
    animTimer += dt
    renderTick++
  }

  private fun updateDayNightState(dt: Float) {
    // 0.00 - 0.20: Dawn
    // 0.20 - 0.60: Day
    // 0.60 - 0.78: Dusk
    // 0.78 - 1.00: Night
    when {
      dayTime < 0.20f -> {
        dayPhase = DayPhase.DAWN
        val p = dayTime / 0.20f
        ambientDarkness = 0.55f * (1f - p)
      }
      dayTime < 0.60f -> {
        dayPhase = DayPhase.DAY
        ambientDarkness = 0.0f
      }
      dayTime < 0.78f -> {
        dayPhase = DayPhase.DUSK
        val p = (dayTime - 0.60f) / 0.18f
        ambientDarkness = 0.45f * p
      }
      else -> {
        dayPhase = DayPhase.NIGHT
        val p = (dayTime - 0.78f) / 0.22f
        ambientDarkness = 0.45f + 0.25f * sin(p * Math.PI.toFloat())
      }
    }

    // Animate fireflies
    for (ff in fireflies) {
      ff.phase = (ff.phase + dt * 1.5f) % 6.28f
      ff.x += sin(ff.phase) * 6f * dt
      ff.y += cos(ff.phase * 0.8f) * 4f * dt
    }
  }

  private fun updateDashPhysics(dt: Float) {
    dashTimer -= dt
    if (dashTimer <= 0f) {
      isDashing = false
      playerVx *= 0.4f
    }
    // Dash ghost trail
    if (animTimer % 0.04f < dt) {
      ghostTrails.add(GhostTrail(playerX, playerY, facingRight, 0.7f))
    }
    // Move with horizontal collision
    moveAndCollideX(playerVx * dt)
  }

  private fun updateNormalPhysics(dt: Float) {
    // Target horizontal speed
    val targetVx = inputMoveX * baseRunSpeed
    val accel = if (isGrounded) 1400f else 900f
    val friction = if (isGrounded) 1600f else 400f

    if (inputMoveX != 0f) {
      if (inputMoveX > 0) facingRight = true else facingRight = false
      if (playerVx < targetVx) {
        playerVx = (playerVx + accel * dt).coerceAtMost(targetVx)
      } else {
        playerVx = (playerVx - accel * dt).coerceAtLeast(targetVx)
      }
    } else {
      if (playerVx > 0) {
        playerVx = (playerVx - friction * dt).coerceAtLeast(0f)
      } else if (playerVx < 0) {
        playerVx = (playerVx + friction * dt).coerceAtMost(0f)
      }
    }

    // Gravity & Wall sliding
    val gravity = 980f
    wallSlideDir = checkWallContact()

    if (wallSlideDir != 0 && playerVy > 0 && !isGrounded && inputMoveX != 0f) {
      // Wall slide: limit descent speed
      playerVy = (playerVy + gravity * 0.35f * dt).coerceAtMost(100f)
      animState = PlayerAnimState.WALL_SLIDE
      // Wall slide particles
      if (Math.random() < 0.2) {
        particles.add(
          GameParticle(
            x = if (wallSlideDir == -1) playerX else playerX + playerWidth,
            y = playerY + playerHeight * 0.7f,
            vx = if (wallSlideDir == -1) 30f else -30f,
            vy = -20f,
            color = 0xFFCCCCCC,
            size = 3f,
            life = 0.2f,
            maxLife = 0.2f
          )
        )
      }
    } else if (hasGlide && isHoldingJump && playerVy > 40f && !isGrounded) {
      // Lunar Glide: gently glide through air
      playerVy = (playerVy + gravity * 0.2f * dt).coerceAtMost(glideFallSpeed)
      if (Math.random() < 0.3) {
        particles.add(
          GameParticle(
            x = playerX + playerWidth / 2f + (-8f + Math.random().toFloat() * 16f),
            y = playerY + playerHeight + 2f,
            vx = (-20f + Math.random().toFloat() * 40f),
            vy = 12f,
            color = 0xFFBAE6FD,
            size = 3.5f,
            life = 0.3f,
            maxLife = 0.3f
          )
        )
      }
    } else {
      playerVy += gravity * dt
      if (playerVy > 650f) playerVy = 650f
    }

    // Jump handling (Buffered Jump & Coyote Time)
    if (jumpBufferTimer > 0f) {
      if (coyoteTimer > 0f || isGrounded) {
        // Ground Jump
        playerVy = baseJumpVelocity
        isGrounded = false
        coyoteTimer = 0f
        jumpBufferTimer = 0f
        isHoldingJump = true
        audio.playJump()
        emitDustParticles(playerX + playerWidth / 2f, playerY + playerHeight)
      } else if (wallSlideDir != 0) {
        // Wall Jump
        playerVy = baseJumpVelocity * 0.95f * (1f + prefs.wallJumpRank * 0.18f)
        playerVx = -wallSlideDir * (baseRunSpeed * 1.15f * wallJumpMultiplier)
        facingRight = wallSlideDir < 0
        wallSlideDir = 0
        jumpBufferTimer = 0f
        isHoldingJump = true
        audio.playJump()
        emitDustParticles(playerX + playerWidth / 2f, playerY + playerHeight / 2f)
      } else if (hasDoubleJump && canDoubleJump) {
        // Double Jump
        playerVy = baseJumpVelocity * 0.88f
        canDoubleJump = false
        jumpBufferTimer = 0f
        isHoldingJump = true
        audio.playDoubleJump()
        emitDoubleJumpWings(playerX + playerWidth / 2f, playerY + playerHeight / 2f)
      }
    }

    // Apply movement with AABB collision resolution
    moveAndCollideX(playerVx * dt)
    moveAndCollideY(playerVy * dt)

    // Animation state
    animState = when {
      wallSlideDir != 0 && !isGrounded -> PlayerAnimState.WALL_SLIDE
      !isGrounded && playerVy < -50f -> PlayerAnimState.JUMP
      !isGrounded && playerVy >= -50f -> PlayerAnimState.FALL
      kotlin.math.abs(playerVx) > 15f -> PlayerAnimState.RUN
      else -> PlayerAnimState.IDLE
    }
  }

  private fun moveAndCollideX(dx: Float) {
    playerX += dx
    val minTileX = (playerX / level.tileSize).toInt().coerceAtLeast(0)
    val maxTileX = ((playerX + playerWidth) / level.tileSize).toInt().coerceAtMost(level.tileCols - 1)
    val minTileY = (playerY / level.tileSize).toInt().coerceAtLeast(0)
    val maxTileY = ((playerY + playerHeight - 1f) / level.tileSize).toInt().coerceAtMost(level.tileRows - 1)

    for (ty in minTileY..maxTileY) {
      for (tx in minTileX..maxTileX) {
        if (isTileSolidAt(tx, ty, false)) {
          if (dx > 0) {
            playerX = tx * level.tileSize - playerWidth - 0.01f
            playerVx = 0f
          } else if (dx < 0) {
            playerX = (tx + 1) * level.tileSize + 0.01f
            playerVx = 0f
          }
          return
        }
      }
    }
  }

  private fun moveAndCollideY(dy: Float) {
    val prevBottom = playerY + playerHeight
    playerY += dy
    val minTileX = (playerX / level.tileSize).toInt().coerceAtLeast(0)
    val maxTileX = ((playerX + playerWidth) / level.tileSize).toInt().coerceAtMost(level.tileCols - 1)
    val minTileY = (playerY / level.tileSize).toInt().coerceAtLeast(0)
    val maxTileY = ((playerY + playerHeight) / level.tileSize).toInt().coerceAtMost(level.tileRows - 1)

    var landed = false

    for (ty in minTileY..maxTileY) {
      for (tx in minTileX..maxTileX) {
        val tileId = level.tiles[ty][tx]
        val tileType = TileType.fromId(tileId)

        // Crumbling block trigger
        if (tileType == TileType.CRUMBLING && isCrumblingSolid(tx, ty)) {
          triggerCrumble(tx, ty)
        }

        // Bounce pad trigger
        if (tileType == TileType.BOUNCE_PAD && dy > 0 && prevBottom <= ty * level.tileSize + 8f) {
          playerY = ty * level.tileSize - playerHeight - 0.01f
          playerVy = -640f // Launch high!
          canDoubleJump = hasDoubleJump
          audio.playJump()
          emitBounceParticles(tx * level.tileSize + level.tileSize / 2f, ty * level.tileSize)
          return
        }

        if (isTileSolidAt(tx, ty, isVerticalMoveDown = dy > 0, prevBottom = prevBottom)) {
          if (dy > 0) {
            playerY = ty * level.tileSize - playerHeight - 0.01f
            playerVy = 0f
            landed = true
          } else if (dy < 0 && !tileType.isPassableFromBelow) {
            playerY = (ty + 1) * level.tileSize + 0.01f
            playerVy = 0f
          }
        }
      }
    }

    // Moving platforms collision (on top)
    if (dy >= 0) {
      for (plat in level.movingPlatforms) {
        if (playerX + playerWidth > plat.currentX && playerX < plat.currentX + plat.width) {
          val platTop = plat.currentY
          if (prevBottom <= platTop + 8f && playerY + playerHeight >= platTop) {
            playerY = platTop - playerHeight - 0.01f
            playerVy = 0f
            // Carry player with platform speed
            playerX += plat.vx * (1f / 60f)
            landed = true
          }
        }
      }
    }

    if (landed) {
      if (!isGrounded) {
        // Just landed: puff dust
        emitDustParticles(playerX + playerWidth / 2f, playerY + playerHeight)
      }
      isGrounded = true
      canDoubleJump = hasDoubleJump
      coyoteTimer = coyoteDuration
    } else {
      isGrounded = false
    }
  }

  private fun isTileSolidAt(tx: Int, ty: Int, isVerticalMoveDown: Boolean = false, prevBottom: Float = 0f): Boolean {
    if (tx < 0 || tx >= level.tileCols || ty < 0 || ty >= level.tileRows) return false
    val tileId = level.tiles[ty][tx]
    val type = TileType.fromId(tileId)

    if (type == TileType.CRUMBLING) {
      return isCrumblingSolid(tx, ty)
    }

    if (type.isPassableFromBelow) {
      // Wood platform: only solid when falling from above the platform surface
      val tileTop = ty * level.tileSize
      return isVerticalMoveDown && prevBottom <= tileTop + 6f
    }

    return type.isSolid
  }

  private fun isCrumblingSolid(tx: Int, ty: Int): Boolean {
    val state = crumblingStates[Pair(tx, ty)] ?: return true
    return state.state != CrumblingBlockState.BlockState.FALLEN
  }

  private fun triggerCrumble(tx: Int, ty: Int) {
    val key = Pair(tx, ty)
    if (!crumblingStates.containsKey(key)) {
      crumblingStates[key] = CrumblingBlockState(tx, ty, timer = 0.35f, state = CrumblingBlockState.BlockState.SHAKING)
    }
  }

  private fun updateCrumblingBlocks(dt: Float) {
    val iterator = crumblingStates.entries.iterator()
    while (iterator.hasNext()) {
      val entry = iterator.next()
      val state = entry.value
      when (state.state) {
        CrumblingBlockState.BlockState.SHAKING -> {
          state.timer -= dt
          if (state.timer <= 0f) {
            state.state = CrumblingBlockState.BlockState.FALLEN
            state.timer = 3.2f // Respawn time
            // Emit crumble debris
            val wx = state.tileX * level.tileSize + level.tileSize / 2f
            val wy = state.tileY * level.tileSize + level.tileSize / 2f
            emitCrumbleDebris(wx, wy)
          }
        }
        CrumblingBlockState.BlockState.FALLEN -> {
          state.timer -= dt
          if (state.timer <= 0f) {
            iterator.remove() // Restored!
          }
        }
        else -> {}
      }
    }
  }

  private fun checkWallContact(): Int {
    val ty1 = ((playerY + 4f) / level.tileSize).toInt()
    val ty2 = ((playerY + playerHeight - 4f) / level.tileSize).toInt()

    // Left check
    val txLeft = ((playerX - 2f) / level.tileSize).toInt()
    if (txLeft >= 0) {
      for (ty in ty1..ty2) {
        if (ty in 0 until level.tileRows && TileType.fromId(level.tiles[ty][txLeft]).isSolid) {
          return -1
        }
      }
    }

    // Right check
    val txRight = ((playerX + playerWidth + 2f) / level.tileSize).toInt()
    if (txRight < level.tileCols) {
      for (ty in ty1..ty2) {
        if (ty in 0 until level.tileRows && TileType.fromId(level.tiles[ty][txRight]).isSolid) {
          return 1
        }
      }
    }

    return 0
  }

  private fun updateMovingPlatforms(dt: Float) {
    for (plat in level.movingPlatforms) {
      val totalDist = kotlin.math.hypot(plat.endX - plat.startX, plat.endY - plat.startY)
      if (totalDist == 0f) continue
      val step = (plat.speed * dt) / totalDist

      if (plat.forward) {
        plat.progress += step
        if (plat.progress >= 1f) {
          plat.progress = 1f
          plat.forward = false
        }
      } else {
        plat.progress -= step
        if (plat.progress <= 0f) {
          plat.progress = 0f
          plat.forward = true
        }
      }

      val oldX = plat.currentX
      val oldY = plat.currentY
      plat.currentX = plat.startX + (plat.endX - plat.startX) * plat.progress
      plat.currentY = plat.startY + (plat.endY - plat.startY) * plat.progress
      plat.vx = (plat.currentX - oldX) / dt
      plat.vy = (plat.currentY - oldY) / dt
    }
  }

  private fun updateMagnet(dt: Float) {
    if (magnetRadius <= 0f) return
    val px = playerX + playerWidth / 2f
    val py = playerY + playerHeight / 2f

    // Pull Star Dust
    for (dust in level.starDustList) {
      if (!dust.collected) {
        val dx = px - dust.x
        val dy = py - dust.y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist < magnetRadius && dist > 5f) {
          val speed = 260f * (1f - dist / magnetRadius) + 80f
          dust.x += (dx / dist) * speed * dt
          dust.y += (dy / dist) * speed * dt
        }
      }
    }

    // Pull Artifacts subtly
    for (art in level.artifacts) {
      if (!art.collected) {
        val dx = px - art.x
        val dy = py - art.y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist < magnetRadius * 0.7f && dist > 5f) {
          art.x += (dx / dist) * 90f * dt
          art.y += (dy / dist) * 90f * dt
        }
      }
    }
  }

  private fun updateRadar() {
    if (hasRadar && !isPortalUnlocked) {
      var minDist = Float.MAX_VALUE
      var targetAngle: Float? = null
      var targetDist: Float? = null
      val px = playerX + playerWidth / 2f
      val py = playerY + playerHeight / 2f
      for (art in level.artifacts) {
        if (!art.collected) {
          val dx = art.x - px
          val dy = art.y - py
          val d = kotlin.math.hypot(dx, dy)
          if (d < minDist) {
            minDist = d
            targetAngle = kotlin.math.atan2(dy, dx)
            targetDist = d
          }
        }
      }
      radarAngle = targetAngle
      radarDistance = targetDist
    } else {
      radarAngle = null
      radarDistance = null
    }
  }

  private fun updateShieldRecharge(dt: Float) {
    if (hasShieldRecharge && !hasBarrierShield && currentHearts > 0) {
      shieldRechargeTimer += dt
      if (shieldRechargeTimer >= shieldRechargeInterval) {
        hasBarrierShield = true
        shieldRechargeTimer = 0f
        audio.playPortalUnlock()
        emitSparkParticles(playerX + playerWidth / 2f, playerY + playerHeight / 2f, 0xFF38BDF8)
      }
    }
  }

  private fun checkCollectibles() {
    val px = playerX + playerWidth / 2f
    val py = playerY + playerHeight / 2f

    // Check Star Dust
    for (dust in level.starDustList) {
      if (!dust.collected) {
        val dx = px - dust.x
        val dy = py - dust.y
        if (dx * dx + dy * dy < 24f * 24f) {
          dust.collected = true
          val gained = (dust.value * dustMultiplier).toInt().coerceAtLeast(1)
          collectedDust += gained
          if (level.id < 1000) {
            prefs.starDust += gained
          }
          audio.playButtonClick()
          emitSparkParticles(dust.x, dust.y, 0xFFFFDF00)
        }
      }
    }

    // Check Artifacts
    for (art in level.artifacts) {
      if (!art.collected) {
        val dx = px - art.x
        val dy = py - art.y
        if (dx * dx + dy * dy < 28f * 28f) {
          art.collected = true
          artifactsCollected++
          val artBonus = (25 * dustMultiplier).toInt()
          if (level.id < 1000) {
            prefs.starDust += artBonus // Huge essence bonus for artifact!
          }
          collectedDust += artBonus
          audio.playArtifactPickup()
          screenShake = 4.0f
          emitSparkParticles(art.x, art.y, 0xFF00FFFF)

          // Check if all artifacts collected to unlock portal
          if (artifactsCollected >= totalArtifacts && !isPortalUnlocked) {
            isPortalUnlocked = true
            level.portal.isUnlocked = true
            audio.playPortalUnlock()
            screenShake = 6.0f
            portalPromptText = "✦ Портал пробуждён! Направляйтесь к выходу! ✦"
            portalPromptTimer = 4.0f
            emitPortalBurst(level.portal.x, level.portal.y)
          }
        }
      }
    }
  }

  private fun checkHazards() {
    val minTileX = (playerX / level.tileSize).toInt().coerceAtLeast(0)
    val maxTileX = ((playerX + playerWidth) / level.tileSize).toInt().coerceAtMost(level.tileCols - 1)
    val minTileY = (playerY / level.tileSize).toInt().coerceAtLeast(0)
    val maxTileY = ((playerY + playerHeight) / level.tileSize).toInt().coerceAtMost(level.tileRows - 1)

    // Check spike hazards
    for (ty in minTileY..maxTileY) {
      for (tx in minTileX..maxTileX) {
        val tile = TileType.fromId(level.tiles[ty][tx])
        if (tile.isHazard) {
          triggerDamage()
          return
        }
      }
    }

    // Fall into void (bottom of level) -> INSTANT DEATH
    if (playerY > level.tileRows * level.tileSize) {
      triggerVoidDeath()
    }
  }

  private fun triggerVoidDeath() {
    if (hasPhoenix && !phoenixUsedInLevel) {
      phoenixUsedInLevel = true
      invulnerabilityTimer = 2.5f
      currentHearts = maxHearts
      hasBarrierShield = true
      playerVx = 0f
      playerVy = -320f
      val cp = activeCheckpoint
      if (cp != null) {
        playerX = cp.x
        playerY = cp.y
      } else {
        playerX = level.spawnX
        playerY = level.spawnY
      }
      screenShake = 9.0f
      audio.playPortalUnlock()
      portalPromptText = "✦ Благословение Феникса спасло из бездны! ✦"
      portalPromptTimer = 3.5f
      emitPortalBurst(playerX + playerWidth / 2f, playerY + playerHeight / 2f)
      return
    }

    // Instant death in void without delay
    invulnerabilityTimer = 0f
    currentHearts = 0
    screenShake = 9.0f
    audio.playHurt()
    portalPromptText = "✦ Падение в бездну — гибель! ✦"
    portalPromptTimer = 2.5f
    respawn()
  }

  private fun triggerDamage(instantKill: Boolean = false) {
    if (invulnerabilityTimer > 0f && !instantKill) return

    if (hasPhoenix && !phoenixUsedInLevel && (instantKill || currentHearts <= 1 && !hasBarrierShield)) {
      phoenixUsedInLevel = true
      invulnerabilityTimer = 2.5f
      currentHearts = maxHearts
      hasBarrierShield = true
      playerVx = 0f
      playerVy = -320f
      val cp = activeCheckpoint
      if (cp != null) {
        playerX = cp.x
        playerY = cp.y
      } else {
        playerX = level.spawnX
        playerY = level.spawnY
      }
      screenShake = 8.0f
      audio.playPortalUnlock()
      portalPromptText = "✦ Благословение Феникса спасло вашу жизнь! ✦"
      portalPromptTimer = 3.5f
      emitPortalBurst(playerX + playerWidth / 2f, playerY + playerHeight / 2f)
      return
    }

    if (!instantKill && hasBarrierShield) {
      // Barrier absorbs the hit!
      hasBarrierShield = false
      invulnerabilityTimer = 1.2f
      screenShake = 5.0f
      audio.playHurt()
      emitSparkParticles(playerX + playerWidth / 2f, playerY + playerHeight / 2f, 0xFF38BDF8)
      playerVy = -250f
      return
    }

    currentHearts = if (instantKill) 0 else (currentHearts - 1).coerceAtLeast(0)
    screenShake = 7.0f
    audio.playHurt()

    if (currentHearts <= 0) {
      // Respawn at checkpoint or spawn
      respawn()
    } else {
      invulnerabilityTimer = 1.4f
      playerVy = -280f
      playerVx = if (facingRight) -140f else 140f
    }
  }

  fun respawn() {
    currentHearts = maxHearts
    hasBarrierShield = prefs.shieldRank > 0
    shieldRechargeTimer = 0f
    invulnerabilityTimer = 1.0f
    playerVx = 0f
    playerVy = 0f

    val cp = activeCheckpoint
    if (cp != null) {
      playerX = cp.x
      playerY = cp.y
    } else {
      playerX = level.spawnX
      playerY = level.spawnY
    }
    emitDustParticles(playerX + playerWidth / 2f, playerY + playerHeight)
  }

  private fun checkCheckpoints() {
    for (cp in level.checkpoints) {
      if (!cp.active) {
        val dx = (playerX + playerWidth / 2f) - cp.x
        val dy = (playerY + playerHeight / 2f) - cp.y
        if (dx * dx + dy * dy < 32f * 32f) {
          cp.active = true
          activeCheckpoint = cp
          audio.playButtonClick()
          portalPromptText = "Обелиск активирован! (Контрольная точка)"
          portalPromptTimer = 2.5f
          emitSparkParticles(cp.x, cp.y, 0xFF22C55E)
        }
      }
    }
  }

  private fun checkPortal() {
    val portal = level.portal
    val dx = (playerX + playerWidth / 2f) - portal.x
    val dy = (playerY + playerHeight / 2f) - portal.y
    val distSq = dx * dx + dy * dy

    if (distSq < 36f * 36f) {
      if (isPortalUnlocked) {
        // Complete the level!
        isLevelCompleted = true
        audio.playLevelComplete()
        prefs.setBestTime(level.id, (levelTimeSeconds * 1000).toLong())
        prefs.setCollectedArtifacts(level.id, artifactsCollected)
        if (level.id >= prefs.unlockedLevelMax) {
          prefs.unlockedLevelMax = level.id + 1
        }
      } else {
        // Portal is locked, show reminder
        val needed = totalArtifacts - artifactsCollected
        portalPromptText = "Портал заблокирован! Соберите ещё $needed реликвий"
        portalPromptTimer = 1.5f
      }
    }
  }

  // Particle Emitters
  private fun emitDustParticles(cx: Float, cy: Float) {
    for (i in 0 until 6) {
      particles.add(
        GameParticle(
          x = cx,
          y = cy,
          vx = (-60f + Math.random().toFloat() * 120f),
          vy = (-15f - Math.random().toFloat() * 30f),
          color = 0xFFE2E8F0,
          size = 3.5f,
          life = 0.25f,
          maxLife = 0.25f
        )
      )
    }
  }

  private fun emitDoubleJumpWings(cx: Float, cy: Float) {
    for (i in 0 until 10) {
      particles.add(
        GameParticle(
          x = cx,
          y = cy,
          vx = (-100f + Math.random().toFloat() * 200f),
          vy = (30f + Math.random().toFloat() * 60f),
          color = 0xFF38BDF8,
          size = 4f,
          life = 0.35f,
          maxLife = 0.35f
        )
      )
    }
  }

  private fun emitDashParticles() {
    for (i in 0 until 8) {
      particles.add(
        GameParticle(
          x = playerX + playerWidth / 2f,
          y = playerY + playerHeight / 2f,
          vx = (-playerVx * 0.25f + (-40f + Math.random().toFloat() * 80f)),
          vy = (-30f + Math.random().toFloat() * 60f),
          color = 0xFFA855F7,
          size = 3.5f,
          life = 0.3f,
          maxLife = 0.3f
        )
      )
    }
  }

  private fun emitBounceParticles(cx: Float, cy: Float) {
    for (i in 0 until 8) {
      particles.add(
        GameParticle(
          x = cx,
          y = cy,
          vx = (-80f + Math.random().toFloat() * 160f),
          vy = (-100f - Math.random().toFloat() * 80f),
          color = 0xFFEC4899,
          size = 4f,
          life = 0.4f,
          maxLife = 0.4f
        )
      )
    }
  }

  private fun emitSparkParticles(cx: Float, cy: Float, color: Long) {
    for (i in 0 until 12) {
      particles.add(
        GameParticle(
          x = cx,
          y = cy,
          vx = (-90f + Math.random().toFloat() * 180f),
          vy = (-90f + Math.random().toFloat() * 180f),
          color = color,
          size = 4.5f,
          life = 0.45f,
          maxLife = 0.45f
        )
      )
    }
  }

  private fun emitPortalBurst(cx: Float, cy: Float) {
    for (i in 0 until 24) {
      particles.add(
        GameParticle(
          x = cx,
          y = cy,
          vx = (-150f + Math.random().toFloat() * 300f),
          vy = (-150f + Math.random().toFloat() * 300f),
          color = if (i % 2 == 0) 0xFF00FFFF else 0xFFA855F7,
          size = 5f,
          life = 0.7f,
          maxLife = 0.7f
        )
      )
    }
  }

  private fun emitCrumbleDebris(cx: Float, cy: Float) {
    for (i in 0 until 8) {
      particles.add(
        GameParticle(
          x = cx,
          y = cy,
          vx = (-40f + Math.random().toFloat() * 80f),
          vy = (20f + Math.random().toFloat() * 100f),
          color = 0xFF78716C,
          size = 4f,
          life = 0.5f,
          maxLife = 0.5f
        )
      )
    }
  }

  private fun updateParticles(dt: Float) {
    val iter = particles.iterator()
    while (iter.hasNext()) {
      val p = iter.next()
      p.life -= dt
      if (p.life <= 0f) {
        iter.remove()
      } else {
        p.x += p.vx * dt
        p.y += p.vy * dt
      }
    }
  }

  private fun updateGhostTrails(dt: Float) {
    val iter = ghostTrails.iterator()
    while (iter.hasNext()) {
      val g = iter.next()
      g.alpha -= dt * 2.8f
      if (g.alpha <= 0f) {
        iter.remove()
      }
    }
  }
}
