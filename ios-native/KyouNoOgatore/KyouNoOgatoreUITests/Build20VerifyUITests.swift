// DO-NOT-COMMIT/TEMP-TEST: ビルド20検証用の一時テスト。検収完了後に削除する。
import XCTest

final class Build20VerifyUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    private func shotURL(_ name: String) -> URL {
        let dir = URL(fileURLWithPath: "/tmp/build20-shots", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir.appendingPathComponent("\(name).png")
    }

    // H-1/A-2/A-3: ホームカード(st短タイトル・メタ行削除)+よびな最小セット置換
    func testHomeCardAndNickname() throws {
        let app = XCUIApplication()
        app.launch()

        let searchTab = app.buttons["動画を探す"]
        XCTAssertTrue(searchTab.waitForExistence(timeout: 10), "タブバーが見つからない")

        XCUIScreen.main.screenshot().image.pngData().map { try? $0.write(to: shotURL("01-home-nickname-set")) }

        // あなた用セグメントの見出し確認
        let heading = app.staticTexts["きょうのたろうちゃんです用"]
        XCTAssertTrue(heading.waitForExistence(timeout: 5), "よびな置換後の小見出しが出ない")
    }

    // T-A/T-B: 初回4枚(地図+予告3枚)
    func testTourFirstRun4Slides() throws {
        let app = XCUIApplication()
        app.launch()

        let searchTab = app.buttons["動画を探す"]
        XCTAssertTrue(searchTab.waitForExistence(timeout: 10))
        searchTab.tap()

        let slide0 = app.staticTexts["まいにちやることは1つだけ"]
        XCTAssertTrue(slide0.waitForExistence(timeout: 5), "T-A地図スライドが出ない")
        XCUIScreen.main.screenshot().image.pngData().map { try? $0.write(to: shotURL("02-tour-first-slide0-map")) }

        app.buttons["つぎへ"].tap()
        XCTAssertTrue(app.staticTexts["悩みは相談室で質問"].waitForExistence(timeout: 5))
        XCUIScreen.main.screenshot().image.pngData().map { try? $0.write(to: shotURL("03-tour-first-slide1-soudan")) }

        app.buttons["つぎへ"].tap()
        XCTAssertTrue(app.staticTexts["オガトレ通信をのぞく"].waitForExistence(timeout: 5))
        app.buttons["つぎへ"].tap()
        let slide3 = app.staticTexts["マイ記録でふりかえる"]
        XCTAssertTrue(slide3.waitForExistence(timeout: 5))
        XCUIScreen.main.screenshot().image.pngData().map { try? $0.write(to: shotURL("04-tour-first-slide3-myrecord")) }

        // tryStartTour(タブタップ)経由はshowClosing:trueのため、4枚目の後に締めスライドが出る。
        app.buttons["つぎへ"].tap()
        let closing = app.staticTexts["これで準備ばっちり！"]
        XCTAssertTrue(closing.waitForExistence(timeout: 5), "締めスライドが出ない")
        XCUIScreen.main.screenshot().image.pngData().map { try? $0.write(to: shotURL("04b-tour-first-closing")) }

        let doneBtn = app.buttons["おわる"]
        XCTAssertTrue(doneBtn.waitForExistence(timeout: 3), "締めスライドのボタンが「おわる」になっていない")
        doneBtn.tap()
    }

