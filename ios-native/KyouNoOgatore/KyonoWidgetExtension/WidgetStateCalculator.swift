//
//  WidgetStateCalculator.swift
//  KyonoWidgetExtension
//
//  GO-H1(ホーム画面ウィジェット): Android版WidgetLogic.ktと同じ考え方の純粋関数。
//  WidgetSummary(ミラーJSON)+ある時刻を受け取り、その時刻での表示状態を決める。
//  ミラーJSON自体はアプリ側で書いた時点のスナップショットだが、effectiveStreakCountに相当する
//  「壊れたかどうか」の判定はstreakBreaksOnDateを使ってウィジェット側の任意の時刻で再評価できる
//  (アプリを開き直さなくても正しく「また1日め」に切り替わる)。
//

import Foundation
import RecordCore

enum CharaAsset: String {
    case cheer = "chara-cheer"
    case kaikyaku = "chara-kaikyaku"
    case congrats = "chara-congrats"
    case good = "chara-good"
    case crown = "chara-crown"
    case cracker = "chara-cracker"
}

struct WidgetDisplayState {
    let chara: CharaAsset
    let message: String
    let streakCount: Int
    let last7: [String]
}

enum WidgetStateCalculator {
    static func compute(summary: WidgetSummary?, at date: Date) -> WidgetDisplayState {
        guard let summary else {
            return WidgetDisplayState(
                chara: .cheer, message: "きょうから また1日め🌱",
                streakCount: 0, last7: Array(repeating: "none", count: 7)
            )
        }

        // GO-H1 監査GO-1(Fable視点A/C): 「今日」は必ずRecordLogic.todayStr(-3h境界)経由で
        // 計算する。以前はここだけCalendar+.currentの深夜0時境界で自前計算しており、
        // WidgetSummaryWriter側(todayStr経由・-3h境界)と定義がズレていた
        // (深夜0:30に記録した直後に「ねる前に1本」が出る・streakBreaksOnDateの判定が
        // 3時間早まる、の2つの実害があった)。
        let today = RecordLogic.todayStr(now: date)
        // GO-H1§2-1(最重要): 生のsummary.streakをそのまま出さない。streakBreaksOnDateを過ぎて
        // いれば0扱いにする(effectiveStreakCountをアプリ再起動なしで再現するための仕組み)。
        var effStreak = summary.streak
        if let breaks = summary.streakBreaksOnDate, today >= breaks {
            effStreak = 0
        }
        // doneTodayもrecordedDateと同じ日でなければ「きょう」の意味を持たない。
        let effDoneToday = summary.doneToday && summary.recordedDate == today

        let cal = Calendar(identifier: .gregorian)
        let hour = cal.component(.hour, from: date)
        let isMorning = hour >= 5 && hour < 17
        let isMilestoneToday = summary.milestone && summary.recordedDate == today

        let chara: CharaAsset
        let message: String
        switch true {
        case isMilestoneToday && summary.milestoneBig:
            chara = .crown
            message = "きょうもおつかれさま！"
        case isMilestoneToday:
            chara = .cracker
            message = "きょうもおつかれさま！"
        case effStreak == 0:
            chara = .cheer
            message = "きょうから また1日め🌱"
        case !effDoneToday && isMorning:
            chara = .cheer
            message = "きょうもいこう！💪"
        case !effDoneToday:
            chara = .kaikyaku
            message = "ねる前に1本 どう？🌙"
        default:
            let celebrating = (summary.celebrateUntil ?? 0) >= date.timeIntervalSince1970
            chara = celebrating ? .congrats : .good
            message = celebrating ? "きょうもおつかれさま！" : "つづいてるね！"
        }

        return WidgetDisplayState(chara: chara, message: message, streakCount: effStreak, last7: summary.last7)
    }
}
