package jp.ogatore.kyouno.card

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

// ネイティブ移植 Step 4(マスタープラン§2-1・§2-4・§6 Step 4): 記録カード描画(index.html:119-349
// drawCard)の1:1移植。同じ1000x1000座標系を使う。android.graphics.Canvas/Bitmap(実機でそのまま動く
// 本物のAPI)で実装する——プレーンJVM単体テストではandroid.jarのCanvas/Bitmapは中身のないスタブで
// 動かないため、Robolectric(JVM上でAndroidフレームワークをシャドウ実装するテストライブラリ)を使って
// エミュレータ無しにテストする(app/build.gradle.ktsのコメント参照)。
//
// Step 4時点のスコープ(§6検収基準4「同一日付での再描画が同一出力」を満たす範囲に絞っている):
//   実装済み: 背景グラデーション・日替わり散らし装飾(cardRand駆動・新旧2方式の分岐込み)・
//             白カードパネル+破線ふち・タイトルピル・日付バッジ・通算日数の大数字・節目の王冠+文言。
//   未実装(Step4のスコープ外・検収基準に含まれない。Step7bのパリティ突合で扱う想定):
//     キャラクター立ち絵(chara-*.png)・CARD_IMG_FROM以降のカード柄モチーフ画像(assets/cards/*.webp)・
//     かたさタイプ/メモのタグピル行・フッター吹き出し文言・M PLUS 1p/BananaNumフォント
//     (アセット未同梱のため現時点はTypeface.DEFAULT_BOLDで代替。フォント差はビットマップ比較の対象外)。
//
// 現在時刻・乱数を直接読まない設計(§1-1第3項・§2-4末尾の禁止事項。厳守): 装飾の並びはCardLottery.cardRand
// (dateIdxを種にした決定的PRNG)のみで決まり、システム乱数源・現在時刻APIには一切触れない。
// 同じ入力(ds・effTotal・theme・milestone)なら常にビット単位で同じBitmapを返す(criterion 4)。
data class ResolvedTheme(val name: String, val bg: List<String>, val main: String, val deco: List<String>)

object CardRenderer {
    fun render(
        ds: String,
        effTotal: Int,
        theme: ResolvedTheme,
        milestone: Boolean,
        milestoneTitle: String?,
        dateIdx: Int,
        cardThemesV2From: Int,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        draw(canvas, ds, effTotal, theme, milestone, milestoneTitle, dateIdx, cardThemesV2From)
        return bitmap
    }

