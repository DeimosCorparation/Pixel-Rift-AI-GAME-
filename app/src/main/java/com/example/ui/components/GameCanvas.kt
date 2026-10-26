package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.example.data.models.Artifact
import com.example.data.models.Checkpoint
import com.example.data.models.CrumblingBlockState
import com.example.data.models.LevelTheme
import com.example.data.models.MovingPlatform
import com.example.data.models.Portal
import com.example.data.models.StarDustPickup
import com.example.data.models.TileType
import com.example.engine.DayPhase
import com.example.engine.GameEngine
import com.example.engine.PlayerAnimState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameCanvas(
  engine: GameEngine,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier.fillMaxSize()) {
    // CRITICAL: Reading engine.renderTick inside DrawScope invalidates this canvas EVERY FRAME at 60 FPS
    val tick = engine.renderTick

    val canvasW = size.width
    val canvasH = size.height
    val level = engine.level
    val levelW = level.tileCols * level.tileSize
    val levelH = level.tileRows * level.tileSize

    // Screen shake offset
    val shakeX = if (engine.screenShake > 0) (-engine.screenShake + Math.random().toFloat() * engine.screenShake * 2) else 0f
    val shakeY = if (engine.screenShake > 0) (-engine.screenShake + Math.random().toFloat() * engine.screenShake * 2) else 0f

    // Camera following player
    val targetCamX = (engine.playerX + engine.playerWidth / 2f - canvasW / 2f)
      .coerceIn(0f, (levelW - canvasW).coerceAtLeast(0f))
    val targetCamY = (engine.playerY + engine.playerHeight / 2f - canvasH / 2f)
      .coerceIn(0f, (levelH - canvasH).coerceAtLeast(0f))

    val camX = targetCamX + shakeX
    val camY = targetCamY + shakeY

    // 1. Draw Parallax Background (Sky, Mountains, Sun/Moon, Stars)
    drawSkyAndParallax(engine, canvasW, canvasH, camX, camY)

    // Translate world coordinate system
    translate(left = -camX, top = -camY) {
      // 2. Draw Level Tilemap
      drawTiles(engine, canvasW, canvasH, camX, camY)

      // 3. Draw Checkpoints
      drawCheckpoints(engine)

      // 4. Draw Moving Platforms
      drawMovingPlatforms(engine)

      // 5. Draw Star Dust & Artifacts
      drawCollectibles(engine)

      // 6. Draw Exit Portal
      drawPortal(engine.level.portal, engine.artifactsCollected, engine.totalArtifacts, engine.animTimer)

      // 7. Draw Ghost Trails
      for (ghost in engine.ghostTrails) {
        drawPlayerSprite(
          x = ghost.x,
          y = ghost.y,
          facingRight = ghost.facingRight,
          animState = PlayerAnimState.DASH,
          animTime = 0f,
          alpha = ghost.alpha * 0.5f,
          tint = Color(0xFFA855F7)
        )
      }

      // 8. Draw Player
      val isInvulnerable = engine.invulnerabilityTimer > 0f
      val flashVisible = !isInvulnerable || ((engine.animTimer * 18).toInt() % 2 == 0)
      if (flashVisible) {
        drawPlayerSprite(
          x = engine.playerX,
          y = engine.playerY,
          facingRight = engine.facingRight,
          animState = engine.animState,
          animTime = engine.animTimer,
          hasBarrier = engine.hasBarrierShield
        )
      }

      // Radar directional indicator toward nearest artifact
      if (engine.hasRadar && engine.radarAngle != null) {
        val angle = engine.radarAngle!!
        val pCenterX = engine.playerX + engine.playerWidth / 2f
        val pCenterY = engine.playerY + engine.playerHeight / 2f
        val radarRadius = 26f
        val pointerX = pCenterX + cos(angle) * radarRadius
        val pointerY = pCenterY + sin(angle) * radarRadius

        drawCircle(
          color = Color(0x4400FFFF),
          radius = radarRadius,
          center = Offset(pCenterX, pCenterY),
          style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f)
        )

        drawCircle(
          color = Color(0xFF00FFFF),
          radius = 3.5f,
          center = Offset(pointerX, pointerY)
        )
      }

      // 9. Draw In-Game Particles
      for (p in engine.particles) {
        val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
        drawCircle(
          color = Color(p.color).copy(alpha = alpha),
          radius = p.size * alpha,
          center = Offset(p.x, p.y)
        )
      }

      // 10. Dynamic Night Darkness and Lighting Cutouts
      if (engine.ambientDarkness > 0.05f) {
        drawNightLighting(engine, canvasW, canvasH, camX, camY)
      }
    }

    // Chrono shift blue edge vignette
    if (engine.isChronoActive) {
      drawRect(
        brush = Brush.radialGradient(
          colors = listOf(Color.Transparent, Color(0x3300FFFF)),
          center = Offset(canvasW / 2f, canvasH / 2f),
          radius = canvasW * 0.7f
        ),
        size = Size(canvasW, canvasH)
      )
    }
  }
}

