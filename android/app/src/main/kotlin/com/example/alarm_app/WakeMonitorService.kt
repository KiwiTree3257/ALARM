package com.example.alarm_app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import kotlin.math.abs
import kotlin.math.sqrt

class WakeMonitorService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val handler = Handler(Looper.getMainLooper())

    private var lastMoveTime = 0L
    private var stillThresholdMs = 120_000L  // 기본 2분 정지 → 재취침 판단
    private var checkIntervalMs = 300_000L   // 기본 5분마다 "아직 깨어 있나요?" 알림
    private var checkTimeoutMs = 30_000L     // 기본 30초 내 응답 없으면 재울림

    companion object {
        const val CHANNEL_ID = "wake_monitor_channel"
        const val NOTIFICATION_ID = 1002
        const val ACTION_START = "ACTION_START_MONITOR"
        const val ACTION_STOP = "ACTION_STOP_MONITOR"
        const val EXTRA_STILL_THRESHOLD_SEC = "still_threshold_sec"
        const val EXTRA_CHECK_INTERVAL_SEC = "check_interval_sec"
        const val EXTRA_CHECK_TIMEOUT_SEC = "check_timeout_sec"
        private const val MOVEMENT_THRESHOLD = 1.5f  // 중력 대비 변화량 임계값 (m/s²)
        private const val POLL_INTERVAL_MS = 10_000L // 10초마다 정지 여부 점검
    }

    // 10초마다 실행: 마지막 움직임 이후 경과 시간으로 재취침 판단
    private val stillnessChecker = object : Runnable {
        override fun run() {
            val stillMs = System.currentTimeMillis() - lastMoveTime
            if (stillMs >= stillThresholdMs) {
                triggerReAlarm()
            } else {
                handler.postDelayed(this, POLL_INTERVAL_MS)
            }
        }
    }

    // checkIntervalMs마다 실행: "아직 깨어 있나요?" 알림 발송 후 타임아웃 체크
    private val periodicChecker = object : Runnable {
        override fun run() {
            sendWakeCheckNotification()
            handler.postDelayed(timeoutChecker, checkTimeoutMs)
            handler.postDelayed(this, checkIntervalMs)
        }
    }

    // periodicChecker가 보낸 알림에 checkTimeoutMs 내 응답 없으면 재울림
    private val timeoutChecker = Runnable {
        // TODO: Flutter 쪽에서 "아직 깨어 있음" 응답을 받으면 cancelTimeout()으로 취소
        triggerReAlarm()
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                stillThresholdMs = intent.getIntExtra(EXTRA_STILL_THRESHOLD_SEC, 120) * 1000L
                checkIntervalMs = intent.getIntExtra(EXTRA_CHECK_INTERVAL_SEC, 300) * 1000L
                checkTimeoutMs = intent.getIntExtra(EXTRA_CHECK_TIMEOUT_SEC, 30) * 1000L
                startMonitoring()
            }
            ACTION_STOP -> stopMonitoring()
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        lastMoveTime = System.currentTimeMillis()
        startForeground(NOTIFICATION_ID, buildNotification())

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        handler.postDelayed(stillnessChecker, POLL_INTERVAL_MS)
        handler.postDelayed(periodicChecker, checkIntervalMs)
    }

    private fun stopMonitoring() {
        sensorManager.unregisterListener(this)
        handler.removeCallbacksAndMessages(null)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun cancelTimeout() {
        handler.removeCallbacks(timeoutChecker)
    }

    private fun triggerReAlarm() {
        // Flutter로 재울림 이벤트 전달 (2단계에서 MethodChannel 연결 예정)
        val reAlarmIntent = Intent("com.example.alarm_app.RE_ALARM")
        sendBroadcast(reAlarmIntent)
    }

    private fun sendWakeCheckNotification() {
        // TODO: flutter_local_notifications 또는 직접 NotificationManager로 "아직 깨어 있나요?" 알림
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)
        if (abs(magnitude - SensorManager.GRAVITY_EARTH) > MOVEMENT_THRESHOLD) {
            lastMoveTime = System.currentTimeMillis()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("기상 모니터링 중")
            .setContentText("재취침 감지 활성화됨")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "기상 모니터링",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "재취침 감지 서비스 실행 중"
            setShowBadge(false)
        }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopMonitoring()
        super.onDestroy()
    }
}
