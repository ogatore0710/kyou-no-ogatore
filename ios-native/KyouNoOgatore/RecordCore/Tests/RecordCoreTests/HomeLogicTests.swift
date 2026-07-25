import XCTest
@testable import RecordCore

final class HomeLogicTests: XCTestCase {
    private let jst = TimeZone(identifier: "Asia/Tokyo")!

    private func date(_ y: Int, _ m: Int, _ d: Int, _ hh: Int, _ mm: Int) -> Date {
        var c = DateComponents()
        c.year = y; c.month = m; c.day = d; c.hour = hh; c.minute = mm; c.second = 0
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = jst
        return cal.date(from: c)!
    }

    // ---- fdFocusHome: ガイド開始日当日のみ発火(§6 Step5a検収基準2・最重要の壊れやすい箇所) ----
    func testFdFocusHomeActiveOnlyOnGuideStartDay() {
        XCTAssertTrue(HomeLogic.fdFocusHomeActive(fd: "go", streakTotal: 0, fdday: "2026-07-25", today: "2026-07-25"))
    }

    func testFdFocusHomeInactiveOnFollowingDay() {
        // 翌日以降に記録せず戻ってきた人には通常ホームを見せる(HANDOVER第7項の再発防止)
        XCTAssertFalse(HomeLogic.fdFocusHomeActive(fd: "go", streakTotal: 0, fdday: "2026-07-25", today: "2026-07-26"))
    }

    func testFdActiveRequiresGoAndZeroTotal() {
        XCTAssertTrue(HomeLogic.fdActive(fd: "go", streakTotal: 0))
        XCTAssertFalse(HomeLogic.fdActive(fd: "go", streakTotal: 1), "1件でも記録があればガイド対象外")
        XCTAssertFalse(HomeLogic.fdActive(fd: "1", streakTotal: 0), "fd=1(完了済み。JS版は数値1・ネイティブは文字列表現)はガイド対象外")
        XCTAssertFalse(HomeLogic.fdActive(fd: nil, streakTotal: 0))
    }

    func testFdFocusHomeInactiveWhenNotGuiding() {
        XCTAssertFalse(HomeLogic.fdFocusHomeActive(fd: nil, streakTotal: 0, fdday: "2026-07-25", today: "2026-07-25"))
        XCTAssertFalse(HomeLogic.fdFocusHomeActive(fd: "go", streakTotal: 3, fdday: "2026-07-25", today: "2026-07-25"))
    }

    // ---- refreshDay: 深夜3時境界をまたいだ復帰でtoday/dayChangedが更新される(§6 Step5a検収基準3) ----
    func testRefreshDayDetectsMidnightThreeBoundaryCrossing() {
        // 2:59 JST時点でlastDay="2026-07-24"のまま復帰→まだ日はまたいでいない
        let r1 = HomeLogic.refreshDay(now: date(2026, 7, 25, 2, 59), lastDay: "2026-07-24", timeZone: jst)
        XCTAssertEqual(r1, .init(dayChanged: false, today: "2026-07-24"))

        // 3:00 JSTで復帰→日をまたいだと判定される
        let r2 = HomeLogic.refreshDay(now: date(2026, 7, 25, 3, 0), lastDay: "2026-07-24", timeZone: jst)
        XCTAssertEqual(r2, .init(dayChanged: true, today: "2026-07-25"))

        // 3:01 JSTでも同様
        let r3 = HomeLogic.refreshDay(now: date(2026, 7, 25, 3, 1), lastDay: "2026-07-24", timeZone: jst)
        XCTAssertEqual(r3, .init(dayChanged: true, today: "2026-07-25"))
    }

    func testRefreshDayNoChangeWithinSameDay() {
        let r = HomeLogic.refreshDay(now: date(2026, 7, 25, 20, 0), lastDay: "2026-07-25", timeZone: jst)
        XCTAssertEqual(r, .init(dayChanged: false, today: "2026-07-25"))
    }

    // ---- checkDoneNudge: 動画タップ→復帰の「やった?」ナッジ(pendingNudge) ----
    func testShouldShowDoneNudgeWhenPendingAndNotYetRecorded() {
        XCTAssertTrue(HomeLogic.shouldShowDoneNudge(pendingNudgeDate: "2026-07-25", today: "2026-07-25", streakDates: ["2026-07-24"]))
    }

    func testShouldNotShowDoneNudgeWhenNoPending() {
        XCTAssertFalse(HomeLogic.shouldShowDoneNudge(pendingNudgeDate: nil, today: "2026-07-25", streakDates: []))
    }

    func testShouldNotShowDoneNudgeAcrossDayBoundary() {
        // 日をまたいでいたら対象外(前日タップぶんを翌日に出さない)
        XCTAssertFalse(HomeLogic.shouldShowDoneNudge(pendingNudgeDate: "2026-07-24", today: "2026-07-25", streakDates: []))
    }

    func testShouldNotShowDoneNudgeWhenAlreadyRecordedToday() {
        XCTAssertFalse(HomeLogic.shouldShowDoneNudge(pendingNudgeDate: "2026-07-25", today: "2026-07-25", streakDates: ["2026-07-25"]))
    }
}
