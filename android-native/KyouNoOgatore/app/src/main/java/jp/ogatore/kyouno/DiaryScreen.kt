package jp.ogatore.kyouno

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.ogatore.kyouno.record.RecordLogic
import jp.ogatore.kyouno.record.RecordStore

// ひとことにっき機能欠落修正タスク(TASK-C2-2026-07-26-diary-list-missing.md): app-record.js:267-273
// renderDiary()の1:1移植(保存済みkyono_memosの一覧表示のみ・保存/編集/削除ロジックには触れない)。
// index.html:884 #fun内「ひとことにっき」カードの1:1移植(見出しアイコンは既存KyonoIcon.Notesを流用)。
@Composable
fun DiaryScreen(store: RecordStore, onBack: () -> Unit) {
    // GO-G6(5視点ワンループ): システム「もどる」を拾い、既存の「◀ もどる」ボタンと同じonBackへ。
    BackHandler(onBack = onBack)
    val themeSetting = store.get("theme", "light")
    KyonoTheme(themeSetting, bigText = store.get("bigtext", true)) {
        val colors = LocalKyonoColors.current
        // index.html:269 keys=Object.keys(memos).sort().reverse().slice(0,7)の1:1移植(新しい順に最大7件)。
        val entries = remember { RecordLogic.loadMemos(store).entries.sortedByDescending { it.key }.take(7) }

        Column(Modifier.fillMaxSize().background(colors.bg).padding(16.dp)) {
            KyonoLineButton("◀ もどる", onBack, Modifier.testTag("diaryBackBtn"))
            Spacer(Modifier.height(12.dp))
            KyonoCard {
                KyonoSectionHeader(KyonoIcon.Notes, "ひとことにっき", fill = colors.pinkSoft)
                Spacer(Modifier.height(10.dp))
                if (entries.isEmpty()) {
                    Text(
                        "「きょうやった！」のあとにメモをのこせます",
                        color = colors.sub, fontSize = 14.sp, modifier = Modifier.testTag("diaryEmpty"),
                    )
                } else {
                    Column(Modifier.testTag("diaryList")) {
                        entries.forEach { (date, memo) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = androidx.compose.ui.Alignment.Top) {
                                Text(
                                    date.substring(5).replace("-", "/"),
                                    color = colors.sub, fontWeight = FontWeight.Black, fontSize = 15.sp,
                                    modifier = Modifier.testTag("diaryDate_$date"),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(memo, color = colors.ink, fontSize = 15.sp, modifier = Modifier.testTag("diaryMemo_$date"))
                            }
                            // TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §8: index.html:271
                            // border-bottom:1px dashed var(--line)の1:1移植(以前は実線で近似としていた)。
                            // Composeに標準の破線ボーダーが無いためCanvas+dashPathEffectで描画。
                            // Web版は全行(最終行含む)に付くため、除外条件は付けない。
                            DashedDivider(colors.line)
                        }
                        // UX13案・案10(2026-07-30): 表示は新しい順に最大7件(index.html:269と同じ)だが、
                        // 8件目以降も消えたわけではなく、マイ記録のカレンダー日タップ(dayInfo)で読める。
                        // その接続を知る手がかりがゼロだったため、G15と同格のトーンで1行案内する
                        // (機能追加はゼロ・既存経路の案内のみ)。
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "まえのメモは マイ記録のカレンダーで日にちをタップすると見られます",
                            color = colors.sub, fontWeight = FontWeight.Black, fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

// index.html:271 border-bottom:1px dashed var(--line)の1:1移植。
@Composable
private fun DashedDivider(color: androidx.compose.ui.graphics.Color) {
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(size.width, 0f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
            cap = StrokeCap.Butt,
        )
    }
}
