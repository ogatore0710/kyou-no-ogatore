package jp.ogatore.kyouno.widget

import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.testing.runGlanceAppWidgetUnitTest
import androidx.glance.appwidget.testing.unit.hasText
import androidx.glance.testing.unit.hasTestTag
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith

// GO-H1(ホーム画面ウィジェット)・alan5指示の手段C: WidgetLogicTestが「どの状態が選ばれるか」を
// 検証するのに対し、こちらは「その状態(WidgetState)を実際にGlanceへ描画すると何が表示されるか」を
// JVM単体テストで検証する(実機配置スクショの代替エビデンス)。ファイルI/Oを避けるため、
// KyonoWidget.provideGlanceは経由せず、WideWidgetContent/SmallWidgetContentへ直接WidgetStateを
// 渡すテスト専用のGlanceAppWidgetでラップする。
private class TestableWidget(private val state: WidgetState, private val wide: Boolean) : GlanceAppWidget() {
    override suspend fun provideGlance(context: android.content.Context, id: androidx.glance.GlanceId) {
        provideContent {
            Box(modifier = GlanceModifier.fillMaxSize().semantics { testTag = "root" }, contentAlignment = Alignment.Center) {
                if (wide) WideWidgetContent(state) else SmallWidgetContent(state)
            }
        }
    }
}

@RunWith(RobolectricTestRunner::class)
class KyonoWidgetRenderTest {
    @Test
    fun zeroStreakRendersRestartCopyNotZeroNichi() = runGlanceAppWidgetUnitTest {
        val state = WidgetState(
            doneToday = false, streakCount = 0,
            last7 = List(7) { DotState.NONE }, chara = CharaAsset.CHEER,
            message = "きょうから また1日め🌱",
        )
        provideComposable { WideWidgetContent(state) }

        // GO-H1§2-2検収基準: 「0日」という数字表記を絶対に出さない・「また1日め」文言が出ること。
        onNode(hasText("きょうから また1日め🌱")).assertExists()
        onNode(hasText("0日つづいてる")).assertDoesNotExist()
    }

    @Test
    fun activeStreakRendersDaysLabel() = runGlanceAppWidgetUnitTest {
        val state = WidgetState(
            doneToday = false, streakCount = 5,
            last7 = List(7) { DotState.NONE }, chara = CharaAsset.CHEER,
            message = "きょうもいこう！💪",
        )
        provideComposable { WideWidgetContent(state) }

        onNode(hasText("5日つづいてる")).assertExists()
        onNode(hasText("きょうもいこう！💪")).assertExists()
    }

    @Test
    fun justRecordedShowsCongratsMessage() = runGlanceAppWidgetUnitTest {
        val state = WidgetState(
            doneToday = true, streakCount = 6,
            last7 = List(7) { DotState.NONE }, chara = CharaAsset.CONGRATS,
            message = "きょうもおつかれさま！",
        )
        provideComposable { WideWidgetContent(state) }

        onNode(hasText("きょうもおつかれさま！")).assertExists()
        onNode(hasText("6日つづいてる")).assertExists()
    }
}
