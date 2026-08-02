import XCTest

final class TempBuild16P8Cal: XCTestCase {
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

    func testCalendarFutureDayColor() throws {
        let app = XCUIApplication()
        app.launch()

        let myRecordTab = app.buttons["マイ記録"]
        XCTAssertTrue(myRecordTab.waitForExistence(timeout: 10))
        myRecordTab.tap()
        sleep(1)
        let scrollView = app.scrollViews.firstMatch
        scrollView.swipeUp()
        sleep(1)
        attach(name: "01-calendar")
    }
}
