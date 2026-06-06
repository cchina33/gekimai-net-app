package com.miahina.ongekimai

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // 再起動時にリマインダーを再設定する
            NotificationHelper.scheduleDailyReminder(context)
        }
    }
}
