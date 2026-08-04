// DO-NOT-COMMIT/TEMP-TEST: ビルド20 F-1/F-2差し戻し確認用。検収完了後に削除する。
import XCTest

final class Build20F1F2UITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    private func shotURL(_ name: String) -> URL {
        let dir = URL(fileURLWithPath: "/tmp/build20-shots", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir.appendingPathComponent("\(name).png")
    }

    // F-1: stの2行表示確認(最長st・rVhg9qRQFh8「ポールで背骨と肩甲骨をほぐす15分ストレッチ」)
    func testHomeCardTwoLineSt() throws {
        let app = XCUIApplication()
        app.launch()
        let searchTab = app.buttons["動画を探す"]
        XCTAssertTrue(searchTab.waitForExistence(timeout: 10))
        usleep(300_000)
        XCUIScreen.main.screenshot().image.pngData().map { try? $0.write(to: shotURL("12-f1-home-longest-st-2line")) }
    }

    // F-2: よびな6文字ちょうど・タブ/小見出し/結果画面/連続再生ボタンの1行固定+自動縮小確認
    func testNicknameSixChars() throws {
        let app = XCUIApplication()
        app.launch()
        let searchTab = app.buttons["動画を探す"]
        XCTAssertTrue(searchTab.waitForExistence(timeout: 10))
        usleep(300_000)
        XCUIScreen.main.screenshot().image.pngData().map { try? $0.write(to: shotURL("13-f2-home-nickname6-tab-heading")) }

        // 結果画面: type/fdは既にseedで設定済み想定。ここではホームからは辿らず、
        // ストア済みtypeでResultViewへの直接遷移導線が無いため、設定画面のよびな欄だけ再確認する。
        let myRecordTab = app.buttons["マイ記録"]
        XCTAssertTrue(myRecordTab.waitForExistence(timeout: 5))
        myRecordTab.tap()
        let settingsLink = app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", "設定をひらく")).firstMatch
        XCTAssertTrue(settingsLink.waitForExistence(timeout: 5))
        settingsLink.tap()
        let nicknameField = app.staticTexts["よびな（にゅうりょくは じゆう・6もじまで）"]
        XCTAssertTrue(nicknameField.waitForExistence(timeout: 5))
        XCUIScreen.main.screenshot().image.pngData().map { try? $0.write(to: shotURL("14-f2-settings-nickname6-label")) }
    }
}
