package jp.ogatore.kyouno.card

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

// ネイティブ移植 Step 4(マスタープラン§2-1・§2-4・§6 Step 4)→Step 4/7bパリティ突合タスク
// (TASK-C2-2026-07-26-native-migration-card-visual-assets.md): 記録カード描画(index.html:119-349
// drawCard)の1:1移植。同じ1000x1000座標系を使う。android.graphics.Canvas/Bitmap(実機でそのまま動く
// 本物のAPI)で実装する——プレーンJVM単体テストではandroid.jarのCanvas/Bitmapは中身のないスタブで
// 動かないため、Robolectric(JVM上でAndroidフレームワークをシャドウ実装するテストライブラリ)を使って
// エミュレータ無しにテストする(app/build.gradle.ktsのコメント参照)。
//
// §6検収基準4「同一日付での再描画が同一出力」を満たす範囲: 全項目実装済み(キャラ立ち絵・カード柄
// モチーフ画像・かたさタイプ/メモのタグピル行・フッター吹き出し文言・M PLUS 1p/BananaNumフォント含む)。
// これらの選択ロジックはすべてcardRand/dateIdx等の決定的入力のみで決まり、現在時刻・乱数は使わない
// (キャラ立ち絵の日替わりローテは、Web版のdayIndex()=現在時刻依存とは意図的に差をつけている。
// dateIdxを種にすることで「同じ日付の再描画は常に同じキャラ」を保証する。§1-1第3項の要請どおり)。
//
// 現在時刻・乱数を直接読まない設計(§1-1第3項・§2-4末尾の禁止事項。厳守): 装飾の並びはCardLottery.cardRand
// (dateIdxを種にした決定的PRNG)のみで決まり、システム乱数源・現在時刻APIには一切触れない。
// 同じ入力(ds・effTotal・theme・milestone・pat・typeName・memoText・streakCount)なら常にビット単位で
// 同じBitmapを返す(criterion 4。ただしcontext==null時はTypeface.DEFAULT_BOLD代替・アセット未解決時は
// 該当パーツを省略するだけで、入力が同じなら出力も同じという性質自体は変わらない)。
data class ResolvedTheme(val name: String, val bg: List<String>, val main: String, val deco: List<String>)

// index.html:46-52 CHARA_FILES(app-card.js)の1:1移植。Web版はdayIndex()(現在時刻)で選ぶが、
// ネイティブはdateIdx(カードの日付から決まる決定的な値)で選ぶ(上記コメント参照)。
// CardDataLoader.shared.CHARA_FILES(card-data.json・手写しではなくgen-card-data.mjsでJSから機械抽出
// 済みのデータ。§1-2「手写し禁止」原則どおり)を再利用する。file値は"assets/chara-good.png"形式の
// Web側パスなので、Android drawable資源名("chara_good")へ変換するだけの純関数。
// internal: BragCardRenderer(同じ日替わりキャラローテを使う。§2-1備考「同じアセット・フォント」)からも参照する。
internal fun charaDrawableName(webPath: String): String =
    webPath.removePrefix("assets/").removeSuffix(".png").replace("-", "_")
internal const val CHARA_CROWN = "chara_crown"

// TestFlight実機フィードバックE1(2026-07-29): 当初は「アイコン画像があるのはこの3タイプのみ」
// だったが、6体ぶんのPNGが揃った(res/drawable-nodpi/type_{koka,ashi,robot}.png)ため6体とも
// 対象に含める。iOS CardCoreと同じ穴(この地図に無いタイプはアイコンなしで描画されていた)が
// こちらにもあり、koka/ashi/robotが実際に欠けていた。
internal val TYPE_IMG = mapOf(
    "momo" to "type_momo", "kenko" to "type_kenko", "yawara" to "type_yawara",
    "koka" to "type_koka", "ashi" to "type_ashi", "robot" to "type_robot",
)

internal enum class CardFontWeight { W700, W800, W900, BANANA }

