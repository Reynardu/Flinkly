package dev.reynardus.flinkly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
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

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        var startDestination by mutableStateOf<String?>(null)
        splashScreen.setKeepOnScreenCondition { startDestination == null }

        lifecycleScope.launch {
            startDestination = when {
                !authRepository.isLoggedIn() -> Route.ServerSetup.path
                !authRepository.hasHousehold() -> Route.HouseholdSetup.path
                else -> Route.Dashboard.path
            }
        }

        setContent {
            FlinklyTheme {
                val dest = startDestination
                if (dest != null) {
                    FlinklyNavGraph(startDestination = dest)
                }
            }
        }
    }
}
