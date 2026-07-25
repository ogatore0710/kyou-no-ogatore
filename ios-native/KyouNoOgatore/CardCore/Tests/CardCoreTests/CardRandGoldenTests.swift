import XCTest
@testable import CardCore

// ネイティブ移植 Step 4: cardRand(mulberry32)の生出力ゴールデン(scripts-native/gen-card-rand-golden.mjs
// で採取。card-golden.jsonが検証しないビット単位のUInt32折り返し挙動を固定する)。

private struct RandGoldenCase: Decodable {
    let seed: Double // JSONの数値。UInt32範囲内だがDecodableの都合上Doubleで受けてからUIntへ変換する
    let values: [Double]
}

final class CardRandGoldenTests: XCTestCase {
    func testCardRandMatchesJSOutput() {
        let url = Bundle.module.url(forResource: "card-rand-golden", withExtension: "json")!
        let cases = try! JSONDecoder().decode([RandGoldenCase].self, from: try! Data(contentsOf: url))
        XCTAssertFalse(cases.isEmpty)

        for c in cases {
            let rnd = CardLottery.cardRand(seed: UInt32(c.seed))
            for (i, want) in c.values.enumerated() {
                let got = rnd()
                XCTAssertEqual(got, want, accuracy: 1e-12, "seed=\(c.seed) index=\(i)")
            }
        }
    }
}
