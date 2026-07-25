package jp.ogatore.kyouno.safety

// ネイティブ移植 Step 2(マスタープラン§3-1・§3-2・§6 Step 2点4): 相談室の応答パイプライン骨格。
// Web版対応元: index.html:3380-3382 (crisisHit → redFlagHit → 通常インテント の順で評価し、
// ヒット時は他を一切見ずに即return)。
//
// この優先順序を落とすと「胸痛にストレッチ動画を案内する」医療安全事故になるため、順序自体をテストで縛る
// (engine-fixtures。マスタープラン§3-1第1項)。判定そのものはSafetyGateの4関数のみが行い、
// このファイルは「どの順で聞くか」「crisis/赤旗のときは動画もfollowupも出さない」という
// パイプラインの形だけを持つ。UI層(将来のCompose画面)はこの結果を表示するだけで、判定を1行も再実装しないこと。
//
// Step 2時点ではscoreIntents(通常応答の動画選定ロジック)は未移植のため、通常応答の中身(動画/文面選定)は
// 対象外。verdict = Normal であること自体だけをこの段階の合格基準とする(§6 Step 2の記載どおり)。

sealed class SoudanVerdict {
    data object Crisis : SoudanVerdict()
    data class RedFlag(val kind: String) : SoudanVerdict() // "state" または "symptom"
    data object Normal : SoudanVerdict()
}

data class SoudanResponse(
    val verdict: SoudanVerdict,
    val empathy: String,
    val message: String,
    val hasVideo: Boolean,
    val hasFollowup: Boolean,
    val needsReferral: Boolean,
)

object SoudanEngine {
    fun respond(raw: String): SoudanResponse {
        val kb = SafetyKBLoader.shared
        val n = SafetyGate.norm(raw)

        // ①crisis最優先(index.html:3380と同じ順序。窓口案内のみ・動画/followupは組み立てない)
        if (SafetyGate.crisisHit(n)) {
            return SoudanResponse(
                verdict = SoudanVerdict.Crisis,
                empathy = "",
                message = kb.crisis.answer,
                hasVideo = false,
                hasFollowup = false,
                needsReferral = false,
            )
        }

        // ②赤旗(crisisの次点。needsReferral=true・動画を出さない。kind=state→answerState/それ以外→answer)
        if (SafetyGate.redFlagHit(n)) {
            // redFlagHit=trueならkindは必ずnon-nullのはずだが、万一の不整合時も安全側(symptom=従来の受診案内文面)に倒す
            val kind = SafetyGate.redFlagKind(n) ?: "symptom"
            val message = if (kind == "state") kb.redFlags.answerState else kb.redFlags.answer
            return SoudanResponse(
                verdict = SoudanVerdict.RedFlag(kind),
                empathy = kb.redFlags.empathy,
                message = message,
                hasVideo = false,
                hasFollowup = false,
                needsReferral = true,
            )
        }

        // ③通常(Step 2時点ではverdictの分岐確認のみが目的。動画/文面選定はStep 6以降)
        return SoudanResponse(
            verdict = SoudanVerdict.Normal,
            empathy = "",
            message = "",
            hasVideo = false,
            hasFollowup = false,
            needsReferral = false,
        )
    }
}
