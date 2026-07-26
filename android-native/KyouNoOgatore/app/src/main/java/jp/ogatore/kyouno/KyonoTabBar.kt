package jp.ogatore.kyouno

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md
// §「コンポーネント様式」下部タブバー): index.html:389-397 .tabbar/1158-1164 <nav class="tabbar">の
// 1:1移植。5項目(使い方/マイ記録/ホーム/再生リスト/動画を探す)固定・選択時アイコンが黄色に塗りつぶされる。
// アイコンはMaterial Icons/SF Symbolsへの置き換え不可(タスク文の明示的な指示)なので、index.htmlの
// インラインSVG(d属性)をCompose Path/Canvasで直接再現する(手描き風の意匠を保つ)。
enum class KyonoTab { Guide, MyRecord, Home, Catalog, Search }

@Composable
fun KyonoTabBar(current: KyonoTab, onSelect: (KyonoTab) -> Unit) {
    val colors = LocalKyonoColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.card)
            .padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        TabItem(colors.tabbarIconOff, colors.yellow, colors.ink, colors.sub, current == KyonoTab.Guide, "使い方") { onSelect(KyonoTab.Guide) }
        TabItem(colors.tabbarIconOff, colors.yellow, colors.ink, colors.sub, current == KyonoTab.MyRecord, "マイ記録") { onSelect(KyonoTab.MyRecord) }
        TabItem(colors.tabbarIconOff, colors.yellow, colors.ink, colors.sub, current == KyonoTab.Home, "ホーム") { onSelect(KyonoTab.Home) }
        TabItem(colors.tabbarIconOff, colors.yellow, colors.ink, colors.sub, current == KyonoTab.Catalog, "再生リスト") { onSelect(KyonoTab.Catalog) }
        TabItem(colors.tabbarIconOff, colors.yellow, colors.ink, colors.sub, current == KyonoTab.Search, "動画を探す") { onSelect(KyonoTab.Search) }
    }
}

@Composable
private fun RowScope.TabItem(
    iconOff: Color, iconOn: Color, labelOn: Color, labelOff: Color,
    selected: Boolean, label: String, onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val fill = if (selected) iconOn else iconOff
        when (label) {
            "使い方" -> GuideIcon(fill)
            "マイ記録" -> MyRecordIcon(fill)
            "ホーム" -> HomeIcon(fill)
            "再生リスト" -> CatalogIcon(fill)
            "動画を探す" -> SearchIcon(fill)
        }
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (selected) labelOn else labelOff)
    }
}

private val strokeColor = Color(0xFF3A3A35)

// index.html:1159 使い方(開いた本)
@Composable
private fun GuideIcon(fill: Color) {
    Canvas(Modifier.size(24.dp)) {
        val s = size.width / 24f
        val path = Path().apply {
            moveTo(4 * s, 5.5f * s)
            quadraticBezierTo(8 * s, 3.5f * s, 12 * s, 5.5f * s)
            quadraticBezierTo(16 * s, 3.5f * s, 20 * s, 5.5f * s)
            lineTo(20 * s, 19 * s)
            quadraticBezierTo(16 * s, 17 * s, 12 * s, 19 * s)
            quadraticBezierTo(8 * s, 17 * s, 4 * s, 19 * s)
            close()
        }
        drawPath(path, color = fill, style = androidx.compose.ui.graphics.drawscope.Fill)
        drawPath(path, color = strokeColor, style = Stroke(width = 1.6f * s))
        drawLine(strokeColor, Offset(12 * s, 5.5f * s), Offset(12 * s, 19 * s), strokeWidth = 1.6f * s)
    }
}

