---
name: android-gradle-build-and-test
description: >-
  Use this skill when building, compiling, running unit/instrumented tests,
  updating Gradle dependencies in libs.versions.toml, or troubleshooting build issues in the DiceRoller project.
---

# Android Gradle Build and Test Skill

This skill provides step-by-step instructions for building, compiling, testing, and managing dependencies for the **DiceRoller** Android project.

---

## 🛠️ Project Environment & Specs

- **Gradle Version**: `9.7.1` (managed via [`gradlew.bat`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/gradlew.bat) wrapper)
- **Android Gradle Plugin (AGP)**: `9.4.1`
- **Kotlin Version**: `2.4.20` with Compose compiler plugin (`org.jetbrains.kotlin.plugin.compose`)
- **KSP Version**: `2.3.9` (`com.google.devtools.ksp`)
- **Target Java Compatibility**: JDK 17 (`JavaVersion.VERSION_17`)
- **SDK Target**: `compileSdkVersion = 37`, `targetSdkVersion = 37`, `minSdkVersion = 26` (native multidex)
- **App Version**: `versionCode = 513`, `versionName = "5.1.3"`
- **Dependency Catalog**: [`gradle/libs.versions.toml`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/gradle/libs.versions.toml)

---

## 🚀 Common Developer Workflows

Execute all commands using PowerShell from the project root:

### 1. Build Debug APK
Build the debug version of the app:
```powershell
.\gradlew.bat assembleDebug
```

### 2. Build Release APK / App Bundle
Build the optimized, minified release artifacts:
```powershell
.\gradlew.bat assembleRelease
```
*Note: Release builds enable R8 shrinking (`minifyEnabled = true`), resource shrinking (`shrinkResources = true`), embed full NDK debug symbols (`ndk { debugSymbolLevel = 'FULL' }`), and apply rules from [`app/proguard-rules.pro`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/proguard-rules.pro).*

### 3. Run Local JVM Unit Tests
Run JVM unit tests located in `app/src/test/`:
```powershell
.\gradlew.bat test
```

### 4. Run Instrumented Android Tests
Run UI and device integration tests (requires connected physical device or running emulator):
```powershell
.\gradlew.bat connectedAndroidTest
```

### 5. Run KSP Code Generation
Explicitly trigger KSP code generation (Room DAO compiler):
```powershell
.\gradlew.bat kspDebugKotlin
```

### 6. Clean Build Artifacts
Delete all build output directories:
```powershell
.\gradlew.bat clean
```
*Note: Clean task uses `rootProject.layout.buildDirectory`.*

---

## 📦 Managing Dependencies (`libs.versions.toml`)

All dependencies are centralized in the TOML version catalog:
1. Open [`gradle/libs.versions.toml`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/gradle/libs.versions.toml).
2. Add or update the version string under `[versions]`.
3. Add the library coordinates under `[libraries]`.
4. Reference the alias in [`app/build.gradle`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/build.gradle) (e.g. `implementation libs.androidx.room.runtime`).

> [!NOTE]
> The app was migrated to 100% Jetpack Compose Material 3. Legacy View-based libraries (`material`, `constraintlayout`, `fragment-ktx`, `multidex`) have been removed from dependencies. Native multidex is supported automatically by Android 8.0+ (API 26+).
> Build features include `compose = true`, `resValues = true`, and `buildConfig = true`. Official AdMob test IDs are configured via `resValue` in `debug` and `release` build types, while `BuildConfig.VERSION_NAME` and `BuildConfig.VERSION_CODE` are dynamically exposed for in-app version displays.

---

## 🔍 Build Verification Checklist

After making modifications to build configurations or code:
1. Run `.\gradlew.bat assembleDebug` to verify compilation.
2. Run `.\gradlew.bat test` to verify unit tests pass.
3. Check that no KSP annotation compiler errors were generated.
