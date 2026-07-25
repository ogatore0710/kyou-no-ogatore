import Foundation

// ネイティブ移植 Step 5b(マスタープラン§6 Step 5b): マイ記録カレンダー(index.html:renderCal・
// 2905行台)の日付計算部分の1:1移植。JSの`new Date(y,m,1).getDay()`(ローカル暦・0=日曜始まり)と
// `new Date(y,m+1,0).getDate()`(その月の日数)を、デバイスのローカルカレンダーで再現する。
// done/today/mute等のマス目スタイリングはUI層の責務(streak.datesを見て判定するだけの純粋表示ロジックで
// あり、決定的アルゴリズムではないため、ここには含めない)。
//
// LazyVerticalGridはverticalScroll内に入れない(masterplan §1-4禁じ手・無限高さ制約クラッシュ)ため、
// カレンダーUIはColumn+Row(最大6週間ぶん)で組む。1ヶ月最大42マス(先頭の空白セル+日数)。
public enum CalendarLogic {
    // 月初(1日)の曜日。0=日曜, 1=月曜, ..., 6=土曜(JSのDate.getDay()と同じ0始まり)。
    public static func firstWeekday(year: Int, month: Int, calendar: Calendar = .current) -> Int {
        var c = DateComponents()
        c.year = year; c.month = month; c.day = 1
        let date = calendar.date(from: c)!
        // Calendar.component(.weekday:) は1=日曜始まり(iOS標準)なので-1するとJSのgetDay()と一致する。
        return calendar.component(.weekday, from: date) - 1
    }

    // その月の日数(28〜31)。
    public static func daysInMonth(year: Int, month: Int, calendar: Calendar = .current) -> Int {
        var c = DateComponents()
        c.year = year; c.month = month; c.day = 1
        let date = calendar.date(from: c)!
        return calendar.range(of: .day, in: .month, for: date)!.count
    }

    // "YYYY-MM-DD"形式の日付文字列を組み立てる(index.htmlのテンプレートリテラルと同じゼロ埋め)。
    public static func dateString(year: Int, month: Int, day: Int) -> String {
        String(format: "%04d-%02d-%02d", year, month, day)
    }
}
