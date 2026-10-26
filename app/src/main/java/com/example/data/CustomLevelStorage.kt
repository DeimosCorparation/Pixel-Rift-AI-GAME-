package com.example.data

import android.content.Context
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.example.data.models.CustomLevelData
import com.example.data.models.CustomArtifactData
import com.example.data.models.CustomStarDustData

class CustomLevelStorage(context: Context) {
  private val prefs = context.getSharedPreferences("pixel_rift_custom_levels", Context.MODE_PRIVATE)
  private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
  private val listType = Types.newParameterizedType(List::class.java, CustomLevelData::class.java)
  private val adapter: JsonAdapter<List<CustomLevelData>> = moshi.adapter(listType)

  fun getCustomLevels(): List<CustomLevelData> {
    val json = prefs.getString("custom_levels_json", null) ?: return getDefaultStarterLevels()
    return try {
      adapter.fromJson(json) ?: getDefaultStarterLevels()
    } catch (_: Exception) {
      getDefaultStarterLevels()
    }
  }

  fun saveCustomLevels(levels: List<CustomLevelData>) {
    try {
      val json = adapter.toJson(levels)
      prefs.edit().putString("custom_levels_json", json).apply()
    } catch (_: Exception) {}
  }

  private fun getDefaultStarterLevels(): List<CustomLevelData> {
    val cols = 50
    val rows = 18
    val tiles = IntArray(cols * rows) { 0 }
    for (r in 15 until rows) {
      for (c in 0 until cols) {
        tiles[r * cols + c] = 2 // Dirt grass
      }
    }
    for (c in 10..18) tiles[11 * cols + c] = 4 // Wood platform
    for (c in 25..34) tiles[9 * cols + c] = 1  // Stone
    for (c in 40..46) tiles[7 * cols + c] = 4  // Wood

    return listOf(
      CustomLevelData(
        id = 1,
        title = "Мой Первый Уровень",
        subtitle = "Создано в редакторе Pixel Rift",
        tileCols = cols,
        tileRows = rows,
        tiles = tiles.toList(),
        spawnX = 2f * 32f,
        spawnY = 13f * 32f,
        portalX = 43f * 32f,
        portalY = 6f * 32f,
        artifacts = listOf(
          CustomArtifactData(1, 14f * 32f, 10f * 32f),
          CustomArtifactData(2, 30f * 32f, 8f * 32f)
        ),
        starDustList = listOf(
          CustomStarDustData(1, 5f * 32f, 14f * 32f, 10),
          CustomStarDustData(2, 14f * 32f, 14f * 32f, 10),
          CustomStarDustData(3, 28f * 32f, 14f * 32f, 15)
        )
      )
    )
  }
}
