package se.islamiskaforbundet.app.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import se.islamiskaforbundet.app.Config

private data class VideoTopic(val title: String, val description: String, val query: String, val premium: Boolean)

private val TOPICS = listOf(
    VideoTopic("Lär dig be", "Steg för steg genom bönen", "hur man ber salah på svenska", false),
    VideoTopic("Quran-recitation", "Vackra recitationer", "quran recitation", false),
    VideoTopic("Fredagspredikan", "Khutba på svenska", "khutba fredagsbön svenska", false),
    VideoTopic("Profeternas berättelser", "Qisas al-anbiya", "profeternas berättelser islam", true),
    VideoTopic("Profetens liv (sira)", "Muhammads ﷺ liv", "profeten muhammads liv sira", true),
    VideoTopic("Tajwid", "Lär dig läsa Quran korrekt", "tajwid lektion", true),
    VideoTopic("Barn och familj", "Islamiska videor för barn", "islam för barn svenska", true),
    VideoTopic("Ramadan", "Fasta, tarawih och Eid", "ramadan föreläsning svenska", true),
    VideoTopic("Hajj och umrah", "Så går vallfärden till", "hajj guide steg för steg", true),
)

@Composable
fun VideosScreen(premium: Boolean, openPremium: () -> Unit) {
    val uri = LocalUriHandler.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        HeroHeader("Islamiska videor", if (premium) "Premium · alla kategorier" else "Lås upp fler med Premium")
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (Config.YOUTUBE_CHANNEL.isNotBlank()) {
                IfCard(onClick = { uri.openUri(Config.YOUTUBE_CHANNEL) }) {
                    Text("Islamiska Förbundets kanal", fontWeight = FontWeight.Bold)
                    Text("Föreläsningar och evenemang", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (!premium) HouseBanner(openPremium)
            TOPICS.forEach { t ->
                val locked = t.premium && !premium
                IfCard(onClick = {
                    if (locked) openPremium()
                    else uri.openUri("https://www.youtube.com/results?search_query=" + Uri.encode(t.query))
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (locked) Icons.Filled.Lock else Icons.Filled.PlayCircle, null,
                            tint = if (locked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp),
                        )
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.title, fontWeight = FontWeight.Bold)
                            Text(if (locked) "Premium" else t.description, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
