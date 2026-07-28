package jp.ogatore.kyouno

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.launch
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
    // GO-G6(5視点ワンループ): システム「もどる」を拾い、既存の「◀ もどる」ボタンと同じonBackへ。
    BackHandler(onBack = onBack)
    val themeSetting = store.get("theme", "auto")
    KyonoTheme(themeSetting, bigText = store.get("bigtext", true)) {
        val colors = LocalKyonoColors.current
        val context = LocalContext.current
        // GO-G7(5視点ワンループ): カード獲得(生成完了)に「きょうやった！」と同じ軽いハプティクスを追加。
        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
        val catalog = remember { CatalogLoader.shared }
        val streak = remember { RecordLogic.loadStreak(store) }
        // index.html:2724-2730 openBrag()の1:1移植: 「つづいている日数」はcount(いま連続)から
        // プリフィル(total=通算ではない)。全画面完全性監査タスク #brag で、Web版と異なりtotalから
        // プリフィルしていた既存の不一致を修正。
        var daysText by remember { mutableStateOf((if (streak.count > 0) streak.count else 1).toString()) }
        var query by remember { mutableStateOf("") }
        var picked by remember { mutableStateOf<CatalogVideo?>(null) }
        var cardBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
        var makingCard by remember { mutableStateOf(false) }
        val scope = androidx.compose.runtime.rememberCoroutineScope()

        val hits = remember(query) { if (query.isBlank()) emptyList() else searchCatalog(catalog, query, null, null).take(20) }

        // TASK-C2-2026-07-27-brag-card-thumbnail.md検証時に発覚した既存バグの修正: 検索結果
        // LazyColumn(旧Modifier.weight(1f))が、非スクロールの外側Columnの中では固定要素群との
        // 高さ配分の関係で実質0pxになり、検索結果自体は正しく計算されている(hits.size>0)のに
        // 一切見えなくなっていた(実機でクエリ「10」=379件ヒットするはずが表示0件になる不具合を
        // デバッグ表示で確認して特定)。iOS版(.frame(maxHeight:240))と同じ「固定高さ+外側は
        // スクロール」の形に直す。
        Column(
            Modifier.fillMaxSize().background(colors.bg).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            KyonoLineButton("◀ もどる", onBack, Modifier.testTag("bragBackBtn"))
            Spacer(Modifier.height(12.dp))
            KyonoCard {
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
                // 全画面完全性監査タスク(TASK-C2-2026-07-26-full-completeness-audit.md #brag):
                // index.html:857 #bragDaysNoteの1:1移植。
                Spacer(Modifier.height(4.dp))
                Text(
                    if (streak.count > 0) "いまの記録から入れておきました（数字はすきにかえてOK）" else "まだ記録がなくてもだいじょうぶ すきな数字でためせます",
                    color = colors.sub, fontSize = 12.sp, modifier = Modifier.testTag("bragDaysNote"),
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
                LazyColumn(Modifier.heightIn(max = 240.dp).fillMaxWidth().testTag("bragSearchResults")) {
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
                        // TASK-C2-2026-07-27-brag-card-thumbnail.md: index.html:2765-2774
                        // loadBragThumb()の1:1移植。サムネイル取得はネットワークI/Oのためsuspend化し、
                        // 取得中もUIをブロックしない(3秒タイムアウトで先へ進むのはfetchThumbnail側)。
                        makingCard = true
                        scope.launch {
                            val videoId = picked?.id
                            val thumbnail = videoId?.let { BragCardRenderer.fetchThumbnail(it) }
                            val days = BragCardRenderer.clampDays(daysText.toIntOrNull() ?: 1)
                            val ds = RecordLogic.todayStr(Instant.now())
                            val dateIdx = CardLottery.dateIdx(ds)
                            val data = CardDataLoader.shared
                            val theme = data.CARD_THEMES[dateIdx % data.CARD_THEMES.size]
                            val resolved = ResolvedTheme(theme.name, theme.bg, theme.main, theme.deco)
                            cardBitmap = BragCardRenderer.render(ds, days, resolved, picked?.t, context, thumbnail)
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            makingCard = false
                        }
                    },
                    modifier = Modifier.testTag("bragMakeBtn"),
                    enabled = !makingCard,
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
