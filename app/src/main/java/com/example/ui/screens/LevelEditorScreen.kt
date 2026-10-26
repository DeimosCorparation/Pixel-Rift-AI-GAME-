package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.CustomLevelStorage
import com.example.data.GamePreferences
import com.example.data.models.CustomArtifactData
import com.example.data.models.CustomBlockDefinition
import com.example.data.models.CustomLevelData
import com.example.data.models.CustomStarDustData
import com.example.data.models.TileType
import com.example.engine.RetroAudioEngine

enum class EditTool {
  PAN, AIR, STONE, GRASS, BRICK, WOOD, SPIKE, CRUMBLING, BOUNCE, CRYSTAL, SPAWN, PORTAL, ARTIFACT, STARDUST, CUSTOM
}

@Composable
fun LevelEditorScreen(
  levelId: Int?, // null if creating new
  storage: CustomLevelStorage,
  prefs: GamePreferences,
  audio: RetroAudioEngine,
  onTestPlay: (CustomLevelData) -> Unit,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler { onBackClick() }

  val existingLevels = storage.getCustomLevels()
  val targetLevel = existingLevels.find { it.id == levelId } ?: CustomLevelData(
    id = (existingLevels.maxOfOrNull { it.id } ?: 0) + 1,
    title = "Мой Уровень #${existingLevels.size + 1}",
    subtitle = "Создано в редакторе",
    tileCols = 60,
    tileRows = 18,
    tiles = IntArray(60 * 18) { r -> if (r >= 15) 2 else 0 }.toList(),
    spawnX = 2f * 32f,
    spawnY = 13f * 32f,
    portalX = 54f * 32f,
    portalY = 13f * 32f,
    artifacts = listOf(CustomArtifactData(1, 25f * 32f, 13f * 32f)),
    starDustList = listOf(CustomStarDustData(1, 12f * 32f, 13f * 32f, 10)),
    customBlocks = emptyList()
  )

  var title by remember { mutableStateOf(targetLevel.title) }
  var subtitle by remember { mutableStateOf(targetLevel.subtitle) }
  var cols by remember { mutableIntStateOf(targetLevel.tileCols) }
  var rows by remember { mutableIntStateOf(targetLevel.tileRows) }
  var tiles by remember { mutableStateOf(targetLevel.tiles) }
  var spawnX by remember { mutableStateOf(targetLevel.spawnX) }
  var spawnY by remember { mutableStateOf(targetLevel.spawnY) }
  var portalX by remember { mutableStateOf(targetLevel.portalX) }
  var portalY by remember { mutableStateOf(targetLevel.portalY) }
  var artifacts by remember { mutableStateOf(targetLevel.artifacts) }
  var starDustList by remember { mutableStateOf(targetLevel.starDustList) }
  var customBlocks by remember { mutableStateOf(targetLevel.customBlocks) }

  var activeTool by remember { mutableStateOf(EditTool.STONE) }
  var activeCustomBlock by remember { mutableStateOf<CustomBlockDefinition?>(customBlocks.firstOrNull()) }
  var showResizeDialog by remember { mutableStateOf(false) }
  var showCustomBlockDialog by remember { mutableStateOf(false) }

  // Canvas pan offset
  var offsetX by remember { mutableFloatStateOf(0f) }
  var offsetY by remember { mutableFloatStateOf(0f) }

  fun applyTileAt(c: Int, r: Int) {
    val idx = r * cols + c
    if (idx !in tiles.indices) return

    val tileTypeVal = when (activeTool) {
      EditTool.AIR -> 0
      EditTool.STONE -> 1
      EditTool.GRASS -> 2
      EditTool.BRICK -> 3
      EditTool.WOOD -> 4
      EditTool.SPIKE -> 5
      EditTool.CRUMBLING -> 7
      EditTool.BOUNCE -> 8
      EditTool.CRYSTAL -> 9
      EditTool.CUSTOM -> activeCustomBlock?.id ?: 1
      else -> null
    }

    if (tileTypeVal != null) {
      if (tiles[idx] != tileTypeVal) {
        val updated = tiles.toMutableList()
        updated[idx] = tileTypeVal
        tiles = updated
      }
    } else {
      val wx = c * 32f
      val wy = r * 32f
      when (activeTool) {
        EditTool.SPAWN -> {
          spawnX = wx
          spawnY = wy
        }
        EditTool.PORTAL -> {
          portalX = wx
          portalY = wy
        }
        EditTool.ARTIFACT -> {
          val existing = artifacts.find { it.x == wx && it.y == wy }
          if (existing != null) {
            artifacts = artifacts.filter { it != existing }
          } else {
            artifacts = artifacts + CustomArtifactData(artifacts.size + 1, wx, wy)
          }
        }
        EditTool.STARDUST -> {
          val existing = starDustList.find { it.x == wx && it.y == wy }
          if (existing != null) {
            starDustList = starDustList.filter { it != existing }
          } else {
            starDustList = starDustList + CustomStarDustData(starDustList.size + 1, wx, wy, 10)
          }
        }
        else -> {}
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF030712))
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFF0F172A))
          .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = {
              audio.playButtonClick()
              onBackClick()
            },
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(Color(0xFF1E293B))
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
          }
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = "РЕДАКТОР УРОВНЕЙ",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 17.sp
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          // Resize Map Button (up to 400x400)
          Button(
            onClick = {
              audio.playButtonClick()
              showResizeDialog = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Icon(Icons.Default.AspectRatio, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("${cols}x${rows}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }

          // Save Button
          Button(
            onClick = {
              audio.playButtonClick()
              val levelData = CustomLevelData(
                id = targetLevel.id,
                title = title,
                subtitle = subtitle,
                tileCols = cols,
                tileRows = rows,
                tiles = tiles,
                spawnX = spawnX,
                spawnY = spawnY,
                portalX = portalX,
                portalY = portalY,
                artifacts = artifacts,
                starDustList = starDustList,
                customBlocks = customBlocks
              )
              val updatedList = existingLevels.filter { it.id != levelData.id } + levelData
              storage.saveCustomLevels(updatedList)
              onBackClick()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Сохранить", fontSize = 11.sp)
          }

          // Test Play Button
          Button(
            onClick = {
              audio.playButtonClick()
              val levelData = CustomLevelData(
                id = targetLevel.id,
                title = title,
                subtitle = subtitle,
                tileCols = cols,
                tileRows = rows,
                tiles = tiles,
                spawnX = spawnX,
                spawnY = spawnY,
                portalX = portalX,
                portalY = portalY,
                artifacts = artifacts,
                starDustList = starDustList,
                customBlocks = customBlocks
              )
              onTestPlay(levelData)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Тест", fontSize = 11.sp)
          }
        }
      }

      // Title & Subtitle Inputs
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFF090D16))
          .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Название уровня", fontSize = 11.sp) },
          modifier = Modifier.weight(1f),
          singleLine = true
        )
        OutlinedTextField(
          value = subtitle,
          onValueChange = { subtitle = it },
          label = { Text("Описание", fontSize = 11.sp) },
          modifier = Modifier.weight(1f),
          singleLine = true
        )
      }

      // Toolbox Palette
      val scrollState = rememberScrollState()
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFF1E293B))
          .horizontalScroll(scrollState)
          .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        ToolButton("🖐 Рука", EditTool.PAN, activeTool) { activeTool = it }
        ToolButton("Ластик", EditTool.AIR, activeTool) { activeTool = it }
        ToolButton("Камень", EditTool.STONE, activeTool) { activeTool = it }
        ToolButton("Трава", EditTool.GRASS, activeTool) { activeTool = it }
        ToolButton("Кирпич", EditTool.BRICK, activeTool) { activeTool = it }
        ToolButton("Платформа", EditTool.WOOD, activeTool) { activeTool = it }
        ToolButton("Шипы", EditTool.SPIKE, activeTool) { activeTool = it }
        ToolButton("Рушащийся", EditTool.CRUMBLING, activeTool) { activeTool = it }
        ToolButton("Батут", EditTool.BOUNCE, activeTool) { activeTool = it }
        ToolButton("Кристалл", EditTool.CRYSTAL, activeTool) { activeTool = it }
        ToolButton("Старт", EditTool.SPAWN, activeTool) { activeTool = it }
        ToolButton("Портал", EditTool.PORTAL, activeTool) { activeTool = it }
        ToolButton("Реликвия", EditTool.ARTIFACT, activeTool) { activeTool = it }
        ToolButton("Пыль", EditTool.STARDUST, activeTool) { activeTool = it }

        // Render created custom blocks in toolbox
        customBlocks.forEach { cb ->
          val isSel = activeTool == EditTool.CUSTOM && activeCustomBlock?.id == cb.id
          Button(
            onClick = {
              activeTool = EditTool.CUSTOM
              activeCustomBlock = cb
              audio.playButtonClick()
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isSel) Color(0xFF0284C7) else Color(cb.colorHex)
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.height(36.dp)
          ) {
            Text(text = "✦ ${cb.name}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
          }
        }

        // Add custom block button
        Button(
          onClick = {
            audio.playButtonClick()
            showCustomBlockDialog = true
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          modifier = Modifier.height(36.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(2.dp))
          Text(text = "Блок+", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
      }

      // Editor Grid Canvas with Drag and Tap Support
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .background(Color(0xFF0F172A))
          .pointerInput(activeTool, activeCustomBlock) {
            if (activeTool == EditTool.PAN) {
              detectDragGestures { _, dragAmount ->
                offsetX += dragAmount.x
                offsetY += dragAmount.y
              }
            } else {
              detectDragGestures(
                onDragStart = { offset ->
                  val worldX = (offset.x - offsetX) / density
                  val worldY = (offset.y - offsetY) / density
                  val c = (worldX / 32f).toInt()
                  val r = (worldY / 32f).toInt()
                  if (c in 0 until cols && r in 0 until rows) {
                    applyTileAt(c, r)
                  }
                }
              ) { change, _ ->
                val worldX = (change.position.x - offsetX) / density
                val worldY = (change.position.y - offsetY) / density
                val c = (worldX / 32f).toInt()
                val r = (worldY / 32f).toInt()
                if (c in 0 until cols && r in 0 until rows) {
                  applyTileAt(c, r)
                }
              }
            }
          }
          .pointerInput(activeTool, activeCustomBlock, cols, rows, tiles) {
            if (activeTool != EditTool.PAN) {
              detectTapGestures(
                onTap = { offset ->
                  val worldX = (offset.x - offsetX) / density
                  val worldY = (offset.y - offsetY) / density
                  val c = (worldX / 32f).toInt()
                  val r = (worldY / 32f).toInt()
                  if (c in 0 until cols && r in 0 until rows) {
                    applyTileAt(c, r)
                  }
                }
              )
            }
          }
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val ts = 32f * density

          for (r in 0 until rows) {
            for (c in 0 until cols) {
              val idx = r * cols + c
              if (idx !in tiles.indices) continue
              val tileId = tiles[idx]
              val x = c * ts + offsetX
              val y = r * ts + offsetY

              // Viewport culling for performance up to 400x400 blocks!
              if (x + ts >= 0 && x <= size.width && y + ts >= 0 && y <= size.height) {
                if (tileId > 0) {
                  val type = TileType.fromId(tileId)
                  if (tileId >= 50) {
                    val cb = customBlocks.find { it.id == tileId }
                    val blockColor = if (cb != null) Color(cb.colorHex) else Color(0xFF6366F1)
                    drawRect(color = blockColor, topLeft = Offset(x, y), size = Size(ts, ts))
                    drawRect(color = Color.White.copy(alpha = 0.4f), topLeft = Offset(x, y), size = Size(ts, 3f))
                    drawRect(color = Color.White.copy(alpha = 0.4f), topLeft = Offset(x, y), size = Size(3f, ts))
                    drawRect(color = Color.Black.copy(alpha = 0.4f), topLeft = Offset(x, y + ts - 3f), size = Size(ts, 3f))
                    drawRect(color = Color.Black.copy(alpha = 0.4f), topLeft = Offset(x + ts - 3f, y), size = Size(3f, ts))
                  } else {
                    when (type) {
                      TileType.STONE -> drawStoneTile(x, y, ts)
                      TileType.DIRT_GRASS -> drawGrassTile(x, y, ts)
                      TileType.BRICK -> drawBrickTile(x, y, ts)
                      TileType.WOOD_PLATFORM -> drawWoodPlatformTile(x, y, ts)
                      TileType.SPIKE_UP -> drawSpikeTile(x, y, ts, isUp = true)
                      TileType.SPIKE_DOWN -> drawSpikeTile(x, y, ts, isUp = false)
                      TileType.CRUMBLING -> drawCrumblingTile(x, y, ts)
                      TileType.BOUNCE_PAD -> drawBouncePadTile(x, y, ts)
                      TileType.CRYSTAL_WALL -> drawCrystalTile(x, y, ts)
                      else -> {}
                    }
                  }
                  drawRect(
                    color = Color(0x15FFFFFF),
                    topLeft = Offset(x, y),
                    size = Size(ts, ts),
                    style = Stroke(1f)
                  )
                }
              }
            }
          }

          // Draw Map Outer Boundary
          drawRect(
            color = Color(0xFF6366F1),
            topLeft = Offset(offsetX, offsetY),
            size = Size(cols * ts, rows * ts),
            style = Stroke(2f)
          )

          // Draw Spawn Point marker
          val spX = (spawnX / 32f) * ts + offsetX
          val spY = (spawnY / 32f) * ts + offsetY
          if (spX + ts >= 0 && spX <= size.width && spY + ts >= 0 && spY <= size.height) {
            drawCircle(color = Color(0xFF22C55E), radius = ts * 0.4f, center = Offset(spX + ts / 2f, spY + ts / 2f))
          }

          // Draw Portal marker
          val pX = (portalX / 32f) * ts + offsetX
          val pY = (portalY / 32f) * ts + offsetY
          if (pX + ts >= 0 && pX <= size.width && pY + ts >= 0 && pY <= size.height) {
            drawCircle(color = Color(0xFFA855F7), radius = ts * 0.45f, center = Offset(pX + ts / 2f, pY + ts / 2f))
          }

          // Draw Artifacts
          for (art in artifacts) {
            val ax = (art.x / 32f) * ts + offsetX
            val ay = (art.y / 32f) * ts + offsetY
            if (ax + ts >= 0 && ax <= size.width && ay + ts >= 0 && ay <= size.height) {
              drawCircle(color = Color(0xFF00FFFF), radius = ts * 0.35f, center = Offset(ax + ts / 2f, ay + ts / 2f))
            }
          }

          // Draw Star Dust
          for (dust in starDustList) {
            val dx = (dust.x / 32f) * ts + offsetX
            val dy = (dust.y / 32f) * ts + offsetY
            if (dx + ts >= 0 && dx <= size.width && dy + ts >= 0 && dy <= size.height) {
              drawCircle(color = Color(0xFFFBBF24), radius = ts * 0.25f, center = Offset(dx + ts / 2f, dy + ts / 2f))
            }
          }
        }
      }
    }

    // Resize Dialog
    if (showResizeDialog) {
      MapResizeDialog(
        currentCols = cols,
        currentRows = rows,
        onApply = { newCols, newRows ->
          val newTiles = MutableList(newCols * newRows) { 0 }
          for (r in 0 until minOf(rows, newRows)) {
            for (c in 0 until minOf(cols, newCols)) {
              newTiles[r * newCols + c] = tiles[r * cols + c]
            }
          }
          cols = newCols
          rows = newRows
          tiles = newTiles

          val maxWx = (newCols - 1) * 32f
          val maxWy = (newRows - 1) * 32f
          spawnX = spawnX.coerceIn(0f, maxWx)
          spawnY = spawnY.coerceIn(0f, maxWy)
          portalX = portalX.coerceIn(0f, maxWx)
          portalY = portalY.coerceIn(0f, maxWy)
          artifacts = artifacts.filter { it.x <= maxWx && it.y <= maxWy }
          starDustList = starDustList.filter { it.x <= maxWx && it.y <= maxWy }

          audio.playButtonClick()
          showResizeDialog = false
        },
        onDismiss = { showResizeDialog = false }
      )
    }

    // Custom Block Creator Dialog
    if (showCustomBlockDialog) {
      CustomBlockCreatorDialog(
        onCreated = { newBlock ->
          val nextId = 50 + customBlocks.size
          val blockWithId = newBlock.copy(id = nextId)
          customBlocks = customBlocks + blockWithId
          activeCustomBlock = blockWithId
          activeTool = EditTool.CUSTOM
          audio.playButtonClick()
          showCustomBlockDialog = false
        },
        onDismiss = { showCustomBlockDialog = false }
      )
    }
  }
}