// フォント(assets/fonts/配下・§7bタスクでMPLUS1p-{Bold,ExtraBold,Black}.ttf/bananaslip.otfを同梱)の
// 遅延読み込み+キャッシュ。context==null(テスト等)ではTypeface.DEFAULT_BOLDへ安全にフォールバックする。
internal object CardFonts {
    private val cache = mutableMapOf<CardFontWeight, Typeface>()
    fun get(context: Context?, weight: CardFontWeight): Typeface {
        if (context == null) return Typeface.DEFAULT_BOLD
        return cache.getOrPut(weight) {
            val path = when (weight) {
                CardFontWeight.W700 -> "fonts/mplus1p-700.ttf"
                CardFontWeight.W800 -> "fonts/mplus1p-800.ttf"
                CardFontWeight.W900 -> "fonts/mplus1p-900.ttf"
                CardFontWeight.BANANA -> "fonts/banananum.otf"
            }
            try { Typeface.createFromAsset(context.assets, path) } catch (e: Exception) { Typeface.DEFAULT_BOLD }
        }
    }
}

object CardRenderer {
    fun render(
        ds: String,
        effTotal: Int,
        theme: ResolvedTheme,
        milestone: Boolean,
        milestoneTitle: String?,
        dateIdx: Int,
        cardThemesV2From: Int,
        context: Context? = null,
        pat: CardPattern? = null,
        typeName: String? = null,
        typeIconKey: String? = null,
        memoText: String? = null,
        streakCount: Int = 0,
        displayTotal: Int? = null,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // TASK-C2-2026-08-05-build27-round5.md R-13: displayTotalは「大数字の表示だけ」を差し替える
        // ためのフック(ツアー練習カード用に0を渡す)。milestone判定・柄抽選(pat)は呼び出し元が
        // effTotal(実データ)で計算済みの結果をそのまま渡しているため、ここでは一切影響しない。
        draw(canvas, ds, displayTotal ?: effTotal, theme, milestone, milestoneTitle, dateIdx, cardThemesV2From, context, pat, typeName, typeIconKey, memoText, streakCount)
        return bitmap
    }

    private fun draw(
        canvas: Canvas, ds: String, effTotal: Int, theme: ResolvedTheme,
        milestone: Boolean, milestoneTitle: String?, dateIdx: Int, cardThemesV2From: Int,
        context: Context?, pat: CardPattern?, typeName: String?, typeIconKey: String?, memoText: String?, streakCount: Int,
    ) {
        val f900 = CardFonts.get(context, CardFontWeight.W900)
        val f800 = CardFonts.get(context, CardFontWeight.W800)
        val f700 = CardFonts.get(context, CardFontWeight.W700)
        val fBanana = CardFonts.get(context, CardFontWeight.BANANA)

        // 背景グラデ(index.html:144-146)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 1000f, 1000f, color(theme.bg[0]), color(theme.bg[1]), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, 1000f, 1000f, bgPaint)

        // カード柄モチーフ画像 or 日替わり散らし装飾(index.html:147-203)
        val motifBitmap = if (context != null && pat?.key != null) loadDrawableBitmap(context, pat.key) else null
        if (motifBitmap != null) {
            // 画像方式(index.html:148-150): 透過モチーフを全面に重ねる
            canvas.drawBitmap(motifBitmap, null, RectF(0f, 0f, 1000f, 1000f), Paint(Paint.ANTI_ALIAS_FLAG))
        } else if (pat != null) {
            // index.html:151-166 ノーマル(画像なし)・画像未読込時のフォールバック=日替わり散らし
            // (yozora特別扱い・月・節目紙吹雪は無し。pat!=nullの時点でmilestoneはfalse確定=cardPatternFor仕様)
            drawScatterSimple(canvas, theme, dateIdx)
        } else {
            // index.html:167-203 CARD_IMG_FROM未満 or 画像なし節目: 従来方式(1バイトも変えない)
            drawDecorationsLegacy(canvas, theme, dateIdx, cardThemesV2From, milestone)
        }

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
        drawCenteredText(canvas, "#きょうのオガトレ", 500f, 190f, 34f, Color.WHITE, f900)

        // 日付バッジ(index.html:213-220)
        val parts = ds.split("-").mapNotNull { it.toIntOrNull() }
        val dtxt = if (parts.size == 3) "${parts[0]}/${parts[1]}/${parts[2]}" else ds
        val dw = textWidth(dtxt, 26f, f800)
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorWithAlpha(theme.main, 0.85f) }
        canvas.drawRoundRect(RectF(868f - dw - 44f, 212f, 868f, 264f), 26f, 26f, badgePaint)
        drawCenteredText(canvas, dtxt, 868f - (dw + 44f) / 2f, 247f, 26f, Color.WHITE, f800)

