package jp.ogatore.kyouno.card

import android.graphics.Bitmap
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

// Step4/7bパリティ突合タスク(TASK-C2-2026-07-26-native-migration-card-visual-assets.md)で追加:
// BragCardRendererにもCardRendererと同じキャラクター立ち絵・実フォントを組み込んだため、
// 現在時刻・乱数を読まない設計(§1-1第3項)が実アセット込みでも保たれているかを確認する。
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BragCardRendererTest {
    private fun sampleTheme(): ResolvedTheme {
        val t = CardDataLoader.shared.CARD_THEMES[1]
        return ResolvedTheme(t.name, t.bg, t.main, t.deco)
    }

    private fun pixels(bitmap: Bitmap): IntArray {
        val out = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(out, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return out
    }

    @Test
    fun sameInputWithRealAssetsProducesIdenticalPixelsTwice() {
        val context = RuntimeEnvironment.getApplication()
        val ds = "2026-07-25"
        val bmp1 = BragCardRenderer.render(ds, 42, sampleTheme(), "好きな1本のタイトル", context)
        val bmp2 = BragCardRenderer.render(ds, 42, sampleTheme(), "好きな1本のタイトル", context)
        assertTrue("同一入力の再描画がピクセル単位で一致しない", pixels(bmp1).contentEquals(pixels(bmp2)))
    }

    @Test
    fun clampDaysMatchesIndexHtmlBounds() {
        // index.html:2808 Math.max(1,Math.min(9999,...))の1:1移植確認。
        assertTrue(BragCardRenderer.clampDays(0) == 1)
        assertTrue(BragCardRenderer.clampDays(99999) == 9999)
        assertTrue(BragCardRenderer.clampDays(42) == 42)
    }
}