private fun DrawScope.drawSkyAndParallax(
  engine: GameEngine,
  canvasW: Float,
  canvasH: Float,
  camX: Float,
  camY: Float
) {
  // Sky Gradient depending on dayPhase
  val skyColors = when (engine.dayPhase) {
    DayPhase.DAWN -> listOf(
      Color(0xFF2E1065), // Deep twilight
      Color(0xFF831843), // Rose
      Color(0xFFFB923C), // Morning gold
      Color(0xFFFDE68A)  // Soft yellow
    )
    DayPhase.DAY -> listOf(
      Color(0xFF0284C7), // Sky blue
      Color(0xFF38BDF8), // Cyan
      Color(0xFFBAE6FD)  // Light horizon
    )
    DayPhase.DUSK -> listOf(
      Color(0xFF1E1B4B), // Indigo
      Color(0xFF581C87), // Violet
      Color(0xFF9F1239), // Crimson
      Color(0xFFF97316)  // Sunset orange
    )
    DayPhase.NIGHT -> listOf(
      Color(0xFF030712), // Pitch midnight
      Color(0xFF0F172A), // Deep navy
      Color(0xFF1E293B)  // Horizon slate
    )
  }

  drawRect(
    brush = Brush.verticalGradient(skyColors),
    size = Size(canvasW, canvasH)
  )

  // Stars (fade in at dusk/night)
  val starsAlpha = when (engine.dayPhase) {
    DayPhase.NIGHT -> 0.95f
    DayPhase.DUSK -> 0.45f
    DayPhase.DAWN -> 0.35f
    else -> 0f
  }

  if (starsAlpha > 0f) {
    // Deterministic twinkling pixel stars
    for (i in 0 until 40) {
      val sx = (i * 137.5f) % canvasW
      val sy = (i * 73.1f) % (canvasH * 0.65f)
      val twinkle = (0.5f + 0.5f * sin(engine.animTimer * 2f + i)).coerceIn(0.2f, 1f)
      val starSize = if (i % 7 == 0) 3.5f else 2f
      drawCircle(
        color = Color.White.copy(alpha = starsAlpha * twinkle),
        radius = starSize,
        center = Offset(sx, sy)
      )
    }
  }

  // Sun / Moon path
  val celestialProgress = (engine.dayTime + 0.25f) % 1.0f // orbit angle
  val celestialAngle = celestialProgress * 2.0 * Math.PI
  val celestialCenterX = canvasW / 2f + (canvasW * 0.42f) * cos(celestialAngle).toFloat()
  val celestialCenterY = canvasH * 0.7f - (canvasH * 0.55f) * sin(celestialAngle).toFloat().coerceAtLeast(-0.2f)

  if (engine.dayPhase == DayPhase.DAY || engine.dayPhase == DayPhase.DAWN || engine.dayPhase == DayPhase.DUSK) {
    // Sun
    drawCircle(
      color = Color(0x33FBBF24),
      radius = 36f,
      center = Offset(celestialCenterX, celestialCenterY)
    )
    drawCircle(
      color = Color(0xFFFDE047),
      radius = 20f,
      center = Offset(celestialCenterX, celestialCenterY)
    )
  } else {
    // Glowing Moon
    val moonX = canvasW * 0.78f
    val moonY = canvasH * 0.22f
    drawCircle(
      color = Color(0x33E0E7FF),
      radius = 32f,
      center = Offset(moonX, moonY)
    )
    drawCircle(
      color = Color(0xFFE0E7FF),
      radius = 22f,
      center = Offset(moonX, moonY)
    )
    // Moon craters
    drawCircle(color = Color(0xFFCBD5E1), radius = 4f, center = Offset(moonX - 5f, moonY - 4f))
    drawCircle(color = Color(0xFFCBD5E1), radius = 6f, center = Offset(moonX + 6f, moonY + 4f))
  }

  // Distant Mountain Peaks (Parallax layer 1: 0.15x speed)
  val ridgeOffset1 = (camX * 0.15f) % 240f
  val mountainColor1 = when (engine.dayPhase) {
    DayPhase.DAY -> Color(0xFF475569)
    DayPhase.DAWN -> Color(0xFF6B21A8)
    DayPhase.DUSK -> Color(0xFF4C1D95)
    DayPhase.NIGHT -> Color(0xFF0F172A)
  }
  for (i in -1..((canvasW / 240f).toInt() + 2)) {
    val mx = i * 240f - ridgeOffset1
    val my = canvasH * 0.6f
    // Triangular mountain peak
    drawCircle(
      color = mountainColor1.copy(alpha = 0.5f),
      radius = 160f,
      center = Offset(mx + 120f, my + 140f)
    )
  }

  // Closer Mountain Peaks (Parallax layer 2: 0.35x speed)
  val ridgeOffset2 = (camX * 0.35f) % 180f
  val mountainColor2 = when (engine.dayPhase) {
    DayPhase.DAY -> Color(0xFF334155)
    DayPhase.DAWN -> Color(0xFF581C87)
    DayPhase.DUSK -> Color(0xFF3B0764)
    DayPhase.NIGHT -> Color(0xFF090D16)
  }
  for (i in -1..((canvasW / 180f).toInt() + 2)) {
    val mx = i * 180f - ridgeOffset2
    val my = canvasH * 0.72f
    drawCircle(
      color = mountainColor2.copy(alpha = 0.75f),
      radius = 120f,
      center = Offset(mx + 90f, my + 100f)
    )
  }
}

