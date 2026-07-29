//
//  D2TempShareSaveUITests.swift
//  KyouNoOgatoreUITests
//
//  TEMPORARY(D2検証専用): TASK-C2-2026-07-29-testflight-feedback-d.md D2の検収基準
//  「押して実際に写真フォルダに入るところまで見ること」を満たすための一回限りの実測用。
//  常設のUIテストとして残す指示は受けていないため、確認後に削除する。
//

import XCTest

final class D2TempShareSaveUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testShareSheetSaveImageOption() throws {
        let app = XCUIApplication()
        app.launch()

        let myRecordTab = app.buttons["マイ記録"]
        XCTAssertTrue(myRecordTab.waitForExistence(timeout: 10))
        myRecordTab.tap()

        let bragEntry = app.descendants(matching: .any)["🎉 じまんカード"]
        XCTAssertTrue(bragEntry.waitForExistence(timeout: 5))
        bragEntry.tap()
        sleep(1)

        let makeCardBtn = app.descendants(matching: .any)["カードをつくる✨"]
        XCTAssertTrue(makeCardBtn.waitForExistence(timeout: 5))
        makeCardBtn.tap()
        sleep(1)
        print("D2DEBUG after makeCard tap:\n\(app.debugDescription)")

        let shareBtn = app.descendants(matching: .any)["保存・シェアする"]
        XCTAssertTrue(shareBtn.waitForExistence(timeout: 5))
        shareBtn.tap()

        // 共有シートが出るまで待つ(Simulatorではアクティビティ列挙に数秒かかることがある)。
        sleep(6)
        print("D2DEBUG activity sheet dump:\n\(app.debugDescription)")

        let saveImage = app.buttons["イメージを保存"].firstMatch
        let saveImageEn = app.buttons["Save Image"].firstMatch
        let target = saveImage.exists ? saveImage : saveImageEn
        XCTAssertTrue(target.waitForExistence(timeout: 10), "保存系のボタンが見つからない")
        target.tap()
        sleep(2)
    }
}
