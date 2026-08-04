// DO-NOT-COMMIT/TEMP-TEST: st第4ラウンド識別語裁定(2節5件戻し)後のホームst表示確認用。検収完了後に削除する。
import XCTest

final class Round4VerifyUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    private func shotURL(_ name: String) -> URL {
        let dir = URL(fileURLWithPath: "/tmp/build20-shots", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir.appendingPathComponent("\(name).png")
    }

    func testHomeStAfterRound4Revert() throws {
        let app = XCUIApplication()
        app.launch()
        let searchTab = app.buttons["動画を探す"]
        XCTAssertTrue(searchTab.waitForExistence(timeout: 10))
        usleep(300_000)
        XCUIScreen.main.screenshot().image.pngData().map { try? $0.write(to: shotURL("16-round4-home-st-plan")) }
    }
}