        // 見出し(節目のときだけ。index.html:222)
        if (milestone) {
            drawCrownShape(canvas, 500f, 258f, 100f, color("#FFD700"))
            val msTxt = "${milestoneTitle ?: "節目たっせい"}！おめでとうございます！"
            drawCenteredText(canvas, msTxt, 500f, 330f, 34f, color("#8A877D"), f800)
        }

        // 山本さん案レイアウト: 数字+日目！を1行 → タグピル行(index.html:223-296)。
        // rowsの有無で大数字のY位置がずれる(shift)ため、rowsを先に確定させる。
        val rows = buildList {
            if (typeName != null) add("かたさタイプ" to typeName)
            if (memoText != null) add("メモ" to memoText)
        }
        val shift = if (milestone) 0f else if (rows.isNotEmpty()) 0f else 60f
        val by = (if (milestone) 520f else 462f) + shift

        // 通算日数の大数字(index.html:230-238)
        val numTxt = effTotal.toString()
        val numW = textWidth(numTxt, 180f, f900)
        val dayW = textWidth("日目！", 84f, f900)
        val bx = 500f - (numW + 16f + dayW) / 2f
        drawLeftText(canvas, numTxt, bx, by, 180f, color(theme.main), f900)
        drawLeftText(canvas, "日目！", bx + numW + 16f, by, 84f, color("#3A3A35"), f900)

        // 連続記録N日(index.html:240)
        if (streakCount >= 2) {
            drawCenteredText(canvas, "連続記録${streakCount}日", 500f, by + 52f, 30f, color("#8A877D"), f700)
        }

        // キャラ選定(dateIdx駆動・決定的。上部コメント参照)
        val charaFiles = CardDataLoader.shared.CHARA_FILES
        val charaPick = charaFiles[((dateIdx % charaFiles.size) + charaFiles.size) % charaFiles.size]
        val charaBitmap = if (context != null) loadDrawableBitmap(context, charaDrawableName(charaPick.file)) else null
        val crownBitmap = if (context != null) loadDrawableBitmap(context, CHARA_CROWN) else null
        val charaW = charaPick.w.toFloat()

