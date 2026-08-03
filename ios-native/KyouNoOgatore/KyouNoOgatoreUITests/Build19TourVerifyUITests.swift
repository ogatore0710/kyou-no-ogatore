// DO-NOT-COMMIT/TEMP-TEST: ビルド19検証用の一時テスト。検収完了後に削除する。
import XCTest

final class Build19TourVerifyUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testFullPracticeToTourWalkthrough() throws {
        let app = XCUIApplication()
        app.launch()

        let startQuizBtn = app.buttons["チェックをはじめる"]
        XCTAssertTrue(startQuizBtn.waitForExistence(timeout: 10), "ホームの「チェックをはじめる」が見つからない")
        startQuizBtn.tap()

        let firstOptions = ["床にペタッとつく", "床にペタッと近い", "鼻より上まで上がる", "余裕でしゃがめる", "肩こり・首こり"]
        for label in firstOptions {
            let opt = app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", label)).firstMatch
            XCTAssertTrue(opt.waitForExistence(timeout: 5), "選択肢「\(label)」が見つからない")
            opt.tap()
            usleep(400_000)
        }

        let practiceBtn = app.buttons["きょうやった！"]
        XCTAssertTrue(practiceBtn.waitForExistence(timeout: 10), "結果画面の「きょうやった！」ボタンが見つからない")
        XCUIScreen.main.screenshot().image.pngData().map { try? $0.write(to: shotURL("00-result-before-tap")) }
        practiceBtn.tap()

        let closeBtn = app.buttons["とじる"].firstMatch
        XCTAssertTrue(closeBtn.waitForExistence(timeout: 5), "カードモーダルの「とじる」が見つからない")
        usleep(500_000)
        XCUIScreen.main.screenshot().image.pngData().map { try? $0.write(to: shotURL("01-card-modal-journey-bar")) }
        closeBtn.tap()

        // closeCardAndMaybeStartTour: 0.35s後にツアー起動
        usleep(600_000)
        let slide0Title = app.staticTexts["悩みは相談室で質問"]
        XCTAssertTrue(slide0Title.waitForExistence(timeout: 5), "ツアー1枚目「悩みは相談室で質問」が出ない")
        XCUIScreen.main.screenshot().image.pngData().map { try? $0.write(to: shotURL("02-tour-slide0-soudan")) }

        let nextBtn = app.buttons["つぎへ"]
        XCTAssertTrue(nextBtn.waitForExistence(timeout: 3))
        nextBtn.tap()
        let slide1Title = app.staticTexts["オガトレ通信をのぞく"]
        XCTAssertTrue(slide1Title.waitForExistence(timeout: 5), "ツアー2枚目「オガトレ通信をのぞく」が出ない")
        XCUIScreen.main.screenshot().image.pngData().map { try? $0.write(to: shotURL("03-tour-slide1-obu")) }

        app.buttons["つぎへ"].tap()
        let slide2Title = app.staticTexts["マイ記録でふりかえる"]
        XCTAssertTrue(slide2Title.waitForExistence(timeout: 5), "ツアー3枚目「マイ記録でふりかえる」が出ない")
        XCUIScreen.main.screenshot().image.pngData().map { try? $0.write(to: shotURL("04-tour-slide2-myrecord")) }

        // closeCardAndMaybeStartTour経由(1日目fdGuide)はonStartTour()がshowClosing:falseで
        // 呼ばれるため、この経路には締めスライドが無い(3枚のみ)。3枚目のボタンは既に「おわる」。
        let doneBtn = app.buttons["おわる"]
        XCTAssertTrue(doneBtn.waitForExistence(timeout: 3), "3枚目のボタンが「おわる」になっていない(showClosing:falseの想定と違う)")
        doneBtn.tap()
    }

    // showClosing:trueの経路(HomeView側の通常復帰ユーザー・tryStartTour(line250)/
    // 通常onStartTour(showClosing引数あり))は別途RecordStoreのtourpend/tourseenを直接
    // 書き換えて再現し、締めスライドを検証する(呼び出し元テスト外でstoreを事前に書き換える)。
    func testClosingSlideViaTabTap() throws {
        let app = XCUIApplication()
        app.launch()

        let searchTab = app.buttons["動画を探す"]
        XCTAssertTrue(searchTab.waitForExistence(timeout: 10), "タブバーが見つからない")
        searchTab.tap()

        let slide0Title = app.staticTexts["悩みは相談室で質問"]
        XCTAssertTrue(slide0Title.waitForExistence(timeout: 5), "タブタップ経由でツアーが起動しない(tourpend未消費?)")
        XCUIScreen.main.screenshot().image.pngData().map { try? $0.write(to: shotURL("06-tour-tabtap-slide0")) }

        app.buttons["つぎへ"].tap()
        XCTAssertTrue(app.staticTexts["オガトレ通信をのぞく"].waitForExistence(timeout: 5))
        app.buttons["つぎへ"].tap()
        XCTAssertTrue(app.staticTexts["マイ記録でふりかえる"].waitForExistence(timeout: 5))
        XCUIScreen.main.screenshot().image.pngData().map { try? $0.write(to: shotURL("07-tour-tabtap-slide2")) }

        app.buttons["つぎへ"].tap()
        let closingTitle = app.staticTexts["これで準備ばっちり！"]
        XCTAssertTrue(closingTitle.waitForExistence(timeout: 5), "締めスライドが出ない(showClosing:true経路)")
        XCUIScreen.main.screenshot().image.pngData().map { try? $0.write(to: shotURL("08-tour-tabtap-closing")) }

        let doneBtn = app.buttons["おわる"]
        XCTAssertTrue(doneBtn.waitForExistence(timeout: 3))
        doneBtn.tap()
    }

    // T-7: 初回オンボチャットの吹き出し(lineSpacing詰め・🆓削除)を確認する。
    func testOnboardingGreetingBubble() throws {
        let app = XCUIApplication()
        app.launch()
        let bubble = app.staticTexts["ここは毎日のストレッチを応援する場所だよ！ぜんぶ無料・とうろく不要 あんしんしてね"]
        XCTAssertTrue(bubble.waitForExistence(timeout: 10), "オンボ2つ目の吹き出しが見つからない(🆓削除後の文言)")
        usleep(300_000)
        XCUIScreen.main.screenshot().image.pngData().map { try? $0.write(to: shotURL("09-onboarding-greet-bubble")) }
    }

    private func shotURL(_ name: String) -> URL {
        let dir = URL(fileURLWithPath: "/tmp/build19-shots", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir.appendingPathComponent("\(name).png")
    }
}
