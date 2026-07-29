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

        // タグ未選択・検索語なしの既定状態でもsearchCatalog()はフィルタなし=catalog全454件を返す
        // (activeTag=nil, query=""はどちらも早期return trueになる)ため、タグをタップしなくても
        // C2と同じ「非スクロール親+weight付きスクロール子」の描画経路をそのまま踏める。
        // カテゴリ/タグをタップしない方が、結果行のaccessibility labelにタグ名(例:「全身」)が
        // 含まれることによる要素検索の曖昧さも避けられる。

        // C2の欠陥はまさにここで「1件しか描画されない」形で起きた。2件以上を要求することで、
        // 検索ロジックが正しく454件→51件に絞り込めていても、器が壊れていれば必ず落ちるようにする。
        let firstRow = app.buttons.matching(identifier: "searchResultRow").firstMatch
        XCTAssertTrue(firstRow.waitForExistence(timeout: 5), "検索結果の行が1件も描画されない")

        let rowCount = app.buttons.matching(identifier: "searchResultRow").count
        XCTAssertGreaterThanOrEqual(rowCount, 2, "検索結果が2件以上描画されていない(実際: \(rowCount)件) — C2と同じ「器」の欠陥の疑い")

        // F2(TASK-C2-2026-07-29-inspection-upgrade.md): D1(サムネイル全滅)は行数だけを数える
        // このテストでは素通りしていた(画像が1枚も無くても行自体は描画されるため)。
        // KyonoAsyncImageは画像が実際に読み込めたときだけ"kyonoThumbnailLoaded"識別子を持つ
        // Imageを描画する(D1修正時に追加)。ネットワーク読み込みを待つため少し余裕を持たせる。
        let loadedThumbnail = app.images["kyonoThumbnailLoaded"].firstMatch
        XCTAssertTrue(loadedThumbnail.waitForExistence(timeout: 10), "サムネイル画像が1枚も読み込まれていない(D1と同じ「全滅」の疑い)")

        // 未フィルタなのでcatalog全件(454件、catalog.json時点)がヒットし、searchLimit=24を
        // 超えるため「もっと見る」が出るはず。ただしLazyVStackは可視域付近しか実体化しないため、
        // ボタンが24件目の下にある間はaccessibilityツリーにまだ現れない。スクロールで近づける。
        // KyonoGhostButton(Text+タップジェスチャで実装)はXCUITest上ではButtonロールではなく
        // StaticTextとして現れるため、要素種別を限定しないdescendants(matching: .any)で探す。
        let moreButton = app.descendants(matching: .any)["searchMoreBtn"]
        let scrollView = app.scrollViews.firstMatch
        for _ in 0..<30 where !moreButton.exists {
            scrollView.swipeUp(velocity: .fast)
        }
        XCTAssertTrue(moreButton.waitForExistence(timeout: 5), "「もっと見る」ボタンが見つからない(最後までスクロールしても出現しない)")
    }

    // F2(TASK-C2-2026-07-29-inspection-upgrade.md): C1(タブバー下端の黒い帯・ignoresSafeArea漏れ)の
    // 再発防止。新しいライブラリは使わず、XCUIScreen.main.screenshot()のCGImageを直接ピクセル標本
    // 抽出する。画面最下端(セーフエリア外)の帯がタブバーの背景色まで届いていれば黒くならないはず。
    //
    // 本人の元の報告はダークモードだった(タブバーのダーク背景色#211E19より「もっと黒い」という
    // 言葉が根拠)。実測したところ、ライトモードでは画面本体側の背景(KyonoBackgroundColor)が
    // 既にその領域まで塗られているため黒く見えず、この検査はダークモードでないと同じ欠陥を
    // 再現できない。シミュレータのOS設定(simctl ui appearance)には検査プロセスからは触れない
    // (サンドボックスされた別プロセス)ため、アプリ自身の設定画面(続ける設定→画面のみため→
    // 「暗い」)を実際に操作して切り替える。production側のコードは一切触らない。
    func testNoBlackBarAtBottomOfScreen() throws {
        let app = XCUIApplication()
        app.launch()

        let myRecordTab = app.buttons["マイ記録"]
        XCTAssertTrue(myRecordTab.waitForExistence(timeout: 10), "タブバーに「マイ記録」が見つからない")
        myRecordTab.tap()

        let settingsBtn = app.descendants(matching: .any)["⚙️ 設定をひらく"]
        let scrollView = app.scrollViews.firstMatch
        for _ in 0..<15 where !settingsBtn.exists {
            scrollView.swipeUp(velocity: .fast)
        }
        XCTAssertTrue(settingsBtn.waitForExistence(timeout: 5), "「⚙️ 設定をひらく」が見つからない")
        settingsBtn.tap()

        let darkOption = app.descendants(matching: .any)["暗い"]
        XCTAssertTrue(darkOption.waitForExistence(timeout: 5), "設定に「暗い」の選択肢が見つからない")
        darkOption.tap()
        sleep(1) // テーマ切り替えの反映を待つ

        let backBtn = app.descendants(matching: .any)["◀ もどる"]
        if backBtn.waitForExistence(timeout: 3) { backBtn.tap() }

        let searchTab = app.buttons["動画を探す"]
        XCTAssertTrue(searchTab.waitForExistence(timeout: 10), "タブバーに「動画を探す」が見つからない")
        searchTab.tap()
        sleep(1) // タブ切り替えの遷移アニメーションが収まるのを待つ

        let screenshot = XCUIScreen.main.screenshot()
        guard let cgImage = screenshot.image.cgImage else {
            XCTFail("スクリーンショットのCGImageが取得できない")
            return
        }
        guard let data = cgImage.dataProvider?.data, let ptr = CFDataGetBytePtr(data) else {
            XCTFail("スクリーンショットのピクセルデータが取得できない")
            return
        }
        let width = cgImage.width
        let bytesPerPixel = max(1, cgImage.bitsPerPixel / 8)
        let bytesPerRow = cgImage.bytesPerRow
        let dataLength = CFDataGetLength(data)
        // 画面いちばん下(セーフエリア外)の帯を標本抽出する。左右の端は角丸/ノッチ由来で暗いことが
        // あるため、中央寄りだけを見る。
        let sampleY = cgImage.height - 2
        var blackCount = 0
        var sampled = 0
        var xs: [Int] = []
        var x = width / 5
        while x < width - width / 5 {
            xs.append(x)
            x += max(1, width / 20)
        }
        for sx in xs {
            let offset = sampleY * bytesPerRow + sx * bytesPerPixel
            guard offset + 2 < dataLength else { continue }
            let r = ptr[offset], g = ptr[offset + 1], b = ptr[offset + 2]
            sampled += 1
            // C1の黒い帯は文字どおり黒(RGBほぼ0)だった。テーマ色(クリーム/ダーク)とは
            // 十分離れたしきい値なので、ライト/ダーク両テーマで誤検知しない。
            if r < 20 && g < 20 && b < 20 { blackCount += 1 }
        }
        XCTAssertGreaterThan(sampled, 0, "ピクセル標本抽出に失敗(0点)")
        XCTAssertEqual(blackCount, 0, "画面下端に黒い帯を検出した(\(blackCount)/\(sampled)点が黒・C1と同じ疑い)")
    }
}
