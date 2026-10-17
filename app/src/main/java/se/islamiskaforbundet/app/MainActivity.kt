package se.islamiskaforbundet.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import se.islamiskaforbundet.app.data.Billing
import se.islamiskaforbundet.app.data.Prefs
import se.islamiskaforbundet.app.ui.DonateScreen
import se.islamiskaforbundet.app.ui.HajjScreen
import se.islamiskaforbundet.app.ui.HomeScreen
import se.islamiskaforbundet.app.ui.IfTheme
import se.islamiskaforbundet.app.ui.MiniPlayer
import se.islamiskaforbundet.app.ui.PremiumScreen
import se.islamiskaforbundet.app.ui.QiblaScreen
import se.islamiskaforbundet.app.ui.QuranListScreen
import se.islamiskaforbundet.app.ui.Routes
import se.islamiskaforbundet.app.ui.SalahScreen
import se.islamiskaforbundet.app.ui.SurahScreen
import se.islamiskaforbundet.app.ui.TasbihScreen
import se.islamiskaforbundet.app.ui.VideoPlayerScreen
import se.islamiskaforbundet.app.ui.VideosScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Prefs.init(this)
        Billing.start(this)
        setContent { IfTheme { AppRoot() } }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab(Routes.HOME, "Hem", Icons.Filled.Home),
    Tab(Routes.SALAH, "Bön", Icons.Filled.NotificationsActive),
    Tab(Routes.QURAN, "Quran", Icons.Filled.MenuBook),
    Tab(Routes.TASBIH, "Tasbih", Icons.Filled.FiberManualRecord),
)

@Composable
private fun AppRoot() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    val premium = Billing.premium

    // Ask once for notifications so the Fajr alarm (on by default) can ring.
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (!Prefs.onboarded) {
            Prefs.onboarded = true
            if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun go(route: String) {
        if (TABS.any { it.route == route }) {
            nav.navigate(route) {
                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        } else {
            nav.navigate(route)
        }
    }

    Scaffold(
        bottomBar = {
            Column {
                MiniPlayer()
                NavigationBar {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route,
                            onClick = { go(tab.route) },
                            icon = { Icon(tab.icon, null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.fillMaxSize().padding(padding)) {
            composable(Routes.HOME) { HomeScreen(premium, ::go) }
            composable(Routes.SALAH) { SalahScreen() }
            composable(Routes.TASBIH) { TasbihScreen() }
            composable(Routes.QURAN) { QuranListScreen { n -> nav.navigate("${Routes.SURAH}/$n") } }
            composable("${Routes.SURAH}/{n}", arguments = listOf(navArgument("n") { type = NavType.IntType })) {
                SurahScreen(it.arguments?.getInt("n")?.coerceIn(1, 114) ?: 1)
            }
            composable(Routes.QIBLA) { QiblaScreen() }
            composable(Routes.DONATE) { DonateScreen() }
            composable(Routes.VIDEOS) {
                VideosScreen(premium, openPremium = { go(Routes.PREMIUM) }, play = { slug -> nav.navigate("${Routes.VIDEO}/$slug") })
            }
            composable("${Routes.VIDEO}/{slug}") { VideoPlayerScreen(it.arguments?.getString("slug").orEmpty()) }
            composable(Routes.PREMIUM) { PremiumScreen() }
            composable(Routes.HAJJ) { HajjScreen() }
        }
    }
}
