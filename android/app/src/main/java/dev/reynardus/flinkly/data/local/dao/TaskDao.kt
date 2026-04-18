package dev.reynardus.flinkly.data.local.dao

import androidx.room.*
import dev.reynardus.flinkly.data.local.entities.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE roomId = :roomId ORDER BY title ASC")
    fun getTasks(roomId: Int): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE roomId = :roomId")
    suspend fun deleteByRoom(roomId: Int)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: Int)
}
