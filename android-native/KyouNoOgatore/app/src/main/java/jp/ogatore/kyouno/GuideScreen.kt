package jp.ogatore.kyouno

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import jp.ogatore.kyouno.safety.SafetyGate

// ネイティブ移植 Step 7b(マスタープラン§6 Step 7b): 使い方タブ=よくあるしつもんUI(index.html
// filterFaq()/toggleFaqGroup()の1:1移植)。検索の正規化はWeb版と同じくSafetyGate.norm(Step2で
// 移植済み・安全判定4関数の1つ)を再利用する——判定ロジックではなく正規化ユーティリティとしての
// 再利用であり、UI層に判定を書いていないことに変わりはない(マスタープラン§3-2の隔離対象=
// crisisHit/redFlagHit/redFlagKindの3関数であり、normはテキスト正規化のみで判定を含まない)。
// A2HS関連項目はGuideData.ktでhidden=trueとしてデータには残しつつ、この画面では表示しない
// (§2-2「非表示化して移植（削除しない）」)。
@Composable
fun GuideScreen(onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val openGroups = remember { mutableStateMapOf<String, Boolean>().apply { put(FAQ_GROUPS[0].title, true) } }
    val openItems = remember { mutableStateMapOf<String, Boolean>() }

    val nq = remember(query) { SafetyGate.norm(query) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onBack, modifier = Modifier.testTag("guideBackBtn")) { Text("◀ もどる") }
        Spacer(Modifier.height(8.dp))
        Text("よくあるしつもん", style = MaterialTheme.typography.headlineSmall)
        Text("しつもんをタップすると こたえがひらきます", style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().testTag("faqSearch"),
            placeholder = { Text("🔍 キーワードでさがす（例: 記録 / 機種変更 / 痛い）") },
        )
        Spacer(Modifier.height(8.dp))

        LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("faqList")) {
            for (group in FAQ_GROUPS) {
                val visibleItems = group.items.filter { item ->
                    if (item.hidden) return@filter false
                    if (nq.isEmpty()) return@filter true
                    SafetyGate.norm(item.q).contains(nq) || SafetyGate.norm(item.a).contains(nq)
                }
                if (visibleItems.isEmpty()) continue
                item {
                    Text(
                        group.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            .clickable { openGroups[group.title] = !(openGroups[group.title] ?: false) }
                            .testTag("faqGroup_${group.title}"),
                    )
                }
                if (openGroups[group.title] == true || nq.isNotEmpty()) {
                    items(visibleItems) { faqItem ->
                        val key = group.title + "|" + faqItem.q
                        val open = openItems[key] ?: false
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                .clickable { openItems[key] = !open }
                                .background(Color(0xFFF3F1EC), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                                .testTag("faqItem_$key"),
                        ) {
                            Text(faqItem.q, style = MaterialTheme.typography.bodyMedium)
                            if (open) {
                                Text(faqItem.a, modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
