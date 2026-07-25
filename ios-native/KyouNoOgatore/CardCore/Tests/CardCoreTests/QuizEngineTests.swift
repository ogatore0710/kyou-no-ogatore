import XCTest
@testable import CardCore

// ネイティブ移植 Step 4(マスタープラン§6 Step4検収基準2): decideTypeの単体テスト。
// scripts/qa.jsのcheckQuizTypeTiebreak(2026-07-20 PO承認「かたさタイプ同点タイブレーク」)と
// 同じ観点をXCTestに移植する。
final class QuizEngineTests: XCTestCase {
    private func date(_ epochDay: Int) -> Date {
        // rotationIndex(now) = floor((now_ms + 6h) / 86400000) となるよう、指定の日数ぶんのepochを渡す。
        // r=0..11の各値をピンポイントで作るための最小限のヘルパー(UTC基準で「r日目の正午」を使えば
        // +6hオフセットを跨がずrotationIndex()==epochDayになる)。
        Date(timeIntervalSince1970: Double(epochDay) * 86400 + 43200)
    }

    // (1) robot/yawaraのゲートは同点処理より優先(qa.js相当)
    func testRobotYawaraGatesTakePriorityOverTiebreak() {
        XCTAssertEqual(QuizEngine.decideType(.init(momo: 3, koka: 3, kenko: 3, ashi: 0), worry: nil, now: date(0)), "robot")
        XCTAssertEqual(QuizEngine.decideType(.init(momo: 2, koka: 0, kenko: 0, ashi: 0), worry: nil, now: date(0)), "yawara")
        XCTAssertEqual(QuizEngine.decideType(.init(momo: 1, koka: 1, kenko: 1, ashi: 1), worry: nil, now: date(0)), "yawara")
    }

    // (2) 単独最高点は悩み・日付に関係なくその部位
    func testSingleMaxHolderWinsRegardlessOfWorryOrDate() {
        XCTAssertEqual(QuizEngine.decideType(.init(momo: 0, koka: 1, kenko: 0, ashi: 2), worry: "katakori", now: date(5)), "ashi")
    }

    // (3) 悩みタイブレーク: 同点の中に悩み対応部位があればそれを選ぶ
    func testWorryTiebreakPicksMatchingHolder() {
        XCTAssertEqual(QuizEngine.decideType(.init(momo: 2, koka: 2, kenko: 2, ashi: 2), worry: "katakori", now: date(5)), "kenko")
        XCTAssertEqual(QuizEngine.decideType(.init(momo: 2, koka: 2, kenko: 2, ashi: 2), worry: "yotsu", now: date(5)), "momo")
        XCTAssertEqual(QuizEngine.decideType(.init(momo: 1, koka: 2, kenko: 1, ashi: 2), worry: "yotsu", now: date(5)), "koka") // 第2候補
    }

    // (4) 悩みで決まらない同点は日付ローテーションで決定的に散る(再現性・rでの切り替わり)
    func testRotationTiebreakIsDeterministicAndVariesByDate() {
        let r0 = QuizEngine.decideType(.init(momo: 2, koka: 1, kenko: 1, ashi: 2), worry: "yawaraka", now: date(0))
        XCTAssertEqual(r0, "momo")
        XCTAssertEqual(QuizEngine.decideType(.init(momo: 2, koka: 1, kenko: 1, ashi: 2), worry: "yawaraka", now: date(0)), r0, "同一入力・同一rなら同一結果")
        XCTAssertEqual(QuizEngine.decideType(.init(momo: 2, koka: 1, kenko: 1, ashi: 2), worry: "yawaraka", now: date(1)), "ashi")
        XCTAssertEqual(QuizEngine.decideType(.init(momo: 2, koka: 2, kenko: 1, ashi: 0), worry: "katakori", now: date(3)), "koka", "肩こりでも同点にkenkoが居なければローテーションへ")
    }

    // (5) 分布の対称性(§6 Step4検収基準2): 全256通り×r=0..11の合算で4部位の当選数が完全一致=各603
    func testDistributionOver256CombosTimes12Rotations() {
        var counts: [String: Int] = ["momo": 0, "koka": 0, "kenko": 0, "ashi": 0]
        for r in 0..<12 {
            let now = date(r)
            for a in 0..<4 { for b in 0..<4 { for c in 0..<4 { for d in 0..<4 {
                let t = QuizEngine.decideType(.init(momo: a, koka: b, kenko: c, ashi: d), worry: nil, now: now)
                if counts[t] != nil { counts[t]! += 1 }
            }}}}
        }
        XCTAssertEqual(counts, ["momo": 603, "koka": 603, "kenko": 603, "ashi": 603], "偏りが発生: \(counts)")
    }
}