        // タグピル行(index.html:241-296)
        val rowY = if (milestone) listOf(648f, 764f) else listOf(614f, 738f)
        val chW = if (milestone) 280f else charaW
        val chActive = if (milestone) (crownBitmap != null || charaBitmap != null) else charaBitmap != null
        val row1RightEdge = if (chActive) 985f - chW - 24f else 860f
        rows.take(2).forEachIndexed { i, row ->
            val (label, value) = row
            val yc = rowY[i]
            val lw = textWidth(label, 28f, fBanana)
            val pw = lw + 48f
            canvas.drawRoundRect(RectF(130f, yc - 30f, 130f + pw, yc + 30f), 30f, 30f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = color(theme.main) })
            drawLeftText(canvas, label, 130f + 24f, yc + 10f, 28f, Color.WHITE, fBanana)
            val vx = 130f + pw + 26f
            val maxW = (if (i == 1) row1RightEdge else 860f) - vx
            var fs = 46f
            val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = f800; color = color("#3A3A35") }
            valuePaint.textSize = fs
            while (valuePaint.measureText(value) > maxW && fs > 32f) { fs -= 2f; valuePaint.textSize = fs }
            if (valuePaint.measureText(value) <= maxW) {
                drawLeftText(canvas, value, vx, yc + Math.round(fs * 0.36f), fs, color("#3A3A35"), f800)
                if (label == "かたさタイプ" && typeIconKey != null) {
                    val iconBitmap = if (context != null) TYPE_IMG[typeIconKey]?.let { loadDrawableBitmap(context, it) } else null
                    if (iconBitmap != null) {
                        val ih = fs + 10f
                        val ar = iconBitmap.width.toFloat() / iconBitmap.height.toFloat()
                        val iw = ih * ar
                        val ix = vx + valuePaint.measureText(value) + 14f
                        val iy = yc - ih / 2f
                        if (ix + iw <= maxW + vx) {
                            canvas.drawBitmap(iconBitmap, null, RectF(ix, iy, ix + iw, iy + ih), Paint(Paint.ANTI_ALIAS_FLAG))
                        }
                    }
                }
            } else if (i == 1) {
                // メモ行はキャラ回避で幅が狭くなりうるため3行まで許容(index.html:274-278)
                fs = 28f
                val lines = wrapLinesWeighted(value, maxW, 3, fs, f800)
                val ly0 = yc - (lines.size - 1) * 19f
                lines.forEachIndexed { li, l -> drawLeftText(canvas, l, vx, ly0 + li * 38f, fs, color("#3A3A35"), f800) }
            } else {
                // 2行に分割(index.html:280-287)
                fs = 30f
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = f800; textSize = fs }
                var l1 = ""; var i2 = 0
                while (i2 < value.length && p.measureText(l1 + value[i2]) <= maxW) { l1 += value[i2]; i2++ }
                var l2 = value.substring(i2)
                while (p.measureText(l2) > maxW && l2.length > 2) { l2 = l2.substring(0, l2.length - 2) + "…" }
                drawLeftText(canvas, l1, vx, yc - 6f, fs, color("#3A3A35"), f800)
                drawLeftText(canvas, l2, vx, yc + 32f, fs, color("#3A3A35"), f800)
            }
        }
        // 点線区切り(index.html:291-296)
        if (rows.size >= 2) {
            val sy = (rowY[0] + rowY[1]) / 2f
            val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = 3f
                color = colorWithAlpha(theme.main, 0.4f)
                pathEffect = android.graphics.DashPathEffect(floatArrayOf(3f, 11f), 0f)
            }
            canvas.drawLine(150f, sy, 850f, sy, dividerPaint)
        }

        // キャラ本体(index.html:297-302)
        val chFig = if (milestone) (crownBitmap ?: charaBitmap) else charaBitmap
        if (chFig != null) {
            val w = if (milestone) 280f else charaW
            val h = w * chFig.height / chFig.width
            canvas.drawBitmap(chFig, null, RectF(985f - w, 998f - h, 985f, 998f), Paint(Paint.ANTI_ALIAS_FLAG))
        }

        // フッター=キャラの吹き出し(index.html:303-341)
        drawFooterBubble(canvas, ds, theme, f800)
    }

    // index.html:151-166「else if(pat)」分岐の1:1移植: pat!=null(トク/季節/レア/ノーマルいずれか確定済み)
    // だが画像未解決の場合の日替わり散らし。CARD_THEMES_V2_FROM分岐・yozora特別扱い・月は無い
    // (この分岐に来る時点でtheme.nameはpatのbg/main/decoから来るためよぞらになり得ない=Web版と同仕様)。
    private fun drawScatterSimple(canvas: Canvas, theme: ResolvedTheme, dateIdx: Int) {
        val rnd = CardLottery.cardRand(dateIdx.toUInt())
        val shapes = listOf("h", "s", "k", "c", "f", "k", "c")
        val bands = listOf(
            floatArrayOf(30f, 970f, 25f, 150f), floatArrayOf(30f, 970f, 850f, 975f),
            floatArrayOf(25f, 75f, 160f, 850f), floatArrayOf(925f, 975f, 160f, 850f),
        )
        val n = 12 + floor(rnd() * 5).toInt()
        for (i in 0 until n) {
            val b = bands[floor(rnd() * bands.size).toInt()]
            val x = kotlin.math.round((b[0] + rnd() * (b[1] - b[0])).toFloat())
            val y = kotlin.math.round((b[2] + rnd() * (b[3] - b[2])).toFloat())
            val sh = shapes[floor(rnd() * shapes.size).toInt()]
            val sz = kotlin.math.round((10 + rnd() * 24).toFloat())
            drawShape(canvas, sh, x, y, sz, color(theme.deco[i % theme.deco.size]))
        }
    }

    private fun drawDecorationsLegacy(canvas: Canvas, theme: ResolvedTheme, dateIdx: Int, cardThemesV2From: Int, milestone: Boolean) {
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
            drawShape(canvas, d.shape, d.x, d.y, d.sz, col)
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

    private fun drawShape(canvas: Canvas, shape: String, x: Float, y: Float, sz: Float, col: Int) {
        when (shape) {
            "h" -> drawHeartShape(canvas, x - sz / 2, y - sz / 2, sz, col)
            "s" -> drawStarShape(canvas, x, y, sz * 0.6f, col)
            "k" -> drawSparkleShape(canvas, x, y, sz * 0.7f, col)
            "f" -> drawFlowerShape(canvas, x, y, sz * 0.62f, col)
            else -> canvas.drawCircle(x, y, sz, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col })
        }
    }

    // index.html:303-329 フッター(キャラの吹き出し)の1:1移植。日付文字列の31進ハッシュで
    // プールから選ぶだけの決定的ロジック(VoicesLogic/BragCardRendererと同じ手法)。
    private fun drawFooterBubble(canvas: Canvas, ds: String, theme: ResolvedTheme, font800: Typeface) {
        val fm = ds.substring(5, 7).toInt()
        val fd = ds.substring(8, 10).toInt()
        val common = listOf(
            "今日も一日、じぶんを大切に。ご自愛くださいね。",
            "がんばりすぎず、ほどよく。ご自愛くださいね。",
            "深呼吸ひとつぶん、じぶんをいたわる時間を。",
            "今日のあなたに、おつかれさまとご自愛を。",
            "体の声をきいて、むりせずご自愛くださいね。",
            "つづけてる自分を、ちゃんと褒めてあげてね。",
        )
        val pool = when {
            fm == 1 && fd <= 7 -> listOf("今年もじぶんのペースで。ご自愛くださいね。", "新年も一日一本、ゆるっといきましょう。")
            fm == 12 && fd >= 28 -> listOf("一年おつかれさま。ゆっくりご自愛くださいね。", "今年もよくがんばりました。よいお年を。")
            fm == 6 -> common + listOf("じめじめの季節も心は軽く。ご自愛くださいね。", "雨の日は、おうちストレッチ日和です。")
            fm == 7 || fm == 8 -> common + listOf("暑い毎日、水分とご自愛を忘れずに。", "夏バテ予防も、ストレッチとご自愛から。")
            fm in 9..11 -> common + listOf("季節の変わり目、ゆっくりご自愛くださいね。", "実りの秋。体にもいいことを少しずつ。")
            fm == 12 || fm <= 2 -> common + listOf("寒い季節も、あたたかくご自愛くださいね。", "湯船にゆっくり。それもご自愛のうち。")
            else -> common + listOf("新しい季節も、マイペースにご自愛くださいね。", "春の陽気と一緒に、体もゆるめてあげてね。")
        }
        var fh = 0u
        for (c in ds) fh = fh * 31u + c.code.toUInt()
        val fmsg = pool[(fh % pool.size.toUInt()).toInt()]
        var ffs = 27f
        while (textWidth(fmsg, ffs, font800) > 560f && ffs > 21f) ffs -= 1f
        val bw = textWidth(fmsg, ffs, font800) + 56f
        val bx1 = maxOf(70f, 690f - bw)
        canvas.drawRoundRect(RectF(bx1, 900f, bx1 + bw, 974f), 37f, 37f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(242, 255, 255, 255) })
        val tailPath = Path().apply {
            moveTo(bx1 + bw - 6f, 918f); lineTo(bx1 + bw + 26f, 930f); lineTo(bx1 + bw - 6f, 950f); close()
        }
        canvas.drawPath(tailPath, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(242, 255, 255, 255) })
        drawLeftText(canvas, fmsg, bx1 + 28f, 900f + 37f + (ffs * 0.36f), ffs, color(theme.main), font800)
    }

    // 図鑑(DexScreen.kt)と同じres/drawable-nodpi参照パターン(Step7a)を再利用。
    internal fun loadDrawableBitmap(context: Context, name: String): Bitmap? {
        val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
        if (resId == 0) return null
        return try { BitmapFactory.decodeResource(context.resources, resId) } catch (e: Exception) { null }
    }

    // MARK: - 図形ヘルパー(index.html:2624-2690 roundRect/drawHeart/drawStar/drawSparkle/drawFlower/drawMoon/drawCrownの移植)

    // internal(privateでない)にしているのはBragCardRenderer(Step7b)が同じ図形/テキスト/色ヘルパーを
    // 再利用するため(記録カードとじまんカードは1000x1000の同じ舞台演出を共有する。index.html:2814
    // 「背景・飾り・白カード（記録カードと同じ舞台）」というコメントどおり)。
    internal fun drawHeartShape(canvas: Canvas, x: Float, y: Float, s: Float, color: Int) {
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

    internal fun drawStarShape(canvas: Canvas, x: Float, y: Float, r: Float, color: Int) {
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

    internal fun drawSparkleShape(canvas: Canvas, x: Float, y: Float, r: Float, color: Int) {
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

    internal fun drawFlowerShape(canvas: Canvas, x: Float, y: Float, r: Float, color: Int) {
        for (i in 0 until 5) {
            val a = (Math.PI * 2 / 5 * i - Math.PI / 2).toFloat()
            val cx = x + r * 0.6f * cos(a)
            val cy = y + r * 0.6f * sin(a)
            canvas.drawCircle(cx, cy, r * 0.42f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
        }
        canvas.drawCircle(x, y, r * 0.3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.argb(230, 255, 255, 255) })
    }

    internal fun drawMoonShape(canvas: Canvas, x: Float, y: Float, r: Float, color: Int) {
        val path = Path().apply {
            addArc(RectF(x - r, y - r, x + r, y + r), -90f, 180f)
            quadTo(x - r * 0.7f, y, x, y - r)
            close()
        }
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }

    internal fun drawCrownShape(canvas: Canvas, x: Float, y: Float, w: Float, color: Int) {
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

    // MARK: - テキスト(フォントは呼び出し側がCardFonts.getで解決したTypefaceを渡す)

    internal fun textPaint(fontSize: Float, textColor: Int, typeface: Typeface): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        textSize = fontSize
        color = textColor
    }

    internal fun textWidth(text: String, fontSize: Float, typeface: Typeface): Float = textPaint(fontSize, Color.BLACK, typeface).measureText(text)

    internal fun drawCenteredText(canvas: Canvas, text: String, centerX: Float, baselineY: Float, fontSize: Float, textColor: Int, typeface: Typeface) {
        val w = textWidth(text, fontSize, typeface)
        drawLeftText(canvas, text, centerX - w / 2, baselineY, fontSize, textColor, typeface)
    }

    internal fun drawLeftText(canvas: Canvas, text: String, x: Float, baselineY: Float, fontSize: Float, textColor: Int, typeface: Typeface) {
        canvas.drawText(text, x, baselineY, textPaint(fontSize, textColor, typeface))
    }

    // index.html:2625-2635 wrapLinesの1:1移植(タグピルのメモ行3行折り返し用)。
    private fun wrapLinesWeighted(text: String, maxW: Float, maxLines: Int, fontSize: Float, typeface: Typeface): List<String> {
        val p = textPaint(fontSize, Color.BLACK, typeface)
        val lines = mutableListOf<String>()
        var rest = text
        while (rest.isNotEmpty() && lines.size < maxLines) {
            var l = ""; var i = 0
            while (i < rest.length && p.measureText(l + rest[i]) <= maxW) { l += rest[i]; i++ }
            if (i == 0) { l = rest[0].toString(); i = 1 }
            lines.add(l); rest = rest.substring(i)
        }
        if (rest.isNotEmpty() && lines.isNotEmpty()) {
            var last = lines[lines.size - 1]
            while (p.measureText(last + "…") > maxW && last.isNotEmpty()) last = last.dropLast(1)
            lines[lines.size - 1] = last + "…"
        }
        return lines
    }

    // MARK: - 色

    internal fun color(hex: String): Int {
        val s = hex.removePrefix("#")
        if (s.length != 6) return Color.BLACK
        val v = s.toLongOrNull(16) ?: return Color.BLACK
        return Color.rgb(((v shr 16) and 0xFF).toInt(), ((v shr 8) and 0xFF).toInt(), (v and 0xFF).toInt())
    }

    internal fun colorWithAlpha(hex: String, alpha: Float): Int {
        val base = color(hex)
        return Color.argb((alpha * 255).toInt(), Color.red(base), Color.green(base), Color.blue(base))
    }
}
