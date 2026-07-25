package jp.ogatore.kyouno.card

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

// ネイティブ移植 Step 4(マスタープラン§6 Step4検収基準4)→Step4/7bパリティ突合タスク
// (TASK-C2-2026-07-26-native-migration-card-visual-assets.md): 「同一日付での再描画が同一出力」を
// ピクセル配列(ARGB IntArray)比較で確認する。CardRendererは現在時刻・乱数を一切読まない設計
// (ForbiddenAPIRegressionTest参照)なので、同じ入力を2回描画すればピクセル単位で一致するはず。
// android.graphics.Canvas/BitmapはプレーンJVMでは動かないためRobolectricでシャドウ実行する。
// context有り(実フォント・実画像アセットを実際にロードする経路)でも同じ保証が成り立つことを、
// パリティ突合タスクで追加したテストで確認する(下の各テストのcontext引数参照)。
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CardRendererTest {
    private fun sampleTheme(): ResolvedTheme {
        val t = CardDataLoader.shared.CARD_THEMES[0]
        return ResolvedTheme(t.name, t.bg, t.main, t.deco)
    }

    private fun pixels(bitmap: Bitmap): IntArray {
        val out = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(out, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return out
    }

    @Test
    fun sameInputProducesIdenticalPixelsTwice() {
        val data = CardDataLoader.shared
        val ds = "2026-07-25"
        val di = CardLottery.dateIdx(ds)
        val bmp1 = CardRenderer.render(ds, 55, sampleTheme(), false, null, di, data.CARD_THEMES_V2_FROM)
        val bmp2 = CardRenderer.render(ds, 55, sampleTheme(), false, null, di, data.CARD_THEMES_V2_FROM)
        val p1 = pixels(bmp1)
        val p2 = pixels(bmp2)
        assertTrue(p1.isNotEmpty())
        assertTrue("同一日付・同一入力の再描画がピクセル単位で一致しない", p1.contentEquals(p2))
    }

    @Test
    fun debugNoContextPixel() {
        val data = CardDataLoader.shared
        val ds = "2026-07-25"
        val di = CardLottery.dateIdx(ds)
        val bmp1 = CardRenderer.render(ds, 55, sampleTheme(), false, null, di, data.CARD_THEMES_V2_FROM)
        val p1 = pixels(bmp1)
        println("DEBUG noctx p1[0]=" + p1[0] + " p1[500500]=" + p1[500500])
        val context = RuntimeEnvironment.getApplication()
        val bmp2 = CardRenderer.render(ds, 55, sampleTheme(), false, null, di, data.CARD_THEMES_V2_FROM, context = context)
        val p2 = pixels(bmp2)
        println("DEBUG ctx p2[0]=" + p2[0] + " p2[500500]=" + p2[500500])
        var diffCount = 0
        for (i in p1.indices) if (p1[i] != p2[i]) diffCount++
        println("DEBUG noctx-vs-ctx diffCount=" + diffCount)
    }

    @Test
    fun differentDatesProduceDifferentPixels() {
        val data = CardDataLoader.shared
        val theme = sampleTheme()
        val bmp1 = CardRenderer.render("2026-07-25", 55, theme, false, null, CardLottery.dateIdx("2026-07-25"), data.CARD_THEMES_V2_FROM)
        val bmp2 = CardRenderer.render("2026-07-20", 50, theme, true, "50日たっせい", CardLottery.dateIdx("2026-07-20"), data.CARD_THEMES_V2_FROM)
        val p1 = pixels(bmp1)
        val p2 = pixels(bmp2)
        assertFalse("異なる日付なのに同じ出力になっている(装飾/数字が反映されていない疑い)", p1.contentEquals(p2))
    }

    // ---- ここからパリティ突合タスク(TASK-C2-2026-07-26-native-migration-card-visual-assets.md)追加分 ----

    @Test
    fun sameInputWithRealAssetsProducesIdenticalPixelsTwice() {
        val data = CardDataLoader.shared
        val ds = "2026-07-25"
        val di = CardLottery.dateIdx(ds)
        val context = RuntimeEnvironment.getApplication()
        val bmp1 = CardRenderer.render(
            ds, 55, sampleTheme(), false, null, di, data.CARD_THEMES_V2_FROM,
            context = context, typeName = "もちもちタイプ", typeIconKey = "momo", memoText = "きょうもいい調子", streakCount = 3,
        )
        val bmp2 = CardRenderer.render(
            ds, 55, sampleTheme(), false, null, di, data.CARD_THEMES_V2_FROM,
            context = context, typeName = "もちもちタイプ", typeIconKey = "momo", memoText = "きょうもいい調子", streakCount = 3,
        )
        assertTrue("実フォント・実画像込みの再描画がピクセル単位で一致しない", pixels(bmp1).contentEquals(pixels(bmp2)))
    }

    @Test
    fun debugRawCanvasDraw() {
        val bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.RED }
        canvas.drawRect(0f, 0f, 100f, 100f, paint)
        val out = IntArray(100 * 100)
        bmp.getPixels(out, 0, 100, 0, 0, 100, 100)
        println("DEBUG rawRedFill center=" + out[5050] + " expectedRed=" + Color.RED)
    }

    @Test
    fun debugTagPillDiff() {
        val data = CardDataLoader.shared
        val ds = "2026-07-25"
        val di = CardLottery.dateIdx(ds)
        val context = RuntimeEnvironment.getApplication()
        val withoutTags = CardRenderer.render(ds, 55, sampleTheme(), false, null, di, data.CARD_THEMES_V2_FROM, context = context)
        val withTags = CardRenderer.render(
            ds, 55, sampleTheme(), false, null, di, data.CARD_THEMES_V2_FROM,
            context = context, typeName = "もちもちタイプ", memoText = "メモです",
        )
        val p1 = pixels(withoutTags)
        val p2 = pixels(withTags)
        var diff = 0
        for (i in p1.indices) if (p1[i] != p2[i]) diff++
        println("DEBUG diff pixel count=" + diff + " total=" + p1.size)
        println("DEBUG p1[0]=" + p1[0] + " p2[0]=" + p2[0])
    }

    @Test
    fun tagPillRowChangesPixelsComparedToNoTagRow() {
        val data = CardDataLoader.shared
        val ds = "2026-07-25"
        val di = CardLottery.dateIdx(ds)
        val context = RuntimeEnvironment.getApplication()
        val withoutTags = CardRenderer.render(ds, 55, sampleTheme(), false, null, di, data.CARD_THEMES_V2_FROM, context = context)
        val withTags = CardRenderer.render(
            ds, 55, sampleTheme(), false, null, di, data.CARD_THEMES_V2_FROM,
            context = context, typeName = "もちもちタイプ", memoText = "メモです",
        )
        assertFalse("タグピル行(かたさタイプ/メモ)の有無で出力が変わっていない", pixels(withoutTags).contentEquals(pixels(withTags)))
    }

    @Test
    fun sameDateIdxAlwaysPicksSameCharacter() {
        // dateIdx駆動のキャラ選定(Web版のdayIndex()=現在時刻依存とは意図的に差をつけている・
        // CardRenderer.kt冒頭コメント参照)が本当に決定的か、同じdsで3回描画してすべて一致するかで確認する。
        val data = CardDataLoader.shared
        val ds = "2026-06-10"
        val di = CardLottery.dateIdx(ds)
        val context = RuntimeEnvironment.getApplication()
        val bitmaps = (1..3).map { CardRenderer.render(ds, 10, sampleTheme(), false, null, di, data.CARD_THEMES_V2_FROM, context = context) }
        val allPixels = bitmaps.map { pixels(it) }
        assertTrue("同じ日付なのにキャラクター選定(dateIdx駆動)が再現しない", allPixels[0].contentEquals(allPixels[1]) && allPixels[1].contentEquals(allPixels[2]))
    }
}
