package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.data.CustomLevelStorage
import com.example.data.GamePreferences
import com.example.data.models.CustomLevelData
import com.example.engine.LevelCatalog
import com.example.engine.RetroAudioEngine
import java.util.Locale

@Composable
fun LevelSelectScreen(
  prefs: GamePreferences,
  storage: CustomLevelStorage,
  audio: RetroAudioEngine,
  onSelectLevel: (Int) -> Unit,
  onSelectCustomLevel: (CustomLevelData) -> Unit,
  onCreateNewLevel: () -> Unit,
  onEditCustomLevel: (Int) -> Unit,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler { onBackClick() }

  var selectedTab by remember { mutableStateOf(0) } // 0 = Campaign, 1 = Custom
  val campaignLevels = (1..LevelCatalog.totalLevels).map { LevelCatalog.getLevel(it) }
  val customLevels = storage.getCustomLevels()

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
      // Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 18.dp),
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
              text = "ВЫБОР УРОВНЯ",
              color = Color.White,
              fontSize = 24.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 2.sp
            )
            Text(
              text = "Кампания или Пользовательские Уровни",
              color = Color(0xFF94A3B8),
              fontSize = 12.sp
            )
          }
        }

        // Tab Selector
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(12.dp))
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          TabButton("Кампания", selectedTab == 0) { selectedTab = 0; audio.playButtonClick() }
          TabButton("Созданные", selectedTab == 1) { selectedTab = 1; audio.playButtonClick() }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Content List
      if (selectedTab == 0) {
        LazyRow(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
          horizontalArrangement = Arrangement.spacedBy(18.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          items(campaignLevels) { level ->
            val isUnlocked = level.id <= prefs.unlockedLevelMax
            val bestTimeMs = prefs.getBestTime(level.id)
            val bestArtifacts = prefs.getCollectedArtifacts(level.id)

            LevelCard(
              levelId = level.id,
              title = level.titleRu,
              subtitle = level.subtitleRu,
              artifactCount = level.artifacts.size,
              bestArtifacts = bestArtifacts,
              bestTimeMs = bestTimeMs,
              isUnlocked = isUnlocked,
              onPlay = {
                if (isUnlocked) {
                  audio.playButtonClick()
                  onSelectLevel(level.id)
                }
              }
            )
          }
        }
      } else {
        LazyRow(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
          horizontalArrangement = Arrangement.spacedBy(18.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Create New Level Card
          item {
            CreateLevelCard(onCreate = {
              audio.playButtonClick()
              onCreateNewLevel()
            })
          }

          items(customLevels) { level ->
            CustomLevelCard(
              level = level,
              onPlay = {
                audio.playButtonClick()
                onSelectCustomLevel(level)
              },
              onEdit = {
                audio.playButtonClick()
                onEditCustomLevel(level.id)
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun TabButton(text: String, selected: Boolean, onClick: () -> Unit) {
  Button(
    onClick = onClick,
    colors = ButtonDefaults.buttonColors(
      containerColor = if (selected) Color(0xFF0284C7) else Color.Transparent
    ),
    shape = RoundedCornerShape(8.dp),
    modifier = Modifier.height(36.dp)
  ) {
    Text(text = text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
  }
}

@Composable
private fun LevelCard(
  levelId: Int,
  title: String,
  subtitle: String,
  artifactCount: Int,
  bestArtifacts: Int,
  bestTimeMs: Long,
  isUnlocked: Boolean,
  onPlay: () -> Unit
) {
  val borderColor = if (isUnlocked) Color(0xFF38BDF8) else Color(0x4464748B)

  Box(
    modifier = Modifier
      .width(280.dp)
      .height(230.dp)
      .clip(RoundedCornerShape(20.dp))
      .background(
        brush = if (isUnlocked)
          Brush.verticalGradient(listOf(Color(0xDD0F172A), Color(0xEE1E293B)))
        else
          Brush.linearGradient(listOf(Color(0x77090D16), Color(0x77090D16)))
      )
      .border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
      .padding(18.dp)
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isUnlocked) Color(0xFF0284C7) else Color(0x4464748B)
          ) {
            Text(
              text = "ЭТАП $levelId",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }

          if (!isUnlocked) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = "Заблокировано",
              tint = Color(0xFF64748B),
              modifier = Modifier.size(20.dp)
            )
          } else {
            Text(
              text = "✦ $bestArtifacts / $artifactCount",
              color = if (bestArtifacts >= artifactCount) Color(0xFF34D399) else Color(0xFF00FFFF),
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = title,
          color = if (isUnlocked) Color.White else Color(0xFF94A3B8),
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = subtitle,
          color = Color(0xFF64748B),
          fontSize = 11.sp,
          maxLines = 2
        )
      }

      Column {
        if (isUnlocked && bestTimeMs > 0L) {
          val mins = (bestTimeMs / 1000) / 60
          val secs = (bestTimeMs / 1000) % 60
          val tenths = (bestTimeMs % 1000) / 100
          val timeStr = String.format(Locale.US, "%02d:%02d.%d", mins, secs, tenths)

          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Timer,
              contentDescription = null,
              tint = Color(0xFFFBBF24),
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Рекорд: $timeStr",
              color = Color(0xFFFDE047),
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.SemiBold
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
        }

        Button(
          onClick = onPlay,
          enabled = isUnlocked,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF0284C7),
            disabledContainerColor = Color(0x33475569)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .testTag("play_level_${levelId}_button")
        ) {
          Icon(
            imageVector = if (isUnlocked) Icons.Default.PlayArrow else Icons.Default.Lock,
            contentDescription = null,
            tint = if (isUnlocked) Color.White else Color(0xFF64748B),
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isUnlocked) "ВОЙТИ В ПОРТАЛ" else "ЗАБЛОКИРОВАНО",
            color = if (isUnlocked) Color.White else Color(0xFF64748B),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
          )
        }
      }
    }
  }
}

@Composable
private fun CreateLevelCard(onCreate: () -> Unit) {
  Box(
    modifier = Modifier
      .width(280.dp)
      .height(230.dp)
      .clip(RoundedCornerShape(20.dp))
      .background(Brush.verticalGradient(listOf(Color(0xFF065F46), Color(0xFF047857))))
      .border(1.5.dp, Color(0xFF34D399), RoundedCornerShape(20.dp))
      .clickable { onCreate() }
      .padding(18.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(56.dp)
          .clip(CircleShape)
          .background(Color(0xFF10B981)),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
      }
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = "Создать Уровень",
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Спроектируйте свой мир",
        color = Color(0xFF6EE7B7),
        fontSize = 12.sp
      )
    }
  }
}

@Composable
private fun CustomLevelCard(
  level: CustomLevelData,
  onPlay: () -> Unit,
  onEdit: () -> Unit
) {
  Box(
    modifier = Modifier
      .width(280.dp)
      .height(230.dp)
      .clip(RoundedCornerShape(20.dp))
      .background(Brush.verticalGradient(listOf(Color(0xDD0F172A), Color(0xEE1E293B))))
      .border(1.5.dp, Color(0xFFA855F7), RoundedCornerShape(20.dp))
      .padding(18.dp)
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFA855F7)
          ) {
            Text(
              text = "ПОЛЬЗОВАТЕЛЬСКИЙ",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 10.sp,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }

          IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Edit, contentDescription = "Редактировать", tint = Color(0xFFC084FC))
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = level.title,
          color = Color.White,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = level.subtitle,
          color = Color(0xFF94A3B8),
          fontSize = 11.sp,
          maxLines = 2
        )
      }

      Button(
        onClick = onPlay,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7)),
        modifier = Modifier
          .fillMaxWidth()
          .height(42.dp)
      ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "ИГРАТЬ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
      }
    }
  }
}
