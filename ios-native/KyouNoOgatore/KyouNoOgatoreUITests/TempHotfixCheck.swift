import XCTest

final class TempHotfixCheck: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    private func attach(_ name: String) {
        let shot = XCUIScreen.main.screenshot()
        let attachment = XCTAttachment(screenshot: shot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    func testResultScreenFollowsAppTheme() throws {
        let app = XCUIApplication()
        app.launch()
        sleep(2)

        // ResultViewのテーマ食い違いバグはfdGuideActiveと無関係(KyonoTheme自体が常に"auto"固定
        // されていた)ため、オンボ全体を通す必要はなく「もう一回チェックする」の再チェック経路で
        // 十分再現できる。
        let myRecordTab = app.buttons["マイ記録"]
        XCTAssertTrue(myRecordTab.waitForExistence(timeout: 10))
        myRecordTab.tap()
        sleep(1)
        let myScroll = app.scrollViews.firstMatch
        for _ in 0..<5 { myScroll.swipeUp() }
        sleep(1)
        let recheckBtn = app.buttons.matching(NSPredicate(format: "label CONTAINS 'もう一回チェックする'")).firstMatch
        XCTAssertTrue(recheckBtn.waitForExistence(timeout: 5))
        recheckBtn.tap()
        sleep(1)

        let firstOptions = ["床にペタッとつく", "床にペタッと近い", "鼻より上まで上がる", "余裕でしゃがめる", "肩こり・首こり"]
        for _ in 0..<6 {
            var tapped = false
            for label in firstOptions {
                let btn = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", label)).firstMatch
                if btn.waitForExistence(timeout: 4) {
                    btn.tap()
                    sleep(1)
                    tapped = true
                    break
                }
            }
            if !tapped { break }
        }
        sleep(2)
        attach("01-result-top-light-system-dark")

        let scroll = app.scrollViews.firstMatch
        scroll.swipeUp()
        sleep(1)
        attach("02-result-mid-light-system-dark")
        scroll.swipeUp()
        sleep(1)
        attach("03-result-bottom-light-system-dark")

        let nextBtn = app.buttons.matching(NSPredicate(format: "label CONTAINS 'つぎへ'")).firstMatch
        if nextBtn.waitForExistence(timeout: 5) {
            nextBtn.tap()
            sleep(2)
            attach("04-celebration-card-light-system-dark")
        }
    }
}
