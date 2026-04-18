package dev.reynardus.flinkly.ui.navigation

sealed class Route(val path: String) {
    object ServerSetup : Route("server_setup")
    object Login : Route("login")
    object HouseholdSetup : Route("household_setup")

    object Dashboard : Route("dashboard")
    object Rooms : Route("rooms")
    object Tasks : Route("tasks/{roomId}/{roomName}") {
        fun createRoute(roomId: Int, roomName: String) =
            "tasks/$roomId/${java.net.URLEncoder.encode(roomName, "UTF-8")}"
    }
    object Scoreboard : Route("scoreboard")
    object Achievements : Route("achievements")
    object Settings : Route("settings")
}

val mainRoutes = setOf(
    Route.Dashboard.path,
    Route.Rooms.path,
    Route.Scoreboard.path,
    Route.Achievements.path,
    Route.Settings.path,
)
