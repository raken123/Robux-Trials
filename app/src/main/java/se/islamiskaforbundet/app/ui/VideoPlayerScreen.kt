package se.islamiskaforbundet.app.ui

import android.speech.tts.TextToSpeech
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.util.Locale
import kotlinx.coroutines.delay
import se.islamiskaforbundet.app.data.Videos
import se.islamiskaforbundet.app.quran.QuranPlayer

/** Plays a bundled video; optional Swedish voice-over is spoken by the phone's text-to-speech in sync with the captions. */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(slug: String) {
    val context = LocalContext.current
    val video = remember(slug) { Videos.find(context, slug) } ?: return
    var narration by remember { mutableStateOf(true) }
    var ttsReady by remember { mutableStateOf(false) }
    var ttsMissing by remember { mutableStateOf(false) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(RawResourceDataSource.buildRawResourceUri(video.rawRes)))
            prepare()
            playWhenReady = true
        }
    }
    val tts = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val r = engine?.setLanguage(Locale.forLanguageTag("sv-SE"))
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) ttsMissing = true
                else { engine?.setSpeechRate(0.95f); ttsReady = true }
            } else {
                ttsMissing = true
            }
        }
        engine!!
    }

    DisposableEffect(Unit) {
        QuranPlayer.stop()
        onDispose {
            tts.stop()
            tts.shutdown()
            player.release()
        }
    }

    // Speak each caption when playback crosses its start time.
    LaunchedEffect(narration, ttsReady) {
        var last = player.currentPosition
        while (true) {
            delay(150)
            val now = player.currentPosition
            if (!player.isPlaying) {
                if (tts.isSpeaking) tts.stop()
            } else if (narration && ttsReady) {
                if (now < last || now - last > 1500) {
                    tts.stop() // seek: wait for the next cue
                } else {
                    video.cues.firstOrNull { (it.seconds * 1000).toLong() in (last + 1)..now }?.let {
                        tts.speak(it.text, TextToSpeech.QUEUE_FLUSH, null, "cue")
                    }
                }
            }
            last = now
        }
    }
    LaunchedEffect(narration) { if (!narration) tts.stop() }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { this.player = player; setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER) } },
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        )
        Column(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(16.dp).weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(video.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(video.subtitle, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Berättarröst", fontWeight = FontWeight.Bold)
                    Text(
                        if (ttsMissing) "Svensk talsyntes saknas på telefonen. Installera den under Inställningar → Text till tal."
                        else "Läser texten högt på svenska",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(checked = narration && !ttsMissing, enabled = !ttsMissing, onCheckedChange = { narration = it })
            }
            Text(
                "Animerad video av Islamiska Förbundet. Texten visas även i bild.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
