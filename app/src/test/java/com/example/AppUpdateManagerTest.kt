package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.GamePreferences
import com.example.updater.AppUpdateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppUpdateManagerTest {

  private lateinit var context: Context
  private lateinit var prefs: GamePreferences
  private lateinit var updateManager: AppUpdateManager

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    prefs = GamePreferences(context)
    updateManager = AppUpdateManager(context, prefs)
  }

  @Test
  fun testTestUpdateInfoContainsRequiredPhraseAndHigherVersion() {
    val info = updateManager.getTestUpdateInfo()
    assertNotNull(info)
    assertTrue("Version code must be greater than current", info.versionCode > updateManager.currentVersionCode)
    assertTrue("Changelog must contain prompt phrase", info.changelog.contains("Странник, доступно космическое обновление! Скачаем прямо сейчас?"))
    assertTrue("Download URL must not be blank", info.downloadUrl.isNotBlank())
  }

  @Test
  fun testPreferencesCustomUrlPersistence() {
    prefs.customUpdateUrl = "https://example.com/test.apk"
    assertEquals("https://example.com/test.apk", prefs.customUpdateUrl)

    prefs.autoCheckUpdates = false
    assertEquals(false, prefs.autoCheckUpdates)
    prefs.autoCheckUpdates = true
    assertEquals(true, prefs.autoCheckUpdates)
  }
}
