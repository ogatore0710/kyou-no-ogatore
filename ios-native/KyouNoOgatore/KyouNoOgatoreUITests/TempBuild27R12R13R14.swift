import XCTest

// TASK-C2-2026-08-05-build27-round5.md R-12/R-13/R-14検収用の一時UITest。
// 検証後は必ず削除する。
final class TempBuild27R12R13R14: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    private func attach(_ name: String) {
        let shot = XCUIScreen.main.screenshot()
        let attachment = XCTAttachment(screenshot: shot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    private func tapChip(_ app: XCUIApplication, _ label: String) {
        let chip = app.staticTexts[label]
        XCTAssertTrue(chip.waitForExistence(timeout: 10), "chip not found: \(label)")
        chip.tap()
    }

    private func tapButton(_ app: XCUIApplication, _ label: String, timeout: TimeInterval = 10) {
        let btn = app.buttons[label]
        XCTAssertTrue(btn.waitForExistence(timeout: timeout), "button not found: \(label)")
        btn.tap()
    }

    private func answerQuizFiveQuestions(_ app: XCUIApplication) {
        let firstOptions = [
            "床にペタッとつく", "床にペタッと近い", "鼻より上まで上がる", "余裕でしゃがめる", "肩こり・首こり",
        ]
        for label in firstOptions {
            tapButton(app, label, timeout: 15)
            sleep(1)
        }
    }

    // ツアー中(fdGuideActive)の一気通貫: オンボ→クイズ→結果(R-12=ペース目安が出ない確認)→
    // 練習「きょうやった！」→カード(R-13=0日目+シェア案内)→ツアー4枚→ホーム着地ポップ(R-14)。
    func testTourFlowR12R13R14() throws {
        let app = XCUIApplication()
        app.launch()

        // オンボチャット4問
        tapChip(app, "ふつう") // もじの大きさ
        tapChip(app, "ガチガチかも") // かたさ(hard→quizルート)
        tapChip(app, "とくにない") // 悩み(none→presetWorryなし・クイズ側にworry設問が残る)
        tapChip(app, "朝おきて") // いつやる
        tapButton(app, "かたさチェックをはじめる", timeout: 15)

        // クイズ5問(momo/koka/kenko/ashi/worry)
        answerQuizFiveQuestions(app)

        // 結果画面(fdGuideActive=true)。スクロールしてペースの目安の有無を確認。
        sleep(1)
        let scroll = app.scrollViews.firstMatch
        XCTAssertTrue(scroll.waitForExistence(timeout: 15))
        scroll.swipeUp()
        scroll.swipeUp()
        attach("33-r12-result-tour-no-pace-card")

        // 練習ブロックまでスクロールして「きょうやった！」をタップ
        scroll.swipeUp()
        let practiceBtn = app.buttons["きょうやった！"]
        if !practiceBtn.waitForExistence(timeout: 5) {
            scroll.swipeUp()
        }
        tapButton(app, "きょうやった！", timeout: 10)

        // カードモーダル出現待ち(0.7秒遅延+アニメーション)
        sleep(2)
        attach("34-r13-practice-card-0days-share-note")

        // カードを閉じる→350ms後にツアー自動開始
        tapButton(app, "とじる", timeout: 10)
        sleep(1)

        // ツアー4枚(isFirstRun): つぎへ×3→おわる
        for _ in 0..<3 {
            tapButton(app, "つぎへ", timeout: 10)
            sleep(1)
        }
        tapButton(app, "おわる", timeout: 10)

        // ホーム着地→ツアー完走ポップ(R-14)
        sleep(1)
        attach("35-r14-tour-finished-popup")
    }

    // ツアー外(通常ユーザー)の結果画面: ペースの目安が出ることを確認(R-12の対照)。
    func testNormalResultShowsPaceCard() throws {
        let app = XCUIApplication()
        app.launch()

        tapButton(app, "チェックをはじめる", timeout: 15)
        answerQuizFiveQuestions(app)

        sleep(1)
        let scroll = app.scrollViews.firstMatch
        XCTAssertTrue(scroll.waitForExistence(timeout: 15))
        scroll.swipeUp()
        scroll.swipeUp()
        attach("36-r12-result-normal-has-pace-card")
    }
}
