@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package jp.ogatore.kyouno

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.ogatore.kyouno.record.RecordStore
import jp.ogatore.kyouno.safety.SafetyKBLoader
import jp.ogatore.kyouno.safety.SoudanEngine
import jp.ogatore.kyouno.safety.SoudanResponse
import jp.ogatore.kyouno.safety.SoudanVerdict
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

// index.html:2979 SOUDAN_BODY_INTENTS の1:1移植(体の悩み系intent=かたさチェック誘導チップの対象)。
private val SOUDAN_BODY_INTENTS = setOf("katakori", "youtsuu", "zenkutsu", "kokansetsu", "ashikubi", "kaikyaku", "nekoze", "nemuri", "zenshin")

// index.html:2946 SD_MAIL の1:1移植(検索タブのリクエスト導線と同じ宛先)。
private const val SD_MAIL = "kyou-no@ogatore.jp"

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
    data class Bot(val text: String, val red: Boolean = false, val videoId: String? = null, val fallbackCaution: Boolean = false) : SdBubble()
    data class User(val text: String) : SdBubble()
    data class PlanConfirm(val intentId: String, val label: String, val replacing: Boolean, var answered: Boolean = false) : SdBubble()
    // index.html:3323-3330 sdAnswerFallback内の2通目(逃げ道リンク3つ)の1:1移植。rawUserTextはmailto本文用。
    data class FallbackLinks(val rawUserText: String) : SdBubble()
    // TASK-C2-2026-07-27-soudan-staged-reveal.md: index.html:3084 sdTypingNode()の1:1移植。
    object Typing : SdBubble()
}

// index.html:3053 sdMsgLen()の1:1移植(タイピング待ち時間の計算専用。空白を除いた文字数)。
private fun sdMsgLen(text: String) = text.replace(Regex("\\s+"), "").length

// タイピング待ち時間計算のために各吹き出しから代表テキストを取り出す。Web版はHTML文字列全体から
// タグを除いた文字数を使うため、動画注記込みの吹き出しもその見出しテキストで近似する。
private fun sdBubbleLen(b: SdBubble): Int = when (b) {
    is SdBubble.Bot -> sdMsgLen(b.text)
    is SdBubble.FallbackLinks -> sdMsgLen("この悩み、オガトレに届けるメールがひらかない方はアドレスをコピー動画を探すタブでさがしてみる")
    else -> 0
}

sealed class SdChipsMode {
    object None : SdChipsMode()
    data class Intents(val activeCat: String) : SdChipsMode()
    data class Followups(val intentId: String, val nextBestId: String?) : SdChipsMode()
    data class Nearmiss(val ids: List<String>) : SdChipsMode()
}

// Fable監査D5-1/D5-2(alan5差し戻し2026-07-28): 回転でActivity再生成されると、相談室の会話
// (messages/chipsMode/lastIntentId/input)が素の`remember`のまま消え、シートは開き直っても
// 中身が空になる欠陥があった。「画面だけ戻って中身が空」は今より悪い(D5-2)ため、
// MainActivity.ktのScreenSaverと同じ「Bundle互換の入れ子ArrayListへ手で平坦化する」方式で
// 会話そのものをrememberSaveableに載せる。
internal fun encodeSdBubble(b: SdBubble): ArrayList<Any?> = when (b) {
    is SdBubble.Bot -> arrayListOf("Bot", b.text, b.red, b.videoId, b.fallbackCaution)
    is SdBubble.User -> arrayListOf("User", b.text)
    is SdBubble.PlanConfirm -> arrayListOf("PlanConfirm", b.intentId, b.label, b.replacing, b.answered)
    is SdBubble.FallbackLinks -> arrayListOf("FallbackLinks", b.rawUserText)
    is SdBubble.Typing -> arrayListOf("Typing")
}

@Suppress("UNCHECKED_CAST")
internal fun decodeSdBubble(saved: Any?): SdBubble {
    val list = saved as? List<Any?> ?: return SdBubble.Typing
    return when (list.getOrNull(0) as? String) {
        "Bot" -> SdBubble.Bot(
            text = list.getOrNull(1) as? String ?: "",
            red = list.getOrNull(2) as? Boolean ?: false,
            videoId = list.getOrNull(3) as? String,
            fallbackCaution = list.getOrNull(4) as? Boolean ?: false,
        )
        "User" -> SdBubble.User(list.getOrNull(1) as? String ?: "")
        "PlanConfirm" -> SdBubble.PlanConfirm(
            intentId = list.getOrNull(1) as? String ?: "",
            label = list.getOrNull(2) as? String ?: "",
            replacing = list.getOrNull(3) as? Boolean ?: false,
            answered = list.getOrNull(4) as? Boolean ?: false,
        )
        "FallbackLinks" -> SdBubble.FallbackLinks(list.getOrNull(1) as? String ?: "")
        else -> SdBubble.Typing
    }
}

