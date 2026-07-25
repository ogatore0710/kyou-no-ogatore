package jp.ogatore.kyouno.safety

// ネイティブ移植 Step 2+Step 6(マスタープラン§3-1・§3-2・§6 Step 2/6): 相談室の応答パイプライン。
// Web版対応元: index.html:3373-3394 sdSend (crisisHit → redFlagHit → scoreIntents通常応答 → フォールバック
// の順で評価し、ヒット時は他を一切見ずに即return)。
//
// この優先順序を落とすと「胸痛にストレッチ動画を案内する」医療安全事故になるため、順序自体をテストで縛る
// (engine-fixtures。マスタープラン§3-1第1項)。crisis/赤旗の判定そのものはSafetyGateの4関数のみが行い、
// このファイルは「どの順で聞くか」「crisis/赤旗のときは動画もfollowupも出さない」「通常時はどのintentを
// どう選び、どう文面を組み立てるか」を持つ。intentのスコアリング・文面組み立ては安全判定(judgment)ではなく
// 通常会話コンテンツの選定であり、SafetyGateの管轄外(マスタープラン§3-2はnorm/crisisHit/redFlagHit/
// redFlagKindの4関数のみを隔離対象とする)。UI層はこのファイルの関数を呼ぶだけで、判定はもちろん
// intentスコアリングも一切再実装しないこと。

sealed class SoudanVerdict {
    data object Crisis : SoudanVerdict()
    data class RedFlag(val kind: String) : SoudanVerdict() // "state" または "symptom"
    data object Normal : SoudanVerdict()
}

data class SoudanVideoRef(val videoId: String, val note: String)
data class SoudanChip(val id: String, val label: String)

data class SoudanResponse(
    val verdict: SoudanVerdict,
    val empathy: String,
    // 通常応答時はmitate(見立て)文面。crisis/赤旗はその案内文。フォールバックは「まだ勉強中」文面。
    val message: String,
    val keizoku: String = "", // 継続アドバイス。通常のintent応答のみ使用
    val hasVideo: Boolean,
    val video: SoudanVideoRef? = null,
    val hasFollowup: Boolean,
    val followupChips: List<SoudanChip> = emptyList(),
    val nextBestChip: SoudanChip? = null, // 複数の悩みが同時ヒットしたときの次点intentへの導線チップ
    val nearmissChips: List<SoudanChip> = emptyList(), // フォールバック時の「近いのはこのあたりかも」チップ
    val needsReferral: Boolean,
    val intentId: String? = null,
)

object SoudanEngine {
    // 新規intent応答は常にvideos[0]を返す(index.html:3287。既出フィルタなし)ため、ここではshownVideoIdsを
    // 使わない。既出フィルタが要るのはfollowupの「他には?」「もっと短いの」(respondToFollowup)のみ——
    // index.html:2999 sdCtx.shownVideoIds(セッション内のみ・保存なし)相当をUI層が保持し、そちらに渡す設計。
    fun respond(raw: String): SoudanResponse {
        val kb = SafetyKBLoader.shared
        val n = SafetyGate.norm(raw)

        // ①crisis最優先(窓口案内のみ・動画/followupは組み立てない)
        if (SafetyGate.crisisHit(n)) return crisisResponse(kb)

        // ②赤旗(crisisの次点。needsReferral=true・動画を出さない)
        if (SafetyGate.redFlagHit(n)) return redFlagResponse(kb, n)

        // ③産後(慢性・時期不明)の一言注意フラグ(index.html:3239 sdSangoChronic)。赤旗を抜けた後だけ使う
        val sango = n.contains("産後") || n.contains("出産後")

        // ④通常: スコア=ヒットkwの文字数合計。閾値2文字未満はフォールバックへ(index.html:3384-3388)
        val ranked = scoreIntents(n, kb.intents)
        if (ranked.isNotEmpty() && ranked[0].score >= 2) {
            val nextBest = if (ranked.size > 1 && ranked[1].score >= 2) ranked[1].intent else null
            return intentResponse(ranked[0].intent, nextBest, sango, kb)
        }

        return fallbackResponse(ranked)
    }

    // 直接チップタップ(index.html:3361 sdChipTap)相当。スコアリングをバイパスして特定intentへ直接回答する。
    fun respondToIntent(id: String): SoudanResponse? {
        val kb = SafetyKBLoader.shared
        val intent = kb.intents.find { it.id == id } ?: return null
        return intentResponse(intent, null, sango = false, kb = kb)
    }

    // followupチップタップ(index.html:3367 sdFollowupTap→3335 sdAnswerFollowup)相当。
    fun respondToFollowup(id: String, lastIntentId: String?, shownVideoIds: List<String>): SoudanResponse? {
        val kb = SafetyKBLoader.shared
        val f = kb.commonFollowups.find { it.id == id } ?: return null
        val intent = lastIntentId?.let { lid -> kb.intents.find { it.id == lid } }
        return when (f.mode) {
            "shorter" -> pickAnotherVideo(intent, shownVideoIds, "それなら、みじかめのこれ！", "どの悩みのことか、下のチップから教えてもらえたら ぴったりの短いのを出すよ😊")
            "more" -> pickAnotherVideo(intent, shownVideoIds, "こっちもおすすめ！", "どの悩みか、下のチップから教えてくれたら おすすめを出すよ😊")
            else -> textOnly(f.answer ?: "")
        }
    }

