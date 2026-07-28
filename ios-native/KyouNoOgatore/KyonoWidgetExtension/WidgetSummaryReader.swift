//
//  WidgetSummaryReader.swift
//  KyonoWidgetExtension
//
//  GO-H1(ホーム画面ウィジェット): App Group共有コンテナのミラーJSONを読むだけの拡張専用ロジック。
//  構造体自体(WidgetSummary)はFable監査GO-14でWidgetCore Swift Packageへ移設した
//  (アプリ本体ターゲットともWidgetCoreを介して共有する)。
//

import Foundation
import WidgetCore

enum WidgetSummaryReader {
    // TestFlight配信準備(alan5判断2026-07-28・選択肢2): WidgetSummaryWriter.swiftと同じ理由で
    // 有料チーム向けに新しい識別子(.app)へ変更。
    static let appGroupId = "group.jp.ogatore.kyouno.app"
    private static let fileName = "widget-summary.json"

    static func read() -> WidgetSummary? {
        guard let dir = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: appGroupId) else { return nil }
        guard let data = try? Data(contentsOf: dir.appendingPathComponent(fileName)) else { return nil }
        return try? JSONDecoder().decode(WidgetSummary.self, from: data)
    }
}
