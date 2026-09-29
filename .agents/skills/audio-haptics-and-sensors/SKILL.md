---
name: audio-haptics-and-sensors
description: >-
  Use this skill when working on programmatic PCM audio synthesis, shake-to-roll accelerometer gesture handling,
  or zero-permission tactile haptic feedback in DiceRoller.
---

# Audio, Haptics & Sensors Skill

This skill explains the programmatic audio synthesizer engine, shake gesture detection via accelerometer sensors, and zero-permission tactile haptic feedback in **DiceRoller**.

---

## 🔊 Programmatic Audio Synthesis (`SoundSynthesizer.kt`)

Rather than relying on static WAV/MP3 asset files that inflate APK size, DiceRoller synthesizes sound effects in real time using the Android `AudioTrack` API.

### Technical Implementation Details:
- **File**: [`SoundSynthesizer.kt`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/SoundSynthesizer.kt)
- **Audio Configuration**:
  - Sample Rate: `22,050 Hz`
  - Audio Format: `AudioFormat.ENCODING_PCM_16BIT`
  - Channel: `AudioFormat.CHANNEL_OUT_MONO`
  - Mode: `AudioTrack.MODE_STATIC`
  - Duration: `80 ms` (`numSamples = 1764`)
- **Signal Synthesis Formula**:
  - **Tone**: Sine wave generator at `950.0 Hz` (\( \sin(2\pi \cdot 950.0 \cdot t) \)).
  - **Noise**: Uniform white noise generator producing values between `[-1.0, 1.0]`.
  - **Decay Envelope**: Exponential decay curve \( \text{envelope} = e^{-t \cdot 45.0} \).
  - **Combined Signal**: \( (0.85 \times \text{sine} + 0.15 \times \text{noise}) \times \text{envelope} \times 28000.0 \) (coerced into `Short` range `[-32768, 32767]`).
- **Execution Threading**: Runs asynchronously inside a background `Thread` to avoid UI thread latency or audio buffer underruns.
- **Preference Guard**: Checks [`PrefsHelper.isSoundEffectsEnabled(context)`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/PrefsHelper.kt#L116-L124) before triggering playback. Also played when enabling sound effects in Settings or picking an unlocked dice theme.

---

## 📳 Zero-Permission Tactile Haptic Feedback

> [!IMPORTANT]
> The app **intentionally avoids** `android.permission.VIBRATE` and does **not** bind to the system `Vibrator` or `VibratorManager` service. This eliminates unnecessary permission warnings in the Play Store while ensuring broad device compatibility.

Tactile clicks are achieved via native View and Compose haptic APIs:
1. **In Jetpack Compose (`MainScreen`, `SettingsScreen`)**:
   Uses `LocalHapticFeedback.current`:
   ```kotlin
   val haptic = LocalHapticFeedback.current
   if (PrefsHelper.isVibrationEnabled(context)) {
       haptic.performHapticFeedback(HapticFeedbackType.LongPress)
   }
   ```
2. **In Android Activity (`MainActivity`)**:
   Uses the window decor view for virtual key taps:
   ```kotlin
   if (PrefsHelper.isVibrationEnabled(this)) {
       window.decorView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
   }
   ```
- **Haptic Touchpoints**:
  - Die roll completion & stagger pulses.
  - Yahtzee die locking / unlocking.
  - Background palette selection in Settings.
  - Dice face theme selection in Settings.
  - Interactive star rating clicks in Settings.
  - Toggle switch activation in Settings.
- **Preference Guard**: Controlled via [`PrefsHelper.isVibrationEnabled(context)`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/PrefsHelper.kt#L96-L105).

---

## 📱 Shake-to-Roll Accelerometer Detection

- **Sensor Type**: `Sensor.TYPE_ACCELEROMETER`.
- **Sampling Rate**: `SensorManager.SENSOR_DELAY_UI`.
- **Activity Lifecycle Management**:
  - Registered in `MainActivity.onResume()`.
  - Unregistered in `MainActivity.onPause()` to prevent battery drain.
- **Detection Algorithm**:
  ```kotlin
  currentAcceleration = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
  val delta = Math.abs(currentAcceleration - lastAcceleration)
  acceleration = acceleration * 0.9f + delta
  ```
  - **Threshold**: Triggers roll when `acceleration > 10f`.
- **Debounce Cooldown**: Enforces a minimum cooldown of **2000 ms** (`2.0s`) between shake rolls:
  ```kotlin
  if (now - lastShakeTime > 2000L) {
      lastShakeTime = now
      runOnUiThread { triggerRoll() }
  }
  ```
- **Preference Guard & State Check**: Only triggers when `PrefsHelper.isShakeToRollEnabled(context)` is `true` and `state.isRolling` is `false`.
