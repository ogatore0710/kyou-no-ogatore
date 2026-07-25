import Foundation

// soudan-kb.json（scripts-native/gen-safety-kb.mjsがsoudan-kb.jsから生成。マスタープラン§3-2）の
// redFlags/crisisセクションを読み込むCodableモデル。Step 6(相談室UI通常応答)からintents(122件)/
// commonFollowups(4件)も追加デコードする(smalltalk(54件)はStep6の作業範囲外のため対象外のまま)。
// これらは判定(crisis/redFlag)とは無関係の通常会話コンテンツであり、SafetyGateの4関数(judgment)には
// 一切関与しない(マスタープラン§3-2の隔離対象はnorm/crisisHit/redFlagHit/redFlagKindの4関数のみ)。
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
    // soudan-kb.json intents[] の1要素。index.html:3274 sdAnswerIntent等が参照するフィールドのみ。
    struct Intent: Decodable {
        struct Video: Decodable {
            let v: String
            let note: String?
        }
        let id: String
        let chip: String
        let kw: [String]?
        let empathy: String?
        let mitate: String?
        let videos: [Video]?
        let keizoku: String?
        let followups: [String]?
        let safety: Bool?
    }
    // soudan-kb.json commonFollowups[] の1要素。mode: "text"|"shorter"|"more"(index.html:3335 sdAnswerFollowup)。
    struct CommonFollowup: Decodable {
        let id: String
        let chip: String
        let mode: String?
        let answer: String?
    }
    let redFlags: RedFlags
    let crisis: Crisis
    let intents: [Intent]
    let commonFollowups: [CommonFollowup]
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
