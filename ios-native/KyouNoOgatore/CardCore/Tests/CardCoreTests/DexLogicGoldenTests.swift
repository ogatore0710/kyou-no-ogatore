import XCTest
@testable import CardCore

// ネイティブ移植 Step 7a: getDexStatus()のゴールデン(scripts-native/gen-dex-golden.mjsでWeb版実行結果を
// 採取)。card-golden.jsonと同一断面(rotAssign空初期化+2026-06-01〜2026-07-25の連続55日)で、
// DexLogic.getDexStatusが返すtoku/season/rare/normalの各tier・key・name・got状態をWeb版と1件ずつ突合する。

private struct GoldenItem: Decodable {
    let tier: String
    let key: String?
    let name: String
    let got: Bool
}

private struct DexGolden: Decodable {
    let seedDateRangeStart: String
    let seedDateRangeEnd: String
    let toku: [GoldenItem]
    let season: [GoldenItem]
    let rare: [GoldenItem]
    let normal: [GoldenItem]
}

final class DexLogicGoldenTests: XCTestCase {
    func testGetDexStatusMatchesWebGolden() {
        let url = Bundle.module.url(forResource: "dex-golden", withExtension: "json")!
        let golden = try! JSONDecoder().decode(DexGolden.self, from: try! Data(contentsOf: url))

        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withFullDate]
        formatter.timeZone = TimeZone(identifier: "UTC")
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC")!
        var dates: [String] = []
        var d = formatter.date(from: golden.seedDateRangeStart)!
        let end = formatter.date(from: golden.seedDateRangeEnd)!
        while d <= end {
            dates.append(formatter.string(from: d))
            d = calendar.date(byAdding: .day, value: 1, to: d)!
        }

        let status = DexLogic.getDexStatus(dates: dates, total: dates.count, rotAssign: [:])

        func assertTier(_ name: String, _ want: [GoldenItem], _ got: [DexItem]) {
            XCTAssertEqual(want.count, got.count, "\(name)件数")
            for (i, w) in want.enumerated() {
                let g = got[i]
                XCTAssertEqual(w.tier, g.tier, "\(name)[\(i)].tier")
                XCTAssertEqual(w.key, g.key, "\(name)[\(i)].key")
                XCTAssertEqual(w.name, g.name, "\(name)[\(i)].name")
                XCTAssertEqual(w.got, g.got, "\(name)[\(i)].got")
            }
        }
        assertTier("toku", golden.toku, status.toku)
        assertTier("season", golden.season, status.season)
        assertTier("rare", golden.rare, status.rare)
        assertTier("normal", golden.normal, status.normal)
    }
}
