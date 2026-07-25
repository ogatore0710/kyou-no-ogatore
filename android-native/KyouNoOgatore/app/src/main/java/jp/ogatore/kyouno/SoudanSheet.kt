package jp.ogatore.kyouno

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.ogatore.kyouno.record.RecordStore
import jp.ogatore.kyouno.safety.SafetyKBLoader
import jp.ogatore.kyouno.safety.SoudanEngine
import jp.ogatore.kyouno.safety.SoudanResponse
import jp.ogatore.kyouno.safety.SoudanVerdict
import kotlinx.serialization.Serializable

// ネイティブ移植 Step 6(マスタープラン§6 Step 6・§2-1「相談室エンジン sd*一式」対応): 相談室チャットUI
// (SoudanSheetView/SoudanSheetの1:1対応)。判定はStep2で移植済みのSafetyGate、通常応答の内容選定は
// このStepで拡張したSoudanEngineを呼ぶだけで、このファイルには判定コードを一切書かない
// (マスタープラン§3-2・§3-4手順6。grep確認対象)。
//
// カテゴリタブによる絞り込み(sdActiveCat/sdCatIds)はindex.html:2988-2994の1:1移植だが、これは
// 表示グルーピングのための単純な配列レンジ抽出であり安全判定ではないため、SoudanEngine(safetyパッケージ)
// ではなくこのUIファイル側に置く(マスタープラン§2-1のSoudanSheetView/SoudanSheet行の役割分担どおり)。
//
// 未移植(Step6のスコープ外として明示的に見送り): タイピングアニメーション・吹き出し分割タイミング演出
// (見た目の演出のみで安全性に無関係)・雑談(smalltalk 54件)・自由入力でのfollowup同義語マッチ(SD_FU_KW)
// ・動画サムネイル画像の読み込み(ネットワーク依存を増やさない方針)・タイプ診断との相性演出(sdTypeFlavor)。

data class SdCatDef(val key: String, val label: String, val from: String, val to: String)

// index.html:2981-2987 SOUDAN_CHIP_CATS の1:1移植(悩み一覧を5つの大項目タブに束ねる境界)。
val SD_CHIP_CATS = listOf(
    SdCatDef("body", "からだの部位で", "katakori", "oshirikori"),
    SdCatDef("ashi", "脚・足まわりで", "momomae", "ashidaru"),
    SdCatDef("scene", "状況・シーンで", "deskwork", "shakitto"),
    SdCatDef("nayami", "お悩み・体型で", "tsukare", "wakibara"),
    SdCatDef("howto", "やり方・Q&Aで", "mainichi", "kubinaru"),
)

// index.html:1827 PLAN_EXCLUDE_INTENTS の1:1移植(「即中止して様子見」が答えのintentは矛盾するため除外)。
private const val PLAN_EXCLUDE_INTENT = "itakunatta"

// index.html:1755 kyono_plan の1:1移植(store方式・保存は1本だけ)。
@Serializable
data class SdPlanData(val intentId: String, val label: String, val videos: List<String>, val start: String, val days: Int = 14)

// index.html:2989-2994 sdCatIds の1:1移植。
fun sdCatIntentIds(cat: SdCatDef): List<String> {
    val intents = SafetyKBLoader.shared.intents
    val i0 = intents.indexOfFirst { it.id == cat.from }
    val i1 = intents.indexOfFirst { it.id == cat.to }
    if (i0 < 0 || i1 < 0) return intents.map { it.id }
    return intents.subList(i0, i1 + 1).map { it.id }
}

sealed class SdBubble {
    data class Bot(val text: String, val red: Boolean = false, val videoId: String? = null) : SdBubble()
    data class User(val text: String) : SdBubble()
    data class PlanConfirm(val intentId: String, val label: String, val replacing: Boolean, var answered: Boolean = false) : SdBubble()
}

sealed class SdChipsMode {
    object None : SdChipsMode()
    data class Intents(val activeCat: String) : SdChipsMode()
    data class Followups(val intentId: String, val nextBestId: String?) : SdChipsMode()
    data class Nearmiss(val ids: List<String>) : SdChipsMode()
}

