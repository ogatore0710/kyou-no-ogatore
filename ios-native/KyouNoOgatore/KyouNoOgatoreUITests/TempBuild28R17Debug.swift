import XCTest

// TASK-C2-2026-08-05-build28-round6.md R-17原因調査用の一時UITest。
// 検証後は必ず削除する。
final class TempBuild28R17Debug: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    private func attach(_ name: String) {
        let shot = XCUIScreen.main.screenshot()
        let attachment = XCTAttachment(screenshot: shot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    func testOpenSoudan() throws {
        let app = XCUIApplication()
        app.launch()

        let searchTab = app.buttons["動画を探す"]
        XCTAssertTrue(searchTab.waitForExistence(timeout: 10), "search tab not found")
        searchTab.tap()
        sleep(1)

        let fab = app.buttons["オガトレ相談室"]
        XCTAssertTrue(fab.waitForExistence(timeout: 10), "soudan fab not found")
        fab.tap()

        sleep(2)
        attach("debug-soudan-input-row")
    }
}
