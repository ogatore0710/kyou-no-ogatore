//
//  KyonoWidgetExtensionBundle.swift
//  KyonoWidgetExtension
//
//  GO-H1(ホーム画面ウィジェット・Duolingo式・本人GO 2026-07-28): ターゲット新設のみのコミット用
//  プレースホルダー。実際のタイムライン/表示ロジックは次のコミット(TASK-C2-2026-07-28-
//  home-widget.md §2〜§4)で置き換える。
//

import WidgetKit
import SwiftUI

@main
struct KyonoWidgetExtensionBundle: WidgetBundle {
    var body: some Widget {
        KyonoWidgetPlaceholder()
    }
}

struct KyonoWidgetPlaceholder: Widget {
    let kind: String = "KyonoWidgetPlaceholder"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: PlaceholderProvider()) { _ in
            Text("きょうのオガトレ")
        }
        .configurationDisplayName("きょうのオガトレ")
        .description("準備中です")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

private struct PlaceholderEntry: TimelineEntry {
    let date: Date
}

private struct PlaceholderProvider: TimelineProvider {
    func placeholder(in context: Context) -> PlaceholderEntry {
        PlaceholderEntry(date: Date())
    }

    func getSnapshot(in context: Context, completion: @escaping (PlaceholderEntry) -> Void) {
        completion(PlaceholderEntry(date: Date()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<PlaceholderEntry>) -> Void) {
        completion(Timeline(entries: [PlaceholderEntry(date: Date())], policy: .never))
    }
}
