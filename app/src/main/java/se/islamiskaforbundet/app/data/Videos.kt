package se.islamiskaforbundet.app.data

import android.content.Context
import org.json.JSONObject
import se.islamiskaforbundet.app.R

data class Cue(val seconds: Double, val text: String)

data class AppVideo(
    val slug: String,
    val title: String,
    val subtitle: String,
    val premium: Boolean,
    val durationSeconds: Double,
    val cues: List<Cue>,
    val rawRes: Int,
    val thumbRes: Int,
)

/** The app's own animated videos (rendered by tools/videos and bundled in res/raw). */
object Videos {
    // Explicit references keep the resource shrinker from removing the files.
    private val RES = mapOf(
        "pelare" to (R.raw.video_pelare to R.drawable.thumb_pelare),
        "salah" to (R.raw.video_salah to R.drawable.thumb_salah),
        "wudu" to (R.raw.video_wudu to R.drawable.thumb_wudu),
        "tasbih" to (R.raw.video_tasbih to R.drawable.thumb_tasbih),
        "bonetider" to (R.raw.video_bonetider to R.drawable.thumb_bonetider),
        "hajj" to (R.raw.video_hajj to R.drawable.thumb_hajj),
        "ramadan" to (R.raw.video_ramadan to R.drawable.thumb_ramadan),
        "zakat" to (R.raw.video_zakat to R.drawable.thumb_zakat),
    )

    @Volatile private var cache: List<AppVideo>? = null

    fun all(context: Context): List<AppVideo> = cache ?: load(context).also { cache = it }

    fun find(context: Context, slug: String): AppVideo? = all(context).firstOrNull { it.slug == slug }

    private fun load(context: Context): List<AppVideo> {
        val text = context.assets.open("videos.json").bufferedReader().use { it.readText() }
        val arr = org.json.JSONArray(text)
        return (0 until arr.length()).mapNotNull { i ->
            val o: JSONObject = arr.getJSONObject(i)
            val slug = o.getString("slug")
            val (raw, thumb) = RES[slug] ?: return@mapNotNull null
            val cues = o.getJSONArray("cues").let { c ->
                (0 until c.length()).map { k -> c.getJSONObject(k).let { Cue(it.getDouble("t"), it.getString("text")) } }
            }
            AppVideo(slug, o.getString("title"), o.getString("subtitle"), o.getBoolean("premium"),
                o.getDouble("duration"), cues, raw, thumb)
        }
    }
}
