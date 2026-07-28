//
//  WidgetSummary.swift
//  KyonoWidgetExtension
//
//  GO-H1(ホーム画面ウィジェット): アプリ側(WidgetSummaryWriterが書く)と拡張側
//  (WidgetSummaryReaderが読む)の両方が同じ形を見る必要があるミラーJSONの構造体。
//
//  Fable監査GO-5(alan5差し戻し2026-07-28): 以前はこの構造体をアプリ側
//  (WidgetSummaryWriter.swift)と拡張側(このファイル)に手で複製しており、
//  「フィールドを変えるときは両方を必ず揃えること」という運用ルールに頼っていた。
//  ズレても気づけない設計は事故の元(実害が出るまで気づけない)なので、この1ファイルだけを
//  KyouNoOgatore(アプリ本体)ターゲットとKyonoWidgetExtension(拡張)ターゲットの
//  両方のCompile Sourcesに所属させ、構造体の定義を物理的に1つにした。
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
