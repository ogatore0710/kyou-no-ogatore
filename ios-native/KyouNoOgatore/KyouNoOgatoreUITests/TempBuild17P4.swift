import XCTest

final class TempBuild17P4: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    private func attach(_ name: String) {
        let shot = XCUIScreen.main.screenshot()
        let attachment = XCTAttachment(screenshot: shot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    func testInputFieldsFollowAppTheme() throws {
        let app = XCUIApplication()
        app.launch()

        let searchTab = app.buttons["動画を探す"]
        XCTAssertTrue(searchTab.waitForExistence(timeout: 10))
        searchTab.tap()
        sleep(1)

        let soudanFab = app.buttons["オガトレ相談室"]
        if soudanFab.waitForExistence(timeout: 5) {
            soudanFab.tap()
            sleep(1)
            attach("01-soudan-input-fixed")
        }

        // Close sheet, go to MyRecord -> Settings.
        let closeBtn = app.buttons["とじる"]
        if closeBtn.waitForExistence(timeout: 3) { closeBtn.tap(); sleep(1) }

        let myRecordTab = app.buttons["マイ記録"]
        if myRecordTab.waitForExistence(timeout: 5) {
            myRecordTab.tap()
            sleep(1)
            let scrollView = app.scrollViews.firstMatch
            scrollView.swipeUp()
            scrollView.swipeUp()
            scrollView.swipeUp()
            scrollView.swipeUp()
            sleep(1)
            let settingsBtn = app.buttons["設定をひらく"]
            if settingsBtn.waitForExistence(timeout: 5) {
                settingsBtn.tap()
                sleep(1)
                let settingsScroll = app.scrollViews.firstMatch
                settingsScroll.swipeUp()
                settingsScroll.swipeUp()
                sleep(1)
                attach("02-settings-import-field-fixed")
            }
        }
    }
}
