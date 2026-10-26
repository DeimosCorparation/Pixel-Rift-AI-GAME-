package com.example.data

import android.content.Context
import android.content.SharedPreferences

class GamePreferences(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("pixel_rift_save_data", Context.MODE_PRIVATE)

  var starDust: Int
    get() = prefs.getInt("star_dust", 50) // start with 50 essence as a welcome gift!
    set(value) = prefs.edit().putInt("star_dust", value).apply()

  var unlockedLevelMax: Int
    get() = prefs.getInt("unlocked_level_max", 1)
    set(value) = prefs.edit().putInt("unlocked_level_max", value.coerceAtLeast(1)).apply()

  fun getBestTime(levelId: Int): Long = prefs.getLong("level_${levelId}_best_time", 0L)
  fun setBestTime(levelId: Int, timeMs: Long) {
    val current = getBestTime(levelId)
    if (current == 0L || timeMs < current) {
      prefs.edit().putLong("level_${levelId}_best_time", timeMs).apply()
    }
  }

  fun getCollectedArtifacts(levelId: Int): Int = prefs.getInt("level_${levelId}_artifacts", 0)
  fun setCollectedArtifacts(levelId: Int, count: Int) {
    val current = getCollectedArtifacts(levelId)
    if (count > current) {
      prefs.edit().putInt("level_${levelId}_artifacts", count).apply()
    }
  }

  // Upgrades
  var doubleJumpRank: Int
    get() = prefs.getInt("upgrade_double_jump", 0) // 0 = locked, 1 = unlocked
    set(value) = prefs.edit().putInt("upgrade_double_jump", value).apply()

  var dashRank: Int
    get() = prefs.getInt("upgrade_dash", 0) // 0 = locked, 1 = short dash, 2 = long dash
    set(value) = prefs.edit().putInt("upgrade_dash", value).apply()

  var speedRank: Int
    get() = prefs.getInt("upgrade_speed", 0) // 0 to 3
    set(value) = prefs.edit().putInt("upgrade_speed", value).apply()

  var jumpRank: Int
    get() = prefs.getInt("upgrade_jump", 0) // 0 to 3
    set(value) = prefs.edit().putInt("upgrade_jump", value).apply()

  var magnetRank: Int
    get() = prefs.getInt("upgrade_magnet", 0) // 0 to 3
    set(value) = prefs.edit().putInt("upgrade_magnet", value).apply()

  var shieldRank: Int
    get() = prefs.getInt("upgrade_shield", 0) // 0 to 3 (grants +1 heart per rank, rank 1 = 4 hearts, rank 2 = 5 hearts)
    set(value) = prefs.edit().putInt("upgrade_shield", value).apply()

  var chronoRank: Int
    get() = prefs.getInt("upgrade_chrono", 0) // 0 to 2 (slow-motion ability)
    set(value) = prefs.edit().putInt("upgrade_chrono", value).apply()

  var glideRank: Int
    get() = prefs.getInt("upgrade_glide", 0) // 0 to 2 (glide / slow falling)
    set(value) = prefs.edit().putInt("upgrade_glide", value).apply()

  var dustMultiplierRank: Int
    get() = prefs.getInt("upgrade_dust_mult", 0) // 0 to 2 (dust alchemist)
    set(value) = prefs.edit().putInt("upgrade_dust_mult", value).apply()

  var wallJumpRank: Int
    get() = prefs.getInt("upgrade_wall_jump", 0) // 0 to 2 (kinetic wall jump boost)
    set(value) = prefs.edit().putInt("upgrade_wall_jump", value).apply()

  var radarRank: Int
    get() = prefs.getInt("upgrade_radar", 0) // 0 or 1 (artifact compass / radar)
    set(value) = prefs.edit().putInt("upgrade_radar", value).apply()

  var shieldRechargeRank: Int
    get() = prefs.getInt("upgrade_shield_recharge", 0) // 0 to 2 (shield auto-regeneration)
    set(value) = prefs.edit().putInt("upgrade_shield_recharge", value).apply()

  var phoenixRank: Int
    get() = prefs.getInt("upgrade_phoenix", 0) // 0 or 1 (phoenix blessing resurrection)
    set(value) = prefs.edit().putInt("upgrade_phoenix", value).apply()

  var rayTeleportRank: Int
    get() = prefs.getInt("upgrade_ray_teleport", 0) // 0 = locked, 1 = 1 block, 2 = 2 blocks
    set(value) = prefs.edit().putInt("upgrade_ray_teleport", value).apply()

  // Settings
  var soundEnabled: Boolean
    get() = prefs.getBoolean("settings_sound", true)
    set(value) = prefs.edit().putBoolean("settings_sound", value).apply()

  var hapticsEnabled: Boolean
    get() = prefs.getBoolean("settings_haptics", true)
    set(value) = prefs.edit().putBoolean("settings_haptics", value).apply()

  var cycleSpeed: Float
    get() = prefs.getFloat("settings_cycle_speed", 1.0f)
    set(value) = prefs.edit().putFloat("settings_cycle_speed", value).apply()

  // In-App Updater
  var autoCheckUpdates: Boolean
    get() = prefs.getBoolean("settings_auto_check_updates", true)
    set(value) = prefs.edit().putBoolean("settings_auto_check_updates", value).apply()

  var customUpdateUrl: String
    get() = prefs.getString("settings_custom_update_url", "") ?: ""
    set(value) = prefs.edit().putString("settings_custom_update_url", value).apply()
}
