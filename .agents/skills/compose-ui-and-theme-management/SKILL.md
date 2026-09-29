---
name: compose-ui-and-theme-management
description: >-
  Use this skill when modifying or creating Jetpack Compose UI components, handling dynamic background/foreground color contrast,
  updating dice renderers and Yahtzee-style lock badges, managing animations, or customizing themes in DiceRoller.
---

# Jetpack Compose UI & Theme Management Skill

This skill guides the implementation, modification, and maintenance of Jetpack Compose UI components, dynamic theme contrast, Yahtzee locking, and custom dice rendering in **DiceRoller**.

---

## 🎨 UI & Theme Architecture

- **Material 3 BOM**: `2026.09.00` (`libs.androidx.compose.bom`).
- **Activity & Lifecycle Compose**: Activity Compose `1.13.0`, Lifecycle ViewModel Compose `2.11.0`.
- **Typography & Theme**: Defined in [`Theme.kt`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/Theme.kt) using [`DiceRollerTheme`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/Theme.kt#L74-L82).
- **Custom Fonts**: Loaded via `FontFamily`:
  - `BebasFontFamily` (`R.font.bebas`)
  - `AladinFontFamily` (`R.font.aladin`)
  - `KabelFontFamily` (`R.font.kabel`)
- **State Management**: Reactive UI state is defined in [`DiceUiState`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/DiceUiState.kt) and observed via `LiveData` in [`MainViewModel`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/MainViewModel.kt).

---

## 📱 Activity Screens & Responsibilities

### 1. [`MainActivity.kt`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/MainActivity.kt) (`MainScreen`)
- **Header Bar**: Displays app title, Pro status / upgrade gift icon, and Settings navigation button.
- **Dice Count Pill**: Pill selector adjusting active dice count between 1 and 9 dice.
- **Main Elevated Card**:
  - 3x3 `DiceGrid` rendering interactive dice views.
  - Roll total sum counter with `graphicsLayer { alpha = if (state.isRolling) 0f else 1f }` to avoid UI jumpiness during rolls.
  - Polyhedral dice selector chips (`D4`, `D6`, `D8` unlocked for all; `D10`, `D12`, `D20` marked with lock badge for non-Pro).
  - "View History" button:
    - **Free Users**: Opens a `ModalBottomSheet` displaying the last 10 rolls with an upsell card offering 6h trial (via ad) or Lifetime Pro.
    - **Pro Users**: Directly opens [`HistoryActivity`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/HistoryActivity.kt).
- **Roll Action**: Full-width gradient "R O L L" button.
- **Review Dialog**: Custom Compose `AlertDialog` ("Enjoying Dice Roller? 🎲") with Review ⭐️, Later 🕒, and Never 🚫 buttons.
- **Pro Status Banner**: Visual gradient card showing trial countdown timer or upgrade prompts.
- **Banner Ad**: AdMob container docked at bottom (suppressed for Pro users, delayed 3 seconds for users with < 5 rolls).

### 2. [`SettingsActivity.kt`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/SettingsActivity.kt) (`SettingsScreen`)
Card-based architecture organized into four primary sections:
- **Header Bar**: Back button and Pro / Trial status chip.
- **Section 1: Pro Membership Hero Card (`ProMembershipCard`)**:
  - *Lifetime Active*: Gold-bordered VIP card confirming permanent access to all dice, history, and themes.
  - *Trial Active*: Live countdown timer (`Expires in: %02dh %02dm %02ds`), upgrade button, and restore purchases button.
  - *Non-Pro*: Feature breakdown (D10-D20, unlimited history, ad-free, exclusive themes), Watch Ad (6h), Buy Lifetime, and Restore.
- **Section 2: Appearance & Themes**:
  - *App Background Palette*: 3x2 grid of selectable color themes with real-time luminance contrast adaptation.
  - *Dice Face Theme*: Row of 5 dice styles with miniature 3D die previews, pip contrast adaptation, and lock overlays for non-lifetime users.
- **Section 3: Gameplay Preferences**:
  - Custom `SettingsSwitchRow` toggles with icons for Roll Total (`PrefsHelper.isTotalHidden`), Dice Clack Audio (`PrefsHelper.isSoundEffectsEnabled`), Tactile Vibration (`PrefsHelper.isVibrationEnabled`), and Shake to Roll (`PrefsHelper.isShakeToRollEnabled`).
- **Section 4: Support & Community**:
  - Interactive 5-star rating (>= 4 stars triggers Google Play review flow).
  - "Explore Our Other Apps" developer button with Chrome Custom Tabs fallback.
  - Dynamic app version credit (`Dice Roller v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})`).

### 3. [`HistoryActivity.kt`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/HistoryActivity.kt) (`HistoryScreen`)
- Material 3 `Scaffold` with `TopAppBar`.
- `LazyColumn` rendering `HistoryUiItem` items:
  - `HistoryUiItem.ProHeader`: Explains unlimited history entitlement.
  - `HistoryUiItem.DateHeader`: Sticky date separators ("Today", "Yesterday", or formatted dates).
  - `HistoryUiItem.Roll`: Visual entry displaying dice type chip, dice count, custom dice vector drawables for each rolled die, and total score.
  - `HistoryUiItem.Empty`: Placeholder with dice emoji when no rolls exist.

---

## 🔒 Yahtzee-Style Dice Locking

- **State**: Tracked in `DiceUiState.lockedIndices: Set<Int>`.
- **Toggle Action**: Invoked via `MainViewModel.toggleDiceLock(index)`. Disabled during active rolling animations (`clickable(enabled = !isRolling && value > 0)`).
- **Rendering in `DiceView`**:
  - **Background**: Locked dice render with `#FFF0F6` (light blush tint); unlocked dice use `#FAFAFA`.
  - **Border**: Locked dice display a `2.5.dp` border in `ColorAccent` (`#EC4899`); unlocked dice display a `1.5.dp` border in `#E5E7EB`.
  - **Badge**: A lock icon (`R.drawable.ic_lock`) appears at the top-end corner of locked dice.
- **Behavior During Roll**: Locked dice preserve their current value; unlocked dice reroll a random number between `1` and `diceType.sides`.

---

## 🌓 Dynamic Contrast & Status Bar System

Users can select from 6 background palettes:
1. **Slate Dark 🌑** (`#0F172A`)
2. **Nordic Light ❄️** (`#F0F4F8`)
3. **Soft Lavender 🪻** (`#F5F3FF`)
4. **Mint Breeze 🍃** (`#ECFDF5`)
5. **Peach Cream 🍑** (`#FFF7ED`)
6. **Classic White 🏳️** (`Color.WHITE`)

### Contrast Calculation
```kotlin
val darkness = 1 - (0.299 * Color.red(bg) + 0.587 * Color.green(bg) + 0.114 * Color.blue(bg)) / 255
val isDark = darkness >= 0.5
```
- **Text & Icons**: Automatically switch between `ComposeColor.White` / `DarkTextSecondary` and `TextPrimary` / `TextSecondary`.
- **System Status Bars**: Handled dynamically via [`SystemBarsColor`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/Theme.kt#L85-L97):
  ```kotlin
  SystemBarsColor(
      statusBarColor = backgroundColor,
      darkIcons = !isDark
  )
  ```

---

## 🎲 Lifetime Pro Custom Dice Themes

Lifetime Pro owners can choose from 5 face themes:
- **Classic (White)**: `Color.TRANSPARENT`
- **Golden 🏆**: `#FFD700`
- **Blood Red 🩸**: `#8B0000`
- **Neon Green 🍏**: `#39FF14`
- **Midnight Purple 🍇**: `#4B0082`

The face text/pip color automatically adapts based on luminance:
```kotlin
val luminance = 0.299 * tint.red + 0.587 * tint.green + 0.114 * tint.blue
val textColor = if (luminance < 0.5f) ComposeColor.White else ComposeColor.Black
```

### Reactive State Synchronization
Dice themes are managed reactively via `diceColorState = mutableStateOf(Color.TRANSPARENT)` in `MainActivity` and re-synced in `applySavedSettings()` (`onResume()` / `settingsLauncher`). The state is passed down as `diceColor: Int` through `MainScreen` -> `DiceGrid` -> `DiceView(..., remember(isLifetimePro, isLocked, diceColor))`, ensuring that changes made in `SettingsActivity` update all visible dice immediately upon returning to the main screen without requiring a dice count change or reroll.

---

## 🎬 Roll Animation Orchestration

Inside `MainScreen`, rolling animations are orchestrated using Compose `Animatable`:
- **Scale Pulse**: Scales to `1.1f` and returns to `1.0f`.
- **Rotation**: Rotates to `720f` with `FastOutSlowInEasing`, then snaps back to `0f`.
- **Duration & Stagger**: `rollDuration = (300L * durationScale).toInt()`, `staggerDelay = (30L * durationScale).toLong()`.
- **No Blur Modifier**: `Modifier.blur` was eliminated to guarantee crisp rendering across all Android devices and GPU chipsets.
- **Audio-Tactile Sync**: At each die's stagger pulse, `SoundSynthesizer.playDiceClack` and haptic clicks are triggered.
- **Completion**: Calls `viewModel.onRollAnimationFinished()` and checks `maybeAskForReviewAfterValue()`.

---

## 📐 Best Practices for Compose Edits

1. **State Hoisting**: Keep composables stateless by passing down states and event lambdas.
2. **Material 3 Dividers**: Use `HorizontalDivider` instead of deprecated `Divider`.
3. **Contrast Testing**: Always test UI layouts against both **Slate Dark** and **Nordic Light**.
4. **No XML Layouts**: Do not introduce Android View XML layouts; all UI must be written in Compose.
