import XCTest

final class TempQ1Q3b: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }

    private func attach(_ name: String) {
        let shot = XCUIScreen.main.screenshot()
        let attachment = XCTAttachment(screenshot: shot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    func testFdGuideFullResultFlow() throws {
        let app = XCUIApplication()
        app.launch()
        sleep(2)

        func tapChip(_ label: String) {
            let btn = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", label)).firstMatch
            if btn.waitForExistence(timeout: 15) { btn.tap(); sleep(2) } else { attach("MISSING-\(label)") }
        }

        tapChip("ふつう") // bigtext
        tapChip("ガチガチかも") // stiff
        tapChip("肩こり・首") // worry
        tapChip("朝おきて") // anchor
        attach("00-before-start-quiz")

        tapChip("かたさチェックをはじめる")
        sleep(2)
        attach("00b-after-start-quiz-tap")

        let firstOptions = ["床にペタッとつく", "床にペタッと近い", "鼻より上まで上がる", "余裕でしゃがめる", "肩こり・首こり"]
        for i in 0..<6 {
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
            attach("quiz-step-\(i)")
            if !tapped { break }
        }
        sleep(2)
        attach("01-result-top")

        let scroll = app.scrollViews.firstMatch
        scroll.swipeUp()
        sleep(1)
        attach("02-result-mid")
        scroll.swipeUp()
        sleep(1)
        attach("03-result-bottom")
        scroll.swipeUp()
        sleep(1)
        attach("03b-result-bottom2")

        let nextBtn = app.buttons.matching(NSPredicate(format: "label CONTAINS 'つぎへ'")).firstMatch
        if nextBtn.waitForExistence(timeout: 5) {
            nextBtn.tap()
            sleep(2)
            attach("04-after-practice-record")
        } else {
            attach("MISSING-next-button")
        }

        let myRecordTab = app.buttons["マイ記録"]
        if myRecordTab.waitForExistence(timeout: 5) {
            myRecordTab.tap()
            sleep(1)
            let myScroll = app.scrollViews.firstMatch
            for _ in 0..<5 { myScroll.swipeUp() }
            sleep(1)
            attach("05-myrecord-scrolled")
        }
    }
}
