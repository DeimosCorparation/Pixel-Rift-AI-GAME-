package com.example.engine

import com.example.data.models.Artifact
import com.example.data.models.Checkpoint
import com.example.data.models.LevelDef
import com.example.data.models.LevelTheme
import com.example.data.models.MovingPlatform
import com.example.data.models.Portal
import com.example.data.models.StarDustPickup
import com.example.data.models.TileType

object LevelCatalog {

  fun getLevel(id: Int): LevelDef {
    return when (id) {
      1 -> createLevel1()
      2 -> createLevel2()
      3 -> createLevel3()
      4 -> createLevel4()
      5 -> createLevel5()
      else -> createLevel1()
    }
  }

  val totalLevels = 5

  // Helper to parse ASCII map into tile matrix
  private fun parseTileGrid(lines: List<String>, cols: Int, rows: Int): Array<IntArray> {
    val grid = Array(rows) { IntArray(cols) { 0 } }
    for (r in 0 until rows) {
      val line = if (r < lines.size) lines[r] else ""
      for (c in 0 until cols) {
        val char = if (c < line.length) line[c] else '.'
        grid[r][c] = when (char) {
          '#' -> TileType.STONE.id
          'G' -> TileType.DIRT_GRASS.id
          'B' -> TileType.BRICK.id
          '_' -> TileType.WOOD_PLATFORM.id
          '^' -> TileType.SPIKE_UP.id
          'v' -> TileType.SPIKE_DOWN.id
          'C' -> TileType.CRUMBLING.id
          'M' -> TileType.BOUNCE_PAD.id
          '*' -> TileType.CRYSTAL_WALL.id
          else -> TileType.AIR.id
        }
      }
    }
    return grid
  }

  /**
   * LEVEL 1: Ancient Ruins
   * Introduction to jumping, crumbling stones, wall climbing, 3 artifacts.
   */
  private fun createLevel1(): LevelDef {
    val cols = 64
    val rows = 18
    val tileSize = 32f

    val mapAscii = listOf(
      "################################################################",
      "#..............................................................#",
      "#..............................................................#",
      "#...................................______.....................#",
      "#......................####..........................#####.....#",
      "#......................#.............................#.........#",
      "#..............CCCC....#...................___.......#.........#",
      "#......................#.............................#.........#",
      "#.........###..........#..........####...............#...P.....#",
      "#......................#.............................#...#.....#",
      "#...___................#.._.._.......................#####.....#",
      "#..................#####.......................................#",
      "#.......#####......#.................#####.....................#",
      "#..................#.................#...#......M.............#",
      "#GGGGGG............#...^^^^^^^^^^^...#...#....GGGGGGGGGGGGGGGGGG",
      "#GGGGGG...^^^^^^...#...###########...#...#....GGGGGGGGGGGGGGGGGG",
      "#GGGGGG############GGGGGGGGGGGGGGGGGGG...GGGGGGGGGGGGGGGGGGGGGGG",
      "################################################################"
    )

    val tiles = parseTileGrid(mapAscii, cols, rows)

    val artifacts = listOf(
      Artifact(id = 1, x = 15 * tileSize, y = 5 * tileSize),  // Above crumbling bridge
      Artifact(id = 2, x = 23 * tileSize, y = 3 * tileSize),  // Top of middle tower
      Artifact(id = 3, x = 50 * tileSize, y = 3 * tileSize)   // High ledge requiring bounce pad
    )

    val starDust = listOf(
      StarDustPickup(1, 6 * tileSize, 10 * tileSize, 5),
      StarDustPickup(2, 10 * tileSize, 11 * tileSize, 5),
      StarDustPickup(3, 27 * tileSize, 10 * tileSize, 5),
      StarDustPickup(4, 30 * tileSize, 10 * tileSize, 5),
      StarDustPickup(5, 38 * tileSize, 7 * tileSize, 10),
      StarDustPickup(6, 44 * tileSize, 4 * tileSize, 10),
      StarDustPickup(7, 48 * tileSize, 13 * tileSize, 5)
    )

    val movingPlatforms = listOf(
      MovingPlatform(
        id = 1,
        startX = 36 * tileSize,
        startY = 11 * tileSize,
        endX = 42 * tileSize,
        endY = 11 * tileSize,
        speed = 50f
      )
    )

    val checkpoints = listOf(
      Checkpoint(id = 1, x = 24 * tileSize, y = 11 * tileSize, active = false),
      Checkpoint(id = 2, x = 44 * tileSize, y = 13 * tileSize, active = false)
    )

    return LevelDef(
      id = 1,
      titleRu = "Древние Руины",
      subtitleRu = "Соберите 3 реликвии, чтобы пробудить портал странников",
      theme = LevelTheme.ANCIENT_RUINS,
      tileCols = cols,
      tileRows = rows,
      tileSize = tileSize,
      spawnX = 3 * tileSize,
      spawnY = 13 * tileSize,
      portal = Portal(x = 57 * tileSize, y = 7 * tileSize, requiredArtifacts = 3),
      tiles = tiles,
      artifacts = artifacts,
      starDustList = starDust,
      movingPlatforms = movingPlatforms,
      checkpoints = checkpoints
    )
  }

