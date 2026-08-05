import XCTest

// TASK-C2-2026-08-05-build25-tour-round3.md R-5検収用の一時UITest。
// 検証後は必ず削除する。
final class TempBuild25Rec2: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    private func attach(_ name: String) {
        let shot = XCUIScreen.main.screenshot()
        let attachment = XCTAttachment(screenshot: shot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    // 結果カード(.softグラデ)のライト実描画。
    func testResultCardGradientLight() throws {
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

        let answers = ["床にペタッとつく", "床にペタッと近い", "鼻より上まで上がる", "余裕でしゃがめる", "肩こり・首こり"]
        for ans in answers {
            let opt = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", ans)).firstMatch
            XCTAssertTrue(opt.waitForExistence(timeout: 5), "option not found: \(ans)")
            opt.tap()
            sleep(1)
        }
        attach("16-result-card-soft-gradient-light")
    }
}
