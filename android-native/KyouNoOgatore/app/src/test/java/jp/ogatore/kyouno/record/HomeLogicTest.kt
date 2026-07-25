package jp.ogatore.kyouno.record

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class HomeLogicTest {
    private val jst = ZoneId.of("Asia/Tokyo")

    private fun instant(y: Int, mo: Int, d: Int, hh: Int, mm: Int) =
        LocalDateTime.of(y, mo, d, hh, mm, 0).atZone(jst).toInstant()

    // ---- fdFocusHome: ガイド開始日当日のみ発火(§6 Step5a検収基準2・最重要の壊れやすい箇所) ----
    @Test
    fun fdFocusHomeActiveOnlyOnGuideStartDay() {
        assertTrue(HomeLogic.fdFocusHomeActive("go", 0, "2026-07-25", "2026-07-25"))
    }

    @Test
    fun fdFocusHomeInactiveOnFollowingDay() {
        // 翌日以降に記録せず戻ってきた人には通常ホームを見せる(HANDOVER第7項の再発防止)
        assertFalse(HomeLogic.fdFocusHomeActive("go", 0, "2026-07-25", "2026-07-26"))
    }

    @Test
    fun fdActiveRequiresGoAndZeroTotal() {
        assertTrue(HomeLogic.fdActive("go", 0))
        assertFalse("1件でも記録があればガイド対象外", HomeLogic.fdActive("go", 1))
        assertFalse("fd=1(完了済み。JS版は数値1・ネイティブは文字列表現)はガイド対象外", HomeLogic.fdActive("1", 0))
        assertFalse(HomeLogic.fdActive(null, 0))
    }

    @Test
    fun fdFocusHomeInactiveWhenNotGuiding() {
        assertFalse(HomeLogic.fdFocusHomeActive(null, 0, "2026-07-25", "2026-07-25"))
        assertFalse(HomeLogic.fdFocusHomeActive("go", 3, "2026-07-25", "2026-07-25"))
    }

    // ---- refreshDay: 深夜3時境界をまたいだ復帰でtoday/dayChangedが更新される(§6 Step5a検収基準3) ----
    @Test
    fun refreshDayDetectsMidnightThreeBoundaryCrossing() {
        val r1 = HomeLogic.refreshDay(instant(2026, 7, 25, 2, 59), "2026-07-24", jst)
        assertEquals(RefreshDayResult(false, "2026-07-24"), r1)

        val r2 = HomeLogic.refreshDay(instant(2026, 7, 25, 3, 0), "2026-07-24", jst)
        assertEquals(RefreshDayResult(true, "2026-07-25"), r2)

        val r3 = HomeLogic.refreshDay(instant(2026, 7, 25, 3, 1), "2026-07-24", jst)
        assertEquals(RefreshDayResult(true, "2026-07-25"), r3)
    }

    @Test
    fun refreshDayNoChangeWithinSameDay() {
        val r = HomeLogic.refreshDay(instant(2026, 7, 25, 20, 0), "2026-07-25", jst)
        assertEquals(RefreshDayResult(false, "2026-07-25"), r)
    }

    // ---- checkDoneNudge: 動画タップ→復帰の「やった?」ナッジ(pendingNudge) ----
    @Test
    fun shouldShowDoneNudgeWhenPendingAndNotYetRecorded() {
        assertTrue(HomeLogic.shouldShowDoneNudge("2026-07-25", "2026-07-25", listOf("2026-07-24")))
    }

    @Test
    fun shouldNotShowDoneNudgeWhenNoPending() {
        assertFalse(HomeLogic.shouldShowDoneNudge(null, "2026-07-25", emptyList()))
    }

    @Test
    fun shouldNotShowDoneNudgeAcrossDayBoundary() {
        assertFalse(HomeLogic.shouldShowDoneNudge("2026-07-24", "2026-07-25", emptyList()))
    }

    @Test
    fun shouldNotShowDoneNudgeWhenAlreadyRecordedToday() {
        assertFalse(HomeLogic.shouldShowDoneNudge("2026-07-25", "2026-07-25", listOf("2026-07-25")))
    }
}
