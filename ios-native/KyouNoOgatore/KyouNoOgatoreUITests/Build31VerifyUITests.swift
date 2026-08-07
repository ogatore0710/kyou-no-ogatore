//
//  Build31VerifyUITests.swift
//  KyouNoOgatore
//
//  build35 R-54/R-55の実描画検収用(オンボ4問チャット・色連動+タイピング演出)。撮影後に削除する。
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

    func testOnboardingChatVerify() throws {
        let app = XCUIApplication()
        app.launch()
        let prefix = ProcessInfo.processInfo.environment["SNAP_PREFIX"] ?? "45-r5455"

        // R-55: あいさつ表示中(タイピングドットが出ているはずのタイミング)を狙って撮影。
        Thread.sleep(forTimeInterval: 0.6)
        snap("\(prefix)-typing-greet")
        Thread.sleep(forTimeInterval: 6.0)
        snap("\(prefix)-greet-done")

        // 最初の設問(もじの大きさ)のチップが出るまで待つ。
        let chip = app.staticTexts["大きめ（いまのまま）"].firstMatch
        XCTAssertTrue(chip.waitForExistence(timeout: 8), "「大きめ」チップが出ない")
        snap("\(prefix)-chips-shown")
        chip.tap()
        // R-55: タップ直後はタイピングドットのはず(i=0→400ms)。
        Thread.sleep(forTimeInterval: 0.15)
        snap("\(prefix)-typing-after-tap")
        // R-54: 実文に差し替わった後、回答吹き出しの色がチップと同じになっているはず。
        Thread.sleep(forTimeInterval: 1.5)
        snap("\(prefix)-answer-bubble")
    }
}
