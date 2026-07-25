import Foundation

// ネイティブ移植 Step 3(マスタープラン§2-3): Web版のlocalStorage(kyono_*プレフィックス・
// キーごとにJSON.stringify済み文字列を保持)を、Documents内の単一JSONファイル`kyono-store.json`に
// 1:1対応させたKVストア。buildExportString/importDataの契約(§2-3)をそのまま成立させるため、
// 内部表現も「フルキー("kyono_"つき)→生JSON文字列」のまま保持する(localStorageと同じ形)。
// 起動時に全読み込み・変更のたび全書き戻し(§2-3の方針。サイズが小さいため十分)。
public final class RecordStore {
    private var raw: [String: String] // "kyono_xxx" -> JSON文字列(Web版のlocalStorage.getItem(key)相当)
    private let fileURL: URL?

    // 実機用: ファイルに永続化する
    public init(fileURL: URL) {
        self.fileURL = fileURL
        self.raw = [:]
        load()
    }

    // テスト用: メモリ上のみで完結させる(ファイルI/Oを一切しない)
    public init(inMemory raw: [String: String] = [:]) {
        self.fileURL = nil
        self.raw = raw
    }

    private func load() {
        guard let url = fileURL, let data = try? Data(contentsOf: url) else { return }
        guard let decoded = try? JSONDecoder().decode([String: String].self, from: data) else { return }
        raw = decoded
    }

    private func persist() {
        guard let url = fileURL else { return }
        guard let data = try? JSONEncoder().encode(raw) else { return }
        try? data.write(to: url, options: .atomic)
    }

    // Web版 store.get(k,d) 相当: "kyono_"+kの値をJSON.parseして返す。欠落/型不一致時はdefaultValue
    // (Web版のtry{JSON.parse}catch{return d}を踏襲。壊れたデータで起動が止まらないことが目的)。
    public func get<T: Decodable>(_ key: String, default defaultValue: T) -> T {
        guard let s = raw["kyono_" + key], let data = s.data(using: .utf8) else { return defaultValue }
        return (try? JSONDecoder().decode(T.self, from: data)) ?? defaultValue
    }

    // Web版 store.set(k,v) 相当: "kyono_"+kにJSON.stringify(v)を保存。成功可否を返す(Web版のtry/catch踏襲)。
    @discardableResult
    public func set<T: Encodable>(_ key: String, _ value: T) -> Bool {
        guard let data = try? JSONEncoder().encode(value), let s = String(data: data, encoding: .utf8) else { return false }
        raw["kyono_" + key] = s
        persist()
        return true
    }

    // ---- インポート/エクスポート・未知キーpassthrough用の生アクセス(型を知らないキーもそのまま運べる) ----

    // "kyono_"始まりの全キー→生JSON文字列(未加工。buildExportStringのdata部分そのもの)
    public var allRawKyonoEntries: [String: String] {
        raw.filter { $0.key.hasPrefix("kyono_") }
    }

    public func rawValue(fullKey: String) -> String? { raw[fullKey] }

    // 生JSON文字列のまま書き込む(インポート用。型を知らないpassthroughキーもこれで保存できる)
    public func setRaw(_ fullKey: String, _ jsonString: String) {
        raw[fullKey] = jsonString
        persist()
    }

    public func removeRaw(_ fullKey: String) {
        raw.removeValue(forKey: fullKey)
        persist()
    }
}