// Block Graphic Drawing Helpers for Level Editor
private fun DrawScope.drawStoneTile(x: Float, y: Float, size: Float) {
  drawRect(color = Color(0xFF64748B), topLeft = Offset(x, y), size = Size(size, size))
  drawRect(color = Color(0xFF94A3B8), topLeft = Offset(x, y), size = Size(size, 3f))
  drawRect(color = Color(0xFF94A3B8), topLeft = Offset(x, y), size = Size(3f, size))
  drawRect(color = Color(0xFF334155), topLeft = Offset(x, y + size - 3f), size = Size(size, 3f))
  drawRect(color = Color(0xFF334155), topLeft = Offset(x + size - 3f, y), size = Size(3f, size))
  drawRect(color = Color(0xFF475569), topLeft = Offset(x + 4f, y + size / 2f), size = Size(size - 8f, 2f))
  drawRect(color = Color(0xFF475569), topLeft = Offset(x + size / 2f, y + 4f), size = Size(2f, size / 2f - 4f))
}

private fun DrawScope.drawGrassTile(x: Float, y: Float, size: Float) {
  drawRect(color = Color(0xFF78350F), topLeft = Offset(x, y), size = Size(size, size))
  drawRect(color = Color(0xFF451A03), topLeft = Offset(x, y + size - 3f), size = Size(size, 3f))
  drawRect(color = Color(0xFF15803D), topLeft = Offset(x, y), size = Size(size, 6f))
  drawRect(color = Color(0xFF22C55E), topLeft = Offset(x, y), size = Size(size, 3f))
  drawRect(color = Color(0xFF15803D), topLeft = Offset(x + 4f, y + 6f), size = Size(3f, 4f))
  drawRect(color = Color(0xFF15803D), topLeft = Offset(x + 14f, y + 6f), size = Size(4f, 3f))
  drawRect(color = Color(0xFF15803D), topLeft = Offset(x + 24f, y + 6f), size = Size(3f, 5f))
}

