package com.miahina.ongekimai

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.Calendar
import kotlin.random.Random

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. 通知をタップしたときにアプリ（MainActivity）を開く設定
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // 💡 2. 通常メッセージ（不定期用）の一覧を作成
        val normalMessages = listOf(
            "にゃふふふ♪元気にしてた？",
            "にゃっほー♪早速遊ぼっ！",
            "待ってて！みずみずしいフルーツたち~"
        )

        // 💡 3. 現在の「月」を取得して月替わりセリフを用意
        val currentCalendar = Calendar.getInstance()
        val currentMonth = currentCalendar.get(Calendar.MONTH) + 1 // 1〜12の値

        val monthlyMessage = when (currentMonth) {
            1 -> "ハッピーニューイヤー！ねぇねぇ、初詣にはもう行った？一緒に行かない？"
            2 -> "毎年バレンタインは大忙しなんだ～友チョコ渡したい子がたくさんいるの！"
            3 -> "むむむ……美亜ちゃんセンサーが発動している……近くに可愛い女の子がいる予感！"
            4 -> "はいっ！美亜ちゃんから提案があります！身体測定は生徒の自主性を重んじて、生徒同士でやるべきだと思います！"
            5 -> "この前、美味しそうなスイーツのお店を見つけたんだ～フルーツ山盛りで美味しそうなの～！ねぇ、帰りに寄っていこうよ～"
            6 -> "おぉ？きれいな紫陽花発見！パシャっとな♪あとでみんなにも教えてあげなきゃ～"
            7 -> "にゃふふ～日焼け止めを塗ってあげよう～はっ！案外、日焼け跡も捨てがたいのでは…？"
            8 -> "今月あたしの誕生日なんだけど～おねだり…していい？にゃふふ、えーとね…あなたがほしい～な♪にゃんてね"
            9 -> "にゃふふ～昨日は色んなフルーツをいっぱい楽しんじゃった～うん？もちろんフルーツ狩りの話だよ？にゃふふっ♪"
            10 -> "トリックオアトリート！なーんてまどろっこしい！いたずらしてもいい？してもいいよねー！？しちゃうからーーー！！！"
            11 -> "運動のあとはマッサージした方がいいよ？にゃっふふ♪美亜ちゃんがやってあげようか~"
            12 -> "メリークリスマース~美亜ちゃんサンタから特別なプレゼント~それは、あ・た・し♪"
            else -> normalMessages.random()
        }

        // 💡 4. 【確率によるメッセージの決定】
        // 50%の確率で「今月のセリフ」、残り50%の確率で「通常の3つからランダム」を選びます。
        // （好みに応じて確率の比率は変更可能です）
        val finalMessage = if (Random.nextBoolean()) {
            monthlyMessage
        } else {
            normalMessages.random()
        }

        // 5. 右側に表示する四角い画像の読み込み
        val rightImageBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.miadda)

        // 6. 通知全体の組み立て（チャンネルIDは mia_monthlogin_channel をそのまま使用）
        val builder = NotificationCompat.Builder(context, "mia_monthlogin_channel")
            .setSmallIcon(R.drawable.ic_stat_name) // 自作の白抜きアイコン
            .setContentTitle("美亜")
            .setContentText(finalMessage)
            .setLargeIcon(rightImageBitmap) // 右側の四角い画像
            .setStyle(NotificationCompat.BigTextStyle().bigText(finalMessage))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // 7. 通知を発行 (ID: 999)
        notificationManager.notify(999, builder.build())

        // 🔄 8. 翌日の同じ時間へリマインダーを自動再スケジュール（ループ処理）
        val credentialManager = CredentialManager(context)
        if (credentialManager.isReminderEnabled()) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val alarmIntent = Intent(context, ReminderReceiver::class.java)
            val alarmPendingIntent = PendingIntent.getBroadcast(
                context, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val hour = credentialManager.getReminderHour()
            val minute = credentialManager.getReminderMinute()

            val nextCalendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                add(Calendar.DAY_OF_YEAR, 1) // 確実に「明日」にする
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextCalendar.timeInMillis,
                    alarmPendingIntent
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextCalendar.timeInMillis,
                    alarmPendingIntent
                )
            }
            Log.d("ReminderReceiver", "翌日のリマインダーを再予約しました: ${hour}:${String.format("%02d", minute)}")
        }
    }
}