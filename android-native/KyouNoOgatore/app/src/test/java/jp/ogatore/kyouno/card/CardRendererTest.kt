package jp.ogatore.kyouno.card

import android.graphics.Bitmap
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// ネイティブ移植 Step 4(マスタープラン§6 Step4検収基準4)→Step4/7bパリティ突合タスク
// (TASK-C2-2026-07-26-native-migration-card-visual-assets.md): 「同一日付での再描画が同一出力」を
// ピクセル配列(ARGB IntArray)比較で確認する。CardRendererは現在時刻・乱数を一切読まない設計
// (ForbiddenAPIRegressionTest参照)なので、同じ入力を2回描画すればピクセル単位で一致するはず。
// android.graphics.Canvas/BitmapはプレーンJVMでは動かないためRobolectricでシャドウ実行する。
//
// @GraphicsMode(NATIVE)必須(パリティ突合タスクで判明): 明示しないとRobolectric既定のLEGACYシャドウ
// (実ラスタライズなし)で動き、getPixels()が実際の描画内容を反映しない空データを返すことがある
// (drawRoundRect/LinearGradient/Bitmap描画はLEGACYでは追跡されず、Path系の一部操作だけ偶然反映される
// ため、旧2テストは「たまたま」通っていた。実測: debugRawCanvasDrawで単純なdrawRectの赤塗りすら
// LEGACYでは読み戻せないことを確認した上でNATIVEへ変更・全テスト green化を確認済み)。
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
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

    // F1(TASK-C2-2026-07-29-inspection-upgrade.md): E1(koka/ashi/robotのアイコン欠落)の再発防止。
    // 既存のsameInputWithRealAssetsProducesIdenticalPixelsTwiceはmomo 1キーしか試しておらず、
    // TYPE_IMGが3体のままでも6体でも通ってしまっていた(alan5の指摘どおり)。ここでは6キー全部を
    // ループし、「アイコンありの描画結果とアイコンなしの描画結果が異なること」を直接確認する。
    // @GraphicsMode(NATIVE)+実contextにより、iOSと違いスタブ無しでres/drawable-nodpi/の実画像を
    // そのまま検査できる(TYPE_IMGから1体でも欠けると、そのタイプだけ両者が一致して赤くなる)。
    @Test
    fun allSixTypeIconsAreActuallyDrawnIntoTheCard() {
        val data = CardDataLoader.shared
        val ds = "2026-07-25"
        val di = CardLottery.dateIdx(ds)
        val context = RuntimeEnvironment.getApplication()
        val noIcon = pixels(
            CardRenderer.render(
                ds, 12, sampleTheme(), false, null, di, data.CARD_THEMES_V2_FROM,
                context = context, typeName = "テストタイプ", typeIconKey = null,
            ),
        )
        for (key in listOf("momo", "kenko", "yawara", "koka", "ashi", "robot")) {
            val withIcon = pixels(
                CardRenderer.render(
                    ds, 12, sampleTheme(), false, null, di, data.CARD_THEMES_V2_FROM,
                    context = context, typeName = "テストタイプ", typeIconKey = key,
                ),
            )
            assertFalse(
                "typeIconKey=${key} のときアイコンが描画結果に反映されていない(TYPE_IMGに${key}が無い疑い)",
                withIcon.contentEquals(noIcon),
            )
        }
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
