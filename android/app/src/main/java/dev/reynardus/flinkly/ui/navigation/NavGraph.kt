package dev.reynardus.flinkly.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.reynardus.flinkly.ui.screens.achievements.AchievementsScreen
import dev.reynardus.flinkly.ui.screens.dashboard.DashboardScreen
import dev.reynardus.flinkly.ui.screens.rooms.RoomsScreen
import dev.reynardus.flinkly.ui.screens.scoreboard.ScoreboardScreen
import dev.reynardus.flinkly.ui.screens.settings.SettingsScreen
import dev.reynardus.flinkly.ui.screens.setup.HouseholdSetupScreen
import dev.reynardus.flinkly.ui.screens.setup.LoginScreen
import dev.reynardus.flinkly.ui.screens.setup.ServerSetupScreen
import dev.reynardus.flinkly.ui.screens.tasks.TasksScreen

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit,
)

private val bottomNavItems = listOf(
    BottomNavItem(Route.Dashboard.path, "Heute") { Icon(Icons.Default.Home, null) },
    BottomNavItem(Route.Rooms.path, "Räume") { Icon(Icons.Default.MeetingRoom, null) },
    BottomNavItem(Route.Scoreboard.path, "Punkte") { Icon(Icons.Default.Leaderboard, null) },
    BottomNavItem(Route.Achievements.path, "Erfolge") { Icon(Icons.Default.EmojiEvents, null) },
    BottomNavItem(Route.Settings.path, "Mehr") { Icon(Icons.Default.Settings, null) },
)

@Composable
fun FlinklyNavGraph(startDestination: String) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in mainRoutes) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = backStackEntry?.destination
                            ?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = item.icon,
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding),
        ) {
            composable(Route.ServerSetup.path) {
                ServerSetupScreen(onSuccess = {
                    navController.navigate(Route.Login.path) {
                        popUpTo(Route.ServerSetup.path) { inclusive = true }
                    }
                })
            }
            composable(Route.Login.path) {
                LoginScreen(onSuccess = { hasHousehold ->
                    val dest = if (hasHousehold) Route.Dashboard.path else Route.HouseholdSetup.path
                    navController.navigate(dest) {
                        popUpTo(Route.Login.path) { inclusive = true }
                    }
                })
            }
            composable(Route.HouseholdSetup.path) {
                HouseholdSetupScreen(onSuccess = {
                    navController.navigate(Route.Dashboard.path) {
                        popUpTo(Route.HouseholdSetup.path) { inclusive = true }
                    }
                })
            }
            composable(Route.Dashboard.path) { DashboardScreen() }
            composable(Route.Rooms.path) {
                RoomsScreen(onRoomClick = { roomId, roomName ->
                    navController.navigate(Route.Tasks.createRoute(roomId, roomName))
                })
            }
            composable(Route.Tasks.path) { entry ->
                val roomId = entry.arguments?.getString("roomId")?.toIntOrNull() ?: return@composable
                val roomName = java.net.URLDecoder.decode(
                    entry.arguments?.getString("roomName") ?: "", "UTF-8"
                )
                TasksScreen(
                    roomId = roomId,
                    roomName = roomName,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Route.Scoreboard.path) { ScoreboardScreen() }
            composable(Route.Achievements.path) { AchievementsScreen() }
            composable(Route.Settings.path) {
                SettingsScreen(onLogout = {
                    navController.navigate(Route.ServerSetup.path) {
                        popUpTo(0) { inclusive = true }
                    }
                })
            }
        }
    }
}
