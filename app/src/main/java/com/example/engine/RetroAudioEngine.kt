package com.example.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * High-performance 8-bit retro audio synthesizer and haptic engine.
 * Synthesizes square and triangle wave tones on-the-fly without needing audio assets.
 */
class RetroAudioEngine(context: Context) {
  private val appContext = context.applicationContext
  private val scope = CoroutineScope(Dispatchers.Default)

  var soundEnabled: Boolean = true
  var hapticsEnabled: Boolean = true

  private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
    vibratorManager?.defaultVibrator
  } else {
    @Suppress("DEPRECATION")
    appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
  }

  fun playJump() {
    vibrate(20, 70)
    if (!soundEnabled) return
    scope.launch {
      // Rising pitch chirp: 250Hz -> 550Hz in 80ms
      generateSweep(startFreq = 260f, endFreq = 580f, durationMs = 80, waveType = WaveType.SQUARE, volume = 0.25f)
    }
  }

  fun playDoubleJump() {
    vibrate(30, 90)
    if (!soundEnabled) return
    scope.launch {
      // High rising double chirp: 500Hz -> 900Hz in 90ms
      generateSweep(startFreq = 500f, endFreq = 950f, durationMs = 90, waveType = WaveType.TRIANGLE, volume = 0.3f)
    }
  }

  fun playDash() {
    vibrate(40, 150)
    if (!soundEnabled) return
    scope.launch {
      // Noise/whoosh dash sound
      generateNoiseBurst(durationMs = 110, volume = 0.28f)
    }
  }

  fun playArtifactPickup() {
    vibrate(45, 120)
    if (!soundEnabled) return
    scope.launch {
      // Ascending arpeggio: C5 (523), E5 (659), G5 (784), C6 (1046)
      playTone(523f, 40, WaveType.TRIANGLE, 0.3f)
      playTone(659f, 40, WaveType.TRIANGLE, 0.32f)
      playTone(784f, 40, WaveType.TRIANGLE, 0.34f)
      playTone(1046f, 90, WaveType.SQUARE, 0.35f)
    }
  }

  fun playPortalUnlock() {
    vibrate(100, 200)
    if (!soundEnabled) return
    scope.launch {
      // Harmonic portal awakening sound
      playTone(440f, 60, WaveType.TRIANGLE, 0.35f)
      playTone(554f, 60, WaveType.TRIANGLE, 0.38f)
      playTone(659f, 60, WaveType.TRIANGLE, 0.4f)
      playTone(880f, 180, WaveType.SQUARE, 0.42f)
    }
  }

  fun playHurt() {
    vibrate(80, 220)
    if (!soundEnabled) return
    scope.launch {
      // Descending buzz: 320Hz -> 90Hz in 120ms
      generateSweep(startFreq = 340f, endFreq = 90f, durationMs = 130, waveType = WaveType.SQUARE, volume = 0.35f)
    }
  }

  fun playLevelComplete() {
    vibrate(150, 180)
    if (!soundEnabled) return
    scope.launch {
      // Victory Fanfare
      val notes = listOf(
        Pair(523f, 90), // C5
        Pair(659f, 90), // E5
        Pair(784f, 90), // G5
        Pair(1046f, 220) // C6
      )
      for ((freq, dur) in notes) {
        playTone(freq, dur, WaveType.SQUARE, 0.35f)
      }
    }
  }

  fun playChronoShift() {
    vibrate(50, 100)
    if (!soundEnabled) return
    scope.launch {
      // Deep resonant pulse
      generateSweep(startFreq = 700f, endFreq = 180f, durationMs = 180, waveType = WaveType.SINE, volume = 0.4f)
    }
  }

  fun playButtonClick() {
    vibrate(12, 60)
    if (!soundEnabled) return
    scope.launch {
      playTone(750f, 25, WaveType.TRIANGLE, 0.18f)
    }
  }

  private fun vibrate(durationMs: Long, amplitude: Int = 100) {
    if (!hapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
      } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(durationMs)
      }
    } catch (_: Exception) {
      // Ignore vibration errors on restrictive devices
    }
  }

  private enum class WaveType { SQUARE, TRIANGLE, SINE }

  private fun playTone(freq: Float, durationMs: Int, waveType: WaveType, volume: Float) {
    val sampleRate = 22050
    val numSamples = (sampleRate * (durationMs / 1000f)).toInt().coerceAtLeast(10)
    val buffer = ShortArray(numSamples)

    for (i in 0 until numSamples) {
      val t = i.toFloat() / sampleRate
      val phase = (t * freq) % 1.0f
      // Envelope decay
      val envelope = (1.0f - (i.toFloat() / numSamples)).coerceIn(0f, 1f)
      val sample = when (waveType) {
        WaveType.SQUARE -> if (phase < 0.5f) 1f else -1f
        WaveType.TRIANGLE -> (4f * kotlin.math.abs(phase - 0.5f) - 1f)
        WaveType.SINE -> sin(2.0 * Math.PI * phase).toFloat()
      }
      buffer[i] = (sample * volume * envelope * Short.MAX_VALUE).toInt().toShort()
    }
    playBuffer(buffer, sampleRate)
  }

  private fun generateSweep(startFreq: Float, endFreq: Float, durationMs: Int, waveType: WaveType, volume: Float) {
    val sampleRate = 22050
    val numSamples = (sampleRate * (durationMs / 1000f)).toInt().coerceAtLeast(10)
    val buffer = ShortArray(numSamples)
    var phase = 0f

    for (i in 0 until numSamples) {
      val progress = i.toFloat() / numSamples
      val currentFreq = startFreq + (endFreq - startFreq) * progress
      phase += currentFreq / sampleRate
      if (phase >= 1f) phase -= 1f

      val envelope = (1.0f - progress * 0.4f).coerceIn(0f, 1f)
      val sample = when (waveType) {
        WaveType.SQUARE -> if (phase < 0.5f) 1f else -1f
        WaveType.TRIANGLE -> (4f * kotlin.math.abs(phase - 0.5f) - 1f)
        WaveType.SINE -> sin(2.0 * Math.PI * phase).toFloat()
      }
      buffer[i] = (sample * volume * envelope * Short.MAX_VALUE).toInt().toShort()
    }
    playBuffer(buffer, sampleRate)
  }

  private fun generateNoiseBurst(durationMs: Int, volume: Float) {
    val sampleRate = 22050
    val numSamples = (sampleRate * (durationMs / 1000f)).toInt().coerceAtLeast(10)
    val buffer = ShortArray(numSamples)
    var seed = 0x54321

    for (i in 0 until numSamples) {
      // Linear congruential generator for fast 8-bit noise
      seed = (seed * 1103515245 + 12345) and 0x7fffffff
      val noiseVal = ((seed shr 16) and 0xFFFF) / 32768.0f - 1.0f
      val envelope = (1.0f - (i.toFloat() / numSamples)).coerceIn(0f, 1f)
      buffer[i] = (noiseVal * volume * envelope * Short.MAX_VALUE).toInt().toShort()
    }
    playBuffer(buffer, sampleRate)
  }

  private fun playBuffer(buffer: ShortArray, sampleRate: Int) {
    try {
      val track = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        )
        .setBufferSizeInBytes(buffer.size * 2)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()

      track.write(buffer, 0, buffer.size)
      track.play()
      // Release track when done
      track.notificationMarkerPosition = buffer.size
      track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
        override fun onMarkerReached(t: AudioTrack?) {
          try {
            t?.stop()
            t?.release()
          } catch (_: Exception) {}
        }
        override fun onPeriodicNotification(t: AudioTrack?) {}
      })
    } catch (_: Exception) {
      // Audio fallback
    }
  }
}
