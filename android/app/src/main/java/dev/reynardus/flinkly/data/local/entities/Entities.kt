package dev.reynardus.flinkly.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "households")
data class HouseholdEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val createdAt: String,
)

@Entity(
    tableName = "rooms",
    foreignKeys = [ForeignKey(
        entity = HouseholdEntity::class,
        parentColumns = ["id"],
        childColumns = ["householdId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("householdId")],
)
data class RoomEntity(
    @PrimaryKey val id: Int,
    val householdId: Int,
    val name: String,
    val icon: String,
    val color: String,
    val taskCount: Int = 0,
    val openTaskCount: Int = 0,
    val createdAt: String,
)

@Entity(
    tableName = "tasks",
    foreignKeys = [ForeignKey(
        entity = RoomEntity::class,
        parentColumns = ["id"],
        childColumns = ["roomId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("roomId")],
)
data class TaskEntity(
    @PrimaryKey val id: Int,
    val roomId: Int,
    val title: String,
    val description: String?,
    val difficulty: Int,
    val frequencyType: String,
    val frequencyValue: String?,
    val dueDate: String?,
    val autoRepeat: Boolean,
    val points: Int,
    val photoUrl: String?,
    val isSuggestion: Boolean,
    val assignedToUserId: Int?,
    val nextDueAt: String?,
    val completionCount: Int = 0,
    val createdAt: String,
)