internal fun encodeSdChipsMode(m: SdChipsMode): ArrayList<Any?> = when (m) {
    is SdChipsMode.None -> arrayListOf("None")
    is SdChipsMode.Intents -> arrayListOf("Intents", m.activeCat)
    is SdChipsMode.Followups -> arrayListOf("Followups", m.intentId, m.nextBestId)
    is SdChipsMode.Nearmiss -> arrayListOf("Nearmiss", ArrayList(m.ids))
}

@Suppress("UNCHECKED_CAST")
internal fun decodeSdChipsMode(saved: Any?): SdChipsMode {
    val list = saved as? List<Any?> ?: return SdChipsMode.None
    return when (list.getOrNull(0) as? String) {
        "Intents" -> SdChipsMode.Intents(list.getOrNull(1) as? String ?: "body")
        "Followups" -> SdChipsMode.Followups(list.getOrNull(1) as? String ?: "", list.getOrNull(2) as? String)
        "Nearmiss" -> SdChipsMode.Nearmiss((list.getOrNull(1) as? List<String>)?.toList() ?: emptyList())
        else -> SdChipsMode.None
    }
}

val SdMessagesSaver: Saver<List<SdBubble>, Any> = Saver(
    save = { list -> ArrayList(list.map { encodeSdBubble(it) }) },
    restore = { saved -> (saved as? List<Any?>)?.map { decodeSdBubble(it) } ?: emptyList() },
)

val SdChipsModeSaver: Saver<SdChipsMode, Any> = Saver(
    save = { mode -> encodeSdChipsMode(mode) },
    restore = { saved -> decodeSdChipsMode(saved) },
)

