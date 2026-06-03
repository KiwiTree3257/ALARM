package com.example.alarm_app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            // alarm 패키지가 AlarmManager 예약을 자동 복구함
            // 추가 커스텀 데이터 복구가 필요하면 여기에 구현 (2단계 이후)
        }
    }
}