private fun DrawScope.drawBrickTile(x: Float, y: Float, size: Float) {
  drawRect(color = Color(0xFF475569), topLeft = Offset(x, y), size = Size(size, size))
  drawRect(color = Color(0xFF1E293B), topLeft = Offset(x, y), size = Size(size, size), style = Stroke(2f))
  drawRect(color = Color(0xFF10B981).copy(alpha = 0.35f), topLeft = Offset(x + 4f, y + 4f), size = Size(6f, 6f))
  drawRect(color = Color(0xFF1E293B), topLeft = Offset(x, y + size / 2f), size = Size(size, 2f))
}

private fun DrawScope.drawWoodPlatformTile(x: Float, y: Float, size: Float) {
  drawRect(color = Color(0xFFB45309), topLeft = Offset(x, y), size = Size(size, 8f))
  drawRect(color = Color(0xFFD97706), topLeft = Offset(x, y), size = Size(size, 3f))
  drawRect(color = Color(0xFF78350F), topLeft = Offset(x, y + 6f), size = Size(size, 2f))
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
    drawCircle(
      color = Color(0xFFFCA5A5),
      radius = 1.5f,
      center = Offset(sx + singleWidth / 2f, if (isUp) y + 3f else y + size - 3f)
    )
  }
}

private fun DrawScope.drawCrumblingTile(x: Float, y: Float, size: Float) {
  drawRect(color = Color(0xFF78716C), topLeft = Offset(x, y), size = Size(size, size))
  val crackColor = Color(0xFF292524)
  drawLine(color = crackColor, start = Offset(x + 4f, y + 2f), end = Offset(x + 16f, y + 18f), strokeWidth = 2f)
  drawLine(color = crackColor, start = Offset(x + 16f, y + 18f), end = Offset(x + 28f, y + 8f), strokeWidth = 2f)
  drawLine(color = crackColor, start = Offset(x + 16f, y + 18f), end = Offset(x + 20f, y + size - 2f), strokeWidth = 2f)
}

