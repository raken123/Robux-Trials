package se.islamiskaforbundet.app.prayer

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import se.islamiskaforbundet.app.IfApp
import se.islamiskaforbundet.app.R

/** Rings until the prayer mat is scanned in [AlarmActivity] (or after a safety timeout). */
class AlarmService : Service() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val prayer = runCatching { Prayer.valueOf(intent?.getStringExtra(AlarmScheduler.EXTRA_PRAYER) ?: "") }
            .getOrDefault(Prayer.FAJR)
        val time = intent?.getLongExtra(AlarmScheduler.EXTRA_TIME, System.currentTimeMillis()) ?: System.currentTimeMillis()

        val notification = buildNotification(this, prayer, time)
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
        )
        startRinging()
        running = true
        currentPrayer = prayer
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ stopSelf() }, TIMEOUT_MS)

        // Try to bring the scan screen up directly (works on the lock screen via full-screen intent too).
        runCatching { startActivity(AlarmActivity.intent(this, prayer, time).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        return START_NOT_STICKY
    }

    private fun startRinging() {
        if (player != null) return
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmService, uri)
                isLooping = true
                prepare()
                start()
            }
        }.getOrNull()

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 600), 0))
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        player?.runCatching { stop(); release() }
        player = null
        vibrator?.cancel()
        running = false
        currentPrayer = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "se.islamiskaforbundet.app.STOP_ALARM"
        private const val NOTIFICATION_ID = 42
        private const val TIMEOUT_MS = 15 * 60 * 1000L

        @Volatile var running = false
            private set
        @Volatile var currentPrayer: Prayer? = null
            private set

        fun stop(context: Context) {
            context.startService(Intent(context, AlarmService::class.java).setAction(ACTION_STOP))
        }

        private fun buildNotification(context: Context, prayer: Prayer, time: Long): Notification {
            val open = PendingIntent.getActivity(
                context, 2, AlarmActivity.intent(context, prayer, time),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            return NotificationCompat.Builder(context, IfApp.ALARM_CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_prayer)
                .setContentTitle("Dags för ${prayer.sv}")
                .setContentText("Skanna din bönematta för att stänga av larmet")
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setContentIntent(open)
                .setFullScreenIntent(open, true)
                .addAction(0, "Skanna bönematta", open)
                .build()
        }
    }
}
