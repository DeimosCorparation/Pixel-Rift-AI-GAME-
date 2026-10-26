package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GameEngine

@Composable
fun TouchControls(
  engine: GameEngine,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp, vertical = 14.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Bottom
  ) {
    // Left D-Pad Controls: Move Left, Move Right
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Move Left Button (D-Pad Left / 'A')
      TouchHoldButton(
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = "Влево",
        tag = "btn_left",
        size = 72.dp,
        onStateChange = { isPressed ->
          if (isPressed) {
            engine.inputMoveX = -1f
          } else {
            if (engine.inputMoveX < 0f) engine.inputMoveX = 0f
          }
        }
      )

      // Move Right Button (D-Pad Right / 'D')
      TouchHoldButton(
        icon = Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = "Вправо",
        tag = "btn_right",
        size = 72.dp,
        onStateChange = { isPressed ->
          if (isPressed) {
            engine.inputMoveX = 1f
          } else {
            if (engine.inputMoveX > 0f) engine.inputMoveX = 0f
          }
        }
      )
    }

    // Right Action Buttons: Jump, Dash, Chrono Shift
    Row(
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Chrono Shift Ability Button (if unlocked)
      if (engine.hasChrono) {
        val cooldownPct = (engine.chronoCooldownTimer / engine.chronoCooldown).coerceIn(0f, 1f)
        val isReady = cooldownPct <= 0f && !engine.isChronoActive
        TouchTapButton(
          icon = Icons.Default.HourglassTop,
          contentDescription = "Хроно-Сдвиг",
          label = if (engine.isChronoActive) "АКТИВНО" else if (isReady) "ВРЕМЯ" else "${engine.chronoCooldownTimer.toInt()}с",
          tag = "btn_chrono",
          size = 58.dp,
          enabled = isReady,
          bgColor = if (engine.isChronoActive) Color(0xFF06B6D4) else Color(0xAA1E1B4B),
          accentColor = Color(0xFF00FFFF),
          onTap = { engine.onChronoPressed() }
        )
      }

      // Ray Teleport Button (if unlocked)
      if (engine.hasRayTeleport) {
        val teleportReady = engine.teleportCooldownTimer <= 0f
        TouchTapButton(
          icon = Icons.Default.FastForward,
          contentDescription = "Лучевой Телепорт",
          label = if (teleportReady) "ТЕЛЕПОРТ" else "${kotlin.math.ceil(engine.teleportCooldownTimer).toInt()}с",
          tag = "btn_teleport",
          size = 60.dp,
          enabled = teleportReady,
          bgColor = if (teleportReady) Color(0xCC0D9488) else Color(0x66134E4A),
          accentColor = Color(0xFF2DD4BF),
          onTap = { engine.onTeleportPressed() }
        )
      }

      // Air Dash Button (if unlocked)
      val dashUnlocked = engine.prefs.dashRank > 0
      if (dashUnlocked) {
        val dashReady = engine.dashCooldownTimer <= 0f
        TouchTapButton(
          icon = Icons.Default.ElectricBolt,
          contentDescription = "Рывок",
          label = if (dashReady) "РЫВОК" else "",
          tag = "btn_dash",
          size = 64.dp,
          enabled = dashReady,
          bgColor = if (dashReady) Color(0xCC7C3AED) else Color(0x664C1D95),
          accentColor = Color(0xFFA855F7),
          onTap = { engine.onDashPressed() }
        )
      }

      // Primary Jump Button (Supports variable height hold)
      TouchHoldButton(
        icon = Icons.Default.KeyboardArrowUp,
        contentDescription = "Прыжок",
        label = "ПРЫЖОК",
        tag = "btn_jump",
        size = 80.dp,
        bgColor = Color(0xDD0284C7),
        accentColor = Color(0xFF38BDF8),
        onStateChange = { isPressed ->
          if (isPressed) {
            engine.onJumpPressed()
          } else {
            engine.onJumpReleased()
          }
        }
      )
    }
  }
}

@Composable
private fun TouchHoldButton(
  icon: ImageVector,
  contentDescription: String,
  tag: String,
  size: Dp,
  label: String? = null,
  bgColor: Color = Color(0xDD0F172A),
  accentColor: Color = Color(0xFF64748B),
  onStateChange: (Boolean) -> Unit
) {
  var isPressed by remember { mutableStateOf(false) }
  val currentOnStateChange by rememberUpdatedState(onStateChange)

  Box(
    modifier = Modifier
      .size(size)
      .clip(CircleShape)
      .background(
        brush = if (isPressed)
          Brush.radialGradient(listOf(accentColor.copy(alpha = 0.9f), bgColor))
        else
          Brush.radialGradient(listOf(bgColor, Color(0xEE050814)))
      )
      .border(
        width = if (isPressed) 3.dp else 1.5.dp,
        color = if (isPressed) Color.White else accentColor.copy(alpha = 0.7f),
        shape = CircleShape
      )
      .pointerInput(Unit) {
        detectTapGestures(
          onPress = {
            isPressed = true
            currentOnStateChange(true)
            try {
              tryAwaitRelease()
            } finally {
              isPressed = false
              currentOnStateChange(false)
            }
          }
        )
      }
      .testTag(tag),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = if (isPressed) Color.White else Color(0xFFF1F5F9),
        modifier = Modifier.size(size * 0.48f)
      )
      if (label != null) {
        Text(
          text = label,
          color = Color.White.copy(alpha = 0.9f),
          fontSize = 9.sp,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 0.5.sp
        )
      }
    }
  }
}

@Composable
private fun TouchTapButton(
  icon: ImageVector,
  contentDescription: String,
  label: String,
  tag: String,
  size: Dp,
  enabled: Boolean,
  bgColor: Color,
  accentColor: Color,
  onTap: () -> Unit
) {
  val currentOnTap by rememberUpdatedState(onTap)
  var isPressed by remember { mutableStateOf(false) }

  Box(
    modifier = Modifier
      .size(size)
      .clip(CircleShape)
      .background(
        brush = if (!enabled)
          Brush.linearGradient(listOf(Color(0x551E293B), Color(0x551E293B)))
        else if (isPressed)
          Brush.radialGradient(listOf(accentColor, bgColor))
        else
          Brush.radialGradient(listOf(bgColor, Color(0xEE0F172A)))
      )
      .border(
        width = 1.5.dp,
        color = if (enabled) accentColor.copy(alpha = 0.8f) else Color(0x3364748B),
        shape = CircleShape
      )
      .pointerInput(enabled) {
        if (enabled) {
          detectTapGestures(
            onPress = {
              isPressed = true
              try {
                tryAwaitRelease()
              } finally {
                isPressed = false
              }
            },
            onTap = {
              currentOnTap()
            }
          )
        }
      }
      .testTag(tag),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = if (enabled) Color.White else Color(0xFF64748B),
        modifier = Modifier.size(size * 0.44f)
      )
      if (label.isNotEmpty()) {
        Text(
          text = label,
          color = if (enabled) Color.White else Color(0xFF94A3B8),
          fontSize = 8.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}
