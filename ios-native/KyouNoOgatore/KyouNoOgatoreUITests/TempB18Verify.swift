import XCTest

final class TempB18Verify: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    private func attach(_ name: String) {
        let shot = XCUIScreen.main.screenshot()
        let attachment = XCTAttachment(screenshot: shot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    func testFullFdGuideFlow() throws {
        let app = XCUIApplication()
        app.launch()
        sleep(2)
        attach("00-onboarding-first-screen-no-4dot-bar")

        func tapChip(_ label: String) {
            let btn = app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", label)).firstMatch
            if btn.waitForExistence(timeout: 15) { btn.tap(); sleep(2) } else { attach("MISSING-\(label)") }
        }

        tapChip("ふつう")
        tapChip("ガチガチかも")
        tapChip("肩こり・首")
        tapChip("朝おきて")
        tapChip("かたさチェックをはじめる")
        sleep(1)
        attach("01-quiz-q1-4step-bar")

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
        attach("02-result-top-light-under-system-dark")

        let scroll = app.scrollViews.firstMatch
        scroll.swipeUp()
        sleep(1)
        attach("03-result-mid-dimmed-videos")
        scroll.swipeUp()
        sleep(1)
        attach("04-result-bottom-kyouyatta-button")

        let nextBtn = app.buttons.matching(NSPredicate(format: "label CONTAINS 'きょうやった'")).firstMatch
        if nextBtn.waitForExistence(timeout: 5) {
            nextBtn.tap()
            sleep(1)
            attach("05-immediately-after-tap-block-hidden")
            sleep(1)
            attach("06-card-modal-opaque-scrim")
        } else {
            attach("MISSING-kyouyatta-button")
        }

        // カードモーダルを閉じるとcloseCardAndMaybeStartTour()がtourpend/tourseenを見て
        // 0.35秒後に同一セッション内でツアーを自動開始する(次回起動時ではない)。
        let closeBtn = app.buttons.matching(NSPredicate(format: "label CONTAINS 'とじる'")).firstMatch
        if closeBtn.waitForExistence(timeout: 5) {
            closeBtn.tap()
            sleep(2)
        }
        attach("07-tour-slide1-no-fab")

        for i in 0..<8 {
            let nextTourBtn = app.buttons.matching(NSPredicate(format: "label CONTAINS 'つぎへ' OR label CONTAINS 'おわる'")).firstMatch
            if nextTourBtn.waitForExistence(timeout: 5) {
                attach("08-tour-slide-\(i)")
                nextTourBtn.tap()
                sleep(1)
            } else {
                attach("MISSING-tour-next-\(i)")
                break
            }
        }
        attach("09-tour-final-state")
    }
}
