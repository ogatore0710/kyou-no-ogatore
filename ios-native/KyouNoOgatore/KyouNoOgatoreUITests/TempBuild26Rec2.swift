import XCTest

// TASK-C2-2026-08-05-build26-round4.md R-7/R-8/R-9検収用の一時UITest。
// 検証後は必ず削除する。
final class TempBuild26Rec2: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    private func attach(_ name: String) {
        let shot = XCUIScreen.main.screenshot()
        let attachment = XCTAttachment(screenshot: shot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    private func navigateToTourResult(_ app: XCUIApplication) {
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
    }

    // R-7: ツアー中結果画面のピンクピル+hero(ライト)。0.4s間隔で3枚撮ってふわふわ動作を記録。
    func testR7PinkPillFloatAndHeroLight() throws {
        let app = XCUIApplication()
        app.launch()
        navigateToTourResult(app)
        app.swipeUp()
        sleep(1)
        attach("19-r7-pill-hero-static-light")
        usleep(400_000)
        attach("20-r7-pill-float-frame1")
        usleep(400_000)
        attach("21-r7-pill-float-frame2")
        usleep(400_000)
        attach("22-r7-pill-float-frame3")
    }

    // R-7: ダークでピルが沈まないか確認。
    func testR7PillDark() throws {
        let app = XCUIApplication()
        app.launch()
        navigateToTourResult(app)
        app.swipeUp()
        sleep(1)
        attach("23-r7-pill-dark")
    }

    // R-8+R-9: ホーム(セグメント〜動画カード間隔+新bg)。
    func testR8R9HomeLight() throws {
        let app = XCUIApplication()
        app.launch()
        sleep(2)
        attach("24-r8r9-home-light")
    }

    // R-9: 初回チャット(新bgでA'チップが沈まないか)。
    func testR9OnboardingChatLight() throws {
        let app = XCUIApplication()
        app.launch()
        sleep(5)
        let bigChip = app.staticTexts["大きめ（いまのまま）"]
        XCTAssertTrue(bigChip.waitForExistence(timeout: 8))
        attach("25-r9-onboard-chat-light")
    }

    // R-9: かたさチェック(新bg)。
    func testR9QuizLight() throws {
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
        attach("26-r9-quiz-light")
    }

    // R-9: 設定画面(新bg)。
    func testR9SettingsLight() throws {
        let app = XCUIApplication()
        app.launch()
        let myRecordTab = app.buttons["マイ記録"]
        XCTAssertTrue(myRecordTab.waitForExistence(timeout: 5))
        myRecordTab.tap()
        sleep(1)
        var settingsBtn = app.buttons["設定をひらく"]
        if !settingsBtn.waitForExistence(timeout: 3) {
            app.scrollViews.firstMatch.swipeUp()
            sleep(1)
            settingsBtn = app.buttons["設定をひらく"]
        }
        XCTAssertTrue(settingsBtn.waitForExistence(timeout: 5))
        settingsBtn.tap()
        sleep(1)
        attach("27-r9-settings-light")
    }
}
