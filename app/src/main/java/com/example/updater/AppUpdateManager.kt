package com.example.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.data.GamePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class UpdateInfo(
  val versionCode: Int,
  val versionName: String,
  val downloadUrl: String,
  val changelog: String
)

class AppUpdateManager(private val context: Context, private val prefs: GamePreferences) {
  private val client = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .followRedirects(true)
    .followSslRedirects(true)
    .build()

  val currentVersionCode: Int = BuildConfig.VERSION_CODE
  val currentVersionName: String = BuildConfig.VERSION_NAME

  /**
   * Checks for remote update.
   */
  fun checkForUpdate(
    isManual: Boolean = false,
    onResult: (UpdateInfo?) -> Unit,
    onError: ((String) -> Unit)? = null
  ) {
    val rawUrl = prefs.customUpdateUrl.trim()
    val url = if (rawUrl.isNotBlank()) {
      rawUrl
    } else {
      // Default GitHub releases API endpoint
      "https://api.github.com/repos/mishamine16/pixel-rift/releases/latest"
    }

    CoroutineScope(Dispatchers.IO).launch {
      // If user provided a direct APK link
      if (url.endsWith(".apk", ignoreCase = true)) {
        withContext(Dispatchers.Main) {
          onResult(
            UpdateInfo(
              versionCode = currentVersionCode + 1,
              versionName = "новейшая",
              downloadUrl = url,
              changelog = "• Прямое скачивание APK по указанной ссылке сети\n• Новые уровни и оптимизация"
            )
          )
        }
        return@launch
      }

      try {
        val request = Request.Builder()
          .url(url)
          .header("User-Agent", "PixelRift-Android/${currentVersionName}")
          .build()

        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
          val body = response.body?.string()
          if (!body.isNullOrBlank()) {
            val trimmed = body.trim()
            if (trimmed.startsWith("{")) {
              val json = JSONObject(trimmed)

              // Check if it's GitHub release API response
              if (json.has("assets") && json.optJSONArray("assets") != null) {
                val assets = json.getJSONArray("assets")
                var apkUrl = ""
                for (i in 0 until assets.length()) {
                  val asset = assets.getJSONObject(i)
                  val name = asset.optString("name", "")
                  if (name.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url", "")
                    break
                  }
                }

                val tag = json.optString("tag_name", "v1.1.0")
                val notes = json.optString("body", "Новое обновление игры!")

                if (apkUrl.isNotBlank()) {
                  withContext(Dispatchers.Main) {
                    onResult(
                      UpdateInfo(
                        versionCode = currentVersionCode + 1,
                        versionName = tag.removePrefix("v"),
                        downloadUrl = apkUrl,
                        changelog = notes
                      )
                    )
                  }
                  return@launch
                }
              }

              // Standard version.json format
              val remoteCode = json.optInt("versionCode", currentVersionCode)
              val remoteName = json.optString("versionName", "1.1.0")
              val dlUrl = json.optString("downloadUrl", "")
              val notes = json.optString(
                "changelog",
                "• Новая версия игры\n• Исправление физики и производительности\n• Новые кастомные уровни"
              )

              if ((remoteCode > currentVersionCode || isManual) && dlUrl.isNotBlank()) {
                withContext(Dispatchers.Main) {
                  onResult(UpdateInfo(remoteCode, remoteName, dlUrl, notes))
                }
                return@launch
              }
            }
          }
        } else {
          if (isManual) {
            withContext(Dispatchers.Main) {
              onError?.invoke("Сервер обновлений вернул код: ${response.code}")
            }
            return@launch
          }
        }
      } catch (e: Exception) {
        if (isManual) {
          withContext(Dispatchers.Main) {
            onError?.invoke("Не удалось подключиться к серверу обновлений: ${e.localizedMessage ?: "ошибка сети"}")
          }
          return@launch
        }
      }

      withContext(Dispatchers.Main) {
        onResult(null)
      }
    }
  }

  /**
   * Generates a sample update notice for instant testing of the update UI.
   */
  fun getTestUpdateInfo(): UpdateInfo {
    return UpdateInfo(
      versionCode = currentVersionCode + 1,
      versionName = "1.1.2",
      downloadUrl = prefs.customUpdateUrl.ifBlank {
        "https://github.com/mishamine16/pixel-rift/releases/latest/download/PixelRift.apk"
      },
      changelog = "• «Странник, доступно космическое обновление! Скачаем прямо сейчас?»\n• Добавлен Лучевой телепорт (1-2 блока вперед, перезарядка 5 сек)\n• Настройка размера карты в редакторе (до 400x400 блоков)\n• Падение в бездну = моментальная гибель\n• Версия игры обновлена до 1.1.1"
    )
  }

  /**
   * Downloads APK from URL and triggers Android Package Installer via FileProvider.
   */
  fun downloadAndInstall(
    updateInfo: UpdateInfo,
    onProgress: (progress: Float, loadedBytes: Long, totalBytes: Long) -> Unit,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
  ) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val updateDir = File(context.cacheDir, "updates")
        if (!updateDir.exists()) updateDir.mkdirs()
        val apkFile = File(updateDir, "PixelRift_v${updateInfo.versionName}.apk")
        if (apkFile.exists()) apkFile.delete()

        val request = Request.Builder()
          .url(updateInfo.downloadUrl)
          .header("User-Agent", "PixelRift-Android/${currentVersionName}")
          .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
          withContext(Dispatchers.Main) {
            onError("Ошибка загрузки файла: HTTP ${response.code}")
          }
          return@launch
        }

        val body = response.body ?: throw Exception("Сервер вернул пустой ответ")
        val totalBytes = body.contentLength()
        val inputStream = body.byteStream()
        val outputStream = FileOutputStream(apkFile)

        val buffer = ByteArray(8192)
        var bytesRead: Int
        var downloadedBytes = 0L

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
          outputStream.write(buffer, 0, bytesRead)
          downloadedBytes += bytesRead
          val progress = if (totalBytes > 0) {
            (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
          } else {
            0.5f // Indeterminate if length unknown
          }
          withContext(Dispatchers.Main) {
            onProgress(progress, downloadedBytes, totalBytes)
          }
        }

        outputStream.flush()
        outputStream.close()
        inputStream.close()

        withContext(Dispatchers.Main) {
          onProgress(1f, downloadedBytes, totalBytes)
          onSuccess()
          installApk(apkFile)
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          onError("Не удалось скачать обновление: ${e.localizedMessage ?: "ошибка сети"}")
        }
      }
    }
  }

  fun installApk(apkFile: File) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (!context.packageManager.canRequestPackageInstalls()) {
          val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
          context.startActivity(settingsIntent)
          // Continue to attempt install as well
        }
      }

      val apkUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        apkFile
      )

      val installIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(installIntent)
    } catch (_: Exception) {
      // Fallback
    }
  }

  fun openInBrowser(url: String) {
    try {
      val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(browserIntent)
    } catch (_: Exception) {}
  }
}
