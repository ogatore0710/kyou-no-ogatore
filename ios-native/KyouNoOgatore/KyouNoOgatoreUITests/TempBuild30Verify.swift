import XCTest

// TASK-C2-2026-08-06-build30-round8.md 検収用の一時UITest(R-24/R-25/R-26/R-27/R-28/R-29)。
// 検証後は必ず削除する。
final class TempBuild30Verify: XCTestCase {
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

    private func answerQuizFiveQuestions(_ app: XCUIApplication) {
        let firstOptions = [
            "床にペタッとつく", "床にペタッと近い", "鼻より上まで上がる", "余裕でしゃがめる", "肩こり・首こり",
        ]
        let scroll = app.scrollViews.firstMatch
        for label in firstOptions {
            let predicate = NSPredicate(format: "label CONTAINS[c] %@", label)
            let btn = app.buttons.matching(predicate).firstMatch
            if !btn.waitForExistence(timeout: 5) { scroll.swipeUp() }
            XCTAssertTrue(btn.waitForExistence(timeout: 15), "quiz option not found: \(label)")
            btn.tap()
            sleep(1)
        }
    }

    // A: クリーン状態から。ツアー中けっか画面(R-24ボタンなし/R-26ツアー中2本/R-27写り込み)。
    func testA_tourResult() throws {
        let app = XCUIApplication()
        app.launch()
        tapChip(app, "ふつう")
        tapChip(app, "ガチガチかも")
        tapChip(app, "とくにない")
        tapChip(app, "朝おきて")
        let ctaBtn = app.buttons["かたさチェックをはじめる"]
        XCTAssertTrue(ctaBtn.waitForExistence(timeout: 15))
        ctaBtn.tap()
        answerQuizFiveQuestions(app)
        sleep(1)
        let scroll = app.scrollViews.firstMatch
        XCTAssertTrue(scroll.waitForExistence(timeout: 15))
        attach("65-r26-tour-result-top")
        scroll.swipeUp()
        sleep(1)
        attach("66-r24-tour-result-bottom-no-button")
        scroll.swipeUp()
        sleep(1)
        attach("67-r24-tour-result-bottom2")
    }

    // B: seed済(onboarded+type=momo+worry=katakori)前提。通常けっか画面(R-24通常/R-26悩みあり3本/R-27)。
    func testB_normalResultWithWorry() throws {
        let app = XCUIApplication()
        app.launch()
        let myRecordTab = app.buttons["マイ記録"]
        XCTAssertTrue(myRecordTab.waitForExistence(timeout: 15))
        myRecordTab.tap()
        let scroll = app.scrollViews.firstMatch
        let resultLink = app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH %@", "前回の結果:")).firstMatch
        for _ in 0..<6 where !resultLink.isHittable {
            scroll.swipeUp()
        }
        XCTAssertTrue(resultLink.waitForExistence(timeout: 10))
        resultLink.tap()
        sleep(1)
        let rscroll = app.scrollViews.firstMatch
        XCTAssertTrue(rscroll.waitForExistence(timeout: 15))
        rscroll.swipeUp()
        sleep(1)
        attach("68-r26-normal-result-3items-worry")
        rscroll.swipeUp()
        sleep(1)
        attach("69-r26-normal-result-bottom-rotate-note")
    }

    // C: 再生リストタブ(R-25)。
    func testC_playlist() throws {
        let app = XCUIApplication()
        app.launch()
        let tab = app.buttons["再生リスト"]
        XCTAssertTrue(tab.waitForExistence(timeout: 15))
        tab.tap()
        sleep(1)
        attach("70-r25-playlist-top")
        let scroll = app.scrollViews.firstMatch
        for _ in 0..<8 { scroll.swipeUp() }
        sleep(1)
        attach("71-r25-playlist-bottom")
    }

    // D: おおきめ(bigtext=true seed済)確認: ホームすごろく道(R-28)+けっか(R-24/R-26)。
    func testD_bigtext() throws {
        let app = XCUIApplication()
        app.launch()
        let myRecordTab = app.buttons["マイ記録"]
        XCTAssertTrue(myRecordTab.waitForExistence(timeout: 15))
        myRecordTab.tap()
        let header = app.staticTexts["続けた記録"]
        XCTAssertTrue(header.waitForExistence(timeout: 10))
        sleep(1)
        attach("72-r28-milestone-track-bigtext")
        let scroll = app.scrollViews.firstMatch
        let resultLink = app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH %@", "前回の結果:")).firstMatch
        for _ in 0..<6 where !resultLink.isHittable {
            scroll.swipeUp()
        }
        XCTAssertTrue(resultLink.waitForExistence(timeout: 10))
        resultLink.tap()
        sleep(1)
        let rscroll = app.scrollViews.firstMatch
        XCTAssertTrue(rscroll.waitForExistence(timeout: 15))
        rscroll.swipeUp()
        sleep(1)
        attach("73-r26-result-bigtext")
    }

    // E: せんぱいの声(R-29)。閉じた2枚の高さ+めくり(録画はホスト側simctlで並行取得)。
    func testE_voices() throws {
        let app = XCUIApplication()
        app.launch()
        let myRecordTab = app.buttons["マイ記録"]
        XCTAssertTrue(myRecordTab.waitForExistence(timeout: 15))
        myRecordTab.tap()
        let scroll = app.scrollViews.firstMatch
        let voicesBtn = app.buttons["せんぱいの声"]
        for _ in 0..<6 where !voicesBtn.isHittable {
            scroll.swipeUp()
        }
        XCTAssertTrue(voicesBtn.waitForExistence(timeout: 10))
        voicesBtn.tap()
        sleep(2)
        attach("74-r29-voices-closed-uniform-height")
        // 1枚目をめくる(録画側でガタつき検分)。
        let vscroll = app.scrollViews.firstMatch
        vscroll.swipeUp()
        sleep(1)
        attach("75-r29-voices-closed-2")
        // 2枚目カードをタップしてめくる
        let card = app.staticTexts["タップでめくる"].firstMatch
        if card.waitForExistence(timeout: 5) {
            card.tap()
            sleep(2)
            attach("76-r29-voices-opened")
            // とじる(再タップ)
            let back = app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH %@", "— せんぱいの声")).firstMatch
            if back.exists { back.tap() }
            sleep(2)
            attach("77-r29-voices-closed-again")
        }
    }
}