private fun DrawScope.drawTiles(
  engine: GameEngine,
  canvasW: Float,
  canvasH: Float,
  camX: Float,
  camY: Float
) {
  val level = engine.level
  val tileSize = level.tileSize

  val startCol = (camX / tileSize).toInt().coerceAtLeast(0)
  val endCol = ((camX + canvasW) / tileSize).toInt().coerceAtMost(level.tileCols - 1)
  val startRow = (camY / tileSize).toInt().coerceAtLeast(0)
  val endRow = ((camY + canvasH) / tileSize).toInt().coerceAtMost(level.tileRows - 1)

  for (r in startRow..endRow) {
    for (c in startCol..endCol) {
      val tileId = level.tiles[r][c]
      val type = TileType.fromId(tileId)
      if (type == TileType.AIR) continue

      val tx = c * tileSize
      val ty = r * tileSize

      if (tileId >= 50) {
        val customBlock = level.customBlocks.find { it.id == tileId }
        val blockColor = if (customBlock != null) Color(customBlock.colorHex) else Color(0xFF6366F1)
        drawRect(color = blockColor, topLeft = Offset(tx, ty), size = Size(tileSize, tileSize))
        drawRect(color = Color.White.copy(alpha = 0.4f), topLeft = Offset(tx, ty), size = Size(tileSize, 3f))
        drawRect(color = Color.White.copy(alpha = 0.4f), topLeft = Offset(tx, ty), size = Size(3f, tileSize))
        drawRect(color = Color.Black.copy(alpha = 0.4f), topLeft = Offset(tx, ty + tileSize - 3f), size = Size(tileSize, 3f))
        drawRect(color = Color.Black.copy(alpha = 0.4f), topLeft = Offset(tx + tileSize - 3f, ty), size = Size(3f, tileSize))
        continue
      }

      when (type) {
        TileType.STONE -> drawStoneTile(tx, ty, tileSize)
        TileType.DIRT_GRASS -> drawGrassTile(tx, ty, tileSize)
        TileType.BRICK -> drawBrickTile(tx, ty, tileSize)
        TileType.WOOD_PLATFORM -> drawWoodPlatformTile(tx, ty, tileSize)
        TileType.SPIKE_UP -> drawSpikeTile(tx, ty, tileSize, isUp = true)
        TileType.SPIKE_DOWN -> drawSpikeTile(tx, ty, tileSize, isUp = false)
        TileType.CRUMBLING -> {
          val state = engine.crumblingStates[Pair(c, r)]
          if (state == null || state.state == CrumblingBlockState.BlockState.STABLE) {
            drawCrumblingTile(tx, ty, tileSize, shaking = false)
          } else if (state.state == CrumblingBlockState.BlockState.SHAKING) {
            val shake = if ((engine.animTimer * 20).toInt() % 2 == 0) 2f else -2f
            drawCrumblingTile(tx + shake, ty, tileSize, shaking = true)
          }
        }
        TileType.BOUNCE_PAD -> drawBouncePadTile(tx, ty, tileSize, engine.animTimer)
        TileType.CRYSTAL_WALL -> drawCrystalTile(tx, ty, tileSize)
        else -> {}
      }
    }
  }
}

