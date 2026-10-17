package se.islamiskaforbundet.app.quran

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class Ayah(val number: Int, val arabic: String, val swedish: String)

data class SurahText(val arabicName: String, val ayahs: List<Ayah>)

/** Arabic (Uthmani) text with Knut Bernström's Swedish translation, cached on disk after first download. */
object QuranRepository {
    private const val API = "https://api.alquran.cloud/v1/surah/%d/editions/quran-uthmani,sv.bernstrom"

    suspend fun load(context: Context, number: Int): SurahText = withContext(Dispatchers.IO) {
        val cache = File(context.filesDir, "quran/surah_$number.json")
        val json = if (cache.exists()) {
            cache.readText()
        } else {
            val conn = URL(API.format(number)).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 20000
            try {
                if (conn.responseCode != 200) error("HTTP ${conn.responseCode}")
                conn.inputStream.bufferedReader().use { it.readText() }.also {
                    cache.parentFile?.mkdirs()
                    cache.writeText(it)
                }
            } finally {
                conn.disconnect()
            }
        }
        parse(json)
    }

    private fun parse(json: String): SurahText {
        val data = JSONObject(json).getJSONArray("data")
        val ar = data.getJSONObject(0)
        val sv = data.getJSONObject(1).getJSONArray("ayahs")
        val arAyahs = ar.getJSONArray("ayahs")
        val list = (0 until arAyahs.length()).map { i ->
            val a = arAyahs.getJSONObject(i)
            Ayah(a.getInt("numberInSurah"), a.getString("text"), sv.optJSONObject(i)?.optString("text").orEmpty())
        }
        return SurahText(ar.getString("name"), list)
    }
}
