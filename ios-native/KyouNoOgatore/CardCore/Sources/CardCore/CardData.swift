import Foundation

// card-data.json(scripts-native/gen-card-data.mjsがindex.html/app-card.jsから生成。マスタープラン§2-4)の
// Decodableモデル。フィールド名はJS側の定数名(SCREAMING_SNAKE_CASE)を意図的にそのまま使う——
// このデータは「人が読み書きするコード」ではなく「Web版の定数テーブルの1:1写像」であり、9月の
// 差分同期(§5-2)でJS側の変更を機械的に追いやすくすることを、Swiftの命名規約より優先している。
public struct CardTheme: Decodable {
    public let name: String
    public let bg: [String]
    public let main: String
    public let deco: [String]
}

// UI/UXパリティ監査GO-1(2026-07-28): m(祝いメッセージ)/q(先輩の声引用)は元々d/tだけに
// 間引かれてスキップされていた(節目のお祝い演出が通常日と見分けつかなくなっていた欠落の
// 原因)。gen-card-data.mjsの機械抽出をd/t/m/qの4項目に広げたので、ここも合わせる。
public struct MilestoneInfo: Decodable {
    public let d: Int
    public let t: String
    public let m: String
    public let q: String
}

public struct CharaFile: Decodable {
    public let file: String
    public let w: Int
}

public struct SeasonCard: Decodable {
    public let key: String
    public let name: String
    public let bg: [String]
    public let ws: Int?
    public let we: Int?
}

public struct RareCard: Decodable {
    public let key: String
    public let name: String
    public let bg: [String]
}

public struct NormalCard: Decodable {
    public let name: String
    public let bg: [String]
    public let main: String
    public let deco: [String]
}

public struct TokuCard: Decodable {
    public let key: String
    public let name: String
    public let bg: [String]
}

public struct CardData: Decodable {
    public let CARD_THEMES: [CardTheme]
    public let GOLD: CardTheme
    public let MS: [MilestoneInfo]
    public let CHARA_FILES: [CharaFile]
    public let CARD_THEMES_V1_COUNT: Int
    public let CARD_THEMES_V2_FROM: Int
    public let MILESTONES: [Int]
    public let CARD_IMG_FROM: Int
    public let MILESTONE_MSG_VIDEO: String
    public let SEASON_CARDS: [SeasonCard]
    public let RARE_CARDS: [RareCard]
    public let NORMAL_CARDS: [NormalCard]
    public let TOKU_CARDS: [String: TokuCard] // キーはeffTotalの文字列("4".."1900")
    public let CARD_ROT_ORDER: [Int]
    // Step 7a(図鑑UI): index.html getDexStatus()が参照するヒント/フレーバー文言。
    public let DEX_TEASE: [String: String] // rareのkey→ロック中ヒント
    public let DEX_FLAVOR: [String: String] // toku/season/rareのkey→アンロック後フレーバー
    public let DEX_FLAVOR_NORMAL: [String: String] // normalのname→アンロック後フレーバー
    public let DEX_NORMAL_TEASE: String
}

public enum CardDataLoader {
    public static let shared: CardData = {
        guard let url = Bundle.module.url(forResource: "card-data", withExtension: "json") else {
            fatalError("card-data.json がCardCoreパッケージに同梱されていない")
        }
        do {
            return try JSONDecoder().decode(CardData.self, from: try Data(contentsOf: url))
        } catch {
            fatalError("card-data.json のデコードに失敗: \(error)")
        }
    }()
}
