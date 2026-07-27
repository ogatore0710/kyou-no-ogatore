import Foundation

// ネイティブ移植 Step 2+Step 6(マスタープラン§3-1・§3-2・§6 Step 2/6): 相談室の応答パイプライン。
// Web版対応元: index.html:3373-3394 sdSend (crisisHit → redFlagHit → scoreIntents通常応答 → フォールバック
// の順で評価し、ヒット時は他を一切見ずに即return)。
//
// この優先順序を落とすと「胸痛にストレッチ動画を案内する」医療安全事故になるため、順序自体をテストで縛る
// (engine-fixtures。マスタープラン§3-1第1項)。crisis/赤旗の判定そのものはSafetyGateの4関数のみが行い、
// このファイルは「どの順で聞くか」「crisis/赤旗のときは動画もfollowupも出さない」「通常時はどのintentを
// どう選び、どう文面を組み立てるか」を持つ。intentのスコアリング・文面組み立ては安全判定(judgment)ではなく
// 通常会話コンテンツの選定であり、SafetyGateの管轄外(マスタープラン§3-2はnorm/crisisHit/redFlagHit/
// redFlagKindの4関数のみを隔離対象とする)。UI層(SoudanSheetView)はこのファイルの関数を呼ぶだけで、
// 判定はもちろんintentスコアリングも一切再実装しないこと。

public enum SoudanVerdict: Equatable {
    case crisis
    case redFlag(kind: String) // "state" または "symptom"(redFlagHit=trueのとき必ずどちらかになる)
    case normal
}

public struct SoudanVideoRef: Equatable {
    public let videoId: String
    public let note: String
}

public struct SoudanChip: Equatable {
    public let id: String
    public let label: String
}

public struct SoudanResponse: Equatable {
    public let verdict: SoudanVerdict
    public let empathy: String
    // 通常応答時はmitate(見立て)文面。crisis/赤旗はその案内文。フォールバックは「まだ勉強中」文面。
    public let message: String
    public let keizoku: String // 継続アドバイス。通常のintent応答のみ使用
    public let hasVideo: Bool
    public let video: SoudanVideoRef?
    public let hasFollowup: Bool
    public let followupChips: [SoudanChip]
    public let nextBestChip: SoudanChip? // 複数の悩みが同時ヒットしたときの次点intentへの導線チップ
    public let nearmissChips: [SoudanChip] // フォールバック時の「近いのはこのあたりかも」チップ
    public let needsReferral: Bool
    public let intentId: String?
    // TASK-C2-2026-07-27-soudan-safety-copy-and-links: フォールバック(未マッチ)応答かどうかの表示専用
    // フラグ。UI層が「安全文言+3つの逃げ道リンクを出すべきか」を文字列マッチ等で再判定せずに済むよう、
    // 判定結果そのものを運ぶだけ(判定ロジック自体はfallbackResponse()のまま・ここでは増減しない)。
    public let isFallback: Bool

    public init(
        verdict: SoudanVerdict, empathy: String, message: String, keizoku: String = "",
        hasVideo: Bool, video: SoudanVideoRef? = nil, hasFollowup: Bool,
        followupChips: [SoudanChip] = [], nextBestChip: SoudanChip? = nil,
        nearmissChips: [SoudanChip] = [], needsReferral: Bool, intentId: String? = nil,
        isFallback: Bool = false
    ) {
        self.verdict = verdict; self.empathy = empathy; self.message = message; self.keizoku = keizoku
        self.hasVideo = hasVideo; self.video = video; self.hasFollowup = hasFollowup
        self.followupChips = followupChips; self.nextBestChip = nextBestChip
        self.nearmissChips = nearmissChips; self.needsReferral = needsReferral; self.intentId = intentId
        self.isFallback = isFallback
    }
}

