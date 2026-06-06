package com.miahina.ongekimai

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.*

object NotificationHelper {

    fun createNotificationChannel(context: Context) {
        val notificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        try {
            val existingChannels = notificationManager.notificationChannels
            for (channel in existingChannels) {
                notificationManager.deleteNotificationChannel(channel.id)
            }
        } catch (e: Exception) {
            Log.e("Notification", "チャンネルの削除中にエラーが発生しました", e)
        }

        val channel1 = NotificationChannel(
            "mia_login_channel",
            "美亜からのメッセージ",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "アプリからの通知やアップデート情報をお知らせします"
        }
        notificationManager.createNotificationChannel(channel1)

        val monthChannel = NotificationChannel(
            "mia_monthlogin_channel",
            "美亜からの月替わり通知",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "月ごとにメッセージが変化する毎日のリマインダー通知です"
        }
        notificationManager.createNotificationChannel(monthChannel)
    }

    fun scheduleDailyReminder(context: Context) {
        val credentialManager = CredentialManager(context)
        val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        if (!credentialManager.isReminderEnabled()) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val hour = credentialManager.getReminderHour()
        val minute = credentialManager.getReminderMinute()

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis() + 10000) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    /**
     * リマインダー通知を送るためのインテントを作成し、アラームを再設定する
     */
    fun scheduleNextReminder(context: Context) {
        scheduleDailyReminder(context)
    }
}
