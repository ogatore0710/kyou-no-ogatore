package jp.ogatore.kyouno.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import jp.ogatore.kyouno.record.RecordLogic
import java.time.Instant

// GO-H1(ホーム画面ウィジェット): markDone()の呼び出し元(MainActivity.kt)から呼ぶ更新フック。
// 独立した小さなSharedPreferences(RecordStoreとは完全に別ファイル。kyono-store.jsonには
// 一切触れない)に「最後に記録した日」だけを覚えておき、KyonoWidget.provideGlanceが
// 「記録した直後〜当日」(congrats)と「それ以外」(good)を区別するために読む。
private const val PREFS_NAME = "kyono_widget_prefs"
private const val KEY_LAST_RECORDED_DATE = "last_recorded_date"

object WidgetUpdater {
    // markDone直後に呼ぶ。ウィジェットの即時更新(GlanceAppWidget.updateAll)はsuspendのため、
    // 呼び出し元でCoroutineScope.launch{}に包むこと。
    suspend fun notifyRecorded(context: Context) {
        val today = RecordLogic.todayStr(Instant.now())
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LAST_RECORDED_DATE, today).apply()
        KyonoWidget().updateAll(context)
    }

    // GO-H1§2-4: 「記録した直後〜当日」= 記録した日と同じ日にきょうやった状態を見ている、
    // という意味に倒す(サマリ相当のデータにタイムスタンプを持たせない設計のため。詳細は
    // WidgetLogic.ktのコメント参照)。
    fun wasRecordedToday(context: Context, now: Instant): Boolean {
        val today = RecordLogic.todayStr(now)
        val last = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_RECORDED_DATE, null)
        return last == today
    }
}
