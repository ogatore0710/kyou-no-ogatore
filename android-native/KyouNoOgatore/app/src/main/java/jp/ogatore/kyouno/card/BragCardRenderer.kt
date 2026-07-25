package jp.ogatore.kyouno.card

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface

// ネイティブ移植 Step 7b(マスタープラン§2-1「index.html drawBragCard」行・§6 Step 7b)→Step4/7b
// パリティ突合タスク(TASK-C2-2026-07-26-native-migration-card-visual-assets.md): じまんカード描画
// (index.html:2805-2919 drawBragCard)の1:1移植。CardRendererとは独立の描画器として作業量を見積もる、
// という§2-1備考どおりファイル自体は分けているが、実際の背景・飾り・白カード・タイトルピル・日付バッジの
// 舞台演出はdrawCard()と全く同じ数値(index.html:2814のコメント「記録カードと同じ舞台」)なので、
// その部分の図形/テキスト/色ヘルパーはCardRenderer(internal公開済み)を呼んで再利用する。
// キャラクター立ち絵・M PLUS 1p/BananaNumフォントもCardRenderer側の実装(CHARA_FILES/CardFonts)を
// そのまま再利用する(§2-1備考「じまん・声…」行の「同じアセット・フォント」)。
//
// 動画サムネイルはネットワーク取得(https://i.ytimg.com/...)を要するため、常にWeb版の「オフラインで
// サムネイルが出せないとき」の代替パス(動画タイトルを2行まで折り返し表示)を採用する——ネットワーク
// 依存を増やさない方針(既存のsdVideoHTML等と同じ判断)。
//
// 現在時刻・乱数を直接読まない設計(§1-1第3項・§2-4末尾の禁止事項。厳守): フッターメッセージ・
// キャラクター選定の選定は日付文字列の31進ハッシュ/dateIdxのみで決まり、システム乱数源・現在時刻APIには
// 一切触れない(キャラ選定がWeb版のdayIndex()=現在時刻依存と違う理由はCardRenderer.kt冒頭コメント参照)。
object BragCardRenderer {
    // index.html:2897-2901 pool の1:1移植。
    private val footerPool = listOf(
        "続けてるじぶん、どんどんじまんしてね✨",
        "この1本と続けた日々が、もうじまんです。",
        "続けてるあなたが、いちばんすてきです。",
    )

    // index.html:2818-2820 deco の1:1移植(drawCard側の固定配置と全く同じ13件。§2-1備考どおり
    // 「記録カードと同じ舞台」のため値を重複させず流用してもよいが、drawBragCard自体が常にこの
    // 固定配置のみを使う=CARD_THEMES_V2_FROM分岐が存在しない仕様なので、ここに独立して持つ)。
    private data class Deco(val shape: String, val x: Float, val y: Float, val sz: Float)
    private val fixedDeco = listOf(
        Deco("h", 95f, 150f, 34f), Deco("s", 885f, 120f, 26f), Deco("k", 120f, 860f, 30f), Deco("h", 905f, 850f, 30f),
        Deco("c", 60f, 480f, 11f), Deco("k", 940f, 430f, 24f), Deco("s", 180f, 70f, 18f), Deco("c", 500f, 60f, 9f),
        Deco("h", 820f, 300f, 22f), Deco("c", 935f, 650f, 10f), Deco("s", 70f, 690f, 16f), Deco("c", 250f, 935f, 9f), Deco("k", 760f, 945f, 22f),
    )

    // index.html:2808 の1:1移植(小数入力を弾かないtype=numberの実測バグ修正込み)。
    fun clampDays(raw: Int): Int = raw.coerceIn(1, 9999)

    fun render(ds: String, days: Int, theme: ResolvedTheme, favoriteTitle: String?, context: Context? = null): Bitmap {
        val bitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        draw(canvas, ds, clampDays(days), theme, favoriteTitle, context)
        return bitmap
    }

    private fun draw(canvas: Canvas, ds: String, days: Int, theme: ResolvedTheme, favoriteTitle: String?, context: Context?) {
        val R = CardRenderer // ヘルパー呼び出しの見た目短縮用
        val f900 = CardFonts.get(context, CardFontWeight.W900)
        val f800 = CardFonts.get(context, CardFontWeight.W800)
        val fBanana = CardFonts.get(context, CardFontWeight.BANANA)

        // 背景グラデ+固定飾り(index.html:2815-2827。記録カードと同じ舞台)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 1000f, 1000f, R.color(theme.bg[0]), R.color(theme.bg[1]), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, 1000f, 1000f, bgPaint)
        fixedDeco.forEachIndexed { i, d ->
            val col = R.color(theme.deco[i % theme.deco.size])
            when (d.shape) {
                "h" -> R.drawHeartShape(canvas, d.x - d.sz / 2, d.y - d.sz / 2, d.sz, col)
                "s" -> R.drawStarShape(canvas, d.x, d.y, d.sz * 0.6f, col)
                "k" -> R.drawSparkleShape(canvas, d.x, d.y, d.sz * 0.7f, col)
                else -> canvas.drawCircle(d.x, d.y, d.sz, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col })
            }
        }

