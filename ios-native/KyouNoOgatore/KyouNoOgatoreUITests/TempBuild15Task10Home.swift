import XCTest

final class TempBuild15Task10Home: XCTestCase {
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

    func testHomeReorderFullScroll() throws {
        let app = XCUIApplication()
        app.launch()

        let homeTab = app.buttons["ホーム"]
        XCTAssertTrue(homeTab.waitForExistence(timeout: 10))
        sleep(1)
        attach(name: "01-top")

        let scrollView = app.scrollViews.firstMatch
        for i in 0..<6 {
            scrollView.swipeUp()
            sleep(1)
            attach(name: "0\(i + 2)-scroll")
        }
    }
}
