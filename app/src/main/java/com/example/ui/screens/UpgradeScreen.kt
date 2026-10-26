package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterTiltShift
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GamePreferences
import com.example.data.models.AbilityRepository
import com.example.data.models.UpgradeItem
import com.example.engine.RetroAudioEngine

@Composable
fun UpgradeScreen(
  prefs: GamePreferences,
  audio: RetroAudioEngine,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler { onBackClick() }

  // State trigger to force recomposition when buying upgrades
  var refreshTrigger by remember { mutableIntStateOf(0) }

  val upgrades = remember(refreshTrigger) {
    AbilityRepository.getUpgrades(
      doubleJumpRank = prefs.doubleJumpRank,
      dashRank = prefs.dashRank,
      speedRank = prefs.speedRank,
      jumpRank = prefs.jumpRank,
      magnetRank = prefs.magnetRank,
      shieldRank = prefs.shieldRank,
      chronoRank = prefs.chronoRank,
      glideRank = prefs.glideRank,
      dustMultiplierRank = prefs.dustMultiplierRank,
      wallJumpRank = prefs.wallJumpRank,
      radarRank = prefs.radarRank,
      shieldRechargeRank = prefs.shieldRechargeRank,
      phoenixRank = prefs.phoenixRank,
      rayTeleportRank = prefs.rayTeleportRank
    )
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          listOf(
            Color(0xFF030712),
            Color(0xFF0F172A),
            Color(0xFF1E1B4B)
          )
        )
      )
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 14.dp),
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
              .size(44.dp)
              .clip(CircleShape)
              .background(Color(0xAA0F172A))
              .border(1.dp, Color(0x4464748B), CircleShape)
              .testTag("back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Назад",
              tint = Color.White
            )
          }

          Spacer(modifier = Modifier.width(16.dp))

          Column {
            Text(
              text = "АЛТАРЬ ПРОКАЧКИ",
              color = Color.White,
              fontSize = 22.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 2.sp
            )
            Text(
              text = "Улучшайте навыки странника с помощью Звёздной Пыли",
              color = Color(0xFFA855F7),
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }

        // Star Dust Balance
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xCC0F172A))
            .border(1.dp, Color(0x44FBBF24), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Звёздная Пыль",
            tint = Color(0xFFFBBF24),
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "${prefs.starDust}",
            color = Color(0xFFFDE047),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      // Upgrades Grid (2 columns on landscape screens)
      LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(upgrades) { upgrade ->
          UpgradeCard(
            item = upgrade,
            currentBalance = prefs.starDust,
            onUpgrade = {
              if (prefs.starDust >= upgrade.nextCost && !upgrade.isMaxed) {
                prefs.starDust -= upgrade.nextCost
                applyUpgrade(prefs, upgrade.id)
                audio.playPortalUnlock()
                refreshTrigger++
              }
            }
          )
        }
      }
    }
  }
}

private fun applyUpgrade(prefs: GamePreferences, upgradeId: String) {
  when (upgradeId) {
    "double_jump" -> prefs.doubleJumpRank = prefs.doubleJumpRank + 1
    "air_dash" -> prefs.dashRank = prefs.dashRank + 1
    "speed" -> prefs.speedRank = prefs.speedRank + 1
    "high_jump" -> prefs.jumpRank = prefs.jumpRank + 1
    "magnet" -> prefs.magnetRank = prefs.magnetRank + 1
    "vitality_shield" -> prefs.shieldRank = prefs.shieldRank + 1
    "chrono_shift" -> prefs.chronoRank = prefs.chronoRank + 1
    "lunar_glide" -> prefs.glideRank = prefs.glideRank + 1
    "dust_alchemist" -> prefs.dustMultiplierRank = prefs.dustMultiplierRank + 1
    "wall_spring" -> prefs.wallJumpRank = prefs.wallJumpRank + 1
    "artifact_radar" -> prefs.radarRank = prefs.radarRank + 1
    "shield_recharge" -> prefs.shieldRechargeRank = prefs.shieldRechargeRank + 1
    "phoenix_blessing" -> prefs.phoenixRank = prefs.phoenixRank + 1
    "ray_teleport" -> prefs.rayTeleportRank = prefs.rayTeleportRank + 1
  }
}

@Composable
private fun UpgradeCard(
  item: UpgradeItem,
  currentBalance: Int,
  onUpgrade: () -> Unit
) {
  val icon: ImageVector = when (item.iconName) {
    "double_jump" -> Icons.Default.Flight
    "air_dash" -> Icons.Default.ElectricBolt
    "speed" -> Icons.Default.Speed
    "high_jump" -> Icons.Default.RocketLaunch
    "glide" -> Icons.Default.Air
    "wall_spring" -> Icons.Default.NorthEast
    "magnet" -> Icons.Default.FilterTiltShift
    "dust_mult" -> Icons.Default.AutoAwesome
    "radar" -> Icons.Default.Explore
    "shield" -> Icons.Default.Security
    "shield_recharge" -> Icons.Default.Autorenew
    "chrono" -> Icons.Default.HourglassTop
    "phoenix" -> Icons.Default.Favorite
    "ray_teleport" -> Icons.Default.ElectricBolt
    else -> Icons.Default.Star
  }

  val canAfford = currentBalance >= item.nextCost && !item.isMaxed
  val cardBorder = if (item.isMaxed) Color(0xFF10B981) else if (canAfford) Color(0xFF38BDF8) else Color(0x3364748B)

  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(16.dp))
      .background(Color(0xDD0F172A))
      .border(1.5.dp, cardBorder, RoundedCornerShape(16.dp))
      .padding(14.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Left: Icon + Info
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
              brush = if (item.currentRank > 0)
                Brush.radialGradient(listOf(Color(0xFF0284C7), Color(0xFF1E1B4B)))
              else
                Brush.linearGradient(listOf(Color(0x44334155), Color(0x44334155)))
            )
            .border(1.dp, if (item.currentRank > 0) Color(0xFF38BDF8) else Color(0x2264748B), RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (item.currentRank > 0) Color.White else Color(0xFF94A3B8),
            modifier = Modifier.size(24.dp)
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = item.nameRu,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Rank pips: [●][○][○]
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
              for (r in 1..item.maxRank) {
                val filled = r <= item.currentRank
                Box(
                  modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (filled) Color(0xFF38BDF8) else Color(0xFF334155))
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(2.dp))

          Text(
            text = item.descRu,
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            lineHeight = 14.sp,
            maxLines = 2
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Right: Upgrade Button / Maxed Badge
      if (item.isMaxed) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Color(0x3310B981),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
        ) {
          Text(
            text = "МАКСИМУМ",
            color = Color(0xFF34D399),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          )
        }
      } else {
        Button(
          onClick = onUpgrade,
          enabled = canAfford,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF0284C7),
            disabledContainerColor = Color(0x33334155)
          ),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.testTag("upgrade_${item.id}_button")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Star,
              contentDescription = null,
              tint = if (canAfford) Color(0xFFFDE047) else Color(0xFF64748B),
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "${item.nextCost}",
              color = if (canAfford) Color.White else Color(0xFF64748B),
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }
    }
  }
}