        // 白カード+破線ふち(index.html:2828-2830。drawCardと同じ数値)
        canvas.drawRoundRect(RectF(85f, 175f, 915f, 825f), 52f, 52f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(240, 255, 255, 255) })
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 4f; strokeCap = Paint.Cap.ROUND
            color = R.colorWithAlpha(theme.main, 0.45f)
            pathEffect = DashPathEffect(floatArrayOf(2f, 16f), 0f)
        }
        canvas.drawRoundRect(RectF(110f, 200f, 890f, 800f), 40f, 40f, borderPaint)

        // タイトルピル(index.html:2832-2834。drawCardと同じ数値)
        canvas.drawRoundRect(RectF(300f, 145f, 700f, 209f), 32f, 32f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = R.color(theme.main) })
        R.drawCenteredText(canvas, "#きょうのオガトレ", 500f, 190f, 34f, Color.WHITE, f900)

        // 日付バッジ(index.html:2836-2841。drawCardと同じ数値)
        val parts = ds.split("-").mapNotNull { it.toIntOrNull() }
        val dtxt = if (parts.size == 3) "${parts[0]}/${parts[1]}/${parts[2]}" else ds
        val dw = R.textWidth(dtxt, 26f, f800)
        canvas.drawRoundRect(RectF(868f - dw - 44f, 212f, 868f, 264f), 26f, 26f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = R.colorWithAlpha(theme.main, 0.85f) })
        R.drawCenteredText(canvas, dtxt, 868f - (dw + 44f) / 2f, 247f, 26f, Color.WHITE, f800)

        // つづけてる日数(index.html:2843-2856。桁数でフォントサイズを変える。数字はBananaNum)
        val numTxt = days.toString()
        val numSize = if (numTxt.length <= 2) 200f else if (numTxt.length == 3) 170f else 140f
        val numW = R.textWidth(numTxt, numSize, fBanana)
        val sw = R.textWidth("日つづいてる！", 52f, f900)
        val totalW = numW + 18f + sw
        val startX = 500f - totalW / 2f
        R.drawLeftText(canvas, numTxt, startX, 438f, numSize, R.color(theme.main), fBanana)
        R.drawLeftText(canvas, "日つづいてる！", startX + numW + 18f, 428f, 52f, R.color("#3A3A35"), f900)

        // 区切り線(index.html:2859-2860)
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 3f
            color = R.colorWithAlpha(theme.main, 0.3f)
            pathEffect = DashPathEffect(floatArrayOf(4f, 12f), 0f)
        }
        canvas.drawLine(170f, 488f, 830f, 488f, dividerPaint)

        // 「すきな1本」タグピル(index.html:2862-2875)
        run {
            val label = "すきな1本"
            val lw = R.textWidth(label, 28f, fBanana)
            val pw = lw + 48f
            val yc = 525f
            canvas.drawRoundRect(RectF(500f - pw / 2f, yc - 30f, 500f - pw / 2f + pw, yc + 30f), 30f, 30f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = R.color(theme.main) })
            R.drawLeftText(canvas, label, 500f - pw / 2f + 24f, yc + 10f, 28f, Color.WHITE, fBanana)
        }

        // サムネイル代替=動画タイトルの折り返し表示(index.html:2883-2889。ネットワーク非依存のため常にこの経路)
        val favT = favoriteTitle ?: "まだえらんでません（これから見つけます！）"
        val lines = wrapLines(favT, 540f, 2, f800)
        lines.forEachIndexed { i, ln -> R.drawCenteredText(canvas, ln, 500f, 645f + i * 52f, 34f, R.color("#3A3A35"), f800) }

        // キャラ(index.html:2891-2894。日替わりローテ・CardRenderer.CHARA_FILESを共用。§2-1備考どおり
        // 「同じアセット・フォント」を使う。dateIdx駆動の選定理由はCardRenderer.kt冒頭コメント参照)。
        if (context != null) {
            val dateIdx = CardLottery.dateIdx(ds)
            val charaFiles = CardDataLoader.shared.CHARA_FILES
            val pick = charaFiles[((dateIdx % charaFiles.size) + charaFiles.size) % charaFiles.size]
            val charaBitmap = R.loadDrawableBitmap(context, charaDrawableName(pick.file))
            if (charaBitmap != null) {
                val w = 255f
                val h = w * charaBitmap.height / charaBitmap.width
                canvas.drawBitmap(charaBitmap, null, RectF(965f - w, 985f - h, 965f, 985f), Paint(Paint.ANTI_ALIAS_FLAG))
            }
        }

        // フッター=キャラの吹き出し(index.html:2896-2914)
        var fh = 0u
        for (c in ds) fh = fh * 31u + c.code.toUInt()
        val fmsg = footerPool[(fh % footerPool.size.toUInt()).toInt()]
        var ffs = 27f
        while (R.textWidth(fmsg, ffs, f800) > 560f && ffs > 21f) ffs -= 1f
        val bw = R.textWidth(fmsg, ffs, f800) + 56f
        val bx1 = maxOf(70f, 690f - bw)
        canvas.drawRoundRect(RectF(bx1, 900f, bx1 + bw, 974f), 37f, 37f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(242, 255, 255, 255) })
        R.drawLeftText(canvas, fmsg, bx1 + 28f, 900f + 37f + (ffs * 0.36f), ffs, R.color(theme.main), f800)
    }

    // index.html:2792-2804(drawBragCard直前の後勝ち定義)の1:1移植。1文字ずつ幅を測って折り返し、
    // maxLines到達時は末尾を"…"に置き換える。
    private fun wrapLines(text: String, maxW: Float, maxLines: Int, typeface: Typeface): List<String> {
        val lines = mutableListOf<String>()
        var cur = ""
        for (ch in text) {
            val test = cur + ch
            if (CardRenderer.textWidth(test, 34f, typeface) > maxW && cur.isNotEmpty()) {
                lines.add(cur)
                cur = ch.toString()
                if (lines.size == maxLines) break
            } else {
                cur = test
            }
        }
        if (lines.size < maxLines && cur.isNotEmpty()) {
            lines.add(cur)
        } else if (lines.size == maxLines && cur.isNotEmpty()) {
            lines[maxLines - 1] = lines[maxLines - 1].dropLast(1) + "…"
        }
        return lines
    }
}
