//
//  SearchViewUITests.swift
//  KyouNoOgatoreUITests
//
//  TestFlight実機フィードバックC2(2026-07-29)の再発防止。alan5指摘: 「動画を探す」の
//  検索ロジック(SearchScreen.kt/SearchView.swift)自体は最後まで正常で、ユニットテストでは
//  絶対に捕まらない「並べる器」側の欠陥(非スクロール親の中にweight付き/高さ不定のスクロール子を
//  ネストすると、上の固定コンテンツが多いときに結果が丸ごと描画されない)だった。ロジックの
//  ユニットテストを何本増やしてもこの種の欠陥は再発を防げないため、実機(シミュレータ)上で
//  実際にレンダリングされた要素数を数えるUIテストとして固定する。
//
//  範囲は指示どおり「動画を探す」1本に絞る(欲張らない)。他タブへの横展開は次の機会に。
//

import XCTest

final class SearchViewUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testSearchResultsRenderMultipleRowsAndMoreButton() throws {
        let app = XCUIApplication()
        app.launch()

        // オンボ済み・記録ありの状態を前提にしない(初回起動でもタブバー自体は出る設計のため)。
        // オンボ中はタブバーが無い可能性があるので、まずタブバーの「動画を探す」が出るまで待つ。
        let searchTab = app.buttons["動画を探す"]
        XCTAssertTrue(searchTab.waitForExistence(timeout: 10), "タブバーに「動画を探す」が見つからない(オンボ画面のまま止まっている可能性)")
        searchTab.tap()

        // index.html/TAG_CATS先頭カテゴリ「からだの場所」の先頭タグ「全身」を選ぶ。
        // カテゴリボタン自体はデフォルトで選択済みのため、タグだけタップすればよい。
        let zenshinTag = app.staticTexts["全身"]
        XCTAssertTrue(zenshinTag.waitForExistence(timeout: 5), "「全身」タグが見つからない")
        zenshinTag.tap()

        // C2の欠陥はまさにここで「1件しか描画されない」形で起きた。2件以上を要求することで、
        // 検索ロジックが正しく454件→51件に絞り込めていても、器が壊れていれば必ず落ちるようにする。
        let firstRow = app.buttons.matching(identifier: "searchResultRow").firstMatch
        XCTAssertTrue(firstRow.waitForExistence(timeout: 5), "検索結果の行が1件も描画されない")

        let rowCount = app.buttons.matching(identifier: "searchResultRow").count
        XCTAssertGreaterThanOrEqual(rowCount, 2, "検索結果が2件以上描画されていない(実際: \(rowCount)件) — C2と同じ「器」の欠陥の疑い")

        // 「全身」は51件ヒットする(catalog.json時点)ため、searchLimit=24を超えて「もっと見る」が出るはず。
        let moreButton = app.buttons["searchMoreBtn"]
        XCTAssertTrue(moreButton.waitForExistence(timeout: 5), "「もっと見る」ボタンが見つからない")
    }
}
