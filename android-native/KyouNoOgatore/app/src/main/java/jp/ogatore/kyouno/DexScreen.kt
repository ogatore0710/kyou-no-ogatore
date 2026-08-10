package jp.ogatore.kyouno

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
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
//
// ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
// Phase 3: index.html:225-241 .dex-box/.dex-sec/.dex-seccount/.dex-thumb/.dex-name/.dex-hintの1:1移植。
@Composable
fun DexScreen(store: RecordStore, onBack: () -> Unit) {
    // GO-G6(5視点ワンループ): システム「もどる」を拾い、既存の「◀ もどる」ボタンと同じonBackへ。
    BackHandler(onBack = onBack)
    val themeSetting = store.get("theme", "light")
    KyonoTheme(themeSetting, bigText = store.get("bigtext", true)) {
        val colors = LocalKyonoColors.current
        val streak = remember { RecordLogic.loadStreak(store) }
        val existing = remember { store.get("rotAssign", emptyMap<String, Int>()) }
        val rot = remember { CardLottery.ensureRotAssign(streak.dates, streak.total, existing) }
        LaunchedEffect(Unit) { if (existing.isEmpty() && rot.isNotEmpty()) store.set("rotAssign", rot) }
        val status = remember { DexLogic.getDexStatus(streak.dates, streak.total, rot) }
        val all = status.toku + status.season + status.rare + status.normal
        val gotCount = all.count { it.got }

        LazyColumn(Modifier.fillMaxSize().background(colors.bg).padding(16.dp).testTag("dexBody")) {
            item {
                KyonoLineButton("◀ もどる", onBack, Modifier.testTag("dexBackBtn"))
                Spacer(Modifier.height(12.dp))
                KyonoSectionHeader(KyonoIcon.DexBook, "図鑑", fill = colors.tealSoft)
                Spacer(Modifier.height(4.dp))
                Text("${gotCount}/${all.size}個 あつめました", color = colors.sub, fontSize = 13.sp, modifier = Modifier.testTag("dexSummary"))
                Spacer(Modifier.height(4.dp))
            }
            item { DexSection("記念日カード", status.toku) }
            item { DexSection("季節のカード", status.season) }
            item { DexSection("レアカード", status.rare) }
            item { DexSection("ノーマルカード", status.normal) }
            // GO-G15(5視点ワンループ): 記録系画面に保存先の事実だけを目立たない位置に一言添える。
            // 数字・達成率は書かない(デザイン原則どおり)。
            item {
                Spacer(Modifier.height(12.dp))
                Text(
                    "この記録はこの端末に保存されるよ",
                    color = colors.sub, fontSize = 12.sp,
                    modifier = Modifier.testTag("deviceStorageNote"),
                )
            }
        }
    }
}

@Composable
private fun DexSection(title: String, items: List<DexItem>) {
    val colors = LocalKyonoColors.current
    val got = items.count { it.got }
    KyonoCard(Modifier.padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            // index.html:233 .dex-seccount(bg丸ピル)
            Box(Modifier.background(colors.bg, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 2.dp)) {
                Text("$got/${items.size}", color = colors.sub, fontSize = kyonoFloorSp(12f), fontWeight = FontWeight.Bold, modifier = Modifier.testTag("dexSecCount_$title"))
            }
        }
        Spacer(Modifier.height(8.dp))
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
    val colors = LocalKyonoColors.current
    val context = LocalContext.current
    Column(modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // index.html:236 .dex-thumb(bg/border/radius12)の1:1移植。
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f)
                .background(colors.bg, RoundedCornerShape(12.dp))
                .border(1.5.dp, colors.borderStrong, RoundedCornerShape(12.dp))
                .testTag("dexThumb_${item.tier}_${item.name}"),
            contentAlignment = Alignment.Center,
        ) {
            if (item.tier == "normal") {
                val nc = CardDataLoader.shared.NORMAL_CARDS.find { n -> n.name == item.name }
                if (item.got && nc != null) {
                    Box(Modifier.fillMaxSize(0.34f).background(Color(android.graphics.Color.parseColor(nc.main)), RoundedCornerShape(50)))
                } else {
                    Text("？", color = colors.sub, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
            } else if (item.key != null) {
                val resId = remember(item.key) { context.resources.getIdentifier(item.key, "drawable", context.packageName) }
                if (resId != 0) {
                    // build41実機FB②(2026-08-10本人・実バグ): 未解放の暗化が黒スクリム55%固定の
                    // ため、ダーク背景(0x0F0E0C)では背景と同化して見えなかった。ダーク時は
                    // Modulate(絵柄を減光)へ切り替える。ライトは従来どおり。iOS DexView.swiftと同趣旨。
                    // tint値0x94はalan5差し戻し(2026-08-10)を受けた実測較正値: 理論同値の0x80では
                    // iOS(colorMultiply(white:0.5))より実描画が暗く出たため(色空間/合成順の差とみられる)、
                    // エミュレータ実描画のタイル内明度をiOSシミュレータ実測(平均32.5/明比率24.0%)に
                    // 合わせ込んだ(0x94で31.2/22.4%)。変える時は実測とセットで。
                    val dark = colors.bg == KyonoDarkColors.bg
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = item.name,
                        colorFilter = if (item.got) null
                        else if (dark) ColorFilter.tint(Color(0xFF949494), androidx.compose.ui.graphics.BlendMode.Modulate)
                        else ColorFilter.tint(Color.Black.copy(alpha = 0.55f), androidx.compose.ui.graphics.BlendMode.SrcAtop),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        // index.html:241 .dex-name{font-size:11px;line-height:1.3}の1:1移植。
        // UI/UXパリティ監査2巡目A1(2026-07-29): カスタムフォントの行送り超過補正
        // (KyonoTightLineTextStyle、前回G2は検索チップのみに適用)をここにも展開する。
        // kyonoFloorSp()はbigtext時に実際のフォントサイズが変わりうるため、lineHeightは
        // その解決後の値に対する比率(1.3倍)で計算し、常にCSSと同じ比率を保つ。
        val nameFontSize = kyonoFloorSp(12f)
        Text(
            if (item.got) item.name else "？？？",
            color = colors.ink,
            fontSize = nameFontSize,
            lineHeight = (nameFontSize.value * 1.3f).sp,
            style = kyonoTightLineTextStyle(),
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        val sub = if (item.got) item.flavor else item.hint
        if (sub.isNotEmpty()) {
            // index.html:242 .dex-hint{font-size:10px;line-height:1.35}の1:1移植。
            val hintFontSize = kyonoFloorSp(12f)
            Text(
                sub, fontSize = hintFontSize, lineHeight = (hintFontSize.value * 1.35f).sp,
                style = kyonoTightLineTextStyle(), color = colors.sub, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
