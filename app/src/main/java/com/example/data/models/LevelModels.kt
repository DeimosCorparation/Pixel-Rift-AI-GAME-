package com.example.data.models

enum class TileType(val id: Int, val isSolid: Boolean, val isHazard: Boolean, val isPassableFromBelow: Boolean = false) {
  AIR(0, false, false),
  STONE(1, true, false),
  DIRT_GRASS(2, true, false),
  BRICK(3, true, false),
  WOOD_PLATFORM(4, true, false, isPassableFromBelow = true),
  SPIKE_UP(5, false, true),
  SPIKE_DOWN(6, false, true),
  CRUMBLING(7, true, false),
  BOUNCE_PAD(8, true, false),
  CRYSTAL_WALL(9, true, false);

  companion object {
    fun fromId(id: Int): TileType = entries.find { it.id == id } ?: AIR
  }
}

data class Artifact(
  val id: Int,
  var x: Float, // in world pixel coordinates
  var y: Float,
  var collected: Boolean = false,
  var bobPhase: Float = (id * 1.5f) % 6.28f
)

data class StarDustPickup(
  val id: Int,
  var x: Float,
  var y: Float,
  val value: Int = 5,
  var collected: Boolean = false
)

data class MovingPlatform(
  val id: Int,
  val startX: Float,
  val startY: Float,
  val endX: Float,
  val endY: Float,
  val width: Float = 64f,
  val height: Float = 16f,
  val speed: Float = 60f,
  var currentX: Float = startX,
  var currentY: Float = startY,
  var vx: Float = 0f,
  var vy: Float = 0f,
  var progress: Float = 0f,
  var forward: Boolean = true
)

data class CrumblingBlockState(
  val tileX: Int,
  val tileY: Int,
  var timer: Float = 0f, // 0 = stable, >0 = crumbling, <0 = respawning
  var state: BlockState = BlockState.STABLE
) {
  enum class BlockState { STABLE, SHAKING, FALLEN }
}

data class Checkpoint(
  val id: Int,
  val x: Float,
  val y: Float,
  var active: Boolean = false
)

data class Portal(
  val x: Float,
  val y: Float,
  val requiredArtifacts: Int,
  var isUnlocked: Boolean = false
)

enum class LevelTheme {
  ANCIENT_RUINS,
  CRYSTAL_ABYSS,
  FORGOTTEN_CITADEL,
  TWILIGHT_RIFT,
  VOID_GATEWAYS
}

data class LevelDef(
  val id: Int,
  val titleRu: String,
  val subtitleRu: String,
  val theme: LevelTheme,
  val tileCols: Int,
  val tileRows: Int,
  val tileSize: Float = 32f,
  val spawnX: Float,
  val spawnY: Float,
  val portal: Portal,
  val tiles: Array<IntArray>,
  val artifacts: List<Artifact>,
  val starDustList: List<StarDustPickup>,
  val movingPlatforms: List<MovingPlatform>,
  val checkpoints: List<Checkpoint>
)
