import XCTest

// TASK-C2-2026-08-06-build30-round8.md R-28検証用の一時UITest。
// 検証後は必ず削除する。
final class TempBuild30R28: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    private func attach(_ name: String) {
        let shot = XCUIScreen.main.screenshot()
        let attachment = XCTAttachment(screenshot: shot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    func testMyRecordMilestoneTrack() throws {
        let app = XCUIApplication()
        app.launch()

        let myRecordTab = app.buttons["マイ記録"]
        XCTAssertTrue(myRecordTab.waitForExistence(timeout: 15))
        myRecordTab.tap()

        let header = app.staticTexts["続けた記録"]
        XCTAssertTrue(header.waitForExistence(timeout: 10))
        sleep(1)
        attach("60-r28-milestone-track")
    }
}
