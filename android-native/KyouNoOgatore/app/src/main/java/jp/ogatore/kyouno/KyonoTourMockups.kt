package jp.ogatore.kyouno

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 見た目パリティ第2弾(TASK-C2-2026-07-26-visual-parity-round2.md §1・最優先): 使い方ツアーの
// 各スライドが「実際のアプリ画面のミニチュア再現」であるWeb版(index.html:4117-4143 OB_TOUR_SLIDES
// のv フィールド)を1:1移植する。タップ不可の静止モックアップ(Web版の「gmock」)であり、
// ここで描く内容はロジック・状態には一切影響しない(見た目のみ)。
//
// TASK-C2-2026-08-04-build19-tour-redesign.md T-1(実バグ修正): B-10でOB_TOUR_SLIDESを8→7枚に
// 詰めた際、このwhenのcase番号(8枚時代のcase 0〜7)を詰め忘れ、3枚目以降が1つ前の話題の絵に
// なっていた(iOS版・alan5が実描画で確認・報告)。以後、位置(index)ではなくTourMockKind
// (意味のある固定キー)でswitchする設計に変更済み(検収基準「見出し⇔絵の一致」を新設)。
//
// TASK-C2-2026-08-04-build20-home-cards-and-tour-tiers.md T-A/T-B: スライド配列が「初回4枚/
// 再生7枚」の2構成に分かれたのに合わせ、地図(MAP)・復活3枚(VIDEO_DAILY/TODAY_DONE/CARD_DEX)の
// モックを追加。
@Composable
fun KyonoTourMockup(kind: TourMockKind) {
    val colors = LocalKyonoColors.current
    when (kind) {
        // T-A: まいにちやることは1つだけ。動画カード→黄色「きょうやった！」ボタン→記録カードの
        // 3コマ縦並び簡略図(alan5指定)。
        // TASK-C2-2026-08-04-build21-addendum.md Y-3: 本人がGPT生成イラストを用意し次第、drawable
        // 追加だけで差し替わるフォールバック構造(KyonoTourDrawableと同じgetIdentifier方式)。
        // Androidのdrawable名はハイフン不可のためtour_map_illust(iOS側のtour-map-illust.pngに相当)。
        // バンドルに無い間(今回のビルド時点)は現行モックのまま。
        TourMockKind.MAP -> {
            val context = LocalContext.current
            val mapIllustResId = remember { context.resources.getIdentifier("tour_map_illust", "drawable", context.packageName) }
            if (mapIllustResId != 0) {
                Image(
                    painter = painterResource(id = mapIllustResId),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth().background(colors.card, RoundedCornerShape(12.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        // TASK-C2-2026-08-05-build23-bg-tuning-and-tour-tap.md W-1(IMG_8780): グレー
                        // 空箱のままだと未完成に見えるため、朝専用の看板動画(asa10=2EfFlQev4rg)の
                        // サムネを仮アセット同梱して表示(GPT生成絵が届くまでの暫定)。
                        Box(Modifier.width(46.dp).aspectRatio(16f / 9f).background(colors.line, RoundedCornerShape(8.dp))) {
                            KyonoTourDrawable("tour_map_thumb", Modifier.fillMaxSize(), RoundedCornerShape(8.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("きょうの1本", color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("↓", color = colors.sub, fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "きょうやった！", color = KyonoBtnPrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Black,
                        modifier = Modifier.fillMaxWidth().background(colors.yellow, RoundedCornerShape(12.dp)).padding(vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Text("↓", color = colors.sub, fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
                    KyonoTourDrawable("card_sample", Modifier.size(60.dp), RoundedCornerShape(12.dp))
                }
            }
        }
        // 復活枚1) まいにち1本、動画をやる: 「きょうの1本」カードのミニチュア(動画サムネイル+タイトル+案内文)
        TourMockKind.VIDEO_DAILY -> KyonoCard {
            Text("きょうの1本", color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(120.dp).aspectRatio(16f / 9f).background(colors.line, RoundedCornerShape(8.dp))) {
                    KyonoAsyncImage(
                        youtubeThumbUrl("Re5FPU5_37g"),
                        Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("開脚できるようになる2週間ストレッチ", color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("▶ タップでYouTubeがひらきます", color = colors.sub, fontSize = 12.sp)
                }
            }
        }
        // 復活枚2) おわったら「きょうやった！」: 「続けた日数」カードのミニチュア(大きい数字「8日目」+done-btn)
        TourMockKind.TODAY_DONE -> KyonoCard {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("続けた日数（通算）", color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("8", color = colors.pink, fontSize = 38.sp, fontWeight = FontWeight.Black)
                    Text("日目", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 6.dp))
                }
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().background(colors.tealStrong, RoundedCornerShape(18.dp)).padding(vertical = 14.dp)) {
                    Text("きょうやった！", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
        // 復活枚3) ためると図鑑がうまる: card_sample.pngの隣に「？」の点線枠3つ
        TourMockKind.CARD_DEX -> KyonoCard {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("カード図鑑", color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KyonoTourDrawable("card_sample", Modifier.size(52.dp), RoundedCornerShape(10.dp))
                    repeat(3) {
                        Box(
                            Modifier.size(52.dp).border(1.5.dp, colors.borderStrong, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) { Text("？", color = colors.sub, fontWeight = FontWeight.Black) }
                    }
                }
            }
        }
        // 予告1) 悩みは相談室で質問: 実際のチャット吹き出し2つ(ユーザー発言→オガトレくんの返答、アバター付き)
        TourMockKind.SOUDAN -> Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    "肩こりがつらい", color = colors.ink,
                    modifier = Modifier.background(colors.yellowSoft, RoundedCornerShape(16.dp, 16.dp, 6.dp, 16.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                KyonoCharaImage("chara_hitokoto", Modifier.size(34.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "それはつらいね…！まずはこの1本からやってみよう", color = colors.ink,
                    modifier = Modifier
                        .background(colors.card, RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp))
                        .border(1.5.dp, colors.borderStrong, RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
        // 予告2) オガトレ通信をのぞく: 丸い写真アイコン+説明
        TourMockKind.OBU -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            KyonoTourDrawable("obu_fab_photo", Modifier.size(56.dp).border(3.dp, colors.yellow, CircleShape), CircleShape)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("右下のこの写真ボタン", color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text("ひとこと・写真・ラジオ", color = colors.sub, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        // 予告3) マイ記録でふりかえる: カレンダーのミニチュア(5個の丸、3個が塗りつぶし=やった日)
        TourMockKind.MY_RECORD -> KyonoCard {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("カレンダー", color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    for (n in 1..5) {
                        val done = n <= 3
                        Box(
                            Modifier.size(34.dp).background(if (done) colors.tealStrong else Color.Transparent, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("$n", color = if (done) Color.White else colors.sub, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KyonoTourDrawable(resName: String, modifier: Modifier, shape: Shape) {
    val context = LocalContext.current
    val resId = remember(resName) { context.resources.getIdentifier(resName, "drawable", context.packageName) }
    if (resId != 0) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(shape),
        )
    }
}
