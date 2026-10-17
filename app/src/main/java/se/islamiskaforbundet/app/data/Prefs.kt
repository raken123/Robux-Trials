package se.islamiskaforbundet.app.data

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import se.islamiskaforbundet.app.prayer.CalcMethod
import se.islamiskaforbundet.app.prayer.Prayer

object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        if (!::sp.isInitialized) {
            sp = context.applicationContext.getSharedPreferences("if_prefs", Context.MODE_PRIVATE)
        }
    }

    // Location (defaults to Stockholm)
    var latitude: Double
        get() = sp.getFloat("lat", 59.3293f).toDouble()
        set(v) = sp.edit().putFloat("lat", v.toFloat()).apply()
    var longitude: Double
        get() = sp.getFloat("lng", 18.0686f).toDouble()
        set(v) = sp.edit().putFloat("lng", v.toFloat()).apply()
    var placeName: String
        get() = sp.getString("place", "Stockholm")!!
        set(v) = sp.edit().putString("place", v).apply()

    var method: CalcMethod
        get() = CalcMethod.entries.getOrElse(sp.getInt("method", 0)) { CalcMethod.MWL }
        set(v) = sp.edit().putInt("method", v.ordinal).apply()
    var hanafiAsr: Boolean
        get() = sp.getBoolean("hanafi", false)
        set(v) = sp.edit().putBoolean("hanafi", v).apply()

    // Alarms: Fajr is on by default.
    fun alarmEnabled(p: Prayer): Boolean = sp.getBoolean("alarm_${p.name}", p == Prayer.FAJR)
    fun setAlarmEnabled(p: Prayer, on: Boolean) = sp.edit().putBoolean("alarm_${p.name}", on).apply()

    /** Raw value of the QR/barcode attached to the user's prayer mat. */
    var matCode: String?
        get() = sp.getString("mat_code", null)
        set(v) = sp.edit().putString("mat_code", v).apply()

    // Prayer log: bitmask of prayed prayers per day.
    fun prayedMask(date: LocalDate): Int = sp.getInt("log_$date", 0)
    fun isPrayed(date: LocalDate, p: Prayer) = prayedMask(date) and (1 shl p.ordinal) != 0
    fun setPrayed(date: LocalDate, p: Prayer, done: Boolean) {
        val m = prayedMask(date)
        val n = if (done) m or (1 shl p.ordinal) else m and (1 shl p.ordinal).inv()
        sp.edit().putInt("log_$date", n).apply()
    }

    // Tasbih
    var tasbihTotal: Long
        get() = sp.getLong("tasbih_total", 0)
        set(v) = sp.edit().putLong("tasbih_total", v).apply()

    var premium: Boolean
        get() = sp.getBoolean("premium", false)
        set(v) = sp.edit().putBoolean("premium", v).apply()

    var reciter: String
        get() = sp.getString("reciter", "ar.alafasy")!!
        set(v) = sp.edit().putString("reciter", v).apply()

    var onboarded: Boolean
        get() = sp.getBoolean("onboarded", false)
        set(v) = sp.edit().putBoolean("onboarded", v).apply()
}