    private fun draw(
        canvas: Canvas, ds: String, effTotal: Int, theme: ResolvedTheme,
        milestone: Boolean, milestoneTitle: String?, dateIdx: Int, cardThemesV2From: Int,
    ) {
        // 背景グラデ(index.html:144-146)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 1000f, 1000f, color(theme.bg[0]), color(theme.bg[1]), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, 1000f, 1000f, bgPaint)

        // 日替わり散らし装飾(index.html:167-203。CARD_THEMES_V2_FROM以降か節目かで新旧いずれかの方式)
        drawDecorations(canvas, theme, dateIdx, cardThemesV2From, milestone)

        // 白カード(index.html:204-207)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(240, 255, 255, 255) }
        canvas.drawRoundRect(RectF(85f, 175f, 915f, 825f), 52f, 52f, cardPaint)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            strokeCap = Paint.Cap.ROUND
            color = colorWithAlpha(theme.main, 0.45f)
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(2f, 16f), 0f)
        }
        canvas.drawRoundRect(RectF(110f, 200f, 890f, 800f), 40f, 40f, borderPaint)

        // タイトルピル(index.html:208-211)
        val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = color(theme.main) }
        canvas.drawRoundRect(RectF(300f, 145f, 700f, 209f), 32f, 32f, mainPaint)
        drawCenteredText(canvas, "#きょうのオガトレ", 500f, 190f, 34f, Color.WHITE)

        // 日付バッジ(index.html:213-220)
        val parts = ds.split("-").mapNotNull { it.toIntOrNull() }
        val dtxt = if (parts.size == 3) "${parts[0]}/${parts[1]}/${parts[2]}" else ds
        val dw = textWidth(dtxt, 26f)
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorWithAlpha(theme.main, 0.85f) }
        canvas.drawRoundRect(RectF(868f - dw - 44f, 212f, 868f, 264f), 26f, 26f, badgePaint)
        drawCenteredText(canvas, dtxt, 868f - (dw + 44f) / 2f, 247f, 26f, Color.WHITE)

        // 見出し(節目のときだけ。index.html:222)
        var by = 462f
        if (milestone) {
            drawCrownShape(canvas, 500f, 258f, 100f, color("#FFD700"))
            val msTxt = "${milestoneTitle ?: "節目たっせい"}！おめでとうございます！"
            drawCenteredText(canvas, msTxt, 500f, 330f, 34f, color("#8A877D"))
            by = 520f
        }

        // 通算日数の大数字(index.html:230-238。M+1p/BananaNumはアセット未同梱のためTypeface.DEFAULT_BOLDで代替)
        val numTxt = effTotal.toString()
        val numW = textWidth(numTxt, 180f)
        val dayW = textWidth("日目！", 84f)
        val bx = 500f - (numW + 16f + dayW) / 2f
        drawLeftText(canvas, numTxt, bx, by, 180f, color(theme.main))
        drawLeftText(canvas, "日目！", bx + numW + 16f, by, 84f, color("#3A3A35"))
    }

    private fun drawDecorations(canvas: Canvas, theme: ResolvedTheme, dateIdx: Int, cardThemesV2From: Int, milestone: Boolean) {
        val isYozora = theme.name == "よぞら"
        data class Deco(val shape: String, val x: Float, val y: Float, val sz: Float)
        val deco = mutableListOf<Deco>()
        if (dateIdx >= cardThemesV2From && !milestone) {
            val rnd = CardLottery.cardRand(dateIdx.toUInt())
            val shapes = listOf("h", "s", "k", "c", "f", "k", "c")
            val bands = listOf(
                floatArrayOf(30f, 970f, 25f, 150f), floatArrayOf(30f, 970f, 850f, 975f),
                floatArrayOf(25f, 75f, 160f, 850f), floatArrayOf(925f, 975f, 160f, 850f),
            )
            val n = 12 + floor(rnd() * 5).toInt()
            repeat(n) {
                val b = bands[floor(rnd() * bands.size).toInt()]
                val x = (b[0] + rnd() * (b[1] - b[0])).toFloat().let { kotlin.math.round(it) }
                val y = (b[2] + rnd() * (b[3] - b[2])).toFloat().let { kotlin.math.round(it) }
                var sh = shapes[floor(rnd() * shapes.size).toInt()]
                if (isYozora && (sh == "h" || sh == "f")) sh = if (rnd() < 0.5) "s" else "k"
                val sz = kotlin.math.round((10 + rnd() * 24).toFloat())
                deco.add(Deco(sh, x, y, sz))
            }
        } else {
            // index.html:185-190 固定配置(CARD_THEMES_V2_FROM未満の従来方式・1バイトも変えない)
            val fixed = listOf(
                Deco("h", 95f, 150f, 34f), Deco("s", 885f, 120f, 26f), Deco("k", 120f, 860f, 30f), Deco("h", 905f, 850f, 30f),
                Deco("c", 60f, 480f, 11f), Deco("k", 940f, 430f, 24f), Deco("s", 180f, 70f, 18f), Deco("c", 500f, 60f, 9f),
                Deco("h", 820f, 300f, 22f), Deco("c", 935f, 650f, 10f), Deco("s", 70f, 690f, 16f), Deco("c", 250f, 935f, 9f), Deco("k", 760f, 945f, 22f),
            )
            deco.addAll(fixed)
        }
        deco.forEachIndexed { i, d ->
            val col = color(theme.deco[i % theme.deco.size])
            when (d.shape) {
                "h" -> drawHeartShape(canvas, d.x - d.sz / 2, d.y - d.sz / 2, d.sz, col)
                "s" -> drawStarShape(canvas, d.x, d.y, d.sz * 0.6f, col)
                "k" -> drawSparkleShape(canvas, d.x, d.y, d.sz * 0.7f, col)
                "f" -> drawFlowerShape(canvas, d.x, d.y, d.sz * 0.62f, col)
                else -> {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col }
                    canvas.drawCircle(d.x, d.y, d.sz, p)
                }
            }
        }
        if (isYozora && !milestone) {
            drawMoonShape(canvas, 152f, 108f, 44f, color("#FFE9A8"))
        }
        if (milestone) {
            for (i in 0 until 24) {
                canvas.save()
                val x = ((i * 173) % 1000).toFloat()
                val y = ((i * 257) % 1000).toFloat()
                canvas.translate(x, y)
                canvas.rotate(Math.toDegrees((i * 1.3).toDouble()).toFloat())
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = color(theme.deco[i % 3]) }
                canvas.drawRect(-8f, -3f, 8f, 3f, p)
                canvas.restore()
            }
        }
    }

    // MARK: - 図形ヘルパー(index.html:2624-2690 roundRect/drawHeart/drawStar/drawSparkle/drawFlower/drawMoon/drawCrownの移植)

    private fun drawHeartShape(canvas: Canvas, x: Float, y: Float, s: Float, color: Int) {
        canvas.save()
        canvas.translate(x, y)
        canvas.scale(s / 24f, s / 24f)
        val path = Path().apply {
            moveTo(12f, 21f)
            cubicTo(4f, 15f, 1f, 10f, 3.5f, 6.5f)
            cubicTo(6f, 3.5f, 10f, 4.5f, 12f, 8f)
            cubicTo(14f, 4.5f, 18f, 3.5f, 20.5f, 6.5f)
            cubicTo(23f, 10f, 20f, 15f, 12f, 21f)
            close()
        }
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
        canvas.restore()
    }

    private fun drawStarShape(canvas: Canvas, x: Float, y: Float, r: Float, color: Int) {
        val path = Path()
        for (i in 0 until 10) {
            val a = (Math.PI / 5 * i - Math.PI / 2).toFloat()
            val rr = if (i % 2 == 0) r else r * 0.45f
            val px = x + rr * cos(a)
            val py = y + rr * sin(a)
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }

    private fun drawSparkleShape(canvas: Canvas, x: Float, y: Float, r: Float, color: Int) {
        val path = Path().apply {
            moveTo(x, y - r)
            quadTo(x + r * 0.15f, y - r * 0.15f, x + r, y)
            quadTo(x + r * 0.15f, y + r * 0.15f, x, y + r)
            quadTo(x - r * 0.15f, y + r * 0.15f, x - r, y)
            quadTo(x - r * 0.15f, y - r * 0.15f, x, y - r)
            close()
        }
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }

    private fun drawFlowerShape(canvas: Canvas, x: Float, y: Float, r: Float, color: Int) {
        for (i in 0 until 5) {
            val a = (Math.PI * 2 / 5 * i - Math.PI / 2).toFloat()
            val cx = x + r * 0.6f * cos(a)
            val cy = y + r * 0.6f * sin(a)
            canvas.drawCircle(cx, cy, r * 0.42f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
        }
        canvas.drawCircle(x, y, r * 0.3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.argb(230, 255, 255, 255) })
    }

    private fun drawMoonShape(canvas: Canvas, x: Float, y: Float, r: Float, color: Int) {
        val path = Path().apply {
            addArc(RectF(x - r, y - r, x + r, y + r), -90f, 180f)
            quadTo(x - r * 0.7f, y, x, y - r)
            close()
        }
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }

    private fun drawCrownShape(canvas: Canvas, x: Float, y: Float, w: Float, color: Int) {
        val h = w * 0.62f
        val path = Path().apply {
            moveTo(x - w / 2, y + h / 2)
            lineTo(x - w / 2, y - h * 0.15f)
            lineTo(x - w * 0.22f, y + h * 0.05f)
            lineTo(x, y - h / 2)
            lineTo(x + w * 0.22f, y + h * 0.05f)
            lineTo(x + w / 2, y - h * 0.15f)
            lineTo(x + w / 2, y + h / 2)
            close()
        }
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL })
        canvas.drawPath(
            path,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.rgb(0x3A, 0x3A, 0x35)
                style = Paint.Style.STROKE
                strokeWidth = 5f
                strokeJoin = Paint.Join.ROUND
            },
        )
    }

    // MARK: - テキスト

    private fun textPaint(fontSize: Float, textColor: Int): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textSize = fontSize
        color = textColor
    }

    private fun textWidth(text: String, fontSize: Float): Float = textPaint(fontSize, Color.BLACK).measureText(text)

    private fun drawCenteredText(canvas: Canvas, text: String, centerX: Float, baselineY: Float, fontSize: Float, textColor: Int) {
        val w = textWidth(text, fontSize)
        drawLeftText(canvas, text, centerX - w / 2, baselineY, fontSize, textColor)
    }

    private fun drawLeftText(canvas: Canvas, text: String, x: Float, baselineY: Float, fontSize: Float, textColor: Int) {
        canvas.drawText(text, x, baselineY, textPaint(fontSize, textColor))
    }

    // MARK: - 色

    private fun color(hex: String): Int {
        val s = hex.removePrefix("#")
        if (s.length != 6) return Color.BLACK
        val v = s.toLongOrNull(16) ?: return Color.BLACK
        return Color.rgb(((v shr 16) and 0xFF).toInt(), ((v shr 8) and 0xFF).toInt(), (v and 0xFF).toInt())
    }

    private fun colorWithAlpha(hex: String, alpha: Float): Int {
        val base = color(hex)
        return Color.argb((alpha * 255).toInt(), Color.red(base), Color.green(base), Color.blue(base))
    }
}