// index.html:1160 マイ記録(カレンダー+チェック)
@Composable
private fun MyRecordIcon(fill: Color) {
    Canvas(Modifier.size(24.dp)) {
        val s = size.width / 24f
        drawRoundRect(fill, topLeft = Offset(3 * s, 5 * s), size = androidx.compose.ui.geometry.Size(18 * s, 16 * s), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.5f * s))
        drawRoundRect(strokeColor, topLeft = Offset(3 * s, 5 * s), size = androidx.compose.ui.geometry.Size(18 * s, 16 * s), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.5f * s), style = Stroke(width = 1.6f * s))
        drawLine(strokeColor, Offset(3 * s, 9.5f * s), Offset(21 * s, 9.5f * s), strokeWidth = 1.6f * s)
        drawLine(strokeColor, Offset(8 * s, 3 * s), Offset(8 * s, 7 * s), strokeWidth = 1.6f * s)
        drawLine(strokeColor, Offset(16 * s, 3 * s), Offset(16 * s, 7 * s), strokeWidth = 1.6f * s)
        val check = Path().apply {
            moveTo(8.5f * s, 14.5f * s)
            lineTo(11 * s, 17 * s)
            lineTo(15.5f * s, 12 * s)
        }
        drawPath(check, color = strokeColor, style = Stroke(width = 1.8f * s, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}

// index.html:1161 ホーム(家)
@Composable
private fun HomeIcon(fill: Color) {
    val doorColor = LocalKyonoColors.current.card
    Canvas(Modifier.size(24.dp)) {
        val s = size.width / 24f
        val path = Path().apply {
            moveTo(4 * s, 10.5f * s)
            lineTo(12 * s, 4 * s)
            lineTo(20 * s, 10.5f * s)
            lineTo(20 * s, 20 * s)
            lineTo(4 * s, 20 * s)
            close()
        }
        path.fillType = PathFillType.NonZero
        drawPath(path, color = fill)
        drawPath(path, color = strokeColor, style = Stroke(width = 1.6f * s, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        drawRect(doorColor, topLeft = Offset(9.5f * s, 15 * s), size = androidx.compose.ui.geometry.Size(5 * s, 6 * s))
        drawRect(strokeColor, topLeft = Offset(9.5f * s, 15 * s), size = androidx.compose.ui.geometry.Size(5 * s, 6 * s), style = Stroke(width = 1.4f * s))
    }
}

// index.html:1162 再生リスト
@Composable
private fun CatalogIcon(fill: Color) {
    Canvas(Modifier.size(24.dp)) {
        val s = size.width / 24f
        drawLine(strokeColor, Offset(6.5f * s, 4.5f * s), Offset(17.5f * s, 4.5f * s), strokeWidth = 1.6f * s, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawRoundRect(fill, topLeft = Offset(3 * s, 7 * s), size = androidx.compose.ui.geometry.Size(14 * s, 13 * s), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3 * s))
        drawRoundRect(strokeColor, topLeft = Offset(3 * s, 7 * s), size = androidx.compose.ui.geometry.Size(14 * s, 13 * s), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3 * s), style = Stroke(width = 1.6f * s))
        val play = Path().apply {
            moveTo(8.5f * s, 12 * s)
            lineTo(8.5f * s, 17 * s)
            lineTo(12.5f * s, 14.5f * s)
            close()
        }
        drawPath(play, color = strokeColor)
    }
}

// index.html:1163 動画を探す(虫眼鏡)
@Composable
private fun SearchIcon(fill: Color) {
    Canvas(Modifier.size(24.dp)) {
        val s = size.width / 24f
        drawCircle(fill, radius = 6.5f * s, center = Offset(10.5f * s, 10.5f * s))
        drawCircle(strokeColor, radius = 6.5f * s, center = Offset(10.5f * s, 10.5f * s), style = Stroke(width = 1.7f * s))
        drawLine(strokeColor, Offset(15.5f * s, 15.5f * s), Offset(20 * s, 20 * s), strokeWidth = 1.9f * s, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

// index.html:1166-1175 obuFab/soudanFabの1:1移植。円形・カラーボーダー3px・影付き(タスク文の
// 「コンポーネント様式」記載どおり)。縦積み(オガトレ通信が下・相談室が1段上)。
//
// 見た目パリティ第2弾(TASK-C2-2026-07-26-visual-parity-round2.md §3): 絵文字だけではTalkBackに
// 内容が伝わらないため、contentDescriptionを追加(Web版には無いネイティブならではの上乗せ。
// 見た目・配色・タップ挙動は変更しない)。
@Composable
fun KyonoFab(emoji: String, borderColor: Color, contentDescription: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalKyonoColors.current
    Box(
        modifier = modifier
            .size(56.dp)
            .shadow(4.dp, CircleShape)
            .background(colors.card, CircleShape)
            .border(3.dp, borderColor, CircleShape)
            .clickable(onClickLabel = contentDescription, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = 22.sp)
    }
}
