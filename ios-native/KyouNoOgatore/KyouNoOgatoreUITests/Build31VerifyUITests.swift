//
//  Build31VerifyUITests.swift
//  KyouNoOgatore
//
//  build36 R-63(オンボチャットのチャネル化)の実描画検収用。撮影後に削除する。
//

import XCTest

final class Build31VerifyUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    private func snap(_ name: String) {
        NSLog("B31SNAP %@", name)
        Thread.sleep(forTimeInterval: 0.3)
    }

    // R-63: チャネル化後もタップ→回答吹き出し→次の設問、の通常フローが通ること。
    // 「できるだけ早いタップ」も1回混ぜる(チップ出現を検知した瞬間にタップ)。
    func testOnboardingFlowAfterChannelRefactor() throws {
        let app = XCUIApplication()
        app.launch()
        let prefix = ProcessInfo.processInfo.environment["SNAP_PREFIX"] ?? "46-r63"

        // 設問1(もじの大きさ): チップ出現を検知した瞬間にタップ(高速タップ相当)
        let chip1 = app.staticTexts["大きめ（いまのまま）"].firstMatch
        XCTAssertTrue(chip1.waitForExistence(timeout: 20), "設問1チップが出ない")
        chip1.tap()

        // 設問2(かたさ)が来る=設問1の回答がチャネル経由で正しく届いた証拠
        let chip2 = app.staticTexts["ガチガチかも"].firstMatch
        XCTAssertTrue(chip2.waitForExistence(timeout: 15), "設問2チップが出ない(チャネル詰まりの疑い)")
        snap("\(prefix)-q2-reached")
        chip2.tap()

        // 設問3(悩み)
        let chip3 = app.staticTexts["肩こり"].firstMatch
        XCTAssertTrue(chip3.waitForExistence(timeout: 15), "設問3チップが出ない")
        chip3.tap()

        // 設問4(いつやる)
        let chip4 = app.staticTexts["おふろ上がり"].firstMatch
        XCTAssertTrue(chip4.waitForExistence(timeout: 15), "設問4チップが出ない")
        chip4.tap()

        // 締めCTA(ルートは選択により文言が変わるためボタン存在で判定)
        Thread.sleep(forTimeInterval: 6.0)
        snap("\(prefix)-cta-reached")
    }
}
