import XCTest
@testable import CardCore

// ネイティブ移植 Step 4(マスタープラン§6 Step4検収基準4): 「同一日付での再描画が同一出力」を
// ビットマップ(PNGバイト列)比較で確認する。CardRendererは現在時刻・乱数を一切読まない設計
// (ForbiddenAPIRegressionTests参照)なので、同じ入力を2回描画すればビット単位で一致するはず。
final class CardRendererTests: XCTestCase {
    private func sampleTheme() -> ResolvedTheme {
        let t = CardDataLoader.shared.CARD_THEMES[0]
        return ResolvedTheme(name: t.name, bg: t.bg, main: t.main, deco: t.deco)
    }

    func testSameInputProducesIdenticalBitmapTwice() {
        let data = CardDataLoader.shared
        let ds = "2026-07-25"
        let di = CardLottery.dateIdx(ds)
        let png1 = CardRenderer.render(
            ds: ds, effTotal: 55, theme: sampleTheme(), milestone: false, milestoneTitle: nil,
            dateIdx: di, cardThemesV2From: data.CARD_THEMES_V2_FROM
        )
        let png2 = CardRenderer.render(
            ds: ds, effTotal: 55, theme: sampleTheme(), milestone: false, milestoneTitle: nil,
            dateIdx: di, cardThemesV2From: data.CARD_THEMES_V2_FROM
        )
        XCTAssertFalse(png1.isEmpty)
        XCTAssertEqual(png1, png2, "同一日付・同一入力の再描画がビット単位で一致しない")
    }

    func testDifferentDatesProduceDifferentBitmaps() {
        let data = CardDataLoader.shared
        let theme = sampleTheme()
        let png1 = CardRenderer.render(
            ds: "2026-07-25", effTotal: 55, theme: theme, milestone: false, milestoneTitle: nil,
            dateIdx: CardLottery.dateIdx("2026-07-25"), cardThemesV2From: data.CARD_THEMES_V2_FROM
        )
        let png2 = CardRenderer.render(
            ds: "2026-07-20", effTotal: 50, theme: theme, milestone: true, milestoneTitle: "50日たっせい",
            dateIdx: CardLottery.dateIdx("2026-07-20"), cardThemesV2From: data.CARD_THEMES_V2_FROM
        )
        XCTAssertNotEqual(png1, png2, "異なる日付なのに同じ出力になっている(装飾/数字が反映されていない疑い)")
    }

    // ---- ここからパリティ突合タスク(TASK-C2-2026-07-26-native-migration-card-visual-assets.md)追加分 ----

    func testSameInputWithTagsProducesIdenticalBitmapTwice() {
        let data = CardDataLoader.shared
        let ds = "2026-07-25"
        let di = CardLottery.dateIdx(ds)
        let png1 = CardRenderer.render(
            ds: ds, effTotal: 55, theme: sampleTheme(), milestone: false, milestoneTitle: nil,
            dateIdx: di, cardThemesV2From: data.CARD_THEMES_V2_FROM,
            typeName: "もちもちタイプ", typeIconKey: "momo", memoText: "きょうもいい調子", streakCount: 3
        )
        let png2 = CardRenderer.render(
            ds: ds, effTotal: 55, theme: sampleTheme(), milestone: false, milestoneTitle: nil,
            dateIdx: di, cardThemesV2From: data.CARD_THEMES_V2_FROM,
            typeName: "もちもちタイプ", typeIconKey: "momo", memoText: "きょうもいい調子", streakCount: 3
        )
        XCTAssertEqual(png1, png2, "タグピル込みの再描画がビット単位で一致しない")
    }

    func testTagPillRowChangesBitmapComparedToNoTagRow() {
        let data = CardDataLoader.shared
        let ds = "2026-07-25"
        let di = CardLottery.dateIdx(ds)
        let withoutTags = CardRenderer.render(
            ds: ds, effTotal: 55, theme: sampleTheme(), milestone: false, milestoneTitle: nil,
            dateIdx: di, cardThemesV2From: data.CARD_THEMES_V2_FROM
        )
        let withTags = CardRenderer.render(
            ds: ds, effTotal: 55, theme: sampleTheme(), milestone: false, milestoneTitle: nil,
            dateIdx: di, cardThemesV2From: data.CARD_THEMES_V2_FROM,
            typeName: "もちもちタイプ", memoText: "メモです"
        )
        XCTAssertNotEqual(withoutTags, withTags, "タグピル行(かたさタイプ/メモ)の有無で出力が変わっていない")
    }

    func testSameDateIdxAlwaysPicksSameCharacter() {
        // dateIdx駆動のキャラ選定(Web版のdayIndex()=現在時刻依存とは意図的に差をつけている・
        // CardRenderer.swift冒頭コメント参照)が本当に決定的か、同じdsで3回描画してすべて一致するかで確認する。
        let data = CardDataLoader.shared
        let ds = "2026-06-10"
        let di = CardLottery.dateIdx(ds)
        let pngs = (1...3).map { _ in
            CardRenderer.render(
                ds: ds, effTotal: 10, theme: sampleTheme(), milestone: false, milestoneTitle: nil,
                dateIdx: di, cardThemesV2From: data.CARD_THEMES_V2_FROM
            )
        }
        XCTAssertEqual(pngs[0], pngs[1], "同じ日付なのにキャラクター選定(dateIdx駆動)が再現しない")
        XCTAssertEqual(pngs[1], pngs[2], "同じ日付なのにキャラクター選定(dateIdx駆動)が再現しない")
    }
}
