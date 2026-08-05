import XCTest

// TASK-C2-2026-08-05-build24-chip-clarity.md 検収用の一時UITest。
// 検証後は必ず削除する。
final class TempBuild24Rec: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    private func attach(_ name: String) {
        let shot = XCUIScreen.main.screenshot()
        let attachment = XCTAttachment(screenshot: shot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    // R-1: フレッシュな初回チャット(onboarded=false)を通し、bigtext→stiff(かたさ)→
    // worry(部位5択)→anchor(時間帯)の全チップ画面+新パレットを撮影。
    func testOnboardingChatChips() throws {
        let app = XCUIApplication()
        app.launch()
        sleep(5) // 挨拶3行のタイプ演出+自動スクロール待ち
        attach("01-onboard-bigtext")

        // ObChip行はButtonではなくHStack+.onTapGestureのため、XCUITest上はstaticTextsに現れる。
        let bigChip = app.staticTexts["大きめ（いまのまま）"]
        XCTAssertTrue(bigChip.waitForExistence(timeout: 8))
        bigChip.tap()
        sleep(4)
        attach("02-onboard-stiff")

        let hardChip = app.staticTexts["ガチガチかも"]
        XCTAssertTrue(hardChip.waitForExistence(timeout: 8))
        hardChip.tap()
        sleep(4)
        attach("03-onboard-worry")

        let worryChip = app.staticTexts["肩こり・首"]
        XCTAssertTrue(worryChip.waitForExistence(timeout: 8))
        worryChip.tap()
        sleep(4)
        attach("04-onboard-anchor")

        let anchorChip = app.staticTexts["朝おきて"]
        XCTAssertTrue(anchorChip.waitForExistence(timeout: 8))
        anchorChip.tap()
        sleep(4)
        attach("05-onboard-after-anchor")
    }

    // R-1: かたさチェックQ1-Q4の段階色カード(新パレット+ink化されたnote)。
    func testQuizOptionNewPalette() throws {
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
        attach("06-quiz-q1-new-palette")

        let opt = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "床にペタッとつく")).firstMatch
        XCTAssertTrue(opt.waitForExistence(timeout: 5))
        opt.tap()
        sleep(1)
        attach("07-quiz-q2-new-palette")
    }

    // R-1押下状態: チップをホールドしてyellowSoft遷移を撮影(かたさチェック選択肢)。
    func testQuizOptionPressTransition() throws {
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
        let lock = NSLock()
        var mid: [(Double, XCUIScreenshot)] = []
        for delay in [0.3, 0.7] {
            DispatchQueue.global().asyncAfter(deadline: .now() + delay) {
                let shot = XCUIScreen.main.screenshot()
                lock.lock(); mid.append((delay, shot)); lock.unlock()
            }
        }
        opt.press(forDuration: 1.2)
        Thread.sleep(forTimeInterval: 0.3)
        lock.lock()
        let sorted = mid.sorted { $0.0 < $1.0 }
        lock.unlock()
        for (delay, shot) in sorted {
            let a = XCTAttachment(screenshot: shot)
            a.name = "08-quiz-press-\(Int(delay * 1000))ms"
            a.lifetime = .keepAlways
            add(a)
        }
    }

    // R-2: fdGuideActive中の結果画面(練習ピル+優しい2行)をライトで撮影。
    func testPracticePillLight() throws {
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
        attach("09-practice-pill-light")
    }

    // R-2: 同上をダークで撮影(ピルが沈んでいないか)。
    func testPracticePillDark() throws {
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
        attach("10-quiz-q1-dark-unchanged")

        let answers = ["床にペタッとつく", "床にペタッと近い", "鼻より上まで上がる", "余裕でしゃがめる", "肩こり・首こり"]
        for ans in answers {
            let opt = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", ans)).firstMatch
            XCTAssertTrue(opt.waitForExistence(timeout: 5), "option not found: \(ans)")
            opt.tap()
            sleep(1)
        }
        attach("11-practice-pill-dark")
    }
}
