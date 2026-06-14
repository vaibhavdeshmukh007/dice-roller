package developer.android.vd.diceroller


enum class DiceType(val sides: Int, val proOnly: Boolean) {
    D4(4, false),
    D6(6, false),
    D8(8, false),
    D10(10, true),
    D12(12, true),
    D20(20, true)
}
