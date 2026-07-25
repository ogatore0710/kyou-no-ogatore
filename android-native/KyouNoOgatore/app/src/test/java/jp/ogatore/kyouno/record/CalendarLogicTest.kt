package jp.ogatore.kyouno.record

import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarLogicTest {
    // 2026-07-01は水曜日(JSのgetDay()=3)
    @Test
    fun firstWeekdayJuly2026() {
        assertEquals(3, CalendarLogic.firstWeekday(2026, 7))
        assertEquals(31, CalendarLogic.daysInMonth(2026, 7))
    }

    @Test
    fun february2026NotLeap() {
        assertEquals(28, CalendarLogic.daysInMonth(2026, 2))
    }

    @Test
    fun february2028Leap() {
        assertEquals(29, CalendarLogic.daysInMonth(2028, 2))
    }

    // 2026-06-01は月曜日(JSのgetDay()=1)
    @Test
    fun firstWeekdayJune2026() {
        assertEquals(1, CalendarLogic.firstWeekday(2026, 6))
    }

    @Test
    fun dateStringFormatting() {
        assertEquals("2026-07-05", CalendarLogic.dateString(2026, 7, 5))
        assertEquals("2026-12-25", CalendarLogic.dateString(2026, 12, 25))
    }

    // 42マス突合(masterplan §6 Step5b検収基準1): 先頭空白+日数がカレンダーグリッドの総マス数になる。
    // 2026年8月は土曜始まり(先頭空白6)+31日=37マス(6週にまたがる)
    @Test
    fun totalCellsForAugust2026() {
        val leading = CalendarLogic.firstWeekday(2026, 8)
        val days = CalendarLogic.daysInMonth(2026, 8)
        assertEquals(6, leading)
        assertEquals(31, days)
        assertEquals(37, leading + days)
    }
}
