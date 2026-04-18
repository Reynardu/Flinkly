package dev.reynardus.flinkly.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    @SerializedName("display_name") val displayName: String,
    val password: String,
)

data class LoginRequest(
    @SerializedName("user_secret") val userSecret: String,
)

data class AuthResponse(
    val token: String,
    @SerializedName("user_secret") val userSecret: String,
    val user: UserDto,
)

data class UserDto(
    val id: Int,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("daily_point_goal") val dailyPointGoal: Int,
    @SerializedName("total_points") val totalPoints: Int,
    @SerializedName("current_streak") val currentStreak: Int,
    @SerializedName("longest_streak") val longestStreak: Int,
    @SerializedName("created_at") val createdAt: String,
)

data class HouseholdCreate(val name: String)

data class HouseholdDto(
    val id: Int,
    val name: String,
    @SerializedName("created_at") val createdAt: String,
    val members: List<MemberDto> = emptyList(),
)

data class MemberDto(
    val id: Int,
    val user: UserDto,
    val role: String,
    @SerializedName("joined_at") val joinedAt: String,
)

data class InviteLinkResponse(
    @SerializedName("invite_url") val inviteUrl: String,
    val token: String,
)

data class RoomCreate(val name: String, val icon: String = "home", val color: String = "#4CAF50")
data class RoomUpdate(val name: String? = null, val icon: String? = null, val color: String? = null)

data class RoomDto(
    val id: Int,
    @SerializedName("household_id") val householdId: Int,
    val name: String,
    val icon: String,
    val color: String,
    @SerializedName("task_count") val taskCount: Int = 0,
    @SerializedName("open_task_count") val openTaskCount: Int = 0,
    @SerializedName("created_at") val createdAt: String,
)

data class TaskCreate(
    val title: String,
    val description: String? = null,
    val difficulty: Int = 2,
    @SerializedName("frequency_type") val frequencyType: String = "WEEKLY",
    @SerializedName("frequency_value") val frequencyValue: String? = null,
    @SerializedName("due_date") val dueDate: String? = null,
    @SerializedName("auto_repeat") val autoRepeat: Boolean = true,
    @SerializedName("assigned_to_user_id") val assignedToUserId: Int? = null,
)

data class TaskDto(
    val id: Int,
    @SerializedName("room_id") val roomId: Int,
    val title: String,
    val description: String?,
    val difficulty: Int,
    @SerializedName("frequency_type") val frequencyType: String,
    @SerializedName("frequency_value") val frequencyValue: String?,
    @SerializedName("due_date") val dueDate: String?,
    @SerializedName("auto_repeat") val autoRepeat: Boolean,
    val points: Int,
    @SerializedName("photo_url") val photoUrl: String?,
    @SerializedName("is_suggestion") val isSuggestion: Boolean,
    @SerializedName("assigned_to_user_id") val assignedToUserId: Int?,
    @SerializedName("next_due_at") val nextDueAt: String?,
    @SerializedName("created_at") val createdAt: String,
    val completions: List<CompletionDto> = emptyList(),
    @SerializedName("completion_count") val completionCount: Int = 0,
)

data class CompletionCreate(
    @SerializedName("photo_url") val photoUrl: String? = null,
    val note: String? = null,
)

data class CompletionDto(
    val id: Int,
    @SerializedName("task_id") val taskId: Int,
    val user: UserDto,
    @SerializedName("completed_at") val completedAt: String,
    @SerializedName("points_earned") val pointsEarned: Int,
    @SerializedName("photo_url") val photoUrl: String?,
    val note: String?,
)

data class ScoreboardDto(
    val period: String,
    val entries: List<ScoreEntryDto>,
    @SerializedName("fairness_percent") val fairnessPercent: Map<String, Double>,
)

data class ScoreEntryDto(
    val user: UserScoreSummaryDto,
    val points: Int,
    @SerializedName("tasks_completed") val tasksCompleted: Int,
    val rank: Int,
)

data class UserScoreSummaryDto(
    val id: Int,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("current_streak") val currentStreak: Int,
)

data class AchievementDto(
    val id: Int,
    val type: String,
    val title: String,
    val description: String,
    val icon: String,
    @SerializedName("earned_at") val earnedAt: String,
)

data class TaskSuggestionDto(
    val title: String,
    val description: String,
    val difficulty: Int,
    @SerializedName("frequency_type") val frequencyType: String,
    val points: Int,
    val reason: String,
)

data class UserLevelDto(
    val title: String,
    val icon: String,
    val points: Int,
    @SerializedName("next_level_title") val nextLevelTitle: String? = null,
    @SerializedName("points_needed") val pointsNeeded: Int? = null,
)

data class PauseCreate(
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    val reason: String? = null,
)

data class HouseholdPauseDto(
    val id: Int,
    @SerializedName("household_id") val householdId: Int,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    val reason: String?,
    @SerializedName("created_by_user_id") val createdByUserId: Int,
    @SerializedName("created_at") val createdAt: String,
)

data class DailyProgressDto(
    @SerializedName("today_points") val todayPoints: Int,
    @SerializedName("daily_goal") val dailyGoal: Int,
    val percent: Int,
    @SerializedName("goal_reached") val goalReached: Boolean,
    val streak: Int,
    @SerializedName("is_paused") val isPaused: Boolean = false,
    @SerializedName("pause_reason") val pauseReason: String? = null,
    @SerializedName("pause_end_date") val pauseEndDate: String? = null,
)