    // T-B: 再生フル7枚(使い方タブから)・ジャーニーバー非表示・N/7頁表示
    func testTourReplay7Slides() throws {
        let app = XCUIApplication()
        app.launch()

        let guideTab = app.buttons["使い方"]
        XCTAssertTrue(guideTab.waitForExistence(timeout: 10))
        guideTab.tap()

        let tourLink = app.descendants(matching: .any).matching(NSPredicate(format: "label == %@", "使い方ツアー")).firstMatch
        XCTAssertTrue(tourLink.waitForExistence(timeout: 5), "使い方タブの「使い方ツアー」リンクが見つからない")
        tourLink.tap()

        let slide0 = app.staticTexts["まいにちやることは1つだけ"]
        XCTAssertTrue(slide0.waitForExistence(timeout: 5), "再生1枚目(地図)が出ない")
        // N/7頁表示・ジャーニーバー非表示の確認
        let page1 = app.staticTexts["1/7"]
        XCTAssertTrue(page1.waitForExistence(timeout: 3), "N/7頁表示(1/7)が出ない")
        XCTAssertFalse(app.staticTexts["チェック"].exists, "再生時にジャーニーバーのラベルが出てしまっている")
        XCUIScreen.main.screenshot().image.pngData().map { try? $0.write(to: shotURL("05-tour-replay-slide0-map-n1of7")) }

        app.buttons["つぎへ"].tap()
        XCTAssertTrue(app.staticTexts["まいにち1本、動画をやる"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["2/7"].waitForExistence(timeout: 3))
        XCUIScreen.main.screenshot().image.pngData().map { try? $0.write(to: shotURL("06-tour-replay-slide1-videodaily-n2of7")) }

        app.buttons["つぎへ"].tap()
        XCTAssertTrue(app.staticTexts["おわったら「きょうやった！」"].waitForExistence(timeout: 5))
        XCUIScreen.main.screenshot().image.pngData().map { try? $0.write(to: shotURL("07-tour-replay-slide2-todaydone")) }

        app.buttons["つぎへ"].tap()
        XCTAssertTrue(app.staticTexts["ためると図鑑がうまる"].waitForExistence(timeout: 5))
        XCUIScreen.main.screenshot().image.pngData().map { try? $0.write(to: shotURL("08-tour-replay-slide3-carddex")) }

        app.buttons["つぎへ"].tap()
        XCTAssertTrue(app.staticTexts["悩みは相談室で質問"].waitForExistence(timeout: 5))
        app.buttons["つぎへ"].tap()
        XCTAssertTrue(app.staticTexts["オガトレ通信をのぞく"].waitForExistence(timeout: 5))
        app.buttons["つぎへ"].tap()
        let slide6 = app.staticTexts["マイ記録でふりかえる"]
        XCTAssertTrue(slide6.waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["7/7"].waitForExistence(timeout: 3), "最終枚のN/7頁表示(7/7)が出ない")
        XCUIScreen.main.screenshot().image.pngData().map { try? $0.write(to: shotURL("09-tour-replay-slide6-myrecord-n7of7")) }

        let doneBtn = app.buttons["おわる"]
        XCTAssertTrue(doneBtn.waitForExistence(timeout: 3), "再生7枚目のボタンが「おわる」になっていない(締めスライドは出ない想定)")
    }

    // A-4: 設定「通知」セクション改称+「毎日の合図」ラベル
    func testSettingsNotifSection() throws {
        let app = XCUIApplication()
        app.launch()

        let guideTab = app.buttons["使い方"]
        XCTAssertTrue(guideTab.waitForExistence(timeout: 10))

        // マイ記録タブ経由で設定を開く(既存の導線)
        let myRecordTab = app.buttons["マイ記録"]
        XCTAssertTrue(myRecordTab.waitForExistence(timeout: 5))
        myRecordTab.tap()

        let settingsLink = app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", "設定をひらく")).firstMatch
        XCTAssertTrue(settingsLink.waitForExistence(timeout: 5), "「設定をひらく」リンクが見つからない")
        settingsLink.tap()

        let notifHeader = app.staticTexts["通知"]
        XCTAssertTrue(notifHeader.waitForExistence(timeout: 5), "「通知」セクション見出しが見つからない")
        notifHeader.tap()
        let dailyLabel = app.staticTexts["毎日の合図"]
        XCTAssertTrue(dailyLabel.waitForExistence(timeout: 5), "「毎日の合図」ラベルが見つからない")

        let nicknameLabel = app.staticTexts["よびな（にゅうりょくは じゆう）"]
        XCTAssertTrue(nicknameLabel.waitForExistence(timeout: 3), "よびな欄の見出しが見つからない")
        XCUIScreen.main.screenshot().image.pngData().map { try? $0.write(to: shotURL("10-settings-notif-and-nickname")) }
    }
}