  /**
   * LEVEL 2: Crystal Abyss
   * Precision jumps, spikes, bounce pads, 4 artifacts.
   */
  private fun createLevel2(): LevelDef {
    val cols = 72
    val rows = 20
    val tileSize = 32f

    val mapAscii = listOf(
      "************************************************************************",
      "*......................................................................*",
      "*......................................................................*",
      "*.................*....................................................*",
      "*.................*....._____..........................................*",
      "*......____.......*....................................................*",
      "*.................*...................CCCC...............*.......P.....*",
      "*.................*......................................*.......*.....*",
      "*...........*.....*..........*****.............*****.....*.......*.....*",
      "*...........*....................*.............*.........*********.....*",
      "*...........*....................*...^^^^^^^...*.......................*",
      "*...____....*....................*...*******...*.......................*",
      "*...........*....................*.............*.....____..............*",
      "*...........*..._____............***************.......................*",
      "*...........*..........................................................*",
      "*...........*................M.........................................*",
      "*GGGG.......*.............*******..............................M........*",
      "*GGGG..^^^..*...^^^^^^^^^^^^^^^^^..............^^^^^^^^^^^^..GGGGGGGGGG*",
      "*GGGG*******G***GGGGGGGGGGGGGGGGG**************GGGGGGGGGGGG**GGGGGGGGGG*",
      "************************************************************************"
    )

    val tiles = parseTileGrid(mapAscii, cols, rows)

    val artifacts = listOf(
      Artifact(id = 1, x = 7 * tileSize, y = 4 * tileSize),
      Artifact(id = 2, x = 25 * tileSize, y = 3 * tileSize),
      Artifact(id = 3, x = 40 * tileSize, y = 5 * tileSize),
      Artifact(id = 4, x = 50 * tileSize, y = 11 * tileSize)
    )

    val starDust = listOf(
      StarDustPickup(1, 4 * tileSize, 10 * tileSize, 5),
      StarDustPickup(2, 14 * tileSize, 4 * tileSize, 10),
      StarDustPickup(3, 30 * tileSize, 14 * tileSize, 5),
      StarDustPickup(4, 40 * tileSize, 9 * tileSize, 10),
      StarDustPickup(5, 54 * tileSize, 10 * tileSize, 5),
      StarDustPickup(6, 61 * tileSize, 15 * tileSize, 5)
    )

    val movingPlatforms = listOf(
      MovingPlatform(
        id = 1,
        startX = 35 * tileSize,
        startY = 14 * tileSize,
        endX = 45 * tileSize,
        endY = 14 * tileSize,
        speed = 65f
      ),
      MovingPlatform(
        id = 2,
        startX = 55 * tileSize,
        startY = 7 * tileSize,
        endX = 55 * tileSize,
        endY = 13 * tileSize,
        speed = 50f
      )
    )

    val checkpoints = listOf(
      Checkpoint(id = 1, x = 20 * tileSize, y = 13 * tileSize),
      Checkpoint(id = 2, x = 48 * tileSize, y = 7 * tileSize)
    )

    return LevelDef(
      id = 2,
      titleRu = "Хрустальная Бездна",
      subtitleRu = "Остерегайтесь шипов и используйте упругие споровые грибы",
      theme = LevelTheme.CRYSTAL_ABYSS,
      tileCols = cols,
      tileRows = rows,
      tileSize = tileSize,
      spawnX = 2 * tileSize,
      spawnY = 15 * tileSize,
      portal = Portal(x = 65 * tileSize, y = 5 * tileSize, requiredArtifacts = 4),
      tiles = tiles,
      artifacts = artifacts,
      starDustList = starDust,
      movingPlatforms = movingPlatforms,
      checkpoints = checkpoints
    )
  }

