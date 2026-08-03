import XCTest

final class TempGlowInvestigate: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    func testChipDragForGlowInvestigation() throws {
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
        }
        let chip = app.buttons.matching(NSPredicate(format: "label CONTAINS '肩こり・首こり'")).firstMatch
        XCTAssertTrue(chip.waitForExistence(timeout: 5))
        let start = chip.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
        let end = start.withOffset(CGVector(dx: -200, dy: 0))
        // 長押し+ゆっくりドラッグ(4秒)。外部プロセスが並列でスクリーンショットを撮る前提。
        start.press(forDuration: 4.0, thenDragTo: end)
        sleep(1)
    }
}
