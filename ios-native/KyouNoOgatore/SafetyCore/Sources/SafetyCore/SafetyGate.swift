import Foundation

// ネイティブ移植 Step 2(マスタープラン§3-2・§3-3): #きょうのオガトレ相談室の安全系判定4関数の1:1移植。
// Web版の対応元: soudan-ai-poc/norm.mjs（= index.html sdNorm/sdRedFlagHit/sdRedFlagKind/sdCrisisHit の忠実移植）。
//
// 判定ロジックはこのファイルにのみ存在させること（マスタープラン§3-2「判定関数の置き場を1箇所に隔離」）。
// UI層（将来のSoudanSheetView等）に判定を1行でも書かない。
//
// ステータス: スタブ実装(2026-07-25 Step2着手時点)。「安全でない側の誤値」を返すことをあえての仕様とし、
// テストを先に全赤で確認してから実装を緑化する(マスタープラン§3-4手順3)。
//   norm            = 入力をそのまま返す(正規化しない)
//   crisisHit       = 常に false
//   redFlagHit      = 常に false
//   redFlagKind     = 常に nil
// fatalError()は使わない(テストプロセスごとクラッシュし1ケース目で中断するため使用禁止・§3-4手順3)。

public enum SafetyGate {
    /// index.html:3009 sdNorm(=norm.mjs:6-11)の忠実移植。
    /// 4ステップ: ①toLowerCase(null→"") ②全角英数→半角(-0xFEE0) ③カタカナU+30A1-30F6→ひらがな(-0x60)
    /// ④許可リスト[0-9a-zぁ-ゖー一-鿿々]以外を全削除。
    public static func norm(_ input: String?) -> String {
        return input ?? ""
    }

    /// index.html:3229 sdCrisisHit の忠実移植。「寝転」除去なし(redFlagHitとの意図的な差)。
    public static func crisisHit(_ normalized: String) -> Bool {
        return false
    }

    /// index.html:3207 sdRedFlagHit の忠実移植。
    /// 「寝転|ねころ|寝ころ|ねっころ|寝っこ」除去→kw部分文字列包含(2文字未満kw無効)。
    public static func redFlagHit(_ normalized: String) -> Bool {
        return false
    }

    /// index.html:3218 sdRedFlagKind の忠実移植。
    /// 症状語ヒット即"symptom"(安全側優先)・stateKwはフラグのみ→"state"。
    public static func redFlagKind(_ normalized: String) -> String? {
        return nil
    }
}
