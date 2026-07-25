package jp.ogatore.kyouno

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.ogatore.kyouno.record.RecordStore
import jp.ogatore.kyouno.safety.SafetyGate

// ネイティブ移植 Step 7b(マスタープラン§6 Step 7b): 使い方タブ=よくあるしつもんUI(index.html
// filterFaq()/toggleFaqGroup()の1:1移植)。検索の正規化はWeb版と同じくSafetyGate.norm(Step2で
// 移植済み・安全判定4関数の1つ)を再利用する——判定ロジックではなく正規化ユーティリティとしての
// 再利用であり、UI層に判定を書いていないことに変わりはない(マスタープラン§3-2の隔離対象=
// crisisHit/redFlagHit/redFlagKindの3関数であり、normはテキスト正規化のみで判定を含まない)。
// A2HS関連項目はGuideData.ktでhidden=trueとしてデータには残しつつ、この画面では表示しない
// (§2-2「非表示化して移植（削除しない）」)。
//
// ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
// Phase 3: index.html:180-197,426-429 .faq-g/.faq details/.searchboxの1:1移植。
@Composable
fun GuideScreen(store: RecordStore, onBack: () -> Unit) {
    val themeSetting = store.get("theme", "auto")
    KyonoTheme(themeSetting) {
        val colors = LocalKyonoColors.current
        var query by remember { mutableStateOf("") }
        val openGroups = remember { mutableStateMapOf<String, Boolean>().apply { put(FAQ_GROUPS[0].title, true) } }
        val openItems = remember { mutableStateMapOf<String, Boolean>() }

        val nq = remember(query) { SafetyGate.norm(query) }

        Column(Modifier.fillMaxSize().background(colors.bg).padding(16.dp)) {
            KyonoLineButton("◀ もどる", onBack, Modifier.testTag("guideBackBtn"))
            Spacer(Modifier.height(12.dp))
            KyonoSectionHeader(KyonoIcon.Question, "よくあるしつもん", fill = colors.coralSoft)
            Spacer(Modifier.height(4.dp))
            Text("しつもんをタップすると こたえがひらきます", color = colors.sub, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))

            // index.html:426-429 .searchbox
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().testTag("faqSearch"),
                placeholder = { Text("🔍 キーワードでさがす（例: 記録 / 機種変更 / 痛い）", color = colors.subFaint) },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.card, unfocusedContainerColor = colors.card,
                    focusedIndicatorColor = colors.line, unfocusedIndicatorColor = colors.line,
                ),
            )
            Spacer(Modifier.height(12.dp))

            LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("faqList")) {
                for (group in FAQ_GROUPS) {
                    val visibleItems = group.items.filter { item ->
                        if (item.hidden) return@filter false
                        if (nq.isEmpty()) return@filter true
                        SafetyGate.norm(item.q).contains(nq) || SafetyGate.norm(item.a).contains(nq)
                    }
                    if (visibleItems.isEmpty()) continue
                    item {
                        // index.html:180-183 .faq-g(グループ見出し・開閉矢印)
                        val isOpen = openGroups[group.title] == true || nq.isNotEmpty()
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp)
                                .clickable { openGroups[group.title] = !(openGroups[group.title] ?: false) }
                                .testTag("faqGroup_${group.title}"),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(group.title, color = colors.sub, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            Text(if (isOpen) "▴" else "▾", color = colors.sub, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (openGroups[group.title] == true || nq.isNotEmpty()) {
                        items(visibleItems) { faqItem ->
                            val key = group.title + "|" + faqItem.q
                            val open = openItems[key] ?: false
                            // index.html:190-196 .faq details/summary(枠線ボックス・"Q"プレフィックス)
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    .clickable { openItems[key] = !open }
                                    .background(colors.bg, RoundedCornerShape(14.dp))
                                    .border(1.5.dp, colors.line, RoundedCornerShape(14.dp))
                                    .padding(13.dp)
                                    .testTag("faqItem_$key"),
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("Q", color = colors.pink, fontWeight = FontWeight.Black, modifier = Modifier.padding(end = 8.dp))
                                    Text(faqItem.q, color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                                    Text(if (open) "▴" else "▾", color = colors.sub)
                                }
                                if (open) {
                                    Text(faqItem.a, color = colors.sub, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp, start = 18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
