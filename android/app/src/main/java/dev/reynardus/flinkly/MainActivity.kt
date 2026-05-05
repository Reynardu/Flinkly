package dev.reynardus.flinkly

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import dagger.hilt.android.AndroidEntryPoint
import dev.reynardus.flinkly.data.repository.AuthRepository
import dev.reynardus.flinkly.ui.navigation.FlinklyNavGraph
import dev.reynardus.flinkly.ui.navigation.Route
import dev.reynardus.flinkly.ui.theme.FlinklyTheme
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    private var navController: NavController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val inviteToken = extractInviteToken(intent)
        var startDestination by mutableStateOf<String?>(null)

        lifecycleScope.launch {
            startDestination = when {
                !authRepository.isLoggedIn() -> Route.ServerSetup.path
                inviteToken != null -> Route.JoinHousehold.createRoute(inviteToken)
                !authRepository.hasHousehold() -> Route.HouseholdSetup.path
                else -> Route.Dashboard.path
            }
        }

        setContent {
            FlinklyTheme {
                val dest = startDestination
                if (dest != null) {
                    FlinklyNavGraph(
                        startDestination = dest,
                        inviteToken = inviteToken,
                        onNavControllerReady = { navController = it },
                    )
                } else {
                    AppLoadingScreen()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val token = extractInviteToken(intent) ?: return
        lifecycleScope.launch {
            val nav = navController ?: return@launch
            val dest = if (authRepository.isLoggedIn())
                Route.JoinHousehold.createRoute(token)
            else
                Route.ServerSetup.path
            nav.navigate(dest) { launchSingleTop = true }
        }
    }

    private fun extractInviteToken(intent: Intent?): String? =
        intent?.data?.let { uri ->
            if (uri.scheme == "flinkly" && uri.host == "join") uri.lastPathSegment else null
        }
}

@Composable
private fun AppLoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Flinkly",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Haushalts-App",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = BuildConfig.VERSION_NAME,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(32.dp))
        CircularProgressIndicator()
    }
}
