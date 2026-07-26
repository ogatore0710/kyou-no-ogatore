package jp.ogatore.kyouno

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md
// §「やること」2「共通コンポーネント化」): index.html .card/.btn/.btn-primary/.btn-ghostの1:1移植。
// アプリ全体がこのカード型ボックス+ボタンの積み重ねで構成される(タスク文どおり最優先で直す箇所)。

// フォント適用漏れ・キャラ/タイプ画像の欠落修正タスク(TASK-C2-2026-07-26-visual-parity-fonts-characters.md)
// §2 キャラクター画像: assets/chara*.pngをdrawable-nodpiへ同梱済みの前提で、複数画面(相談室・
// オンボ・ホーム等)から共通で使えるオガトレくん画像コンポーネント。resNameは拡張子なしのdrawable名
// (例: "chara_hitokoto")。
@Composable
fun KyonoCharaImage(resName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val resId = remember(resName) { context.resources.getIdentifier(resName, "drawable", context.packageName) }
    if (resId != 0) {
        Image(painter = painterResource(id = resId), contentDescription = null, modifier = modifier)
    }
}

// index.html:95 .card{background:var(--card);border-radius:var(--radius);padding:20px;margin-bottom:16px}
// 内部はColumn(縦積み)。中身が複数要素のとき単純にBoxへ渡すと重なって描画されてしまうため注意
// (実機検証で発見・修正: KyonoCard内の複数Text/Buttonが同一座標に重なって表示されるバグがあった)。
@Composable
fun KyonoCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalKyonoColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.card, KyonoCardShape)
            .padding(20.dp),
        content = content,
    )
}

// index.html:107-110,115-118 .grad-warm/.grad-mint/.grad-pink/.grad-softの1:1移植。診断結果・
// ホームの一部カードなど「白一色ではない」目立たせカードに使う斜めグラデーション背景。
enum class KyonoGradient { Warm, Mint, Pink, Soft }

@Composable
fun KyonoGradientCard(gradient: KyonoGradient, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val dark = LocalKyonoColors.current.bg.let { it == KyonoDarkColors.bg }
    val (from, to) = when (gradient) {
        KyonoGradient.Warm -> if (dark) Color(0xFF37301C) to Color(0xFF33232B) else Color(0xFFFFF3C4) to Color(0xFFFFEDF3)
        KyonoGradient.Mint -> if (dark) Color(0xFF22403B) to Color(0xFF33301C) else Color(0xFFE7F8F1) to Color(0xFFFFF9DC)
        KyonoGradient.Pink -> if (dark) Color(0xFF33232B) to Color(0xFF33301C) else Color(0xFFFFEDF3) to Color(0xFFFFF9DC)
        KyonoGradient.Soft -> if (dark) Color(0xFF2C2822) to Color(0xFF33232B) else Color(0xFFFFFDF5) to Color(0xFFFFEDF3)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(from, to)), KyonoCardShape)
            .padding(20.dp),
        content = content,
    )
}

// index.html:99-102 .btn/.btn-primary(黄色背景+太字20px+下方向の立体シャドウ)の1:1移植。
// box-shadow:0 4px 0 #E8BE1E(ぼかし無しのオフセット矩形)をCompose上でBox二重描画により再現。
// :active時はtranslateY(3px)+shadow 1pxに縮む(押した感触)ため、pressed状態をMutableInteractionSource経由で検知する。
@Composable
fun KyonoPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = LocalKyonoColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shadowOffset = if (pressed) 1.dp else 4.dp
    val faceOffset = if (pressed) 3.dp else 0.dp
    val alpha = if (enabled) 1f else 0.5f
    Box(modifier = modifier.fillMaxWidth()) {
        // シャドウ層(下地。面層と同じテキスト・paddingを透明色で重ねて高さを一致させる)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = shadowOffset)
                .background(colors.btnPrimaryShadow.copy(alpha = alpha), KyonoButtonShape)
                .padding(16.dp, 18.dp),
            contentAlignment = Alignment.Center,
        ) { Text(text, color = Color.Transparent, fontSize = 20.sp, fontWeight = FontWeight.Black) }
        // 面(前景)層
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = faceOffset)
                .background(colors.yellow.copy(alpha = alpha), KyonoButtonShape)
                .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick)
                .padding(16.dp, 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, color = colors.ink, fontSize = 20.sp, fontWeight = FontWeight.Black, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

// index.html:103 .btn-ghost{background:var(--teal-soft);color:var(--tealink);font-size:15px}
@Composable
fun KyonoGhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalKyonoColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.tealSoft, KyonoButtonShape)
            .clickable(onClick = onClick)
            .padding(16.dp, 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = colors.tealInk, fontSize = 15.sp, fontWeight = FontWeight.Black)
    }
}

// index.html:104 .btn-line{background:none;border:2px solid #E0D5BE;color:var(--sub2);font-weight:800;font-size:15px}
@Composable
fun KyonoLineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalKyonoColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent, KyonoButtonShape)
            .clickable(onClick = onClick)
            .padding(16.dp, 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = colors.sub2, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    }
}

// index.html:372-376 .seg/.seg button/.seg button.on(セグメントコントロール)の1:1移植。
// 例: 設定画面の「画面のみため」「もじの大きさ」トグル。
@Composable
fun <T> KyonoSegmentedControl(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalKyonoColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.line, RoundedCornerShape(16.dp))
            .padding(4.dp),
    ) {
        options.forEach { (value, label) ->
            val on = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (on) colors.card else Color.Transparent, RoundedCornerShape(12.dp))
                    .clickable { onSelect(value) }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (on) colors.ink else colors.sub, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
