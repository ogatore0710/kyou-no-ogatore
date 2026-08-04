import XCTest

// TASK-C2-2026-08-05-build23-bg-tuning-and-tour-tap.md 検収用の一時UITest。
// 検証後は必ず削除する(build17 Q-2/build18 P-6/build22と同じ後始末方針)。
final class TempBuild23Rec: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    private func attach(_ name: String) {
        let shot = XCUIScreen.main.screenshot()
        let attachment = XCTAttachment(screenshot: shot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    // W-7(起動スプラッシュ)+W-3/W-4(使い方タブのグラデカード・目次チップ)+W-1(ツアー地図モック)。
    func testSplashGuideAndTourMap() throws {
        let app = XCUIApplication()
        app.launch()
        // W-7: 起動直後(スプラッシュがまだ出ている可能性が高いタイミング)を撮る。
        attach("01-launch-splash")
        sleep(1)
        attach("02-home-after-splash")

        let guideTab = app.buttons["使い方"]
        XCTAssertTrue(guideTab.waitForExistence(timeout: 5))
        guideTab.tap()
        sleep(1)
        // W-3(グラデカード)+W-4(目次チップ)。
        attach("03-guide-gradient-and-chips")

        let tourLink = app.staticTexts["使い方ツアー"]
        XCTAssertTrue(tourLink.waitForExistence(timeout: 5))
        tourLink.tap()
        sleep(1)
        // W-1: ツアー1枚目=地図モック。
        attach("04-tour-map-slide")
    }

    // W-8(ダーク階層化): ホーム動画行・セグメントノブ+設定のセグメント/時分ピッカー。
    func testDarkHierarchy() throws {
        let app = XCUIApplication()
        app.launch()
        sleep(1)
        attach("05-home-dark")

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
        attach("06-settings-dark")
    }

    // W-2: フレッシュな状態からチェック→結果画面で1本目だけタップ可を確認。
    func testQuizResultFirstVideoTappable() throws {
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
        attach("07-result-before-tap")

        let firstVideo = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "①まずほぐす")).firstMatch
        if firstVideo.waitForExistence(timeout: 5) {
            firstVideo.tap()
            sleep(1)
            attach("08-result-notice-shown")
        }
    }
}
