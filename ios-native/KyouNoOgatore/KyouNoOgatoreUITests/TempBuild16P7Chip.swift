import XCTest

final class TempBuild16P7Chip: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    private func attach(name: String) {
        let shot = XCUIScreen.main.screenshot()
        let attachment = XCTAttachment(screenshot: shot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    func testPinkChipSelectedContrast() throws {
        let app = XCUIApplication()
        app.launch()

        let searchTab = app.buttons["動画を探す"]
        XCTAssertTrue(searchTab.waitForExistence(timeout: 10))
        searchTab.tap()
        sleep(1)

        let mokutekiTab = app.staticTexts["目的"]
        if mokutekiTab.waitForExistence(timeout: 5) {
            mokutekiTab.tap()
            sleep(1)
        }

        let chip = app.buttons["むくみ"]
        if chip.waitForExistence(timeout: 5) {
            chip.tap()
            sleep(1)
            attach(name: "01-pink-chip-selected")
        }
    }
}
