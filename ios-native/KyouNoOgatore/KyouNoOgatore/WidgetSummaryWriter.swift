//
//  WidgetSummaryWriter.swift
//  KyouNoOgatore
//
//  GO-H1(ホーム画面ウィジェット・Duolingo式・本人GO 2026-07-28): 発注書§3のiOS注意事項どおり、
//  RecordStore本体(Documents配下)は一切動かさず、表示専用の小さなサマリJSONだけを片道で
//  App Groupの共有コンテナへ書き出す(ミラー方式)。ウィジェット拡張はこのJSONしか読まない。
//

import Foundation
import RecordCore
import CardCore

struct WidgetSummary: Codable {
    // このスナップショットが「きょう」として計算された日(doneToday/streakの基準日)。
    let recordedDate: String
    let doneToday: Bool
    // GO-H1§2-1(最重要): 書き出す時点でRecordLogic.effectiveStreakCountを通した値。
    // 生のstreak.countは絶対に書き出さない。
    let streak: Int
    // 何もしなければ、この日付(yyyy-MM-dd)以降はstreakBrokenNowがtrueになる(=0扱いにすべき)。
    // アプリを開き直さなくてもウィジェット側で正しく「また1日め」に切り替えられるようにするための
    // 事前計算値(nilは「直近14日は壊れない」)。
    let streakBreaksOnDate: String?
    // 直近7日(きょう含む・古い→新しい順)。"done" / "freeze" / "none" のいずれか。
    let last7: [String]
    let milestone: Bool
    let milestoneBig: Bool
    // GO-H1§2-4「記録した直後〜当日」vs「翌日以降に見たとき」の区別用(congrats→goodへの
    // 切り替え)。この時刻(epoch秒)まではcongrats、以降はgoodをウィジェット側が選ぶ。
    let celebrateUntil: TimeInterval?
}

enum WidgetSummaryWriter {
    static let appGroupId = "group.jp.ogatore.kyouno"
    private static let fileName = "widget-summary.json"
    // GO-H1(alan5承認2026-07-28): 既存のMS配列(3/4/7/14/21/30…)の並びに合わせ、30日以上を
    // 「大きい節目」とする(Android版WidgetLogic.ktと同じ閾値)。
    private static let bigMilestoneThreshold = 30
    private static let lookaheadDays = 14

    static func containerURL() -> URL? {
        FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: appGroupId)
    }

    // GO-H1§4: 記録した瞬間に呼ぶ(HomeView.swiftのmarkDone直後)。
    static func write(store: RecordStore, now: Date = Date()) {
        guard let dir = containerURL() else { return }
        let cal = Calendar(identifier: .gregorian)
        let streak = RecordLogic.loadStreak(store)
        let today = RecordLogic.todayStr(now: now)
        let doneToday = streak.dates.contains(today)
        let effCount = RecordLogic.effectiveStreakCount(store, streak, now: now)

        var breaksOnDate: String?
        if effCount > 0 {
            for offset in 1...lookaheadDays {
                guard let candidate = cal.date(byAdding: .day, value: offset, to: now) else { break }
                if RecordLogic.streakBrokenNow(store, streak, now: candidate) {
                    breaksOnDate = RecordLogic.todayStr(now: candidate)
                    break
                }
            }
        }

        let doneDates = Set(streak.dates)
        let sortedDates = streak.dates.sorted()
        let last7: [String] = (0..<7).compactMap { offsetFromOldest -> String? in
            guard let d = cal.date(byAdding: .day, value: -(6 - offsetFromOldest), to: now) else { return nil }
            let ds = RecordLogic.todayStr(now: d)
            if doneDates.contains(ds) { return "done" }
            if sortedDates.contains(where: { $0 < ds }) && sortedDates.contains(where: { $0 > ds }) { return "freeze" }
            return "none"
        }

        // renderTodayCard(HomeView.swift)と同じ考え方: 節目判定はeffectiveStreakCountではなく
        // streak.total(通算・途切れても減らない値)をMILESTONESと突き合わせる。
        let data = CardDataLoader.shared
        let isMilestoneToday = doneToday && data.MILESTONES.contains(streak.total)
        let isBig = isMilestoneToday && streak.total >= bigMilestoneThreshold

        let summary = WidgetSummary(
            recordedDate: today,
            doneToday: doneToday,
            streak: effCount,
            streakBreaksOnDate: breaksOnDate,
            last7: last7,
            milestone: isMilestoneToday,
            milestoneBig: isBig,
            celebrateUntil: doneToday ? now.addingTimeInterval(4 * 3600).timeIntervalSince1970 : nil
        )

        guard let encoded = try? JSONEncoder().encode(summary) else { return }
        try? encoded.write(to: dir.appendingPathComponent(fileName), options: .atomic)
    }
}
