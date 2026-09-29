# AGENTS.md - DiceRoller Project Guide

Welcome to **DiceRoller**! This file provides essential guidelines, architectural constraints, repository conventions, and actionable workflows for AI coding agents modifying or extending this codebase.

---

## 🏗️ Project Architecture & Tech Stack

DiceRoller is a single-module Android application (`:app`) designed for tabletop gamers, RPG players, and D&D sessions. It is written entirely in **Kotlin** with modern **Jetpack Compose Material 3**.

- **App ID**: `developer.android.vd.diceroller`
- **Build System**: Gradle 9.7.1 via [`gradlew.bat`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/gradlew.bat) wrapper
- **Android Gradle Plugin (AGP)**: `9.4.1`
- **Kotlin Version**: `2.4.20` with Jetpack Compose Compiler Plugin (`org.jetbrains.kotlin.plugin.compose`)
- **Annotation Processing**: KSP `2.3.9` (`com.google.devtools.ksp`)
- **Compatibility**:
  - `minSdkVersion`: 26 (Android 8.0 Oreo - native multidex)
  - `targetSdkVersion`: 37
  - `compileSdkVersion`: 37
  - Version Code: `513`, Version Name: `"5.1.3"`
  - Java / JVM Target: JDK 17 (`JavaVersion.VERSION_17`)
- **Dependency Management**: Centralized version catalog in [`gradle/libs.versions.toml`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/gradle/libs.versions.toml)

### Key Libraries & Tooling
- **UI Toolkit**: Jetpack Compose Material 3 (BOM `2026.09.00`), Navigation / Activity Compose (`1.13.0`), Lifecycle ViewModel Compose (`2.11.0`)
- **Persistence**: AndroidX Room ORM `2.8.5` (KSP generated)
- **Monetization**: Google Play Billing Library `9.1.0`
- **Advertising**: Google Mobile Ads SDK (AdMob) `25.5.0`
- **Reviews**: Google Play In-App Review KTX `2.0.2`
- **Build Features**: `compose = true`, `resValues = true`, `buildConfig = true` (AdMob test IDs injected via `resValue`; `BuildConfig.VERSION_NAME` and `BuildConfig.VERSION_CODE` exposed dynamically)

---

## 📁 Repository Layout

```
DiceRoller/
├── AGENTS.md                                # Root agent guidance and instructions
├── README.md                                # Project overview and Play Store details
├── build.gradle                             # Root build configuration
├── settings.gradle                          # Settings and repository definitions
├── gradle/
│   ├── libs.versions.toml                   # Version catalog (dependencies & plugins)
│   └── wrapper/                             # Gradle 9.7.1 wrapper binaries
├── .agents/skills/                          # Specialized workflow skill guides
│   ├── android-gradle-build-and-test/       # Gradle compilation, KSP, R8, and testing
│   ├── audio-haptics-and-sensors/           # PCM audio synthesis, haptics, shake gesture
│   ├── compose-ui-and-theme-management/     # Compose M3 UI, themes, contrast, locking
│   ├── play-billing-and-monetization/       # PBL 9.1.0, trial timers, AdMob, reviews
│   └── room-database-and-history-management/# Room entities, DAO, repository, history UI
└── app/
    ├── build.gradle                         # App module build configuration
    ├── proguard-rules.pro                   # R8 release shrinking rules
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml          # Permissions, activities, metadata
        │   ├── java/developer/android/vd/diceroller/
        │   │   ├── MainActivity.kt          # Main screen, dice grid, roll animation, ads, review prompt
        │   │   ├── SettingsActivity.kt      # Settings UI, card-based theme picker, billing dialogs
        │   │   ├── HistoryActivity.kt       # Full roll history list, date headers, dice chips
        │   │   ├── MainViewModel.kt         # LiveData state holder for rolls and locks
        │   │   ├── MainViewModelFactory.kt  # ViewModel dependency injection factory
        │   │   ├── DiceUiState.kt           # Immutable UI state data class
        │   │   ├── DiceType.kt              # Enum: D4, D6, D8 (free), D10, D12, D20 (Pro)
        │   │   ├── PrefsHelper.kt           # Centralized SharedPreferences storage
        │   │   ├── SoundSynthesizer.kt      # Programmatic AudioTrack PCM sound synthesis
        │   │   ├── BillingManager.kt        # Play Billing 9.1.0 client and purchase flows
        │   │   ├── Security.kt              # Cryptographic RSA purchase signature check
        │   │   ├── ProStatusProvider.kt     # Pro entitlement query abstraction
        │   │   ├── AppDatabase.kt           # RoomDatabase definition (v1)
        │   │   ├── RollEntity.kt            # Room entity (@PrimaryKey timestamp: Long)
        │   │   ├── RollEntry.kt             # Domain roll history model
        │   │   ├── RollHistoryDao.kt        # Room DAO with insert, getLatest, clearAll
        │   │   ├── RollHistoryRepository.kt # History data repository (10k Pro vs 10 Free)
        │   │   ├── HistoryUiItem.kt         # Sealed class for History UI list items
        │   │   └── Theme.kt                 # Material 3 colors, typography, SystemBarsColor
        │   └── res/
        │       ├── drawable/                # Vector dice bases, icons
        │       ├── font/                    # Bebas, Aladin, Kabel custom fonts
        │       └── values/                  # Strings, colors, styles
        ├── test/                            # Local JVM unit tests
        └── androidTest/                     # Instrumented device tests
```

