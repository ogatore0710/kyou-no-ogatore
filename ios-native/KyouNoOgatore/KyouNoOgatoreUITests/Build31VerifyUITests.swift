//
//  Build31VerifyUITests.swift
//  KyouNoOgatoreUITests
//
//  build31ラウンド9(R-32〜R-40)の実描画検収用スクショ収集。検収完了後に削除する
//  (Build20F1F2UITests.swiftと同じ運用)。
//  - testOnboardingChatCaptures: 未オンボ状態(ホスト側でuninstall済み)で初回チャットを撮る(R-40)
//  - testMainScreenCaptures: ホスト側でkyono-store.jsonをseed済みの状態で各画面を撮る
//    (R-33/34/35/36/37/38/39。アプリ内設定でライト→ダークを切り替えて両テーマを撮る)
//

import XCTest

final class Build31VerifyUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    // xcresult添付・ホストパス直書き・ランナーコンテナ書き(テスト終了時にコンテナごと消える)・
    // UIPasteboard(ホストへ届かず)がいずれも空振りしたため、unified logを同期チャネルにする:
    // ここでNSLogにチェックポイント名を流し→ホスト側の監視(run-b31-verify.shの
    // `simctl spawn ... log stream`)が検知して`simctl io screenshot`で撮る。
    // 3秒はホスト側のログ検知+撮影の猶予。
    private func snap(_ name: String) {
        NSLog("B31SNAP %@", name)
        Thread.sleep(forTimeInterval: 3.0)
    }

    private func tapAny(_ app: XCUIApplication, _ label: String, timeout: TimeInterval = 8) {
        let el = app.descendants(matching: .any)[label].firstMatch
        XCTAssertTrue(el.waitForExistence(timeout: timeout), "「\(label)」が見つからない")
        el.tap()
    }

    func testOnboardingChatCaptures() throws {
        let app = XCUIApplication()
        app.launch()

        // あいさつ3吹き出し(1.5秒間隔)が出そろうまで待つ。3つ目の存在で判定。
        let greet3 = app.staticTexts.containing(NSPredicate(format: "label CONTAINS %@", "最初に4つだけ教えてね！")).firstMatch
        XCTAssertTrue(greet3.waitForExistence(timeout: 15), "あいさつ3行目が出ない")
        // 2行目の指定文言(R-40)も実描画で確認
        let greet2 = app.staticTexts.containing(NSPredicate(format: "label CONTAINS %@", "すべて無料で登録はナシ！安心してね！")).firstMatch
        XCTAssertTrue(greet2.exists, "R-40指定文言が出ていない")
        snap("01-r40-chat-greeting")

        // 4問に回答して相づち(R-40「！」締め)を撮る
        tapAny(app, "大きめ（いまのまま）")
        tapAny(app, "ガチガチかも")
        tapAny(app, "肩こり・首", timeout: 10)
        tapAny(app, "おふろ上がり", timeout: 10)
        let ack = app.staticTexts.containing(NSPredicate(format: "label CONTAINS %@", "おふろ上がりは体もほぐれてて効果的！覚えたよ！")).firstMatch
        XCTAssertTrue(ack.waitForExistence(timeout: 10), "anchor相づちが出ない")
        // 締めメッセージ+CTAまで待ってから全景を撮る
        let cta = app.descendants(matching: .any)["かたさチェックをはじめる"].firstMatch
        _ = cta.waitForExistence(timeout: 10)
        snap("02-r40-chat-acks")
    }

    func testMainScreenCaptures() throws {
        let app = XCUIApplication()
        app.launch()

        // ---- ホーム(ライト): R-33ラベル・R-34ボタン文言 ----
        let mainLabel = app.staticTexts["メインの一本"].firstMatch
        XCTAssertTrue(mainLabel.waitForExistence(timeout: 15), "あなた用「メインの一本」が出ない(seed失敗の疑い)")
        snap("03-r33r34-home-light")

        // ---- マイ記録(ライト): R-35見出し・R-37カード ----
        tapAny(app, "マイ記録")
        let msNote = app.staticTexts.containing(NSPredicate(format: "label CONTAINS %@", "次のお祝いポイントは")).firstMatch
        XCTAssertTrue(msNote.waitForExistence(timeout: 10), "続けた記録見出しが出ない")
        snap("04-r35-myrecord-light")
        // マイ設定カードまでスクロール(R-37)
        var guard1 = 0
        while !app.staticTexts["マイ設定"].firstMatch.isHittable && guard1 < 8 {
            app.swipeUp()
            guard1 += 1
        }
        snap("05-r37-myrecord-mysettings-card-light")

        // ---- 設定画面(ライト): R-37見出し・R-39やるタイミング・チップ(R-38ライト不変確認) ----
        tapAny(app, "設定をひらく")
        let anchorLine = app.staticTexts.containing(NSPredicate(format: "label CONTAINS %@", "やるタイミング")).firstMatch
        XCTAssertTrue(anchorLine.waitForExistence(timeout: 10), "やるタイミング行が出ない")
        snap("06-r39-settings-light")
        tapAny(app, "変える")
        _ = app.descendants(matching: .any)["おふろ上がりに"].firstMatch.waitForExistence(timeout: 5)
        snap("07-r38-settings-anchorpicker-light")
        tapAny(app, "おふろ上がりに") // 閉じる(値は同じ)

        // ---- ダークへ切り替え(アプリ内設定・R-39の実バグ経路そのもの) ----
        tapAny(app, "暗い")
        // 切り替え反映を少し待つ
        Thread.sleep(forTimeInterval: 1.0)
        snap("08-r39r38-settings-dark-top")
        // よびな欄・記録をコピーするまでスクロール
        var guard2 = 0
        while !app.buttons["記録をコピーする"].firstMatch.isHittable && guard2 < 8 {
            app.swipeUp()
            guard2 += 1
        }
        snap("09-r38-settings-dark-buttons")
        tapAny(app, "変える", timeout: 5)
        _ = app.descendants(matching: .any)["おふろ上がりに"].firstMatch.waitForExistence(timeout: 5)
        snap("10-r38-settings-anchorpicker-dark")
        tapAny(app, "おふろ上がりに")

        // ---- もどる→マイ記録(ダーク)→ホーム(ダーク)→使い方(両テーマ) ----
        var guard3 = 0
        while !app.buttons["◀ もどる"].firstMatch.isHittable && guard3 < 8 {
            app.swipeDown()
            guard3 += 1
        }
        snap("11-r38-settings-dark-modoru")
        tapAny(app, "◀ もどる")
        _ = app.staticTexts.containing(NSPredicate(format: "label CONTAINS %@", "次のお祝いポイントは")).firstMatch.waitForExistence(timeout: 8)
        snap("12-r35-myrecord-dark")

        tapAny(app, "ホーム")
        let renzoku = app.descendants(matching: .any).containing(NSPredicate(format: "label CONTAINS %@", "専用の動画連続再生はこちら")).firstMatch
        XCTAssertTrue(renzoku.waitForExistence(timeout: 10), "連続再生ボタンが出ない")
        snap("13-r38r34-home-dark")

        tapAny(app, "使い方")
        let tourBtn = app.descendants(matching: .any)["使い方ツアー"].firstMatch
        XCTAssertTrue(tourBtn.waitForExistence(timeout: 10), "使い方ツアーボタンが出ない")
        snap("14-r36-guide-dark")

        // ---- ライトへ戻して使い方タブ(R-36ライト)を撮って終了 ----
        tapAny(app, "マイ記録")
        var guard4 = 0
        while !app.descendants(matching: .any)["設定をひらく"].firstMatch.isHittable && guard4 < 8 {
            app.swipeUp()
            guard4 += 1
        }
        tapAny(app, "設定をひらく")
        tapAny(app, "明るい")
        Thread.sleep(forTimeInterval: 1.0)
        tapAny(app, "使い方")
        _ = app.descendants(matching: .any)["使い方ツアー"].firstMatch.waitForExistence(timeout: 10)
        snap("15-r36-guide-light")
    }
}
