package se.islamiskaforbundet.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.islamiskaforbundet.app.data.Prefs
import se.islamiskaforbundet.app.quran.QuranPlayer
import se.islamiskaforbundet.app.quran.QuranRepository
import se.islamiskaforbundet.app.quran.RECITERS
import se.islamiskaforbundet.app.quran.SURAHS
import se.islamiskaforbundet.app.quran.SurahText

@Composable
fun QuranListScreen(open: (Int) -> Unit) {
    var query by remember { mutableStateOf("") }
    val list = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) SURAHS
        else SURAHS.filter { it.name.lowercase().contains(q) || it.meaning.lowercase().contains(q) || it.number.toString() == q }
    }
    Column(Modifier.fillMaxSize()) {
        HeroHeader("Quran", "114 suror · arabiska och svenska")
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            placeholder = { Text("Sök sura") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(list, key = { it.number }) { s ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(38.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text("${s.number}", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.name, fontWeight = FontWeight.Bold)
                        Text("${s.meaning} · ${s.ayahs} verser", style = MaterialTheme.typography.bodySmall)
                    }
                    val playing = QuranPlayer.playingSurah == s.number && QuranPlayer.isPlaying
                    val ctx = LocalContext.current
                    IconButton(onClick = {
                        if (QuranPlayer.playingSurah == s.number) QuranPlayer.toggle() else QuranPlayer.play(ctx, s.number, Prefs.reciter)
                    }) { Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Lyssna") }
                    Button(onClick = { open(s.number) }, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("Läs") }
                }
            }
        }
    }
}

@Composable
fun SurahScreen(number: Int) {
    val context = LocalContext.current
    val surah = SURAHS[number - 1]
    var text by remember { mutableStateOf<SurahText?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    var reciter by remember { mutableStateOf(Prefs.reciter) }
    var showTranslation by remember { mutableStateOf(true) }

    LaunchedEffect(number, attempt) {
        error = null
        text = runCatching { QuranRepository.load(context, number) }
            .onFailure { error = "Kunde inte hämta suran. Kontrollera internetanslutningen." }
            .getOrNull()
    }

    Column(Modifier.fillMaxSize()) {
        HeroHeader("${surah.number}. ${surah.name}", "${surah.meaning} · ${surah.ayahs} verser") {
            val playing = QuranPlayer.playingSurah == number && QuranPlayer.isPlaying
            IconButton(onClick = {
                if (QuranPlayer.playingSurah == number) QuranPlayer.toggle() else QuranPlayer.play(context, number, reciter)
            }) { Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Lyssna", tint = IfGold, modifier = Modifier.size(36.dp)) }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RECITERS.forEach { r ->
                FilterChip(
                    selected = reciter == r.id,
                    onClick = {
                        reciter = r.id; Prefs.reciter = r.id
                        if (QuranPlayer.playingSurah == number) QuranPlayer.play(context, number, r.id)
                    },
                    label = { Text(r.name.substringAfterLast(' ')) },
                )
            }
            FilterChip(selected = showTranslation, onClick = { showTranslation = !showTranslation }, label = { Text("Svenska") })
        }
        QuranPlayer.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) }
        when {
            error != null -> Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(error!!, textAlign = TextAlign.Center)
                Button(onClick = { attempt++ }, modifier = Modifier.padding(top = 12.dp)) { Text("Försök igen") }
            }
            text == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text(
                        text!!.arabicName,
                        fontSize = 30.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                items(text!!.ayahs, key = { it.number }) { a ->
                    Column {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                Modifier.size(30.dp).background(IfGold.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { Text("${a.number}", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                a.arabic,
                                fontSize = 24.sp,
                                lineHeight = 44.sp,
                                style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Rtl),
                                textAlign = TextAlign.Right,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (showTranslation && a.swedish.isNotBlank()) {
                            Text(a.swedish, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp, start = 40.dp))
                        }
                        HorizontalDivider(Modifier.padding(top = 12.dp))
                    }
                }
                item {
                    Text(
                        "Svensk översättning: Knut Bernström. Text och ljud hämtas från alquran.cloud / islamic.network.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )
                }
            }
        }
    }
}

/** Small bar shown above the bottom navigation while a recitation is loaded. */
@Composable
fun MiniPlayer() {
    val s = QuranPlayer.playingSurah ?: return
    val surah = SURAHS[s - 1]
    Row(
        Modifier.fillMaxWidth().background(IfNavyDark).padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("▶ ${surah.number}. ${surah.name}", color = Color.White, modifier = Modifier.weight(1f))
        IconButton(onClick = { QuranPlayer.toggle() }) {
            Icon(if (QuranPlayer.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Spela/pausa", tint = IfGold)
        }
        IconButton(onClick = { QuranPlayer.stop() }) { Icon(Icons.Filled.Stop, "Stoppa", tint = IfGold) }
    }
}
