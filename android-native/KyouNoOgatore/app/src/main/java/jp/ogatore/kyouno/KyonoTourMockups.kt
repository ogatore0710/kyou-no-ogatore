package jp.ogatore.kyouno

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
// なっていた(iOS版・alan5が実描画で確認・報告)。T-2で3枚+締めへ再構成したのに合わせ、caseを
// ゼロから書き直す(以後、スライド文言の変更時は必ずこのwhenも同時に見直すこと・検収基準
// 「見出し⇔絵の一致」を新設)。
@Composable
fun KyonoTourMockup(slideIndex: Int) {
    val colors = LocalKyonoColors.current
    when (slideIndex) {
        // 1) 悩みは相談室で質問: 実際のチャット吹き出し2つ(ユーザー発言→オガトレくんの返答、アバター付き)
        0 -> Column {
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
                        .border(1.5.dp, colors.line, RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
        // 2) オガトレ通信をのぞく: 丸い写真アイコン+説明
        1 -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            KyonoTourDrawable("obu_fab_photo", Modifier.size(56.dp).border(3.dp, colors.yellow, CircleShape), CircleShape)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("右下のこの写真ボタン", color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text("ひとこと・写真・ラジオ", color = colors.sub, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        // 3) マイ記録でふりかえる: カレンダーのミニチュア(5個の丸、3個が塗りつぶし=やった日)
        2 -> KyonoCard {
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