public enum SoudanEngine {
    // 新規intent応答は常にvideos[0]を返す(index.html:3287。既出フィルタなし)ため、ここではshownVideoIdsを
    // 使わない。既出フィルタが要るのはfollowupの「他には?」「もっと短いの」(respondToFollowup)のみ——
    // index.html:2999 sdCtx.shownVideoIds(セッション内のみ・保存なし)相当をUI層が保持し、そちらに渡す設計。
    public static func respond(to raw: String) -> SoudanResponse {
        let kb = SafetyKBLoader.shared
        let n = SafetyGate.norm(raw)

        // ①crisis最優先(窓口案内のみ・動画/followupは組み立てない)
        if SafetyGate.crisisHit(n) { return crisisResponse(kb) }

        // ②赤旗(crisisの次点。needsReferral=true・動画を出さない)
        if SafetyGate.redFlagHit(n) { return redFlagResponse(kb, n) }

        // ③産後(慢性・時期不明)の一言注意フラグ(index.html:3239 sdSangoChronic)。赤旗を抜けた後だけ使う
        let sango = n.contains("産後") || n.contains("出産後")

        // ④通常: スコア=ヒットkwの文字数合計。閾値2文字未満はフォールバックへ(index.html:3384-3388)
        let ranked = scoreIntents(n, kb.intents)
        if let top = ranked.first, top.score >= 2 {
            let nextBest = (ranked.count > 1 && ranked[1].score >= 2) ? ranked[1].intent : nil
            return intentResponse(top.intent, nextBest, sango, kb)
        }

        return fallbackResponse(ranked)
    }

    // 直接チップタップ(index.html:3361 sdChipTap)相当。スコアリングをバイパスして特定intentへ直接回答する。
    public static func respondToIntent(id: String) -> SoudanResponse? {
        let kb = SafetyKBLoader.shared
        guard let intent = kb.intents.first(where: { $0.id == id }) else { return nil }
        return intentResponse(intent, nil, false, kb)
    }

    // followupチップタップ(index.html:3367 sdFollowupTap→3335 sdAnswerFollowup)相当。
    public static func respondToFollowup(id: String, lastIntentId: String?, shownVideoIds: [String]) -> SoudanResponse? {
        let kb = SafetyKBLoader.shared
        guard let f = kb.commonFollowups.first(where: { $0.id == id }) else { return nil }
        let intent = lastIntentId.flatMap { lid in kb.intents.first(where: { $0.id == lid }) }
        switch f.mode ?? "text" {
        case "shorter":
            return pickAnotherVideo(intent, shownVideoIds, found: "それなら、みじかめのこれ！", noIntent: "どの悩みのことか、下のチップから教えてもらえたら ぴったりの短いのを出すよ😊")
        case "more":
            return pickAnotherVideo(intent, shownVideoIds, found: "こっちもおすすめ！", noIntent: "どの悩みか、下のチップから教えてくれたら おすすめを出すよ😊")
        default:
            return textOnly(f.answer ?? "")
        }
    }

    private static func crisisResponse(_ kb: SafetyKB) -> SoudanResponse {
        SoudanResponse(verdict: .crisis, empathy: "", message: kb.crisis.answer, hasVideo: false, hasFollowup: false, needsReferral: false)
    }

    private static func redFlagResponse(_ kb: SafetyKB, _ n: String) -> SoudanResponse {
        // redFlagHit=trueならkindは必ずnon-nilのはずだが、万一の不整合時も安全側(症状系案内)に倒す
        let kind = SafetyGate.redFlagKind(n) ?? "symptom"
        let message = kind == "state" ? kb.redFlags.answerState : kb.redFlags.answer
        return SoudanResponse(
            verdict: .redFlag(kind: kind), empathy: kb.redFlags.empathy, message: message,
            hasVideo: false, hasFollowup: false, needsReferral: true
        )
    }

    private struct ScoredIntent { let intent: SafetyKB.Intent; let score: Int; let firstHit: Int }

    // index.html:3240 sdScoreIntents の1:1移植。score=ヒットkwの文字数合計、同点はkw配列内の出現順(firstHit)が早い方優先。
    private static func scoreIntents(_ n: String, _ intents: [SafetyKB.Intent]) -> [ScoredIntent] {
        var out: [ScoredIntent] = []
        for intent in intents {
            var score = 0
            var firstHit = -1
            for (j, kw0) in (intent.kw ?? []).enumerated() {
                let k = SafetyGate.norm(kw0)
                if !k.isEmpty && n.contains(k) {
                    score += k.count
                    if firstHit < 0 { firstHit = j }
                }
            }
            if score > 0 { out.append(ScoredIntent(intent: intent, score: score, firstHit: firstHit)) }
        }
        return out.sorted { a, b in a.score != b.score ? a.score > b.score : a.firstHit < b.firstHit }
    }

