package jp.ogatore.kyouno

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
    val themeSetting = store.get("theme", "auto")
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
                        entries.forEachIndexed { i, (date, memo) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = androidx.compose.ui.Alignment.Top) {
                                Text(
                                    date.substring(5).replace("-", "/"),
                                    color = colors.sub, fontWeight = FontWeight.Black, fontSize = 15.sp,
                                    modifier = Modifier.testTag("diaryDate_$date"),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(memo, color = colors.ink, fontSize = 15.sp, modifier = Modifier.testTag("diaryMemo_$date"))
                            }
                            // index.html:271 border-bottom:1px dashed var(--line)の簡略化(実線)。
                            // Composeに標準の破線ボーダーが無いため、区切り線としての機能を優先し実線で近似。
                            if (i < entries.size - 1) {
                                Row(Modifier.fillMaxWidth()) {
                                    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
