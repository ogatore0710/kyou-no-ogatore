import XCTest
@testable import RecordCore

final class RecordLogicTests: XCTestCase {
    private let jst = TimeZone(identifier: "Asia/Tokyo")!

    private func date(_ y: Int, _ m: Int, _ d: Int, _ hh: Int, _ mm: Int) -> Date {
        var c = DateComponents()
        c.year = y; c.month = m; c.day = d; c.hour = hh; c.minute = mm; c.second = 0
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = jst
        return cal.date(from: c)!
    }

    // ---- todayStr 深夜3時境界(§6 Step3検収基準・2:59/3:00/3:01の3点) ----
    func testTodayStrMidnightThreeBoundary() {
        XCTAssertEqual(RecordLogic.todayStr(now: date(2026, 7, 25, 2, 59), timeZone: jst), "2026-07-24")
        XCTAssertEqual(RecordLogic.todayStr(now: date(2026, 7, 25, 3, 0), timeZone: jst), "2026-07-25")
        XCTAssertEqual(RecordLogic.todayStr(now: date(2026, 7, 25, 3, 1), timeZone: jst), "2026-07-25")
    }

    func testDaysBetweenIsTimezoneIndependentCalendarDays() {
        XCTAssertEqual(RecordLogic.daysBetween("2026-07-20", "2026-07-25"), 5)
        XCTAssertEqual(RecordLogic.daysBetween("2026-07-25", "2026-07-20"), -5)
        XCTAssertEqual(RecordLogic.daysBetween("2026-06-30", "2026-07-01"), 1) // 月境界
        XCTAssertEqual(RecordLogic.daysBetween("2025-12-31", "2026-01-01"), 1) // 年境界
    }

    // ---- markDone: streak2の増分・ギャップ・おやすみ券橋渡し・新章 ----
    func testMarkDoneFirstEver() {
        let store = RecordStore(inMemory: [:])
        let r = RecordLogic.markDone(store, now: date(2026, 7, 25, 10, 0), timeZone: jst)
        XCTAssertFalse(r.alreadyDone)
        XCTAssertEqual(r.streak, .init(dates: ["2026-07-25"], count: 1, total: 1))
        XCTAssertNil(r.gap)
    }

