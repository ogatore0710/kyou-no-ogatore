//
//  Build31VerifyUITests.swift
//  KyouNoOgatore
//
//  build34 R-56(せんぱいの声コメント全文表示の実態調査)用の実描画検収。撮影後に削除する。
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

    func testVoicesInspect() throws {
        let app = XCUIApplication()
        app.launch()
        let prefix = ProcessInfo.processInfo.environment["SNAP_PREFIX"] ?? "40-r56"

        tapAny(app, "マイ記録", timeout: 15)
        Thread.sleep(forTimeInterval: 1.0)
        app.swipeUp(); app.swipeUp()
        Thread.sleep(forTimeInterval: 0.5)
        tapAny(app, "せんぱいの声", timeout: 8)
        Thread.sleep(forTimeInterval: 1.0)
        snap("\(prefix)-voices-top")

        // 最初のカードをタップしてめくる(裏面=コメント全文の表示確認)
        let card = app.staticTexts["タップでめくる"].firstMatch
        if card.waitForExistence(timeout: 5) {
            card.tap()
        }
        Thread.sleep(forTimeInterval: 1.0)
        snap("\(prefix)-voices-flipped")
    }
}
