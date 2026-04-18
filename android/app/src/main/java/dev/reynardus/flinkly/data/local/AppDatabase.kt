package dev.reynardus.flinkly.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.reynardus.flinkly.data.local.dao.HouseholdDao
import dev.reynardus.flinkly.data.local.dao.RoomDao
import dev.reynardus.flinkly.data.local.dao.TaskDao
import dev.reynardus.flinkly.data.local.entities.HouseholdEntity
import dev.reynardus.flinkly.data.local.entities.RoomEntity
import dev.reynardus.flinkly.data.local.entities.TaskEntity

@Database(
    entities = [HouseholdEntity::class, RoomEntity::class, TaskEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun householdDao(): HouseholdDao
    abstract fun roomDao(): RoomDao
    abstract fun taskDao(): TaskDao
}
