package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.engine.RetroAudioEngine

@Composable
fun MainMenuScreen(
  prefs: GamePreferences,
  audio: RetroAudioEngine,
  onPlayClick: () -> Unit,
  onLevelSelectClick: () -> Unit,
  onUpgradesClick: () -> Unit,
  onEditorClick: () -> Unit,
  onSettingsClick: () -> Unit,
  onUpdateClick: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "title_glow")
  val glowPulse by infiniteTransition.animateFloat(
    initialValue = 0.6f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(1400),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glow"
  )

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
    // Subtle background decorative pixel portal rings
    Box(
      modifier = Modifier
        .align(Alignment.Center)
        .size(380.dp)
        .clip(CircleShape)
        .background(
          Brush.radialGradient(
            listOf(
              Color(0x3300FFFF).copy(alpha = 0.25f * glowPulse),
              Color(0x22A855F7).copy(alpha = 0.15f * glowPulse),
              Color.Transparent
            )
          )
        )
    )

    // Top-Right Action Buttons (Update & Settings)
    Row(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(20.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // In-app network update button
      IconButton(
        onClick = {
          audio.playButtonClick()
          onUpdateClick()
        },
        modifier = Modifier
          .size(46.dp)
          .clip(CircleShape)
          .background(Color(0xCC0F172A))
          .border(1.5.dp, Color(0xFF38BDF8), CircleShape)
          .testTag("update_button")
      ) {
        Icon(
          imageVector = Icons.Default.CloudDownload,
          contentDescription = "Обновление по сети",
          tint = Color(0xFF38BDF8),
          modifier = Modifier.size(24.dp)
        )
      }

      // Settings button
      IconButton(
        onClick = {
          audio.playButtonClick()
          onSettingsClick()
        },
        modifier = Modifier
          .size(46.dp)
          .clip(CircleShape)
          .background(Color(0xAA0F172A))
          .border(1.dp, Color(0x4464748B), CircleShape)
          .testTag("settings_button")
      ) {
        Icon(
          imageVector = Icons.Default.Settings,
          contentDescription = "Настройки",
          tint = Color(0xFFE2E8F0),
          modifier = Modifier.size(24.dp)
        )
      }
    }

    // Star Dust Balance top-left
    Row(
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(20.dp)
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
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "Пыли",
        color = Color(0xFF94A3B8),
        fontSize = 12.sp
      )
    }

    // Center Content
    Column(
      modifier = Modifier
        .align(Alignment.Center)
        .padding(horizontal = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // Game Logo Badge
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E293B),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00FFFF).copy(alpha = glowPulse)),
        modifier = Modifier.padding(bottom = 8.dp)
      ) {
        Text(
          text = "2D RETRO PLATFORMER",
          color = Color(0xFF00FFFF),
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp,
          letterSpacing = 2.sp,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
      }

      // Title
      Text(
        text = "PIXEL RIFT",
        color = Color.White,
        fontSize = 42.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 4.sp,
        fontFamily = FontFamily.SansSerif
      )

      Text(
        text = "ОХОТНИК ЗА РЕЛИКВИЯМИ",
        color = Color(0xFF38BDF8),
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 3.sp
      )

      Spacer(modifier = Modifier.height(28.dp))

      // Action Buttons
      Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Play / Continue
        MenuActionButton(
          text = "ИГРАТЬ",
          icon = Icons.Default.PlayArrow,
          tag = "play_button",
          primary = true,
          onClick = {
            audio.playButtonClick()
            onPlayClick()
          }
        )

        // Level Select
        MenuActionButton(
          text = "УРОВНИ",
          icon = Icons.Default.ViewList,
          tag = "level_select_button",
          primary = false,
          onClick = {
            audio.playButtonClick()
            onLevelSelectClick()
          }
        )

        // Level Editor
        MenuActionButton(
          text = "РЕДАКТОР",
          icon = Icons.Default.Edit,
          tag = "editor_button",
          primary = false,
          accent = Color(0xFF34D399),
          onClick = {
            audio.playButtonClick()
            onEditorClick()
          }
        )

        // Upgrade Shrine
        MenuActionButton(
          text = "ПРОКАЧКА",
          icon = Icons.Default.AutoAwesome,
          tag = "upgrades_button",
          primary = false,
          accent = Color(0xFFA855F7),
          onClick = {
            audio.playButtonClick()
            onUpgradesClick()
          }
        )
      }

      Spacer(modifier = Modifier.height(22.dp))

      // Current Progress Summary
      Text(
        text = "Открыто уровней: ${prefs.unlockedLevelMax} из 5  •  Поддержка редактора уровней",
        color = Color(0xFF64748B),
        fontSize = 12.sp
      )
    }
  }
}

@Composable
private fun MenuActionButton(
  text: String,
  icon: ImageVector,
  tag: String,
  primary: Boolean,
  accent: Color = Color(0xFF0284C7),
  onClick: () -> Unit
) {
  if (primary) {
    Button(
      onClick = onClick,
      shape = RoundedCornerShape(16.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFF0284C7)
      ),
      modifier = Modifier
        .height(56.dp)
        .testTag(tag)
        .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(16.dp))
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(24.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = text,
        color = Color.White,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 16.sp,
        letterSpacing = 1.sp
      )
    }
  } else {
    OutlinedButton(
      onClick = onClick,
      shape = RoundedCornerShape(16.dp),
      colors = ButtonDefaults.outlinedButtonColors(
        containerColor = Color(0x990F172A)
      ),
      border = androidx.compose.foundation.BorderStroke(1.5.dp, accent.copy(alpha = 0.8f)),
      modifier = Modifier
        .height(56.dp)
        .testTag(tag)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = accent,
        modifier = Modifier.size(22.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = text,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        letterSpacing = 1.sp
      )
    }
  }
}
