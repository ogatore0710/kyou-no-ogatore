//
//  WidgetSummaryReader.swift
//  KyonoWidgetExtension
//
//  GO-H1(ホーム画面ウィジェット): App Group共有コンテナのミラーJSONを読むだけの拡張専用ロジック。
//  構造体自体(WidgetSummary)はWidgetSummary.swiftへ分離し、アプリ本体ターゲットとも共有する。
//

import Foundation

enum WidgetSummaryReader {
    static let appGroupId = "group.jp.ogatore.kyouno"
    private static let fileName = "widget-summary.json"

    static func read() -> WidgetSummary? {
        guard let dir = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: appGroupId) else { return nil }
        guard let data = try? Data(contentsOf: dir.appendingPathComponent(fileName)) else { return nil }
        return try? JSONDecoder().decode(WidgetSummary.self, from: data)
    }
}
