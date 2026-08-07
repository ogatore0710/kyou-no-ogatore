import XCTest
@testable import RecordCore

// TASK build36 R-58(Fable監査A-1+設計裁定・2026-08-07): 全消失シナリオの再発防止テスト。
// 「破損しても唯一のコピーが保全される」ことの直接証明が本丸(Android RecordStoreTestと対)。
final class RecordStoreTests: XCTestCase {
    private var dir: URL!

    override func setUpWithError() throws {
        dir = FileManager.default.temporaryDirectory
            .appendingPathComponent("RecordStoreTests-\(UUID().uuidString)")
        try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
    }

    override func tearDownWithError() throws {
        try? FileManager.default.removeItem(at: dir)
    }

    private var storeURL: URL { dir.appendingPathComponent("kyono-store.json") }

    private func corruptFiles() throws -> [String] {
        try FileManager.default.contentsOfDirectory(atPath: dir.path)
            .filter { $0.hasPrefix("kyono-store.json.corrupt-") }
    }

    // 1) 基本の往復: set→別インスタンスでload→一致
    func testRoundtripPersistsAcrossInstances() {
        let store = RecordStore(fileURL: storeURL)
        XCTAssertTrue(store.set("onboarded", true))
        XCTAssertTrue(store.set("anchor", "furo"))
        let reloaded = RecordStore(fileURL: storeURL)
        XCTAssertEqual(reloaded.get("onboarded", default: false), true)
        XCTAssertEqual(reloaded.get("anchor", default: ""), "furo")
    }

    // 2) 全消失シナリオの直接証明: 途中切れファイル→隔離され空で起動→次のsetでも隔離は無傷
    func testCorruptedFileIsQuarantinedAndNeverOverwritten() throws {
        let store = RecordStore(fileURL: storeURL)
        store.set("streak2", ["total": "5"])
        // 書き込み途中killの再現: バイト列を前半だけにtruncate
        let fullBytes = try Data(contentsOf: storeURL)
        let broken = fullBytes.prefix(fullBytes.count / 2)
        try broken.write(to: storeURL)

        let reloaded = RecordStore(fileURL: storeURL)
        // (a) 空で起動(デフォルト値に落ちる)
        XCTAssertEqual(reloaded.get("streak2", default: [String: String]()), [:])
        // (b) 破損データは.corrupt-*へ原文のまま保全されている
        let quarantined = try corruptFiles()
        XCTAssertEqual(quarantined.count, 1)
        let qData = try Data(contentsOf: dir.appendingPathComponent(quarantined[0]))
        XCTAssertEqual(qData, Data(broken))
        // (c) その後の操作は新しい本体に書かれ、隔離ファイルは無傷のまま
        XCTAssertTrue(reloaded.set("onboarded", true))
        XCTAssertEqual(RecordStore(fileURL: storeURL).get("onboarded", default: false), true)
        XCTAssertEqual(try Data(contentsOf: dir.appendingPathComponent(quarantined[0])), Data(broken))
    }

    // 3) サルベージ: 手編集で生bool/null/数値が混入した過去実績の事故がゼロ損失で復元される
    func testSalvageRecoversRawJsonValues() throws {
        let broken = #"{"kyono_onboarded": true, "kyono_anchor": "\"furo\"", "kyono_broken": null, "kyono_n": 3}"#
        try broken.data(using: .utf8)!.write(to: storeURL)
        let store = RecordStore(fileURL: storeURL)
        XCTAssertEqual(store.get("onboarded", default: false), true)
        XCTAssertEqual(store.get("anchor", default: ""), "furo")
        XCTAssertEqual(store.get("n", default: 0), 3)
        // 隔離はされない(復元できたので)・原本は.bakに残る
        XCTAssertEqual(try corruptFiles().count, 0)
        XCTAssertTrue(FileManager.default.fileExists(atPath: dir.appendingPathComponent("kyono-store.json.bak").path))
        // 次のpersistで正規形(二重エンコード)に直っている
        store.set("theme", "dark")
        let reloaded = RecordStore(fileURL: storeURL)
        XCTAssertEqual(reloaded.get("onboarded", default: false), true)
        XCTAssertEqual(reloaded.get("theme", default: ""), "dark")
    }

    // 4) 隔離ファイルの上限: 新しい順に2個までへトリムされる
    func testQuarantineFilesAreTrimmedToNewestTwo() throws {
        for stamp in [100, 200, 300] {
            try "x".data(using: .utf8)!
                .write(to: dir.appendingPathComponent("kyono-store.json.corrupt-\(stamp)"))
        }
        _ = RecordStore(fileURL: storeURL)
        let names = try corruptFiles().sorted()
        XCTAssertEqual(names, ["kyono-store.json.corrupt-200", "kyono-store.json.corrupt-300"])
    }

    // 5) persist失敗時の無害性: 保存先が消えていてもクラッシュしない
    func testPersistFailureDoesNotThrow() throws {
        let store = RecordStore(fileURL: storeURL)
        store.set("onboarded", true)
        try FileManager.default.removeItem(at: dir)
        store.set("anchor", "asa") // 例外にならないこと(戻り値は問わない)
    }
}
