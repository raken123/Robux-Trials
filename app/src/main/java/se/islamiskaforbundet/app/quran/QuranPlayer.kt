package se.islamiskaforbundet.app.quran

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

data class Reciter(val id: String, val name: String)

val RECITERS = listOf(
    Reciter("ar.alafasy", "Mishary Rashid Alafasy"),
    Reciter("ar.husary", "Mahmoud Khalil Al-Husary"),
)

/** App-wide recitation player so audio keeps going while the user browses other screens. */
object QuranPlayer {
    private var player: ExoPlayer? = null

    var playingSurah by mutableStateOf<Int?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private fun get(context: Context): ExoPlayer = player ?: ExoPlayer.Builder(context.applicationContext).build().also { p ->
        p.setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(),
            true,
        )
        p.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) { playingSurah = null }
            }
            override fun onPlayerError(e: PlaybackException) {
                error = "Kunde inte spela upp: kontrollera internetanslutningen"
                playingSurah = null
            }
        })
        player = p
    }

    fun play(context: Context, surah: Int, reciter: String) {
        error = null
        val p = get(context)
        p.setMediaItem(MediaItem.fromUri("https://cdn.islamic.network/quran/audio-surah/128/$reciter/$surah.mp3"))
        p.prepare()
        p.play()
        playingSurah = surah
    }

    fun toggle() {
        val p = player ?: return
        if (p.isPlaying) p.pause() else p.play()
    }

    fun stop() {
        player?.stop()
        playingSurah = null
    }
}
