import XCTest

// TASK-C2-2026-08-05-build28-round6.md R-18検収用の一時UITest。
// 検証後は必ず削除する。
final class TempBuild28R18: XCTestCase {
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

    func testTourCardTransitionsNoFlash() throws {
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

        // ①カード出現直後を連写(労い文字の透け・背後透けが写っていないことの確認)。
        practiceBtn.tap()
        for i in 1...8 {
            attach("46-r18-card-appear-\(i)")
        }

        // ②とじた直後を連写(出戻り「おかえり！」画面が写っていないことの確認)。
        let closeBtn = app.buttons["とじる"]
        XCTAssertTrue(closeBtn.waitForExistence(timeout: 10))
        closeBtn.tap()
        for i in 1...8 {
            attach("47-r18-card-close-\(i)")
        }

        // 最終的にツアー(みどころ=カードデックス等)へ到達していることの確認。
        sleep(1)
        attach("48-r18-tour-landed")
    }
}