private fun DrawScope.drawStoneTile(x: Float, y: Float, size: Float) {
  drawRect(color = Color(0xFF64748B), topLeft = Offset(x, y), size = Size(size, size))
  // Highlight top & left
  drawRect(color = Color(0xFF94A3B8), topLeft = Offset(x, y), size = Size(size, 3f))
  drawRect(color = Color(0xFF94A3B8), topLeft = Offset(x, y), size = Size(3f, size))
  // Shadow bottom & right
  drawRect(color = Color(0xFF334155), topLeft = Offset(x, y + size - 3f), size = Size(size, 3f))
  drawRect(color = Color(0xFF334155), topLeft = Offset(x + size - 3f, y), size = Size(3f, size))
  // Inner mortar lines
  drawRect(color = Color(0xFF475569), topLeft = Offset(x + 4f, y + size / 2f), size = Size(size - 8f, 2f))
  drawRect(color = Color(0xFF475569), topLeft = Offset(x + size / 2f, y + 4f), size = Size(2f, size / 2f - 4f))
}

private fun DrawScope.drawGrassTile(x: Float, y: Float, size: Float) {
  // Earthen base
  drawRect(color = Color(0xFF78350F), topLeft = Offset(x, y), size = Size(size, size))
  drawRect(color = Color(0xFF451A03), topLeft = Offset(x, y + size - 3f), size = Size(size, 3f))
  // Green grass top
  drawRect(color = Color(0xFF15803D), topLeft = Offset(x, y), size = Size(size, 6f))
  drawRect(color = Color(0xFF22C55E), topLeft = Offset(x, y), size = Size(size, 3f))
  // Pixel grass blades drooping
  drawRect(color = Color(0xFF15803D), topLeft = Offset(x + 4f, y + 6f), size = Size(3f, 4f))
  drawRect(color = Color(0xFF15803D), topLeft = Offset(x + 14f, y + 6f), size = Size(4f, 3f))
  drawRect(color = Color(0xFF15803D), topLeft = Offset(x + 24f, y + 6f), size = Size(3f, 5f))
}

