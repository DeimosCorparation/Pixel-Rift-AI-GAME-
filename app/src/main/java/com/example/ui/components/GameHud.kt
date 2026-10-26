package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.DayPhase
import com.example.engine.GameEngine
import java.util.Locale

@Composable
fun GameHud(
  engine: GameEngine,
  onPauseClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  // Reading engine.renderTick ensures HUD counters, timer, and hearts update continuously
  val tick = engine.renderTick
  Box(modifier = modifier.fillMaxWidth()) {
    // Top Bar Container
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Left: Hearts & Barrier Shield
      Row(verticalAlignment = Alignment.CenterVertically) {
        // Hearts
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC0F172A))
            .border(1.dp, Color(0x4464748B), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          for (i in 0 until engine.maxHearts) {
            val isFull = i < engine.currentHearts
            Icon(
              imageVector = if (isFull) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
              contentDescription = "Жизнь ${i + 1}",
              tint = if (isFull) Color(0xFFEF4444) else Color(0xFF64748B),
              modifier = Modifier
                .size(20.dp)
                .padding(end = 2.dp)
            )
          }

          if (engine.hasBarrierShield) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = "Щит",
              tint = Color(0xFF38BDF8),
              modifier = Modifier.size(18.dp)
            )
          }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Artifacts Counter (Relics needed to unlock portal)
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
              if (engine.isPortalUnlocked)
                Brush.horizontalGradient(listOf(Color(0xDD065F46), Color(0xDD047857)))
              else
                Brush.horizontalGradient(listOf(Color(0xCC0F172A), Color(0xCC1E293B)))
            )
            .border(
              1.dp,
              if (engine.isPortalUnlocked) Color(0xFF10B981) else Color(0xFF00FFFF).copy(alpha = 0.5f),
              RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "✦",
            color = if (engine.isPortalUnlocked) Color(0xFF34D399) else Color(0xFF00FFFF),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = "${engine.artifactsCollected}/${engine.totalArtifacts}",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace
          )
          if (engine.isPortalUnlocked) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "ВХОД ОТКРЫТ!",
              color = Color(0xFF34D399),
              fontWeight = FontWeight.ExtraBold,
              fontSize = 11.sp
            )
          }
        }
      }

      // Center: Day/Night indicator & Level Title
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(20.dp))
          .background(Color(0xCC0F172A))
          .border(1.dp, Color(0x4464748B), RoundedCornerShape(20.dp))
          .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Sun/Moon icon based on phase
        val (phaseIcon, phaseColor) = when (engine.dayPhase) {
          DayPhase.DAY -> Pair(Icons.Default.LightMode, Color(0xFFFBBF24))
          DayPhase.DUSK -> Pair(Icons.Default.WbTwilight, Color(0xFFFB923C))
          DayPhase.NIGHT -> Pair(Icons.Default.DarkMode, Color(0xFF818CF8))
          DayPhase.DAWN -> Pair(Icons.Default.WbTwilight, Color(0xFFF472B6))
        }

        Icon(
          imageVector = phaseIcon,
          contentDescription = engine.dayPhase.titleRu,
          tint = phaseColor,
          modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = engine.dayPhase.titleRu,
          color = phaseColor,
          fontWeight = FontWeight.SemiBold,
          fontSize = 12.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "• ${engine.level.titleRu}",
          color = Color(0xFFE2E8F0),
          fontWeight = FontWeight.Medium,
          fontSize = 12.sp
        )
      }

      // Right: Star Dust, Speedrun Timer & Pause Button
      Row(verticalAlignment = Alignment.CenterVertically) {
        // Star Dust (Currency)
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC0F172A))
            .border(1.dp, Color(0x4464748B), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Звёздная Пыль",
            tint = Color(0xFFFBBF24),
            modifier = Modifier.size(17.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${engine.prefs.starDust}",
            color = Color(0xFFFDE047),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Timer
        val minutes = (engine.levelTimeSeconds / 60).toInt()
        val seconds = (engine.levelTimeSeconds % 60).toInt()
        val tenths = ((engine.levelTimeSeconds * 10) % 10).toInt()
        val timeFormatted = String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, tenths)

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0xCC0F172A),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4464748B)),
          modifier = Modifier.padding(end = 8.dp)
        ) {
          Text(
            text = timeFormatted,
            color = Color(0xFF94A3B8),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
          )
        }

        // Pause Button
        IconButton(
          onClick = onPauseClick,
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xCC0F172A))
            .border(1.dp, Color(0x4464748B), CircleShape)
            .testTag("pause_button")
        ) {
          Icon(
            imageVector = Icons.Default.Pause,
            contentDescription = "Пауза",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }

    // Portal & Checkpoint Notification Banner (floating centered toast)
    val prompt = engine.portalPromptText
    AnimatedVisibility(
      visible = prompt != null,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(top = 62.dp)
    ) {
      if (prompt != null) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
              Brush.horizontalGradient(
                listOf(Color(0xEE1E1B4B), Color(0xEE312E81), Color(0xEE1E1B4B))
              )
            )
            .border(1.dp, Color(0xFF818CF8), RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
          Text(
            text = prompt,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
        }
      }
    }
  }
}
