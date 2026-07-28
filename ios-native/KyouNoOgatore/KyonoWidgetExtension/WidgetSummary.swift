//
//  WidgetSummary.swift
//  KyonoWidgetExtension
//
//  GO-H1(ホーム画面ウィジェット): アプリ側WidgetSummaryWriter.swiftのWidgetSummaryと同じ形。
//  拡張ターゲットはRecordCoreに依存させない(ミラーJSONを読むだけ)ため、小さな構造体を
//  あえて重複させている。フィールドを変えるときは両方を必ず揃えること。
//

import Foundation

struct WidgetSummary: Codable {
    let recordedDate: String
    let doneToday: Bool
    let streak: Int
    let streakBreaksOnDate: String?
    let last7: [String]
    let milestone: Bool
    let milestoneBig: Bool
    let celebrateUntil: TimeInterval?
}

enum WidgetSummaryReader {
    static let appGroupId = "group.jp.ogatore.kyouno"
    private static let fileName = "widget-summary.json"

    static func read() -> WidgetSummary? {
        guard let dir = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: appGroupId) else { return nil }
        guard let data = try? Data(contentsOf: dir.appendingPathComponent(fileName)) else { return nil }
        return try? JSONDecoder().decode(WidgetSummary.self, from: data)
    }
}
