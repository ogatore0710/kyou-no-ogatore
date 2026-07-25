import Foundation

// ネイティブ移植 Step 2(マスタープラン§3-1・§3-2・§6 Step 2点4): 相談室の応答パイプライン骨格。
// Web版対応元: index.html:3380-3382 (crisisHit → redFlagHit → 通常インテント の順で評価し、
// ヒット時は他を一切見ずに即return)。
//
// この優先順序を落とすと「胸痛にストレッチ動画を案内する」医療安全事故になるため、順序自体をテストで縛る
// (engine-fixtures。マスタープラン§3-1第1項)。判定そのものはSafetyGateの4関数のみが行い、
// このファイルは「どの順で聞くか」「crisis/赤旗のときは動画もfollowupも出さない」という
// パイプラインの形だけを持つ。UI層（将来のSoudanSheetView）はこの結果を表示するだけで、
// 判定を1行も再実装しないこと。
//
// Step 2時点ではscoreIntents(通常応答の動画選定ロジック)は未移植のため、通常応答の中身(動画/文面選定)は
// 対象外。verdict === .normal であること自体だけをこの段階の合格基準とする(§6 Step 2の記載どおり)。

public enum SoudanVerdict: Equatable {
    case crisis
    case redFlag(kind: String) // "state" または "symptom"(redFlagHit=trueのとき必ずどちらかになる)
    case normal
}

public struct SoudanResponse: Equatable {
    public let verdict: SoudanVerdict
    public let empathy: String
    public let message: String
    public let hasVideo: Bool
    public let hasFollowup: Bool
    public let needsReferral: Bool
}

public enum SoudanEngine {
    public static func respond(to raw: String) -> SoudanResponse {
        let kb = SafetyKBLoader.shared
        let n = SafetyGate.norm(raw)

        // ①crisis最優先(index.html:3380と同じ順序。窓口案内のみ・動画/followupは組み立てない)
        if SafetyGate.crisisHit(n) {
            return SoudanResponse(
                verdict: .crisis,
                empathy: "",
                message: kb.crisis.answer,
                hasVideo: false,
                hasFollowup: false,
                needsReferral: false
            )
        }

        // ②赤旗(crisisの次点。needsReferral=true・動画を出さない。kind===state→answerState/それ以外→answer)
        if SafetyGate.redFlagHit(n) {
            let kind = SafetyGate.redFlagKind(n) ?? "symptom" // redFlagHit=trueならkindは必ずnon-nilのはずだが、
                                                               // 万一の不整合時も安全側(symptom=従来の受診案内文面)に倒す
            let message = kind == "state" ? kb.redFlags.answerState : kb.redFlags.answer
            return SoudanResponse(
                verdict: .redFlag(kind: kind),
                empathy: kb.redFlags.empathy,
                message: message,
                hasVideo: false,
                hasFollowup: false,
                needsReferral: true
            )
        }

        // ③通常(Step 2時点ではverdictの分岐確認のみが目的。動画/文面選定はStep 6以降)
        return SoudanResponse(
            verdict: .normal,
            empathy: "",
            message: "",
            hasVideo: false,
            hasFollowup: false,
            needsReferral: false
        )
    }
}