@Composable
fun SoudanSheet(store: RecordStore, openUrl: (String) -> Unit, onClose: () -> Unit) {
    val kb = remember { SafetyKBLoader.shared }
    var messages by remember { mutableStateOf(listOf<SdBubble>()) }
    var chipsMode by remember { mutableStateOf<SdChipsMode>(SdChipsMode.Intents("body")) }
    var lastIntentId by remember { mutableStateOf<String?>(null) }
    val shownVideoIds = remember { mutableStateListOf<String>() } // index.html:2999 sdCtx.shownVideoIds相当(セッション内のみ)
    var input by remember { mutableStateOf("") }
    var plan by remember { mutableStateOf(store.get("plan", null as SdPlanData?)) }

    // index.html:3090 sdPush相当。応答1件をbot/userの吹き出し列へ展開しchipsModeを更新する。
    fun applyResponse(userText: String?, r: SoudanResponse) {
        val newMsgs = mutableListOf<SdBubble>()
        if (userText != null) newMsgs.add(SdBubble.User(userText))
        val red = r.verdict is SoudanVerdict.RedFlag || r.verdict is SoudanVerdict.Crisis
        if (r.empathy.isNotEmpty()) newMsgs.add(SdBubble.Bot(r.empathy, red))
        if (r.message.isNotEmpty()) newMsgs.add(SdBubble.Bot(r.message, red))
        r.video?.let { v ->
            newMsgs.add(SdBubble.Bot(v.note.ifEmpty { "おすすめの1本" }, videoId = v.videoId))
            if (shownVideoIds.none { it == v.videoId }) shownVideoIds.add(v.videoId)
        }
        if (r.keizoku.isNotEmpty()) newMsgs.add(SdBubble.Bot(r.keizoku))
        messages = messages + newMsgs
        if (r.intentId != null) lastIntentId = r.intentId
        chipsMode = when {
            r.verdict is SoudanVerdict.Crisis -> SdChipsMode.None // index.html:3310 チップ・カテゴリタブなし
            r.verdict is SoudanVerdict.RedFlag -> SdChipsMode.Intents("body") // index.html:3304
            r.hasFollowup && r.intentId != null -> SdChipsMode.Followups(r.intentId, r.nextBestChip?.id)
            r.nearmissChips.isNotEmpty() -> SdChipsMode.Nearmiss(r.nearmissChips.map { c -> c.id })
            else -> SdChipsMode.Intents("body")
        }
    }

    fun sendText() {
        val raw = input.trim()
        if (raw.isEmpty()) return
        input = ""
        applyResponse(raw, SoudanEngine.respond(raw))
    }

    fun chipTap(id: String) {
        val intent = kb.intents.find { it.id == id } ?: return
        val r = SoudanEngine.respondToIntent(id) ?: return
        applyResponse(intent.chip, r)
    }

    fun followupTap(id: String) {
        val f = kb.commonFollowups.find { it.id == id } ?: return
        val r = SoudanEngine.respondToFollowup(id, lastIntentId, shownVideoIds) ?: return
        applyResponse(f.chip, r)
    }

    // index.html:1844 planChipTap相当。即開始はせず確認の吹き出しを積む。
    fun planChipTap(id: String) {
        val intent = kb.intents.find { it.id == id } ?: return
        val replacing = plan != null && plan?.intentId != id
        messages = messages + SdBubble.User("📅 この悩みを2週間プランにする") +
            SdBubble.PlanConfirm(intentId = id, label = intent.chip, replacing = replacing)
    }

    // index.html:1857 planStart相当。
    fun planStart(id: String) {
        val intent = kb.intents.find { it.id == id } ?: return
        val vids = intent.videos.map { it.v }.distinct()
        if (vids.isEmpty()) return
        val today = jp.ogatore.kyouno.record.RecordLogic.todayStr(java.time.Instant.now())
        val newPlan = SdPlanData(intentId = id, label = intent.chip, videos = vids, start = today, days = 14)
        store.set("plan", newPlan)
        plan = newPlan
        messages = messages + SdBubble.Bot("よし、きょうから14日間いっしょにやろう！ホームの「きょうの1本」が${intent.chip}用になったよ😊")
    }

    fun planDecline() {
        messages = messages + SdBubble.Bot("OK！1本ずつでも十分えらいよ😊 プランにしたくなったら、いつでもここから組めるからね")
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("オガトレ相談室", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Text("✕", modifier = Modifier.clickable { onClose() }.padding(8.dp).testTag("soudanCloseBtn"))
        }
        Text(
            "※目安をつかむ相談室です 強い痛み・しびれがあるときは医療機関へ",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(vertical = 4.dp),
        )

        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).testTag("sdLog")) {
            Text(
                "肩こりや腰痛など、気になることを教えてね。下のチップから選んでもいいよ😊",
                modifier = Modifier.padding(vertical = 4.dp),
            )
            for (m in messages) {
                when (m) {
                    is SdBubble.User -> Text(
                        m.text,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).testTag("sdUserBubble"),
                        textAlign = TextAlign.End,
                    )
                    is SdBubble.Bot -> {
                        val bg = if (m.red) Color(0xFFF6D6D6) else Color(0xFFF3F1EC)
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                .background(bg, RoundedCornerShape(12.dp)).padding(10.dp)
                                .testTag(if (m.red) "sdBotBubbleRed" else "sdBotBubble"),
                        ) {
                            if (m.text.isNotEmpty()) Text(m.text)
                            if (m.videoId != null) {
                                Button(
                                    onClick = { openUrl("https://www.youtube.com/watch?v=${m.videoId}") },
                                    modifier = Modifier.padding(top = 4.dp).testTag("sdVideoBtn_${m.videoId}"),
                                ) { Text("▶ 動画を見る") }
                            }
                        }
                    }
                    is SdBubble.PlanConfirm -> Column(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            .background(Color(0xFFF3F1EC), RoundedCornerShape(12.dp)).padding(10.dp),
                    ) {
                        Text(
                            if (m.replacing) "いまのプランと入れ替える？きょうの1本が、あなたの${m.label}プランになるよ"
                            else "きょうの1本が、あなたの${m.label}プランになるよ！2週間いっしょにやってみる？",
                        )
                        Row(Modifier.padding(top = 6.dp)) {
                            Button(
                                onClick = { planStart(m.intentId) },
                                modifier = Modifier.testTag("planStartBtn"),
                            ) { Text(if (m.replacing) "入れ替えてはじめる！" else "はじめる！") }
                            Spacer(Modifier.padding(horizontal = 4.dp))
                            Button(
                                onClick = { planDecline() },
                                modifier = Modifier.testTag("planDeclineBtn"),
                            ) { Text("まずは1本だけ") }
                        }
                    }
                }
            }
        }

        // ---- チップ列(index.html:3139 sdRenderChips相当) ----
        when (val mode = chipsMode) {
            is SdChipsMode.None -> {} // crisis直後: チップ・カテゴリタブなし(index.html:3143-3145)
            is SdChipsMode.Intents -> {
                LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("sdCatRow")) {
                    items(SD_CHIP_CATS) { cat ->
                        Button(
                            onClick = { chipsMode = SdChipsMode.Intents(cat.key) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (cat.key == mode.activeCat) Color(0xFF6B4EA6) else Color(0xFFE8E3F5),
                                contentColor = if (cat.key == mode.activeCat) Color.White else Color.Black,
                            ),
                            modifier = Modifier.padding(end = 4.dp).testTag("sdCat_${cat.key}"),
                        ) { Text(cat.label) }
                    }
                }
                val activeCat = SD_CHIP_CATS.find { it.key == mode.activeCat } ?: SD_CHIP_CATS[0]
                val ids = sdCatIntentIds(activeCat).toSet()
                LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("sdChips")) {
                    items(kb.intents.filter { ids.contains(it.id) }) { intent ->
                        Button(onClick = { chipTap(intent.id) }, modifier = Modifier.padding(end = 4.dp).testTag("sdChip_${intent.id}")) {
                            Text(intent.chip)
                        }
                    }
                }
            }
            is SdChipsMode.Followups -> {
                val intent = kb.intents.find { it.id == mode.intentId }
                // index.html:1828 planInjectChip相当: 動画2本以上・除外intentでない・実行中プランと同一でないときだけ出す
                val showPlanChip = intent != null && intent.videos.size >= 2 &&
                    intent.id != PLAN_EXCLUDE_INTENT && plan?.intentId != intent.id
                LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("sdChips")) {
                    if (showPlanChip && intent != null) {
                        item {
                            Button(onClick = { planChipTap(intent.id) }, modifier = Modifier.padding(end = 4.dp).testTag("sdPlanChip")) {
                                Text("📅 この悩みを2週間プランにする")
                            }
                        }
                    }
                    items(intent?.followups.orEmpty()) { fid ->
                        val label = kb.commonFollowups.find { it.id == fid }?.chip
                            ?: kb.intents.find { it.id == fid }?.chip
                        if (label != null) {
                            Button(
                                onClick = {
                                    if (kb.commonFollowups.any { it.id == fid }) followupTap(fid) else chipTap(fid)
                                },
                                modifier = Modifier.padding(end = 4.dp).testTag("sdFollowup_$fid"),
                            ) { Text(label) }
                        }
                    }
                    mode.nextBestId?.let { nbId ->
                        val nb = kb.intents.find { it.id == nbId }
                        if (nb != null) {
                            item {
                                Button(onClick = { chipTap(nb.id) }, modifier = Modifier.padding(end = 4.dp).testTag("sdNextBestChip")) {
                                    Text("${nb.chip}の話も")
                                }
                            }
                        }
                    }
                    item {
                        Button(onClick = { chipsMode = SdChipsMode.Intents("body") }, modifier = Modifier.padding(end = 4.dp).testTag("sdBackToIntentsBtn")) {
                            Text("べつの悩みをそうだん")
                        }
                    }
                }
            }
            is SdChipsMode.Nearmiss -> {
                LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("sdChips")) {
                    items(mode.ids) { id ->
                        val intent = kb.intents.find { it.id == id }
                        if (intent != null) {
                            Button(onClick = { chipTap(id) }, modifier = Modifier.padding(end = 4.dp).testTag("sdNearmiss_$id")) {
                                Text(intent.chip)
                            }
                        }
                    }
                    item {
                        Button(onClick = { chipsMode = SdChipsMode.Intents("body") }, modifier = Modifier.padding(end = 4.dp).testTag("sdBackToIntentsBtn2")) {
                            Text("べつの悩みをそうだん")
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f).testTag("sdInput"),
                placeholder = { Text("気になることを入力") },
            )
            Button(onClick = { sendText() }, modifier = Modifier.padding(start = 4.dp).testTag("sdSendBtn")) { Text("送信") }
        }
    }
}

