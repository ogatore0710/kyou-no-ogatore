//
//  WidgetStateCalculatorTests.swift
//  WidgetCoreTests
//
//  Fable監査GO-14(alan5差し戻し2026-07-28・141条案件): H1ホーム画面ウィジェットのiOS側計算
//  ロジックにコミット済みテストが1つも無かった問題への対応。D3(congrats→good→次の日)の
//  3点遷移は、これまでこのセッション限りのswiftcワンショット検証で終わっていたものを、
//  本番コードそのもの(WidgetStateCalculator.compute)に対するXCTestとして固定する。
//
//  タイムゾーン注記: WidgetStateCalculator.compute()は内部でRecordLogic.todayStr(デフォルトが
//  デバイスのローカルTZ)と素のCalendar(こちらもデフォルトはローカルTZ)を使うため、
//  この開発フリート(JST)を前提に時刻を選んでいる(既存のRecordLogicTest.swift等と同じ前提)。

import XCTest
@testable import WidgetCore

final class WidgetStateCalculatorTests: XCTestCase {
    private func summary(
        recordedDate: String, doneToday: Bool, streak: Int, streakBreaksOnDate: String? = nil,
        last7: [String] = Array(repeating: "none", count: 7), milestone: Bool = false,
        milestoneBig: Bool = false, celebrateUntil: TimeInterval? = nil
    ) -> WidgetSummary {
        WidgetSummary(
            recordedDate: recordedDate, doneToday: doneToday, streak: streak, streakBreaksOnDate: streakBreaksOnDate,
            last7: last7, milestone: milestone, milestoneBig: milestoneBig, celebrateUntil: celebrateUntil
        )
    }

    // GO-H1 D3(alan5差し戻し2026-07-28): 「記録直後(congrats)→4時間後(good)→翌日(cheer/
    // kaikyaku)」の3点遷移を固定する。recordedAt=2026-07-20T00:00:00Z(=09:00 JST、朝)。
    func testCongratsDecaysToGoodThenNextDayFallsToKaikyaku() {
        let iso = ISO8601DateFormatter()
        let recordedAt = iso.date(from: "2026-07-20T00:00:00Z")!
        let celebrateUntil = recordedAt.addingTimeInterval(4 * 3600).timeIntervalSince1970
        let s = summary(recordedDate: "2026-07-20", doneToday: true, streak: 1, celebrateUntil: celebrateUntil)

        let justAfter = WidgetStateCalculator.compute(summary: s, at: recordedAt.addingTimeInterval(60))
        let threeHoursLater = WidgetStateCalculator.compute(summary: s, at: recordedAt.addingTimeInterval(3 * 3600))
        let fourHoursOneMinLater = WidgetStateCalculator.compute(summary: s, at: recordedAt.addingTimeInterval(4 * 3600 + 60))
        let lateSameDay = WidgetStateCalculator.compute(summary: s, at: recordedAt.addingTimeInterval(10 * 3600)) // 19時JST、当日いっぱい
        let nextDayEvening = WidgetStateCalculator.compute(summary: s, at: recordedAt.addingTimeInterval(33 * 3600)) // 18時JST翌日

        XCTAssertEqual(justAfter.chara, .congrats)
        XCTAssertEqual(justAfter.message, "きょうもおつかれさま！")
        XCTAssertEqual(threeHoursLater.chara, .congrats)
        XCTAssertEqual(fourHoursOneMinLater.chara, .good)
        XCTAssertEqual(fourHoursOneMinLater.message, "つづいてるね！")
        XCTAssertEqual(lateSameDay.chara, .good)
        XCTAssertEqual(nextDayEvening.chara, .kaikyaku)
    }

    // Fable監査GO-6の裏取り(iOS側は元々chara/messageを同じswitch式で両方セットしており
    // 揃っている・Android版のような食い違いが無いことの確認)。
    func testMilestoneDayKeepsCrownAndCrackerMessagesConsistent() {
        // recordedDateは両方とも「今日」の判定対象(at)と同じ日にする(この検証で見たいのは
        // 節目分岐のchara/message一致であって日付そのものではないため)。
        let smallMilestone = summary(recordedDate: "2026-07-07", doneToday: true, streak: 7, milestone: true, milestoneBig: false)
        let bigMilestone = summary(recordedDate: "2026-07-07", doneToday: true, streak: 30, milestone: true, milestoneBig: true)
        let at = ISO8601DateFormatter().date(from: "2026-07-07T12:00:00Z")!

        let small = WidgetStateCalculator.compute(summary: smallMilestone, at: at)
        let big = WidgetStateCalculator.compute(summary: bigMilestone, at: at)

        XCTAssertEqual(small.chara, .cracker)
        XCTAssertEqual(small.message, "きょうもおつかれさま！")
        XCTAssertEqual(big.chara, .crown)
        XCTAssertEqual(big.message, "きょうもおつかれさま！")
    }

    // Fable監査GO-1/GO-5(alan5差し戻し2026-07-28): ミラーJSON不在(nil)と本当の連続0日を
    // 表示上区別する。isUnavailable=trueかつ「また1日め」を名乗らないこと。
    func testNilSummaryIsDistinctFromGenuineZeroStreak() {
        let unavailable = WidgetStateCalculator.compute(summary: nil, at: Date(timeIntervalSince1970: 1_753_000_000))
        XCTAssertTrue(unavailable.isUnavailable)
        XCTAssertNotEqual(unavailable.message, "きょうから また1日め🌱")

        let genuineZero = summary(recordedDate: "2026-07-20", doneToday: false, streak: 12, streakBreaksOnDate: "2026-07-20")
        let zero = WidgetStateCalculator.compute(summary: genuineZero, at: ISO8601DateFormatter().date(from: "2026-07-20T12:00:00Z")!)
        XCTAssertFalse(zero.isUnavailable)
        XCTAssertEqual(zero.message, "きょうから また1日め🌱")
    }
}
