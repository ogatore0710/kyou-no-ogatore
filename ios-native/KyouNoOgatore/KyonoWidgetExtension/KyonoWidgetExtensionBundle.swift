//
//  KyonoWidgetExtensionBundle.swift
//  KyonoWidgetExtension
//
//  GO-H1(ホーム画面ウィジェット・Duolingo式・本人GO 2026-07-28): Phase1=表示専用。
//  タップでアプリを開くだけ・RecordStoreへは一切触れない(WidgetSummaryReaderがミラーJSONを
//  読むだけ)。systemSmall=絵+数字だけ・systemMedium=絵+一言+数字+7日ドットの設計(Android版と
//  同じくサイズごとに表示量を変える。発注書に明記は無いが事故ではなく意図的な判断)。
//

import WidgetKit
import SwiftUI
import WidgetCore

@main
struct KyonoWidgetExtensionBundle: WidgetBundle {
    var body: some Widget {
        KyonoWidget()
    }
}

struct KyonoEntry: TimelineEntry {
    let date: Date
    let state: WidgetDisplayState
}

struct KyonoProvider: TimelineProvider {
    func placeholder(in context: Context) -> KyonoEntry {
        KyonoEntry(date: Date(), state: WidgetStateCalculator.compute(summary: nil, at: Date()))
    }

    func getSnapshot(in context: Context, completion: @escaping (KyonoEntry) -> Void) {
        let summary = WidgetSummaryReader.read()
        completion(KyonoEntry(date: Date(), state: WidgetStateCalculator.compute(summary: summary, at: Date())))
    }

    // GO-H1§4「時間帯の境界（5時・11時・17時）でタイムラインを組む」: 今日・明日・明後日ぶんの
    // 5時/17時境界と、congrats→good切り替え時刻・streak破断予定日をまとめてエントリ化する。
    // アプリを開き直さなくても正しい絵に自動で切り替わる(高頻度ポーリングはしない)。
    func getTimeline(in context: Context, completion: @escaping (Timeline<KyonoEntry>) -> Void) {
        let summary = WidgetSummaryReader.read()
        let now = Date()
        let cal = Calendar(identifier: .gregorian)

        var dates: [Date] = [now]
        for dayOffset in 0...2 {
            guard let day = cal.date(byAdding: .day, value: dayOffset, to: now) else { continue }
            for hour in [5, 17] {
                if let boundary = cal.date(bySettingHour: hour, minute: 0, second: 0, of: day), boundary > now {
                    dates.append(boundary)
                }
            }
        }
        if let celebrateUntil = summary?.celebrateUntil {
            let d = Date(timeIntervalSince1970: celebrateUntil)
            if d > now { dates.append(d.addingTimeInterval(1)) }
        }
        if let breaksStr = summary?.streakBreaksOnDate {
            let f = DateFormatter()
            f.calendar = cal
            f.timeZone = .current
            f.dateFormat = "yyyy-MM-dd"
            if let breaksDate = f.date(from: breaksStr), breaksDate > now {
                dates.append(breaksDate)
            }
        }
        dates.sort()

        let entries = dates.map { KyonoEntry(date: $0, state: WidgetStateCalculator.compute(summary: summary, at: $0)) }
        let refreshAt = dates.last ?? now.addingTimeInterval(3600)
        completion(Timeline(entries: entries, policy: .after(refreshAt)))
    }
}

struct KyonoWidget: Widget {
    let kind: String = "KyonoWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: KyonoProvider()) { entry in
            KyonoWidgetEntryView(entry: entry)
                .containerBackground(Color(red: 1, green: 0.98, blue: 0.953), for: .widget)
        }
        .configurationDisplayName("きょうのオガトレ")
        .description("きょうのストレッチ状況をひとめで")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

struct KyonoWidgetEntryView: View {
    @Environment(\.widgetFamily) private var family
    let entry: KyonoEntry

    // GO-H1§1: タップでアプリが開くだけ(それだけ)。WidgetKitは明示的なLink/widgetURLが
    // 無くても、ウィジェットタップでホストアプリを開く既定動作を持つため、URL Schemeの新設は
    // 不要(発注書はPhase1で深いリンク先を要求していない)。
    var body: some View {
        switch family {
        case .systemMedium:
            KyonoWideView(state: entry.state)
        default:
            KyonoSmallView(state: entry.state)
        }
    }
}

private let inkColor = Color(red: 0.227, green: 0.227, blue: 0.208)
private let subColor = Color(red: 0.431, green: 0.42, blue: 0.373)
private let tealStrong = Color(red: 0.118, green: 0.482, blue: 0.439)
private let freezeColor = Color(red: 0.949, green: 0.851, blue: 0.545)
private let noneDotColor = Color(red: 0.910, green: 0.886, blue: 0.824)

struct KyonoSmallView: View {
    let state: WidgetDisplayState

    var body: some View {
        VStack(spacing: 4) {
            Spacer()
            Image(state.chara.rawValue)
                .resizable()
                .scaledToFit()
                .frame(height: 64)
            Text(streakLabel(state))
                .font(.system(size: 12, weight: .bold))
                .foregroundColor(inkColor)
        }
        .padding(10)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
    }
}

struct KyonoWideView: View {
    let state: WidgetDisplayState

    var body: some View {
        HStack(spacing: 10) {
            Image(state.chara.rawValue)
                .resizable()
                .scaledToFit()
                .frame(width: 72, height: 72)
            VStack(alignment: .leading, spacing: 2) {
                Text(state.message)
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(inkColor)
                Text(streakLabel(state))
                    .font(.system(size: 12))
                    .foregroundColor(subColor)
                Spacer().frame(height: 4)
                HStack(spacing: 4) {
                    ForEach(Array(state.last7.enumerated()), id: \.offset) { _, dot in
                        Circle()
                            .fill(dotColor(dot))
                            .frame(width: 9, height: 9)
                    }
                }
            }
            Spacer()
        }
        .padding(12)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
    }
}

// GO-H1§2-1: 表示は必ずeffectiveStreakCount経由(WidgetStateCalculator.compute内で確定済み)。
// Fable監査GO-5: ミラーJSON不在(isUnavailable)のときはstreakCountの値を一切見ない
// (0日の新規ユーザーと区別する中立文言)。
private func streakLabel(_ state: WidgetDisplayState) -> String {
    if state.isUnavailable { return "じゅんび中…" }
    return state.streakCount == 0 ? "また1日め" : "\(state.streakCount)日つづいてる"
}

private func dotColor(_ dot: String) -> Color {
    switch dot {
    case "done": return tealStrong
    case "freeze": return freezeColor
    default: return noneDotColor
    }
}
