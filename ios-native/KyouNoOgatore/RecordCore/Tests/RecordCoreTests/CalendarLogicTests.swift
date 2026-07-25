import XCTest
@testable import RecordCore

final class CalendarLogicTests: XCTestCase {
    private var jstCal: Calendar {
        var c = Calendar(identifier: .gregorian)
        c.timeZone = TimeZone(identifier: "Asia/Tokyo")!
        return c
    }

    // 2026-07-01は水曜日(Python: datetime.date(2026,7,1).weekday()==2=水, JSのgetDay()=3)
    func testFirstWeekdayJuly2026() {
        XCTAssertEqual(CalendarLogic.firstWeekday(year: 2026, month: 7, calendar: jstCal), 3)
        XCTAssertEqual(CalendarLogic.daysInMonth(year: 2026, month: 7, calendar: jstCal), 31)
    }

    // 2026年2月(平年)は28日
    func testFebruary2026NotLeap() {
        XCTAssertEqual(CalendarLogic.daysInMonth(year: 2026, month: 2, calendar: jstCal), 28)
    }

    // 2028年2月(閏年)は29日
    func testFebruary2028Leap() {
        XCTAssertEqual(CalendarLogic.daysInMonth(year: 2028, month: 2, calendar: jstCal), 29)
    }

    // 2026-06-01は月曜日(JSのgetDay()=1)
    func testFirstWeekdayJune2026() {
        XCTAssertEqual(CalendarLogic.firstWeekday(year: 2026, month: 6, calendar: jstCal), 1)
    }

    func testDateStringFormatting() {
        XCTAssertEqual(CalendarLogic.dateString(year: 2026, month: 7, day: 5), "2026-07-05")
        XCTAssertEqual(CalendarLogic.dateString(year: 2026, month: 12, day: 25), "2026-12-25")
    }

    // 42マス突合(masterplan §6 Step5b検収基準1): 先頭空白+日数がカレンダーグリッドの総マス数になる。
    // 2026年8月は土曜始まり(先頭空白6)+31日=37マス(6週にまたがる)
    func testTotalCellsForAugust2026() {
        let leading = CalendarLogic.firstWeekday(year: 2026, month: 8, calendar: jstCal)
        let days = CalendarLogic.daysInMonth(year: 2026, month: 8, calendar: jstCal)
        XCTAssertEqual(leading, 6) // 2026-08-01は土曜
        XCTAssertEqual(days, 31)
        XCTAssertEqual(leading + days, 37)
    }
}
