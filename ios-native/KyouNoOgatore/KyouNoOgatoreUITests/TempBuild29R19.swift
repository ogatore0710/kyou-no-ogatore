import XCTest

// TASK-C2-2026-08-05-build29-round7.md R-19検収用の一時UITest。
// 検証後は必ず削除する。
final class TempBuild29R19: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    private func attach(_ name: String) {
        let shot = XCUIScreen.main.screenshot()
        let attachment = XCTAttachment(screenshot: shot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    private func tapChip(_ app: XCUIApplication, _ label: String) {
        let chip = app.staticTexts[label]
        XCTAssertTrue(chip.waitForExistence(timeout: 10), "chip not found: \(label)")
        chip.tap()
    }

    private func answerQuizFiveQuestions(_ app: XCUIApplication) {
        let firstOptions = [
            "床にペタッとつく", "床にペタッと近い", "鼻より上まで上がる", "余裕でしゃがめる", "肩こり・首こり",
        ]
        let scroll = app.scrollViews.firstMatch
        for label in firstOptions {
            let predicate = NSPredicate(format: "label CONTAINS[c] %@", label)
            let btn = app.buttons.matching(predicate).firstMatch
            if !btn.waitForExistence(timeout: 5) {
                scroll.swipeUp()
            }
            XCTAssertTrue(btn.waitForExistence(timeout: 15), "quiz option not found: \(label)")
            btn.tap()
            sleep(1)
        }
    }

    func testTourResultPracticeBlockButtonOnly() throws {
        let app = XCUIApplication()
        app.launch()

        tapChip(app, "ふつう")
        tapChip(app, "ガチガチかも")
        tapChip(app, "とくにない")
        tapChip(app, "朝おきて")
        let ctaBtn = app.buttons["かたさチェックをはじめる"]
        XCTAssertTrue(ctaBtn.waitForExistence(timeout: 15))
        ctaBtn.tap()

        answerQuizFiveQuestions(app)

        sleep(1)
        let scroll = app.scrollViews.firstMatch
        XCTAssertTrue(scroll.waitForExistence(timeout: 15))
        scroll.swipeUp()
        let practiceBtn = app.buttons["きょうやった！"]
        if !practiceBtn.waitForExistence(timeout: 5) { scroll.swipeUp() }
        XCTAssertTrue(practiceBtn.waitForExistence(timeout: 10))

        // R-19: ボタン周辺(旧ピル+2行が消えたことの確認)。
        attach("51-r19-practice-block-button-only")
    }
}