private fun DrawScope.drawBrickTile(x: Float, y: Float, size: Float) {
  drawRect(color = Color(0xFF475569), topLeft = Offset(x, y), size = Size(size, size))
  // Ancient mossy brick pattern
  drawRect(color = Color(0xFF1E293B), topLeft = Offset(x, y), size = Size(size, size), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
  drawRect(color = Color(0xFF10B981).copy(alpha = 0.35f), topLeft = Offset(x + 4f, y + 4f), size = Size(6f, 6f))
  drawRect(color = Color(0xFF1E293B), topLeft = Offset(x, y + size / 2f), size = Size(size, 2f))
}

private fun DrawScope.drawWoodPlatformTile(x: Float, y: Float, size: Float) {
  // One-way wooden plank (thickness 8px)
  drawRect(color = Color(0xFFB45309), topLeft = Offset(x, y), size = Size(size, 8f))
  drawRect(color = Color(0xFFD97706), topLeft = Offset(x, y), size = Size(size, 3f))
  drawRect(color = Color(0xFF78350F), topLeft = Offset(x, y + 6f), size = Size(size, 2f))
  // Rivets / wood grain
  drawCircle(color = Color(0xFF451A03), radius = 1.5f, center = Offset(x + 5f, y + 4f))
  drawCircle(color = Color(0xFF451A03), radius = 1.5f, center = Offset(x + size - 5f, y + 4f))
}

private fun DrawScope.drawSpikeTile(x: Float, y: Float, size: Float, isUp: Boolean) {
  val spikeCount = 3
  val singleWidth = size / spikeCount
  for (i in 0 until spikeCount) {
    val sx = x + i * singleWidth
    val path = androidx.compose.ui.graphics.Path()
    if (isUp) {
      path.moveTo(sx, y + size)
      path.lineTo(sx + singleWidth / 2f, y)
      path.lineTo(sx + singleWidth, y + size)
      path.close()
    } else {
      path.moveTo(sx, y)
      path.lineTo(sx + singleWidth / 2f, y + size)
      path.lineTo(sx + singleWidth, y)
      path.close()
    }
    drawPath(path = path, color = Color(0xFFEF4444))
    // Metallic gleam
    drawCircle(
      color = Color(0xFFFCA5A5),
      radius = 1.5f,
      center = Offset(sx + singleWidth / 2f, if (isUp) y + 3f else y + size - 3f)
    )
  }
}

private fun DrawScope.drawCrumblingTile(x: Float, y: Float, size: Float, shaking: Boolean) {
  drawRect(
    color = if (shaking) Color(0xFFA8A29E) else Color(0xFF78716C),
    topLeft = Offset(x, y),
    size = Size(size, size)
  )
  // Cracks
  val crackColor = Color(0xFF292524)
  drawLine(color = crackColor, start = Offset(x + 4f, y + 2f), end = Offset(x + 16f, y + 18f), strokeWidth = 2f)
  drawLine(color = crackColor, start = Offset(x + 16f, y + 18f), end = Offset(x + 28f, y + 8f), strokeWidth = 2f)
  drawLine(color = crackColor, start = Offset(x + 16f, y + 18f), end = Offset(x + 20f, y + size - 2f), strokeWidth = 2f)
}

private fun DrawScope.drawBouncePadTile(x: Float, y: Float, size: Float, animTime: Float) {
  // Stem
  drawRect(color = Color(0xFFF1F5F9), topLeft = Offset(x + 8f, y + 12f), size = Size(size - 16f, size - 12f))
  // Mushroom cap (bouncing bulb)
  val pulse = 1f + 0.08f * sin(animTime * 6f)
  scale(scaleX = pulse, scaleY = 1f, pivot = Offset(x + size / 2f, y + 8f)) {
    drawArc(
      color = Color(0xFFEC4899),
      startAngle = 180f,
      sweepAngle = 180f,
      useCenter = true,
      topLeft = Offset(x + 2f, y),
      size = Size(size - 4f, 18f)
    )
    // White polka dots
    drawCircle(color = Color.White, radius = 2.5f, center = Offset(x + size / 2f, y + 4f))
    drawCircle(color = Color.White, radius = 2f, center = Offset(x + 8f, y + 7f))
    drawCircle(color = Color.White, radius = 2f, center = Offset(x + size - 8f, y + 7f))
  }
}

private fun DrawScope.drawCrystalTile(x: Float, y: Float, size: Float) {
  drawRect(color = Color(0xFF1E1B4B), topLeft = Offset(x, y), size = Size(size, size))
  // Violet/cyan crystals embedded in stone
  drawRect(color = Color(0xFF8B5CF6), topLeft = Offset(x + 6f, y + 6f), size = Size(8f, 12f))
  drawRect(color = Color(0xFF06B6D4), topLeft = Offset(x + 18f, y + 14f), size = Size(10f, 10f))
  drawRect(color = Color.White.copy(alpha = 0.6f), topLeft = Offset(x + 8f, y + 7f), size = Size(2f, 4f))
}

private fun DrawScope.drawMovingPlatforms(engine: GameEngine) {
  for (plat in engine.level.movingPlatforms) {
    val px = plat.currentX
    val py = plat.currentY
    // Tech/floating stone platform
    drawRect(color = Color(0xFF334155), topLeft = Offset(px, py), size = Size(plat.width, plat.height))
    drawRect(color = Color(0xFF64748B), topLeft = Offset(px, py), size = Size(plat.width, 3f))
    // Glowing cyan power core
    drawRect(
      color = Color(0xFF00FFFF),
      topLeft = Offset(px + plat.width * 0.25f, py + 5f),
      size = Size(plat.width * 0.5f, 4f)
    )
    // Thruster glow underneath
    val thrusterPulse = 3f + 2f * sin(engine.animTimer * 12f)
    drawRect(
      color = Color(0x8800FFFF),
      topLeft = Offset(px + plat.width * 0.35f, py + plat.height),
      size = Size(plat.width * 0.3f, thrusterPulse)
    )
  }
}

private fun DrawScope.drawCollectibles(engine: GameEngine) {
  val animTime = engine.animTimer

  // 1. Star Dust
  for (dust in engine.level.starDustList) {
    if (!dust.collected) {
      val bob = sin(animTime * 4f + dust.id) * 3f
      val cx = dust.x
      val cy = dust.y + bob

      // 4-pointed golden star
      drawCircle(color = Color(0x66FBBF24), radius = 8f, center = Offset(cx, cy))
      drawCircle(color = Color(0xFFFBBF24), radius = 5f, center = Offset(cx, cy))
      drawCircle(color = Color.White, radius = 2.5f, center = Offset(cx, cy))
    }
  }

  // 2. Ancient Relic Artifacts
  for (art in engine.level.artifacts) {
    if (!art.collected) {
      val bob = sin(animTime * 3.5f + art.bobPhase) * 5f
      val cx = art.x
      val cy = art.y + bob

      // Glowing pulsating halo
      val pulse = 14f + 3f * sin(animTime * 5f + art.id)
      drawCircle(color = Color(0x4400FFFF), radius = pulse, center = Offset(cx, cy))

      // Ancient diamond crystal artifact
      rotate(degrees = (animTime * 45f + art.id * 30f) % 360f, pivot = Offset(cx, cy)) {
        val path = androidx.compose.ui.graphics.Path().apply {
          moveTo(cx, cy - 11f)
          lineTo(cx + 8f, cy)
          lineTo(cx, cy + 11f)
          lineTo(cx - 8f, cy)
          close()
        }
        drawPath(path = path, color = Color(0xFF06B6D4))
        // Crystal facets
        drawPath(
          path = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx, cy - 11f)
            lineTo(cx, cy + 11f)
            lineTo(cx - 8f, cy)
            close()
          },
          color = Color(0xFF38BDF8)
        )
        // Core gleam
        drawCircle(color = Color.White, radius = 3f, center = Offset(cx, cy))
      }
    }
  }
}

