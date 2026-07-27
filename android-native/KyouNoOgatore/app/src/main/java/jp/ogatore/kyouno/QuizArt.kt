package jp.ogatore.kyouno

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

// TASK-C2-2026-07-28-quiz-result-reach-parity.md §4: app-quiz.js:92-137 QUIZ_ART[2]/[3]の移植。
// Web版コメント「あたま/あごの高さ線＝両ひじが上がる目安」「かかとの浮き」は判定基準そのものの
// 可視化であり装飾ではない(以前は「kenko/ashiは元々手描きSVG/装飾で写真なし」と誤認されて
// 移植対象から外れていた)。SVGの正確な手描き曲線までは再現せず、判定基準(高さの目安線・
// かかとの浮き)が明確に伝わる簡略化した図として実装する(タスク文が明示的に許容する範囲)。
private val quizArtInk = Color(0xFF3A3A35)
private val quizArtSkin = Color(0xFFE8B48C)
private val quizArtPink = Color(0xFFE56A9A)
private val quizArtHead = Color(0xFFFFE3C9)
private val quizArtGround = Color(0xFFE0D8C4)
private val quizArtGuide = Color(0xFFCFC9B8)
private val quizArtLabel = Color(0xFF6E6B5F)

@Composable
fun QuizArtKenko(modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier.fillMaxWidth().aspectRatio(300f / 170f)) {
        val s = size.width / 300f
        fun pt(x: Float, y: Float) = Offset(x * s, y * s)

        // index.html app-quiz.js:96 M30 150h240(ゆか)
        drawLine(quizArtGround, pt(30f, 150f), pt(270f, 150f), strokeWidth = 5f * s, cap = StrokeCap.Round)

        // app-quiz.js:97-99 はな/あごの高さの目安線(点線)+ラベル。判定基準そのもの。
        val dash = PathEffect.dashPathEffect(floatArrayOf(2f * s, 6f * s))
        drawLine(quizArtGuide, pt(136f, 57f), pt(210f, 57f), strokeWidth = 2.5f * s, pathEffect = dash)
        drawLine(quizArtGuide, pt(136f, 69f), pt(210f, 69f), strokeWidth = 2.5f * s, pathEffect = dash)
        val labelStyle = androidx.compose.ui.text.TextStyle(color = quizArtLabel, fontSize = 11.sp, fontWeight = FontWeight.Black)
        drawText(textMeasurer, "はな", topLeft = pt(215f, 53f), style = labelStyle)
        drawText(textMeasurer, "あご", topLeft = pt(215f, 66f), style = labelStyle)

        // app-quiz.js:100 legs
        drawLine(Color(0xFF55524A), pt(112f, 118f), pt(106f, 150f), strokeWidth = 9f * s, cap = StrokeCap.Round)
        drawLine(Color(0xFF55524A), pt(132f, 118f), pt(138f, 150f), strokeWidth = 9f * s, cap = StrokeCap.Round)
        // app-quiz.js:101 torso
        drawLine(quizArtInk, pt(122f, 118f), pt(122f, 80f), strokeWidth = 15f * s, cap = StrokeCap.Round)
        // app-quiz.js:102 head
        drawCircle(quizArtHead, radius = 13f * s, center = pt(122f, 58f))
        drawCircle(quizArtInk, radius = 13f * s, center = pt(122f, 58f), style = Stroke(4f * s))
        // app-quiz.js:105 eyes
        drawCircle(quizArtInk, radius = 2f * s, center = pt(118f, 59f))
        drawCircle(quizArtInk, radius = 2f * s, center = pt(126f, 59f))

        // app-quiz.js:107-110 両ひじをつけて上げた腕(肩→ひじ)
        val armPath = Path().apply {
            moveTo(pt(108f, 80f).x, pt(108f, 80f).y)
            quadraticBezierTo(pt(106f, 58f).x, pt(106f, 58f).y, pt(118f, 42f).x, pt(118f, 42f).y)
            moveTo(pt(136f, 80f).x, pt(136f, 80f).y)
            quadraticBezierTo(pt(138f, 58f).x, pt(138f, 58f).y, pt(126f, 42f).x, pt(126f, 42f).y)
        }
        drawPath(armPath, quizArtSkin, style = Stroke(8f * s, cap = StrokeCap.Round))
        // app-quiz.js:112 ひじが合わさる位置(判定の中心点)
        drawCircle(quizArtPink, radius = 7f * s, center = pt(122f, 41f))
    }
}

