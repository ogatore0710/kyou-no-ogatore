package jp.ogatore.kyouno

import androidx.compose.foundation.Image
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import jp.ogatore.kyouno.card.BragCardRenderer
import jp.ogatore.kyouno.card.CardDataLoader
import jp.ogatore.kyouno.card.CardLottery
import jp.ogatore.kyouno.card.ResolvedTheme
import jp.ogatore.kyouno.catalog.CatalogLoader
import jp.ogatore.kyouno.catalog.CatalogVideo
import jp.ogatore.kyouno.record.RecordLogic
import jp.ogatore.kyouno.record.RecordStore
import java.time.Instant

// ネイティブ移植 Step 7b(マスタープラン§6 Step 7b・§2-1「index.html drawBragCard」行): じまんカード
// 作成UI(index.html openBrag()〜makeBragCard()の1:1移植)。日数入力(1〜9999の整数のみ・自由入力
// なし)+動画検索(Step7aのsearchCatalogを再利用)+作成ボタン。判定/描画ロジックはBragCardRenderer
// (Step7b新設・CardRendererと同じ舞台演出を再利用)を呼ぶだけ。
@Composable
fun BragScreen(store: RecordStore, onBack: () -> Unit) {
    val context = LocalContext.current
    val catalog = remember { CatalogLoader.shared }
    val streak = remember { RecordLogic.loadStreak(store) }
    var daysText by remember { mutableStateOf((if (streak.total > 0) streak.total else 1).toString()) }
    var query by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf<CatalogVideo?>(null) }
    var cardBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    val hits = remember(query) { if (query.isBlank()) emptyList() else searchCatalog(catalog, query, null, null).take(20) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onBack, modifier = Modifier.testTag("bragBackBtn")) { Text("◀ もどる") }
        Spacer(Modifier.height(8.dp))
        Text("じまんカードをつくる", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(12.dp))
        Text("つづけてる日数")
        OutlinedTextField(
            value = daysText,
            onValueChange = { s -> if (s.all { it.isDigit() } && s.length <= 4) daysText = s },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.testTag("bragDaysInput"),
        )

        Spacer(Modifier.height(12.dp))
        Text("すきな1本をさがす")
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().testTag("bragSearchInput"),
            placeholder = { Text("動画のタイトルやタグで検索") },
        )
        picked?.let { Text("選択中: ${it.t}", modifier = Modifier.padding(top = 4.dp).testTag("bragPickedText")) }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(top = 4.dp).testTag("bragSearchResults")) {
            items(hits) { v ->
                Text(
                    v.t,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        .clickableTestTag("bragHit_${v.id}") { picked = v },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                val days = BragCardRenderer.clampDays(daysText.toIntOrNull() ?: 1)
                val ds = RecordLogic.todayStr(Instant.now())
                val dateIdx = CardLottery.dateIdx(ds)
                val data = CardDataLoader.shared
                val theme = data.CARD_THEMES[dateIdx % data.CARD_THEMES.size]
                val resolved = ResolvedTheme(theme.name, theme.bg, theme.main, theme.deco)
                cardBitmap = BragCardRenderer.render(ds, days, resolved, picked?.t)
            },
            modifier = Modifier.fillMaxWidth().testTag("bragMakeBtn"),
        ) { Text("カードをつくる✨") }
    }

    cardBitmap?.let { bmp ->
        AlertDialog(
            onDismissRequest = { cardBitmap = null },
            confirmButton = { Button(onClick = { cardBitmap = null }, modifier = Modifier.testTag("bragCardCloseBtn")) { Text("とじる") } },
            dismissButton = {
                Button(
                    onClick = {
                        val days = BragCardRenderer.clampDays(daysText.toIntOrNull() ?: 1)
                        ShareImage.shareBitmap(context, bmp, "kyono-ogatore-brag-${RecordLogic.todayStr(Instant.now())}.png", "#きょうのオガトレ ${days}日つづいてる！")
                    },
                    modifier = Modifier.testTag("bragCardShareBtn"),
                ) { Text("保存・シェアする") }
            },
            text = {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "じまんカード",
                    modifier = Modifier.fillMaxWidth().testTag("bragCardImage"),
                )
            },
        )
    }
}

// LazyColumn item内でモディファイアのclickable+testTagを短く書くための小ヘルパー。
private fun Modifier.clickableTestTag(tag: String, onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick).testTag(tag)
