//
//  WidgetSummary.swift
//  WidgetCore
//
//  GO-H1(ホーム画面ウィジェット): アプリ側(WidgetSummaryWriterが書く)と拡張側
//  (WidgetSummaryReaderが読む)の両方が同じ形を見る必要があるミラーJSONの構造体。
//
//  Fable監査GO-5/GO-14(alan5差し戻し2026-07-28): 以前はこの構造体をアプリ側と拡張側に手で
//  複製しており(「フィールドを変えるときは両方を必ず揃えること」という運用ルール任せ)、
//  一度は1ファイルの両ターゲット所属で共有する形にしたが、GO-14でWidgetStateCalculatorごと
//  このSwift Packageへ寄せたことで、より一般的な「1箇所に定義してimportする」形になった。
//

import Foundation

public struct WidgetSummary: Codable {
    public let recordedDate: String
    public let doneToday: Bool
    public let streak: Int
    public let streakBreaksOnDate: String?
    public let last7: [String]
    public let milestone: Bool
    public let milestoneBig: Bool
    public let celebrateUntil: TimeInterval?

    public init(
        recordedDate: String, doneToday: Bool, streak: Int, streakBreaksOnDate: String?,
        last7: [String], milestone: Bool, milestoneBig: Bool, celebrateUntil: TimeInterval?
    ) {
        self.recordedDate = recordedDate
        self.doneToday = doneToday
        self.streak = streak
        self.streakBreaksOnDate = streakBreaksOnDate
        self.last7 = last7
        self.milestone = milestone
        self.milestoneBig = milestoneBig
        self.celebrateUntil = celebrateUntil
    }
}
