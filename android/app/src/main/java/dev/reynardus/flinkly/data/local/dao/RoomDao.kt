package dev.reynardus.flinkly.data.local.dao

import androidx.room.*
import dev.reynardus.flinkly.data.local.entities.RoomEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomDao {
    @Query("SELECT * FROM rooms WHERE householdId = :householdId ORDER BY name ASC")
    fun getRooms(householdId: Int): Flow<List<RoomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rooms: List<RoomEntity>)

    @Query("DELETE FROM rooms WHERE householdId = :householdId")
    suspend fun deleteByHousehold(householdId: Int)
}