private fun DrawScope.drawCheckpoints(engine: GameEngine) {
  val animTime = engine.animTimer
  for (cp in engine.level.checkpoints) {
    val cx = cp.x
    val cy = cp.y

    // Obelisk pillar
    drawRect(color = Color(0xFF475569), topLeft = Offset(cx - 6f, cy - 18f), size = Size(12f, 22f))
    drawRect(color = Color(0xFF64748B), topLeft = Offset(cx - 8f, cy + 4f), size = Size(16f, 4f))

    if (cp.active) {
      // Blazing emerald holy flame
      val flamePulse = 7f + 2f * sin(animTime * 8f)
      drawCircle(color = Color(0x6622C55E), radius = 14f, center = Offset(cx, cy - 24f))
      drawCircle(color = Color(0xFF22C55E), radius = flamePulse, center = Offset(cx, cy - 24f))
      drawCircle(color = Color(0xFF86EFAC), radius = flamePulse * 0.5f, center = Offset(cx, cy - 24f))
    } else {
      // Unlit crystal tip
      drawCircle(color = Color(0xFF94A3B8), radius = 4f, center = Offset(cx, cy - 22f))
    }
  }
}

private fun DrawScope.drawPortal(
  portal: Portal,
  collectedArtifacts: Int,
  totalArtifacts: Int,
  animTime: Float
) {
  val cx = portal.x
  val cy = portal.y

  if (portal.isUnlocked) {
    // Unlocked: Brilliant swirling mystic rift!
    // Outer aura
    val outerPulse = 32f + 4f * sin(animTime * 6f)
    drawCircle(color = Color(0x44A855F7), radius = outerPulse, center = Offset(cx, cy))
    drawCircle(color = Color(0x4400FFFF), radius = outerPulse * 0.75f, center = Offset(cx, cy))

    // Rotating cosmic vortex rings
    rotate(degrees = (animTime * 120f) % 360f, pivot = Offset(cx, cy)) {
      drawCircle(
        color = Color(0xFFA855F7),
        radius = 24f,
        center = Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
      )
    }
    rotate(degrees = (-animTime * 180f) % 360f, pivot = Offset(cx, cy)) {
      drawCircle(
        color = Color(0xFF00FFFF),
        radius = 16f,
        center = Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
      )
    }

    // Portal singularity center
    drawCircle(color = Color.White, radius = 8f, center = Offset(cx, cy))
  } else {
    // Locked / Dormant stone ring
    drawCircle(
      color = Color(0xFF334155),
      radius = 22f,
      center = Offset(cx, cy),
      style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f)
    )
    // Swirling dark mist inside
    drawCircle(color = Color(0x660F172A), radius = 17f, center = Offset(cx, cy))

    // Locked runic glyph
    val lockColor = Color(0xFF94A3B8)
    drawRect(color = lockColor, topLeft = Offset(cx - 5f, cy - 2f), size = Size(10f, 9f))
    drawArc(
      color = lockColor,
      startAngle = 180f,
      sweepAngle = 180f,
      useCenter = false,
      topLeft = Offset(cx - 4f, cy - 8f),
      size = Size(8f, 8f),
      style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
    )
  }
}

