package jp.ogatore.kyouno.widget

import jp.ogatore.kyouno.record.RecordLogic
import jp.ogatore.kyouno.record.RecordStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

// GO-H1(ホーム画面ウィジェット)検収基準: 「1週間休んで券でも埋まらない状態で、ウィジェットの
// 連続日数が0→「きょうから また1日め🌱」表示になる」ことをユニットテストで固定する
// (effectiveStreakCount経由であることの証明。生のstreak.countを読んでいたら、この
// テストは古い連続日数のまま失敗する)。
class WidgetLogicTest {
    private val zone = ZoneOffset.UTC

    @Test
    fun streakBrokenBeyondFreezeShowsZeroDayRestartCopy() {
        val store = RecordStore.inMemory()
        // 1週間以上前の1日だけ記録し、以降まったく記録が無い状態(freeze2も未使用)を作る。
        RecordLogic.markDone(store, Instant.parse("2026-07-01T09:00:00Z"), zone)
        val now = Instant.parse("2026-07-20T09:00:00Z")

        val state = WidgetLogic.compute(store, now, zone)

        assertEquals(0, state.streakCount)
        assertEquals("きょうから また1日め🌱", state.message)
        assertEquals(CharaAsset.CHEER, state.chara)
    }

    @Test
    fun activeStreakUsesEffectiveCountNotRawCount() {
        val store = RecordStore.inMemory()
        var t = Instant.parse("2026-07-01T09:00:00Z")
        repeat(5) {
            RecordLogic.markDone(store, t, zone)
            t = t.plusSeconds(86400)
        }
        val state = WidgetLogic.compute(store, t.minusSeconds(3600), zone)
        assertEquals(5, state.streakCount)
    }

    @Test
    fun doneTodayMorningVsEveningMessageDiffersOnlyWhenNotDone() {
        // 「連続0」状態はどの時間帯でもcheer/また1日めが優先されるため(発注書§2-4「連続0からの
        // 再開初日」行)、朝/夕の絵の切り替えを見るには連続が生きている状態(=きのう済み・きょう未)
        // を作ってから検証する。
        val store = RecordStore.inMemory()
        RecordLogic.markDone(store, Instant.parse("2026-07-19T09:00:00Z"), zone)
        val morning = Instant.parse("2026-07-20T09:00:00Z") // 9時UTC = 朝〜昼側(5-17時)
        val evening = Instant.parse("2026-07-20T20:00:00Z") // 20時UTC = 夕夜側

        val morningState = WidgetLogic.compute(store, morning, zone)
        val eveningState = WidgetLogic.compute(store, evening, zone)

        assertEquals(CharaAsset.CHEER, morningState.chara)
        assertEquals("きょうもいこう！💪", morningState.message)
        assertEquals(CharaAsset.KAIKYAKU, eveningState.chara)
        assertEquals("ねる前に1本 どう？🌙", eveningState.message)
    }

    @Test
    fun justRecordedShowsCongratsOtherwiseGood() {
        val store = RecordStore.inMemory()
        val now = Instant.parse("2026-07-20T09:00:00Z")
        RecordLogic.markDone(store, now, zone)

        val justRecorded = WidgetLogic.compute(store, now, zone, justRecorded = true)
        val laterSameDay = WidgetLogic.compute(store, now.plusSeconds(3600), zone, justRecorded = false)

        assertEquals(CharaAsset.CONGRATS, justRecorded.chara)
        assertEquals(CharaAsset.GOOD, laterSameDay.chara)
        assertTrue(justRecorded.doneToday && laterSameDay.doneToday)
    }

    @Test
    fun last7NoRedOrCrossStatesOnlyThreeKinds() {
        val doneDates = setOf("2026-07-18", "2026-07-20")
        val sorted = doneDates.sorted()
        val dots = WidgetLogic.buildLast7(doneDates, sorted, "2026-07-20")

        assertEquals(7, dots.size)
        // 7/19はdoneDatesに無いが、前後(7/18・7/20)にやった日があるのでFREEZE扱いになる。
        val julyNineteenIndex = 5 // today=7/20がindex6、7/19がindex5
        assertEquals(DotState.FREEZE, dots[julyNineteenIndex])
        assertEquals(DotState.DONE, dots[6])
        assertTrue(dots.all { it == DotState.DONE || it == DotState.FREEZE || it == DotState.NONE })
    }
}