// index.html:1781 renderPlanCard相当の簡略版(進捗バー・完走時の卒業表示・解除ボタン)。
// 紙吹雪演出(launchConfetti)・章システムとの連携(mode_manual)等の見た目演出は移植対象外(Step6の
// 検収基準に含まれないため。安全性に無関係)。
//
// ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md):
// index.html:667-676 #planCard(.bar進捗バー・「やめる」は下線付きテキストリンクでボタンではない)の
// 1:1移植。KyonoCard化(ホーム画面スクショで唯一浮いて見えていた箇所)。
@Composable
fun PlanProgressCard(store: RecordStore, plan: SdPlanData, onCleared: () -> Unit) {
    val colors = LocalKyonoColors.current
    val today = jp.ogatore.kyouno.record.RecordLogic.todayStr(java.time.Instant.now())
    val dayNum = (jp.ogatore.kyouno.record.RecordLogic.daysBetween(plan.start, today) + 1).coerceAtLeast(1)
    val finished = dayNum > plan.days
    if (finished) {
        // index.html:1798 planFinished時のstore.set("plan",null)相当。合成中の副作用を避けるため
        // LaunchedEffectで1度だけ実行する。
        androidx.compose.runtime.LaunchedEffect(plan) {
            store.set("plan", null as SdPlanData?)
            onCleared()
        }
    }
    KyonoCard(Modifier.testTag("planCard")) {
        if (finished) {
            Text("🎉 ${plan.label}プラン完走！すごい！", color = colors.ink, fontWeight = FontWeight.Black, modifier = Modifier.testTag("planDoneText"))
            Text("${plan.days}日間続けたの、ほんとにえらい👏", color = colors.sub)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "📅 ${plan.label}プラン $dayNum/${plan.days}日", color = colors.ink, fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f).testTag("planTitle"),
                )
                Text(
                    "やめる", color = colors.sub, fontWeight = FontWeight.Black, fontSize = 13.sp,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                    modifier = Modifier
                        .clickable { store.set("plan", null as SdPlanData?); onCleared() }
                        .testTag("planQuitBtn"),
                )
            }
            // index.html:414-415 .bar/.bar>div(teal系グラデーションの進捗バー)の1:1移植。
            val progress = (dayNum.toFloat() / plan.days.toFloat()).coerceIn(0f, 1f)
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(14.dp)
                    .background(colors.line, RoundedCornerShape(99.dp)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(colors.teal, RoundedCornerShape(99.dp)),
                )
            }
        }
    }
}
