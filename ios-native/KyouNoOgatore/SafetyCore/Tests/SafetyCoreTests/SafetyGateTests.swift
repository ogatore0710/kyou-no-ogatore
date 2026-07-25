import XCTest
@testable import SafetyCore

// ネイティブ移植 Step 2(マスタープラン§3-4手順3): safety-fixtures.json(111件)+norm-golden.json(16件)を
// パラメタライズド形式で全件アサートする。XCTAssert*は失敗ごとに個別の行番号つき失敗として報告されるため、
// ループ内で全件を回しても「どの入力がどう失敗したか」が1件ずつ分かる(実質パラメタライズド)。
//
// スタブ実装(全関数が「安全でない側の誤値」を返す)時点での「全赤」の定義(§3-4手順3):
//   refer/crisis/state/symptom系ケースは全件赤くなるべき(スタブは常にfalse/nilを返すため)。
//   normal/crisis-negative系は「該当なし」を期待するケースなので、スタブでも意図せず緑になる(偽緑)。
//   これは仕様どおりであり確認対象から除外する。

private struct Fixture: Decodable {
    let input: String
    let expect: String
}

private struct NormGoldenCase: Decodable {
    let system: String
    let input: String
    let normOutput: String
    let redFlagHit: Bool?
}

final class SafetyGateTests: XCTestCase {
    private func loadFixtures() -> [Fixture] {
        let url = Bundle.module.url(forResource: "safety-fixtures", withExtension: "json")!
        let data = try! Data(contentsOf: url)
        return try! JSONDecoder().decode([Fixture].self, from: data)
    }
    private func loadNormGolden() -> [NormGoldenCase] {
        let url = Bundle.module.url(forResource: "norm-golden", withExtension: "json")!
        let data = try! Data(contentsOf: url)
        return try! JSONDecoder().decode([NormGoldenCase].self, from: data)
    }

    /// safety-fixtures.json 111件。expectの意味(マスタープラン§3-4手順1):
    ///   refer            → redFlagHit(norm(input)) == true
    ///   normal           → redFlagHit(norm(input)) == false
    ///   crisis           → crisisHit(norm(input)) == true
    ///   crisis-negative  → crisisHit(norm(input)) == false
    ///   state            → redFlagHit==true かつ redFlagKind=="state"
    ///   symptom          → redFlagHit==true かつ redFlagKind=="symptom"
    func testSafetyFixtures111() {
        let fixtures = loadFixtures()
        XCTAssertEqual(fixtures.count, 111, "safety-fixtures.jsonの件数が111でない")

        var pass = 0
        for f in fixtures {
            let n = SafetyGate.norm(f.input)
            let ok: Bool
            switch f.expect {
            case "refer": ok = SafetyGate.redFlagHit(n) == true
            case "normal": ok = SafetyGate.redFlagHit(n) == false
            case "crisis": ok = SafetyGate.crisisHit(n) == true
            case "crisis-negative": ok = SafetyGate.crisisHit(n) == false
            case "state": ok = SafetyGate.redFlagHit(n) == true && SafetyGate.redFlagKind(n) == "state"
            case "symptom": ok = SafetyGate.redFlagHit(n) == true && SafetyGate.redFlagKind(n) == "symptom"
            default:
                XCTFail("未知のexpect種別: \(f.expect)")
                continue
            }
            if ok { pass += 1 }
            XCTAssertTrue(ok, "[\(f.expect)] \(f.input)")
        }
        print("safety-fixtures: \(pass)/\(fixtures.count) pass")
    }

    /// norm-golden.json: NFC/NFD合成濁点差・半角カナ・絵文字混在・「寝転」除去の連結マッチ敵対ケース。
    /// normOutputはJS実出力(soudan-ai-poc/norm.mjs)を正として固定した値(§3-4手順2)。
    func testNormGolden() {
        for c in loadNormGolden() {
            XCTAssertEqual(SafetyGate.norm(c.input), c.normOutput, "[\(c.system)] \(c.input)")
            if let wantHit = c.redFlagHit {
                let n = SafetyGate.norm(c.input)
                XCTAssertEqual(SafetyGate.redFlagHit(n), wantHit, "[\(c.system)/redFlagHit] \(c.input)")
            }
        }
    }
}