    private fun crisisResponse(kb: SafetyKB) = SoudanResponse(
        verdict = SoudanVerdict.Crisis,
        empathy = "",
        message = kb.crisis.answer,
        hasVideo = false,
        hasFollowup = false,
        needsReferral = false,
    )

    private fun redFlagResponse(kb: SafetyKB, n: String): SoudanResponse {
        // redFlagHit=trueならkindは必ずnon-nullのはずだが、万一の不整合時も安全側(症状系案内)に倒す
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

    private data class ScoredIntent(val intent: SafetyKB.Intent, val score: Int, val firstHit: Int)

    // index.html:3240 sdScoreIntents の1:1移植。score=ヒットkwの文字数合計、同点はkw配列内の出現順(firstHit)が早い方優先。
    private fun scoreIntents(n: String, intents: List<SafetyKB.Intent>): List<ScoredIntent> {
        val out = mutableListOf<ScoredIntent>()
        for (intent in intents) {
            var score = 0
            var firstHit = -1
            intent.kw.forEachIndexed { j, kw0 ->
                val k = SafetyGate.norm(kw0)
                if (k.isNotEmpty() && n.contains(k)) {
                    score += k.length
                    if (firstHit < 0) firstHit = j
                }
            }
            if (score > 0) out.add(ScoredIntent(intent, score, firstHit))
        }
        return out.sortedWith(compareByDescending<ScoredIntent> { it.score }.thenBy { it.firstHit })
    }

    // index.html:3274 sdAnswerIntent の1:1移植。動画は常にvideos[0](「まずは1本」。残りはfollowupの
    // 「他には?」「もっと短いの」で出す=sdUnseenによる既出フィルタは適用しない)。
    // タイプ挨拶によるパーソナライズ(sdTypeFlavor/sdTypeBoost)は診断結果(state.type)への依存が生じ、
    // Step6の作業項目(SafetyGate/SoudanEngine呼び出しのみのUI)の範囲を超えるため未移植。
    private fun intentResponse(intent: SafetyKB.Intent, nextBest: SafetyKB.Intent?, sango: Boolean, kb: SafetyKB): SoudanResponse {
        val pick = intent.videos.firstOrNull()
        var keizoku = intent.keizoku
        if (intent.safety) {
            keizoku += (if (keizoku.isNotEmpty()) "\n" else "") + "※つよい痛みやしびれが出たら中止して、医療機関に相談してね"
        }
        if (sango) {
            keizoku += (if (keizoku.isNotEmpty()) "\n" else "") +
                "※産後の体は回復のペースに個人差があるから、痛みが強い日・出血や発熱があるときは無理せず受診してね。" +
                "産後まもない時期(〜2ヶ月)や帝王切開のあとは、お医者さんのOKをもらってからにしよう"
        }
        val followupChips = intent.followups.mapNotNull { fid ->
            kb.commonFollowups.find { f -> f.id == fid }?.let { f -> SoudanChip(f.id, f.chip) }
                ?: kb.intents.find { i2 -> i2.id == fid }?.let { li -> SoudanChip(li.id, li.chip) }
        }
        return SoudanResponse(
            verdict = SoudanVerdict.Normal,
            empathy = intent.empathy,
            message = intent.mitate,
            keizoku = keizoku,
            hasVideo = pick != null,
            video = pick?.let { v -> SoudanVideoRef(v.v, v.note) },
            hasFollowup = followupChips.isNotEmpty(),
            followupChips = followupChips,
            nextBestChip = nextBest?.let { nb -> SoudanChip(nb.id, nb.chip) },
            needsReferral = false,
            intentId = intent.id,
        )
    }

    // index.html:3338-3353 sdAnswerFollowupのshorter/more分岐の簡略移植。動画の長さ情報(分数)は
    // ネイティブ側カタログに未同梱のため、Web版の「7分以下優先ソート」は移植せず、単純に未出動画の
    // 先頭を返す(意味的な安全性への影響はない=単なる選定順序の簡略化)。
    private fun pickAnotherVideo(intent: SafetyKB.Intent?, shownVideoIds: List<String>, foundMsg: String, noIntentMsg: String): SoudanResponse {
        if (intent == null) return textOnly(noIntentMsg)
        val pick = intent.videos.firstOrNull { v -> shownVideoIds.none { shown -> shown == v.v } }
            ?: return textOnly("いま出せるおすすめは以上!まずはさっきの1本からどうぞ😊")
        return SoudanResponse(
            verdict = SoudanVerdict.Normal,
            empathy = "",
            message = foundMsg,
            hasVideo = true,
            video = SoudanVideoRef(pick.v, pick.note),
            hasFollowup = false,
            needsReferral = false,
            intentId = intent.id,
        )
    }

    private fun textOnly(msg: String) = SoudanResponse(
        verdict = SoudanVerdict.Normal,
        empathy = "",
        message = msg,
        hasVideo = false,
        hasFollowup = false,
        needsReferral = false,
    )

    // index.html:3323 sdAnswerFallback の1:1移植(メール送信導線はUI層で組み立てる。ここではnearmissのみ)。
    private fun fallbackResponse(ranked: List<ScoredIntent>): SoudanResponse {
        val near = ranked.filter { it.score > 0 }.take(3).map { SoudanChip(it.intent.id, it.intent.chip) }
        return SoudanResponse(
            verdict = SoudanVerdict.Normal,
            empathy = "",
            message = "ごめんね、その悩みはまだ勉強中🙏\n近いのはこのあたりかも！下のチップからどうぞ",
            hasVideo = false,
            hasFollowup = false,
            nearmissChips = near,
            needsReferral = false,
        )
    }
}