---

## ⚡ Developer Workflows & Commands

Run all commands using PowerShell in the repository root:

### 1. Build Debug APK
```powershell
.\gradlew.bat assembleDebug
```

### 2. Run Local Unit Tests
```powershell
.\gradlew.bat test
```

### 3. Run KSP Code Generation
Trigger Room annotation compilation explicitly:
```powershell
.\gradlew.bat kspDebugKotlin
```

### 4. Build Release Bundle / APK
```powershell
.\gradlew.bat assembleRelease
```
*Note: Release build enables R8 shrinking (`minifyEnabled = true`), resource shrinking (`shrinkResources = true`), embeds full NDK debug symbols (`ndk { debugSymbolLevel = 'FULL' }`), and requires release signing.*

### 5. Clean Build Artifacts
```powershell
.\gradlew.bat clean
```
*Note: Clean task uses `rootProject.layout.buildDirectory`.*

---

## 🏛️ Core Architectural Conventions

### 1. 100% Jetpack Compose UI
- All screen layouts are built with Jetpack Compose Material 3. Do **not** use legacy XML layout files (`setContentView(R.layout...)` is forbidden).
- Every Activity sets its content via `setContent { DiceRollerTheme { ... } }`.
- Status bar colors and icon luminance are controlled dynamically via [`SystemBarsColor`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/Theme.kt#L85-L97).
- Prefer `HorizontalDivider` over deprecated `Divider`.
- Total score in `MainScreen` uses `graphicsLayer { alpha = if (state.isRolling) 0f else 1f }` to avoid layout reflow during active rolls.
- Roll animations run for `300ms * durationScale` with `30ms * durationScale` stagger and `720f` rotation via `FastOutSlowInEasing`. Do **not** reintroduce `Modifier.blur` which caused GPU rendering issues.

### 2. State & ViewModel
- State is held in [`DiceUiState`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/DiceUiState.kt) and exposed via `LiveData` in [`MainViewModel`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/MainViewModel.kt).
- Composables observe state using `observeAsState()`.
- Dice counts range from 1 to 9 dice (`MIN_DICE = 1`, `MAX_DICE = 9`).

### 3. Pro Entitlements & Monetization
- **Lifetime Pro**: Persisted in [`PrefsHelper`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/PrefsHelper.kt) via `KEY_PRO_PURCHASED`. Product ID: `dice_roller_pro_lifetime`.
- **Temporary Pro Trial**: Rewarded video ad grants 6 hours of Pro (`activateProTrial()`). Remaining duration formatted via `formatRemainingTime(context)`.
- **Active Pro Check**: [`PrefsHelper.isProActive(context)`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/PrefsHelper.kt#L132-L139) returns `true` if lifetime Pro is owned OR current time is before the trial expiration timestamp.
- **Entitlement Tiers**:
  - **Free**: D4, D6, D8 (unlocked for all users); up to 10 roll history items (previewed in an in-app `ModalBottomSheet`); banner ads enabled (with 3-second delay on first launch for users with < 5 rolls).
  - **Pro (Trial or Lifetime)**: D10, D12, D20 unlocked; up to 10,000 roll history items (opens full [`HistoryActivity`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/HistoryActivity.kt)); banner ads removed.
  - **Lifetime Pro EXCLUSIVE**: Custom Dice Color Themes (Golden, Blood Red, Neon Green, Midnight Purple) require permanent lifetime purchase.
- **In-App Reviews**:
  - Scheduled after 15 initial rolls, then every 50 rolls (`REVIEW_INTERVAL = 50`), or triggered via 4+ stars in Settings.
  - Custom Compose prompt ("Enjoying Dice Roller? 🎲") with Review, Later (3-day delay), Never, and Dismiss (50-roll delay) actions.
  - Fast-dismiss (< 400ms duration) or API failure falls back to Play Store app listing.

### 4. Zero-Permission Audio & Haptics
- **Audio**: [`SoundSynthesizer.kt`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/SoundSynthesizer.kt) synthesizes PCM 16-bit sound programmatically at 22,050 Hz via Android `AudioTrack.MODE_STATIC`. Do **not** add WAV/MP3 files.
- **Haptics**: Tactile feedback is triggered through `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)` and `window.decorView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)`. Do **not** request `android.permission.VIBRATE` or use `Vibrator` service.
- **Shake-to-Roll**: Uses `Sensor.TYPE_ACCELEROMETER` with threshold `> 10f` and 2000ms debounce cooldown. Guarded by `PrefsHelper.isShakeToRollEnabled(context)` and `!state.isRolling`.

### 5. Room Database Schema Invariants
- Entity: [`RollEntity`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/RollEntity.kt) (table `roll_history`).
- Primary Key: `@PrimaryKey val timestamp: Long` (Unix millis). There is **no** auto-incrementing `id` column.
- Results Column: `val results: String` stores comma-separated numbers (e.g., `"3,5,1"`), serialized via `results.joinToString(",")` and deserialized via `results.split(",").map { it.toInt() }`. Do **not** assume JSON array formatting.

---

## 🚫 AI Agent Do's & Don'ts

| Do | Don't |
|---|---|
| Always add new dependencies to [`gradle/libs.versions.toml`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/gradle/libs.versions.toml) and reference them via `libs.*` aliases in [`app/build.gradle`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/build.gradle). | Do not hardcode dependency strings or version numbers directly inside `app/build.gradle`. |
| Respect the `minSdkVersion = 26` target. | Do not use APIs that require API 27+ without runtime checks. |
| Use `SystemBarsColor` and luminance calculations for contrast on custom backgrounds. | Do not hardcode black status bar text on dark backgrounds. |
| Respect the free status of D4, D6, and D8 dice types. | Do not lock D4, D6, or D8 behind Pro status. |
| Verify Room schema matches `@PrimaryKey val timestamp: Long` and comma-separated `results`. | Do not reference a non-existent `id` column or `resultsJson` in queries. |
| Keep tactile vibrations permission-free via View/Compose haptic mechanisms. | Do not introduce `android.permission.VIBRATE` to the manifest. |
| Run `.\gradlew.bat test` to validate changes before concluding tasks. | Do not push uncompiled or untested Kotlin changes. |
