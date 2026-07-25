import XCTest
@testable import CardCore

// ネイティブ移植 Step 4(マスタープラン§6 Step4検収基準1): card-golden.json(Step0採取・55件)を
// scripts-native/verify-card-data.mjsと同じ手順で再生し、JS実出力(dateIdx/effTotal/milestone/
// isImgEra/isThemeV2Era/pat/rotAssignPos)と全一致することを確認する。
// note通り: streak2.datesは2026-06-01〜2026-07-25の連続55日(=cases自体)。rotAssignは空から出発
// (ensureRotAssignで一括バックフィル。Step0が明示的に指定した仕様)。

private struct GoldenPattern: Decodable, Equatable {
    let tier: String
    let name: String
    let key: String?
}

private struct GoldenCase: Decodable {
    let ds: String
    let dateIdx: Int
    let effTotal: Int
    let milestone: Bool
    let isImgEra: Bool
    let isThemeV2Era: Bool
    let pat: GoldenPattern?
    let rotAssignPos: Int?
}

private struct GoldenFile: Decodable {
    let cases: [GoldenCase]
}

final class CardLotteryTests: XCTestCase {
    private func loadGolden() -> GoldenFile {
        let url = Bundle.module.url(forResource: "card-golden", withExtension: "json")!
        return try! JSONDecoder().decode(GoldenFile.self, from: try! Data(contentsOf: url))
    }

    func testCardGolden55CasesMatchJSOutput() {
        let golden = loadGolden()
        XCTAssertEqual(golden.cases.count, 55, "card-golden.jsonの件数が55でない")

        let dates = golden.cases.map { $0.ds }
        let total = dates.count
        var rot = CardLottery.ensureRotAssign(dates: dates, total: total, existing: [:])
        let data = CardDataLoader.shared

        var failures: [String] = []
        for (i, c) in golden.cases.enumerated() {
            let effTotal = i + 1
            let di = CardLottery.dateIdx(c.ds)
            let milestone = data.MILESTONES.contains(effTotal)
            let isImgEra = di >= data.CARD_IMG_FROM
            let isThemeV2Era = di >= data.CARD_THEMES_V2_FROM
            let pat = CardLottery.cardPatternFor(ds: c.ds, effTotal: effTotal, dateIdx: di, rot: &rot)
            let rotAssignPos: Int? = (isImgEra && (pat?.tier == "normal" || pat?.tier == "rare")) ? rot[c.ds] : nil

            if di != c.dateIdx { failures.append("\(c.ds) dateIdx: got=\(di) want=\(c.dateIdx)") }
            if effTotal != c.effTotal { failures.append("\(c.ds) effTotal: got=\(effTotal) want=\(c.effTotal)") }
            if milestone != c.milestone { failures.append("\(c.ds) milestone: got=\(milestone) want=\(c.milestone)") }
            if isImgEra != c.isImgEra { failures.append("\(c.ds) isImgEra: got=\(isImgEra) want=\(c.isImgEra)") }
            if isThemeV2Era != c.isThemeV2Era { failures.append("\(c.ds) isThemeV2Era: got=\(isThemeV2Era) want=\(c.isThemeV2Era)") }
            let gotPat = pat.map { GoldenPattern(tier: $0.tier, name: $0.name, key: $0.key) }
            if gotPat != c.pat { failures.append("\(c.ds) pat: got=\(String(describing: gotPat)) want=\(String(describing: c.pat))") }
            if rotAssignPos != c.rotAssignPos { failures.append("\(c.ds) rotAssignPos: got=\(String(describing: rotAssignPos)) want=\(String(describing: c.rotAssignPos))") }
        }
        XCTAssertTrue(failures.isEmpty, "不一致 \(failures.count)/\(golden.cases.count) 件:\n" + failures.joined(separator: "\n"))
        if failures.isEmpty { print("card-golden: \(golden.cases.count)/\(golden.cases.count) match") }
    }
}
