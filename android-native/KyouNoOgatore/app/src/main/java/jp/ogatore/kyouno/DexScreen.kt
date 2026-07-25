package jp.ogatore.kyouno

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.ogatore.kyouno.card.CardDataLoader
import jp.ogatore.kyouno.card.CardLottery
import jp.ogatore.kyouno.card.DexItem
import jp.ogatore.kyouno.card.DexLogic
import jp.ogatore.kyouno.record.RecordLogic
import jp.ogatore.kyouno.record.RecordStore

// ネイティブ移植 Step 7a(マスタープラン§6 Step 7a・図鑑UI): index.html renderDex()の1:1移植。
// ロック/アンロック判定はDexLogic.getDexStatus(Step4のCardLottery呼び出しのみ)を呼ぶだけで、
// このファイルは4段(記念日/季節/レア/ノーマル)のグリッド表示だけを持つ。
//
// LazyVerticalGridをverticalScroll/LazyColumn内に入れると無限高さ制約でクラッシュするため
// (マスタープラン§1-4禁じ手。カレンダーと同じ制約)、各セクションはColumn+Row(4列固定)で組み、
// 画面全体のスクロールだけLazyColumnに持たせる(セクション自体はLazyでない)。
//
// カード画像(assets/cards/*.webp)はAndroid res/drawable-nodpiへそのまま同梱(ファイル名=カードkey)。
// ロック中はCSSアルファマスクのシルエット効果の代わりに、同じ画像を暗くティントして表示する簡略版。
@Composable
fun DexScreen(store: RecordStore, onBack: () -> Unit) {
    val streak = remember { RecordLogic.loadStreak(store) }
    val existing = remember { store.get("rotAssign", emptyMap<String, Int>()) }
    val rot = remember { CardLottery.ensureRotAssign(streak.dates, streak.total, existing) }
    LaunchedEffect(Unit) { if (existing.isEmpty() && rot.isNotEmpty()) store.set("rotAssign", rot) }
    val status = remember { DexLogic.getDexStatus(streak.dates, streak.total, rot) }
    val all = status.toku + status.season + status.rare + status.normal
    val gotCount = all.count { it.got }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp).testTag("dexBody")) {
        item {
            Button(onClick = onBack, modifier = Modifier.testTag("dexBackBtn")) { Text("◀ もどる") }
            Spacer(Modifier.height(8.dp))
            Text("図鑑", style = MaterialTheme.typography.headlineSmall)
            Text("${gotCount}/${all.size}個 あつめました", modifier = Modifier.testTag("dexSummary"))
        }
        item { DexSection("記念日カード", status.toku) }
        item { DexSection("季節のカード", status.season) }
        item { DexSection("レアカード", status.rare) }
        item { DexSection("ノーマルカード", status.normal) }
    }
}

@Composable
private fun DexSection(title: String, items: List<DexItem>) {
    val got = items.count { it.got }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("$got/${items.size}", modifier = Modifier.testTag("dexSecCount_$title"))
        }
        Spacer(Modifier.height(4.dp))
        val cols = 4
        items.chunked(cols).forEach { rowItems ->
            Row(Modifier.fillMaxWidth()) {
                for (it in rowItems) {
                    DexCell(it, Modifier.weight(1f))
                }
                repeat(cols - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DexCell(item: DexItem, modifier: Modifier) {
    val context = LocalContext.current
    Column(modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFFEDEAE2), RoundedCornerShape(10.dp)).testTag("dexThumb_${item.tier}_${item.name}"),
            contentAlignment = Alignment.Center,
        ) {
            if (item.tier == "normal") {
                val nc = CardDataLoader.shared.NORMAL_CARDS.find { n -> n.name == item.name }
                if (item.got && nc != null) {
                    Box(Modifier.fillMaxSize(0.5f).background(Color(android.graphics.Color.parseColor(nc.main)), RoundedCornerShape(50)))
                } else {
                    Text("？", style = MaterialTheme.typography.titleLarge)
                }
            } else if (item.key != null) {
                val resId = remember(item.key) { context.resources.getIdentifier(item.key, "drawable", context.packageName) }
                if (resId != 0) {
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = item.name,
                        colorFilter = if (item.got) null else ColorFilter.tint(Color.Black.copy(alpha = 0.55f), androidx.compose.ui.graphics.BlendMode.SrcAtop),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        Text(
            if (item.got) item.name else "？？？",
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        val sub = if (item.got) item.flavor else item.hint
        if (sub.isNotEmpty()) {
            Text(sub, fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}
