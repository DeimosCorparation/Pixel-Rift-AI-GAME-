package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.window.Dialog
import com.example.data.GamePreferences
import com.example.engine.RetroAudioEngine
import com.example.updater.AppUpdateManager
import com.example.updater.UpdateInfo
import java.util.Locale

@Composable
fun SettingsDialog(
  prefs: GamePreferences,
  audio: RetroAudioEngine,
  updateManager: AppUpdateManager? = null,
  onCheckUpdate: () -> Unit = {},
  onTestUpdate: () -> Unit = {},
  onDismiss: () -> Unit
) {
  var soundOn by remember { mutableStateOf(prefs.soundEnabled) }
  var hapticsOn by remember { mutableStateOf(prefs.hapticsEnabled) }
  var cycleSpeed by remember { mutableFloatStateOf(prefs.cycleSpeed) }
  var autoCheck by remember { mutableStateOf(prefs.autoCheckUpdates) }

  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .clip(RoundedCornerShape(24.dp))
        .background(
          Brush.verticalGradient(
            listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
          )
        )
        .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(24.dp))
        .padding(20.dp)
    ) {
      Column(
        modifier = Modifier
          .verticalScroll(rememberScrollState())
      ) {
        // Title & Close
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "НАСТРОЙКИ",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            letterSpacing = 2.sp
          )
          IconButton(
            onClick = {
              audio.playButtonClick()
              onDismiss()
            },
            modifier = Modifier.testTag("close_settings_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Закрыть",
              tint = Color.White
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sound Toggle
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF38BDF8))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = "Звуковые 8-бит эффекты", color = Color.White, fontSize = 13.sp)
          }
          Switch(
            checked = soundOn,
            onCheckedChange = {
              soundOn = it
              prefs.soundEnabled = it
              audio.soundEnabled = it
              if (it) audio.playButtonClick()
            },
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = Color(0xFF0284C7)
            )
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Haptics Toggle
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Vibration, contentDescription = null, tint = Color(0xFFA855F7))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = "Вибрация и тактильный отклик", color = Color.White, fontSize = 13.sp)
          }
          Switch(
            checked = hapticsOn,
            onCheckedChange = {
              hapticsOn = it
              prefs.hapticsEnabled = it
              audio.hapticsEnabled = it
              if (it) audio.playButtonClick()
            },
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = Color(0xFFA855F7)
            )
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Day/Night Cycle Speed Slider
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "Скорость смены времени суток",
              color = Color.White,
              fontSize = 13.sp
            )
            Text(
              text = String.format(Locale.US, "%.1fx", cycleSpeed),
              color = Color(0xFFFDE047),
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              fontSize = 12.sp
            )
          }
          Slider(
            value = cycleSpeed,
            onValueChange = {
              cycleSpeed = it
              prefs.cycleSpeed = it
            },
            valueRange = 0.5f..3.0f,
            steps = 4,
            colors = SliderDefaults.colors(
              thumbColor = Color(0xFFFBBF24),
              activeTrackColor = Color(0xFFF59E0B)
            )
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // In-App Updates Section
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0x66090D16),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.CloudDownload,
                  contentDescription = null,
                  tint = Color(0xFF38BDF8),
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "ОБНОВЛЕНИЕ ПО СЕТИ",
                  color = Color(0xFF38BDF8),
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  letterSpacing = 1.sp
                )
              }
              if (updateManager != null) {
                Text(
                  text = "v${updateManager.currentVersionName}",
                  color = Color(0xFF94A3B8),
                  fontSize = 11.sp,
                  fontFamily = FontFamily.Monospace
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(text = "Автопроверка при запуске", color = Color(0xFFE2E8F0), fontSize = 12.sp)
              Switch(
                checked = autoCheck,
                onCheckedChange = {
                  autoCheck = it
                  prefs.autoCheckUpdates = it
                },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = Color.White,
                  checkedTrackColor = Color(0xFF0284C7)
                )
              )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Cloud network update notice
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x330284C7))
                .padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.CloudDownload,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Автоматическая загрузка новейшего релиза по защищённой сети.",
                color = Color(0xFFBAE6FD),
                fontSize = 11.sp,
                lineHeight = 14.sp
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = {
                  audio.playButtonClick()
                  onCheckUpdate()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(40.dp)
              ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Проверить", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }

              OutlinedButton(
                onClick = {
                  audio.playButtonClick()
                  onTestUpdate()
                },
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                modifier = Modifier.weight(1.2f).height(40.dp)
              ) {
                Text("Тест окна обновления", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = {
            audio.playButtonClick()
            onDismiss()
          },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
          modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
          Text(text = "ГОТОВО", color = Color.White, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

/**
 * In-game update dialogue asking the player:
 * «Эй, я нашёл новую версию, скачаем?»
 */
@Composable
fun UpdateDialog(
  updateInfo: UpdateInfo,
  currentVersionName: String,
  isDownloading: Boolean,
  progress: Float,
  downloadedBytes: Long,
  totalBytes: Long,
  errorMessage: String?,
  audio: RetroAudioEngine,
  onDownloadClick: () -> Unit,
  onOpenBrowserClick: () -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = {
    if (!isDownloading) onDismiss()
  }) {
    Box(
      modifier = Modifier
        .width(380.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(
          Brush.verticalGradient(
            listOf(Color(0xFF090D16), Color(0xFF0F172A), Color(0xFF1E1B4B))
          )
        )
        .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(24.dp))
        .padding(20.dp)
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
      ) {
        // Portal magic badge
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0x330284C7),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
          modifier = Modifier.padding(bottom = 10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.CloudDownload,
              contentDescription = null,
              tint = Color(0xFF38BDF8),
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "✦ ДОСТУПНО ОБНОВЛЕНИЕ ✦",
              color = Color(0xFF7DD3FC),
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp,
              letterSpacing = 1.sp
            )
          }
        }

        // Friendly quote requested by user!
        Text(
          text = "«Странник, доступно космическое обновление! Скачаем прямо сейчас?»",
          color = Color(0xFFFDE047),
          fontWeight = FontWeight.Black,
          fontSize = 18.sp,
          textAlign = androidx.compose.ui.text.style.TextAlign.Center,
          lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Versions chip
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x661E293B))
            .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Text(
            text = "Текущая: v$currentVersionName",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
          )
          Text("➔", color = Color(0xFF38BDF8), fontSize = 12.sp)
          Text(
            text = "Новая: v${updateInfo.versionName}",
            color = Color(0xFF34D399),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Changelog container
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x880A0F1D))
            .border(1.dp, Color(0x33475569), RoundedCornerShape(14.dp))
            .padding(12.dp)
        ) {
          Text(
            text = "Что нового:",
            color = Color(0xFFCBD5E1),
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = updateInfo.changelog,
            color = Color(0xFFE2E8F0),
            fontSize = 12.sp,
            lineHeight = 18.sp
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Progress section or error
        if (isDownloading) {
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            LinearProgressIndicator(
              progress = { progress },
              color = Color(0xFF38BDF8),
              trackColor = Color(0x3338BDF8),
              modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "Скачивание APK...",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
              )
              Text(
                text = "${(progress * 100).toInt()}%" +
                  if (totalBytes > 0) " (${String.format(Locale.US, "%.1f", downloadedBytes / 1048576f)} / ${String.format(Locale.US, "%.1f", totalBytes / 1048576f)} MB)"
                  else "",
                color = Color(0xFF38BDF8),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
              )
            }
          }
          Spacer(modifier = Modifier.height(12.dp))
        }

        if (errorMessage != null) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0x33EF4444),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(text = errorMessage, color = Color(0xFFFCA5A5), fontSize = 11.sp, lineHeight = 16.sp)
            }
          }
        }

        // Action buttons
        if (!isDownloading) {
          Button(
            onClick = {
              audio.playButtonClick()
              onDownloadClick()
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
            modifier = Modifier.fillMaxWidth().height(48.dp)
          ) {
            Icon(Icons.Default.GetApp, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("ДА, СКАЧИВАЕМ!", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = {
                audio.playButtonClick()
                onOpenBrowserClick()
              },
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
              modifier = Modifier.weight(1f).height(42.dp)
            ) {
              Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("В браузере", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
              onClick = {
                audio.playButtonClick()
                onDismiss()
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).height(42.dp)
            ) {
              Text("ПОЗЖЕ", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}


@Composable
fun PauseDialog(
  audio: RetroAudioEngine,
  onResume: () -> Unit,
  onRestartCheckpoint: () -> Unit,
  onRestartLevel: () -> Unit,
  onLevelSelect: () -> Unit,
  onMainMenu: () -> Unit
) {
  Dialog(onDismissRequest = onResume) {
    Box(
      modifier = Modifier
        .width(320.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(
          Brush.verticalGradient(
            listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
          )
        )
        .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(24.dp))
        .padding(20.dp)
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = "ПАУЗА",
          color = Color.White,
          fontWeight = FontWeight.Black,
          fontSize = 24.sp,
          letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
          onClick = {
            audio.playButtonClick()
            onResume()
          },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
          modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("ПРОДОЛЖИТЬ", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
          onClick = {
            audio.playButtonClick()
            onRestartCheckpoint()
          },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0x330284C7)),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
          modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF38BDF8))
          Spacer(modifier = Modifier.width(6.dp))
          Text("К КОНТРОЛЬНОЙ ТОЧКЕ", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
          onClick = {
            audio.playButtonClick()
            onRestartLevel()
          },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
          Text("НАЧАТЬ УРОВЕНЬ СНАЧАЛА", color = Color(0xFFE2E8F0), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              audio.playButtonClick()
              onLevelSelect()
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).height(42.dp)
          ) {
            Text("УРОВНИ", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }

          OutlinedButton(
            onClick = {
              audio.playButtonClick()
              onMainMenu()
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).height(42.dp)
          ) {
            Text("МЕНЮ", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun VictoryDialog(
  levelId: Int,
  levelTimeSeconds: Float,
  artifactsCount: Int,
  dustEarned: Int,
  hasNextLevel: Boolean,
  audio: RetroAudioEngine,
  onNextLevel: () -> Unit,
  onUpgrades: () -> Unit,
  onMainMenu: () -> Unit
) {
  val mins = (levelTimeSeconds / 60).toInt()
  val secs = (levelTimeSeconds % 60).toInt()
  val tenths = ((levelTimeSeconds * 10) % 10).toInt()
  val timeFormatted = String.format(Locale.US, "%02d:%02d.%d", mins, secs, tenths)

  Dialog(onDismissRequest = {}) {
    Box(
      modifier = Modifier
        .width(360.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(
          Brush.verticalGradient(
            listOf(Color(0xFF064E3B), Color(0xFF0F172A))
          )
        )
        .border(2.dp, Color(0xFF10B981), RoundedCornerShape(24.dp))
        .padding(22.dp)
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
      ) {
        // Glowing portal icon
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color(0xFF047857),
          modifier = Modifier.padding(bottom = 8.dp)
        ) {
          Text(
            text = "✦ ПОРТАЛ ПРОБУЖДЁН ✦",
            color = Color(0xFF6EE7B7),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
          )
        }

        Text(
          text = "УРОВЕНЬ ПРОЙДЕН!",
          color = Color.White,
          fontSize = 24.sp,
          fontWeight = FontWeight.Black,
          letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Stats Box
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x880F172A))
            .border(1.dp, Color(0x3364748B), RoundedCornerShape(16.dp))
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("✦", color = Color(0xFF00FFFF), fontSize = 16.sp)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Реликвии собрано", color = Color(0xFFCBD5E1), fontSize = 13.sp)
            }
            Text("$artifactsCount / $artifactsCount", color = Color(0xFF34D399), fontWeight = FontWeight.Bold)
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Время прохождения", color = Color(0xFFCBD5E1), fontSize = 13.sp)
            }
            Text(timeFormatted, color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFDE047), modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Звёздная Пыль", color = Color(0xFFCBD5E1), fontSize = 13.sp)
            }
            Text("+$dustEarned", color = Color(0xFFFDE047), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Action Buttons
        if (hasNextLevel) {
          Button(
            onClick = {
              audio.playButtonClick()
              onNextLevel()
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            modifier = Modifier.fillMaxWidth().height(48.dp)
          ) {
            Icon(Icons.Default.FastForward, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("СЛЕДУЮЩИЙ УРОВЕНЬ", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
          }

          Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              audio.playButtonClick()
              onUpgrades()
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).height(44.dp)
          ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("ПРОКАЧКА", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }

          OutlinedButton(
            onClick = {
              audio.playButtonClick()
              onMainMenu()
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).height(44.dp)
          ) {
            Text("МЕНЮ", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
