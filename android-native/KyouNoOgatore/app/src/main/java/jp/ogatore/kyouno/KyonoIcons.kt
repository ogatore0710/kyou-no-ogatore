package jp.ogatore.kyouno

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
// Phase 3(§「やること」4「アイコン移植」): index.html全編の.sec-head内インラインSVG(手描き風アイコン)を
// Compose Canvas/Pathで1:1移植する。Material Icons代替は不可(タスク文の明示的な指示)。
// 全24箇所のsec-headは形状で見ると14種類に集約される(同一形状で塗り色/アクセント色だけ違う箇所あり)。
// KyonoSectionHeader(icon, title)がsec-head全体(アイコン+タイトルの横並び)を1:1移植する。

private val ink = Color(0xFF3A3A35)

enum class KyonoIcon {
    Clock, Question, QuizCheck, SoudanBubble, ObuBubble, Play, CalendarCheck,
    DexBook, Heart, Envelope, Notes, MountainCheck, ShieldCheck, Star,
    // アイコン方針(TASK-C2-2026-07-30-icon-system.md)既存アイコン適用バッチ: タブバーの
    // 「動画を探す」(虫眼鏡)を検索欄の目印として再利用するため、KyonoTabBar.ktのSearchIconと
    // 同じ形状をここに複製する(タブバー本体は既存のまま・privateなComposableも変更しない)。
    // 「使い方」(開いた本)は`GuideBook`として一度追加したが、実際に見出しとして使う場所が無く
    // (図鑑自体の見出しは既に`DexBook`が担当)、未使用のenumケースを残すと混乱を招くため
    // 削除した(alan5判断・2026-07-30)。
    Search,
    // アイコン方針(I) 新規描き起こしバッチ1(2026-07-30): 「記録のひっこし」の3ボタン
    // (📦記録をコピーする/📋自動で読みこむ/📥よみこむ)は隣接して並ぶため、判断①(3つとも
    // 別の絵にする)のとおり、意味が近くても見分けがつく3種類を個別に描く。
    ExportBox, ClipboardPaste, ImportTray,
    // アイコン方針(I) 新規描き起こしバッチ2(2026-07-30): 「お楽しみ・ごほうび」系3種
    // (👑節目ゴールドカード/🎉お楽しみ機能/🎫おやすみ券)を同じテーマでまとめて描く。
    CrownBadge, ConfettiBurst, TicketStub,
    // アイコン方針(I) 新規描き起こしバッチ3・残り5種(2026-07-30): 🎯は当初「的(同心円)」を
    // 想定していたが、alan5指摘のとおり`Clock`/`Question`と同じ「丸+中身」の構図になり
    // 見分けが怪しくなるため、同心円をやめて旗(ゴールを立てる)に変更した。同じ理由で⏱も
    // 円形の腕時計ではなく砂時計(三角×2)にし、丸い意匠が増えすぎないようにする。
    GoalFlag, Sprout, HourglassTime, PhoneDevice, PaletteArt,
    // UX13案・案6(2026-07-30): index.html:656-658 セグメント(あなた用/あさ/よる)の3アイコン。
    // `Heart`は角丸カード背景+小さいハート(見出し用の合成アイコン)なのでそのまま使えず、
    // Web版のsegMine(単体のハートだけ・背景なし)に相当する専用ケースを新設する。
    SegHeart, SegSun, SegMoon,
}