  /**
   * LEVEL 3: Forgotten Citadel
   * Vertical scaling, collapsing bridges, moving lifts, 4 artifacts.
   */
  private fun createLevel3(): LevelDef {
    val cols = 75
    val rows = 22
    val tileSize = 32f

    val mapAscii = listOf(
      "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB",
      "B.........................................................................B",
      "B...............................................................P.........B",
      "B...___.........................................................B.........B",
      "B........B...................BBBBBB.............................B.........B",
      "B........B...................B....B.........CCCC................BBBBBBB...B",
      "B........B.....CCCC..........B....B.......................................B",
      "B...B....B...................B....B..................____.................B",
      "B...B....B............BBBB...B....BBBB....................................B",
      "B...B....B............B......B.......B.........BBBB.......................B",
      "B...B....BBBB.........B......B.......B.........B..B.......................B",
      "B...B........................B.......B.........B..B..................BBBBBB",
      "B...BBBB........___..........B.......B.........B..B.......................B",
      "B............................B.......B.........B..B.......................B",
      "B..................BBBB......B.......B.........B..B..........___..........B",
      "B...........____.............B.......B.........B..B.......................B",
      "B............................B.......B.........B..B.......................B",
      "B.....M......................B.......B.........B..B..............M........B",
      "B...BBBBB....................B.......B.........B..B............BBBBBBBB...B",
      "B...B...B...^^^^^^^^^^^^^^^^^B.......B^^^^^^^^^B..B^^^^^^^^^^^^B......B...B",
      "BBBBB...BBBBBBBBBBBBBBBBBBBBBB.......BBBBBBBBBBB..BBBBBBBBBBBBBB......BBBBB",
      "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB"
    )

    val tiles = parseTileGrid(mapAscii, cols, rows)

    val artifacts = listOf(
      Artifact(id = 1, x = 3 * tileSize, y = 2 * tileSize),
      Artifact(id = 2, x = 16 * tileSize, y = 5 * tileSize),
      Artifact(id = 3, x = 45 * tileSize, y = 4 * tileSize),
      Artifact(id = 4, x = 60 * tileSize, y = 13 * tileSize)
    )

    val starDust = listOf(
      StarDustPickup(1, 10 * tileSize, 14 * tileSize, 5),
      StarDustPickup(2, 22 * tileSize, 7 * tileSize, 10),
      StarDustPickup(3, 31 * tileSize, 13 * tileSize, 10),
      StarDustPickup(4, 52 * tileSize, 6 * tileSize, 10),
      StarDustPickup(5, 68 * tileSize, 16 * tileSize, 5)
    )

    val movingPlatforms = listOf(
      MovingPlatform(
        id = 1,
        startX = 30 * tileSize,
        startY = 17 * tileSize,
        endX = 30 * tileSize,
        endY = 5 * tileSize,
        speed = 55f
      ),
      MovingPlatform(
        id = 2,
        startX = 56 * tileSize,
        startY = 8 * tileSize,
        endX = 64 * tileSize,
        endY = 8 * tileSize,
        speed = 60f
      )
    )

    val checkpoints = listOf(
      Checkpoint(id = 1, x = 20 * tileSize, y = 13 * tileSize),
      Checkpoint(id = 2, x = 36 * tileSize, y = 8 * tileSize),
      Checkpoint(id = 3, x = 50 * tileSize, y = 8 * tileSize)
    )

    return LevelDef(
      id = 3,
      titleRu = "Забытая Цитадель",
      subtitleRu = "Древняя крепость полна вертикальных подъёмов и рушащихся плит",
      theme = LevelTheme.FORGOTTEN_CITADEL,
      tileCols = cols,
      tileRows = rows,
      tileSize = tileSize,
      spawnX = 3 * tileSize,
      spawnY = 17 * tileSize,
      portal = Portal(x = 64 * tileSize, y = 2 * tileSize, requiredArtifacts = 4),
      tiles = tiles,
      artifacts = artifacts,
      starDustList = starDust,
      movingPlatforms = movingPlatforms,
      checkpoints = checkpoints
    )
  }

