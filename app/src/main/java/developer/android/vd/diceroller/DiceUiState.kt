package developer.android.vd.diceroller

data class DiceUiState(
    val diceCount: Int = 1,
    val diceType: DiceType = DiceType.D6,
    val results: List<Int> = listOf(0),
    val total: Int = 0,
    val isRolling: Boolean = false,
    val lockedIndices: Set<Int> = emptySet()
)
