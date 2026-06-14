package developer.android.vd.diceroller

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed class HistoryUiItem {
    object ProHeader : HistoryUiItem()
    data class DateHeader(val label: String) : HistoryUiItem()
    data class Roll(val entry: RollEntry) : HistoryUiItem()
    object Empty : HistoryUiItem()
}

fun Long.toDateLabel(): String {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)

    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> {
            val pattern = if (date.year == today.year) {
                "EEE, d MMM"
            } else {
                "EEE, d MMM yyyy"
            }

            DateTimeFormatter
                .ofPattern(pattern, Locale.getDefault())
                .format(date)
        }
    }
}

