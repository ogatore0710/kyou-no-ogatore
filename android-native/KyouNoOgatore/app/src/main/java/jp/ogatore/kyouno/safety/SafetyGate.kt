package jp.ogatore.kyouno.safety

// ネイティブ移植 Step 2(マスタープラン§3-2・§3-3): #きょうのオガトレ相談室の安全系判定4関数の1:1移植。
// Web版の対応元: soudan-ai-poc/norm.mjs（= index.html sdNorm/sdRedFlagHit/sdRedFlagKind/sdCrisisHit の忠実移植）。
//
// 判定ロジックはこのファイルにのみ存在させること（マスタープラン§3-2「判定関数の置き場を1箇所に隔離」）。
// UI層(将来のCompose画面)に判定を1行でも書かない。
//
// 実装規約(§1-4・§3-3。プラットフォーム差が出やすい箇所の防衛線):
//   - 文字クラス判定(許可リスト削除)はregexでなく数値範囲比較で自前実装する。
//   - KotlinのCharはJava/JSと同じUTF-16コード単位そのものなので、for(c in string)によるchar単位の走査は
//     JSの1文字=1コード単位の走査とそのまま対応する(サロゲートペア分割込みで一致。BMP外の絵文字等は
//     許可リストのどのレンジにも一致せず単位ごとに脱落するため、結果は変わらない)。
//   - JVMのString.contains()はUnicode正準等価比較をしない(生のUTF-16列比較。Swiftとは違いここは素朴でよい)。
//   - 合成濁点(「か」+結合濁点U+3099)は許可リスト削除で基底文字だけが残る(濁点が消える)。
//     このJS版のバグ込み挙動をそのまま再現する(§3-3・norm-golden.jsonで固定)。
//   - 「寝転」除去5語は順次replaceで実装しない。正規表現の選択置換(Regex.replace)を使う
//     (leftmost選択・非再スキャンのセマンティクスがJSのreplace(/…|…/g,"")と一致するため)。

object SafetyGate {
    // index.html:3009 sdNorm(=norm.mjs:6-11)の忠実移植。
    // 4ステップ: ①lowercase(null→"") ②全角英数→半角(-0xFEE0) ③カタカナU+30A1-30F6→ひらがな(-0x60)
    // ④許可リスト[0-9a-zぁ-ゖー一-鿿々]以外を全削除。
    fun norm(input: String?): String {
        val lowered = (input ?: "").lowercase()
        val sb = StringBuilder(lowered.length)
        for (c in lowered) {
            val v = c.code
            val shifted = when {
                isFullwidthAsciiRange(v) -> (v - 0xFEE0)
                isFullwidthKatakanaRange(v) -> (v - 0x60)
                else -> v
            }
            if (isAllowed(shifted)) sb.append(shifted.toChar())
        }
        return sb.toString()
    }

    // index.html:3229 sdCrisisHit の忠実移植。「寝転」除去なし(redFlagHitとの意図的な差)。
    fun crisisHit(normalized: String): Boolean {
        val kb = SafetyKBLoader.shared
        return kwHit(normalized, kb.crisis.kw)
    }

    // index.html:3207 sdRedFlagHit の忠実移植。
    // 「寝転|ねころ|寝ころ|ねっころ|寝っこ」除去→kw部分文字列包含(2文字未満kw無効)。
    fun redFlagHit(normalized: String): Boolean {
        val kb = SafetyKBLoader.shared
        val n = removeNekorogari(normalized)
        return kwHit(n, kb.redFlags.kw)
    }

    // index.html:3218 sdRedFlagKind の忠実移植。
    // 症状語ヒット即"symptom"(安全側優先)・stateKwはフラグのみ→"state"。
    fun redFlagKind(normalized: String): String? {
        val kb = SafetyKBLoader.shared
        val stateSet = (kb.redFlags.stateKw ?: emptyList()).map { norm(it) }.toSet()
        val n = removeNekorogari(normalized)
        var stateHit = false
        for (kw0 in kb.redFlags.kw) {
            val k = norm(kw0)
            if (k.length >= 2 && n.contains(k)) {
                if (stateSet.contains(k)) {
                    stateHit = true
                } else {
                    return "symptom"
                }
            }
        }
        return if (stateHit) "state" else null
    }

    // --- 内部ヘルパー ---

    private fun kwHit(normalized: String, kwList: List<String>): Boolean {
        for (kw0 in kwList) {
            val k = norm(kw0)
            if (k.length >= 2 && normalized.contains(k)) return true
        }
        return false
    }

    // 「寝転|ねころ|寝ころ|ねっころ|寝っこ」を正規表現の選択置換(global・leftmost)で除去する。
    // Kotlin(java.util.regex)の置換はleftmost・非再スキャンで、JSのreplace(/…|…/g,"")と同じセマンティクス。
    private val nekorogariRegex = Regex("寝転|ねころ|寝ころ|ねっころ|寝っこ")
    private fun removeNekorogari(s: String): String = nekorogariRegex.replace(s, "")

    // 全角英数(Ａ-Ｚａ-ｚ０-９ = U+FF21-FF3A, U+FF41-FF5A, U+FF10-FF19)
    private fun isFullwidthAsciiRange(v: Int): Boolean =
        v in 0xFF21..0xFF3A || v in 0xFF41..0xFF5A || v in 0xFF10..0xFF19

    // 全角カタカナ ァ-ヶ = U+30A1-30F6
    private fun isFullwidthKatakanaRange(v: Int): Boolean = v in 0x30A1..0x30F6

    // 許可リスト [0-9a-zぁ-ゖー一-鿿々]
    private fun isAllowed(v: Int): Boolean =
        v in 0x30..0x39 || // 0-9
            v in 0x61..0x7A || // a-z
            v in 0x3041..0x3096 || // ぁ-ゖ
            v == 0x30FC || // ー
            v in 0x4E00..0x9FFF || // 一-鿿
            v == 0x3005 // 々
}
