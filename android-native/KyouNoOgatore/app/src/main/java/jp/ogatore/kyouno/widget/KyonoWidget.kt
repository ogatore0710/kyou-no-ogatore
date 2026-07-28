package jp.ogatore.kyouno.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import jp.ogatore.kyouno.MainActivity
import jp.ogatore.kyouno.R
import jp.ogatore.kyouno.record.RecordStore
import java.io.File
import java.time.Instant

// GO-H1(ホーム画面ウィジェット・Duolingo式・本人GO 2026-07-28): Phase1=表示専用。
// タップでアプリを開くだけ(actionStartActivity)・RecordStoreへは一切書き込まない(読むだけ)。
// 同一パッケージ内のGlance AppWidgetなので、App Group相当の共有コンテナは不要
// (Context.filesDirを直読みできる=iOS版のようなミラーJSONは不要)。
private val WidgetBg = Color(0xFFFFFAF3)
private val WidgetCard = Color(0xFFFFFFFF)
private val WidgetInk = Color(0xFF3A3A35)
private val WidgetSub = Color(0xFF6E6B5F)
private val WidgetTeal = Color(0xFF2BB3A3)
private val WidgetTealStrong = Color(0xFF1E7B70)
private val WidgetFreeze = Color(0xFFF2D98B)
private val WidgetNoneDot = Color(0xFFE8E2D2)
private val WidgetLine = Color(0xFFF2EADB)

private val Small = DpSize(110.dp, 110.dp)
private val Medium = DpSize(250.dp, 110.dp)

class KyonoWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(Small, Medium))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // GO-H1§1: RecordStoreには1バイトも書き込まない(forFileは既存の読み書き両対応APIだが、
        // このウィジェットからはget/loadStreak等の読み取り専用の呼び出ししか行わない)。
        val store = RecordStore.forFile(File(context.filesDir, "kyono-store.json"))
        val now = Instant.now()
        val state = WidgetLogic.compute(store, now, justRecorded = WidgetUpdater.wasRecordedToday(context, now))

        provideContent {
            val size = LocalSize.current
            val isWide = size.width >= 180.dp
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(WidgetBg)
                    .cornerRadius(20.dp)
                    .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
                contentAlignment = Alignment.Center,
            ) {
                if (isWide) WideWidgetContent(state) else SmallWidgetContent(state)
            }
        }
    }
}

@Composable
internal fun SmallWidgetContent(state: WidgetState) {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(modifier = GlanceModifier.height(64.dp), contentAlignment = Alignment.Center) {
            Image(
                provider = ImageProvider(charaResId(state.chara)),
                contentDescription = state.message,
                modifier = GlanceModifier.height(64.dp),
            )
        }
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            streakLabel(state),
            style = TextStyle(color = ColorProvider(WidgetInk), fontWeight = FontWeight.Bold, fontSize = 12.sp),
        )
    }
}

@Composable
internal fun WideWidgetContent(state: WidgetState) {
    Row(
        modifier = GlanceModifier.fillMaxSize().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(charaResId(state.chara)),
            contentDescription = state.message,
            modifier = GlanceModifier.height(72.dp).width(72.dp),
        )
        Spacer(modifier = GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                state.message,
                style = TextStyle(color = ColorProvider(WidgetInk), fontWeight = FontWeight.Bold, fontSize = 14.sp),
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                streakLabel(state),
                style = TextStyle(color = ColorProvider(WidgetSub), fontSize = 12.sp),
            )
            Spacer(modifier = GlanceModifier.height(6.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                state.last7.forEach { dot ->
                    Box(modifier = GlanceModifier.size(12.dp), contentAlignment = Alignment.Center) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_dot),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(ColorProvider(dotColor(dot))),
                            modifier = GlanceModifier.size(9.dp),
                        )
                    }
                    Spacer(modifier = GlanceModifier.width(2.dp))
                }
            }
        }
    }
}

// GO-H1§2-1: 表示は必ずeffectiveStreakCount経由(WidgetLogic.compute内で確定済み)。
// ここではその結果を文言に整形するだけ(生のcountを読み直すことは絶対にしない)。
private fun streakLabel(state: WidgetState): String =
    if (state.streakCount == 0) "また1日め" else "${state.streakCount}日つづいてる"

private fun dotColor(dot: DotState): Color = when (dot) {
    DotState.DONE -> WidgetTealStrong
    DotState.FREEZE -> WidgetFreeze
    DotState.NONE -> WidgetNoneDot
}

// 使用可6枚のみ(chara-2/chara-3/chara-hitokoto/charaの4枚は本人指示で使用禁止・参照もしない)。
private fun charaResId(chara: CharaAsset): Int = when (chara) {
    CharaAsset.CHEER -> R.drawable.chara_cheer
    CharaAsset.KAIKYAKU -> R.drawable.chara_kaikyaku
    CharaAsset.CONGRATS -> R.drawable.chara_congrats
    CharaAsset.GOOD -> R.drawable.chara_good
    CharaAsset.CROWN -> R.drawable.chara_crown
    CharaAsset.CRACKER -> R.drawable.chara_cracker
}