@Composable
fun QuizArtAshi(modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier.fillMaxWidth().aspectRatio(300f / 170f)) {
        val s = size.width / 300f
        fun pt(x: Float, y: Float) = Offset(x * s, y * s)

        // app-quiz.js:117 ゆか
        drawLine(quizArtGround, pt(30f, 150f), pt(270f, 150f), strokeWidth = 5f * s, cap = StrokeCap.Round)

        // app-quiz.js:118-120 しゃがんだ脚(太もも・すね・かかとが浮いた足)
        drawLine(Color(0xFF55524A), pt(120f, 112f), pt(152f, 110f), strokeWidth = 13f * s, cap = StrokeCap.Round)
        drawLine(Color(0xFF55524A), pt(120f, 112f), pt(134f, 142f), strokeWidth = 11f * s, cap = StrokeCap.Round)
        drawLine(quizArtInk, pt(120f, 148f), pt(142f, 144f), strokeWidth = 6f * s, cap = StrokeCap.Round)
        // app-quiz.js:122 背中
        val backPath = Path().apply {
            moveTo(pt(152f, 112f).x, pt(152f, 112f).y)
            quadraticBezierTo(pt(150f, 88f).x, pt(150f, 88f).y, pt(158f, 76f).x, pt(158f, 76f).y)
        }
        drawPath(backPath, quizArtInk, style = Stroke(15f * s, cap = StrokeCap.Round))
        // app-quiz.js:123 head
        drawCircle(quizArtHead, radius = 15f * s, center = pt(152f, 60f))
        drawCircle(quizArtInk, radius = 15f * s, center = pt(152f, 60f), style = Stroke(4f * s))
        drawCircle(quizArtInk, radius = 2.2f * s, center = pt(146f, 62f))
        // app-quiz.js:126-127 前に伸ばした腕
        drawLine(quizArtSkin, pt(156f, 80f), pt(118f, 86f), strokeWidth = 8f * s, cap = StrokeCap.Round)

        // app-quiz.js:128-134 かかとの浮きをズームする円+点線の引き出し線+ラベル(判定基準そのもの)
        val dash = PathEffect.dashPathEffect(floatArrayOf(3f * s, 4f * s))
        drawLine(quizArtGuide, pt(158f, 140f), pt(190f, 144f), strokeWidth = 2.5f * s, pathEffect = dash)
        drawCircle(Color.White, radius = 34f * s, center = pt(224f, 110f))
        drawCircle(quizArtInk, radius = 34f * s, center = pt(224f, 110f), style = Stroke(3.5f * s))
        drawLine(quizArtGround, pt(204f, 128f), pt(244f, 128f), strokeWidth = 4f * s)
        drawLine(quizArtInk, pt(209f, 124f), pt(235f, 118f), strokeWidth = 6f * s, cap = StrokeCap.Round)
        // かかとが浮いている隙間を示す矢印
        val arrowDash = PathEffect.dashPathEffect(floatArrayOf(2f * s, 3f * s))
        drawLine(quizArtPink, pt(234f, 120f), pt(234f, 128f), strokeWidth = 3.5f * s, pathEffect = arrowDash)
        val arrowHead = Path().apply {
            moveTo(pt(229f, 122f).x, pt(229f, 122f).y)
            lineTo(pt(234f, 128f).x, pt(234f, 128f).y)
            lineTo(pt(239f, 122f).x, pt(239f, 122f).y)
        }
        drawPath(arrowHead, quizArtPink, style = Stroke(3f * s, cap = StrokeCap.Round))
        val labelStyle = androidx.compose.ui.text.TextStyle(color = quizArtPink, fontSize = 12.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        val measured = textMeasurer.measure("かかと", labelStyle)
        drawText(textMeasurer, "かかと", topLeft = Offset(pt(224f, 90f).x - measured.size.width / 2f, pt(224f, 90f).y), style = labelStyle)
    }
}
