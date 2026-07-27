import XCTest
@testable import RecordCore

// ネイティブ移植 Step 3(マスタープラン§6 Step3検収基準1・2): Step 0で採取したexport-fixture.json
// (PWA版buildExportStringの実出力)をインポートし、export-fixture-expected.jsonの期待値と機械照合する。

private struct ExportFixture: Decodable { let exportString: String }
private struct ExportExpected: Decodable {
    let streak2_count: Int
    let streak2_total: Int
    let daylog_keyCount: Int
    let keys: [String]
    let keyCount: Int
    let passThroughOnlyKeys: [String]
}

final class KyonoTransferTests: XCTestCase {
    private func loadFixture() -> ExportFixture {
        let url = Bundle.module.url(forResource: "export-fixture", withExtension: "json")!
        return try! JSONDecoder().decode(ExportFixture.self, from: Data(contentsOf: url))
    }

    private func loadExpected() -> ExportExpected {
        let url = Bundle.module.url(forResource: "export-fixture-expected", withExtension: "json")!
        return try! JSONDecoder().decode(ExportExpected.self, from: Data(contentsOf: url))
    }

    // 検収基準1: streak2のcount/total・daylog件数・キー集合が期待値JSONと一致
    func testImportExportFixtureMatchesExpectedValues() throws {
        let fixture = loadFixture()
        let expected = loadExpected()
        let store = RecordStore(inMemory: [:])
        try KyonoTransfer.importString(fixture.exportString, into: store)

        let st = RecordLogic.loadStreak(store)
        XCTAssertEqual(st.count, expected.streak2_count)
        XCTAssertEqual(st.total, expected.streak2_total)

        let dl = RecordLogic.loadDaylog(store)
        XCTAssertEqual(dl.count, expected.daylog_keyCount)

        let keys = Set(store.allRawKyonoEntries.keys)
        XCTAssertEqual(keys, Set(expected.keys))
        XCTAssertEqual(keys.count, expected.keyCount)

        // 未知キー(a2hs2/homehint_next)もネイティブが使わない値としてパススルー保全されている
        for k in expected.passThroughOnlyKeys {
            XCTAssertNotNil(store.rawValue(fullKey: k), "パススルーキー\(k)が保全されていない")
        }
    }

    // 検収基準2: インポート→エクスポートの往復でキー集合が減らない(a2hs2等の未使用キー含む)
    func testImportExportRoundTripDoesNotShrinkKeySet() throws {
        let fixture = loadFixture()
        let store = RecordStore(inMemory: [:])
        try KyonoTransfer.importString(fixture.exportString, into: store)
        let keysAfterImport = Set(store.allRawKyonoEntries.keys)

        let reExported = KyonoTransfer.buildExportString(store)
        let store2 = RecordStore(inMemory: [:])
        try KyonoTransfer.importString(reExported, into: store2)
        let keysAfterRoundTrip = Set(store2.allRawKyonoEntries.keys)

        XCTAssertEqual(keysAfterRoundTrip, keysAfterImport, "往復でキー集合が変化した")
    }

    func testRejectsInvalidPrefix() {
        let store = RecordStore(inMemory: [:])
        XCTAssertThrowsError(try KyonoTransfer.importString("NOTKYONO:xxxx", into: store))
    }

    func testRejectsMissingKyonoPrefixKeysOnly() {
        // "kyono_"始まりでないキーだけのペイロードは無効(index.html:2097 if(!cnt) throw 0 と同じ)
        let payload = #"{"v":1,"data":{"other_key":"1"}}"#
        let b64 = Data(payload.utf8).base64EncodedString()
        let store = RecordStore(inMemory: [:])
        XCTAssertThrowsError(try KyonoTransfer.importString("KYONO1:" + b64, into: store))
    }

    // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §6: Web版のatobはforgiving-base64
    // (ASCII空白を無視する)。メモアプリ経由のコピー&ペーストで改行が混入しても読めることを確認する
    // (Data(base64Encoded:)の既定=厳格版だと改行混入だけで失敗していた回帰テスト)。
    func testImportStringToleratesEmbeddedNewlinesInBase64() throws {
        let payload = #"{"v":1,"data":{"kyono_theme":"\"light\""}}"#
        let b64 = Data(payload.utf8).base64EncodedString()
        let withNewlines = b64.chunked(into: 20).joined(separator: "\n")
        let store = RecordStore(inMemory: [:])
        try KyonoTransfer.importString("KYONO1:" + withNewlines, into: store)
        XCTAssertEqual(store.rawValue(fullKey: "kyono_theme"), "\"light\"")
    }
}

private extension String {
    func chunked(into size: Int) -> [String] {
        var result: [String] = []
        var idx = startIndex
        while idx < endIndex {
            let end = index(idx, offsetBy: size, limitedBy: endIndex) ?? endIndex
            result.append(String(self[idx..<end]))
            idx = end
        }
        return result
    }
}
