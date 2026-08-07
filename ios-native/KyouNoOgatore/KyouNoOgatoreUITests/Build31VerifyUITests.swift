//
//  Build31VerifyUITests.swift
//  KyouNoOgatore
//
//  build37 R-68/70/71/72の実描画検収用。撮影後に削除する。
//

import XCTest

final class Build31VerifyUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    private func snap(_ name: String, wait: TimeInterval = 0.3) {
        NSLog("B31SNAP %@", name)
        Thread.sleep(forTimeInterval: wait)
    }

    private func tapAny(_ app: XCUIApplication, _ label: String, timeout: TimeInterval = 8) {
        let el = app.descendants(matching: .any)[label].firstMatch
        XCTAssertTrue(el.waitForExistence(timeout: timeout), "「\(label)」が見つからない")
        el.tap()
    }

    // フェーズA(初回起動seed): スプラッシュ+オンボgreetingの静止確認
    func testSplashAndGreetingStillness() throws {
        let app = XCUIApplication()
        app.launch()
        // R-70: 起動直後のスプラッシュ(LaunchChara画像)を撮る
        snap("50-r70-splash", wait: 0.1)
        // R-71: greeting表示中に2回撮り、既出吹き出しの位置が動かないことを比較確認
        Thread.sleep(forTimeInterval: 2.5)
        snap("51-r71-greet-t1")
        Thread.sleep(forTimeInterval: 2.5)
        snap("52-r71-greet-t2")
        // フロー無事故確認: 設問1チップ到達
        let chip1 = app.staticTexts["大きめ（いまのまま）"].firstMatch
        XCTAssertTrue(chip1.waitForExistence(timeout: 15), "設問1チップが出ない")
    }

    // フェーズB(通常seed): 再生リストヘッダー+けっか画面の動画間隔
    func testCatalogHeaderAndResultSpacing() throws {
        let app = XCUIApplication()
        app.launch()
        tapAny(app, "再生リスト", timeout: 15)
        Thread.sleep(forTimeInterval: 1.0)
        snap("53-r68-catalog-header")

        tapAny(app, "マイ記録")
        Thread.sleep(forTimeInterval: 1.0)
        app.swipeUp(); app.swipeUp(); app.swipeUp()
        Thread.sleep(forTimeInterval: 0.5)
        // かたさタイプカードの「前回の結果」からけっか画面へ
        let resultLink = app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH '前回の結果'")).firstMatch
        XCTAssertTrue(resultLink.waitForExistence(timeout: 8), "前回の結果リンクが見つからない")
        resultLink.tap()
        Thread.sleep(forTimeInterval: 1.5)
        app.swipeUp()
        Thread.sleep(forTimeInterval: 0.5)
        snap("54-r72-result-videos")
    }
}
