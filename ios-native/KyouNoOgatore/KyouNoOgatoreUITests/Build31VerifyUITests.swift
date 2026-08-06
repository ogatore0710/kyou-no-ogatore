//
//  Build31VerifyUITests.swift
//  KyouNoOgatore
//
//  build34 R-53(ボタン影撤去・枠のみ維持)の実描画検収用。撮影後に削除する。
//  同期方式: NSLog→ホストのlog stream検知→simctl io screenshot(ラウンド9で確立した型)。
//

import XCTest

final class Build31VerifyUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    private func snap(_ name: String) {
        NSLog("B31SNAP %@", name)
        Thread.sleep(forTimeInterval: 3.0)
    }

    private func tapAny(_ app: XCUIApplication, _ label: String, timeout: TimeInterval = 8) {
        let el = app.descendants(matching: .any)[label].firstMatch
        XCTAssertTrue(el.waitForExistence(timeout: timeout), "「\(label)」が見つからない")
        el.tap()
    }

    // ホストからSNAP_PREFIX環境変数でライト/ダークの撮影名を切り替える。
    func testR49HomeCapture() throws {
        let app = XCUIApplication()
        app.launch()
        let prefix = ProcessInfo.processInfo.environment["SNAP_PREFIX"] ?? "34-r53"

        // ホーム: カード枠は影あり・連続再生(ゴースト)ボタンは影なしを確認
        Thread.sleep(forTimeInterval: 2.0)
        snap("\(prefix)-home-top")
        app.swipeUp()
        Thread.sleep(forTimeInterval: 0.5)
        snap("\(prefix)-home-mid")

        // マイ記録: 図鑑バナー影なし+お楽しみ3ボタン影なし+カードは影あり
        tapAny(app, "マイ記録", timeout: 15)
        Thread.sleep(forTimeInterval: 1.0)
        snap("\(prefix)-myrecord-top")
        app.swipeUp(); app.swipeUp(); app.swipeUp(); app.swipeUp()
        Thread.sleep(forTimeInterval: 0.5)
        snap("\(prefix)-myrecord-bottom")

        // 動画を探す: VideoRow(動画の枠)は影を維持
        tapAny(app, "動画を探す")
        Thread.sleep(forTimeInterval: 2.0)
        snap("\(prefix)-search")
    }
}
