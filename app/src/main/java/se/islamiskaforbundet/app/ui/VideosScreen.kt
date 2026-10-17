package se.islamiskaforbundet.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import se.islamiskaforbundet.app.Config
import se.islamiskaforbundet.app.data.Videos

@Composable
fun VideosScreen(premium: Boolean, openPremium: () -> Unit, play: (String) -> Unit) {
    val context = LocalContext.current
    val uri = LocalUriHandler.current
    val videos = remember { Videos.all(context) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        HeroHeader("Islamiska videor", if (premium) "Premium · alla videor" else "Lås upp fler med Premium")
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!premium) HouseBanner(openPremium)
            videos.forEach { v ->
                val locked = v.premium && !premium
                Card(onClick = { if (locked) openPremium() else play(v.slug) }, modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                        Image(
                            painterResource(v.thumbRes), null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (locked) 0.55f else 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (locked) Icons.Filled.Lock else Icons.Filled.PlayCircle, null,
                                tint = if (locked) Color.White else IfGold,
                                modifier = Modifier.size(64.dp),
                            )
                        }
                        val mins = (v.durationSeconds / 60).toInt()
                        val secs = (v.durationSeconds % 60).toInt()
                        Text(
                            "%d:%02d".format(mins, secs),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                                .clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(v.title, fontWeight = FontWeight.Bold)
                            Text(v.subtitle, style = MaterialTheme.typography.bodySmall)
                        }
                        if (v.premium) {
                            Text(
                                "Premium",
                                style = MaterialTheme.typography.labelSmall,
                                color = IfNavyDark,
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(IfGold)
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }
            if (Config.YOUTUBE_CHANNEL.isNotBlank()) {
                IfCard(onClick = { uri.openUri(Config.YOUTUBE_CHANNEL) }) {
                    Text("Islamiska Förbundets YouTube-kanal", fontWeight = FontWeight.Bold)
                    Text("Föreläsningar och evenemang", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
