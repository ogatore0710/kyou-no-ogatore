import XCTest

// TASK-C2-2026-08-05-build27-round5.md R-10検収用の一時UITest。
// 検証後は必ず削除する。
final class TempBuild27Rec: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    private func attach(_ name: String) {
        let shot = XCUIScreen.main.screenshot()
        let attachment = XCTAttachment(screenshot: shot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    func testReachMeterCopyLight() throws {
        let app = XCUIApplication()
        app.launch()
        let myRecordTab = app.buttons["マイ記録"]
        XCTAssertTrue(myRecordTab.waitForExistence(timeout: 5))
        myRecordTab.tap()
        sleep(1)
        app.scrollViews.firstMatch.swipeUp()
        sleep(1)
        attach("28-r10-reach-meter-copy-light")
    }
}
