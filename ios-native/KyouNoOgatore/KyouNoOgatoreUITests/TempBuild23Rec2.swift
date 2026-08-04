import XCTest

// TASK-C2-2026-08-05-build23-bg-tuning-and-tour-tap.md 差し戻し対応(W-5/W-6再検収)用の一時UITest。
// 検証後は必ず削除する。
final class TempBuild23Rec2: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    private func attach(_ name: String) {
        let shot = XCUIScreen.main.screenshot()
        let attachment = XCTAttachment(screenshot: shot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    // 押下中(press-and-hold)の数フレームを撮る定石: press(forDuration:)をバックグラウンドで
    // 走らせつつ、メインスレッド側で少し待ってからスクリーンショットを複数枚撮る。
    private func captureDuringPress(_ element: XCUIElement, namePrefix: String, holdSeconds: Double = 1.6) {
        let exp = expectation(description: "press-\(namePrefix)")
        DispatchQueue.global().async {
            element.press(forDuration: holdSeconds)
            exp.fulfill()
        }
        usleep(300_000)
        attach("\(namePrefix)-press-1")
        usleep(400_000)
        attach("\(namePrefix)-press-2")
        usleep(400_000)
        attach("\(namePrefix)-press-3")
        wait(for: [exp], timeout: holdSeconds + 3)
        usleep(300_000)
        attach("\(namePrefix)-released")
    }

    // W-5(相談室placeholder実描画)+W-6(相談室チップの押下ハロー)。
    func testSoudanChipPressHalo() throws {
        let app = XCUIApplication()
        app.launch()
        let searchTab = app.buttons["動画を探す"]
        XCTAssertTrue(searchTab.waitForExistence(timeout: 5))
        searchTab.tap()
        sleep(1)
        let fab = app.buttons["オガトレ相談室"]
        XCTAssertTrue(fab.waitForExistence(timeout: 5))
        fab.tap()
        sleep(1)
        // W-5: プレースホルダが読めることの実描画(押す前の素の状態)。
        attach("w5-soudan-placeholder")

        let chip = app.buttons["肩こり・首こり"]
        XCTAssertTrue(chip.waitForExistence(timeout: 5))
        captureDuringPress(chip, namePrefix: "w6-soudan-chip")
    }

    // W-6(かたさチェック選択肢の押下ハロー)。
    func testQuizOptionPressHalo() throws {
        let app = XCUIApplication()
        app.launch()
        let guideTab = app.buttons["使い方"]
        XCTAssertTrue(guideTab.waitForExistence(timeout: 5))
        guideTab.tap()
        sleep(1)
        app.scrollViews.firstMatch.swipeUp()
        sleep(1)
        let startQuizBtn = app.buttons["チェックをはじめる"]
        XCTAssertTrue(startQuizBtn.waitForExistence(timeout: 5))
        startQuizBtn.tap()
        sleep(1)

        let opt = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "床にペタッとつく")).firstMatch
        XCTAssertTrue(opt.waitForExistence(timeout: 5))
        captureDuringPress(opt, namePrefix: "w6-quiz-option")
    }
}
