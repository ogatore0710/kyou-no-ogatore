import XCTest

// TASK-C2-2026-08-05-build29-round7.md R-20/R-21/R-22検収用の一時UITest。
// 検証後は必ず削除する。
final class TempBuild29R20R21R22: XCTestCase {
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

    func testR20R21R22() throws {
        let app = XCUIApplication()
        app.launch()

        // R-22: 最初のチャットチップ画面(文字black900化の確認)。
        let firstChip = app.staticTexts["ふつう"]
        XCTAssertTrue(firstChip.waitForExistence(timeout: 15))
        attach("52-r22-chip-black900")

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
        practiceBtn.tap()

        let closeBtn = app.buttons["とじる"]
        XCTAssertTrue(closeBtn.waitForExistence(timeout: 10))
        closeBtn.tap()

        // ツアーstep5(みどころ)へ着地。1枚目=地図。
        let nextBtn = app.buttons["つぎへ"]
        XCTAssertTrue(nextBtn.waitForExistence(timeout: 15))
        sleep(1)

        // 2枚目=相談室(R-21)。
        nextBtn.tap()
        sleep(1)
        attach("53-r21-soudan-mockup")

        // 3枚目=オガトレ通信(R-21)。
        nextBtn.tap()
        sleep(1)
        attach("54-r21-obu-mockup")

        // 4枚目=マイ記録(R-21)。
        nextBtn.tap()
        sleep(1)
        attach("55-r21-myrecord-mockup")

        // 締めスライドへ→おわるでホームへ。
        nextBtn.tap()
        sleep(1)
        let doneBtn = app.buttons["おわる"]
        XCTAssertTrue(doneBtn.waitForExistence(timeout: 10))
        doneBtn.tap()

        // R-20: ホームの「つづけた日数」数字(black900化の確認)。
        sleep(1)
        let streakLabel = app.staticTexts["つづけた日数"]
        XCTAssertTrue(streakLabel.waitForExistence(timeout: 15))
        attach("56-r20-streak-number-black900")
    }
}