private fun DrawScope.drawBouncePadTile(x: Float, y: Float, size: Float) {
  drawRect(color = Color(0xFFF1F5F9), topLeft = Offset(x + 8f, y + 12f), size = Size(size - 16f, size - 12f))
  drawArc(
    color = Color(0xFFEC4899),
    startAngle = 180f,
    sweepAngle = 180f,
    useCenter = true,
    topLeft = Offset(x + 2f, y),
    size = Size(size - 4f, 18f)
  )
  drawCircle(color = Color.White, radius = 2.5f, center = Offset(x + size / 2f, y + 4f))
  drawCircle(color = Color.White, radius = 2f, center = Offset(x + 8f, y + 7f))
  drawCircle(color = Color.White, radius = 2f, center = Offset(x + size - 8f, y + 7f))
}

private fun DrawScope.drawCrystalTile(x: Float, y: Float, size: Float) {
  drawRect(color = Color(0xFF1E1B4B), topLeft = Offset(x, y), size = Size(size, size))
  drawRect(color = Color(0xFF8B5CF6), topLeft = Offset(x + 6f, y + 6f), size = Size(8f, 12f))
  drawRect(color = Color(0xFF06B6D4), topLeft = Offset(x + 18f, y + 14f), size = Size(10f, 10f))
  drawRect(color = Color.White.copy(alpha = 0.6f), topLeft = Offset(x + 8f, y + 7f), size = Size(2f, 4f))
}

