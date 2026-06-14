package developer.android.vd.diceroller

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RollHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: RollEntity)

    @Query("SELECT * FROM roll_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLatest(limit: Int): List<RollEntity>

    @Query("DELETE FROM roll_history")
    suspend fun clearAll()
}