@Composable
fun SoudanSheet(
    store: RecordStore,
    openUrl: (String) -> Unit,
    onClose: () -> Unit,
    presetIntentId: String? = null,
    greeted: Boolean = false,
    onGreeted: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenQuiz: () -> Unit = {},
) {
    val kb = remember { SafetyKBLoader.shared }
    // Fable監査D5-1/D5-2(alan5差し戻し2026-07-28): 会話・チップの状態・直近の話題・入力途中の
    // 文章は回転をまたいで保持する(rememberSaveable)。shownVideoIds(表示済み動画の重複防止)は
    // 今回のD5対象外(alan5指示・「持っていない券を使ったように見える」ほどの実害ではない)。
    var messages by rememberSaveable(stateSaver = SdMessagesSaver) { mutableStateOf(listOf<SdBubble>()) }
    var chipsMode by rememberSaveable(stateSaver = SdChipsModeSaver) { mutableStateOf<SdChipsMode>(SdChipsMode.Intents("body")) }
    var lastIntentId by rememberSaveable { mutableStateOf<String?>(null) }
    val shownVideoIds = remember { mutableStateListOf<String>() } // index.html:2999 sdCtx.shownVideoIds相当(セッション内のみ)
    var input by rememberSaveable { mutableStateOf("") }
    var plan by remember { mutableStateOf(store.get("plan", null as SdPlanData?)) }
    val hasType = store.get<QuizTypeResult?>("type", null) != null // index.html:3024 sdHasType()の1:1移植
    val scope = rememberCoroutineScope()
    // TASK-C2-2026-07-27-soudan-staged-reveal.md: index.html:3096 sdPending(応答演出中は次の
    // チップタップ/送信を受け付けない)の1:1移植。演出中の吹き出し順序が入り乱れるのを防ぐ。
    var sdPending by remember { mutableStateOf(false) }
    val sdReducedMotion = rememberReducedMotion()

    // index.html:3090-3134 sdPush()の1:1移植。bot発言を1つずつ、タイピングドットを挟みながら
    // 段階的に表示する。チップ列(chipsMode)の更新はWeb版のsdRenderChips()と同じく、
    // 全吹き出しの表示が終わったタイミングでまとめて行う(表示中は直前のチップがそのまま残る)。
    // カテゴリタブの早期畳み(index.html:3097-3100)はComposeのレイアウトでは該当する見切れ問題が
    // 起きないため見送り(表示タイミングの本質=段階表示自体は1:1)。
    suspend fun revealBotMessages(botMsgs: List<SdBubble>, newChipsMode: SdChipsMode) {
        // §D: index.html:3051 sdReduced()がtrueの場合、queueNext()はタイピングドット+待機を
        // 挟まず即座に全件を表示する(index.html:3119-3131 if(reduced){showNext();return;})の1:1移植。
        if (sdReducedMotion) {
            messages = messages + botMsgs
            chipsMode = newChipsMode
            return
        }
        for ((i, msg) in botMsgs.withIndex()) {
            messages = messages + SdBubble.Typing
            val base = minOf(1600, 500 + sdBubbleLen(msg) * 22)
            val wait = if (i == 0) 400L else if (i == 1) base.toLong() else (base * 2).toLong()
            delay(wait)
            messages = messages.dropLast(1) + msg
        }
        chipsMode = newChipsMode
    }

    // index.html:3090 sdPush相当。応答1件をbot/userの吹き出し列へ展開しchipsModeを更新する。
    suspend fun applyResponse(userText: String?, r: SoudanResponse) {
        if (userText != null) messages = messages + SdBubble.User(userText)
        val red = r.verdict is SoudanVerdict.RedFlag || r.verdict is SoudanVerdict.Crisis
        val botMsgs = mutableListOf<SdBubble>()
        if (r.empathy.isNotEmpty()) botMsgs.add(SdBubble.Bot(r.empathy, red))
        if (r.message.isNotEmpty()) botMsgs.add(SdBubble.Bot(r.message, red, fallbackCaution = r.isFallback))
        r.video?.let { v ->
            botMsgs.add(SdBubble.Bot(v.note.ifEmpty { "おすすめの1本" }, videoId = v.videoId))
            if (shownVideoIds.none { it == v.videoId }) shownVideoIds.add(v.videoId)
        }
        if (r.keizoku.isNotEmpty()) botMsgs.add(SdBubble.Bot(r.keizoku))
        // index.html:3328-3330 sdAnswerFallback2通目(逃げ道リンク3つ)の1:1移植。
        if (r.isFallback) botMsgs.add(SdBubble.FallbackLinks(userText.orEmpty()))
        if (r.intentId != null) lastIntentId = r.intentId
        val newChipsMode = when {
            r.verdict is SoudanVerdict.Crisis -> SdChipsMode.None // index.html:3310 チップ・カテゴリタブなし
            r.verdict is SoudanVerdict.RedFlag -> SdChipsMode.Intents("body") // index.html:3304
            r.hasFollowup && r.intentId != null -> SdChipsMode.Followups(r.intentId, r.nextBestChip?.id)
            r.nearmissChips.isNotEmpty() -> SdChipsMode.Nearmiss(r.nearmissChips.map { c -> c.id })
            else -> SdChipsMode.Intents("body")
        }
        revealBotMessages(botMsgs, newChipsMode)
    }

    // index.html:3361-3384 sdChipTap/sdFollowupTap/sdSendの「if(sdPending) return」の1:1移植。
    fun runResponse(userText: String?, r: SoudanResponse) {
        if (sdPending) return
        sdPending = true
        scope.launch {
            applyResponse(userText, r)
            sdPending = false
        }
    }

    fun sendText() {
        if (sdPending) return
        val raw = input.trim()
        if (raw.isEmpty()) return
        input = ""
        runResponse(raw, SoudanEngine.respond(raw))
    }

    fun chipTap(id: String) {
        if (sdPending) return
        val intent = kb.intents.find { it.id == id } ?: return
        val r = SoudanEngine.respondToIntent(id) ?: return
        runResponse(intent.chip, r)
    }

    fun followupTap(id: String) {
        if (sdPending) return
        val f = kb.commonFollowups.find { it.id == id } ?: return
        val r = SoudanEngine.respondToFollowup(id, lastIntentId, shownVideoIds) ?: return
        runResponse(f.chip, r)
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

    // TASK-C2-2026-07-27-soudan-safety-copy-and-links: index.html:3459 sdEnsureGreeting()の1:1移植。
    // 相談室をアプリのこのセッションで初めて開いたときだけ、吹き出し形式であいさつを1通出す
    // (sdGreetedはWeb版と同じくセッション内のみ・store非永続。呼び出し元がルート階層で保持する)。
    // ホーム構造修正タスクのpresetIntentId自動応答(index.html:3464-3467)より先に出す(Web版と同順)。
    LaunchedEffect(Unit) {
        if (!greeted) {
            messages = messages + SdBubble.Bot("こんにちは、オガトレです！からだの悩み、なんでも聞かせて😊\n下のチップか、ことばで入力してね")
            onGreeted()
        }
        presetIntentId?.let { chipTap(it) }
    }

    // ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
    // Phase 3: index.html:459-489 .sd-sheet/.sd-head/.sd-b/.chip/.catbtnの1:1移植。見た目の変更のみで、
    // 上の判定・状態管理ロジック(applyResponse/chipTap/sendText等)には一切手を入れていない。
    val themeSetting = store.get("theme", "auto")
    KyonoTheme(themeSetting, bigText = store.get("bigtext", true)) {
        val colors = LocalKyonoColors.current
        Column(Modifier.fillMaxSize().background(colors.bg)) {
            // index.html:461-465 .sd-head(ヘッダー・円形×クローズボタン)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().background(colors.card).padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                KyonoSectionHeader(KyonoIcon.SoudanBubble, "オガトレ相談室", fill = colors.tealSoft, accent = colors.teal, modifier = Modifier.weight(1f))
                // GO-G3(5視点ワンループ): index.html:464 .sd-closeの視覚サイズ(40px)は変えず、
                // タップ領域だけ44dp相当に広げる(外側Boxがヒットエリア・内側Boxが従来どおりの見た目)。
                Box(
                    modifier = Modifier.size(44.dp).clickable { onClose() }.testTag("soudanCloseBtn")
                        .semantics { contentDescription = "とじる" },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).background(colors.line, androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text("✕", color = colors.ink, fontWeight = FontWeight.Black) }
                }
            }
            // index.html:1223 .sd-disc(2行・2行目後半は太字強調)
            Text(
                buildAnnotatedString {
                    append("※回答はオガトレ監修のパターン集から選んでいます\n※目安をつかむ相談室です ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Black)) { append("強い痛み・しびれがあるときは医療機関へ") }
                },
                color = colors.sub,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().background(colors.card).padding(horizontal = 16.dp, vertical = 6.dp).testTag("sdDisc"),
            )
            androidx.compose.material3.HorizontalDivider(color = colors.line)

            // TestFlight実機フィードバックB5(2026-07-29): 新しい発言が増えても自動で下へ
            // スクロールしなかった(index.html:3056 sdScrollEnd()/3061 sdScrollToTop()相当の
            // 実装が丸ごと欠落)。チップを押しても画面が動かず「押せたか分からない」体験に
            // なっていた。
            //
            // B5差し戻し(2026-07-29): 最初の実装は常に最下部へ送っていたが、それだと長いbot
            // 返信の1行目が画面外に押し出されて読み始められないことがある(相談室はチップ行+
            // 入力欄で下がふさがっており見える高さが狭いため)。index.html:3057-3060の実測
            // コメントのとおり、Web版は「下端合わせ一辺倒」を2026-07-15に捨てている:
            // 新規bot発言はその行の「頭」へ、ユーザー自身の発言(index.html:3136
            // sdUserBubble→sdScrollEnd)だけ最下部へ、と使い分ける。行の下に読む余地が無い
            // (最後の短い行)ときはscrollTo/animateScrollToが自動でmaxValueにクランプする
            // ため、index.html:3069のMath.min(...)クランプと同じ効果になる(特別扱い不要)。
            val sdScrollState = rememberScrollState()
            // B5バグ修正(2026-07-29・実機ログで発見): 当初はonGloballyPositioned内で
            // 「自分がmessages.lastIndexと一致するか」「前回スクロール済みインスタンスと
            // 参照が違うか」を判定していたが2つの理由で機能していなかった。
            // (1) SdBubble.Typingはobject(シングルトン)のため、一度スクロール済みと
            //     記録すると以降すべてのTyping出現が「同じ参照」判定され二度と発火しない。
            // (2) 実メッセージが積まれた直後に次のTypingが即追加されるため、そのメッセージの
            //     onGloballyPositioned発火時には既にmessages.lastIndexが1つ先へ進んでおり
            //     「自分が最後」判定に失敗する(競合)。
            // 結果、最初のTyping出現時の1回しかスクロールが発火せず、それ以降(mitate本文・
            // video note・keizoku)は一切スクロールしていなかった。
            // 修正: 各行の位置はindexキーで無条件に記録し続け、「どこへスクロールするか」は
            // messages.sizeの変化を捉えるLaunchedEffectで都度「そのときのmessages.lastIndex」を
            // 再評価する。sizeが連続で変わればLaunchedEffectの自動キャンセルにより古い狙いは
            // 破棄され、常に最新の「今の最後の行」だけへ届く。
            val sdRowPositions = remember { mutableStateMapOf<Int, Float>() }
            LaunchedEffect(messages.size) {
                val idx = messages.lastIndex
                if (idx < 0) return@LaunchedEffect
                val isUser = messages[idx] is SdBubble.User
                var y = sdRowPositions[idx]
                var tries = 0
                while (y == null && tries < 10) {
                    withFrameNanos {}
                    y = sdRowPositions[idx]
                    tries++
                }
                val target = if (isUser) sdScrollState.maxValue else (y?.roundToInt() ?: sdScrollState.maxValue)
                if (sdReducedMotion) sdScrollState.scrollTo(target) else sdScrollState.animateScrollTo(target)
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(sdScrollState)
                    .padding(16.dp).testTag("sdLog"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // TASK-C2-2026-07-27-chips-overflow-and-bubble-pop.md §3: index.html:3079,3085
                // .sd-pop(opacity0→1・translateY(4px)→0・.18s ease-out)の1:1移植。§Dの対象表に
                // 洩れていた分(index.html:497は.sd-sheetと.sd-popの2つを指しており、後者は今回まで
                // 未実装だった)。reduced-motion時は無演出即表示にする。
                val bubblePopDensity = LocalDensity.current
                messages.forEachIndexed { index, m ->
                    AnimatedVisibility(
                        visible = true,
                        // UI/UXパリティ監査GO-13(2026-07-28・SUSPECTED): index.html:489 .sd-pop
                        // {animation:sdpop .18s ease-out}の1:1移植。easing省略でCompose既定の
                        // FastOutSlowInEasingにフォールバックしていた。
                        enter = if (sdReducedMotion) {
                            fadeIn(tween(0))
                        } else {
                            fadeIn(tween(180, easing = KyonoEaseOut)) +
                                slideInVertically(tween(180, easing = KyonoEaseOut)) { with(bubblePopDensity) { 4.dp.roundToPx() } }
                        },
                        // B5: 全行の位置を無条件で記録するだけ(スクロール判断はLaunchedEffect側)。
                        modifier = Modifier.onGloballyPositioned { coords ->
                            sdRowPositions[index] = coords.positionInParent().y
                        },
                    ) {
                    when (m) {
                        // index.html:482-483 .sd-row.user .sd-b(黄色系吹き出し・右寄せ)
                        is SdBubble.User -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            // アクセシビリティ対応: liveRegionはこのTextコンポーザブル自体に付ける
                            // (メッセージ全体を囲むsdLog Columnには付けない)。messagesは末尾へのみ
                            // 追加され、for(m in messages)はkey()なし=位置ベース記憶のため、既存の
                            // 吹き出しは新規吹き出し追加時に再コンポーズされず、このTextは
                            // 「新規生成された瞬間」だけliveRegionが発火する。
                            // UI/UXパリティ監査GO-12(2026-07-28): index.html:481 .sd-b{font-size:15px;
                            // line-height:1.75}の1:1移植。指定なしでMaterial3既定(bodyLarge=16sp/24sp=
                            // 比率1.5)にフォールバックし、Web版より行間が詰まっていた欠落を修正する。
                            Text(
                                m.text, color = colors.ink, fontSize = 15.sp, lineHeight = 26.25.sp,
                                modifier = Modifier
                                    .background(colors.yellowSoft, RoundedCornerShape(16.dp, 16.dp, 6.dp, 16.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("sdUserBubble")
                                    .semantics { liveRegion = LiveRegionMode.Polite },
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
                                // アクセシビリティ対応: liveRegionは本文Textのみに付け、この吹き出しの
                                // Column(caution文言・動画ボタンを含む)には付けない。動画ボタンが
                                // mergeDescendants経由でTalkBackから個別に触れなくなる副作用を避けるため。
                                if (m.text.isNotEmpty()) {
                                    // UI/UXパリティ監査GO-12(2026-07-28): index.html:481 .sd-b{font-size:15px;
                                    // line-height:1.75}の1:1移植。
                                    Text(
                                        m.text, color = colors.ink, fontSize = 15.sp, lineHeight = 26.25.sp,
                                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                                    )
                                }
                                // index.html:3330 sdAnswerFallback1通目の末尾<span>(小さめ注意書き)の1:1移植。
                                if (m.fallbackCaution) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "※つらい症状（強い痛み・胸の苦しさ・熱など）があるときは、メールより先に医療機関に相談してね",
                                        color = colors.sub, fontSize = 13.sp,
                                        modifier = Modifier.testTag("sdFallbackCaution"),
                                    )
                                }
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
                        // TASK-C2-2026-07-27-soudan-staged-reveal.md: index.html:3084 sdTypingNode()の
                        // 1:1移植。「…」の3点を位相をずらして明滅させるタイピングドット。
                        is SdBubble.Typing -> Row(verticalAlignment = Alignment.Bottom) {
                            KyonoCharaImage("chara_hitokoto", Modifier.size(38.dp))
                            Spacer(Modifier.width(8.dp))
                            Box(
                                Modifier
                                    .background(colors.card, RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp))
                                    .border(1.5.dp, colors.line, RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp))
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                                    .testTag("sdTypingBubble"),
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    repeat(3) { dotIndex ->
                                        val infinite = rememberInfiniteTransition(label = "sdTypingDot$dotIndex")
                                        val alpha by infinite.animateFloat(
                                            initialValue = 0.3f,
                                            targetValue = 1f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(600, delayMillis = dotIndex * 150, easing = LinearEasing),
                                                repeatMode = RepeatMode.Reverse,
                                            ),
                                            label = "sdTypingDotAlpha$dotIndex",
                                        )
                                        Box(Modifier.size(7.dp).background(colors.sub.copy(alpha = alpha), androidx.compose.foundation.shape.CircleShape))
                                    }
                                }
                            }
                        }
                        // index.html:3323-3330 sdAnswerFallback2通目(逃げ道リンク3つ)の1:1移植。
                        // ①mailto(検索タブと同じopenMailIntent流用)②クリップボードコピー③検索タブへ遷移。
                        is SdBubble.FallbackLinks -> Row(verticalAlignment = Alignment.Bottom) {
                            KyonoCharaImage("chara_hitokoto", Modifier.size(38.dp))
                            Spacer(Modifier.width(8.dp))
                            val context = LocalContext.current
                            val clipboard = LocalClipboardManager.current
                            val copyScope = rememberCoroutineScope()
                            var copied by remember { mutableStateOf(false) }
                            Column(
                                Modifier.fillMaxWidth(0.86f)
                                    .background(colors.card, RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp))
                                    .border(1.5.dp, colors.line, RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                            ) {
                                // アクセシビリティ対応: このFallbackLinks吹き出しには単独の本文Textが
                                // 無いため、先頭のボタン(既に押せる要素として自前のsemanticsノードを
                                // 持つ)にliveRegionだけ足す。ボタン自体のcontentDescription/クリック
                                // 動作はKyonoGhostButton内部の実装のまま変えない。
                                KyonoGhostButton(
                                    "📮 この悩み、オガトレに届ける",
                                    {
                                        val subject = "【相談室リクエスト】きょうのオガトレ"
                                        val body = "相談した内容:\n${m.rawUserText}\n---\n送信元: きょうのオガトレ「オガトレ相談室」"
                                        openMailIntent(context, SD_MAIL, subject, body)
                                    },
                                    Modifier.testTag("sdFallbackMailBtn").semantics { liveRegion = LiveRegionMode.Polite },
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    if (copied) "コピーしました✅" else "📋 メールがひらかない方はアドレスをコピー",
                                    color = colors.tealInk, fontSize = 14.sp, fontWeight = FontWeight.Black,
                                    modifier = Modifier
                                        .clickable {
                                            clipboard.setText(AnnotatedString(SD_MAIL))
                                            copied = true
                                            copyScope.launch { delay(kyonoTransientMessageMillis(store)); copied = false }
                                        }
                                        .testTag("sdFallbackCopyBtn"),
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "🔍 動画を探すタブでさがしてみる",
                                    color = colors.tealInk, fontSize = 14.sp, fontWeight = FontWeight.Black,
                                    modifier = Modifier.clickable { onOpenSearch() }.testTag("sdFallbackSearchBtn"),
                                )
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
                                // アクセシビリティ対応: liveRegionは案内文Textのみに付け、下の2つの
                                // ボタン(はじめる！/まずは1本だけ)は個別のTalkBack操作対象のまま残す。
                                Text(
                                    if (m.replacing) "いまのプランと入れ替える？きょうの1本が、あなたの${m.label}プランになるよ"
                                    else "きょうの1本が、あなたの${m.label}プランになるよ！2週間いっしょにやってみる？",
                                    color = colors.ink,
                                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
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
            }

            // ---- チップ列(index.html:3139 sdRenderChips相当・.chip/.catbtnの1:1移植) ----
            Column(Modifier.background(colors.card).padding(horizontal = 14.dp, vertical = 8.dp)) {
                when (val mode = chipsMode) {
                    is SdChipsMode.None -> {} // crisis直後: チップ・カテゴリタブなし(index.html:3143-3145)
                    is SdChipsMode.Intents -> {
                        // TASK-C2-2026-07-27-chips-overflow-and-bubble-pop.md §2/§5: index.html:471
                        // .sd-foot .sd-catrow{flex-wrap:wrap}の1:1移植。相談室フッターのチップ行は
                        // 例外的に横スクロールだが、カテゴリ行だけは折り返しに戻る指定。
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).testTag("sdCatRow"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            SD_CHIP_CATS.forEach { cat ->
                                KyonoCatButton(cat.label, cat.key == mode.activeCat, { chipsMode = SdChipsMode.Intents(cat.key) }, Modifier.testTag("sdCat_${cat.key}"))
                            }
                        }
                        val activeCat = SD_CHIP_CATS.find { it.key == mode.activeCat } ?: SD_CHIP_CATS[0]
                        val ids = sdCatIntentIds(activeCat).toSet()
                        // TASK-C2-2026-07-27-chips-overflow-and-bubble-pop.md §1/§5: index.html:470
                        // .sd-foot .chips{flex-wrap:nowrap;overflow-x:auto}(相談室フッターのチップ行
                        // だけの例外的な横スクロール)+472 .fade-r(右端フェード+「›」ヒント)の1:1移植。
                        FadingChipRow(modifier = Modifier.fillMaxWidth(), testTag = "sdChips") {
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
                        FadingChipRow(modifier = Modifier.fillMaxWidth(), testTag = "sdChips") {
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
                            // index.html:3160-3163 未チェックの人への導線(体の悩み系回答にだけ出す)の1:1移植。
                            if (intent != null && !intent.safety && !hasType && SOUDAN_BODY_INTENTS.contains(intent.id)) {
                                item {
                                    KyonoChip("30秒のかたさチェックやってみる?", { onOpenQuiz() }, Modifier.padding(end = 8.dp).testTag("sdQuizChip"))
                                }
                            }
                            item {
                                KyonoChip("べつの悩みをそうだん", { chipsMode = SdChipsMode.Intents("body") }, Modifier.padding(end = 8.dp).testTag("sdBackToIntentsBtn"))
                            }
                        }
                    }
                    is SdChipsMode.Nearmiss -> {
                        FadingChipRow(modifier = Modifier.fillMaxWidth(), testTag = "sdChips") {
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
                        // TASK-C2-2026-07-29-ux-audit-G.md G5: index.html:1230 placeholder="例: 肩がこる／
                        // 前屈できない"の1:1移植。何を打てばいいかの手本が消えていた欠落。
                        placeholder = { Text("例: 肩がこる／前屈できない") },
                    )
                    Spacer(Modifier.width(8.dp))
                    KyonoPrimaryButton("送信", { sendText() }, Modifier.weight(0.4f).testTag("sdSendBtn"), enabled = !sdPending)
                }
                // GO-G16(5視点ワンループ): 高さ92%のシートで✕が最上部右端40×40だけだったため、
                // 入力欄の下にも「とじる」を1つ追加する。
                Spacer(Modifier.height(8.dp))
                KyonoLineButton("とじる", onClose, Modifier.testTag("soudanCloseBtnBottom"))
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

// 2週間プラン完走お祝いカード欠落修正タスク(TASK-C2-2026-07-27-plan-completion-celebration.md):
// app-record.js/index.html:1795 planFinishedCache相当(完走直後の「もう2週間続ける」用に
// intentId/label/videos/daysだけ退避する)。
data class PlanFinishedCache(val intentId: String, val label: String, val videos: List<String>, val days: Int)

// index.html:1781 renderPlanCard相当(進捗バー・「やめる」は下線付きテキストリンクでボタンではない)。
// ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md):
// index.html:667-676 #planCard の1:1移植。KyonoCard化(ホーム画面スクショで唯一浮いて見えていた箇所)。
//
// 完走(finished)時はこのカード自体を描画しない(呼び出し側がonFinishedを受けてPlanDoneCardを
// 表示する設計に変更 — TASK-C2-2026-07-27-plan-completion-celebration.mdで発見: 従来はfinished時に
// onClearedを即座に呼んでいたため、呼び出し側でplan=nullになり「finishedの表示」自体が
// 次の再合成で消えてしまうバグがあった)。
@Composable
fun PlanProgressCard(store: RecordStore, plan: SdPlanData, onCleared: () -> Unit, onFinished: (PlanFinishedCache) -> Unit) {
    val colors = LocalKyonoColors.current
    val today = jp.ogatore.kyouno.record.RecordLogic.todayStr(java.time.Instant.now())
    val dayNum = (jp.ogatore.kyouno.record.RecordLogic.daysBetween(plan.start, today) + 1).coerceAtLeast(1)
    val finished = dayNum > plan.days
    if (finished) {
        // index.html:1798 planFinished時のstore.set("plan",null)相当。合成中の副作用を避けるため
        // LaunchedEffectで1度だけ実行する。
        LaunchedEffect(plan) {
            onFinished(PlanFinishedCache(plan.intentId, plan.label, plan.videos, plan.days))
            store.set("plan", null as SdPlanData?)
            onCleared()
        }
        return
    }
    KyonoCard(Modifier.testTag("planCard")) {
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
        // index.html:414-415 .bar/.bar>div(teal系グラデーションの進捗バー・transition:width .4s)の
        // 1:1移植。挙動パリティ監査タスク(TASK-C2-2026-07-27-behavior-parity-audit.md §A):
        // 幅の変化が無アニメーションだったためanimateFloatAsStateを追加。
        val progress = (dayNum.toFloat() / plan.days.toFloat()).coerceIn(0f, 1f)
        val animatedProgress by animateFloatAsState(progress, tween(400), label = "planBar")
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(14.dp)
                .background(colors.line, RoundedCornerShape(99.dp)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .background(colors.teal, RoundedCornerShape(99.dp)),
            )
        }
    }
}

// index.html:678-684 #planDoneCard(プラン完走のお祝い。chara-congrats+紙吹雪+3ボタン)の1:1移植
// (TASK-C2-2026-07-27-plan-completion-celebration.md)。
@Composable
fun PlanDoneCard(
    cache: PlanFinishedCache,
    alreadyCelebrated: Boolean,
    onCelebrate: () -> Unit,
    onPlanAgain: () -> Unit,
    onStartQuiz: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = LocalKyonoColors.current
    // index.html:1759 planCelebrated(セッション内で紙吹雪を重ね撃ちしない)の1:1移植。
    // remember(初回合成時のみ評価)でキャプチャし、onCelebrate()が呼ばれてalreadyCelebratedが
    // trueに変わっても紙吹雪アニメーションの途中で消えないようにする。
    val showConfettiOnce = remember { !alreadyCelebrated }
    LaunchedEffect(Unit) { if (!alreadyCelebrated) onCelebrate() }
    Box(Modifier.testTag("planDoneCard")) {
        KyonoGradientCard(KyonoGradient.Mint) {
            // フォント適用漏れ・キャラ/タイプ画像の欠落修正タスク(TASK-C2-2026-07-26-visual-parity-fonts-characters.md)
            // §2 キャラクター画像: index.html:679 #planDoneCard(chara-congrats.png 84x84・中央寄せ)の1:1移植。
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                KyonoCharaImage("chara_congrats", Modifier.size(84.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = colors.pink, fontWeight = FontWeight.Black)) { append("🎉 ${cache.label}プラン完走！すごい！") }
                    append("\n${cache.days}日間続けたの、ほんとにえらい👏\n体はちゃんと応えてくれてるよ")
                },
                color = colors.ink, fontSize = 15.sp, lineHeight = 27.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().testTag("planDoneText"),
            )
            Spacer(Modifier.height(12.dp))
            KyonoPrimaryButton("💪 もう2週間続ける", onPlanAgain, Modifier.testTag("planAgainBtn"))
            Spacer(Modifier.height(8.dp))
            KyonoGhostButton("📏 かたさチェックで変化をみる", onStartQuiz, Modifier.testTag("planQuizBtn"))
            Spacer(Modifier.height(8.dp))
            KyonoLineButton("とじる", onClose, Modifier.testTag("planDoneCloseBtn"))
        }
        // §D: index.html:1921 launchConfetti()冒頭のif(matchMedia(prefers-reduced-motion:reduce).matches) return;の1:1移植。
        if (showConfettiOnce && !rememberReducedMotion()) {
            KyonoConfetti(count = 105, modifier = Modifier.matchParentSize())
        }
    }
}

private data class ConfettiParticle(
    val x0: Float, val y0: Float, val vx: Float, val vy: Float,
    val w: Float, val h: Float, val rot0: Float, val vr: Float,
    val sway: Float, val phase: Float, val color: Color,
)

// index.html:1919 launchConfetti()の見た目の1:1移植(紙吹雪演出。DUR=1500ms・4色パレット・
// 落下+左右のsway+回転)。§2-4許容箇所(markDoneのcheer選択と同じくUI装飾のみの乱数使用。
// 判定/安全ロジックではない)。
@Composable
fun KyonoConfetti(count: Int, modifier: Modifier = Modifier) {
    val palette = listOf(Color(0xFFFFD93B), Color(0xFF2BB3A3), Color(0xFFE56A9A), Color(0xFFFF8A70))
    val particles = remember(count) {
        List(count) { i ->
            ConfettiParticle(
                x0 = Random.nextFloat(),
                y0 = -0.05f - Random.nextFloat() * 0.35f,
                vx = (Random.nextFloat() - 0.5f) * 80f,
                vy = 200f + Random.nextFloat() * 260f,
                w = 6f + Random.nextFloat() * 5f,
                h = 9f + Random.nextFloat() * 6f,
                rot0 = Random.nextFloat() * 360f,
                vr = (Random.nextFloat() - 0.5f) * 720f,
                sway = 20f + Random.nextFloat() * 26f,
                phase = Random.nextFloat() * 6.283f,
                color = palette[i % palette.size],
            )
        }
    }
    val anim = remember(count) { Animatable(0f) }
    LaunchedEffect(count) {
        anim.snapTo(0f)
        anim.animateTo(1f, animationSpec = tween(durationMillis = 1500, easing = LinearEasing))
    }
    Canvas(modifier = modifier) {
        val t = anim.value * 1.5f
        particles.forEach { p ->
            val x = p.x0 * size.width + p.vx * t + p.sway * sin(t * 3f + p.phase)
            val y = p.y0 * size.height + p.vy * t
            if (y in -40f..(size.height + 40f)) {
                rotate(degrees = p.rot0 + p.vr * t, pivot = Offset(x, y)) {
                    drawRect(color = p.color, topLeft = Offset(x - p.w / 2, y - p.h / 2), size = Size(p.w, p.h))
                }
            }
        }
    }
}
