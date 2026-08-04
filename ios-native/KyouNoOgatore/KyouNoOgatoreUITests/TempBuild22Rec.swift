import XCTest

// TASK-C2-2026-08-04-build22-yellow-return.md 検収差し戻し: Z-7(つづけた日数・完了/未完了)・
// Z-8(相談室カードのチップ削除)・Z-9(図鑑1枠バナー)の実描画スクショを撮る一時UITest。
// 検証後は必ず削除する(build17 Q-2/build18 P-6と同じ後始末方針)。
final class TempBuild22Rec: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    private func attach(_ name: String) {
        let shot = XCUIScreen.main.screenshot()
        let attachment = XCTAttachment(screenshot: shot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    func testZ7Z8Z9Recording() throws {
        let app = XCUIApplication()
        app.launch()

        // Z-7(未完了)+Z-8(相談室カード): ホームの「きょうの1本」下、streak/soudanカードまでスクロール。
        let scrollView = app.scrollViews.firstMatch
        XCTAssertTrue(scrollView.waitForExistence(timeout: 10))
        scrollView.swipeUp()
        sleep(1)
        attach("01-home-notdone-and-soudan")

        // Z-7(完了): 「きょうやった！」をタップして完了状態へ遷移。
        let doneBtn = app.buttons["きょうやった！"]
        if doneBtn.waitForExistence(timeout: 5) {
            doneBtn.tap()
        } else {
            // 既にスクロールで隠れている場合に備え、少し戻ってから再検索。
            scrollView.swipeDown()
            sleep(1)
            let retryBtn = app.buttons["きょうやった！"]
            XCTAssertTrue(retryBtn.waitForExistence(timeout: 5))
            retryBtn.tap()
        }
        // 労い演出(0.7秒後にカードモーダル入場+紙吹雪)を待つ。
        sleep(3)
        attach("02-card-modal-after-done")
        let closeBtn = app.buttons["とじる"]
        XCTAssertTrue(closeBtn.waitForExistence(timeout: 5))
        closeBtn.tap()
        sleep(1)
        attach("03-home-done-and-soudan")

        // Z-9: マイ記録タブへ移動し、お楽しみ機能カード(図鑑統合バナー)までスクロール。
        let myRecordTab = app.buttons["マイ記録"]
        XCTAssertTrue(myRecordTab.waitForExistence(timeout: 5))
        myRecordTab.tap()
        sleep(1)
        let myRecordScroll = app.scrollViews.firstMatch
        XCTAssertTrue(myRecordScroll.waitForExistence(timeout: 5))
        myRecordScroll.swipeUp()
        sleep(1)
        attach("04a-myrecord-funcard-dexbanner")
        myRecordScroll.swipeUp()
        sleep(1)
        attach("04b-myrecord-funcard-dexbanner")
    }
}
