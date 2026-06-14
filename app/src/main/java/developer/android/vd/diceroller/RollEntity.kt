package developer.android.vd.diceroller

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "roll_history")
data class RollEntity(
    @PrimaryKey
    val timestamp: Long,
    val diceType: String,
    val diceCount: Int,
    val results: String, // "3,5,1"
    val total: Int
)


fun RollEntity.toDomain(): RollEntry =
    RollEntry(
        timestamp = timestamp,
        diceType = DiceType.valueOf(diceType),
        diceCount = diceCount,
        results = results.split(",").map { it.toInt() },
        total = total
    )