  /**
   * LEVEL 4: Twilight Rift
   * Floating sky platforms, requiring double jump & dash, 5 artifacts.
   */
  private fun createLevel4(): LevelDef {
    val cols = 80
    val rows = 22
    val tileSize = 32f

    val mapAscii = listOf(
      "################################################################################",
      "#..............................................................................#",
      "#..............................................................................#",
      "#...................###.............................................#####......#",
      "#...................#.#..................___........................#...#..P...#",
      "#..........___......###..........CCCC............___................#...####...#",
      "#...................................................................#..........#",
      "#...................................................................#..........#",
      "#....###.................___...................................._____..........#",
      "#....#.#.........................................###...........................#",
      "#....###...........................####..........#.#...........................#",
      "#............____..................#..#..........###...........................#",
      "#..................................####........................................#",
      "#...........................................................___................#",
      "#.......................CCCC...................................................#",
      "#..............................................................................#",
      "#.......M...............................................M......................#",
      "#.....#####...........................................#####....................#",
      "#.....#...#...........................................#...#....................#",
      "#.....#...#.....^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^.....#...#....................#",
      "#GGGGGG...GGGGGG#################################GGGGGG...GGGGGGGGGGGGGGGGGGGGGG",
      "################################################################################"
    )

    val tiles = parseTileGrid(mapAscii, cols, rows)

    val artifacts = listOf(
      Artifact(id = 1, x = 11 * tileSize, y = 4 * tileSize),
      Artifact(id = 2, x = 20 * tileSize, y = 2 * tileSize),
      Artifact(id = 3, x = 35 * tileSize, y = 9 * tileSize),
      Artifact(id = 4, x = 50 * tileSize, y = 8 * tileSize),
      Artifact(id = 5, x = 62 * tileSize, y = 12 * tileSize)
    )

    val starDust = listOf(
      StarDustPickup(1, 15 * tileSize, 10 * tileSize, 10),
      StarDustPickup(2, 28 * tileSize, 7 * tileSize, 10),
      StarDustPickup(3, 42 * tileSize, 4 * tileSize, 15),
      StarDustPickup(4, 58 * tileSize, 7 * tileSize, 10),
      StarDustPickup(5, 70 * tileSize, 7 * tileSize, 15)
    )

    val movingPlatforms = listOf(
      MovingPlatform(
        id = 1,
        startX = 23 * tileSize,
        startY = 14 * tileSize,
        endX = 33 * tileSize,
        endY = 14 * tileSize,
        speed = 70f
      ),
      MovingPlatform(
        id = 2,
        startX = 38 * tileSize,
        startY = 14 * tileSize,
        endX = 48 * tileSize,
        endY = 14 * tileSize,
        speed = 80f
      ),
      MovingPlatform(
        id = 3,
        startX = 64 * tileSize,
        startY = 10 * tileSize,
        endX = 64 * tileSize,
        endY = 4 * tileSize,
        speed = 55f
      )
    )

    val checkpoints = listOf(
      Checkpoint(id = 1, x = 18 * tileSize, y = 19 * tileSize),
      Checkpoint(id = 2, x = 52 * tileSize, y = 19 * tileSize)
    )

    return LevelDef(
      id = 4,
      titleRu = "Сумеречный Разлом",
      subtitleRu = "Парящие острова в облаках — используйте двойной прыжок и рывок",
      theme = LevelTheme.TWILIGHT_RIFT,
      tileCols = cols,
      tileRows = rows,
      tileSize = tileSize,
      spawnX = 2 * tileSize,
      spawnY = 19 * tileSize,
      portal = Portal(x = 75 * tileSize, y = 3 * tileSize, requiredArtifacts = 5),
      tiles = tiles,
      artifacts = artifacts,
      starDustList = starDust,
      movingPlatforms = movingPlatforms,
      checkpoints = checkpoints
    )
  }

