package se.islamiskaforbundet.app.prayer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import se.islamiskaforbundet.app.MainActivity
import se.islamiskaforbundet.app.data.Prefs

object AlarmScheduler {
    const val EXTRA_PRAYER = "prayer"
    const val EXTRA_TIME = "time"

    fun today(date: LocalDate = LocalDate.now()): DayTimes =
        PrayerTimes.compute(date, Prefs.latitude, Prefs.longitude, Prefs.method, Prefs.hanafiAsr)

    /** Next prayer after [now] regardless of alarm settings (for the home countdown). */
    fun nextPrayer(now: LocalDateTime = LocalDateTime.now()): Pair<Prayer, LocalDateTime>? =
        upcoming(now) { true }

    private fun upcoming(now: LocalDateTime, filter: (Prayer) -> Boolean): Pair<Prayer, LocalDateTime>? {
        for (offset in 0L..2L) {
            val times = today(now.toLocalDate().plusDays(offset))
            for (p in Prayer.entries) {
                val t = times[p] ?: continue
                if (t.isAfter(now) && filter(p)) return p to t
            }
        }
        return null
    }

    fun scheduleNext(context: Context) {
        Prefs.init(context)
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = alarmIntent(context, null, null)
        am.cancel(pi)
        val (prayer, time) = upcoming(LocalDateTime.now()) { Prefs.alarmEnabled(it) } ?: return
        scheduleAt(context, prayer, time)
    }

    fun scheduleAt(context: Context, prayer: Prayer, time: LocalDateTime) {
        val am = context.getSystemService(AlarmManager::class.java)
        val millis = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pi = alarmIntent(context, prayer, millis)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (canExact) {
            val show = PendingIntent.getActivity(
                context, 1, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            am.setAlarmClock(AlarmManager.AlarmClockInfo(millis, show), pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        }
    }

    private fun alarmIntent(context: Context, prayer: Prayer?, millis: Long?): PendingIntent {
        val i = Intent(context, AlarmReceiver::class.java)
        if (prayer != null) i.putExtra(EXTRA_PRAYER, prayer.name)
        if (millis != null) i.putExtra(EXTRA_TIME, millis)
        return PendingIntent.getBroadcast(
            context, 0, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
