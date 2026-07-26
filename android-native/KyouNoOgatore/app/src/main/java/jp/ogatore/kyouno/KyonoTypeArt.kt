package jp.ogatore.kyouno

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

// フォント適用漏れ・キャラ/タイプ画像の欠落修正タスク(TASK-C2-2026-07-26-visual-parity-fonts-characters.md)
// §3 かたさタイプの画像: app-quiz.js:3-7 TYPE_ART(koka/ashi/robotのインラインSVG)・
// index.html:1437 TYPE_IMG(momo/kenko/yawaraのPNG)の1:1移植。診断結果画面のタイプ名上部
// (Web版 .type-illust、104x104)に表示する。既存のKyonoIcons.ktと同じ要領でSVGはCanvas直書き。
private val TYPE_IMG_RES = mapOf(
    "momo" to "type_momo",
    "kenko" to "type_kenko",
    "yawara" to "type_yawara",
)

@Composable
fun KyonoTypeArt(typeKey: String, modifier: Modifier = Modifier) {
    val resName = TYPE_IMG_RES[typeKey]
    if (resName != null) {
        val context = LocalContext.current
        val resId = remember(resName) { context.resources.getIdentifier(resName, "drawable", context.packageName) }
        if (resId != 0) {
            Image(painter = painterResource(id = resId), contentDescription = typeKey, modifier = modifier.size(104.dp))
        }
    } else {
        Canvas(modifier.size(104.dp)) {
            val s = size.width / 96f
            fun pt(x: Float, y: Float) = Offset(x * s, y * s)
            val ink = Color(0xFF3A3A35)
            when (typeKey) {
                "koka" -> {
                    // app-quiz.js TYPE_ART.koka(開かずのトビラ=扉)の1:1移植。
                    val door = Path().apply {
                        addRoundRect(androidx.compose.ui.geometry.RoundRect(pt(24f, 12f).x, pt(24f, 12f).y, pt(72f, 86f).x, pt(72f, 86f).y, CornerRadius(6f * s)))
                    }
                    drawPath(door, Color(0xFFD9A066), style = Fill)
                    drawPath(door, ink, style = Stroke(3f * s))
                    val panelTop = Path().apply {
                        addRoundRect(androidx.compose.ui.geometry.RoundRect(pt(31f, 20f).x, pt(31f, 20f).y, pt(65f, 46f).x, pt(65f, 46f).y, CornerRadius(4f * s)))
                    }
                    val panelBottom = Path().apply {
                        addRoundRect(androidx.compose.ui.geometry.RoundRect(pt(31f, 52f).x, pt(31f, 52f).y, pt(65f, 78f).x, pt(65f, 78f).y, CornerRadius(4f * s)))
                    }
                    val panelFill = Color(0xFFE8B87E)
                    val panelBorder = Color(0xFFB4805A)
                    drawPath(panelTop, panelFill, style = Fill); drawPath(panelTop, panelBorder, style = Stroke(2f * s))
                    drawPath(panelBottom, panelFill, style = Fill); drawPath(panelBottom, panelBorder, style = Stroke(2f * s))
                    drawCircle(ink, radius = 3.5f * s, center = pt(63f, 49f))
                    drawCircle(ink, radius = 1.8f * s, center = pt(39f, 33f))
                    drawCircle(ink, radius = 1.8f * s, center = pt(53f, 33f))
                    val smile = Path().apply {
                        moveTo(pt(42f, 39f).x, pt(42f, 39f).y)
                        quadraticBezierTo(pt(46f, 41.5f).x, pt(46f, 41.5f).y, pt(50f, 39f).x, pt(50f, 39f).y)
                    }
                    drawPath(smile, ink, style = Stroke(2.2f * s, cap = StrokeCap.Round))
                }
                "ashi" -> {
                    // app-quiz.js TYPE_ART.ashi(棒立ちペンギン)の1:1移植。
                    drawOval(Color(0xFF4E5A6E), topLeft = Offset(pt(24f, 24f).x, pt(24f, 24f).y), size = androidx.compose.ui.geometry.Size(48f * s, 60f * s))
                    drawOval(ink, topLeft = Offset(pt(24f, 24f).x, pt(24f, 24f).y), size = androidx.compose.ui.geometry.Size(48f * s, 60f * s), style = Stroke(3f * s))
                    drawOval(Color.White, topLeft = Offset(pt(33f, 40f).x, pt(33f, 40f).y), size = androidx.compose.ui.geometry.Size(30f * s, 40f * s))
                    drawCircle(ink, radius = 2.2f * s, center = pt(41f, 40f))
                    drawCircle(ink, radius = 2.2f * s, center = pt(55f, 40f))
                    val beak = Path().apply {
                        moveTo(pt(44f, 47f).x, pt(44f, 47f).y); lineTo(pt(48f, 50f).x, pt(48f, 50f).y); lineTo(pt(52f, 47f).x, pt(52f, 47f).y); close()
                    }
                    val beakColor = Color(0xFFF5A25D)
                    drawPath(beak, beakColor, style = Fill)
                    drawPath(beak, ink, style = Stroke(2f * s, join = StrokeJoin.Round))
                    val footL = Path().apply {
                        moveTo(pt(34f, 82f).x, pt(34f, 82f).y)
                        quadraticBezierTo(pt(38f, 85f).x, pt(38f, 85f).y, pt(43f, 83f).x, pt(43f, 83f).y)
                    }
                    val footR = Path().apply {
                        moveTo(pt(62f, 82f).x, pt(62f, 82f).y)
                        quadraticBezierTo(pt(58f, 85f).x, pt(58f, 85f).y, pt(53f, 83f).x, pt(53f, 83f).y)
                    }
                    drawPath(footL, beakColor, style = Stroke(4f * s, cap = StrokeCap.Round))
                    drawPath(footR, beakColor, style = Stroke(4f * s, cap = StrokeCap.Round))
                    val wingL = Path().apply {
                        moveTo(pt(24f, 44f).x, pt(24f, 44f).y)
                        quadraticBezierTo(pt(20f, 52f).x, pt(20f, 52f).y, pt(26f, 58f).x, pt(26f, 58f).y)
                    }
                    val wingR = Path().apply {
                        moveTo(pt(72f, 44f).x, pt(72f, 44f).y)
                        quadraticBezierTo(pt(76f, 52f).x, pt(76f, 52f).y, pt(70f, 58f).x, pt(70f, 58f).y)
                    }
                    drawPath(wingL, ink, style = Stroke(3f * s, cap = StrokeCap.Round))
                    drawPath(wingR, ink, style = Stroke(3f * s, cap = StrokeCap.Round))
                }
                "robot" -> {
                    // app-quiz.js TYPE_ART.robot(ガチガチロボット)の1:1移植。
                    val head = Path().apply {
                        addRoundRect(androidx.compose.ui.geometry.RoundRect(pt(24f, 26f).x, pt(24f, 26f).y, pt(72f, 68f).x, pt(72f, 68f).y, CornerRadius(10f * s)))
                    }
                    val headFill = Color(0xFFC7D3DE)
                    drawPath(head, headFill, style = Fill)
                    drawPath(head, ink, style = Stroke(3f * s))
                    drawLine(ink, pt(48f, 26f), pt(48f, 14f), strokeWidth = 3f * s, cap = StrokeCap.Round)
                    drawCircle(Color(0xFFFF8A70), radius = 4f * s, center = pt(48f, 12f))
                    drawCircle(ink, radius = 4f * s, center = pt(48f, 12f), style = Stroke(2.5f * s))
                    val eyeL = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(pt(33f, 38f).x, pt(33f, 38f).y, pt(43f, 48f).x, pt(43f, 48f).y, CornerRadius(3f * s))) }
                    val eyeR = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(pt(53f, 38f).x, pt(53f, 38f).y, pt(63f, 48f).x, pt(63f, 48f).y, CornerRadius(3f * s))) }
                    drawPath(eyeL, Color.White, style = Fill); drawPath(eyeL, ink, style = Stroke(2.5f * s))
                    drawPath(eyeR, Color.White, style = Fill); drawPath(eyeR, ink, style = Stroke(2.5f * s))
                    drawCircle(ink, radius = 1.8f * s, center = pt(38f, 43f))
                    drawCircle(ink, radius = 1.8f * s, center = pt(58f, 43f))
                    drawLine(ink, pt(40f, 57f), pt(56f, 57f), strokeWidth = 2.5f * s, cap = StrokeCap.Round)
                    val base = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(pt(30f, 72f).x, pt(30f, 72f).y, pt(66f, 82f).x, pt(66f, 82f).y, CornerRadius(5f * s))) }
                    drawPath(base, Color(0xFFE4EAF0), style = Fill)
                    drawPath(base, ink, style = Stroke(2.5f * s))
                }
            }
        }
    }
}