  /**
   * LEVEL 5: Void Gateways
   * The ultimate challenge: dense traps, multiple moving hazards, 5 artifacts.
   */
  private fun createLevel5(): LevelDef {
    val cols = 85
    val rows = 24
    val tileSize = 32f

    val mapAscii = listOf(
      "*************************************************************************************",
      "*...................................................................................*",
      "*...................................................................................*",
      "*.................*...................................*...................*...P.....*",
      "*.................*..........______...................*...................*...*.....*",
      "*........CCCC.....*...................................*.......____........*...*.....*",
      "*.................*...................................*...................*****.....*",
      "*.................*.......................CCCC........*.............................*",
      "*...____..........*..........*****....................*.............................*",
      "*.................*..........*...*....................*.............................*",
      "*.................*..........*...*...^^^^^^^^^^^......*............****.............*",
      "*...........*.....*..........*...*...***********......*............*..*.............*",
      "*...........*................*...*....................*............*..*.............*",
      "*...........*................*...*....................*............****.............*",
      "*.....*.....*................*****....................*.............................*",
      "*.....*.....*.........................................*.............................*",
      "*.....*.....*..............................M..........*...................M.........*",
      "*.....*.....*............................*****........*.................*****.......*",
      "*.....*.....*............................*...*........*.................*...*.......*",
      "*.....*.....*............................*...*........*.................*...*.......*",
      "*..*..*..*..*....^^^^^^^^^^^^^^^^^^^^^^^^*...*^^^^^^^^*^^^^^^^^^^^^^^^^^*...*.......*",
      "*..*..*..*..*....*************************************************************.......*",
      "*GGGGGGGGGGGG....*************************************************************GGGGGGG*",
      "*************************************************************************************"
    )

    val tiles = parseTileGrid(mapAscii, cols, rows)

    val artifacts = listOf(
      Artifact(id = 1, x = 4 * tileSize, y = 7 * tileSize),
      Artifact(id = 2, x = 10 * tileSize, y = 4 * tileSize),
      Artifact(id = 3, x = 31 * tileSize, y = 3 * tileSize),
      Artifact(id = 4, x = 46 * tileSize, y = 6 * tileSize),
      Artifact(id = 5, x = 68 * tileSize, y = 4 * tileSize)
    )

    val starDust = listOf(
      StarDustPickup(1, 14 * tileSize, 10 * tileSize, 15),
      StarDustPickup(2, 25 * tileSize, 13 * tileSize, 15),
      StarDustPickup(3, 40 * tileSize, 9 * tileSize, 20),
      StarDustPickup(4, 58 * tileSize, 7 * tileSize, 20),
      StarDustPickup(5, 73 * tileSize, 8 * tileSize, 25)
    )

    val movingPlatforms = listOf(
      MovingPlatform(
        id = 1,
        startX = 18 * tileSize,
        startY = 14 * tileSize,
        endX = 26 * tileSize,
        endY = 14 * tileSize,
        speed = 85f
      ),
      MovingPlatform(
        id = 2,
        startX = 38 * tileSize,
        startY = 14 * tileSize,
        endX = 50 * tileSize,
        endY = 14 * tileSize,
        speed = 90f
      ),
      MovingPlatform(
        id = 3,
        startX = 55 * tileSize,
        startY = 12 * tileSize,
        endX = 55 * tileSize,
        endY = 4 * tileSize,
        speed = 70f
      )
    )

    val checkpoints = listOf(
      Checkpoint(id = 1, x = 16 * tileSize, y = 14 * tileSize),
      Checkpoint(id = 2, x = 32 * tileSize, y = 7 * tileSize),
      Checkpoint(id = 3, x = 53 * tileSize, y = 14 * tileSize)
    )

    return LevelDef(
      id = 5,
      titleRu = "Врата Пустоты",
      subtitleRu = "Финальный рубеж. Преодолейте все преграды и найдите 5 реликвий",
      theme = LevelTheme.VOID_GATEWAYS,
      tileCols = cols,
      tileRows = rows,
      tileSize = tileSize,
      spawnX = 2 * tileSize,
      spawnY = 21 * tileSize,
      portal = Portal(x = 78 * tileSize, y = 3 * tileSize, requiredArtifacts = 5),
      tiles = tiles,
      artifacts = artifacts,
      starDustList = starDust,
      movingPlatforms = movingPlatforms,
      checkpoints = checkpoints
    )
  }
}
