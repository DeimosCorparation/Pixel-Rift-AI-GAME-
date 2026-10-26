package com.example.data.models

import com.example.data.models.Artifact
import com.example.data.models.LevelDef
import com.example.data.models.LevelTheme
import com.example.data.models.Portal
import com.example.data.models.StarDustPickup

data class CustomLevelData(
  val id: Int,
  val title: String,
  val subtitle: String,
  val tileCols: Int,
  val tileRows: Int,
  val tiles: List<Int>, // flattened 2D grid
  val spawnX: Float,
  val spawnY: Float,
  val portalX: Float,
  val portalY: Float,
  val artifacts: List<CustomArtifactData>,
  val starDustList: List<CustomStarDustData>
)

data class CustomArtifactData(val id: Int, val x: Float, val y: Float)
data class CustomStarDustData(val id: Int, val x: Float, val y: Float, val value: Int)

fun CustomLevelData.toLevelDef(): LevelDef {
  val tileSize = 32f
  val grid = Array(tileRows) { r ->
    IntArray(tileCols) { c ->
      val idx = r * tileCols + c
      if (idx in tiles.indices) tiles[idx] else 0
    }
  }

  val artList = artifacts.map { Artifact(it.id, it.x, it.y) }
  val dustList = starDustList.map { StarDustPickup(it.id, it.x, it.y, it.value) }
  val portalObj = Portal(portalX, portalY, requiredArtifacts = artList.size.coerceAtLeast(1))

  return LevelDef(
    id = 1000 + id,
    titleRu = title,
    subtitleRu = subtitle,
    theme = LevelTheme.ANCIENT_RUINS,
    tileCols = tileCols,
    tileRows = tileRows,
    tileSize = tileSize,
    spawnX = spawnX,
    spawnY = spawnY,
    portal = portalObj,
    tiles = grid,
    artifacts = artList,
    starDustList = dustList,
    movingPlatforms = emptyList(),
    checkpoints = emptyList()
  )
}