// UI/UXパリティ監査2巡目A3(2026-07-29): index.html:98 .sec-head svg{width:21px;height:21px}の
// 1:1移植。従来24dpだったため常時+14%(bigtext時はさらに1.18倍で実効+35%相当)大きく、
// 図鑑・使い方・検索・マイ記録・ホームの全セクション見出しに波及していた欠落を修正する。
@Composable
fun KyonoSectionHeader(icon: KyonoIcon, title: String, fill: Color, accent: Color = Color(0xFFE56A9A), modifier: Modifier = Modifier) {
    val colors = LocalKyonoColors.current
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        KyonoIconGlyph(icon, fill, accent, Modifier.size(21.dp))
        Text(title, color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun KyonoIconGlyph(icon: KyonoIcon, fill: Color, accent: Color = Color(0xFFE56A9E), modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.width / 24f
        fun pt(x: Float, y: Float) = Offset(x * s, y * s)
        when (icon) {
            KyonoIcon.Clock -> {
                drawCircle(fill, radius = 8.5f * s, center = pt(12f, 12f))
                drawCircle(ink, radius = 8.5f * s, center = pt(12f, 12f), style = Stroke(2.2f * s))
                val hand = Path().apply { moveTo(pt(12f, 7.5f).x, pt(12f, 7.5f).y); lineTo(pt(12f, 12f).x, pt(12f, 12f).y); lineTo(pt(15f, 14.5f).x, pt(15f, 14.5f).y) }
                drawPath(hand, ink, style = Stroke(2.2f * s, cap = StrokeCap.Round))
            }
            KyonoIcon.Question -> {
                drawCircle(fill, radius = 8.5f * s, center = pt(12f, 12f))
                drawCircle(ink, radius = 8.5f * s, center = pt(12f, 12f), style = Stroke(2.2f * s))
                val q = Path().apply {
                    moveTo(pt(9f, 10f).x, pt(9f, 10f).y)
                    quadraticBezierTo(pt(9f, 7f).x, pt(9f, 7f).y, pt(12f, 7f).x, pt(12f, 7f).y)
                    quadraticBezierTo(pt(15f, 7f).x, pt(15f, 7f).y, pt(15f, 10f).x, pt(15f, 10f).y)
                    quadraticBezierTo(pt(15f, 12f).x, pt(15f, 12f).y, pt(12f, 13f).x, pt(12f, 13f).y)
                    lineTo(pt(12f, 14f).x, pt(12f, 14f).y)
                }
                drawPath(q, ink, style = Stroke(2.2f * s, cap = StrokeCap.Round))
                drawCircle(ink, radius = 0.3f * s, center = pt(12f, 17f))
            }
            KyonoIcon.QuizCheck -> {
                drawCircle(Color.Transparent, radius = 6.5f * s, center = pt(10.5f, 10.5f))
                drawCircle(ink, radius = 6.5f * s, center = pt(10.5f, 10.5f), style = Stroke(2.2f * s))
                val handle = Path().apply { moveTo(pt(15.5f, 15.5f).x, pt(15.5f, 15.5f).y); lineTo(pt(20f, 20f).x, pt(20f, 20f).y) }
                drawPath(handle, ink, style = Stroke(2.2f * s, cap = StrokeCap.Round))
                val cross = Path().apply {
                    moveTo(pt(8f, 10.5f).x, pt(8f, 10.5f).y); lineTo(pt(13f, 10.5f).x, pt(13f, 10.5f).y)
                    moveTo(pt(10.5f, 8f).x, pt(10.5f, 8f).y); lineTo(pt(10.5f, 13f).x, pt(10.5f, 13f).y)
                }
                drawPath(cross, accent, style = Stroke(2.2f * s, cap = StrokeCap.Round))
            }
            KyonoIcon.SoudanBubble, KyonoIcon.ObuBubble -> {
                val bubble = Path().apply {
                    moveTo(pt(4f, 5.5f).x, pt(4f, 5.5f).y)
                    lineTo(pt(20f, 5.5f).x, pt(20f, 5.5f).y)
                    lineTo(pt(20f, 15.5f).x, pt(20f, 15.5f).y)
                    lineTo(pt(10f, 15.5f).x, pt(10f, 15.5f).y)
                    lineTo(pt(6f, 19f).x, pt(6f, 19f).y)
                    lineTo(pt(6f, 15.5f).x, pt(6f, 15.5f).y)
                    lineTo(pt(4f, 15.5f).x, pt(4f, 15.5f).y)
                    close()
                }
                drawPath(bubble, fill, style = Fill)
                drawPath(bubble, ink, style = Stroke(2.2f * s, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                if (icon == KyonoIcon.SoudanBubble) {
                    val lines = Path().apply {
                        moveTo(pt(8.5f, 9f).x, pt(8.5f, 9f).y); lineTo(pt(15.5f, 9f).x, pt(15.5f, 9f).y)
                        moveTo(pt(8.5f, 12f).x, pt(8.5f, 12f).y); lineTo(pt(13f, 12f).x, pt(13f, 12f).y)
                    }
                    drawPath(lines, accent, style = Stroke(2.2f * s, cap = StrokeCap.Round))
                }
            }
            KyonoIcon.Play -> {
                val rect = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(2.5f * s, 4.5f * s, 21.5f * s, 19.5f * s, CornerRadius(4f * s))) }
                drawPath(rect, fill, style = Fill)
                drawPath(rect, ink, style = Stroke(2.2f * s))
                val tri = Path().apply {
                    moveTo(pt(10f, 9.2f).x, pt(10f, 9.2f).y); lineTo(pt(10f, 14.8f).x, pt(10f, 14.8f).y); lineTo(pt(14.8f, 12f).x, pt(14.8f, 12f).y); close()
                }
                drawPath(tri, accent, style = Fill)
            }
            KyonoIcon.CalendarCheck -> {
                val rect = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(3f * s, 5f * s, 21f * s, 21f * s, CornerRadius(3.5f * s))) }
                drawPath(rect, fill, style = Fill)
                drawPath(rect, ink, style = Stroke(2.2f * s))
                val lines = Path().apply {
                    moveTo(pt(3f, 9.5f).x, pt(3f, 9.5f).y); lineTo(pt(21f, 9.5f).x, pt(21f, 9.5f).y)
                    moveTo(pt(8f, 3f).x, pt(8f, 3f).y); lineTo(pt(8f, 7f).x, pt(8f, 7f).y)
                    moveTo(pt(16f, 3f).x, pt(16f, 3f).y); lineTo(pt(16f, 7f).x, pt(16f, 7f).y)
                }
                drawPath(lines, ink, style = Stroke(2.2f * s, cap = StrokeCap.Round))
                val check = Path().apply {
                    moveTo(pt(8.5f, 14.5f).x, pt(8.5f, 14.5f).y); lineTo(pt(11f, 17f).x, pt(11f, 17f).y); lineTo(pt(15.5f, 12f).x, pt(15.5f, 12f).y)
                }
                drawPath(check, accent, style = Stroke(2.2f * s, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
            KyonoIcon.DexBook -> {
                val rect = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(3.5f * s, 4f * s, 20.5f * s, 20f * s, CornerRadius(3f * s))) }
                drawPath(rect, fill, style = Fill)
                drawPath(rect, ink, style = Stroke(2.2f * s))
                val lines = Path().apply {
                    moveTo(pt(12f, 4f).x, pt(12f, 4f).y); lineTo(pt(12f, 20f).x, pt(12f, 20f).y)
                    moveTo(pt(8f, 8f).x, pt(8f, 8f).y); lineTo(pt(9.5f, 8f).x, pt(9.5f, 8f).y)
                    moveTo(pt(8f, 12f).x, pt(8f, 12f).y); lineTo(pt(9.5f, 12f).x, pt(9.5f, 12f).y)
                    moveTo(pt(14.5f, 8f).x, pt(14.5f, 8f).y); lineTo(pt(16f, 8f).x, pt(16f, 8f).y)
                    moveTo(pt(14.5f, 12f).x, pt(14.5f, 12f).y); lineTo(pt(16f, 12f).x, pt(16f, 12f).y)
                }
                drawPath(lines, ink, style = Stroke(2.2f * s, cap = StrokeCap.Round))
            }
            KyonoIcon.Heart -> {
                val rect = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(3f * s, 5f * s, 21f * s, 20f * s, CornerRadius(3.5f * s))) }
                drawPath(rect, fill, style = Fill)
                drawPath(rect, ink, style = Stroke(2.2f * s))
                val heart = Path().apply {
                    moveTo(pt(12f, 17f).x, pt(12f, 17f).y)
                    cubicTo(pt(9.4f, 15f).x, pt(9.4f, 15f).y, pt(7.9f, 13.3f).x, pt(7.9f, 13.3f).y, pt(8.4f, 11.5f).x, pt(8.4f, 11.5f).y)
                    cubicTo(pt(8.9f, 9.5f).x, pt(8.9f, 9.5f).y, pt(10.9f, 9.9f).x, pt(10.9f, 9.9f).y, pt(11.65f, 10.9f).x, pt(11.65f, 10.9f).y)
                    cubicTo(pt(12f, 9.9f).x, pt(12f, 9.9f).y, pt(14f, 9.5f).x, pt(14f, 9.5f).y, pt(14.5f, 11.5f).x, pt(14.5f, 11.5f).y)
                    cubicTo(pt(15f, 13.3f).x, pt(15f, 13.3f).y, pt(13.5f, 15f).x, pt(13.5f, 15f).y, pt(12f, 17f).x, pt(12f, 17f).y)
                    close()
                }
                drawPath(heart, accent, style = Fill)
            }
            KyonoIcon.Envelope -> {
                val rect = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(3f * s, 5f * s, 21f * s, 20f * s, CornerRadius(3.5f * s))) }
                drawPath(rect, fill, style = Fill)
                drawPath(rect, ink, style = Stroke(2.2f * s))
                val chevron = Path().apply {
                    moveTo(pt(3.5f, 8f).x, pt(3.5f, 8f).y); lineTo(pt(12f, 13.5f).x, pt(12f, 13.5f).y); lineTo(pt(20.5f, 8f).x, pt(20.5f, 8f).y)
                }
                drawPath(chevron, ink, style = Stroke(2.2f * s, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
            KyonoIcon.Notes -> {
                val rect = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(4f * s, 3.5f * s, 20f * s, 20.5f * s, CornerRadius(3f * s))) }
                drawPath(rect, fill, style = Fill)
                drawPath(rect, ink, style = Stroke(2.2f * s))
                val lines = Path().apply {
                    moveTo(pt(8f, 8.5f).x, pt(8f, 8.5f).y); lineTo(pt(16f, 8.5f).x, pt(16f, 8.5f).y)
                    moveTo(pt(8f, 12f).x, pt(8f, 12f).y); lineTo(pt(16f, 12f).x, pt(16f, 12f).y)
                    moveTo(pt(8f, 15.5f).x, pt(8f, 15.5f).y); lineTo(pt(13f, 15.5f).x, pt(13f, 15.5f).y)
                }
                drawPath(lines, ink, style = Stroke(2.2f * s, cap = StrokeCap.Round))
            }
            KyonoIcon.MountainCheck -> {
                val mountain = Path().apply {
                    moveTo(pt(3f, 17f).x, pt(3f, 17f).y); lineTo(pt(17f, 3f).x, pt(17f, 3f).y); lineTo(pt(21f, 7f).x, pt(21f, 7f).y); lineTo(pt(7f, 21f).x, pt(7f, 21f).y); close()
                }
                drawPath(mountain, fill, style = Fill)
                drawPath(mountain, ink, style = Stroke(2.2f * s, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                val check = Path().apply {
                    moveTo(pt(13f, 7f).x, pt(13f, 7f).y); lineTo(pt(17f, 11f).x, pt(17f, 11f).y)
                    moveTo(pt(7f, 13f).x, pt(7f, 13f).y); lineTo(pt(9f, 15f).x, pt(9f, 15f).y)
                    moveTo(pt(10f, 10f).x, pt(10f, 10f).y); lineTo(pt(12f, 12f).x, pt(12f, 12f).y)
                }
                drawPath(check, ink, style = Stroke(2.2f * s, cap = StrokeCap.Round))
            }
            KyonoIcon.ShieldCheck -> {
                val shield = Path().apply {
                    moveTo(pt(12f, 3f).x, pt(12f, 3f).y)
                    lineTo(pt(19f, 6f).x, pt(19f, 6f).y)
                    lineTo(pt(19f, 12f).x, pt(19f, 12f).y)
                    cubicTo(pt(19f, 16.5f).x, pt(19f, 16.5f).y, pt(16f, 19.5f).x, pt(16f, 19.5f).y, pt(12f, 21f).x, pt(12f, 21f).y)
                    cubicTo(pt(8f, 19.5f).x, pt(8f, 19.5f).y, pt(5f, 16.5f).x, pt(5f, 16.5f).y, pt(5f, 12f).x, pt(5f, 12f).y)
                    lineTo(pt(5f, 6f).x, pt(5f, 6f).y)
                    close()
                }
                drawPath(shield, fill, style = Fill)
                drawPath(shield, ink, style = Stroke(2.2f * s, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                val check = Path().apply {
                    moveTo(pt(8.5f, 12f).x, pt(8.5f, 12f).y); lineTo(pt(11f, 14.5f).x, pt(11f, 14.5f).y); lineTo(pt(15.5f, 9.5f).x, pt(15.5f, 9.5f).y)
                }
                drawPath(check, accent, style = Stroke(2.2f * s, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
            KyonoIcon.Star -> {
                val star = Path().apply {
                    moveTo(pt(12f, 3f).x, pt(12f, 3f).y)
                    lineTo(pt(14.5f, 8.5f).x, pt(14.5f, 8.5f).y)
                    lineTo(pt(20f, 9f).x, pt(20f, 9f).y)
                    lineTo(pt(16f, 13f).x, pt(16f, 13f).y)
                    lineTo(pt(17f, 19f).x, pt(17f, 19f).y)
                    lineTo(pt(12f, 16f).x, pt(12f, 16f).y)
                    lineTo(pt(7f, 19f).x, pt(7f, 19f).y)
                    lineTo(pt(8f, 13f).x, pt(8f, 13f).y)
                    lineTo(pt(4f, 9f).x, pt(4f, 9f).y)
                    lineTo(pt(9.5f, 8.5f).x, pt(9.5f, 8.5f).y)
                    close()
                }
                drawPath(star, fill, style = Fill)
                drawPath(star, ink, style = Stroke(1.6f * s, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
            KyonoIcon.Search -> {
                drawCircle(fill, radius = 6.5f * s, center = pt(10.5f, 10.5f))
                drawCircle(ink, radius = 6.5f * s, center = pt(10.5f, 10.5f), style = Stroke(2.2f * s))
                val handle = Path().apply { moveTo(pt(15.5f, 15.5f).x, pt(15.5f, 15.5f).y); lineTo(pt(20f, 20f).x, pt(20f, 20f).y) }
                drawPath(handle, ink, style = Stroke(2.2f * s, cap = StrokeCap.Round))
            }
            KyonoIcon.ExportBox -> {
                val rect = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(4f * s, 9f * s, 20f * s, 21f * s, CornerRadius(1.5f * s))) }
                drawPath(rect, fill, style = Fill)
                drawPath(rect, ink, style = Stroke(2.2f * s))
                val flap = Path().apply {
                    moveTo(pt(4f, 9f).x, pt(4f, 9f).y); lineTo(pt(12f, 4.5f).x, pt(12f, 4.5f).y); lineTo(pt(20f, 9f).x, pt(20f, 9f).y)
                }
                drawPath(flap, ink, style = Stroke(2.2f * s, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                val tape = Path().apply {
                    moveTo(pt(12f, 9f).x, pt(12f, 9f).y); lineTo(pt(12f, 21f).x, pt(12f, 21f).y)
                    moveTo(pt(4f, 15f).x, pt(4f, 15f).y); lineTo(pt(20f, 15f).x, pt(20f, 15f).y)
                }
                drawPath(tape, accent, style = Stroke(2.2f * s, cap = StrokeCap.Round))
            }
            KyonoIcon.ClipboardPaste -> {
                val board = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(4f * s, 5.5f * s, 20f * s, 21f * s, CornerRadius(2f * s))) }
                drawPath(board, fill, style = Fill)
                drawPath(board, ink, style = Stroke(2.2f * s))
                val clip = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(9f * s, 3f * s, 15f * s, 6f * s, CornerRadius(1.2f * s))) }
                drawPath(clip, ink, style = Fill)
                val lines = Path().apply {
                    moveTo(pt(7f, 11f).x, pt(7f, 11f).y); lineTo(pt(17f, 11f).x, pt(17f, 11f).y)
                    moveTo(pt(7f, 14.5f).x, pt(7f, 14.5f).y); lineTo(pt(15f, 14.5f).x, pt(15f, 14.5f).y)
                }
                drawPath(lines, accent, style = Stroke(2.2f * s, cap = StrokeCap.Round))
            }
            KyonoIcon.ImportTray -> {
                val tray = Path().apply {
                    moveTo(pt(3f, 14f).x, pt(3f, 14f).y); lineTo(pt(21f, 14f).x, pt(21f, 14f).y)
                    lineTo(pt(17.5f, 20f).x, pt(17.5f, 20f).y); lineTo(pt(6.5f, 20f).x, pt(6.5f, 20f).y); close()
                }
                drawPath(tray, fill, style = Fill)
                drawPath(tray, ink, style = Stroke(2.2f * s, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                val shaft = Path().apply { moveTo(pt(12f, 3f).x, pt(12f, 3f).y); lineTo(pt(12f, 12f).x, pt(12f, 12f).y) }
                drawPath(shaft, ink, style = Stroke(2.2f * s, cap = StrokeCap.Round))
                val head = Path().apply {
                    moveTo(pt(8f, 8f).x, pt(8f, 8f).y); lineTo(pt(12f, 12f).x, pt(12f, 12f).y); lineTo(pt(16f, 8f).x, pt(16f, 8f).y)
                }
                drawPath(head, accent, style = Stroke(2.2f * s, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
            KyonoIcon.CrownBadge -> {
                val crown = Path().apply {
                    moveTo(pt(4f, 17f).x, pt(4f, 17f).y); lineTo(pt(4f, 8f).x, pt(4f, 8f).y); lineTo(pt(8f, 13f).x, pt(8f, 13f).y)
                    lineTo(pt(12f, 6f).x, pt(12f, 6f).y); lineTo(pt(16f, 13f).x, pt(16f, 13f).y); lineTo(pt(20f, 8f).x, pt(20f, 8f).y)
                    lineTo(pt(20f, 17f).x, pt(20f, 17f).y); close()
                }
                drawPath(crown, fill, style = Fill)
                drawPath(crown, ink, style = Stroke(2.2f * s, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                drawLine(ink, pt(4f, 17f), pt(20f, 17f), strokeWidth = 2.2f * s, cap = StrokeCap.Round)
                drawCircle(accent, radius = 1.3f * s, center = pt(12f, 6f))
            }
            KyonoIcon.ConfettiBurst -> {
                // alan5差し戻し(2026-07-30): 閉じた三角+点3つは「再生ボタン」に読める
                // (.playの三角と衝突)ため、筒の口を閉じずに開けた形に描き直す。
                // fill用は閉じたPath、stroke用は口側(遠い辺)を結ばない2本の別Pathにすることで、
                // 塗りは筒の形のまま・線は開いた口に見えるようにする。
                val hornFill = Path().apply {
                    moveTo(pt(5f, 19f).x, pt(5f, 19f).y); lineTo(pt(9f, 8f).x, pt(9f, 8f).y); lineTo(pt(17f, 12f).x, pt(17f, 12f).y); close()
                }
                drawPath(hornFill, fill, style = Fill)
                val hornStroke = Path().apply {
                    moveTo(pt(5f, 19f).x, pt(5f, 19f).y); lineTo(pt(9f, 8f).x, pt(9f, 8f).y)
                    moveTo(pt(5f, 19f).x, pt(5f, 19f).y); lineTo(pt(17f, 12f).x, pt(17f, 12f).y)
                }
                drawPath(hornStroke, ink, style = Stroke(2.2f * s, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                drawCircle(accent, radius = 1f * s, center = pt(11f, 3f))
                drawCircle(accent, radius = 1f * s, center = pt(21f, 8f))
                drawLine(accent, pt(17f, 3f), pt(19f, 5f), strokeWidth = 1.8f * s, cap = StrokeCap.Round)
            }
            KyonoIcon.TicketStub -> {
                // alan5相談(2026-07-30): 左右のヘコミ(切り取り線)を試す。単純な丸角長方形+
                // 円の別パスによる打ち抜きではなく、外周パス自体に凹アーチを組み込む
                // (quadraticBezierToで中心へ引き寄せる)ことで、通常のfill/strokeだけで
                // 背景色に依存せず正しく描ける。
                val ticket = Path().apply {
                    moveTo(pt(3f, 6f).x, pt(3f, 6f).y)
                    lineTo(pt(21f, 6f).x, pt(21f, 6f).y)
                    lineTo(pt(21f, 10f).x, pt(21f, 10f).y)
                    quadraticBezierTo(pt(18.5f, 12f).x, pt(18.5f, 12f).y, pt(21f, 14f).x, pt(21f, 14f).y)
                    lineTo(pt(21f, 18f).x, pt(21f, 18f).y)
                    lineTo(pt(3f, 18f).x, pt(3f, 18f).y)
                    lineTo(pt(3f, 14f).x, pt(3f, 14f).y)
                    quadraticBezierTo(pt(5.5f, 12f).x, pt(5.5f, 12f).y, pt(3f, 10f).x, pt(3f, 10f).y)
                    close()
                }
                drawPath(ticket, fill, style = Fill)
                drawPath(ticket, ink, style = Stroke(2.2f * s, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                drawLine(
                    accent, pt(12f, 7f), pt(12f, 17f), strokeWidth = 2.2f * s, cap = StrokeCap.Round,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(2.2f * s, 2.4f * s)),
                )
            }
            KyonoIcon.GoalFlag -> {
                drawLine(ink, pt(6f, 20f), pt(6f, 4f), strokeWidth = 2.2f * s, cap = StrokeCap.Round)
                val flag = Path().apply {
                    moveTo(pt(6f, 4f).x, pt(6f, 4f).y); lineTo(pt(18f, 7.5f).x, pt(18f, 7.5f).y); lineTo(pt(6f, 11f).x, pt(6f, 11f).y); close()
                }
                drawPath(flag, accent, style = Fill)
                drawPath(flag, ink, style = Stroke(2.2f * s, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
            KyonoIcon.Sprout -> {
                drawLine(ink, pt(12f, 20f), pt(12f, 12f), strokeWidth = 2.2f * s, cap = StrokeCap.Round)
                val leftLeaf = Path().apply {
                    moveTo(pt(12f, 14f).x, pt(12f, 14f).y)
                    quadraticBezierTo(pt(6f, 14f).x, pt(6f, 14f).y, pt(6f, 10f).x, pt(6f, 10f).y)
                    quadraticBezierTo(pt(9f, 9f).x, pt(9f, 9f).y, pt(12f, 12f).x, pt(12f, 12f).y)
                    close()
                }
                drawPath(leftLeaf, fill, style = Fill)
                drawPath(leftLeaf, ink, style = Stroke(2.2f * s, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                val rightLeaf = Path().apply {
                    moveTo(pt(12f, 12f).x, pt(12f, 12f).y)
                    quadraticBezierTo(pt(18f, 12f).x, pt(18f, 12f).y, pt(18f, 8f).x, pt(18f, 8f).y)
                    quadraticBezierTo(pt(15f, 7f).x, pt(15f, 7f).y, pt(12f, 10f).x, pt(12f, 10f).y)
                    close()
                }
                drawPath(rightLeaf, accent, style = Fill)
                drawPath(rightLeaf, ink, style = Stroke(2.2f * s, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
            KyonoIcon.HourglassTime -> {
                val top = Path().apply {
                    moveTo(pt(6f, 4f).x, pt(6f, 4f).y); lineTo(pt(18f, 4f).x, pt(18f, 4f).y); lineTo(pt(12f, 11f).x, pt(12f, 11f).y); close()
                }
                drawPath(top, fill, style = Fill)
                drawPath(top, ink, style = Stroke(2.2f * s, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                val bottom = Path().apply {
                    moveTo(pt(6f, 20f).x, pt(6f, 20f).y); lineTo(pt(18f, 20f).x, pt(18f, 20f).y); lineTo(pt(12f, 13f).x, pt(12f, 13f).y); close()
                }
                drawPath(bottom, fill, style = Fill)
                drawPath(bottom, ink, style = Stroke(2.2f * s, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                drawCircle(accent, radius = 1.2f * s, center = pt(12f, 12.4f))
            }
            KyonoIcon.PhoneDevice -> {
                val body = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(7f * s, 3f * s, 17f * s, 21f * s, CornerRadius(2f * s))) }
                drawPath(body, fill, style = Fill)
                drawPath(body, ink, style = Stroke(2.2f * s))
                drawLine(ink, pt(10f, 5.5f), pt(14f, 5.5f), strokeWidth = 2.2f * s, cap = StrokeCap.Round)
                drawLine(accent, pt(10f, 18.5f), pt(14f, 18.5f), strokeWidth = 2.2f * s, cap = StrokeCap.Round)
            }
            KyonoIcon.PaletteArt -> {
                val punch = Path().apply {
                    addOval(androidx.compose.ui.geometry.Rect(3f * s, 6f * s, 19f * s, 18f * s))
                    addOval(androidx.compose.ui.geometry.Rect(5.2f * s, 13.2f * s, 8.8f * s, 16.8f * s))
                    fillType = PathFillType.EvenOdd
                }
                drawPath(punch, fill, style = Fill)
                drawOval(ink, topLeft = pt(3f, 6f), size = androidx.compose.ui.geometry.Size(16f * s, 12f * s), style = Stroke(2.2f * s))
                drawOval(ink, topLeft = pt(5.2f, 13.2f), size = androidx.compose.ui.geometry.Size(3.6f * s, 3.6f * s), style = Stroke(2.2f * s))
                // alan5差し戻し(2026-07-30): 前回の3点はパレット本体の楕円からはみ出して
                // いた(右端に寄せるほど楕円は細くなるため)。中心寄りに置き直し、親指の穴
                // (center 7,15・半径1.8)とも重ならない位置を選ぶ。
                drawCircle(accent, radius = 1.3f * s, center = pt(13f, 9f))
                drawCircle(accent, radius = 1.3f * s, center = pt(14.5f, 13f))
                drawCircle(accent, radius = 1.3f * s, center = pt(13f, 16f))
            }
            // UX13案・案6(2026-07-30): index.html:656 segMineの1:1移植(単体のハート・
            // fill #FFEDF3/stroke #E56A9A・背景カードなし)。`Heart`は角丸カード背景+小さいハート
            // (見出し用の合成アイコン)なのでそのまま使えず、専用ケースを新設する。
            KyonoIcon.SegHeart -> {
                val heart = Path().apply {
                    moveTo(pt(12f, 21f).x, pt(12f, 21f).y)
                    cubicTo(pt(7f, 17.5f).x, pt(7f, 17.5f).y, pt(4f, 14f).x, pt(4f, 14f).y, pt(4.8f, 10.6f).x, pt(4.8f, 10.6f).y)
                    cubicTo(pt(5.47f, 8.2f).x, pt(5.47f, 8.2f).y, pt(8.13f, 7.87f).x, pt(8.13f, 7.87f).y, pt(9.8f, 7.6f).x, pt(9.8f, 7.6f).y)
                    cubicTo(pt(10.6f, 7.87f).x, pt(10.6f, 7.87f).y, pt(11.4f, 8.53f).x, pt(11.4f, 8.53f).y, pt(12f, 10f).x, pt(12f, 10f).y)
                    cubicTo(pt(12.6f, 8.53f).x, pt(12.6f, 8.53f).y, pt(13.4f, 7.87f).x, pt(13.4f, 7.87f).y, pt(14.2f, 7.6f).x, pt(14.2f, 7.6f).y)
                    cubicTo(pt(15.87f, 7.87f).x, pt(15.87f, 7.87f).y, pt(18.53f, 8.2f).x, pt(18.53f, 8.2f).y, pt(19.2f, 10.6f).x, pt(19.2f, 10.6f).y)
                    cubicTo(pt(20f, 14f).x, pt(20f, 14f).y, pt(17f, 17.5f).x, pt(17f, 17.5f).y, pt(12f, 21f).x, pt(12f, 21f).y)
                    close()
                }
                drawPath(heart, fill, style = Fill)
                drawPath(heart, Color(0xFFE56A9A), style = Stroke(2.2f * s))
            }
            // index.html:657 segAsaの1:1移植(太陽・光線4本)。
            KyonoIcon.SegSun -> {
                drawOval(Color(0xFFFFD93B), topLeft = pt(7.5f, 8.5f), size = androidx.compose.ui.geometry.Size(9f * s, 9f * s), style = Fill)
                val rays = Path().apply {
                    moveTo(pt(12f, 3.5f).x, pt(12f, 3.5f).y); lineTo(pt(12f, 6f).x, pt(12f, 6f).y)
                    moveTo(pt(4f, 13f).x, pt(4f, 13f).y); lineTo(pt(2f, 13f).x, pt(2f, 13f).y)
                    moveTo(pt(22f, 13f).x, pt(22f, 13f).y); lineTo(pt(20f, 13f).x, pt(20f, 13f).y)
                    moveTo(pt(5.5f, 6.5f).x, pt(5.5f, 6.5f).y); lineTo(pt(7.3f, 8.3f).x, pt(7.3f, 8.3f).y)
                    moveTo(pt(18.5f, 6.5f).x, pt(18.5f, 6.5f).y); lineTo(pt(16.7f, 8.3f).x, pt(16.7f, 8.3f).y)
                }
                drawPath(rays, ink, style = Stroke(2.2f * s, cap = StrokeCap.Round))
            }
            // index.html:658 segYoruの1:1移植(三日月・大円から小円を欠き取る)。
            // alan5差し戻し(2026-07-30): 旧実装はevenOddで2円の「XOR」領域を塗っていたため、
            // 小円が大円からはみ出た部分(右上)まで余計に塗られ、縁取りも二重円の輪郭がそのまま
            // 出ていた(evenOddは小円が大円に完全に内包される場合しか正しい「差分」にならない・
            // 三日月は小円が意図的にはみ出る形のため不成立)。Path.combine(Difference)で本当に
            // 「大円から小円をくり抜いた」1本の輪郭パスを作り、それを塗り・縁取りの両方に使う
            // (結果パスの輪郭がそのまま三日月の外側/内側の弧になるため、Web版と同じ二重弧の
            // 縁取りも自然に再現できる)。
            KyonoIcon.SegMoon -> {
                val big = Path().apply { addOval(androidx.compose.ui.geometry.Rect(4f * s, 2.5f * s, 20f * s, 18.5f * s)) }
                val small = Path().apply { addOval(androidx.compose.ui.geometry.Rect(9f * s, 1.5f * s, 23f * s, 15.5f * s)) }
                val moon = Path.combine(PathOperation.Difference, big, small)
                drawPath(moon, Color(0xFFB8A9F0), style = Fill)
                drawPath(moon, ink, style = Stroke(2.2f * s))
            }
        }
    }
}
