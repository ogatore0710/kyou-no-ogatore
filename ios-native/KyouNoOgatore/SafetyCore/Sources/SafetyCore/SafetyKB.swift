import Foundation

// soudan-kb.json（scripts-native/gen-safety-kb.mjsがsoudan-kb.jsから生成。マスタープラン§3-2）の
// redFlags/crisisセクションを読み込むCodableモデル。Step 6(相談室UI通常応答)からintents(122件)/
// commonFollowups(4件)も追加デコードする(smalltalk(54件)はStep6の作業範囲外のため対象外のまま)。
// これらは判定(crisis/redFlag)とは無関係の通常会話コンテンツであり、SafetyGateの4関数(judgment)には
// 一切関与しない(マスタープラン§3-2の隔離対象はnorm/crisisHit/redFlagHit/redFlagKindの4関数のみ)。
// Step6でSoudanSheetView(アプリターゲット)がintents/commonFollowupsを直接参照するため、SafetyKB自体と
// intents/commonFollowups関連の型はpublicにする(SafetyCoreはSPMパッケージとしてアプリと別モジュールの
// ため、internalのままではモジュール境界を越えられない)。redFlags/crisis(判定用KB)はSoudanEngine/
// SafetyGate経由でのみ使う設計を維持し、あえてpublicにしない(UI層が安全判定データへ直接触れる経路を
// 増やさないため)。
public struct SafetyKB: Decodable {
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
    public struct Intent: Decodable {
        public struct Video: Decodable {
            public let v: String
            public let note: String?
        }
        public let id: String
        public let chip: String
        public let kw: [String]?
        public let empathy: String?
        public let mitate: String?
        public let videos: [Video]?
        public let keizoku: String?
        public let followups: [String]?
        public let safety: Bool?
    }
    // soudan-kb.json commonFollowups[] の1要素。mode: "text"|"shorter"|"more"(index.html:3335 sdAnswerFollowup)。
    public struct CommonFollowup: Decodable {
        public let id: String
        public let chip: String
        public let mode: String?
        public let answer: String?
    }
    let redFlags: RedFlags
    let crisis: Crisis
    public let intents: [Intent]
    public let commonFollowups: [CommonFollowup]
}

public enum SafetyKBLoader {
    /// SafetyCoreパッケージに同梱した soudan-kb.json を読み込む。
    /// 失敗時はビルド構成の異常(リソース同梱漏れ)なので、判定ロジック内でのfatalError濫用とは別物として許容する
    /// (アプリ起動シーケンスの一部であり、テストのアサーションループ内で1件ずつ評価される経路ではないため)。
    public static let shared: SafetyKB = {
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
