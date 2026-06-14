package developer.android.vd.diceroller

import android.content.Context

class RollHistoryRepository(context: Context) {

    private val dao = AppDatabase.get(context).rollHistoryDao()

    suspend fun add(entry: RollEntry) {
        dao.insert(entry.toEntity())
    }

    suspend fun getHistory(isPro: Boolean): List<RollEntry> {
        val limit = if (isPro) 10000 else 10
        return dao.getLatest(limit).map { it.toDomain() }
    }

}
