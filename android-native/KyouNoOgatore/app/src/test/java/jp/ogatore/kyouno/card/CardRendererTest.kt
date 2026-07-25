package jp.ogatore.kyouno.card

import android.graphics.Bitmap
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// ネイティブ移植 Step 4(マスタープラン§6 Step4検収基準4): 「同一日付での再描画が同一出力」を
// ピクセル配列(ARGB IntArray)比較で確認する。CardRendererは現在時刻・乱数を一切読まない設計
// (ForbiddenAPIRegressionTest参照)なので、同じ入力を2回描画すればピクセル単位で一致するはず。
// android.graphics.Canvas/BitmapはプレーンJVMでは動かないためRobolectricでシャドウ実行する。
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
    fun differentDatesProduceDifferentPixels() {
        val data = CardDataLoader.shared
        val theme = sampleTheme()
        val bmp1 = CardRenderer.render("2026-07-25", 55, theme, false, null, CardLottery.dateIdx("2026-07-25"), data.CARD_THEMES_V2_FROM)
        val bmp2 = CardRenderer.render("2026-07-20", 50, theme, true, "50日たっせい", CardLottery.dateIdx("2026-07-20"), data.CARD_THEMES_V2_FROM)
        val p1 = pixels(bmp1)
        val p2 = pixels(bmp2)
        assertFalse("異なる日付なのに同じ出力になっている(装飾/数字が反映されていない疑い)", p1.contentEquals(p2))
    }
}
