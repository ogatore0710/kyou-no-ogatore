//
//  WidgetSummaryWriter.swift
//  KyouNoOgatore
//
//  GO-H1(ホーム画面ウィジェット・Duolingo式・本人GO 2026-07-28): 発注書§3のiOS注意事項どおり、
//  RecordStore本体(Documents配下)は一切動かさず、表示専用の小さなサマリJSONだけを片道で
//  App Groupの共有コンテナへ書き出す(ミラー方式)。ウィジェット拡張はこのJSONしか読まない。
//
//  WidgetSummary構造体自体はFable監査GO-14でWidgetCore Swift Package(RecordCore等と同じ
//  ローカルパッケージ)へ移設した。アプリ本体・拡張の両ターゲットがWidgetCoreをimportして
//  同じ定義を共有する(以前は手で複製し「フィールドを変えるときは両方揃えること」という
//  運用ルール任せだった)。
//

import Foundation
import RecordCore
import CardCore
import WidgetCore

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
            if isFreezeBridged(store: store, sortedDoneDates: sortedDates, d: ds, today: today) { return "freeze" }
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

    // GO-H1 D4(alan5差し戻し2026-07-28・両OS共通の指摘): 以前は「その日の前後どちらにもやった日が
    // ある」だけでfreeze扱いにしていたが、券を使っていない日(単に連続が切れて再開しただけ)も
    // 券色に見せてしまう欠陥があった(「持っていない券を使ったように見える」)。Android版
    // WidgetLogic.ktのisFreezeBridgedと同じ考え方で、実際の残数チェックを通す。
    // 現在進行中の末尾ギャップ(afterが無い)はまだ確定していないのでcanBridgeFreezesをそのまま
    // 使えるが、既に確定した過去のギャップはtryUseFreezesで既にfreeze2へ加算済みのため、同じ
    // canBridgeFreezesを再度呼ぶと使用済み分を二重計上してfalseになってしまう。過去のギャップは
    // 「その月に実際に記録されている使用量が、このギャップの日数以上あるか」で近似する。
    private static func isFreezeBridged(store: RecordStore, sortedDoneDates: [String], d: String, today: String) -> Bool {
        guard let before = sortedDoneDates.last(where: { $0 < d }) else { return false }
        let after = sortedDoneDates.first(where: { $0 > d })
        if after == nil {
            let missedRun = datesBetweenExclusive(before, today)
            return missedRun.contains(d) && RecordLogic.canBridgeFreezes(store, missedDates: missedRun)
        }
        // Fable監査GO-4(alan5差し戻し2026-07-28): ギャップ全体の日数を1つの月の使用量とだけ
        // 比べると、月をまたぐギャップ(例: 7/30-8/1)で壊れる。tryUseFreezesがmissedDatesを
        // 月ごとに分割してneedを積むのと同じく、ここもdの月に属する部分だけに絞ってから
        // その月の使用量と比べる(Android版WidgetLogic.ktと同じ修正)。
        let missedRun = datesBetweenExclusive(before, after!)
        guard missedRun.contains(d) else { return false }
        let monthKey = String(d.prefix(7))
        let monthPortionSize = missedRun.filter { $0.prefix(7) == monthKey }.count
        let usedThisMonth = RecordLogic.freezeMap(store)[monthKey] ?? 0
        return monthPortionSize <= usedThisMonth
    }

    // RecordLogic.addDays相当は非公開(internal実装詳細)のため、ここではCalendarで同じ
    // "yyyy-MM-dd"文字列の加算をローカルに再実装する(RecordLogic.daysBetweenは公開済みなので
    // それだけ流用する)。
    private static func ymdFormatter() -> DateFormatter {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.timeZone = TimeZone(identifier: "UTC")
        f.dateFormat = "yyyy-MM-dd"
        return f
    }

    private static func datesBetweenExclusive(_ startInclusive: String, _ endExclusive: String) -> [String] {
        let gap = RecordLogic.daysBetween(startInclusive, endExclusive)
        guard gap > 1 else { return [] }
        let f = ymdFormatter()
        guard let startDate = f.date(from: startInclusive) else { return [] }
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = TimeZone(identifier: "UTC")!
        return (1..<gap).compactMap { offset in
            cal.date(byAdding: .day, value: offset, to: startDate).map { f.string(from: $0) }
        }
    }
}
