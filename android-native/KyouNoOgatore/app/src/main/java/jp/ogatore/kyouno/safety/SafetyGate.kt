package jp.ogatore.kyouno.safety

// ネイティブ移植 Step 2(マスタープラン§3-4手順3): まずスタブ実装で「全赤」を確認するための土台。
// スタブは常に「安全でない側の誤値」を返す(norm=恒等・crisisHit/redFlagHit=常にfalse・redFlagKind=常にnull)。
// throw等で即死させない(1件目の失敗でテスト実行全体が止まると、どのケースが失敗しているか111件分わからなくなるため)。
//
// このあと norm → crisisHit → redFlagHit → redFlagKind の順に実装して緑化する(Task #22)。
object SafetyGate {
    fun norm(input: String?): String = input ?: ""

    fun crisisHit(normalized: String): Boolean = false

    fun redFlagHit(normalized: String): Boolean = false

    fun redFlagKind(normalized: String): String? = null
}
