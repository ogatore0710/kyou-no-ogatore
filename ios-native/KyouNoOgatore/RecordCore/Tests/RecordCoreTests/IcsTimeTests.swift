import XCTest
@testable import RecordCore

// TASK 2026-08-10 A-1+B-3(C3総監査→alan5発注): 「毎日の合図」時刻の正本(IcsTime)のテスト。
// 表示(SettingsView)と通知予約(DailyNotifications)が共にこの1実装を参照する前提なので、
// ここが正しければ「表示と実通知の既定時刻ずれ」は構造的に起きない。
final class IcsTimeTests: XCTestCase {
    // 既定時刻テーブル: index.html:1974-1979 ANCHORSと同値・未設定/未知キーはfree扱い
    func testAnchorDefaults() {
        XCTAssertEqual(IcsTime.defaultFor(anchorKey: "asa").hour, 7)
        XCTAssertEqual(IcsTime.defaultFor(anchorKey: "asa").minute, 30)
        XCTAssertEqual(IcsTime.defaultFor(anchorKey: "furo").hour, 20)
        XCTAssertEqual(IcsTime.defaultFor(anchorKey: "neru").hour, 21)
        XCTAssertEqual(IcsTime.defaultFor(anchorKey: "free").hour, 20)
        XCTAssertEqual(IcsTime.defaultFor(anchorKey: "free").minute, 0)
        XCTAssertEqual(IcsTime.defaultFor(anchorKey: nil).key, "free")
        XCTAssertEqual(IcsTime.defaultFor(anchorKey: "unknown").key, "free")
    }

    func testResolvePrefersSavedIcstime() {
        let store = RecordStore(inMemory: [:])
        store.set("anchor", "asa")
        store.set("icstime", "21:07")
        let (h, m) = IcsTime.resolve(store: store)
        XCTAssertEqual(h, 21)
        XCTAssertEqual(m, 7) // 生値(丸めない)。丸め+書き戻しはSettingsViewの自己修復が担う
    }

    func testResolveFallsBackToAnchorDefault() {
        let store = RecordStore(inMemory: [:])
        store.set("anchor", "neru")
        let (h, m) = IcsTime.resolve(store: store)
        XCTAssertEqual(h, 21)
        XCTAssertEqual(m, 30)
    }

    func testResolveMalformedIcstimeFallsBackToDefault() {
        let store = RecordStore(inMemory: [:])
        store.set("anchor", "furo")
        store.set("icstime", "ab:cd") // 数値にできない→既定へ(旧実装と同じ挙動)
        let (h, m) = IcsTime.resolve(store: store)
        XCTAssertEqual(h, 20)
        XCTAssertEqual(m, 30)
    }

    // 15分丸め(§2-2): 最も近い15分・上限45。既存実装 min(45, ((m+7)/15)*15) の仕様固定
    func testRoundedMinute() {
        XCTAssertEqual(IcsTime.roundedMinute(0), 0)
        XCTAssertEqual(IcsTime.roundedMinute(7), 0)
        XCTAssertEqual(IcsTime.roundedMinute(8), 15)
        XCTAssertEqual(IcsTime.roundedMinute(22), 15)
        XCTAssertEqual(IcsTime.roundedMinute(23), 30)
        XCTAssertEqual(IcsTime.roundedMinute(37), 30) // A-1再現ケース: 07:37→07:30
        XCTAssertEqual(IcsTime.roundedMinute(38), 45)
        XCTAssertEqual(IcsTime.roundedMinute(59), 45) // 上限45で頭打ち(繰り上がりで時を跨がない)
    }

    func testFormat() {
        XCTAssertEqual(IcsTime.format(hour: 7, minute: 5), "07:05")
        XCTAssertEqual(IcsTime.format(hour: 21, minute: 45), "21:45")
    }

    // A-1の書き戻しシナリオの中核: 非15分刻みの保存値→丸め→formatで正規形になる往復
    func testSelfHealRoundtrip() {
        let store = RecordStore(inMemory: [:])
        store.set("icstime", "07:37")
        let (h, m) = IcsTime.resolve(store: store)
        let healed = IcsTime.format(hour: h, minute: IcsTime.roundedMinute(m))
        XCTAssertEqual(healed, "07:30")
        store.set("icstime", healed)
        let (h2, m2) = IcsTime.resolve(store: store)
        XCTAssertEqual(IcsTime.roundedMinute(m2), m2) // 書き戻し後は丸め安定(再修復不要)
        XCTAssertEqual(h2, 7)
    }
}
