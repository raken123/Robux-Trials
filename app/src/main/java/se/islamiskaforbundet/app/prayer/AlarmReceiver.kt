package se.islamiskaforbundet.app.prayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayer = intent.getStringExtra(AlarmScheduler.EXTRA_PRAYER) ?: Prayer.FAJR.name
        val svc = Intent(context, AlarmService::class.java)
            .putExtra(AlarmScheduler.EXTRA_PRAYER, prayer)
            .putExtra(AlarmScheduler.EXTRA_TIME, intent.getLongExtra(AlarmScheduler.EXTRA_TIME, System.currentTimeMillis()))
        runCatching { context.startForegroundService(svc) }
        // Queue the following prayer right away; stopping this alarm must not be required for it.
        AlarmScheduler.scheduleNext(context)
    }
}
