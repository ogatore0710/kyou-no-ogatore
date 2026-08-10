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

    // ---- TASK 2026-08-10 A-2(C3総監査→alan5発注): 書き込み失敗の検知とデータ保全の直接証明 ----

    // 6) 書き込み失敗はfalseで検知でき、ディスク上の旧データは無傷のまま残る
    //    (従来は.atomic失敗をtry?で握りつぶしset()が無条件trueだった)
    func testSetReturnsFalseOnWriteFailureAndKeepsPreviousFile() throws {
        let store = RecordStore(fileURL: storeURL)
        XCTAssertTrue(store.set("onboarded", true))
        let before = try Data(contentsOf: storeURL)

        // 失敗の注入: 親ディレクトリを読み取り専用にして一時ファイル作成を失敗させる
        try FileManager.default.setAttributes([.posixPermissions: 0o555], ofItemAtPath: dir.path)
        defer { try? FileManager.default.setAttributes([.posixPermissions: 0o755], ofItemAtPath: dir.path) }

        XCTAssertFalse(store.set("anchor", "asa"))
        try FileManager.default.setAttributes([.posixPermissions: 0o755], ofItemAtPath: dir.path)
        // 旧ファイルはバイト単位で無傷(黙って空になったり途中状態になったりしない)
        XCTAssertEqual(try Data(contentsOf: storeURL), before)
        XCTAssertEqual(RecordStore(fileURL: storeURL).get("onboarded", default: false), true)
    }

    // 7) 失敗した書き込みぶんはメモリに保持され、次に成功したpersistで一緒に回復する
    func testFailedWriteRecoversOnNextSuccessfulPersist() throws {
        let store = RecordStore(fileURL: storeURL)
        XCTAssertTrue(store.set("onboarded", true))
        try FileManager.default.setAttributes([.posixPermissions: 0o555], ofItemAtPath: dir.path)
        XCTAssertFalse(store.set("anchor", "asa"))
        try FileManager.default.setAttributes([.posixPermissions: 0o755], ofItemAtPath: dir.path)

        XCTAssertTrue(store.set("theme", "dark"))
        let reloaded = RecordStore(fileURL: storeURL)
        XCTAssertEqual(reloaded.get("anchor", default: ""), "asa") // 失敗時の値も一緒に保存されている
        XCTAssertEqual(reloaded.get("theme", default: ""), "dark")
    }

    // 8) 成功パス: 一時ファイル(.tmp)が残らない・成否がtrueで返る
    func testNoTmpLeftoverAfterSuccessfulPersist() throws {
        let store = RecordStore(fileURL: storeURL)
        XCTAssertTrue(store.set("onboarded", true))
        let names = try FileManager.default.contentsOfDirectory(atPath: dir.path)
        XCTAssertFalse(names.contains("kyono-store.json.tmp"))
        XCTAssertTrue(names.contains("kyono-store.json"))
    }

    // 9) メモリのみ(テスト用init)のset()は従来どおりtrue(永続化なし=成功扱い)
    func testInMemorySetStillReturnsTrue() {
        let store = RecordStore(inMemory: [:])
        XCTAssertTrue(store.set("onboarded", true))
        XCTAssertEqual(store.get("onboarded", default: false), true)
    }
}