    func testMarkDoneConsecutiveDayIncrementsCount() {
        let seed = ["kyono_streak2": #"{"dates":["2026-07-24"],"count":3,"total":3}"#]
        let store = RecordStore(inMemory: seed)
        let r = RecordLogic.markDone(store, now: date(2026, 7, 25, 10, 0), timeZone: jst)
        XCTAssertEqual(r.streak, .init(dates: ["2026-07-24", "2026-07-25"], count: 4, total: 4))
        XCTAssertEqual(r.gap, 1)
    }

    func testMarkDoneAlreadyDoneTodayIsNoop() {
        let seed = ["kyono_streak2": #"{"dates":["2026-07-25"],"count":2,"total":2}"#]
        let store = RecordStore(inMemory: seed)
        let r = RecordLogic.markDone(store, now: date(2026, 7, 25, 20, 0), timeZone: jst)
        XCTAssertTrue(r.alreadyDone)
        XCTAssertEqual(r.streak.count, 2)
    }

    func testMarkDoneGapBridgedByFreezeKeepsCount() {
        // 前回記録が2日前(2026-07-23)→ 1日(2026-07-24)だけ欠席。券残数(月3枚)の範囲内なので橋渡しされる
        let seed = ["kyono_streak2": #"{"dates":["2026-07-23"],"count":5,"total":5}"#]
        let store = RecordStore(inMemory: seed)
        let r = RecordLogic.markDone(store, now: date(2026, 7, 25, 10, 0), timeZone: jst)
        XCTAssertFalse(r.newChapter)
        XCTAssertEqual(r.usedFreezeCount, 1)
        XCTAssertEqual(r.streak.count, 6) // 橋渡し成功: 連続は途切れず+1
        XCTAssertEqual(r.streak.total, 6)
        XCTAssertEqual(RecordLogic.freezeMap(store)["2026-07"], 1)
    }

    func testMarkDoneGapTooLargeStartsNewChapter() {
        // 券予算(月3枚)を使い切った状態で10日ギャップ→橋渡し不可→新章
        let seed = [
            "kyono_streak2": #"{"dates":["2026-07-15"],"count":20,"total":20}"#,
            "kyono_freeze2": #"{"2026-07":3}"#,
            "kyono_chapters": "1",
        ]
        let store = RecordStore(inMemory: seed)
        let r = RecordLogic.markDone(store, now: date(2026, 7, 25, 10, 0), timeZone: jst)
        XCTAssertTrue(r.newChapter)
        XCTAssertEqual(r.streak.count, 1) // 新章スタートでカウントは1に戻る
        XCTAssertEqual(r.streak.total, 21) // 通算は途切れず継続加算
        XCTAssertEqual(r.chapters, 2)
    }

    func testStreakMigratesFromOldFormat() {
        let seed = ["kyono_streak": #"{"last":"2026-07-20","count":4,"total":9}"#]
        let store = RecordStore(inMemory: seed)
        let st = RecordLogic.loadStreak(store)
        XCTAssertEqual(st, .init(dates: ["2026-07-20"], count: 4, total: 9))
    }

    func testStreakDefensiveCoercionKeepsGoodFieldsOnPartialCorruption() {
        // datesだけ型が壊れていても(数値5)、count/totalは維持される(Web版の「形の防御」を踏襲)
        let seed = ["kyono_streak2": #"{"dates":5,"count":3,"total":10}"#]
        let store = RecordStore(inMemory: seed)
        let st = RecordLogic.loadStreak(store)
        XCTAssertEqual(st, .init(dates: [], count: 3, total: 10))
    }

    // ---- freeze2: 旧freezeからの移行・月次上限 ----
    func testFreezeMapMigratesFromOldSingularFreeze() {
        let seed = ["kyono_freeze": #"{"m":"2026-06","used":2}"#]
        let store = RecordStore(inMemory: seed)
        XCTAssertEqual(RecordLogic.freezeMap(store), ["2026-06": 2])
    }

    func testFreezeLeftReflectsUsedBudget() {
        let seed = ["kyono_freeze2": #"{"2026-07":2}"#]
        let store = RecordStore(inMemory: seed)
        XCTAssertEqual(RecordLogic.freezeLeft(store, now: date(2026, 7, 25, 10, 0), timeZone: jst), 1)
    }

    func testCanBridgeFreezesRespectsPerMonthCap() {
        let seed = ["kyono_freeze2": #"{"2026-07":3}"#] // 今月はもう0枚
        let store = RecordStore(inMemory: seed)
        XCTAssertFalse(RecordLogic.canBridgeFreezes(store, missedDates: ["2026-07-24"]))
    }

    // ---- daylog: 400件トリム ----
    func testDaylogTrimsTo400() {
        var seed: [String: RecordLogic.DaylogEntry] = [:]
        for i in 1...400 {
            seed[String(format: "2026-01-%03d", i)] = .init(v: "x", t: "t", c: i)
        }
        let store = RecordStore(inMemory: [:])
        store.set("daylog", seed)
        RecordLogic.recordDaylog(store, today: "2027-01-01", videoId: "newid", videoTitle: "new", count: 1)
        let dl = RecordLogic.loadDaylog(store)
        XCTAssertEqual(dl.count, 400)
        XCTAssertNotNil(dl["2027-01-01"])
    }

    // ---- memos: 保存・空で削除・400件トリム ----
    func testSaveMemoStoresAndClearsOnEmpty() {
        let store = RecordStore(inMemory: [:])
        RecordLogic.saveMemo(store, today: "2026-07-25", text: "からだが軽くなった")
        XCTAssertEqual(RecordLogic.loadMemos(store)["2026-07-25"], "からだが軽くなった")
        RecordLogic.saveMemo(store, today: "2026-07-25", text: "")
        XCTAssertNil(RecordLogic.loadMemos(store)["2026-07-25"])
    }

    // ---- reach: 200件トリム時に自己ベストを保護 ----
    func testSetReachProtectsBestOnTrim() {
        var arr: [RecordLogic.ReachEntry] = []
        arr.append(.init(d: "2020-01-01", lv: 5)) // 自己ベスト(古い日付。トリムで消えるはずの位置)
        for i in 1...200 {
            arr.append(.init(d: String(format: "2026-01-%03d", i), lv: 1))
        }
        let store = RecordStore(inMemory: [:])
        store.set("reach", arr)
        RecordLogic.setReach(store, lv: 2, now: date(2026, 7, 25, 10, 0), timeZone: jst) // 202件目→トリム発生
        let after = RecordLogic.getReach(store)
        // index.html: splice(0,len-200)で200件に切ってから、ベストが残っていなければunshiftで戻す
        // ため、ベスト保護が発動した回だけ意図的に201件になる(JS版そのままの仕様。バグではない)。
        XCTAssertEqual(after.count, 201)
        XCTAssertTrue(after.contains { $0.lv == 5 }, "自己ベスト(lv5)がトリムで失われてはいけない")
    }
}
