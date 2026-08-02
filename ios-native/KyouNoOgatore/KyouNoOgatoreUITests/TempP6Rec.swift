import XCTest

final class TempP6Rec: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    func testQuizTransitionRecording() throws {
        let app = XCUIApplication()
        app.launch()

        let myRecordTab = app.buttons["マイ記録"]
        XCTAssertTrue(myRecordTab.waitForExistence(timeout: 10))
        myRecordTab.tap()
        sleep(1)

        let scrollView = app.scrollViews.firstMatch
        for _ in 0..<3 { scrollView.swipeUp() }
        sleep(1)
        let shot = XCUIScreen.main.screenshot()
        let attachment = XCTAttachment(screenshot: shot)
        attachment.name = "debug-myrecord-scrolled"
        attachment.lifetime = .keepAlways
        add(attachment)

        let recheckBtn = app.buttons["もう一回チェックする"]
        XCTAssertTrue(recheckBtn.waitForExistence(timeout: 5))
        recheckBtn.tap()
        sleep(2)

        // 外部でxcrun simctl io recordVideoが開始済みの前提。alan5の実測(録画1 フレーム75-87・
        // 126-147)がQ1(momo)→Q2(koka)遷移だったため、同じ「床にペタッとつく」→「ちょっと浮く」の
        // 順でタップし、遷移の瞬間を録画に収める。
        sleep(1)
        // QuizOptionCardは.accessibilityElement(children: .combine)でラベル+注記文を結合するため、
        // 完全一致ではなくCONTAINSで探す。
        let q1opt = app.buttons.matching(NSPredicate(format: "label CONTAINS '床にペタッとつく'")).firstMatch
        XCTAssertTrue(q1opt.waitForExistence(timeout: 5))
        q1opt.tap()
        // simctl io recordVideoがホスト側で詰まって使えなかったため、代わりにタップ直後から
        // 高頻度の連写スクリーンショットを撮り、録画のフレーム抽出と同等の検証データにする。
        for i in 0..<20 {
            let shot = XCUIScreen.main.screenshot()
            let attachment = XCTAttachment(screenshot: shot)
            attachment.name = String(format: "burst-%02d", i)
            attachment.lifetime = .keepAlways
            add(attachment)
        }
        // Q2の選択肢(色付きカード)をスクロールで表示し、混ざり無く描画されていることを確認する。
        let quizScroll = app.scrollViews.firstMatch
        quizScroll.swipeUp()
        sleep(1)
        let q2Shot = XCUIScreen.main.screenshot()
        let q2Attachment = XCTAttachment(screenshot: q2Shot)
        q2Attachment.name = "q2-options-scrolled"
        q2Attachment.lifetime = .keepAlways
        add(q2Attachment)

        let q2opt = app.buttons.matching(NSPredicate(format: "label CONTAINS 'ちょっと浮く'")).firstMatch
        XCTAssertTrue(q2opt.waitForExistence(timeout: 5))
        q2opt.tap()
        for i in 0..<20 {
            let shot = XCUIScreen.main.screenshot()
            let attachment = XCTAttachment(screenshot: shot)
            attachment.name = String(format: "burst2-%02d", i)
            attachment.lifetime = .keepAlways
            add(attachment)
        }
        quizScroll.swipeUp()
        sleep(1)
        let q3Shot = XCUIScreen.main.screenshot()
        let q3Attachment = XCTAttachment(screenshot: q3Shot)
        q3Attachment.name = "q3-options-scrolled"
        q3Attachment.lifetime = .keepAlways
        add(q3Attachment)
        sleep(1)
    }
}
