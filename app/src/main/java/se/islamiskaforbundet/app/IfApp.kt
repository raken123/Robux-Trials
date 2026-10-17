package se.islamiskaforbundet.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import se.islamiskaforbundet.app.data.Prefs
import se.islamiskaforbundet.app.prayer.AlarmScheduler

class IfApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        val nm = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            ALARM_CHANNEL, "Böneväckning", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Ringer vid bönetid tills du skannar din bönematta"
            setSound(null, null)
            enableVibration(false)
        }
        nm.createNotificationChannel(channel)
        AlarmScheduler.scheduleNext(this)
    }

    companion object {
        const val ALARM_CHANNEL = "prayer_alarm"
    }
}
