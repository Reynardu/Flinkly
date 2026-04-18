package dev.reynardus.flinkly.data.remote

import dev.reynardus.flinkly.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Auth
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("auth/me")
    suspend fun getMe(): Response<UserDto>

    @PUT("auth/me")
    suspend fun updateMe(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<UserDto>

    // Household
    @POST("household")
    suspend fun createHousehold(@Body body: HouseholdCreate): Response<HouseholdDto>

    @GET("household/{id}")
    suspend fun getHousehold(@Path("id") id: Int): Response<HouseholdDto>

    @POST("household/{id}/invite")
    suspend fun createInviteLink(@Path("id") id: Int): Response<InviteLinkResponse>

    @POST("household/join/{token}")
    suspend fun joinHousehold(@Path("token") token: String): Response<HouseholdDto>

    @DELETE("household/{id}/members/{userId}")
    suspend fun removeMember(@Path("id") householdId: Int, @Path("userId") userId: Int): Response<Unit>

    @GET("household/{id}/pauses")
    suspend fun getPauses(@Path("id") householdId: Int): Response<List<HouseholdPauseDto>>

    @GET("household/{id}/pauses/active")
    suspend fun getActivePause(@Path("id") householdId: Int): Response<HouseholdPauseDto?>

    @POST("household/{id}/pauses")
    suspend fun createPause(@Path("id") householdId: Int, @Body body: PauseCreate): Response<HouseholdPauseDto>

    @DELETE("household/{id}/pauses/{pauseId}")
    suspend fun deletePause(@Path("id") householdId: Int, @Path("pauseId") pauseId: Int): Response<Unit>

    // Rooms
    @GET("rooms/household/{householdId}")
    suspend fun getRooms(@Path("householdId") householdId: Int): Response<List<RoomDto>>

    @POST("rooms/household/{householdId}")
    suspend fun createRoom(@Path("householdId") householdId: Int, @Body body: RoomCreate): Response<RoomDto>

    @PUT("rooms/{id}")
    suspend fun updateRoom(@Path("id") id: Int, @Body body: RoomUpdate): Response<RoomDto>

    @DELETE("rooms/{id}")
    suspend fun deleteRoom(@Path("id") id: Int): Response<Unit>

    @GET("rooms/suggestions")
    suspend fun getRoomSuggestions(): Response<List<Map<String, String>>>

    // Tasks
    @GET("tasks/room/{roomId}")
    suspend fun getTasks(@Path("roomId") roomId: Int): Response<List<TaskDto>>

    @POST("tasks/room/{roomId}")
    suspend fun createTask(@Path("roomId") roomId: Int, @Body body: TaskCreate): Response<TaskDto>

    @DELETE("tasks/{id}")
    suspend fun deleteTask(@Path("id") id: Int): Response<Unit>

    @POST("tasks/{id}/complete")
    suspend fun completeTask(@Path("id") id: Int, @Body body: CompletionCreate): Response<CompletionDto>

    @GET("tasks/suggestions/{householdId}")
    suspend fun getTaskSuggestions(@Path("householdId") householdId: Int): Response<List<TaskSuggestionDto>>

    // Scores
    @GET("scores/{householdId}/{period}")
    suspend fun getScoreboard(@Path("householdId") householdId: Int, @Path("period") period: String): Response<ScoreboardDto>

    @GET("scores/{householdId}/daily-progress")
    suspend fun getDailyProgress(@Path("householdId") householdId: Int): Response<DailyProgressDto>

    @GET("scores/{householdId}/level/{userId}")
    suspend fun getUserLevel(@Path("householdId") householdId: Int, @Path("userId") userId: Int): Response<UserLevelDto>

    // Achievements
    @GET("achievements/{userId}")
    suspend fun getAchievements(@Path("userId") userId: Int): Response<List<AchievementDto>>

    // Health
    @GET("health")
    suspend fun health(): Response<Map<String, String>>
}
