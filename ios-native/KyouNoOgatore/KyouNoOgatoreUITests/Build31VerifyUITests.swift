//
//  Build31VerifyUITests.swift
//  KyouNoOgatoreUITests
//
//  build33 R-49(ホーム立体化・案A/案B比較)の実描画検収用。撮影後に削除する。
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

    // ホストからSNAP_PREFIX環境変数でライト/ダークの撮影名を切り替える。
    func testR49HomeCapture() throws {
        let app = XCUIApplication()
        app.launch()
        let prefix = ProcessInfo.processInfo.environment["SNAP_PREFIX"] ?? "26-r49-home"
        let mainLabel = app.staticTexts["メインの一本"].firstMatch
        XCTAssertTrue(mainLabel.waitForExistence(timeout: 15), "あなた用行が出ない(seed失敗の疑い)")
        snap("\(prefix)-top")
        app.swipeUp()
        Thread.sleep(forTimeInterval: 0.5)
        snap("\(prefix)-mid")
    }
}
