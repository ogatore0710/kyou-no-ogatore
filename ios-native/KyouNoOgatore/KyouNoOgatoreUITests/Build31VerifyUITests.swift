//
//  Build31VerifyUITests.swift
//  KyouNoOgatoreUITests
//
//  build32 R-46/R-47(1行化・図鑑バナー)の実描画検収用。撮影後に削除する。
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

    // 事前にホスト側でtheme=dark+type=momoをseed済みの前提(bigtext=true=おおきめ)。
    func testR46R47DarkRecapture() throws {
        let app = XCUIApplication()
        app.launch()

        // ホーム(ダーク・おおきめ): バッジ+連続再生ボタンの1行化
        let mainLabel = app.staticTexts["メインの一本"].firstMatch
        XCTAssertTrue(mainLabel.waitForExistence(timeout: 15), "あなた用行が出ない(seed失敗の疑い)")
        snap("22-r46-home-dark-singleline")

        // マイ記録(ダーク): 図鑑バナーのtealベタ塗り。isHittable判定が画面外でもtrueを返す
        // ことがあったため、固定回数スワイプで確実にバナー位置まで送る。
        tapAny(app, "マイ記録")
        let dex = app.descendants(matching: .any).containing(NSPredicate(format: "label CONTAINS %@", "カード図鑑")).firstMatch
        XCTAssertTrue(dex.waitForExistence(timeout: 10), "図鑑バナーが出ない")
        app.swipeUp()
        app.swipeUp()
        Thread.sleep(forTimeInterval: 0.5)
        snap("23-r47-dexbanner-dark")
    }
}