@Composable
private fun ToolButton(
  label: String,
  tool: EditTool,
  activeTool: EditTool,
  onClick: (EditTool) -> Unit
) {
  val isActive = tool == activeTool
  Button(
    onClick = { onClick(tool) },
    colors = ButtonDefaults.buttonColors(
      containerColor = if (isActive) Color(0xFF0284C7) else Color(0xFF334155)
    ),
    shape = RoundedCornerShape(8.dp),
    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    modifier = Modifier.height(36.dp)
  ) {
    Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
  }
}

@Composable
fun CustomBlockCreatorDialog(
  onCreated: (CustomBlockDefinition) -> Unit,
  onDismiss: () -> Unit
) {
  var nameText by remember { mutableStateOf("Мой Блок") }
  var selectedColor by remember { mutableStateOf(0xFF6366F1L) }
  var canDamage by remember { mutableStateOf(false) }
  var damageAmountText by remember { mutableStateOf("1") }
  var givesBoost by remember { mutableStateOf(false) }
  var boostType by remember { mutableStateOf("SPEED") } // SPEED, SHIELD, JUMP
  var boostValueText by remember { mutableStateOf("1.5") }

  val colors = listOf(
    0xFF6366F1L, // Indigo
    0xFFEC4899L, // Pink
    0xFF8B5CF6L, // Purple
    0xFF10B981L, // Emerald
    0xFFF59E0BL, // Amber
    0xFFEF4444L, // Red
    0xFF06B6D4L, // Cyan
    0xFF64748BL  // Slate
  )

  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier = Modifier
        .width(380.dp)
        .clip(RoundedCornerShape(20.dp))
        .background(
          Brush.verticalGradient(
            listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
          )
        )
        .border(1.5.dp, Color(0xFF6366F1), RoundedCornerShape(20.dp))
        .padding(20.dp)
    ) {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "СОЗДАНИЕ БЛОКА",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp
          )
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = Color(0xFF94A3B8))
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = nameText,
          onValueChange = { nameText = it },
          label = { Text("Название блока", fontSize = 11.sp) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Color selection palette
        Text(
          text = "Выбор цвета блока:",
          color = Color(0xFFA5B4FC),
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          colors.forEach { col ->
            val isSelected = selectedColor == col
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(col))
                .border(
                  width = if (isSelected) 3.dp else 1.dp,
                  color = if (isSelected) Color.White else Color.Transparent,
                  shape = CircleShape
                )
                .pointerInput(col) {
                  detectTapGestures(onTap = { selectedColor = col })
                }
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Can Damage Section
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Может дажить?", color = Color.White, fontSize = 13.sp)
          Switch(
            checked = canDamage,
            onCheckedChange = { canDamage = it }
          )
        }
        if (canDamage) {
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = damageAmountText,
            onValueChange = { damageAmountText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Сколько урона?", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Gives Boost Section
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Даёт буст?", color = Color.White, fontSize = 13.sp)
          Switch(
            checked = givesBoost,
            onCheckedChange = { givesBoost = it }
          )
        }
        if (givesBoost) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Какой именно?",
            color = Color(0xFFA5B4FC),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.Start)
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            val boostOptions = listOf("SPEED" to "Ускорение", "SHIELD" to "Щит", "JUMP" to "Прыжок")
            boostOptions.forEach { (typeKey, typeLabel) ->
              val isSel = boostType == typeKey
              OutlinedButton(
                onClick = { boostType = typeKey },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                  containerColor = if (isSel) Color(0xFF6366F1) else Color(0x33334155)
                ),
                modifier = Modifier.weight(1f).height(36.dp)
              ) {
                Text(typeLabel, fontSize = 10.sp, color = Color.White)
              }
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = boostValueText,
            onValueChange = { boostValueText = it },
            label = { Text(if (boostType == "SHIELD") "Сколько сердец/щита?" else "Множитель ускорения/прыжка?", fontSize = 10.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = {
            val dmg = damageAmountText.toIntOrNull() ?: 1
            val bVal = boostValueText.toFloatOrNull() ?: 1.5f
            onCreated(
              CustomBlockDefinition(
                id = 50,
                name = nameText.ifBlank { "Блок" },
                colorHex = selectedColor,
                canDamage = canDamage,
                damageAmount = dmg,
                givesBoost = givesBoost,
                boostType = boostType,
                boostValue = bVal
              )
            )
          },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
          modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
          Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("СОЗДАТЬ И ДОБАВИТЬ БЛОК", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      }
    }
  }
}

