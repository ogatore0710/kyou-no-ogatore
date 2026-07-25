import Foundation

// ネイティブ移植 Step 2(マスタープラン§3-2・§3-3): #きょうのオガトレ相談室の安全系判定4関数の1:1移植。
// Web版の対応元: soudan-ai-poc/norm.mjs（= index.html sdNorm/sdRedFlagHit/sdRedFlagKind/sdCrisisHit の忠実移植）。
//
// 判定ロジックはこのファイルにのみ存在させること（マスタープラン§3-2「判定関数の置き場を1箇所に隔離」）。
// UI層（将来のSoudanSheetView等）に判定を1行でも書かない。
//
// 実装規約(§1-4・§3-3。プラットフォーム差が出やすい箇所の防衛線):
//   - 文字クラス判定(許可リスト削除)はregexでなく数値範囲比較で自前実装する。
//   - Character(書記素クラスタ)単位の走査・比較は禁止。すべてUnicode.Scalar単位で処理する。
//     Swift標準のString.contains/range(of:)は暗黙に正準等価(NFC/NFD無差別)比較になりうるため使わない。
//   - 合成濁点(「か」+結合濁点U+3099)は許可リスト削除で基底文字だけが残る(濁点が消える)。
//     このJS版のバグ込み挙動をそのまま再現する(§3-3・norm-golden.jsonで固定)。
//   - 「寝転」除去5語は順次replaceで実装しない。正規表現の選択置換(NSRegularExpression)を使う
//     (leftmost選択・非再スキャンのセマンティクスがJSのreplace(/…|…/g,"")と一致するため)。

public enum SafetyGate {
    /// index.html:3009 sdNorm(=norm.mjs:6-11)の忠実移植。
    /// 4ステップ: ①toLowerCase(null→"") ②全角英数→半角(-0xFEE0) ③カタカナU+30A1-30F6→ひらがな(-0x60)
    /// ④許可リスト[0-9a-zぁ-ゖー一-鿿々]以外を全削除。
    public static func norm(_ input: String?) -> String {
        let lowered = (input ?? "").lowercased()
        var scalars: [Unicode.Scalar] = []
        scalars.reserveCapacity(lowered.unicodeScalars.count)
        for s in lowered.unicodeScalars {
            let v = s.value
            if isFullwidthAsciiRange(v) {
                scalars.append(Unicode.Scalar(v - 0xFEE0)!)
            } else if isFullwidthKatakanaRange(v) {
                scalars.append(Unicode.Scalar(v - 0x60)!)
            } else {
                scalars.append(s)
            }
        }
        var out = String.UnicodeScalarView()
        for s in scalars where isAllowed(s.value) {
            out.append(s)
        }
        return String(out)
    }

    /// index.html:3229 sdCrisisHit の忠実移植。「寝転」除去なし(redFlagHitとの意図的な差)。
    public static func crisisHit(_ normalized: String) -> Bool {
        let kb = SafetyKBLoader.shared
        return kwHit(normalized, kb.crisis.kw)
    }

    /// index.html:3207 sdRedFlagHit の忠実移植。
    /// 「寝転|ねころ|寝ころ|ねっころ|寝っこ」除去→kw部分文字列包含(2文字未満kw無効)。
    public static func redFlagHit(_ normalized: String) -> Bool {
        let kb = SafetyKBLoader.shared
        let n = removeNekorogari(normalized)
        return kwHit(n, kb.redFlags.kw)
    }

    /// index.html:3218 sdRedFlagKind の忠実移植。
    /// 症状語ヒット即"symptom"(安全側優先)・stateKwはフラグのみ→"state"。
    public static func redFlagKind(_ normalized: String) -> String? {
        let kb = SafetyKBLoader.shared
        let stateSet = Set((kb.redFlags.stateKw ?? []).map { norm($0) })
        let n = removeNekorogari(normalized)
        var stateHit = false
        for kw0 in kb.redFlags.kw {
            let k = norm(kw0)
            if k.count >= 2, scalarsContain(n, k) {
                if stateSet.contains(k) {
                    stateHit = true
                } else {
                    return "symptom"
                }
            }
        }
        return stateHit ? "state" : nil
    }

    // MARK: - 内部ヘルパー

    private static func kwHit(_ normalized: String, _ kwList: [String]) -> Bool {
        for kw0 in kwList {
            let k = norm(kw0)
            if k.count >= 2, scalarsContain(normalized, k) {
                return true
            }
        }
        return false
    }

    /// 「寝転|ねころ|寝ころ|ねっころ|寝っこ」を正規表現の選択置換(global・leftmost)で除去する。
    /// NSRegularExpressionはUTF-16(NSString)ベースで動作し、JSのreplace(/…|…/g,"")と同じ
    /// leftmost・非再スキャンのセマンティクスになる。
    private static let nekorogariRegex = try! NSRegularExpression(pattern: "寝転|ねころ|寝ころ|ねっころ|寝っこ")
    private static func removeNekorogari(_ s: String) -> String {
        let ns = s as NSString
        return nekorogariRegex.stringByReplacingMatches(
            in: s, options: [], range: NSRange(location: 0, length: ns.length), withTemplate: ""
        )
    }

    /// Unicode.Scalar単位での部分文字列包含判定(String.contains/range(of:)の正準等価比較を避けるため自前実装)。
    private static func scalarsContain(_ haystack: String, _ needle: String) -> Bool {
        let h = Array(haystack.unicodeScalars.map { $0.value })
        let n = Array(needle.unicodeScalars.map { $0.value })
        if n.isEmpty || n.count > h.count { return n.isEmpty }
        let limit = h.count - n.count
        outer: for start in 0...limit {
            for i in 0..<n.count where h[start + i] != n[i] {
                continue outer
            }
            return true
        }
        return false
    }

    // 全角英数(Ａ-Ｚａ-ｚ０-９ = U+FF21-FF3A, U+FF41-FF5A, U+FF10-FF19)
    private static func isFullwidthAsciiRange(_ v: UInt32) -> Bool {
        (0xFF21...0xFF3A).contains(v) || (0xFF41...0xFF5A).contains(v) || (0xFF10...0xFF19).contains(v)
    }
    // 全角カタカナ ァ-ヶ = U+30A1-30F6
    private static func isFullwidthKatakanaRange(_ v: UInt32) -> Bool {
        (0x30A1...0x30F6).contains(v)
    }
    // 許可リスト [0-9a-zぁ-ゖー一-鿿々]
    private static func isAllowed(_ v: UInt32) -> Bool {
        (0x30...0x39).contains(v)      // 0-9
            || (0x61...0x7A).contains(v)   // a-z
            || (0x3041...0x3096).contains(v) // ぁ-ゖ
            || v == 0x30FC                 // ー
            || (0x4E00...0x9FFF).contains(v) // 一-鿿
            || v == 0x3005                 // 々
    }
}