private fun DrawScope.drawPlayerSprite(
  x: Float,
  y: Float,
  facingRight: Boolean,
  animState: PlayerAnimState,
  animTime: Float,
  alpha: Float = 1.0f,
  tint: Color? = null,
  hasBarrier: Boolean = false
) {
  val width = 20f
  val height = 28f
  val cx = x + width / 2f
  val cy = y + height / 2f

  // Protective barrier shield bubble
  if (hasBarrier && alpha >= 0.9f) {
    val bubblePulse = 20f + 2f * sin(animTime * 6f)
    drawCircle(color = Color(0x3338BDF8), radius = bubblePulse, center = Offset(cx, cy))
    drawCircle(
      color = Color(0xFF38BDF8),
      radius = bubblePulse,
      center = Offset(cx, cy),
      style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
    )
  }

  scale(scaleX = if (facingRight) 1f else -1f, scaleY = 1f, pivot = Offset(cx, cy)) {
    // Colors
    val cloakColor = tint ?: Color(0xFF3B82F6) // Royal blue cloak
    val cloakDark = tint ?: Color(0xFF1D4ED8)
    val skinColor = tint ?: Color(0xFFFDBA74) // Warm peach
    val hairColor = tint ?: Color(0xFF78350F) // Brown hair/hood
    val bootColor = tint ?: Color(0xFF1E293B) // Dark boots
    val eyeColor = tint ?: Color(0xFF0F172A)
    val gemColor = tint ?: Color(0xFF00FFFF) // Glowing pendant

    val isRunning = animState == PlayerAnimState.RUN
    val runFrame = if (isRunning) ((animTime * 10f).toInt() % 4) else 0

    // Bobbing
    val bob = if (isRunning && (runFrame == 1 || runFrame == 3)) -2f else 0f
    val py = y + bob

    // 1. Billowing Cloak
    val cloakOffset = when (animState) {
      PlayerAnimState.JUMP -> -3f
      PlayerAnimState.FALL -> 3f
      PlayerAnimState.DASH -> -6f
      else -> 0f
    }
    drawRect(
      color = cloakDark.copy(alpha = alpha),
      topLeft = Offset(x + 2f + cloakOffset, py + 10f),
      size = Size(14f, 13f)
    )

    // 2. Head & Hood
    drawRect(color = hairColor.copy(alpha = alpha), topLeft = Offset(x + 4f, py), size = Size(12f, 10f))
    drawRect(color = skinColor.copy(alpha = alpha), topLeft = Offset(x + 7f, py + 3f), size = Size(8f, 6f))
    // Pixel Eye
    drawRect(color = eyeColor.copy(alpha = alpha), topLeft = Offset(x + 12f, py + 4f), size = Size(2f, 2f))

    // 3. Body Tunic & Glowing Amulet
    drawRect(color = cloakColor.copy(alpha = alpha), topLeft = Offset(x + 4f, py + 10f), size = Size(12f, 10f))
    drawCircle(color = gemColor.copy(alpha = alpha), radius = 2f, center = Offset(x + 11f, py + 13f))

    // 4. Legs / Boots animation
    when (animState) {
      PlayerAnimState.RUN -> {
        when (runFrame) {
          0 -> {
            drawRect(color = bootColor.copy(alpha = alpha), topLeft = Offset(x + 4f, py + 20f), size = Size(5f, 8f))
            drawRect(color = bootColor.copy(alpha = alpha), topLeft = Offset(x + 11f, py + 20f), size = Size(5f, 8f))
          }
          1 -> {
            drawRect(color = bootColor.copy(alpha = alpha), topLeft = Offset(x + 2f, py + 19f), size = Size(5f, 7f))
            drawRect(color = bootColor.copy(alpha = alpha), topLeft = Offset(x + 13f, py + 21f), size = Size(5f, 7f))
          }
          2 -> {
            drawRect(color = bootColor.copy(alpha = alpha), topLeft = Offset(x + 4f, py + 20f), size = Size(5f, 8f))
            drawRect(color = bootColor.copy(alpha = alpha), topLeft = Offset(x + 11f, py + 20f), size = Size(5f, 8f))
          }
          3 -> {
            drawRect(color = bootColor.copy(alpha = alpha), topLeft = Offset(x + 13f, py + 19f), size = Size(5f, 7f))
            drawRect(color = bootColor.copy(alpha = alpha), topLeft = Offset(x + 2f, py + 21f), size = Size(5f, 7f))
          }
        }
      }
      PlayerAnimState.JUMP -> {
        // Tucked boots
        drawRect(color = bootColor.copy(alpha = alpha), topLeft = Offset(x + 5f, py + 18f), size = Size(4f, 7f))
        drawRect(color = bootColor.copy(alpha = alpha), topLeft = Offset(x + 11f, py + 19f), size = Size(4f, 6f))
      }
      PlayerAnimState.FALL -> {
        // Extended boots
        drawRect(color = bootColor.copy(alpha = alpha), topLeft = Offset(x + 4f, py + 20f), size = Size(5f, 8f))
        drawRect(color = bootColor.copy(alpha = alpha), topLeft = Offset(x + 11f, py + 20f), size = Size(5f, 8f))
      }
      PlayerAnimState.WALL_SLIDE -> {
        drawRect(color = bootColor.copy(alpha = alpha), topLeft = Offset(x + 2f, py + 20f), size = Size(5f, 8f))
        drawRect(color = bootColor.copy(alpha = alpha), topLeft = Offset(x + 9f, py + 18f), size = Size(5f, 6f))
      }
      else -> {
        // Idle
        drawRect(color = bootColor.copy(alpha = alpha), topLeft = Offset(x + 4f, py + 20f), size = Size(5f, 8f))
        drawRect(color = bootColor.copy(alpha = alpha), topLeft = Offset(x + 11f, py + 20f), size = Size(5f, 8f))
      }
    }
  }
}

