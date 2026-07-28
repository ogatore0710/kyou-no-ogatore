package jp.ogatore.kyouno.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import java.time.Instant

// GO-H1(ホーム画面ウィジェット): markDone()の呼び出し元(MainActivity.kt)から呼ぶ更新フック。
// 独立した小さなSharedPreferences(RecordStoreとは完全に別ファイル。kyono-store.jsonには
// 一切触れない)に「最後に記録した時刻(epoch millis)」だけを覚えておき、KyonoWidget.provideGlanceが
// 「記録から4時間はcongrats・それ以降は当日いっぱいgood」(alan5差し戻しD3・iOS側に統一)を
// 判定するために読む。
private const val PREFS_NAME = "kyono_widget_prefs"
private const val KEY_LAST_RECORDED_AT_MILLIS = "last_recorded_at_millis"
// GO-H1 D3(alan5差し戻し2026-07-28): iOS版のcelebrateUntil(記録+4時間)に合わせる。
const val CELEBRATE_WINDOW_MILLIS = 4L * 3600 * 1000

object WidgetUpdater {
    // markDone直後に呼ぶ。ウィジェットの即時更新(GlanceAppWidget.updateAll)はsuspendのため、
    // 呼び出し元でCoroutineScope.launch{}に包むこと。
    suspend fun notifyRecorded(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_LAST_RECORDED_AT_MILLIS, System.currentTimeMillis()).apply()
        KyonoWidget().updateAll(context)
    }

    // GO-H1 D3(alan5差し戻し): 「記録直後」= 記録した時刻から4時間以内(iOS版celebrateUntilと同じ
    // 考え方)。以前は「記録した日と同じ日か」の日付比較だったため、記録した日は一日じゅうtrueに
    // なりGOODへ絶対に落ちないデッドコードだった(alan5指摘のとおり)。
    fun recordedAtMillis(context: Context): Long? {
        val v = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_RECORDED_AT_MILLIS, -1L)
        return if (v < 0) null else v
    }

    fun isCelebrating(context: Context, now: Instant): Boolean {
        val recordedAt = recordedAtMillis(context) ?: return false
        return now.toEpochMilli() - recordedAt < CELEBRATE_WINDOW_MILLIS
    }
}
