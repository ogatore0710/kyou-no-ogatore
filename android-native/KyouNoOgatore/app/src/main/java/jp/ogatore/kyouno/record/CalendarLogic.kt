package jp.ogatore.kyouno.record

import java.time.LocalDate
import java.time.YearMonth

// ネイティブ移植 Step 5b(マスタープラン§6 Step 5b): マイ記録カレンダー(index.html:renderCal・
// 2905行台)の日付計算部分の1:1移植。JSの`new Date(y,m,1).getDay()`(ローカル暦・0=日曜始まり)と
// `new Date(y,m+1,0).getDate()`(その月の日数)を、java.time(デバイスのローカルカレンダー)で再現する。
// done/today/mute等のマス目スタイリングはUI層の責務。
//
// LazyVerticalGridはverticalScroll内に入れない(masterplan §1-4禁じ手・無限高さ制約クラッシュ)ため、
// カレンダーUIはColumn+Row(最大6週間ぶん)で組む。1ヶ月最大42マス(先頭の空白セル+日数)。
object CalendarLogic {
    // 月初(1日)の曜日。0=日曜, 1=月曜, ..., 6=土曜(JSのDate.getDay()と同じ0始まり)。
    // java.time.DayOfWeek.value は1=月曜..7=日曜なので、%7でJS方式(0=日曜)に変換する。
    fun firstWeekday(year: Int, month: Int): Int {
        return LocalDate.of(year, month, 1).dayOfWeek.value % 7
    }

    // その月の日数(28〜31)。
    fun daysInMonth(year: Int, month: Int): Int = YearMonth.of(year, month).lengthOfMonth()

    // "YYYY-MM-DD"形式の日付文字列を組み立てる(index.htmlのテンプレートリテラルと同じゼロ埋め)。
    fun dateString(year: Int, month: Int, day: Int): String = "%04d-%02d-%02d".format(year, month, day)
}
