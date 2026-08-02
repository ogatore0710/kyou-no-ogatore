package jp.ogatore.kyouno

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import jp.ogatore.kyouno.record.RecordLogic
import jp.ogatore.kyouno.record.RecordStore
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

// TASK-C2-2026-07-27-local-notifications.md: index.html 38箇所の「カレンダー登録/ショートカット
// 自動化」案内(PWAが通知を送れないことへの回避策)をネイティブのローカル通知で置き換える。
// index.html:1974-1979 ANCHORSのtm/gの1:1移植(通知本文のみ。freeだけ本人指示で通知用の別文言に
// 差し替え・ANCHORS.free.g自体=アプリ内挨拶は変更しない)。
object DailyNotifications {
    const val CHANNEL_ID = "daily_stretch"
    private const val REQUEST_CODE = 1001

    data class AnchorNotif(val key: String, val defaultHour: Int, val defaultMinute: Int, val body: String)

    val ANCHOR_NOTIFS = listOf(
        AnchorNotif("asa", 7, 30, "おはようございます朝の1本いきましょう！"),
        AnchorNotif("furo", 20, 30, "おふろ上がりの1本いきましょう"),
        AnchorNotif("neru", 21, 30, "寝るまえの1本できょうをしめくくりましょう"),
        // index.html:1978 ANCHORS.free.gは「すきな時間にきょうの1本をどうぞ🌿」(アプリ内挨拶用)。
        // 通知では本人指示により「夜がおすすめの時間」を伝える文言に差し替える(アプリ内挨拶自体は不変)。
        AnchorNotif("free", 20, 0, "夜はストレッチにおすすめの時間だよ きょうの1本どうぞ"),
    )

    private fun storeFile(context: Context): File = File(context.filesDir, "kyono-store.json")

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(CHANNEL_ID, "毎日のおしらせ", NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = "決めた時間に、一言だけ、やわらかくお知らせします"
            manager.createNotificationChannel(channel)
        }
    }

    // index.html:2003 renderIcs()のdef計算(未設定時はfree扱い)+保存済みicstimeがあればそちらを優先、
    // の1:1移植。設定画面の「カレンダーのおしらせ時間」と同じ値を通知時刻としても使う(タスク仕様どおり
    // 新しい設定を増やさない)。
    private fun resolveTime(store: RecordStore): Pair<Int, Int> {
        val anchorKey = store.get<String?>("anchor", null)
        val anchorInfo = ANCHOR_NOTIFS.find { it.key == anchorKey } ?: ANCHOR_NOTIFS.last()
        val saved = store.get<String?>("icstime", null)
        val hour = saved?.substringBefore(":")?.toIntOrNull() ?: anchorInfo.defaultHour
        val minute = saved?.substringAfter(":")?.toIntOrNull() ?: anchorInfo.defaultMinute
        return hour to minute
    }

    private fun notifBody(store: RecordStore): String {
        val anchorKey = store.get<String?>("anchor", null)
        val info = ANCHOR_NOTIFS.find { it.key == anchorKey } ?: ANCHOR_NOTIFS.last()
        return info.body
    }

    // index.html:3970 checkDoneNudge等と同じ「今日の記録が済んでいるか」の判定を再利用する
    // (判定ロジックはRecordLogicのみ・ここでは再実装しない)。
    private fun isTodayDone(store: RecordStore, zone: ZoneId): Boolean {
        val today = RecordLogic.todayStr(Instant.now(), zone)
        return RecordLogic.loadStreak(store).dates.contains(today)
    }

    // 次回の発火時刻(epoch millis)を計算する。今日の時刻がまだ来ておらず今日が未記録ならその時刻、
    // それ以外(時刻を過ぎた/今日はもう記録済み)は翌日の同時刻。
    private fun computeNextTrigger(store: RecordStore, zone: ZoneId = ZoneId.systemDefault()): Long {
        val (hour, minute) = resolveTime(store)
        val now = ZonedDateTime.now(zone)
        var candidate = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        val todayDone = isTodayDone(store, zone)
        if (candidate.isBefore(now) || candidate.isEqual(now) || todayDone) {
            candidate = candidate.plusDays(1)
        }
        return candidate.toInstant().toEpochMilli()
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyNotificationReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    fun scheduleNext(context: Context) {
        val store = RecordStore.forFile(storeFile(context))
        if (!store.get("notif_enabled", false)) {
            cancel(context)
            return
        }
        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val trigger = computeNextTrigger(store)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent(context))
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context))
    }

    fun showNotificationIfDue(context: Context) {
        val store = RecordStore.forFile(storeFile(context))
        if (store.get("notif_enabled", false) && !isTodayDone(store, ZoneId.systemDefault())) {
            ensureChannel(context)
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val contentIntent = PendingIntent.getActivity(
                context, REQUEST_CODE, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("きょうのオガトレ")
                .setContentText(notifBody(store))
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .build()
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(REQUEST_CODE, notification)
        }
        // 表示の有無に関わらず、次回分を組み直して連鎖を維持する。
        scheduleNext(context)
    }
}

class DailyNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        DailyNotifications.showNotificationIfDue(context)
    }
}

class NotificationBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            DailyNotifications.scheduleNext(context)
        }
    }
}
