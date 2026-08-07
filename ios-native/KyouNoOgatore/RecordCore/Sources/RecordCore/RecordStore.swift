import Foundation

// ネイティブ移植 Step 3(マスタープラン§2-3): Web版のlocalStorage(kyono_*プレフィックス・
// キーごとにJSON.stringify済み文字列を保持)を、Documents内の単一JSONファイル`kyono-store.json`に
// 1:1対応させたKVストア。buildExportString/importDataの契約(§2-3)をそのまま成立させるため、
// 内部表現も「フルキー("kyono_"つき)→生JSON文字列」のまま保持する(localStorageと同じ形)。
// 起動時に全読み込み・変更のたび全書き戻し(§2-3の方針。サイズが小さいため十分)。
//
// TASK build36 R-58(Fable監査A-1+設計裁定・2026-08-07): 全消失シナリオへの防波堤。
// 書き込みは元から.atomicのため途中切れリスクは低いが、「load()のdecode失敗→沈黙で空→
// 次のset()が空同然で全上書き」の後段はAndroid版と同型だった。対策:
//  1) decode失敗時はまず寛容ロード(サルベージ): 手編集で生bool/null等が混入しても(過去実績あり・
//     MEMORY記載)、JSONテキスト表現を文字列として採用すればゼロ損失で自己修復できる
//     (二重JSONエンコード規約上、生trueの意図は文字列"true"のため)
//  2) それも無理な完全破損は .corrupt-<epoch秒> へ隔離して空で継続(唯一のコピーを上書きで
//     壊さない)。隔離moveすら失敗した場合のみpersistを封印する
// なおウィジェットはApp Group内の別ファイルwidget-summary.json(片道ミラー)しか読まないため、
// このファイルの破損処理がウィジェットへ波及することはない(設計レビューで実コード確認済み)。
public final class RecordStore {
    // R-58: 隔離ファイルは新しい順に2個まで残す(証拠保全と肥大化防止のバランス)。
    private static let quarantineKeep = 2

    private var raw: [String: String] // "kyono_xxx" -> JSON文字列(Web版のlocalStorage.getItem(key)相当)
    private let fileURL: URL?
    // R-58: 破損ファイルの隔離(退避move)にすら失敗したときだけtrue。唯一のコピーを
    // 上書きから守るため、そのセッション中のpersistを封じる(次回起動で再試行される)。
    private var persistDisabled = false

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
        guard let url = fileURL else { return }
        guard let data = try? Data(contentsOf: url) else {
            // ファイル無し=初回起動(または隔離直後の再起動)。古い隔離ファイルの整理だけはやる。
            Self.trimQuarantine(around: url)
            return
        }
        if let decoded = try? JSONDecoder().decode([String: String].self, from: data) {
            raw = decoded
            Self.trimQuarantine(around: url)
            return
        }
        if let salvaged = Self.salvage(data) {
            // 誤修復に備えて原本バイトを.bakに残してから採用(次のpersistで正規形に直る)。
            let bak = url.deletingLastPathComponent()
                .appendingPathComponent(url.lastPathComponent + ".bak")
            try? data.write(to: bak)
            raw = salvaged
            Self.trimQuarantine(around: url)
            return
        }
        // 完全破損: 証拠を保全して空で継続。全損の本質は「破損データの唯一のコピーを
        // 次のpersistが上書きすること」であり、隔離が成功すれば上書きしても失うものは無い。
        let quarantine = url.deletingLastPathComponent()
            .appendingPathComponent(url.lastPathComponent + ".corrupt-\(Int(Date().timeIntervalSince1970))")
        if (try? FileManager.default.moveItem(at: url, to: quarantine)) == nil {
            persistDisabled = true
        }
        Self.trimQuarantine(around: url)
    }

    // R-58: 寛容ロード。トップレベルがJSONオブジェクトなら、値が文字列以外(生bool/null/数値/
    // オブジェクト等)でも「そのJSONテキスト表現を文字列として」採用する。
    private static func salvage(_ data: Data) -> [String: String]? {
        guard let obj = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] else { return nil }
        var out: [String: String] = [:]
        for (k, v) in obj {
            if let s = v as? String {
                out[k] = s
            } else if let fragment = try? JSONSerialization.data(withJSONObject: v, options: [.fragmentsAllowed]),
                      let text = String(data: fragment, encoding: .utf8) {
                out[k] = text
            } else {
                return nil // 表現不能な値(想定外)は無理に直さず完全破損側へ
            }
        }
        return out
    }

    private static func trimQuarantine(around url: URL) {
        let dir = url.deletingLastPathComponent()
        let prefix = url.lastPathComponent + ".corrupt-"
        guard let names = try? FileManager.default.contentsOfDirectory(atPath: dir.path) else { return }
        let quarantined = names.filter { $0.hasPrefix(prefix) }.sorted(by: >)
        for name in quarantined.dropFirst(quarantineKeep) {
            try? FileManager.default.removeItem(at: dir.appendingPathComponent(name))
        }
    }

    private func persist() {
        guard let url = fileURL, !persistDisabled else { return }
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
