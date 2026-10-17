package se.islamiskaforbundet.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import se.islamiskaforbundet.app.Config
import se.islamiskaforbundet.app.data.Prefs
import se.islamiskaforbundet.app.prayer.AlarmScheduler

private data class Tool(val label: String, val icon: ImageVector, val route: String)

private val TOOLS = listOf(
    Tool("Salah", Icons.Filled.NotificationsActive, Routes.SALAH),
    Tool("Tasbih", Icons.Filled.FiberManualRecord, Routes.TASBIH),
    Tool("Quran", Icons.Filled.MenuBook, Routes.QURAN),
    Tool("Qibla", Icons.Filled.Explore, Routes.QIBLA),
    Tool("Donera", Icons.Filled.Favorite, Routes.DONATE),
    Tool("Videor", Icons.Filled.OndemandVideo, Routes.VIDEOS),
    Tool("Premium", Icons.Filled.WorkspacePremium, Routes.PREMIUM),
    Tool("Hajj-kalkyl", Icons.Filled.Calculate, Routes.HAJJ),
)

@Composable
fun HomeScreen(premium: Boolean, navigate: (String) -> Unit) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) { delay(1000); now = LocalDateTime.now() }
    }
    val next = remember(now.toLocalDate(), now.hour, now.minute) { AlarmScheduler.nextPrayer(now) }
    val uri = LocalUriHandler.current
    val dateText = remember(now.toLocalDate()) {
        now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.forLanguageTag("sv-SE")))
            .replaceFirstChar { it.uppercase() }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        HeroHeader("Islamiska Förbundet", "$dateText · ${Prefs.placeName}")
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (next != null) {
                val (prayer, time) = next
                val left = Duration.between(now, time).coerceAtLeast(Duration.ZERO)
                Card(Modifier.fillMaxWidth(), onClick = { navigate(Routes.SALAH) }) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Nästa bön", style = MaterialTheme.typography.labelLarge)
                        Text(prayer.ar, fontSize = 30.sp, color = MaterialTheme.colorScheme.primary)
                        Text(
                            "${prayer.sv} · ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "om %d:%02d:%02d".format(left.seconds / 3600, left.seconds / 60 % 60, left.seconds % 60),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            if (!premium) HouseBanner { navigate(Routes.PREMIUM) }

            SectionTitle("Verktyg")
            TOOLS.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { tool ->
                        Card(onClick = { navigate(tool.route) }, modifier = Modifier.weight(1f)) {
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(tool.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.height(6.dp))
                                Text(tool.label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 1)
                            }
                        }
                    }
                }
            }

            IfCard(onClick = { uri.openUri(Config.WEBSITE) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Public, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text("islamiskaforbundet.se", fontWeight = FontWeight.Bold)
                        Text("Nyheter, aktiviteter och kontakt", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            val ctx = LocalContext.current
            Text(
                "Version ${ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}
