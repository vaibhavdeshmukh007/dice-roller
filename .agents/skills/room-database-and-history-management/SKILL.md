---
name: room-database-and-history-management
description: >-
  Use this skill when modifying the Room database schema, adding database migrations, querying roll history,
  or managing history repository operations in DiceRoller.
---

# Room Database & History Management Skill

This skill details the Room ORM architecture, database schema, data transformations, and repository workflows for **DiceRoller**.

---

## 🗄️ Database Architecture

- **Room Version**: `2.8.5` (`libs.androidx.room.runtime`, `libs.androidx.room.ktx`, `libs.androidx.room.compiler`).
- **Database Class**: [`AppDatabase`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/AppDatabase.kt) (`dice_roller.db`, database version `1`, `exportSchema = false`).
- **Entity**: [`RollEntity`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/RollEntity.kt) (table: `roll_history`).
- **DAO**: [`RollHistoryDao`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/RollHistoryDao.kt).
- **Repository**: [`RollHistoryRepository`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/RollHistoryRepository.kt).

---

## 📊 Data Model & Transformations

### 1. `RollEntity` (Persistence Model)
Located in [`RollEntity.kt`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/RollEntity.kt):
```kotlin
@Entity(tableName = "roll_history")
data class RollEntity(
    @PrimaryKey
    val timestamp: Long,
    val diceType: String,
    val diceCount: Int,
    val results: String, // Comma-separated integers, e.g. "3,5,1"
    val total: Int
)
```
> [!IMPORTANT]
> The `@PrimaryKey` is `timestamp: Long`. There is **no** separate auto-incrementing `id` column.
> The `results` column contains comma-separated values (e.g. `"3,5,1"`), **not** a JSON array string.

### 2. `RollEntry` (Domain Model)
Located in [`RollEntry.kt`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/RollEntry.kt):
```kotlin
data class RollEntry(
    val timestamp: Long,
    val diceType: DiceType,
    val diceCount: Int,
    val results: List<Int>,
    val total: Int
)
```

**Mappers:**
- `RollEntity.toDomain()`: Deserializes `results` using `results.split(",").map { it.toInt() }`.
- `RollEntry.toEntity()`: Serializes `results` using `results.joinToString(",")`.

### 3. `HistoryUiItem` (UI Model)
Located in [`HistoryUiItem.kt`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/HistoryUiItem.kt):
Used by [`HistoryActivity`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/HistoryActivity.kt) to construct structured list views:
- `HistoryUiItem.ProHeader`: Shown at the top when an active Pro user views history.
- `HistoryUiItem.DateHeader(label: String)`: Groups items by date ("Today", "Yesterday", or "EEE, d MMM").
- `HistoryUiItem.Roll(entry: RollEntry)`: Individual roll entry with dice vectors and result tags.
- `HistoryUiItem.Empty`: Placeholder state when no history exists.

---

## 🔒 Pro vs. Free History Filtering

Query limits are managed in [`RollHistoryRepository.kt`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/RollHistoryRepository.kt):
- **Free Users**: Limited to the most recent **10** entries (`dao.getLatest(10)`). Previewed in `MainActivity` via an embedded `ModalBottomSheet` with an upsell card.
- **Pro Users (Trial or Lifetime)**: Extended to **10,000** entries (`dao.getLatest(10000)`). Clicking history opens the full dedicated [`HistoryActivity`](file:///c:/VD/Android/AndroidStudioProjects/DiceRoller/app/src/main/java/developer/android/vd/diceroller/HistoryActivity.kt).

---

## ⚙️ Schema Modifications & Migrations

When altering `RollEntity` fields or adding table columns:
1. Update `RollEntity.kt` with new fields or annotations.
2. Increment `version` in `@Database(entities = [RollEntity::class], version = 2)` in `AppDatabase.kt`.
3. Create an explicit `Migration(oldVersion, newVersion)` object:
   ```kotlin
   val MIGRATION_1_2 = object : Migration(1, 2) {
       override fun migrate(db: SupportSQLiteDatabase) {
           db.execSQL("ALTER TABLE roll_history ADD COLUMN notes TEXT DEFAULT ''")
       }
   }
   ```
4. Register the migration in `AppDatabase.get(context)`:
   ```kotlin
   Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "dice_roller.db")
       .addMigrations(MIGRATION_1_2)
       .build()
   ```
5. Run KSP to verify DAO generation:
   ```powershell
   .\gradlew.bat kspDebugKotlin
   ```
6. Run unit tests to verify no syntax errors:
   ```powershell
   .\gradlew.bat test
   ```
