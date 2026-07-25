import XCTest
@testable import CardCore

// Step4/7bパリティ突合タスク(TASK-C2-2026-07-26-native-migration-card-visual-assets.md)で追加:
// BragCardRendererにもCardRendererと同じキャラクター立ち絵・実フォントを組み込んだため、
// 現在時刻・乱数を読まない設計(§1-1第3項)が保たれているかを確認する。
final class BragCardRendererTests: XCTestCase {
    private func sampleTheme() -> ResolvedTheme {
        let t = CardDataLoader.shared.CARD_THEMES[1]
        return ResolvedTheme(name: t.name, bg: t.bg, main: t.main, deco: t.deco)
    }

    func testSameInputProducesIdenticalBitmapTwice() {
        let ds = "2026-07-25"
        let png1 = BragCardRenderer.render(ds: ds, days: 42, theme: sampleTheme(), favoriteTitle: "好きな1本のタイトル")
        let png2 = BragCardRenderer.render(ds: ds, days: 42, theme: sampleTheme(), favoriteTitle: "好きな1本のタイトル")
        XCTAssertFalse(png1.isEmpty)
        XCTAssertEqual(png1, png2, "同一入力の再描画がビット単位で一致しない")
    }

    func testClampDaysMatchesIndexHtmlBounds() {
        // index.html:2808 Math.max(1,Math.min(9999,...))の1:1移植確認。
        XCTAssertEqual(BragCardRenderer.clampDays(0), 1)
        XCTAssertEqual(BragCardRenderer.clampDays(99999), 9999)
        XCTAssertEqual(BragCardRenderer.clampDays(42), 42)
    }
}
