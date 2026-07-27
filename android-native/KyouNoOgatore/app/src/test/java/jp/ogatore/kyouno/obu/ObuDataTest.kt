package jp.ogatore.kyouno.obu

import org.junit.Assert.assertEquals
import org.junit.Test

// TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §5: index.html:1314-1318 obuFmtDate()の
// 1:1移植確認(ゼロ埋め無し・timeの有無で末尾"ごろ"の有無が変わる)。
class ObuDataTest {
    @Test
    fun formatsDateWithoutLeadingZeros() {
        assertEquals("7月9日", obuFmtDate("2026-07-09", null))
    }

    @Test
    fun formatsDoubleDigitMonthAndDayWithoutTruncation() {
        assertEquals("12月25日", obuFmtDate("2026-12-25", null))
    }

    @Test
    fun appendsTimeWithGoroSuffixWhenTimePresent() {
        assertEquals("7月9日 12:30ごろ", obuFmtDate("2026-07-09", "12:30"))
    }
}
