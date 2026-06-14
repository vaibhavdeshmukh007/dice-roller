package developer.android.vd.diceroller

data class RollEntry(
    val timestamp: Long,
    val diceType: DiceType,
    val diceCount: Int,
    val results: List<Int>,
    val total: Int
)

fun RollEntry.toEntity(): RollEntity =
    RollEntity(
        timestamp = timestamp,
        diceType = diceType.name,
        diceCount = diceCount,
        results = results.joinToString(","),
        total = total
    )