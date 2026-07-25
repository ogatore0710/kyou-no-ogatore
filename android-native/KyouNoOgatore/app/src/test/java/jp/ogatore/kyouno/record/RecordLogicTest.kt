package jp.ogatore.kyouno.record

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class RecordLogicTest {
    private val jst = ZoneId.of("Asia/Tokyo")

    private fun instant(y: Int, mo: Int, d: Int, hh: Int, mm: Int) =
        LocalDateTime.of(y, mo, d, hh, mm, 0).atZone(jst).toInstant()

    // ---- todayStr 深夜3時境界(§6 Step3検収基準・2:59/3:00/3:01の3点) ----
    @Test
    fun todayStrMidnightThreeBoundary() {
        assertEquals("2026-07-24", RecordLogic.todayStr(instant(2026, 7, 25, 2, 59), jst))
        assertEquals("2026-07-25", RecordLogic.todayStr(instant(2026, 7, 25, 3, 0), jst))
        assertEquals("2026-07-25", RecordLogic.todayStr(instant(2026, 7, 25, 3, 1), jst))
    }

    @Test
    fun daysBetweenIsTimezoneIndependentCalendarDays() {
        assertEquals(5, RecordLogic.daysBetween("2026-07-20", "2026-07-25"))
        assertEquals(-5, RecordLogic.daysBetween("2026-07-25", "2026-07-20"))
        assertEquals(1, RecordLogic.daysBetween("2026-06-30", "2026-07-01"))
        assertEquals(1, RecordLogic.daysBetween("2025-12-31", "2026-01-01"))
    }

    // ---- markDone: streak2の増分・ギャップ・おやすみ券橋渡し・新章 ----
    @Test
    fun markDoneFirstEver() {
        val store = RecordStore.inMemory()
        val r = RecordLogic.markDone(store, instant(2026, 7, 25, 10, 0), jst)
        assertFalse(r.alreadyDone)
        assertEquals(RecordLogic.StreakData(listOf("2026-07-25"), 1, 1), r.streak)
        assertNull(r.gap)
    }

    @Test
    fun markDoneConsecutiveDayIncrementsCount() {
        val seed = mapOf("kyono_streak2" to """{"dates":["2026-07-24"],"count":3,"total":3}""")
        val store = RecordStore.inMemory(seed)
        val r = RecordLogic.markDone(store, instant(2026, 7, 25, 10, 0), jst)
        assertEquals(RecordLogic.StreakData(listOf("2026-07-24", "2026-07-25"), 4, 4), r.streak)
        assertEquals(1, r.gap)
    }

    @Test
    fun markDoneAlreadyDoneTodayIsNoop() {
        val seed = mapOf("kyono_streak2" to """{"dates":["2026-07-25"],"count":2,"total":2}""")
        val store = RecordStore.inMemory(seed)
        val r = RecordLogic.markDone(store, instant(2026, 7, 25, 20, 0), jst)
        assertTrue(r.alreadyDone)
        assertEquals(2, r.streak.count)
    }

    @Test
    fun markDoneGapBridgedByFreezeKeepsCount() {
        // 前回記録が2日前(2026-07-23)→ 1日(2026-07-24)だけ欠席。券残数(月3枚)の範囲内なので橋渡しされる
        val seed = mapOf("kyono_streak2" to """{"dates":["2026-07-23"],"count":5,"total":5}""")
        val store = RecordStore.inMemory(seed)
        val r = RecordLogic.markDone(store, instant(2026, 7, 25, 10, 0), jst)
        assertFalse(r.newChapter)
        assertEquals(1, r.usedFreezeCount)
        assertEquals(6, r.streak.count)
        assertEquals(6, r.streak.total)
        assertEquals(1, RecordLogic.freezeMap(store)["2026-07"])
    }

    @Test
    fun markDoneGapTooLargeStartsNewChapter() {
        // 券予算(月3枚)を使い切った状態で10日ギャップ→橋渡し不可→新章
        val seed = mapOf(
            "kyono_streak2" to """{"dates":["2026-07-15"],"count":20,"total":20}""",
            "kyono_freeze2" to """{"2026-07":3}""",
            "kyono_chapters" to "1",
        )
        val store = RecordStore.inMemory(seed)
        val r = RecordLogic.markDone(store, instant(2026, 7, 25, 10, 0), jst)
        assertTrue(r.newChapter)
        assertEquals(1, r.streak.count)
        assertEquals(21, r.streak.total)
        assertEquals(2, r.chapters)
    }

    @Test
    fun streakMigratesFromOldFormat() {
        val seed = mapOf("kyono_streak" to """{"last":"2026-07-20","count":4,"total":9}""")
        val store = RecordStore.inMemory(seed)
        val st = RecordLogic.loadStreak(store)
        assertEquals(RecordLogic.StreakData(listOf("2026-07-20"), 4, 9), st)
    }

    @Test
    fun streakDefensiveCoercionKeepsGoodFieldsOnPartialCorruption() {
        // datesだけ型が壊れていても(数値5)、count/totalは維持される(Web版の「形の防御」を踏襲)
        val seed = mapOf("kyono_streak2" to """{"dates":5,"count":3,"total":10}""")
        val store = RecordStore.inMemory(seed)
        val st = RecordLogic.loadStreak(store)
        assertEquals(RecordLogic.StreakData(emptyList(), 3, 10), st)
    }

    // ---- freeze2: 旧freezeからの移行・月次上限 ----
    @Test
    fun freezeMapMigratesFromOldSingularFreeze() {
        val seed = mapOf("kyono_freeze" to """{"m":"2026-06","used":2}""")
        val store = RecordStore.inMemory(seed)
        assertEquals(mapOf("2026-06" to 2), RecordLogic.freezeMap(store))
    }

    @Test
    fun freezeLeftReflectsUsedBudget() {
        val seed = mapOf("kyono_freeze2" to """{"2026-07":2}""")
        val store = RecordStore.inMemory(seed)
        assertEquals(1, RecordLogic.freezeLeft(store, instant(2026, 7, 25, 10, 0), jst))
    }

    @Test
    fun canBridgeFreezesRespectsPerMonthCap() {
        val seed = mapOf("kyono_freeze2" to """{"2026-07":3}""") // 今月はもう0枚
        val store = RecordStore.inMemory(seed)
        assertFalse(RecordLogic.canBridgeFreezes(store, listOf("2026-07-24")))
    }

    // ---- daylog: 400件トリム ----
    @Test
    fun daylogTrimsTo400() {
        val seed = (1..400).associate { i ->
            "%04d-01-01".format(2000 + i) to RecordLogic.DaylogEntry("x", "t", i)
        }
        val store = RecordStore.inMemory()
        store.set("daylog", seed)
        RecordLogic.recordDaylog(store, "2027-01-01", "newid", "new", 1)
        val dl = RecordLogic.loadDaylog(store)
        assertEquals(400, dl.size)
        assertTrue(dl.containsKey("2027-01-01"))
    }

    // ---- memos: 保存・空で削除 ----
    @Test
    fun saveMemoStoresAndClearsOnEmpty() {
        val store = RecordStore.inMemory()
        RecordLogic.saveMemo(store, "2026-07-25", "からだが軽くなった")
        assertEquals("からだが軽くなった", RecordLogic.loadMemos(store)["2026-07-25"])
        RecordLogic.saveMemo(store, "2026-07-25", "")
        assertNull(RecordLogic.loadMemos(store)["2026-07-25"])
    }

    // ---- reach: 200件トリム時に自己ベストを保護 ----
    @Test
    fun setReachProtectsBestOnTrim() {
        val arr = mutableListOf(RecordLogic.ReachEntry("2020-01-01", 5)) // 自己ベスト(トリムで消える位置)
        for (i in 1..200) arr.add(RecordLogic.ReachEntry("%04d-01-01".format(2100 + i), 1))
        val store = RecordStore.inMemory()
        store.set("reach", arr)
        RecordLogic.setReach(store, 2, instant(2026, 7, 25, 10, 0), jst) // 202件目→トリム発生
        val after = RecordLogic.getReach(store)
        // index.html: splice(0,len-200)で200件に切ってから、ベストが残っていなければunshiftで戻す
        // ため、ベスト保護が発動した回だけ意図的に201件になる(JS版そのままの仕様。バグではない)。
        assertEquals(201, after.size)
        assertTrue("自己ベスト(lv5)がトリムで失われてはいけない", after.any { it.lv == 5 })
    }
}
