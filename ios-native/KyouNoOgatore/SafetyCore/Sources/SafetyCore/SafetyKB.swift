import Foundation

// soudan-kb.json（scripts-native/gen-safety-kb.mjsがsoudan-kb.jsから生成。マスタープラン§3-2）の
// redFlags/crisisセクションだけを読み込む最小Codableモデル。Step 2時点ではintents(122件)は未使用のため
// デコード対象に含めない(Codableは宣言していないキーを無視するので、フルKBのJSONをそのまま渡してよい)。
struct SafetyKB: Decodable {
    struct RedFlags: Decodable {
        let kw: [String]
        let stateKw: [String]?
        let empathy: String
        let answer: String
        let answerState: String
    }
    struct Crisis: Decodable {
        let kw: [String]
        let answer: String
    }
    let redFlags: RedFlags
    let crisis: Crisis
}

enum SafetyKBLoader {
    /// SafetyCoreパッケージに同梱した soudan-kb.json を読み込む。
    /// 失敗時はビルド構成の異常(リソース同梱漏れ)なので、判定ロジック内でのfatalError濫用とは別物として許容する
    /// (アプリ起動シーケンスの一部であり、テストのアサーションループ内で1件ずつ評価される経路ではないため)。
    static let shared: SafetyKB = {
        guard let url = Bundle.module.url(forResource: "soudan-kb", withExtension: "json") else {
            fatalError("soudan-kb.json がSafetyCoreパッケージに同梱されていない")
        }
        do {
            let data = try Data(contentsOf: url)
            return try JSONDecoder().decode(SafetyKB.self, from: data)
        } catch {
            fatalError("soudan-kb.json のデコードに失敗: \(error)")
        }
    }()
}
