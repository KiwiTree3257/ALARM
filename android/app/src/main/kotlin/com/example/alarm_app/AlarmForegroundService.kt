package com.example.alarm_app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat

class AlarmForegroundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var audioManager: AudioManager? = null
    private var originalAlarmVolume = -1

    companion object {
        const val CHANNEL_ID = "alarm_foreground_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START_ALARM"
        const val ACTION_STOP = "ACTION_STOP_ALARM"
        const val EXTRA_GRADE = "alarm_grade"
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val grade = intent.getStringExtra(EXTRA_GRADE) ?: "B"
                startAlarm(grade)
            }
            ACTION_STOP -> stopAlarm()
        }
        return START_STICKY
    }

    private fun startAlarm(grade: String) {
        acquireWakeLock(grade)
        applyVolumePolicy(grade)
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    private fun stopAlarm() {
        releaseWakeLock()
        restoreVolume()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun acquireWakeLock(grade: String) {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        // C/D 등급은 화면까지 켜서 잠금화면 침투, 그 외는 CPU만 유지
        val levelFlag = if (grade == "C" || grade == "D") {
            @Suppress("DEPRECATION")
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP
        } else {
            PowerManager.PARTIAL_WAKE_LOCK
        }
        wakeLock = pm.newWakeLock(levelFlag, "alarm_app:AlarmWakeLock")
        wakeLock?.acquire(30 * 60 * 1000L) // 최대 30분
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = null
    }

    private fun applyVolumePolicy(grade: String) {
        val am = audioManager ?: return
        val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        originalAlarmVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)

        val targetVolume = when (grade) {
            "A" -> (maxVolume * 0.4).toInt()
            "B" -> (maxVolume * 0.65).toInt()
            "C" -> (maxVolume * 0.85).toInt()
            "D" -> maxVolume
            else -> (maxVolume * 0.65).toInt()
        }
        am.setStreamVolume(AudioManager.STREAM_ALARM, targetVolume, 0)
        // TODO: C/D 등급 — 볼륨 버튼 입력 차단은 MainActivity에서 onKeyDown 오버라이드로 처리
    }

    private fun restoreVolume() {
        if (originalAlarmVolume >= 0) {
            audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, originalAlarmVolume, 0)
            originalAlarmVolume = -1
        }
    }

    private fun buildNotification(): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("알람")
            .setContentText("알람이 울리고 있습니다")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "알람 서비스",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "알람 실행 중 표시되는 알림"
            setShowBadge(false)
        }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
