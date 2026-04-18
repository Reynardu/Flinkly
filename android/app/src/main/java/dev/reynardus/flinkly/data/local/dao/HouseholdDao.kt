package dev.reynardus.flinkly.data.local.dao

import androidx.room.*
import dev.reynardus.flinkly.data.local.entities.HouseholdEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseholdDao {
    @Query("SELECT * FROM households WHERE id = :id")
    fun getHousehold(id: Int): Flow<HouseholdEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(household: HouseholdEntity)

    @Delete
    suspend fun delete(household: HouseholdEntity)
}
