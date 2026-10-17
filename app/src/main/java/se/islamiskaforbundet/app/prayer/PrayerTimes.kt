package se.islamiskaforbundet.app.prayer

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

enum class Prayer(val sv: String, val ar: String) {
    FAJR("Fajr", "الفجر"),
    DHUHR("Dhuhr", "الظهر"),
    ASR("Asr", "العصر"),
    MAGHRIB("Maghrib", "المغرب"),
    ISHA("Isha", "العشاء"),
}

/** Fajr angle, and Isha as either an angle or fixed minutes after Maghrib. */
enum class CalcMethod(val label: String, val fajr: Double, val isha: Double, val ishaMinutes: Boolean = false) {
    MWL("Muslim World League", 18.0, 17.0),
    ISNA("ISNA (Nordamerika)", 15.0, 15.0),
    EGYPT("Egyptian General Authority", 19.5, 17.5),
    MAKKAH("Umm al-Qura, Mecka", 18.5, 90.0, ishaMinutes = true),
    KARACHI("Karachi", 18.0, 18.0),
}

data class DayTimes(
    val date: LocalDate,
    val fajr: LocalDateTime?,
    val sunrise: LocalDateTime?,
    val dhuhr: LocalDateTime?,
    val asr: LocalDateTime?,
    val maghrib: LocalDateTime?,
    val isha: LocalDateTime?,
) {
    operator fun get(p: Prayer): LocalDateTime? = when (p) {
        Prayer.FAJR -> fajr
        Prayer.DHUHR -> dhuhr
        Prayer.ASR -> asr
        Prayer.MAGHRIB -> maghrib
        Prayer.ISHA -> isha
    }
}

/**
 * Astronomical prayer-time calculation (after the PrayTimes.org algorithm), with the
 * angle-based high-latitude rule so Fajr and Isha stay defined in Swedish summers.
 */
object PrayerTimes {
    private fun dsin(d: Double) = sin(Math.toRadians(d))
    private fun dcos(d: Double) = cos(Math.toRadians(d))
    private fun dtan(d: Double) = tan(Math.toRadians(d))
    private fun darcsin(x: Double) = Math.toDegrees(asin(x))
    private fun darccos(x: Double) = Math.toDegrees(acos(x))
    private fun darctan2(y: Double, x: Double) = Math.toDegrees(atan2(y, x))
    private fun darccot(x: Double) = Math.toDegrees(atan(1 / x))
    private fun fix(a: Double, b: Double): Double { val r = a - b * floor(a / b); return if (r < 0) r + b else r }
    private fun fixAngle(a: Double) = fix(a, 360.0)
    private fun fixHour(a: Double) = fix(a, 24.0)

    private fun julian(year: Int, month: Int, day: Int): Double {
        var y = year; var m = month
        if (m <= 2) { y -= 1; m += 12 }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    /** Returns (declination, equation of time). */
    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = fixHour(darctan2(dcos(e) * dsin(l), dcos(l)) / 15)
        val eqt = q / 15 - ra
        val decl = darcsin(dsin(e) * dsin(l))
        return decl to eqt
    }

    fun compute(
        date: LocalDate,
        lat: Double,
        lng: Double,
        method: CalcMethod,
        hanafi: Boolean,
        zone: ZoneId = ZoneId.systemDefault(),
    ): DayTimes {
        val tz = zone.rules.getOffset(date.atTime(12, 0)).totalSeconds / 3600.0
        val jDate = julian(date.year, date.monthValue, date.dayOfMonth) - lng / (15 * 24)

        fun midDay(t: Double): Double = fixHour(12 - sunPosition(jDate + t).second)
        fun sunAngleTime(angle: Double, t: Double, ccw: Boolean): Double {
            val decl = sunPosition(jDate + t).first
            val noon = midDay(t)
            val tt = darccos((-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat))) / 15
            return noon + if (ccw) -tt else tt
        }
        fun asrTime(factor: Double, t: Double): Double {
            val decl = sunPosition(jDate + t).first
            val angle = -darccot(factor + dtan(abs(lat - decl)))
            return sunAngleTime(angle, t, false)
        }

        var fajr = sunAngleTime(method.fajr, 5 / 24.0, true)
        val sunrise = sunAngleTime(0.833, 6 / 24.0, true)
        val dhuhr = midDay(12 / 24.0)
        val asr = asrTime(if (hanafi) 2.0 else 1.0, 13 / 24.0)
        val sunset = sunAngleTime(0.833, 18 / 24.0, false)
        var isha = if (method.ishaMinutes) sunset + method.isha / 60 else sunAngleTime(method.isha, 18 / 24.0, false)

        // High-latitude adjustment (angle-based): cap Fajr/Isha to a portion of the night.
        val night = fixHour(sunrise - sunset)
        if (!night.isNaN()) {
            val fajrPortion = method.fajr / 60 * night
            if (fajr.isNaN() || fixHour(sunrise - fajr) > fajrPortion) fajr = sunrise - fajrPortion
            if (!method.ishaMinutes) {
                val ishaPortion = method.isha / 60 * night
                if (isha.isNaN() || fixHour(isha - sunset) > ishaPortion) isha = sunset + ishaPortion
            }
        }

        val shift = tz - lng / 15
        val start = date.atStartOfDay()
        fun toTime(h: Double, extraMinutes: Long = 0): LocalDateTime? =
            if (h.isNaN()) null
            else start.plusSeconds(((h + shift) * 3600).toLong() + 30).withSecond(0).withNano(0).plusMinutes(extraMinutes)

        return DayTimes(
            date = date,
            fajr = toTime(fajr),
            sunrise = toTime(sunrise),
            // Small safety margins after zenith and sunset, as most Swedish timetables use.
            dhuhr = toTime(dhuhr, 1),
            asr = toTime(asr),
            maghrib = toTime(sunset, 1),
            isha = toTime(isha),
        )
    }
}
