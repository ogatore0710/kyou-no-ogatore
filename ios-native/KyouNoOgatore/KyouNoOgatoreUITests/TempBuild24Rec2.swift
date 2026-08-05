import XCTest

// TASK-C2-2026-08-05-build24-chip-clarity.md 検収追加依頼(Q1の4枚全部+note行のink確認)用の
// 一時UITest。検証後は必ず削除する。
final class TempBuild24Rec2: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    private func attach(_ name: String) {
        let shot = XCUIScreen.main.screenshot()
        let attachment = XCTAttachment(screenshot: shot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    func testQuizQ1AllOptionsLight() throws {
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
        // Q1(momo「立って前屈」)は4択: 床にペタッとつく/つま先にさわれる/すねの途中まで/
        // ひざから下に行かない。写真+説明文の高さがあるため複数回スワイプして4枚全部を画角に収める。
        app.swipeUp()
        sleep(1)
        app.swipeUp()
        sleep(1)
        attach("12-quiz-q1-all-options-light")
    }
}
