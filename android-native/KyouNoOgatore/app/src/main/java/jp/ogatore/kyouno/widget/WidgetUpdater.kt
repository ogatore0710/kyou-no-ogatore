package jp.ogatore.kyouno.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

// GO-H1(ホーム画面ウィジェット): markDone()の呼び出し元(MainActivity.kt)から呼ぶ更新フック。
// 独立した小さなSharedPreferences(RecordStoreとは完全に別ファイル。kyono-store.jsonには
// 一切触れない)に「最後に記録した時刻(epoch millis)」だけを覚えておき、KyonoWidget.provideGlanceが
// WidgetLogic.compute()へ渡す(「記録から4時間はcongrats・それ以降は当日いっぱいgood」の
// 判定はWidgetLogic.CELEBRATE_WINDOW_MILLIS1箇所だけで行う)。
private const val PREFS_NAME = "kyono_widget_prefs"
private const val KEY_LAST_RECORDED_AT_MILLIS = "last_recorded_at_millis"

object WidgetUpdater {
    // markDone直後に呼ぶ。ウィジェットの即時更新(GlanceAppWidget.updateAll)はsuspendのため、
    // 呼び出し元でCoroutineScope.launch{}に包むこと。
    suspend fun notifyRecorded(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_LAST_RECORDED_AT_MILLIS, System.currentTimeMillis()).apply()
        KyonoWidget().updateAll(context)
    }

    fun recordedAtMillis(context: Context): Long? {
        val v = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_RECORDED_AT_MILLIS, -1L)
        return if (v < 0) null else v
    }
}
