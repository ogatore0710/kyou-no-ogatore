import XCTest

final class TempP5Debug: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    func testSoudanChipRowDebug() throws {
        let app = XCUIApplication()
        app.launch()
        let searchTab = app.buttons["動画を探す"]
        XCTAssertTrue(searchTab.waitForExistence(timeout: 10))
        searchTab.tap()
        sleep(1)
        let soudanFab = app.buttons["オガトレ相談室"]
        if soudanFab.waitForExistence(timeout: 5) {
            soudanFab.tap()
            sleep(2)
            let shot = XCUIScreen.main.screenshot()
            let attachment = XCTAttachment(screenshot: shot)
            attachment.name = "debug-chip-row"
            attachment.lifetime = .keepAlways
            add(attachment)
        }
    }
}
