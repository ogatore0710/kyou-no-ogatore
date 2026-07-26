package jp.ogatore.kyouno

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
fun SoudanSheet(store: RecordStore, openUrl: (String) -> Unit, onClose: () -> Unit, presetIntentId: String? = null) {
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

    // ホーム構造修正タスク(TASK-C2-2026-07-26-home-structure-fix.md): index.html:3409
    // soudanCardChips「タップでそのまま聞けるよ」チップ→openSoudan(intentId)相当。ホームの
    // オガトレ相談室カードのおすすめチップから開いたときだけ、開いた瞬間にそのintentへ自動応答する。
    LaunchedEffect(Unit) {
        presetIntentId?.let { chipTap(it) }
    }

    // ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
    // Phase 3: index.html:459-489 .sd-sheet/.sd-head/.sd-b/.chip/.catbtnの1:1移植。見た目の変更のみで、
    // 上の判定・状態管理ロジック(applyResponse/chipTap/sendText等)には一切手を入れていない。
    val themeSetting = store.get("theme", "auto")
    KyonoTheme(themeSetting) {
        val colors = LocalKyonoColors.current
        Column(Modifier.fillMaxSize().background(colors.bg)) {
            // index.html:461-465 .sd-head(ヘッダー・円形×クローズボタン)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().background(colors.card).padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                KyonoSectionHeader(KyonoIcon.SoudanBubble, "オガトレ相談室", fill = colors.tealSoft, accent = colors.teal, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(colors.line, androidx.compose.foundation.shape.CircleShape)
                        .clickable { onClose() }
                        .testTag("soudanCloseBtn"),
                    contentAlignment = Alignment.Center,
                ) { Text("✕", color = colors.ink, fontWeight = FontWeight.Black) }
            }
            // index.html:466-467 .sd-disc
            Text(
                "※目安をつかむ相談室です 強い痛み・しびれがあるときは医療機関へ",
                color = colors.sub,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().background(colors.card).padding(horizontal = 16.dp, vertical = 6.dp),
            )
            androidx.compose.material3.HorizontalDivider(color = colors.line)

            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(16.dp).testTag("sdLog"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("肩こりや腰痛など、気になることを教えてね。下のチップから選んでもいいよ😊", color = colors.sub)
                for (m in messages) {
                    when (m) {
                        // index.html:482-483 .sd-row.user .sd-b(黄色系吹き出し・右寄せ)
                        is SdBubble.User -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            Text(
                                m.text, color = colors.ink,
                                modifier = Modifier
                                    .background(colors.yellowSoft, RoundedCornerShape(16.dp, 16.dp, 6.dp, 16.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("sdUserBubble"),
                            )
                        }
                        // index.html:481,488,3080 .sd-b/.sd-row.sd-red .sd-b(通常=card+line枠・赤旗=coral-soft+coral枠)/
                        // .sd-ava(chara-hitokoto.pngアバター・botメッセージのみ)の1:1移植。
                        is SdBubble.Bot -> Row(verticalAlignment = Alignment.Bottom) {
                            KyonoCharaImage("chara_hitokoto", Modifier.size(38.dp))
                            Spacer(Modifier.width(8.dp))
                            val bg = if (m.red) colors.coralSoft else colors.card
                            val border = if (m.red) colors.coral else colors.line
                            Column(
                                Modifier.fillMaxWidth(0.86f)
                                    .background(bg, RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp))
                                    .border(1.5.dp, border, RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag(if (m.red) "sdBotBubbleRed" else "sdBotBubble"),
                            ) {
                                if (m.text.isNotEmpty()) Text(m.text, color = colors.ink)
                                if (m.videoId != null) {
                                    Spacer(Modifier.height(6.dp))
                                    KyonoGhostButton(
                                        "▶ 動画を見る",
                                        { openUrl("https://www.youtube.com/watch?v=${m.videoId}") },
                                        Modifier.testTag("sdVideoBtn_${m.videoId}"),
                                    )
                                }
                            }
                        }
                        is SdBubble.PlanConfirm -> Row(verticalAlignment = Alignment.Bottom) {
                            KyonoCharaImage("chara_hitokoto", Modifier.size(38.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(
                                Modifier.fillMaxWidth(0.86f)
                                    .background(colors.card, RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp))
                                    .border(1.5.dp, colors.line, RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                            ) {
                                Text(
                                    if (m.replacing) "いまのプランと入れ替える？きょうの1本が、あなたの${m.label}プランになるよ"
                                    else "きょうの1本が、あなたの${m.label}プランになるよ！2週間いっしょにやってみる？",
                                    color = colors.ink,
                                )
                                Spacer(Modifier.height(8.dp))
                                KyonoPrimaryButton(
                                    if (m.replacing) "入れ替えてはじめる！" else "はじめる！",
                                    { planStart(m.intentId) },
                                    Modifier.testTag("planStartBtn"),
                                )
                                Spacer(Modifier.height(6.dp))
                                KyonoGhostButton("まずは1本だけ", { planDecline() }, Modifier.testTag("planDeclineBtn"))
                            }
                        }
                    }
                }
            }

            // ---- チップ列(index.html:3139 sdRenderChips相当・.chip/.catbtnの1:1移植) ----
            Column(Modifier.background(colors.card).padding(horizontal = 14.dp, vertical = 8.dp)) {
                when (val mode = chipsMode) {
                    is SdChipsMode.None -> {} // crisis直後: チップ・カテゴリタブなし(index.html:3143-3145)
                    is SdChipsMode.Intents -> {
                        LazyRow(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).testTag("sdCatRow")) {
                            items(SD_CHIP_CATS) { cat ->
                                KyonoCatButton(cat.label, cat.key == mode.activeCat, { chipsMode = SdChipsMode.Intents(cat.key) }, Modifier.padding(end = 8.dp).testTag("sdCat_${cat.key}"))
                            }
                        }
                        val activeCat = SD_CHIP_CATS.find { it.key == mode.activeCat } ?: SD_CHIP_CATS[0]
                        val ids = sdCatIntentIds(activeCat).toSet()
                        LazyRow(modifier = Modifier.fillMaxWidth().testTag("sdChips")) {
                            items(kb.intents.filter { ids.contains(it.id) }) { intent ->
                                KyonoChip(intent.chip, { chipTap(intent.id) }, Modifier.padding(end = 8.dp).testTag("sdChip_${intent.id}"))
                            }
                        }
                    }
                    is SdChipsMode.Followups -> {
                        val intent = kb.intents.find { it.id == mode.intentId }
                        // index.html:1828 planInjectChip相当: 動画2本以上・除外intentでない・実行中プランと同一でないときだけ出す
                        val showPlanChip = intent != null && intent.videos.size >= 2 &&
                            intent.id != PLAN_EXCLUDE_INTENT && plan?.intentId != intent.id
                        LazyRow(modifier = Modifier.fillMaxWidth().testTag("sdChips")) {
                            if (showPlanChip && intent != null) {
                                item {
                                    KyonoChip("📅 この悩みを2週間プランにする", { planChipTap(intent.id) }, Modifier.padding(end = 8.dp).testTag("sdPlanChip"))
                                }
                            }
                            items(intent?.followups.orEmpty()) { fid ->
                                val label = kb.commonFollowups.find { it.id == fid }?.chip
                                    ?: kb.intents.find { it.id == fid }?.chip
                                if (label != null) {
                                    KyonoChip(
                                        label,
                                        { if (kb.commonFollowups.any { it.id == fid }) followupTap(fid) else chipTap(fid) },
                                        Modifier.padding(end = 8.dp).testTag("sdFollowup_$fid"),
                                    )
                                }
                            }
                            mode.nextBestId?.let { nbId ->
                                val nb = kb.intents.find { it.id == nbId }
                                if (nb != null) {
                                    item {
                                        KyonoChip("${nb.chip}の話も", { chipTap(nb.id) }, Modifier.padding(end = 8.dp).testTag("sdNextBestChip"))
                                    }
                                }
                            }
                            item {
                                KyonoChip("べつの悩みをそうだん", { chipsMode = SdChipsMode.Intents("body") }, Modifier.padding(end = 8.dp).testTag("sdBackToIntentsBtn"))
                            }
                        }
                    }
                    is SdChipsMode.Nearmiss -> {
                        LazyRow(modifier = Modifier.fillMaxWidth().testTag("sdChips")) {
                            items(mode.ids) { id ->
                                val intent = kb.intents.find { it.id == id }
                                if (intent != null) {
                                    KyonoChip(intent.chip, { chipTap(id) }, Modifier.padding(end = 8.dp).testTag("sdNearmiss_$id"))
                                }
                            }
                            item {
                                KyonoChip("べつの悩みをそうだん", { chipsMode = SdChipsMode.Intents("body") }, Modifier.padding(end = 8.dp).testTag("sdBackToIntentsBtn2"))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f).testTag("sdInput"),
                        placeholder = { Text("気になることを入力") },
                    )
                    Spacer(Modifier.width(8.dp))
                    KyonoPrimaryButton("送信", { sendText() }, Modifier.weight(0.4f).testTag("sdSendBtn"))
                }
            }
        }
    }
}

// index.html:440 .chip(丸ピル・line枠・card背景)の1:1移植。
@Composable
fun KyonoChip(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalKyonoColors.current
    Box(
        modifier = modifier
            .background(colors.card, RoundedCornerShape(99.dp))
            .border(2.dp, colors.line, RoundedCornerShape(99.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) { Text(label, color = colors.sub, fontSize = 14.sp, fontWeight = FontWeight.Black) }
}

// index.html:436-437 .catbtn/.catbtn.on(カテゴリタブ・選択時=yellow背景)の1:1移植。
@Composable
private fun KyonoCatButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalKyonoColors.current
    Box(
        modifier = modifier
            .background(if (selected) colors.yellow else colors.line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 10.dp),
    ) { Text(label, color = if (selected) Color(0xFF3A3A35) else colors.sub, fontSize = 14.sp, fontWeight = FontWeight.Black) }
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
            // フォント適用漏れ・キャラ/タイプ画像の欠落修正タスク(TASK-C2-2026-07-26-visual-parity-fonts-characters.md)
            // §2 キャラクター画像: index.html:679 #planDoneCard(chara-congrats.png 84x84・中央寄せ)の1:1移植。
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                KyonoCharaImage("chara_congrats", Modifier.size(84.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "🎉 ${plan.label}プラン完走！すごい！", color = colors.ink, fontWeight = FontWeight.Black,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth().testTag("planDoneText"),
            )
            Text("${plan.days}日間続けたの、ほんとにえらい👏", color = colors.sub, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
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
