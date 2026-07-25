package jp.ogatore.kyouno

import androidx.compose.foundation.Image
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
//
// ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
// Phase 3: index.html:850-866,340 #brag .card/.lbl/.hint/sec-head(Heartアイコン)の1:1移植。
@Composable
fun BragScreen(store: RecordStore, onBack: () -> Unit) {
    val themeSetting = store.get("theme", "auto")
    KyonoTheme(themeSetting) {
        val colors = LocalKyonoColors.current
        val context = LocalContext.current
        val catalog = remember { CatalogLoader.shared }
        val streak = remember { RecordLogic.loadStreak(store) }
        var daysText by remember { mutableStateOf((if (streak.total > 0) streak.total else 1).toString()) }
        var query by remember { mutableStateOf("") }
        var picked by remember { mutableStateOf<CatalogVideo?>(null) }
        var cardBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

        val hits = remember(query) { if (query.isBlank()) emptyList() else searchCatalog(catalog, query, null, null).take(20) }

        Column(Modifier.fillMaxSize().background(colors.bg).padding(16.dp)) {
            KyonoLineButton("◀ もどる", onBack, Modifier.testTag("bragBackBtn"))
            Spacer(Modifier.height(12.dp))
            KyonoCard(Modifier.weight(1f)) {
                KyonoSectionHeader(KyonoIcon.Heart, "じまんカードをつくる", fill = colors.pinkSoft, accent = colors.pink)
                Spacer(Modifier.height(8.dp))
                Text("続けてる日数と すきな1本を 1枚のカードに✨\nできたカードは保存やSNS投稿ができます", color = colors.sub, fontSize = 14.sp, lineHeight = 20.sp)

                // index.html:340 #brag .lbl
                Spacer(Modifier.height(14.dp))
                Text("つづいている日数", color = colors.sub, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(6.dp))
                TextField(
                    value = daysText,
                    onValueChange = { s -> if (s.all { it.isDigit() } && s.length <= 4) daysText = s },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.card, unfocusedContainerColor = colors.card,
                        focusedIndicatorColor = colors.line, unfocusedIndicatorColor = colors.line,
                    ),
                    modifier = Modifier.testTag("bragDaysInput"),
                )

                Spacer(Modifier.height(14.dp))
                Text("すきな1本をさがす🎬", color = colors.sub, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(6.dp))
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("例: 肩甲骨／朝／開脚", color = colors.subFaint) },
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.card, unfocusedContainerColor = colors.card,
                        focusedIndicatorColor = colors.line, unfocusedIndicatorColor = colors.line,
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("bragSearchInput"),
                )
                Spacer(Modifier.height(6.dp))
                LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("bragSearchResults")) {
                    items(hits) { v ->
                        Text(
                            v.t, color = colors.ink, fontSize = 14.sp,
                            modifier = Modifier.fillMaxWidth()
                                .background(colors.bg, RoundedCornerShape(12.dp))
                                .border(1.5.dp, colors.line, RoundedCornerShape(12.dp))
                                .clickable { picked = v }
                                .padding(12.dp)
                                .testTag("bragHit_${v.id}"),
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("えらんだ1本", color = colors.sub, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text(
                    picked?.let { "選択中: ${it.t}" } ?: "まだえらんでいません 上の検索からどうぞ",
                    color = colors.sub, fontSize = 14.sp, modifier = Modifier.testTag("bragPickedText"),
                )

                Spacer(Modifier.height(16.dp))
                KyonoPrimaryButton(
                    "カードをつくる✨",
                    {
                        val days = BragCardRenderer.clampDays(daysText.toIntOrNull() ?: 1)
                        val ds = RecordLogic.todayStr(Instant.now())
                        val dateIdx = CardLottery.dateIdx(ds)
                        val data = CardDataLoader.shared
                        val theme = data.CARD_THEMES[dateIdx % data.CARD_THEMES.size]
                        val resolved = ResolvedTheme(theme.name, theme.bg, theme.main, theme.deco)
                        cardBitmap = BragCardRenderer.render(ds, days, resolved, picked?.t, context)
                    },
                    modifier = Modifier.testTag("bragMakeBtn"),
                )
                Spacer(Modifier.height(8.dp))
                Text("えらんだ1本は カードにサムネイル画像で入ります", color = colors.sub, fontSize = 13.sp)
            }
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
}