    // index.html:3274 sdAnswerIntent の1:1移植。動画は常にvideos[0](「まずは1本」。残りはfollowupの
    // 「他には?」「もっと短いの」で出す=sdUnseenによる既出フィルタは適用しない)。
    // タイプ挨拶によるパーソナライズ(sdTypeFlavor/sdTypeBoost)は診断結果(state.type)への依存が生じ、
    // Step6の作業項目(SafetyGate/SoudanEngine呼び出しのみのUI)の範囲を超えるため未移植。
    private static func intentResponse(_ intent: SafetyKB.Intent, _ nextBest: SafetyKB.Intent?, _ sango: Bool, _ kb: SafetyKB) -> SoudanResponse {
        let pick = (intent.videos ?? []).first
        var keizoku = intent.keizoku ?? ""
        if intent.safety ?? false {
            keizoku += (keizoku.isEmpty ? "" : "\n") + "※つよい痛みやしびれが出たら中止して、医療機関に相談してね"
        }
        if sango {
            keizoku += (keizoku.isEmpty ? "" : "\n") +
                "※産後の体は回復のペースに個人差があるから、痛みが強い日・出血や発熱があるときは無理せず受診してね。" +
                "産後まもない時期(〜2ヶ月)や帝王切開のあとは、お医者さんのOKをもらってからにしよう"
        }
        let followupChips: [SoudanChip] = (intent.followups ?? []).compactMap { fid in
            if let f = kb.commonFollowups.first(where: { $0.id == fid }) { return SoudanChip(id: f.id, label: f.chip) }
            if let li = kb.intents.first(where: { $0.id == fid }) { return SoudanChip(id: li.id, label: li.chip) }
            return nil
        }
        return SoudanResponse(
            verdict: .normal, empathy: intent.empathy ?? "", message: intent.mitate ?? "", keizoku: keizoku,
            hasVideo: pick != nil, video: pick.map { v in SoudanVideoRef(videoId: v.v, note: v.note ?? "") },
            hasFollowup: !followupChips.isEmpty, followupChips: followupChips,
            nextBestChip: nextBest.map { nb in SoudanChip(id: nb.id, label: nb.chip) },
            needsReferral: false, intentId: intent.id
        )
    }

    // index.html:3338-3353 sdAnswerFollowupのshorter/more分岐の簡略移植。動画の長さ情報(分数)は
    // ネイティブ側カタログに未同梱のため、Web版の「7分以下優先ソート」は移植せず、単純に未出動画の
    // 先頭を返す(意味的な安全性への影響はない=単なる選定順序の簡略化)。
    private static func pickAnotherVideo(_ intent: SafetyKB.Intent?, _ shownVideoIds: [String], found foundMsg: String, noIntent noIntentMsg: String) -> SoudanResponse {
        guard let intent else { return textOnly(noIntentMsg) }
        guard let pick = (intent.videos ?? []).first(where: { v in !shownVideoIds.contains(v.v) }) else {
            return textOnly("いま出せるおすすめは以上!まずはさっきの1本からどうぞ😊")
        }
        return SoudanResponse(
            verdict: .normal, empathy: "", message: foundMsg, hasVideo: true,
            video: SoudanVideoRef(videoId: pick.v, note: pick.note ?? ""), hasFollowup: false,
            needsReferral: false, intentId: intent.id
        )
    }

    private static func textOnly(_ msg: String) -> SoudanResponse {
        SoudanResponse(verdict: .normal, empathy: "", message: msg, hasVideo: false, hasFollowup: false, needsReferral: false)
    }

    // index.html:3323 sdAnswerFallback の1:1移植(メール送信導線はUI層で組み立てる。ここではnearmissのみ)。
    private static func fallbackResponse(_ ranked: [ScoredIntent]) -> SoudanResponse {
        let near = ranked.filter { $0.score > 0 }.prefix(3).map { SoudanChip(id: $0.intent.id, label: $0.intent.chip) }
        return SoudanResponse(
            verdict: .normal, empathy: "",
            message: "ごめんね、その悩みはまだ勉強中🙏\n近いのはこのあたりかも！下のチップからどうぞ",
            hasVideo: false, hasFollowup: false, nearmissChips: Array(near), needsReferral: false,
            isFallback: true
        )
    }
}
