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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 見た目パリティ第2弾(TASK-C2-2026-07-26-visual-parity-round2.md §1・最優先): 使い方ツアーの
// 各スライドが「実際のアプリ画面のミニチュア再現」であるWeb版(index.html:4117-4143 OB_TOUR_SLIDES
// のv フィールド)を1:1移植する。タップ不可の静止モックアップ(Web版の「gmock」)であり、
// ここで描く内容はロジック・状態には一切影響しない(見た目のみ)。
@Composable
fun KyonoTourMockup(slideIndex: Int) {
    val colors = LocalKyonoColors.current
    when (slideIndex) {
        // 1) 📺まいにち1本: 「きょうの1本」カードのミニチュア(動画サムネイル+タイトル+案内文)
        0 -> KyonoCard {
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
        // 2) ✅きょうやった！: 「続けた日数」カードのミニチュア(大きい数字「8日目」+done-btn)
        1 -> KyonoCard {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("続けた日数（通算）", color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("8", color = colors.pink, fontSize = 38.sp, fontWeight = FontWeight.Black)
                    Text("日目", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 6.dp))
                }
                Spacer(Modifier.height(6.dp))
                // index.html:380-381 .done-btn(teal-strong塗り+立体シャドウ。gmockのため押下は無し)
                Box(
                    Modifier.fillMaxWidth()
                        .background(Color(0xFF1E8A7D), RoundedCornerShape(18.dp))
                        .padding(top = 4.dp),
                ) {
                    Box(Modifier.fillMaxWidth().background(colors.tealStrong, RoundedCornerShape(18.dp)).padding(vertical = 14.dp)) {
                        Text("きょうやった！", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }
                }
            }
        }
        // 3) 記録カードをつくる: card-sample.pngを180x180角丸で中央表示
        2 -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            KyonoTourDrawable("card_sample", Modifier.size(180.dp), RoundedCornerShape(20.dp))
        }
        // 4) ためると図鑑がうまる: card-sample.pngの隣に「？」の点線枠3つ
        3 -> KyonoCard {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("カード図鑑", color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KyonoTourDrawable("card_sample", Modifier.size(52.dp), RoundedCornerShape(10.dp))
                    repeat(3) {
                        Box(
                            Modifier.size(52.dp)
                                .border(1.5.dp, colors.line, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) { Text("？", color = colors.sub, fontWeight = FontWeight.Black) }
                    }
                }
            }
        }
        // 5) 悩みは相談室で質問: 実際のチャット吹き出し2つ(ユーザー発言→オガトレくんの返答、アバター付き)
        4 -> Column {
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
                    "それはつらいね…！まずはこの1本からやってみよう😊", color = colors.ink,
                    modifier = Modifier
                        .background(colors.card, RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp))
                        .border(1.5.dp, colors.line, RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
        // 6) オガトレ通信をのぞく: 丸い写真アイコン+説明
        5 -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            KyonoTourDrawable("obu_fab_photo", Modifier.size(56.dp).border(3.dp, colors.yellow, CircleShape), CircleShape)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("右下のこの写真ボタン", color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text("ひとこと・写真・ラジオ📻", color = colors.sub, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        // 7) マイ記録でふりかえる: カレンダーのミニチュア(5個の丸、3個が塗りつぶし=やった日)
        6 -> KyonoCard {
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
        // 8) 忘れてもだいじょうぶ: シンプルな案内カード
        7 -> KyonoCard {
            Text(
                "下の「使い方」タブに\nぜんぶ書いてあります", color = colors.ink, fontSize = 14.sp, lineHeight = 24.sp,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            )
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
