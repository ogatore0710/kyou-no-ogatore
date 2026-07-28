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
        }
    }
}
