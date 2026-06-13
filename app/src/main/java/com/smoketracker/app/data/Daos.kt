package com.smoketracker.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CigaretteDao {
    @Query("SELECT * FROM cigarettes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Cigarette>>

    @Query("SELECT * FROM cigarettes WHERE isDefault = 1 LIMIT 1")
    fun observeDefault(): Flow<Cigarette?>

    @Query("SELECT * FROM cigarettes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Cigarette?

    @Query("SELECT COUNT(*) FROM cigarettes")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cigarette: Cigarette): Long

    @Update
    suspend fun update(cigarette: Cigarette)

    @Delete
    suspend fun delete(cigarette: Cigarette)

    @Query("UPDATE cigarettes SET isDefault = 0")
    suspend fun clearDefault()

    @Query("UPDATE cigarettes SET isDefault = 1 WHERE id = :id")
    suspend fun markDefault(id: Long)

    /** 把某个烟品设为唯一默认。 */
    @Transaction
    suspend fun setDefault(id: Long) {
        clearDefault()
        markDefault(id)
    }
}

@Dao
interface SmokeEventDao {
    @Query("SELECT * FROM smoke_events ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<SmokeEvent>>

    @Query("SELECT * FROM smoke_events ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): SmokeEvent?

    @Query("SELECT * FROM smoke_events WHERE kind = :kind ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestOfKind(kind: String): SmokeEvent?

    @Query("SELECT COUNT(*) FROM smoke_events")
    fun observeTotalCount(): Flow<Int>

    @Insert
    suspend fun insert(event: SmokeEvent): Long

    @Update
    suspend fun update(event: SmokeEvent)

    @Delete
    suspend fun delete(event: SmokeEvent)

    @Query("DELETE FROM smoke_events WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchases ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<Purchase>>

    @Insert
    suspend fun insert(purchase: Purchase): Long

    @Update
    suspend fun update(purchase: Purchase)

    @Delete
    suspend fun delete(purchase: Purchase)
}