private fun DrawScope.drawNightLighting(
  engine: GameEngine,
  canvasW: Float,
  canvasH: Float,
  camX: Float,
  camY: Float
) {
  val darkAlpha = engine.ambientDarkness.coerceIn(0f, 0.72f)

  // Darkness overlay across view
  drawRect(
    color = Color(0xFF030712).copy(alpha = darkAlpha),
    topLeft = Offset(camX, camY),
    size = Size(canvasW, canvasH)
  )

  // Dynamic light cutouts (additive glow circles in night):
  // 1. Lantern around player
  val px = engine.playerX + engine.playerWidth / 2f
  val py = engine.playerY + engine.playerHeight / 2f
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(Color(0x88FFD54F), Color(0x33FFB300), Color.Transparent),
      center = Offset(px, py),
      radius = 110f
    ),
    radius = 110f,
    center = Offset(px, py)
  )

  // 2. Light around active checkpoints
  for (cp in engine.level.checkpoints) {
    if (cp.active) {
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(Color(0x8822C55E), Color.Transparent),
          center = Offset(cp.x, cp.y - 20f),
          radius = 80f
        ),
        radius = 80f,
        center = Offset(cp.x, cp.y - 20f)
      )
    }
  }

  // 3. Light around uncollected artifacts
  for (art in engine.level.artifacts) {
    if (!art.collected) {
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(Color(0x9900FFFF), Color.Transparent),
          center = Offset(art.x, art.y),
          radius = 75f
        ),
        radius = 75f,
        center = Offset(art.x, art.y)
      )
    }
  }

  // 4. Light around Portal
  val portalRadius = if (engine.level.portal.isUnlocked) 140f else 60f
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(Color(0x88A855F7), Color(0x3300FFFF), Color.Transparent),
      center = Offset(engine.level.portal.x, engine.level.portal.y),
      radius = portalRadius
    ),
    radius = portalRadius,
    center = Offset(engine.level.portal.x, engine.level.portal.y)
  )

  // 5. Fireflies hovering in the dark
  for (ff in engine.fireflies) {
    val glow = (ff.baseAlpha * (0.6f + 0.4f * sin(ff.phase))).coerceIn(0.1f, 1f)
    drawCircle(
      color = Color(0xFFFDE047).copy(alpha = glow),
      radius = 3f,
      center = Offset(ff.x, ff.y)
    )
    drawCircle(
      color = Color(0x44FDE047).copy(alpha = glow * 0.5f),
      radius = 7f,
      center = Offset(ff.x, ff.y)
    )
  }
}