@Composable
fun MapResizeDialog(
  currentCols: Int,
  currentRows: Int,
  onApply: (Int, Int) -> Unit,
  onDismiss: () -> Unit
) {
  var widthText by remember { mutableStateOf(currentCols.toString()) }
  var heightText by remember { mutableStateOf(currentRows.toString()) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier = Modifier
        .width(360.dp)
        .clip(RoundedCornerShape(20.dp))
        .background(
          Brush.verticalGradient(
            listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
          )
        )
        .border(1.5.dp, Color(0xFF6366F1), RoundedCornerShape(20.dp))
        .padding(20.dp)
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "РАЗМЕР КАРТЫ",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp
          )
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = Color(0xFF94A3B8))
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = "Ширина и высота в блоках (от 10 до 400). Построенные блоки сохраняются.",
          color = Color(0xFF94A3B8),
          fontSize = 11.sp,
          lineHeight = 15.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Presets
        Text(
          text = "Быстрые шаблоны:",
          color = Color(0xFFA5B4FC),
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          val presets = listOf(
            Pair(60, 18) to "60x18",
            Pair(100, 30) to "100x30",
            Pair(200, 50) to "200x50",
            Pair(400, 400) to "400x400"
          )
          presets.forEach { (dim, label) ->
            OutlinedButton(
              onClick = {
                widthText = dim.first.toString()
                heightText = dim.second.toString()
                errorMsg = null
              },
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
              modifier = Modifier.weight(1f).height(32.dp)
            ) {
              Text(label, fontSize = 9.5.sp, color = Color.White)
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Width & Height text fields
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedTextField(
            value = widthText,
            onValueChange = {
              widthText = it.filter { ch -> ch.isDigit() }
              errorMsg = null
            },
            label = { Text("Ширина (10-400)", fontSize = 10.sp) },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          OutlinedTextField(
            value = heightText,
            onValueChange = {
              heightText = it.filter { ch -> ch.isDigit() }
              errorMsg = null
            },
            label = { Text("Высота (10-400)", fontSize = 10.sp) },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }

        if (errorMsg != null) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(text = errorMsg ?: "", color = Color(0xFFEF4444), fontSize = 11.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = {
            val w = widthText.toIntOrNull()
            val h = heightText.toIntOrNull()
            if (w == null || h == null || w !in 10..400 || h !in 10..400) {
              errorMsg = "Размеры должны быть от 10 до 400 блоков!"
            } else {
              onApply(w, h)
            }
          },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
          modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
          Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("ПРИМЕНИТЬ РАЗМЕР", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
      }
    }
  }
}
