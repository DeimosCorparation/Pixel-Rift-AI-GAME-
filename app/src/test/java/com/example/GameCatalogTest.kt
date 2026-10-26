package com.example

import com.example.engine.LevelCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameCatalogTest {

  @Test
  fun testAllLevelsLoaded() {
    assertEquals(5, LevelCatalog.totalLevels)

    for (id in 1..5) {
      val level = LevelCatalog.getLevel(id)
      assertNotNull(level)
      assertEquals(id, level.id)
      assertTrue(level.tileCols > 50)
      assertTrue(level.tileRows >= 18)
      assertTrue(level.artifacts.isNotEmpty())
      assertEquals(level.artifacts.size, level.portal.requiredArtifacts)
      assertTrue(level.spawnX > 0)
      assertTrue(level.spawnY > 0)
      assertTrue(level.portal.x > 0)
    }
  }
}
