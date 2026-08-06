package jp.ogatore.kyouno

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import jp.ogatore.kyouno.card.QuizEngine
import jp.ogatore.kyouno.card.QuizScores
import jp.ogatore.kyouno.catalog.CatalogLoader
import jp.ogatore.kyouno.catalog.CatalogVideo
import jp.ogatore.kyouno.record.HomeLogic
import jp.ogatore.kyouno.record.RecordLogic
import jp.ogatore.kyouno.record.RecordStore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.Instant

// ネイティブ移植 Step 5c(マスタープラン§6 Step 5c): オンボーディング(4問チャット)・使い方ツアー(8枚)・
// かたさ診断(QuizEngine呼び出しのみ・判定ロジックはStep4で移植済み)のUI一式。
// index.html:4087-4143(ONBOARDING_SCRIPT/OB_TOUR_SLIDES)・app-quiz.js:10-79(QUESTIONS/TYPES)の
// 1:1移植(文言はWeb版から転記。判定ロジックはQuizEngine.decideTypeを呼ぶだけで再実装しない)。
//
// A2HS関連UI(a2hsModal/envBanner/脱出バナー・ホーム画面追加の誘い等)はこのファイルにも他のどこにも
// 実装しない(マスタープラン§2-2・§6 Step5c検収基準3のgrep確認対象)。Web版は2026-07-21時点で
// A2HSを起動シーケンスから既に切り離し済みのため、ネイティブ側もはじめから素通りできる。

// TASK-C2-2026-08-05-build27-round5.md R-15(本人裁定・案①硬い=赤): colorKeyがnullなら従来どおり
// 並び順の巡回(obgColors(dark:)[i % count])にフォールバックする(もじの大きさ設問はこちら)。
data class ObChip(val label: String, val v: String, val colorKey: String? = null)
data class ObQuestionDef(val key: String, val q: String, val chips: List<ObChip>)

// TASK-C2-2026-08-01-build14-fixes-and-5lens-audit.md A-2: 固定フッターCTA(かたさチェックを
// はじめる/きょうの1本を見る)のおおよその高さ(ボタン本体+外側padding)。オンボチャットの
// 自動スクロール着地位置に、この分の余白を確保するために使う。
private val KYONO_ONBOARDING_CTA_INSET = 100.dp

// TASK build31 R-40(本人指示・2026-08-06): 絵文字撤去後に語尾がぶつ切りだったため「！」で
// 締めて適宜改行する。2行目後半は本人の指定文言そのまま(「すべて無料で登録はナシ！安心してね！」・
// 漢字表記も指定どおり)。
val OB_GREET = listOf(
    "いつもありがとうございます！理学療法士のオガトレです！",
    "ここは毎日のストレッチを応援する場所だよ！\nすべて無料で登録はナシ！安心してね！",
    "最初に4つだけ教えてね！\nあなた用にこのアプリをととのえます！",
)

// index.html:4093-4102 ONBOARDING_SCRIPT.questions の1:1移植。かたさチェック本体(QUESTIONS)とは
// 別の、オンボ専用の簡易4問(bigtext/stiff/worry/anchor)。
val OB_QUESTIONS = listOf(
    ObQuestionDef(
        "bigtext", "もじの大きさ、どっちが見やすい？",
        listOf(ObChip("大きめ（いまのまま）", "big"), ObChip("ふつう", "normal")),
    ),
    ObQuestionDef(
        "stiff", "体、硬いほう？",
        listOf(
            ObChip("ガチガチかも", "hard", "rose"), ObChip("ふつう", "normal", "yellow"),
            ObChip("やわらかい", "soft", "green"), ObChip("わからない", "unknown", "neutral"),
        ),
    ),
    ObQuestionDef(
        "worry", "いちばん気になるのは？",
        listOf(
            ObChip("肩こり・首", "katakori", "orange"), ObChip("腰", "youtsuu", "rose"),
            ObChip("前屈できない", "zenkutsu", "blue"), ObChip("眠り", "nemuri", "purple"),
            ObChip("とくにない", "none", "green"),
        ),
    ),
    ObQuestionDef(
        "anchor", "ストレッチ、いつやる派？",
        listOf(
            ObChip("朝おきて", "asa", "yellow"), ObChip("おふろ上がり", "furo", "blue"),
            ObChip("寝るまえ", "neru", "purple"), ObChip("きめてない", "free", "neutral"),
        ),
    ),
)

val OB_ANCHOR_ACK = mapOf(
    // TASK build31 R-40: 相づちも同トーンで「！」締めに統一。
    "asa" to "朝おきてすぐだね！ホームにも覚えさせたよ！",
    "furo" to "おふろ上がりは体もほぐれてて効果的！覚えたよ！",
    "neru" to "寝るまえの1本はねむりにも効くよ！覚えたよ！",
    "free" to "きめなくてもOK！そのつどでだいじょうぶ！",
)

// app-quiz.js:193 WORRY_TIEBREAKと紐づくQ5語彙(katakori/yotsu/tsukare/yawaraka)への対応表
// (index.html:4370 OB_WORRY_TO_QUIZ)。"none"は対応表に含めない(実質的な悩みではないためQ5を
// スキップする対象外=worry!=="none"のときだけquizルートへ行く条件と対になっている)。
val OB_WORRY_TO_QUIZ = mapOf("katakori" to "katakori", "youtsuu" to "yotsu", "zenkutsu" to "yawaraka", "nemuri" to "tsukare")

// index.html:4108-4111 ONBOARDING_SCRIPT.routesの1:1移植(TASK-C2-2026-07-27-onboarding-routes-closing-message)。
data class ObRouteInfo(val say: List<String>, val btn: String)
val OB_ROUTES = mapOf(
    "quiz" to ObRouteInfo(listOf("そしたら30秒で硬さチェックをしよう！下のボタンタップしてね！"), "かたさチェックをはじめる"),
    // TASK build31 R-40: 語尾「！」締め(quiz側は元から「！」で不変)。
    "today" to ObRouteInfo(listOf("じゃあ今日の1本から！むずかしいことはなしだよ！"), "きょうの1本を見る"),
)

// index.html:4377 obGo()内の条件式の1:1移植(stiff=hard/unknown、またはworry!=noneならquizへ)。
fun obDecideRoute(stiff: String, worry: String): String =
    if (stiff == "hard" || stiff == "unknown" || worry != "none") "quiz" else "today"

// かたさチェックの.opt.g0〜g3(index.html:301-309)・オンボの#obChips .chip.obg0-3(index.html:537-544)と
// 同じ「明→暗」段階色パレット(bg,border)。実際の難易度でなくチップの並び順で明→暗を巡回させる
// (index.html:4211と同じ「obg"+(i%4)」方式)。ライト/ダークで別パレット。
// TASK-C2-2026-08-04-build22-yellow-return.md Z-3: 淡色チップが背景に沈む問題(IMG_8768)を
// 解消するため、境界線と対にした濃色文字(text)を追加。
private data class ObgColor(val bg: Color, val border: Color, val text: Color)
// TASK-C2-2026-08-01-build14-fixes-and-5lens-audit.md A-1: 5択の質問(部位選択など)で
// i%4のため1番目と5番目が同色になっていた欠落。5色目(青系・色相約200)を追加し5色パレットにした。
// TASK-C2-2026-08-05-build24-chip-clarity.md(案A'・本人GO): ビルド23実機で「見にくい」指摘。
// 黄CTA(#FFD93B・ink文字・濃縁)と同じ「高彩度の塗り+ink文字+カテゴリ濃縁」の文法へ刷新。
// bgを淡パステルから高彩度へ、textはカテゴリ濃色でなくink固定に統一。border据え置き。ライトのみ。
private val OBG_LIGHT = listOf(
    ObgColor(Color(0xFF6FCDA6), Color(0xFF177065), Color(0xFF3A3A35)),
    ObgColor(Color(0xFFFFDB4D), Color(0xFF7A5E00), Color(0xFF3A3A35)),
    ObgColor(Color(0xFFFFB558), Color(0xFF995400), Color(0xFF3A3A35)),
    ObgColor(Color(0xFFEE9B82), Color(0xFF863213), Color(0xFF3A3A35)),
    ObgColor(Color(0xFF7BC2E8), Color(0xFF006199), Color(0xFF3A3A35)),
)
// TASK-C2-2026-08-01-build13-round3.md ②: 旧配色は4色の色相が29〜40度に密集し、
// ダークでは「全部こげ茶」に潰れて見えた。4色目を茶系からローズ/マゼンタ(色相約320度)へ
// 大きく振り、緑(154)・黄(48)・橙(28)・薔薇(320)へ色相を広く分散させた。
// TASK-C2-2026-08-04-build22-yellow-return.md Z-3: ダークはbuild21から不変(text=既存ink)。
private val OBG_DARK = listOf(
    ObgColor(Color(0xFF223D33), Color(0xFF2E5A48), Color(0xFFF2EDE1)),
    ObgColor(Color(0xFF4A3D14), Color(0xFF6B5A1C), Color(0xFFF2EDE1)),
    ObgColor(Color(0xFF4D3018), Color(0xFF704620), Color(0xFFF2EDE1)),
    ObgColor(Color(0xFF4A1F35), Color(0xFF6B2C4C), Color(0xFFF2EDE1)),
    ObgColor(Color(0xFF1F3A4D), Color(0xFF2B5570), Color(0xFFF2EDE1)),
)
private fun obgColors(dark: Boolean) = if (dark) OBG_DARK else OBG_LIGHT

// TASK-C2-2026-08-05-build27-round5.md R-15: 意味リンク配色用の色キー辞書。既存OBG_LIGHT/OBG_DARKの
// 5色(green/yellow/orange/rose/blue)はそのまま名前引きできるようにし、新色2つ(purple/neutral・
// 本人指定のダーク値込み)を追加する。OBG_LIGHT/OBG_DARK自体は変更しない(かたさチェック本体
// Q1-Q4の並び順巡回・obgColors(dark:)[i % count]がそのまま使い続けるため)。
private val OBG_NAMED_LIGHT: Map<String, ObgColor> = mapOf(
    "green" to OBG_LIGHT[0], "yellow" to OBG_LIGHT[1], "orange" to OBG_LIGHT[2], "rose" to OBG_LIGHT[3], "blue" to OBG_LIGHT[4],
    "purple" to ObgColor(Color(0xFFB1A6E6), Color(0xFF463B8C), Color(0xFF3A3A35)),
    "neutral" to ObgColor(Color(0xFFE7E0D2), Color(0xFF6B6557), Color(0xFF3A3A35)),
)
private val OBG_NAMED_DARK: Map<String, ObgColor> = mapOf(
    "green" to OBG_DARK[0], "yellow" to OBG_DARK[1], "orange" to OBG_DARK[2], "rose" to OBG_DARK[3], "blue" to OBG_DARK[4],
    "purple" to ObgColor(Color(0xFF2E2847), Color(0xFF453C6B), Color(0xFFF2EDE1)),
    "neutral" to ObgColor(Color(0xFF2F2C26), Color(0xFF4A463E), Color(0xFFF2EDE1)),
)
private fun obgNamedColor(key: String, dark: Boolean): ObgColor? = (if (dark) OBG_NAMED_DARK else OBG_NAMED_LIGHT)[key]

// TASK-C2-2026-07-30-icon-system-addendum-chips.md: 部位・時間帯チップの生成イラスト
// (硬さチェック6タイプ=KyonoTypeArtはこの対象外)。ObChip.vの値と1:1対応させる
// (SettingsScreen.ktのやるタイミング「変える」もasa/furo/neruキーを共有するため再利用できる)。
// TASK-C2-2026-08-01-build13-round3.md ①: hard/normal/soft/unknown(かたさ設問用の前屈シルエット
// 3種、build11で絵は追加済みだったが本マップ未配線でAndroidだけ無表示だった欠落を解消)。
// 呼び出し側でq.key=="bigtext"のときはこのマップを一切参照しない(normalキー衝突対策)。
fun obChipIconRes(v: String): Int? = when (v) {
    "hard" -> R.drawable.chip_hard
    "normal" -> R.drawable.chip_normal
    "soft" -> R.drawable.chip_soft
    "unknown" -> R.drawable.chip_unknown
    "katakori" -> R.drawable.chip_katakori
    "youtsuu" -> R.drawable.chip_youtsuu
    "zenkutsu" -> R.drawable.chip_zenkutsu
    "nemuri" -> R.drawable.chip_nemuri
    "none" -> R.drawable.chip_none
    "asa" -> R.drawable.chip_asa
    "furo" -> R.drawable.chip_furo
    "neru" -> R.drawable.chip_neru
    "free" -> R.drawable.chip_free
    else -> null
}

data class ChatBubble(val text: String, val fromUser: Boolean)

// index.html:4211 「今後変えたくなったら…」bigtext回答時の相槌の1:1移植(obPick内)。
private const val OB_BIGTEXT_ACK = "OK！今後変えたくなったら「マイ記録」タブの「マイ設定」でいつでも変更できるよ！"

// index.html:4395-4434 obOpen/obAskQ/obPick/obGoの1:1移植。「welcome」専用画面は無く、この会話UI自体が
// あいさつ(greet)を最初の3吹き出しとして描画することでwelcome相当を兼ねる(index.html:4405)。
//
// 見た目パリティ第2弾(TASK-C2-2026-07-26-visual-parity-round2.md §1): index.html:4182 obSay()の
// 「1.5秒間隔で吹き出しが1つずつ出る」演出をLaunchedEffect+delay(1500)のコルーチンで1:1再現する。
// §D(TASK-C2-2026-07-27-behavior-parity-audit.md): index.html:4145 obReducedMotion()/4186
// const wait=obReducedMotion()?0:1500の1:1移植として、reduced-motion時は待機をなくす。
@Composable
fun OnboardingScreen(store: RecordStore, onComplete: (route: String, presetWorry: String?) -> Unit) {
    // TASK-C2-2026-07-31-build12-journey2-splash-emoji.md W1-a: 初回起動(まだonboarded==falseの
    // タイミング)だけ、見出しを「📖 使い方ツアー」+4点バーに差し替える。使い方タブ経由の再入場
    // (onboarded==true済み)は既存の「🌱 はじめてガイド」・バーなしのまま。
    val isFirstRun = remember { !store.get("onboarded", false) }
    var bubbles by remember { mutableStateOf(listOf<ChatBubble>()) }
    var activeQuestion by remember { mutableStateOf<ObQuestionDef?>(null) }
    var routeCta by remember { mutableStateOf<ObRouteInfo?>(null) }
    val answers = remember { mutableStateMapOf<String, String>() }
    val pickChannel = remember { Channel<ObChip>(Channel.CONFLATED) }
    val ctaChannel = remember { Channel<Unit>(Channel.CONFLATED) }

    fun finish() {
        // bigtext/anchorは実際の設定として即時反映(index.html:4218-4235 obPick)
        store.set("bigtext", answers["bigtext"] == "big")
        answers["anchor"]?.let { store.set("anchor", it) }
        val stiff = answers["stiff"] ?: "normal"
        val worry = answers["worry"] ?: "none"
        val route = obDecideRoute(stiff, worry)
        val presetWorry = if (worry != "none") OB_WORRY_TO_QUIZ[worry] else null
        store.set("onboarded", true)
        // index.html:4375-4384 はじめの1本ガイド開始条件(オンボ完走・通算0日・fd未設定のときだけ)
        if (RecordLogic.loadStreak(store).total == 0 && store.get("fd", null as String?) == null) {
            store.set("fd", "go")
            store.set("fdday", RecordLogic.todayStr(Instant.now()))
        }
        onComplete(route, presetWorry)
    }

    val obReducedMotion = rememberReducedMotion()
    LaunchedEffect(Unit) {
        // index.html:4182 obSay()の1:1移植: 1行ごとに表示→1.5秒待つ、を繰り返す。
        suspend fun say(lines: List<String>) {
            val wait = if (obReducedMotion) 0L else 1500L
            for (line in lines) {
                bubbles = bubbles + ChatBubble(line, false)
                delay(wait)
            }
        }
        say(OB_GREET)
        for (q in OB_QUESTIONS) {
            // index.html:4197 obAskQ(): 質問文もobSay経由(1行)なので表示後に1.5秒待ってからチップを出す。
            say(listOf(q.q))
            activeQuestion = q
            val picked = pickChannel.receive()
            activeQuestion = null
            answers[q.key] = picked.v
            bubbles = bubbles + ChatBubble(picked.label, true) // index.html:4221 obPick内obBubble("user",...)は即時
            when (q.key) {
                "anchor" -> say(listOf(OB_ANCHOR_ACK[picked.v] ?: "OK！おぼえたよ！"))
                "bigtext" -> say(listOf(OB_BIGTEXT_ACK))
            }
        }
        // index.html:4108-4111 ONBOARDING_SCRIPT.routes: 相槌の後にもう1往復、締めメッセージ+
        // 専用ボタンを表示し、タップされて初めてfinish()(=画面遷移)する(自動遷移しない)。
        val stiff = answers["stiff"] ?: "normal"
        val worry = answers["worry"] ?: "none"
        val routeInfo = OB_ROUTES.getValue(obDecideRoute(stiff, worry))
        say(routeInfo.say)
        routeCta = routeInfo
        ctaChannel.receive()
        routeCta = null
        finish()
    }

    val themeSetting = store.get("theme", "light")
    KyonoTheme(themeSetting, bigText = store.get("bigtext", true)) {
        val colors = LocalKyonoColors.current
        val dark = colors.bg == KyonoDarkColors.bg
        // TASK-C2-2026-07-28-onboarding-sheet-tap-stolen.md: 新しい設問/選択肢が追加されても
        // スクロール位置が追従しておらず、最新の選択肢がシートの可視領域の端(タップ判定が
        // 効かない位置)ぎりぎりに描画される欠落があった。追加のたびに最下部へ自動スクロールし、
        // 選択肢が常に見える・押せる位置に来るようにする。
        val obScrollState = rememberScrollState()
        // TASK-C2-2026-07-30-onboarding-scroll-and-copy.md A1: 固定delay(60)後に1回だけ
        // scrollする実装だと、バブルのポップイン(180ms)と競合し、レイアウト確定前に着地する
        // ことがあった。SoudanSheet.kt:485-497のフレーム単位リトライ方式を移植: 「いま増えた
        // バブル」の位置がonGloballyPositionedで記録されるまで最大10フレーム待ってから
        // スクロールする。選択肢・CTAはA2でスクロール領域の外(固定フッター)に出たため、
        // ここでスクロール対象にする必要があるのはbubblesの増減だけになった。
        val obRowPositions = remember { mutableStateMapOf<Int, Float>() }
        LaunchedEffect(bubbles.size) {
            val targetKey = bubbles.lastIndex
            if (targetKey < 0) return@LaunchedEffect
            var y = obRowPositions[targetKey]
            var tries = 0
            while (y == null && tries < 10) {
                withFrameNanos {}
                y = obRowPositions[targetKey]
                tries++
            }
            if (obReducedMotion) obScrollState.scrollTo(obScrollState.maxValue) else obScrollState.animateScrollTo(obScrollState.maxValue)
        }
        // TASK-C2-2026-07-30-onboarding-scroll-and-copy.md A2: TourScreen(D6)と同じ構造。
        // 選択肢・CTAボタンを本文と同じverticalScrollから外し、外側Columnの固定フッターにする。
        // これでCTAは常に画面内の同じ位置にあり、本文の長さに関わらず動かない。
        Column(Modifier.fillMaxSize().background(colors.bg)) {
        // W1-a: 初回起動だけ見出しをverticalScroll外の固定上部へ移し「📖 使い方ツアー」を出す。
        // 再入場は既存どおり本文内に「🌱 はじめてガイド」を出す。
        // TASK-C2-2026-08-03-build18-tutorial-quality.md B-9(本人GO): この見出し下で
        // 「4点バー(この画面)→チェック4/5段(Quiz/Result)→ツアー7/8点(Tour)」と3種類の
        // 進捗バーが連続して出ていた引き算。質問4つはチャットの吹き出しの流れそのもので
        // 十分伝わるため、この4点バーだけを消す(チェック・ツアーの2種は残す)。
        if (isFirstRun) {
            Text(
                "使い方ツアー", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.testTag("obTitle").padding(horizontal = 20.dp, vertical = 20.dp),
            )
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(obScrollState).padding(20.dp),
        ) {
            if (!isFirstRun) {
                Text("はじめてガイド", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("obTitle"))
            }
            Spacer(Modifier.height(12.dp))
            // TASK-C2-2026-07-27-chips-overflow-and-bubble-pop.md §3: index.html:4149 .sd-pop
            // (opacity0→1・translateY(4px)→0・.18s ease-out)の1:1移植。reduced-motion時は無演出即表示。
            val obBubblePopDensity = LocalDensity.current
            bubbles.forEachIndexed { bIndex, b ->
                AnimatedVisibility(
                    visible = true,
                    enter = if (obReducedMotion) {
                        fadeIn(tween(0))
                    } else {
                        fadeIn(tween(180)) + slideInVertically(tween(180)) { with(obBubblePopDensity) { 4.dp.roundToPx() } }
                    },
                    // A1: 全行の位置を無条件で記録するだけ(スクロール判断はLaunchedEffect側)。
                    modifier = Modifier.onGloballyPositioned { coords ->
                        obRowPositions[bIndex] = coords.positionInParent().y
                    },
                ) {
                // index.html:478-483,4150 .sd-row/.sd-b/.sd-ava(相談室と共用の吹き出しCSS・
                // chara-hitokotoアバターをオンボでも流用)の1:1移植。
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = if (b.fromUser) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    if (!b.fromUser) {
                        KyonoCharaImage("chara_good", Modifier.size(38.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Box(
                        Modifier.fillMaxWidth(0.82f)
                            .let {
                                if (b.fromUser) it.background(colors.yellowSoft, RoundedCornerShape(16.dp, 16.dp, 6.dp, 16.dp))
                                else it.background(colors.card, RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp)).border(1.5.dp, colors.borderStrong, RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp))
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        // アクセシビリティ対応(スクリーンリーダー無音問題の解消): liveRegionはこの
                        // 「1つの吹き出しのTextコンポーザブル」自体に付ける(親のColumn/Rowコンテナには
                        // 付けない)。bubblesリストは常に末尾へのみ追加され、この for ループは
                        // key()なしの位置ベース記憶のため、既存の吹き出し(bはbubbles中で同じ位置・
                        // 同じ値のまま)はbubbles.size変化時も再コンポーズがスキップされ、この
                        // Text自体は「新しく生成された瞬間」にしかliveRegionが発火しない
                        // (=新しい吹き出しが増えるたびに全文が読み直される事故を回避)。
                        // TASK-C2-2026-08-04-build19-tour-redesign.md T-7: lineHeight 26sp@15ptだと
                        // 行がバラけて痩せて見えていた(iOS版lineSpacing 11→7の等価値)ため詰める。
                        Text(
                            b.text, color = colors.ink, fontSize = 15.sp, lineHeight = 22.sp,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                }
                }
            }
            // TASK-C2-2026-08-01-build14-fixes-and-5lens-audit.md A-2: 固定フッターCTA
            // (かたさチェックをはじめる/きょうの1本を見る)の高さぶん、スクロール末尾に
            // インセットを確保する。maxValueまでの自動スクロールもこの分だけ多く進むように
            // なり、最後の吹き出しがCTAに隠れず全文読めるようになる。
            Spacer(Modifier.height(KYONO_ONBOARDING_CTA_INSET))
        }
        // A2: 選択肢・CTAは固定フッター(スクロールしない)。
        val q = activeQuestion
        if (q != null) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
                Text("タップしてえらんでね", color = colors.sub, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                val palette = obgColors(dark)
                q.chips.forEachIndexed { i, chip ->
                    // TASK-C2-2026-08-05-build27-round5.md R-15: colorKeyがあれば意味リンク配色を
                    // 使う(かたさ/悩み/時間帯)。無ければ従来どおり並び順の巡回(もじの大きさ)。
                    val c = chip.colorKey?.let { obgNamedColor(it, dark) } ?: palette[i % palette.size]
                    // TASK-C2-2026-07-30-icon-system-addendum-chips.md: 部位・時間帯チップの
                    // 生成イラスト(硬さチェック6タイプ=KyonoTypeArtはこの対象外・触らない)。
                    // TASK-C2-2026-08-01-build13-round3.md ①: 「もじの大きさ」設問(key=="bigtext")は
                    // 絵を一切付けない(バグ修正: chip.v="normal"がかたさ設問と衝突し、かたさ用の
                    // 前屈絵が誤って出ていた欠落の根本対策)。代わりにボタン文字自身のサイズで
                    // 「大きめ/ふつう」を実演する(自己実演型)。
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                            .background(c.bg, RoundedCornerShape(16.dp))
                            .border(2.5.dp, c.border, RoundedCornerShape(16.dp))
                            .clickable { pickChannel.trySend(chip) }
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                            .testTag("obChip_${q.key}_${chip.v}"),
                    ) {
                        val chipIconRes = if (q.key != "bigtext") obChipIconRes(chip.v) else null
                        if (chipIconRes != null) {
                            Image(
                                painter = painterResource(chipIconRes),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        val labelSize = if (q.key == "bigtext" && chip.v == "big") 20.sp else 16.sp
                        // TASK-C2-2026-08-05-build29-round7.md R-22(本人指示・IMG_8823/8824):
                        // 文字ウェイトをいちばん太いblack900へ。サイズ・色は不変。
                        Text(chip.label, color = c.text, fontSize = labelSize, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        val cta = routeCta
        if (cta != null) {
            KyonoPrimaryButton(
                cta.btn, { ctaChannel.trySend(Unit) },
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp).testTag("obRouteCtaBtn"),
            )
        }
        }
    }
}

data class QuizOptDef(val label: String, val note: String, val score: Int?, val worryKey: String?)
data class QuizQuestionDef(val key: String, val title: String, val note: String, val opts: List<QuizOptDef>, val artRes: Int? = null)

// app-quiz.js:89-137 QUIZ_ARTの1:1移植。momo/kokaは実写(R.drawable.quiz_q1/quiz_q2、
// assets/check/q1.jpg・q2.jpgをそのまま同梱・§6 Step5c検収基準「QUIZ_ART写真のアセット同梱」)。
// kenko/ashiは手描きSVGで「はな/あごの高さ線」「かかとの浮き」という判定基準そのものを可視化して
// おり装飾ではない(QuizArt.ktのQuizArtKenko/QuizArtAshiとして移植済み・TASK-C2-2026-07-28-
// quiz-result-reach-parity.md §4)。worryのみ図解なし(選択肢はテキストのみ)。
val QUIZ_QUESTIONS = listOf(
    QuizQuestionDef(
        "momo", "立って前屈 手はどこまでいく？", "ひざを曲げずに ゆっくり倒れてみて",
        artRes = R.drawable.quiz_q1,
        opts = listOf(
            QuizOptDef("床にペタッとつく", "手のひら全体がゆかにつく（ゆかタッチ）", 0, null),
            QuizOptDef("つま先にさわれる", "指先が足先〜床すれすれ（目安 0〜10cm）", 1, null),
            QuizOptDef("すねの途中まで", "指先がすねの中ほどで止まる（目安 10〜25cm）", 2, null),
            QuizOptDef("ひざから下に行かない", "指先がひざ上で止まる（目安 25cm以上）", 3, null),
        ),
    ),
    QuizQuestionDef(
        "koka", "あぐらで座ると ひざは？", "床に座って 足の裏どうしを合わせてみて",
        artRes = R.drawable.quiz_q2,
        opts = listOf(
            QuizOptDef("床にペタッと近い", "ひざと床のすき間が こぶし1個未満", 0, null),
            QuizOptDef("ちょっと浮く", "すき間 こぶし1〜2個ぶん", 1, null),
            QuizOptDef("山みたいに浮く", "すき間 こぶし3個以上", 2, null),
            QuizOptDef("そもそもあぐらがつらい", "骨盤が立たず 体が後ろに倒れてしまう", 3, null),
        ),
    ),
    QuizQuestionDef(
        "kenko", "胸の前で両ひじをつけて上げると どこまで上がる？", "手のひらを合わせて 胸の前でひじをくっつけたまま ゆっくり上げてみて",
        listOf(
            QuizOptDef("鼻より上まで上がる", "ひじをつけたまま鼻の高さをこえる", 0, null),
            QuizOptDef("あごより上まで上がる", "ひじをつけたままあごの高さをこえる", 1, null),
            QuizOptDef("ひじはつくけど あまり上がらない", "ひじはくっつくが胸〜肩の高さまでしか上がらない", 2, null),
            QuizOptDef("そもそもひじがつかない", "胸の前でひじをくっつけることができない", 3, null),
        ),
    ),
    QuizQuestionDef(
        "ashi", "かかとを付けたまま しゃがめる？", "和式トイレのポーズ 無理はしないでね",
        listOf(
            QuizOptDef("余裕でしゃがめる", "かかとを付けたまま深くしゃがみ 保持できる", 0, null),
            QuizOptDef("しゃがめるけど ぐらぐら", "しゃがめるが姿勢を保てない", 1, null),
            QuizOptDef("かかとが浮いちゃう", "足首の曲がり（背屈）が足りないサイン", 2, null),
            QuizOptDef("後ろにコロンと転がる", "足首＋股関節の複合的な硬さのサイン", 3, null),
        ),
    ),
    QuizQuestionDef(
        "worry", "いちばんの悩みは？", "あなたに合うおすすめの仕上げに使います",
        listOf(
            QuizOptDef("肩こり・首こり", "デスクワーク・スマホ首のお供に", null, "katakori"),
            QuizOptDef("腰痛", "骨盤まわりからケアします", null, "yotsu"),
            QuizOptDef("疲れ・眠りの浅さ", "自律神経をととのえます", null, "tsukare"),
            QuizOptDef("とにかく柔らかくなりたい", "王道の柔軟コースへ", null, "yawaraka"),
        ),
    ),
)

data class TypeInfo(val name: String, val copy: String, val hope: String, val pt: String, val area: String)

// app-quiz.js:45-79 TYPES の1:1移植(name/copy/hope/pt/area。rx/poolは動画選出専用データのため
// TYPE_RX_POOLへ別に切り出す)。areaはTASK-C2-2026-07-26-result-video-recommendations.md(#result
// のrxHead文言生成)で追加。
val QUIZ_TYPES = mapOf(
    "momo" to TypeInfo(
        "つっぱりモモンガ",
        "前屈すると、つま先がとても遠い。それはあなたの脚が長い…わけではなく、もも裏がモモンガの滑空ポーズみたいにピンとつっぱっているサイン。",
        "でもモモンガも、着地すればちゃんと脚をゆるめます。もも裏は変化が出やすい場所。2週間後の前屈で、床がぐっと近くなってるはず。",
        "硬いのは<b>ハムストリングス（もも裏の筋肉）</b>。ここが硬いと骨盤が後ろに倒れたまま固定され、前屈で腰だけが無理に曲がります。放っておくと<b>腰痛や座り姿勢の悪化</b>につながる場所。逆に言えば、もも裏をゆるめるだけで前屈も腰もラクになります。",
        "もも裏",
    ),
    "koka" to TypeInfo(
        "開かずのトビラ",
        "あぐらでひざが山になるのは、股関節のとびらが閉まっているから。股関節の封印は解きたいですよね。",
        "とびらは、毎日すこしずつ油をさせば開きます。股関節は9分の習慣がいちばん効く場所。あせらずコツコツ。",
        "硬いのは<b>内もも（内転筋）とお尻（大臀筋・梨状筋）</b>。股関節を外に開く動きが制限されて、あぐら・開脚が苦手になります。股関節は体の土台なので、ここが動くと<b>歩く・座る・立つ全部がラクに</b>。腰への負担も減ります。",
        "股関節",
    ),
    "kenko" to TypeInfo(
        "飛べないダチョウ",
        "ひじをつけたまま上がらないのは、肩甲骨まわりの羽根が飛べないダチョウみたいに、すっかり休眠しているから。デスクワークの勲章です。",
        "ダチョウの羽根だって、バサバサ動かせば血が巡ります。肩甲骨がゆるむと、肩こりも呼吸もぐっとラクに。",
        "硬いのは<b>肩甲骨まわり（僧帽筋・広背筋・大胸筋など）</b>。肩甲骨の動きが小さくなると、首と肩の筋肉が代わりに働き続けて<b>肩こり・巻き肩・浅い呼吸</b>の原因に。肩甲骨を動かす習慣がつくと、背中が軽くなって姿勢も変わります。",
        "肩甲骨",
    ),
    "ashi" to TypeInfo(
        "棒立ちペンギン",
        "しゃがむとかかとがプカッ あるいは後ろにコロン。それは足首がカチッと固まっている証拠。ペンギンは可愛いけど、転ぶと痛い。",
        "足首がゆるむと、歩くのも立つのも軽くなります。つまむだけの簡単ストレッチから始めましょう。",
        "硬いのは<b>足首の背屈（すねに向けて曲げる動き）＝ふくらはぎ・アキレス腱まわり</b>。ここが硬いと、しゃがむ動作でかかとが浮き、<b>つまずき・むくみ・ふくらはぎの張り</b>につながります。足首は毎日使う関節なので、ゆるめた効果を実感しやすい場所です。",
        "足首",
    ),
    "robot" to TypeInfo(
        "ガチガチロボット",
        "全体的に、ガチガチ。でも言いかえれば、どこを伸ばしても効く「伸びしろの宝庫」ということ。",
        "ロボットにも心はあります。全身をやさしくほぐす1本から始めれば、ガチガチの体もちゃんと応えてくれます。",
        "特定の場所というより<b>全身が複合的に硬い状態</b>。この場合は部位を絞るより、全身をまんべんなく動かすルーティンで底上げするのが近道です。<b>どこを伸ばしても効く＝変化を感じやすい</b>ので、実はいちばん楽しいスタート地点だったりします。",
        "全身",
    ),
    "yawara" to TypeInfo(
        "しなやかネコ",
        "おっと、けっこうしなやか！あなたはもう「しなやかネコ」。ここから先は、そのしなやかさを守るステージです。",
        "しなやかさは資産。猫が毎朝伸びをするみたいに、朝と夜の習慣で守っていきましょう。悩みに合わせた1本もどうぞ。",
        "関節の可動域は良好です。次の課題は<b>「維持」と「使い方」</b>。柔らかくても、支える筋力や毎日の習慣が崩れると体は硬さに戻ります。朝晩の軽いルーティンで可動域を守りつつ、悩みのある部位を先回りでケアしましょう。",
        "メンテナンス",
    ),
)

// 診断結果画面「おすすめ動画3本」欠落修正タスク(TASK-C2-2026-07-26-result-video-recommendations.md):
// app-quiz.js:45-90 TYPES[].rx/poolの1:1移植(動画選出専用データ。§1-2に基づき機械抽出)。
private data class TypeRxPool(val rx: List<String>, val pool: List<String>)
private val TYPE_RX_POOL = mapOf(
    "momo" to TypeRxPool(listOf("momo7"), listOf("kaikyaku", "momoKai", "momoIsho", "zenkutsu15", "hamu10", "kaikyaku2", "kotsuban5", "yotsu12", "yotsu8", "asa10", "nagomi7")),
    "koka" to TypeRxPool(listOf("koka9"), listOf("kominka", "kokaSai", "koka22", "koka3cho", "kokaIsho", "kokaPoki", "kaikyaku", "kaikyaku90", "nagomi7", "ashisuki", "yotsu12")),
    "kenko" to TypeRxPool(listOf("kenko12"), listOf("asa5", "kenkoIsho", "kenko22", "kenkoIsho2", "kenko3cho", "katakori", "katakori8", "zutsu7", "suwatta8", "nagomi7")),
    "ashi" to TypeRxPool(listOf("ashi1"), listOf("ashi2", "ashi10", "ashi3cho", "ashiIsho", "fukura5", "fukuraMassa", "fukura8", "ashi4", "katai8st", "ashisuki")),
    "robot" to TypeRxPool(listOf("honki9"), listOf("asa10", "asaBaki9", "yoru9umi", "yoru9ice", "zenshinCho", "yoru12kai", "senaka5", "ofuro10", "nagomi7")),
    "yawara" to TypeRxPool(listOf("asa10"), listOf("asa9shi", "asaGachi5", "asa3", "honki9", "yoru9umi", "jukusui9", "jiritsu10", "ofuro6", "choyokin10", "ibuki10", "nagomi7")),
)

// app-quiz.js:81-85 WORRYの1:1移植(悩みキー→追加のおすすめ1本+ラベル。yawaraka=null相当は
// マップに含めないことで表現)。
// TASK-C2-2026-08-06-build30-round8.md R-26: おまけはリスト内「③おまけ: 〜」の形式で名乗るため、
// 旧来の「＋1本」文脈で付けていた「もう1本」語尾は不要になった(labelから削除)。
private data class WorryExtra(val v: String, val label: String)
private val WORRY_EXTRA = mapOf(
    "katakori" to WorryExtra("katakori", "肩こりさんへ"),
    "yotsu" to WorryExtra("yotsu12", "腰痛さんへ"),
    "tsukare" to WorryExtra("jiritsu10", "おつかれさんへ"),
)

// index.html:1458 V(かたさタイプおすすめ動画専用の小規模動画カタログ)のキー→YouTube動画IDの
// 1:1移植(§1-2に基づき機械抽出)。タイトル・サブタイトル等の実体はすでに移植済みの一般カタログ
// (catalog.json/CatalogLoader)に同じ動画IDが含まれているため、そちらを検索して表示に使う
// (V自体のt/s/tagsは重複移植しない)。
val QUIZ_VIDEO_KEY_TO_ID = mapOf(
    "momo7" to "CyWthETY73s", "momoKai" to "3_z8R2l4CKE", "kaikyaku" to "Re5FPU5_37g", "asa10" to "2EfFlQev4rg",
    "koka9" to "-Y5bOC_ecB0", "kominka" to "LMz4DV66bV8", "kenko12" to "ZYTlwh_FhoU", "yoru15" to "HCVb47eWgqA",
    "asa5" to "VTMYfFnkHh4", "ashi1" to "6U4fgJu0ZMw", "ashi2" to "86u3S-epkRg", "ashi10" to "t3C-N5_828k",
    "fukura5" to "gdvjMR61Z4k", "honki9" to "q8jr0KhoML4", "yaruki22" to "oV0Rqt76bhM", "yoru9umi" to "NrJIhK_gOXc",
    "yoru9ice" to "_2g_qWssAEI", "asa9shi" to "H9ctJbhTR0Y", "jiritsu10" to "XkgsF39kkRw", "ashisuki" to "4SsJx5W8hNQ",
    "katakori" to "7FY6SR6cyts", "yotsu12" to "vZ4LYE0Ahe8", "nagomi7" to "aIIU5R2l-kQ", "momoIsho" to "CnxxUFl373A",
    "zenkutsu15" to "0-LT6LWLwOQ", "hamu10" to "7LgLQuHx-DI", "kaikyaku2" to "P6-GHA1AuwE", "kotsuban5" to "3F53Us-nwDY",
    "yotsu8" to "laNHVUwdxZM", "kokaSai" to "0jhnX8BPzes", "koka22" to "uG2_e0Y7qkw", "koka3cho" to "Imgtayb1v78",
    "kokaIsho" to "3br07_9ZbyQ", "kokaPoki" to "_ETT9HRUxQE", "kaikyaku90" to "2gb2LlmK5XQ", "kenkoIsho" to "LdnJXMB2kZs",
    "kenko22" to "Qxqcjj_k0WE", "kenkoIsho2" to "xhloKtNFgeQ", "kenko3cho" to "lUOSasCDvM8", "katakori8" to "Sw5MvxmAoGg",
    "zutsu7" to "8rOq_AqiNaw", "suwatta8" to "bzGMeDoGpeA", "ashi3cho" to "cs1A8W_HofI", "ashiIsho" to "8vftEiHldF8",
    "fukuraMassa" to "uy4loFazBgM", "fukura8" to "vVNi7jhGBpU", "ashi4" to "nkvn6zyYx08", "katai8st" to "B-vdrGt8hlA",
    "zenshinCho" to "NWl4iQSpkgw", "yoru12kai" to "9mCCZ39Gb5c", "ofuro20" to "JdPVMVfmdzc", "zenshin15" to "VDy2XlF9EBE",
    "senaka5" to "aSrdZ4aNRmg", "ofuro10" to "JIOnn1-NSHM", "asaBaki9" to "0wZ5nElZaRA", "asaGachi5" to "gMIlRS_lbYA",
    "jukusui9" to "09C7ti0xY4k", "ofuro6" to "WvnX_RsX_jY", "choyokin10" to "HCLVdX5esK0", "asa3" to "ZVSkWhJVlfk",
    "ibuki10" to "mRz5ZZAi9dU", "ogaRadio6" to "6jSlocilSYk", "asa10kesen" to "Jz7WdjFV5aw", "neochi10" to "TfkPz1DNK2Y",
)

// app-quiz.js:238-255 currentRx()の1:1移植(乱数不使用・rotationIndexのみで決定的な動画選出)。
// CardLottery.rotationIndex(既存・CardCoreで移植済み)を再利用するだけで、選出ロジック自体は
// ここで新規実装するが判定/安全ロジックではないため§3-2の対象外(表示用の推薦リスト生成)。
// TASK-C2-2026-08-06-build30-round8.md R-26(本人裁定): 「メイン(固定1本)+しあげ(ローテ1本)」の
// 計2本に減らす(旧仕様は「しあげ」枠が2本でメイン込み計3本になりうった)。ローテの決定的計算・
// 重複回避ロジック自体は現行踏襲(需要本数が2→1になるだけ)。
fun currentRx(typeKey: String, now: Instant): List<String> {
    val t = TYPE_RX_POOL[typeKey] ?: return emptyList()
    val need = 2 - t.rx.size
    if (need <= 0 || t.pool.isEmpty()) return t.rx.take(2)
    val r = jp.ogatore.kyouno.card.CardLottery.rotationIndex(now)
    val spacing = t.pool.size / need
    val picks = mutableListOf<String>()
    for (i in 0 until need) {
        var idx = (r + i * spacing) % t.pool.size
        var tries = 0
        while ((t.pool[idx] in t.rx || t.pool[idx] in picks) && tries < t.pool.size) {
            idx = (idx + 1) % t.pool.size
            tries++
        }
        picks.add(t.pool[idx])
    }
    return t.rx + picks
}

// TASK-C2-2026-07-29-soudan-video-card.md(H1): SoudanSheet.ktのsdTypeBoost相当が「そのタイプの
// rx+pool全件」を必要とするため公開する(TYPE_RX_POOL自体はfile-privateのまま。currentRx()と
// 同じ「非公開テーブルを公開関数越しに使わせる」形)。
fun typeRxPoolAllKeys(typeKey: String): List<String> {
    val t = TYPE_RX_POOL[typeKey] ?: return emptyList()
    return t.rx + t.pool
}

@Serializable
data class QuizTypeResult(val key: String, val worry: String?, val at: String)

// 全画面完全性監査タスク(TASK-C2-2026-07-26-full-completeness-audit.md #result): index.html:2976
// SOUDAN_TYPE_INTENT(タイプ→相談室プリセットintentId)の1:1移植。最初の1件のみ使う(Web版と同じ)。
private val SOUDAN_TYPE_INTENT = mapOf(
    "momo" to "zenkutsu", "koka" to "kokansetsu", "kenko" to "katakori",
    "ashi" to "ashikubi", "robot" to "zenshin", "yawara" to null,
)

// index.html <b>タグの簡易リッチテキスト化(app-quiz.js TYPES[].ptの太字表現)。判定・データ構造では
// なく表示専用の変換のためロジック層には置かない。
private fun annotatedBoldHtml(raw: String, boldColor: Color): AnnotatedString = buildAnnotatedString {
    var rest = raw
    while (true) {
        val start = rest.indexOf("<b>")
        if (start < 0) { append(rest); break }
        append(rest.substring(0, start))
        val afterOpen = rest.substring(start + 3)
        val end = afterOpen.indexOf("</b>")
        if (end < 0) { append(afterOpen); break }
        withStyle(SpanStyle(fontWeight = FontWeight.Black, color = boldColor)) { append(afterOpen.substring(0, end)) }
        rest = afterOpen.substring(end + 4)
    }
}

// app-quiz.js:145-153 activeQuestions()・194+ decideType呼び出し部分の1:1移植。判定そのものは
// QuizEngine.decideType(Step4で移植済み)を呼ぶだけで、ここでは一切再実装しない
// (マスタープラン§6 Step5c検収基準2)。presetWorryがあるときはQ5(worry)を出題しない。
// 全画面完全性監査タスク(TASK-C2-2026-07-26-full-completeness-audit.md #result): app-quiz.js:211
// REACH_FROM_MOMO(Q1の回答index→とどくメーター段位への対応表)の1:1移植。
private val REACH_FROM_MOMO = listOf(5, 4, 2, 1)

// Fable監査D5-1(alan5差し戻し2026-07-28): クイズは(オンボと違いLaunchedEffect(Unit)の
// 一発台本フローが無く)qi/scores/worry/pickedを直接読むだけの単純な画面のため、回転を
// またいでもそのまま安全に復元できる。scoresの値はInt・pickedの値はInt(score)かString
// (worryKey)かnullのため、それぞれ型タグ付きでBundle互換の入れ子リストへ平坦化する。
private fun encodeQuizPickedEntry(key: String, value: Any?): ArrayList<Any?> = when (value) {
    is Int -> arrayListOf(key, "int", value)
    is String -> arrayListOf(key, "string", value)
    else -> arrayListOf(key, "null", null)
}

@Suppress("UNCHECKED_CAST")
private fun decodeQuizPickedEntry(saved: List<Any?>): Pair<String, Any?> {
    val key = saved.getOrNull(0) as? String ?: ""
    val value: Any? = when (saved.getOrNull(1) as? String) {
        "int" -> saved.getOrNull(2) as? Int
        "string" -> saved.getOrNull(2) as? String
        else -> null
    }
    return key to value
}

internal val QuizScoresSaver: Saver<SnapshotStateMap<String, Int>, Any> = Saver(
    save = { map -> ArrayList(map.flatMap { (k, v) -> listOf(k, v) }) },
    restore = { saved ->
        @Suppress("UNCHECKED_CAST")
        val list = saved as List<Any?>
        mutableStateMapOf<String, Int>().apply {
            list.chunked(2).forEach { (k, v) -> put(k as String, v as Int) }
        }
    },
)

internal val QuizPickedSaver: Saver<SnapshotStateMap<String, Any?>, Any> = Saver(
    save = { map -> ArrayList(map.map { (k, v) -> encodeQuizPickedEntry(k, v) }) },
    restore = { saved ->
        @Suppress("UNCHECKED_CAST")
        val list = saved as List<List<Any?>>
        mutableStateMapOf<String, Any?>().apply {
            list.forEach { entry -> val (k, v) = decodeQuizPickedEntry(entry); put(k, v) }
        }
    },
)

// TASK-C2-2026-07-31-build11-renshu-journey.md D(本丸): 練習モード(かたさチェック開始〜初回
// 記録カード表示まで)5段の共通ラベル。QuizScreen/ResultScreenの両方から参照する。
// TASK-C2-2026-08-03-build18-tutorial-quality.md B-2: fdGuide中は動画サムネがno-op(Q-4)の
// ためタップされることがなく、「どうが」段が実際には体験されないままバーだけ進んで見えていた
// (本人指摘)。5段から「どうが」を外し4段にする。journeyIndex(この下)は必ず同時に直す
// (段の位置がズレる=alan5の警告どおり)。
// TASK-C2-2026-08-04-build19-tour-redesign.md T-3: ツアー独自の(番号のみの)進捗バーを廃止し、
// 体験ジャーニーバーの5段目「みどころ」を共用する(予告3枚+締めの間は常にカレント)。
// QuizScreen(currentIndex常に0固定)/ResultScreen(journeyIndex最大3=カード)は5段目を
// 参照しないため、添字の変更は不要。
val KYONO_JOURNEY_STEPS = listOf("チェック", "けっか", "きろく", "カード", "みどころ")

@Composable
fun QuizScreen(store: RecordStore, presetWorry: String?, onComplete: (typeKey: String, autoReachLv: Int?) -> Unit, onClose: () -> Unit) {
    val activeQuestions = remember(presetWorry) {
        if (presetWorry != null) QUIZ_QUESTIONS.filter { it.key != "worry" } else QUIZ_QUESTIONS
    }
    var qi by rememberSaveable { mutableStateOf(0) }
    val scores = rememberSaveable(saver = QuizScoresSaver) { mutableStateMapOf<String, Int>() }
    var worry by rememberSaveable { mutableStateOf(presetWorry) }
    // TASK-C2-2026-07-28-quiz-result-reach-parity.md §5: app-quiz.js:166 state.pickedの1:1移植。
    // 「まえの質問へ」で戻ったとき前回選んだ選択肢が分かるよう、質問key→選択値(scoreまたはworryKey)を覚えておく。
    val picked = rememberSaveable(saver = QuizPickedSaver) { mutableStateMapOf<String, Any?>() }
    // TASK-C2-2026-07-28-quiz-result-reach-parity.md §2: app-quiz.js:180の1:1移植。回答タップ直後に
    // 選択肢を無効化し、次の設問が描画されるまで二度押しで判定の入力が汚れるのを防ぐ(想定層は
    // ダブルタップの癖がある人が多いため)。
    var answering by remember { mutableStateOf(false) }
    LaunchedEffect(qi) { answering = false }
    // TASK-C2-2026-07-28-quiz-result-reach-parity.md §6(Android限定): app-quiz.js:156-158の
    // history.pushState設計(戻るで1問ずつ遡れる)の1:1移植。BackHandlerが1つも無く、ハードウェア/
    // ジェスチャーの「もどる」を押すと確認なしに回答が消えていた欠落。
    // TASK-C2-2026-07-31-build11-renshu-journey.md D: 練習モードジャーニーバーはfdGuide中
    // (はじめの1本ガイド・streakTotal==0)だけに出す。既存ユーザーの再チェックには一切出さない。
    val fdGuideActive = HomeLogic.fdActive(store.get("fd", null as String?), RecordLogic.loadStreak(store).total)
    // TASK-C2-2026-07-31-build11-renshu-journey.md C: qi==0でfdGuide中は何もしない(「ホームに
    // もどる」を削除し、練習モードの一貫ジャーニーとして出口を設けない設計に統一したため)。
    // 出荷前小修正(alan5 2026-07-31): fdGuide外(再チェック)ではqi==0でも「もどる」でonCloseへ
    // (「もう一回チェックする」→気が変わった→出られない、という閉じ込めの解消)。
    BackHandler(enabled = qi > 0 || !fdGuideActive) {
        if (qi > 0) qi-- else onClose()
    }

    val themeSetting = store.get("theme", "light")
    KyonoTheme(themeSetting, bigText = store.get("bigtext", true)) {
        val colors = LocalKyonoColors.current
        val dark = colors.bg == KyonoDarkColors.bg
        val q = activeQuestions.getOrNull(qi)
        // TASK-C2-2026-07-30-onboarding-scroll-and-copy.md A2: TourScreen(D6)と同じ構造。
        // 「まえの質問へ」「ホームにもどる」を本文と同じverticalScrollから外し、外側Columnの
        // 固定フッターにする。これでCTAは常に画面内の同じ位置にあり、本文の長さ(選択肢のnote文の
        // 折返し行数など)に関わらず動かない。
        Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(colors.bg)) {
        // D(本丸): 練習モードジャーニーバー。fdGuide中だけ画面上部に固定表示(verticalScrollの外)。
        // TASK-C2-2026-08-01-build13-round3.md ③⑦: 見出し「📖 使い方ツアー」をオンボチャットと
        // 同じ見た目でバーの上に常設する(既存のfdGuideActive条件=初回ジャーニー中のみを維持)。
        if (fdGuideActive) {
            Text(
                "使い方ツアー", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 20.dp).padding(top = 20.dp),
            )
            KyonoJourneyBar(labels = KYONO_JOURNEY_STEPS, currentIndex = 0)
        }
        // TASK-C2-2026-08-01-build13-round3.md ⑤: 設問切替時に前の設問のスクロール位置のまま
        // 新しい設問が描画され、旧選択肢と新選択肢が一瞬重なって見える不具合対策。TourScreenの
        // LaunchedEffect(si){scrollTo(0)}と同じ作法でqi変化時に必ず先頭へ戻す。
        val quizScrollState = rememberScrollState()
        LaunchedEffect(qi) { quizScrollState.scrollTo(0) }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(quizScrollState).padding(20.dp)) {
            Text("かたさチェック", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text("Q${qi + 1} / ${activeQuestions.size}", color = colors.sub, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("quizProgress"))
            // TASK-C2-2026-08-01-build15-subtraction9.md #6: 通常時(非fdGuide)は直上の「Qn/N」
            // テキストと9pxドット行が同じ進捗を二重表示していた(5視点監査指摘)ため、ドット行を
            // 削除(引き算)。fdGuide中はジャーニーバー(①チェック)が進捗を示すため、この画面の
            // ドットはfdGuide中ももとから非表示だった(元コード: TASK-C2-2026-07-28-quiz-result-
            // reach-parity.md §5・index.html:719 .dots+app-quiz.js:175-176の1:1移植)。
            if (q != null) {
                Spacer(Modifier.height(10.dp))
                Text(q.title, color = colors.ink, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("quizTitle"))
                Spacer(Modifier.height(4.dp))
                Text(q.note, color = colors.sub, fontSize = 13.sp)
                // TASK-C2-2026-07-28-quiz-result-reach-parity.md §7(Android限定): index.html:713-718
                // qArt→qtitle→qnote→tap-hint→optsの順序の1:1移植。以前はhint→写真の順で、
                // 👇が写真を指してしまい写真をタップしても反応がなく戸惑う欠落があった(iOS版は
                // 元から正しい順序)。写真/図解をヒントより先に描画するよう入れ替える。
                q.artRes?.let { res ->
                    Spacer(Modifier.height(10.dp))
                    Image(
                        painter = painterResource(id = res),
                        contentDescription = "${q.title}のお手本",
                        modifier = Modifier.fillMaxWidth().background(colors.bg, RoundedCornerShape(16.dp)).testTag("quizArt_${q.key}"),
                    )
                }
                // TASK-C2-2026-07-28-quiz-result-reach-parity.md §4: app-quiz.js:92-137
                // QUIZ_ART[2]/[3](kenko/ashi)の1:1移植。「あたま/あごの高さ線」「かかとの浮き」は
                // 判定基準そのものの可視化であり装飾ではない(以前は装飾と誤認して未移植だった)。
                when (q.key) {
                    "kenko" -> { Spacer(Modifier.height(10.dp)); QuizArtKenko(Modifier.testTag("quizArt_${q.key}")) }
                    "ashi" -> { Spacer(Modifier.height(10.dp)); QuizArtAshi(Modifier.testTag("quizArt_${q.key}")) }
                }
                // 全画面完全性監査タスク #quiz: index.html:717 .tap-hint(タップ誘導文言)の1:1移植。
                Spacer(Modifier.height(6.dp))
                Text("タップしてえらんでね", color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(10.dp))
                // index.html:293-309 .opt/.opt.g0〜g3(明→暗の段階色カード)の1:1移植。
                // TASK-C2-2026-07-28-quiz-result-reach-parity.md §5: app-quiz.js:168-169
                // 「段階色は数値スコアの設問(Q1-Q4)だけ」の1:1移植。Q5(worry)はscore==nullのため
                // 段階色を付けず、通常のカード色(colors.card/colors.line)にする。
                val palette = obgColors(dark)
                q.opts.forEachIndexed { i, opt ->
                    val c = if (opt.score != null) palette[i % palette.size] else null
                    val pickedVal: Any? = opt.score ?: opt.worryKey
                    // app-quiz.js:171 .opt.on(前回選んだ選択肢に枠色)の1:1移植。
                    val isPicked = picked[q.key] == pickedVal
                    // UI/UXパリティ監査GO-2(2026-07-28)・視点D確信度CONFIRMED: indication指定なしの
                    // 素の.clickableでCompose既定のripple(広がる波紋)にフォールバックしており、
                    // Webの「色/枠がパッと変わる」質感(index.html:295 .opt:active{background:
                    // var(--yellow-soft);border-color:var(--yellow)})とは別物だった欠落。
                    // KyonoPrimaryButtonと同じinteractionSource.collectIsPressedAsState()の手法を
                    // ここにも展開し、indication=nullでripple自体を止めてbackground/borderを
                    // 直接yellow-soft/yellowへ切り替える(遷移なし=CSS同様の瞬時切り替え)。
                    val optInteractionSource = remember { MutableInteractionSource() }
                    val optPressed by optInteractionSource.collectIsPressedAsState()
                    // TASK-C2-2026-08-05-build23-bg-tuning-and-tour-tap.md W-6: かたさチェック選択肢の
                    // 押下ハロー(相談室チップと同じ意図的実装)。matchParentSize()はBoxScope限定のため
                    // 外側にBoxを1枚かぶせる。
                    Box {
                    KyonoPressHaloBackground(pressed = optPressed, color = colors.teal)
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .background(if (optPressed) colors.yellowSoft else (c?.bg ?: colors.card), RoundedCornerShape(16.dp))
                            .border(
                                2.dp,
                                if (optPressed) colors.yellow else if (isPicked) colors.teal else (c?.border ?: colors.line),
                                RoundedCornerShape(16.dp),
                            )
                            // TASK-C2-2026-07-27-text-size-accessibility.md 項目4: 選択肢の見出し+
                            // 補足説明を1回のTalkBackスワイプで読める1つの単位にまとめる。
                            .semantics(mergeDescendants = true) {}
                            .clickable(interactionSource = optInteractionSource, indication = null, enabled = !answering) {
                                answering = true
                                picked[q.key] = pickedVal
                                opt.score?.let { scores[q.key] = it }
                                opt.worryKey?.let { worry = it }
                                qi++
                                if (qi >= activeQuestions.size) {
                                    val s = QuizScores(scores["momo"] ?: 0, scores["koka"] ?: 0, scores["kenko"] ?: 0, scores["ashi"] ?: 0)
                                    val typeKey = QuizEngine.decideType(s, worry, Instant.now())
                                    store.set("type", QuizTypeResult(typeKey, worry, RecordLogic.todayStr(Instant.now())))
                                    // app-quiz.js:223-234 finishQuiz()の自動転記(A案)の1:1移植: とどくメーターが
                                    // まだ1件も無ければ、Q1(momo)の回答を初回記録として自動で書きこむ
                                    // (ユーザーが自分で測った値があるときは上書きしない)。
                                    var autoReachLv: Int? = null
                                    if (RecordLogic.getReach(store).isEmpty()) {
                                        scores["momo"]?.let { idx -> REACH_FROM_MOMO.getOrNull(idx)?.let { autoReachLv = it } }
                                    }
                                    autoReachLv?.let { RecordLogic.setReach(store, it, Instant.now()) }
                                    onComplete(typeKey, autoReachLv)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("quizOpt_${q.key}_$i"),
                    ) {
                        // UI/UXパリティ監査2巡目A1(2026-07-29): カスタムフォントの行送り超過補正
                        // (KyonoTightLineTextStyle、前回G2は検索チップのみに適用)をクイズ選択肢にも展開。
                        // index.html:293-297 .opt(行送り指定なし=タイト)/.opt .crit{line-height:1.5}の1:1移植。
                        Text(
                            // UI/UXパリティ監査2巡目A4(2026-07-29): index.html:294 .opt{font-size:18px}
                            // の1:1移植。従来15spで-16.7%小さく値がズレていた欠落を修正する。
                            // TASK-C2-2026-08-04-build22-yellow-return.md Z-3(棚卸し対象): このQ1-Q4
                            // 段階色カードもobgColorsパレットを共有するため、同基準で文字も濃色化。
                            opt.label, color = c?.text ?: colors.ink, fontSize = 18.sp, lineHeight = 18.sp,
                            style = KyonoTightLineTextStyle, fontWeight = FontWeight.Black,
                        )
                        // TASK-C2-2026-08-05-build24-chip-clarity.md: 段階色カード(bgが高彩度化)では
                        // sub(#6E6B5F)がコントラスト不足(alan5実測2.45〜3.94:1)になるためinkにする。
                        // 通常カード(Q5 worry・c==null)はcolors.subのまま。
                        Text(
                            opt.note, color = if (c != null) colors.ink else colors.sub, fontSize = 13.sp, lineHeight = 19.5.sp,
                            style = KyonoTightLineTextStyle, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    }
                }
            }
        }
        // A2: 「まえの質問へ」は固定フッター(スクロールしない)。TASK-C2-2026-07-31-
        // build11-renshu-journey.md C: 「ホームにもどる」は削除(練習モードの一貫ジャーニーの
        // 一部として、出口を設けない設計に統一)。
        if (q != null && qi > 0) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
                // 全画面完全性監査タスク #quiz: index.html:720 #qBackBtn(Q1以外で表示・まえの質問へ戻る)の1:1移植。
                KyonoLineButton("← まえの質問へ", { qi-- }, Modifier.testTag("qBackBtn"))
            }
        }
        }
        // 出荷前小修正(alan5 2026-07-31): fdGuide外(再チェック)で入ったときだけ、途中離脱できる
        // ✕を出す。SoudanSheet.kt:464-473の✕(44dpタップ域+40dp円)と同じ見た目。fdGuide中
        // (初回練習)は前進のみのまま出さない。
        if (!fdGuideActive) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd).size(44.dp).clickable { onClose() }.testTag("quizCloseBtn")
                    .semantics { contentDescription = "とじる" },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.size(40.dp).background(colors.line, RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center,
                ) { Text("✕", color = colors.ink, fontWeight = FontWeight.Black) }
            }
        }
        }
    }
}

@Composable
// ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
// Phase 3: index.html:726-735 #result .card.grad-soft/.type-name/.type-copy/.type-hopeの1:1移植。
// 全画面完全性監査タスク(TASK-C2-2026-07-26-full-completeness-audit.md #result)で
// rPT/rReachNote/rPace/hint/rRecheckBtn/rSoudanLinkを追加。rxListは
// TASK-C2-2026-07-26-result-video-recommendations.mdで追加。rDoneNudge/rTourBtnは
// TASK-C2-2026-07-27-darkmode-recheck-and-nudges.mdで追加。
fun ResultScreen(
    store: RecordStore,
    typeKey: String,
    autoReachLv: Int?,
    showTourBtn: Boolean,
    openUrl: (String) -> Unit,
    onDone: () -> Unit,
    // TASK-C2-2026-07-27-scroll-parity-and-reduced-motion-gaps.md §C補足(alan5指摘): index.html:3991
    // rDoneNudgeBtn経由の1:1移植。既定はonDoneと同じ(呼び出し元が渡し忘れても壊れない)だが、
    // 呼び出し元(MainActivity)からはHome側のshowDoneNudgeも立てる版を渡してもらう。
    onDoneFromNudge: () -> Unit = onDone,
    onStartQuiz: () -> Unit,
    onOpenSoudan: (String?) -> Unit,
    onStartTour: () -> Unit,
) {
    // Fable監査GO-2(視点B): 結果画面にBackHandlerが無く、システム「もどる」が即アプリ終了
    // していた。既存の「ホームへ」導線と同じonDoneへ揃える。
    BackHandler(onBack = onDone)
    val info = QUIZ_TYPES[typeKey] ?: TypeInfo(typeKey, "", "", "", "")
    // 診断結果画面「おすすめ動画3本」欠落修正タスク(TASK-C2-2026-07-26-result-video-recommendations.md):
    // app-quiz.js:238-255 currentRx()の1:1移植呼び出し。日付のみで決まる(乱数不使用)。
    val rx = remember(typeKey) { currentRx(typeKey, Instant.now()) }
    val worry = remember { store.get<QuizTypeResult?>("type", null)?.worry }
    val catalogById = remember { CatalogLoader.shared.associateBy { it.id } }
    fun lookupVideo(key: String): CatalogVideo? = QUIZ_VIDEO_KEY_TO_ID[key]?.let { catalogById[it] }
    val today = remember { RecordLogic.todayStr(Instant.now()) }
    // ダークモード再確認+rDoneNudge/rTourBtn実装タスク(TASK-C2-2026-07-27-darkmode-recheck-and-
    // nudges.md): index.html:3958-3969 rDoneNudge用タップ検知(#result内のa.videoクリックで
    // pendingNudgeを立てる)+index.html:3970-4001 checkDoneNudge()の「結果画面表示中」分岐の1:1移植。
    // HomeScreenの既存の同種ロジック(pendingNudgeDate/showDoneNudge)とは独立させ、既存の
    // 壊れやすい仕組み(HANDOFF.md「複数日貼りつきバグ」既知箇所)には一切触れない。
    var pendingNudgeDate by remember { mutableStateOf<String?>(null) }
    var showDoneNudge by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val nowToday = RecordLogic.todayStr(Instant.now())
                val dates = RecordLogic.loadStreak(store).dates
                if (HomeLogic.shouldShowDoneNudge(pendingNudgeDate, nowToday, dates)) {
                    showDoneNudge = true
                }
                pendingNudgeDate = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val onVideoTap: (String) -> Unit = { url -> pendingNudgeDate = today; openUrl(url) }
    val fdGuideActive = remember { HomeLogic.fdActive(store.get("fd", null as String?), RecordLogic.loadStreak(store).total) }
    // ResultScreenはRecordStoreを従来受け取らなかったが、rSoudanLink表示条件(既存のSafetyKBLoader
    // 読み込み有無)には依存しない前提で常時表示にする(ネイティブはKBを起動時に同期読み込み済み)。
    // TASK-C2-2026-07-31-feedback-round2.md A-2/A-3: 診断結果画面(タイプカード)と練習ガイド
    // (「きょうはこの1本だけでOK！」)が地続きでモード切替が伝わらなかった件(A-2)と、YouTubeから
    // 戻ったあと「おかえりなさい」ブロックが画面外で気づけなかった件(A-3)。MainActivity.kt
    // 1111-1129(doneNudge)/1389-1397(doneBtnScale)と同じ、positionInRoot手計算+パルスの作法を流用。
    // TASK-C2-2026-07-31-build12-journey2-splash-emoji.md W1-a: 練習開始ポップ(showPracticePop)は
    // 削除(初回チャット画面に④点バーが出るようになり、結果画面での二重の「ここからは練習」案内が
    // 冗長になったため)。代わりに「動画タップまで」タイプカードを見せ続け、タップした瞬間に
    // どうが(③)へ進段させる。
    var videoTapped by remember { mutableStateOf(false) }
    val resultReducedMotion = rememberReducedMotion()
    val resultScrollState = rememberScrollState()
    var resultColumnPositionInRootY by remember { mutableStateOf(0f) }
    var resultViewportHeightPx by remember { mutableStateOf(0) }
    var doneNudgeCardPositionInRootY by remember { mutableStateOf(0f) }
    var doneNudgeCardHeightPx by remember { mutableStateOf(0) }
    val doneNudgeScale = remember { Animatable(1f) }
    LaunchedEffect(showDoneNudge) {
        if (showDoneNudge) {
            repeat(2) {
                doneNudgeScale.animateTo(1.045f, tween(350))
                doneNudgeScale.animateTo(1f, tween(350))
            }
        }
    }
    LaunchedEffect(showDoneNudge) {
        if (showDoneNudge) {
            delay(150)
            val contentY = doneNudgeCardPositionInRootY - resultColumnPositionInRootY + resultScrollState.value
            val target = (contentY - resultViewportHeightPx / 2f + doneNudgeCardHeightPx / 2f).toInt()
            if (resultReducedMotion) resultScrollState.scrollTo(target) else resultScrollState.animateScrollTo(target)
        }
    }
    // TASK-C2-2026-07-31-build11-renshu-journey.md D(本丸): fdGuide中は「おかえりなさい」の
    // 記録ボタンをその場(結果画面)で完結させる(ホームへ回り道させない)。MainActivity.ktの
    // wasGuide分岐(markDone→労い→confetti→カード入場→tourpend遷移)をこの画面専用に再現する。
    // 通常ユーザー(!fdGuideActive)の「おかえりなさい」は従来どおりonDoneFromNudge(ホームへ)を使う。
    var cardResult by remember { mutableStateOf<TodayCardResult?>(null) }
    var confettiTrigger by remember { mutableStateOf<Int?>(null) }
    // TASK-C2-2026-08-05-build23-bg-tuning-and-tour-tap.md W-2: 1本目だけYouTube往復の練習に
    // 使えるようにする。案内を一拍見せてからopenUrlする間の再タップ二重発火を防ぐガード。
    var youtubeNoticeVisible by remember { mutableStateOf(false) }
    val resultContext = androidx.compose.ui.platform.LocalContext.current
    val resultScope = androidx.compose.runtime.rememberCoroutineScope()
    // D: 練習モードジャーニーバーの現在地(0-based)。①チェックはQuizScreenが担当するため
    // ここでは②〜④(index 1〜3)のみ動く。
    // TASK-C2-2026-08-03-build18-tutorial-quality.md B-2: KYONO_JOURNEY_STEPS側で「どうが」を
    // 外し4段にしたのに合わせ添字を詰める(旧: けっか1・どうが2・きろく3・カード4 →
    // 新: けっか1・きろく2・カード3)。videoTapped/showDoneNudgeはどちらも
    // 「けっかの次=きろく」段に該当するため同じ2にまとめる。
    val journeyIndex = when {
        cardResult != null -> 3
        showDoneNudge || videoTapped -> 2
        else -> 1
    }
    val resultHaptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    // MainActivity.kt:1400-1480のwasGuide分岐だけを抜き出した版(日1目は必ずこの分岐を通る。
    // 節目/通常cheerの分岐はfdGuide初日には到達しないため移植不要)。
    // TASK-C2-2026-08-05-build28-round6.md R-18(本人動画指摘・裁定GO): この関数はfdGuideActive時
    // にしか呼ばれない(呼び出し元2箇所とも`fdGuideActive`ガード済み。通常ユーザーの記録演出=
    // 労い→700ms→カードはMainActivity側の別ロジックで別途担当・ここには一切触れていない)ため、
    // ツアー中は労い演出(旧fdCelebrationVisible「1日目クリア！ナイスご自愛！」)と700msの
    // 待ち時間を省き、即カードモーダルを表示する。旧実装ではカードダイアログの出現アニメーションが
    // 完了するまでの間、背後の労いテキスト(0日目カードと矛盾する「1日目クリア！」)が透けて
    // 見えていた。
    fun performPracticeRecord() {
        resultHaptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        RecordLogic.markDone(store, Instant.now())
        val streak = RecordLogic.loadStreak(store)
        resultScope.launch { jp.ogatore.kyouno.widget.WidgetUpdater.notifyRecorded(resultContext) }
        // 練習モードは「きょうはこれ1本でOK！」で示した動画がそのまま今日の1本なので、
        // MainActivity側のtodayVideoIdAndTitle()より確実に特定できる。
        rx.firstOrNull()?.let { vk -> lookupVideo(vk)?.let { v -> RecordLogic.recordDaylog(store, today, v.id, v.t, streak.count) } }
        store.set("fd", "1")
        store.set("tourpend", true)
        // TASK-C2-2026-08-05-build27-round5.md R-13(本人指示「この画面は0日って表示させて。
        // テストだから」): このComposable自体がfdGuide中の練習専用(通常ユーザーはMainActivity側の
        // renderTodayCard呼び出しを使う)なので、大数字表示だけ常に0にする。markDone/
        // recordDaylogは通常どおり実行済みで実カウントには一切影響しない(表示だけの変更)。
        val newCard = renderTodayCard(store, streak, today, resultContext, displayTotalOverride = 0)
        cardResult = newCard
        confettiTrigger = (confettiTrigger ?: 0) + 1
    }
    // D(本丸): 練習モードジャーニーバー。fdGuide中だけ画面上部に固定表示(verticalScrollの外)。
    // バーの実測高さぶん本文側にtop paddingを入れて重なりを避ける(Box内でColumnの兄弟として
    // 置く方式。バーをColumnの子にするとAnimatedVisibility呼び出しがColumnScope拡張版と衝突する
    // ため、あえてBox直下の兄弟構成にする)。
    var journeyBarHeightPx by remember { mutableStateOf(0) }
    // TASK-C2-2026-08-03-build17-hotfix-result-theme.md: themeSettingを"auto"に固定していたため、
    // アプリ内テーマ(kyono_theme)が「明るい」でもシステム側がダークだと結果画面だけダーク描画
    // されていた欠陥。build16まではデフォルト値も"auto"だったため気づかれなかったが、build17の
    // P-3(デフォルトを"light"へ変更)により、他画面(QuizScreen/TourScreen等)は正しく追従する
    // 一方ResultScreenだけ食い違うようになった(alan5実機報告)。他画面と同じ
    // store.get("theme", "light")に揃える。
    KyonoTheme(store.get("theme", "light"), bigText = store.get("bigtext", true)) {
        val colors = LocalKyonoColors.current
        Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.bg)
                .onGloballyPositioned { coords ->
                    resultColumnPositionInRootY = coords.positionInRoot().y
                    resultViewportHeightPx = coords.size.height
                }
                .padding(top = with(LocalDensity.current) { journeyBarHeightPx.toDp() })
                .verticalScroll(resultScrollState)
                .padding(20.dp),
        ) {
            // TASK-C2-2026-08-02-build17-feedback-fixes.md Q-1: ガイド中(fdGuideActive)だけ
            // 「タイプ+①」に削ぎ落としていた結果画面を廃止し、通常のかたさチェックと同じ
            // フル版(タイプカード+解説+動画3本+ペース目安+相談室リンク)を常に表示する
            // (「一度正確な自分の結果がきちんと出る」という本人の狙いどおり)。
            KyonoGradientCard(KyonoGradient.Soft, Modifier.testTag("resultCard")) {
                // TASK-C2-2026-08-04-build20-addendum.md A-3(最小セット置換)。
                Text(
                    "${kyonoDisplayName(store)}のかたさタイプは…", color = colors.sub, fontSize = 14.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                // index.html:317-318,729 .type-illust(104x104・中央寄せ)の1:1移植。
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    KyonoTypeArt(typeKey, Modifier.testTag("resultTypeArt"))
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    info.name, color = colors.ink, fontSize = 29.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.fillMaxWidth().testTag("resultTypeName"),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    info.copy, color = colors.sub, fontSize = 15.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().background(colors.yellowSoft, RoundedCornerShape(14.dp)).padding(14.dp)) {
                    Text("" + info.hope, color = colors.ink, fontSize = 15.sp)
                }
                // 全画面完全性監査タスク #result: index.html:733 #rPT(理学療法士のひとくち解説)の1:1移植。
                Spacer(Modifier.height(12.dp))
                Column {
                    Text("理学療法士のひとくち解説", color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        annotatedBoldHtml(info.pt, colors.ink), color = colors.sub, fontSize = 14.sp, lineHeight = 21.sp,
                        modifier = Modifier.testTag("resultPT"),
                    )
                }
                // TASK-C2-2026-08-01-build13-round3.md ④: 「とどくメーターにも記録したよ」の
                // 表示行を削除(自動転記=setReach(lv, silent=true)自体は既存どおり継続、
                // 表示だけを消す。alan5指摘: 結果画面が説明過多だった)。
            }
            Spacer(Modifier.height(16.dp))
            // 診断結果画面「おすすめ動画3本」欠落修正タスク(TASK-C2-2026-07-26-result-video-recommendations.md):
            // index.html:736-744 rxHead/rxList/worryExtra/rRotateNoteの1:1移植。
            // Q-1: ガイド中専用の「①だけ練習」カードは廃止し、常にこの通常版を表示する。
            // TASK-C2-2026-08-06-build30-round8.md R-26(本人指定見出し): 「おすすめの3本:〜」を
            // 「あなたへのおすすめ再生リスト」に差し替え、タイプ別のサブ文(まずは「〜」から！
            // 2週間続けてみて)は独立したサブ行として残す(仮案・本人が後日赤ペンする前提)。
            KyonoCard {
                Text(
                    "あなたへのおすすめ再生リスト", color = colors.ink,
                    fontSize = 15.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("rxHead"),
                )
                Text(
                    "まずは「${info.area}」から！2週間続けてみて", color = colors.sub,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp),
                )
                // TASK-C2-2026-08-05-build25-tour-round3.md R-4(本人生指摘・本人校正済み文言):
                // ツアー中(fdGuideActive)だけ、見出しと1本目カードの間にR-2と同じ視覚言語の
                // 練習ピル+案内1行を挟む。タップ時notice・復帰フローには一切触れない。
                if (fdGuideActive) {
                    Spacer(Modifier.height(10.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        // TASK-C2-2026-08-05-build26-round4.md R-7(本人モック確認済み・
                        // mock-pink-highlight-v3.png/pill-float-preview.gifが見た目の正解):
                        // ピンク化+16sp拡大+ふわふわ(±4dp・周期1.6s・easeInOut・reduceMotion時静止)。
                        val pillOffsetY = if (!rememberReducedMotion()) {
                            val pillFloat = rememberInfiniteTransition(label = "r7PillFloat")
                            val v by pillFloat.animateFloat(
                                initialValue = 4f, targetValue = -4f,
                                animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                label = "r7PillFloatY",
                            )
                            v
                        } else 0f
                        Text(
                            "＼ 動画をひらく練習 ／", color = colors.pinkInk, fontSize = 16.sp, fontWeight = FontWeight.Black,
                            modifier = Modifier
                                .offset(y = pillOffsetY.dp)
                                .background(colors.pinkSoft, RoundedCornerShape(percent = 50))
                                .padding(horizontal = 18.dp, vertical = 6.dp),
                        )
                        // R-7: 案内行を自動折返しに任せず明示的に2行にする。
                        Text(
                            "今は1本目だけタップできるよ！\n動画をひらいてもどってきてね！", color = colors.sub, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                // TASK-C2-2026-08-02-build17-feedback-fixes.md Q-4: ガイド中(fdGuideActive)は
                // 動画サムネをタップ不可のままにする(本人裁定・離脱回避)。onVideoTapを差し替えず
                // no-opにする(見た目はQ-1どおり通常の3本リストのまま・タップだけ無効化)。
                // TASK-C2-2026-08-05-build23-bg-tuning-and-tour-tap.md W-2(本人発案・YouTube往復の
                // 練習): build17 Q-4を部分改訂し、1本目だけはタップ可にする(2・3本目は従来どおり
                // no-op+減光)。タップ時は一拍だけ案内を見せてからonVideoTap(既存のpendingNudgeDate
                // 記録込み)を呼ぶ。performPracticeRecordやtourpend配線には一切触れない
                // (既存のON_RESUME復帰検知→showDoneNudge→「1日目の記録をつけにいく」ボタンの
                // 練習合流フローをそのまま再利用するだけ)。
                val videoTapHandler: (String) -> Unit = if (fdGuideActive) { {} } else onVideoTap
                val firstVideoTapHandler: (String) -> Unit = { url ->
                    if (!youtubeNoticeVisible) {
                        youtubeNoticeVisible = true
                        resultScope.launch {
                            delay(900)
                            youtubeNoticeVisible = false
                            onVideoTap(url)
                        }
                    }
                }
                // TASK-C2-2026-08-06-build30-round8.md R-26(本人裁定・カードUIで確定):
                // 「メイン+しあげ+おまけ」最大3本を1つのリストに統合する。旧仕様の「カード外の
                // ＋もう1本」枠は廃止。おまけは悩み選択(肩こり/腰痛/疲れ)がある人だけ、かつ
                // メイン/しあげと重複しないときだけ3本目としてリストに含める(重複時は現行の
                // 重複ガードのとおり2本のまま)。
                // index.html:81-85,327-328 WORRY[saved.worry]の1:1移植(重複ガード踏襲)。
                val worryExtra = worry?.let { WORRY_EXTRA[it] }?.takeIf { it.v !in rx }
                // TASK build32 R-44(本人指示・2026-08-06): ①メイン/②しあげの番号ラベルを
                // やめ、ホームのあなた用(R-33)と同じ役割表記に統一する(ツアー内も同じ画面)。
                val badges = listOf("メインの一本", "余裕があったら追加の一本")
                val displayItems = buildList {
                    rx.forEachIndexed { i, vk -> add(vk to (badges.getOrNull(i) ?: "")) }
                    worryExtra?.let { add(it.v to "おまけ: ${it.label}") }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.testTag("rxList")) {
                    displayItems.forEachIndexed { i, (vk, badge) ->
                        // TASK-C2-2026-08-03-build18-tutorial-quality.md B-7: no-op裁定は維持した
                        // まま、見た目でも押せないことを明示する。
                        val isFirst = fdGuideActive && i == 0
                        lookupVideo(vk)?.let { v ->
                            VideoRow(
                                v, if (isFirst) firstVideoTapHandler else videoTapHandler,
                                badge = badge,
                                // TASK-C2-2026-08-05-build26-round4.md R-7: ツアー中の1本目カードを
                                // 既存のhero強調枠(pink 2.5dp+pinkSoft地)で目立たせる。
                                hero = isFirst,
                                disabledLook = fdGuideActive && !isFirst,
                                useShortTitle = true,
                            )
                        }
                    }
                }
                if (fdGuideActive && youtubeNoticeVisible) {
                    Text(
                        "YouTubeがひらくよ。見おわったら〈きょうのオガトレ〉にもどってきてね",
                        color = colors.tealInk, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    )
                }
                if (displayItems.isNotEmpty() && !fdGuideActive) {
                    Spacer(Modifier.height(8.dp))
                    KyonoGhostButton(
                        "▶ ${displayItems.size}本続けて再生する",
                        { openUrl("https://www.youtube.com/watch_videos?video_ids=" + displayItems.mapNotNull { QUIZ_VIDEO_KEY_TO_ID[it.first] }.joinToString(",")) },
                        Modifier.testTag("rxPlayAllBtn"),
                    )
                }
                // index.html:740 #rRotateNoteの1:1移植。
                Spacer(Modifier.height(4.dp))
                // GO-G2(5視点ワンループ): index.html:740 .rotate-note{color:var(--sub)}の1:1移植。
                // subFaintは実測コントラスト不足(3.87:1)で、Web版でもここはvar(--sub)であり
                // 元々subFaintの用途ではなかった(subFaintの正しい用途はオガトレ通信の
                // 30日超の古い投稿日付のみ・index.html:277-278)。
                // TASK-C2-2026-08-06-build30-round8.md R-27(本人裁定): 実装は毎日ローテのまま
                // (CardLottery.rotationIndex不変)。表記だけ「3日ごと」→「毎日」に修正。
                Text(
                    "おすすめは毎日自動で入れ替わります", color = colors.sub, fontSize = 12.sp,
                    modifier = Modifier.testTag("rRotateNote"),
                )
            }
            // TASK-C2-2026-08-02-build17-feedback-fixes.md Q-1: rPace/rSoudanLinkもガイド中だけ
            // 隠していたが、フル版統一のため常に表示する。
            // 全画面完全性監査タスク #result: index.html:741-742 #rPace/hint(ペースの目安・免責注意書き)の1:1移植。
            // TASK-C2-2026-08-05-build27-round5.md R-12(本人赤ペン指摘): ツアー中はこのカード
            // 丸ごと非表示にする(Spacerごと隠して余白も残さない)。通常の結果画面(!fdGuideActive)では
            // 従来どおり表示。
            if (!fdGuideActive) {
            Spacer(Modifier.height(16.dp))
            KyonoCard {
                Text("ペースの目安", color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(6.dp))
                Text(
                    "・毎日が理想！週3でも効きます\n・1日1回で十分\n・痛い日は休むのが正解\n・痛みは「イタ気持ちいい」まで",
                    color = colors.sub, fontSize = 14.sp, lineHeight = 24.sp,
                )
                Spacer(Modifier.height(8.dp))
                // GO-G2: index.html:742 .hint{color:var(--sub)}の1:1移植。
                Text(
                    "※効果には個人差があります 痛みが強いときは中止して医療機関へ",
                    color = colors.sub, fontSize = 12.sp,
                )
                // 全画面完全性監査タスク #result: index.html:743 #rSoudanLink(タイプ別の相談室逆導線)の1:1移植。
                SOUDAN_TYPE_INTENT[typeKey]?.let { intentId ->
                    Spacer(Modifier.height(10.dp))
                    // GO-G3(5視点ワンループ): 最小タップ領域44pt/48dpの確保(見た目は変えず当たり判定のみ拡張)。
                    // UX13案・案8(2026-07-30): ボタン用途の残存絵文字をCanvasアイコンへ(SoudanBubble)。
                    Row(
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { onOpenSoudan(intentId) }.padding(vertical = 12.dp).testTag("resultSoudanLink"),
                    ) {
                        KyonoIconGlyph(KyonoIcon.SoudanBubble, fill = Color.Transparent, accent = colors.tealInk, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("この悩み、相談室で聞いてみる", color = colors.tealInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            }
            // TASK-C2-2026-08-02-build17-feedback-fixes.md Q-3: 結果表示と同時/直後に出していた
            // 「練習モード」ポップアップ的な専用ブロック(旧「きょうは練習してみよう」カード)を廃止し、
            // 静かな一行+ボタンに差し替える。読み終わったら自分のタイミングで進む設計。
            // TASK-C2-2026-08-03-build18-tutorial-quality.md B-6: 文言をalan5指定どおりに変更
            // (練習ボタンを本番と同じ「きょうやった！」に)。B-1: タップと同時にこのブロック
            // 自体を消し、カードダイアログ出現までの700ms間、背後にボタンが残って見えることを
            // 防ぐ(videoTappedをそのまま表示条件に使う)。B-8: QuizScreenのansweringガードと
            // 同じ考え方で、videoTapped自体を「既に処理済みか」の判定にも使い、ダイアログ出現
            // までの700ms間の再タップでperformPracticeRecordが二重発火しないようにする。
            // TASK-C2-2026-08-05-build26-round4.md R-6(本人赤ペン指摘): 動画タップ→YouTube→
            // アプリ復帰の経路ではvideoTapped=trueにならないため、復帰カード(showDoneNudge)と
            // この練習ブロックが同時に表示され「記録の入り口が二重」になっていた過去の設計。
            // TASK-C2-2026-08-06-build30-round8.md R-24(本人指示): ツアー中は上の「動画をひらく
            // 練習」ピルが主役のため、この「きょうやった！」ボタン自体をツアー中は表示しない
            // (R-19で文字を削って残った最後のボタンをここで削除)。videoTapped経由の記録は
            // 動画サムネをタップ→復帰→showDoneNudgeカードの「1日目の記録をつけにいく」から
            // 引き続き行える(performPracticeRecordの到達経路が1本に絞られるだけ)。
            // 通常時(!fdGuideActive)のけっか画面はそもそもこのブロックの対象外(現状維持)。
            // ダークモード再確認+rDoneNudge/rTourBtn実装タスク: index.html:745 #rDoneNudgeの1:1移植。
            // はじめの1本ガイド中、結果画面を表示したまま動画を見に行って戻ってきたときに、
            // ホームのcheerの代わりに結果画面内へ「やった？」の復帰案内を出す。
            if (showDoneNudge && cardResult == null) {
                Spacer(Modifier.height(16.dp))
                // A-3: HomeScreen(MainActivity.kt:1111-1129/1389-1397)と同じパルス+スクロール作法。
                KyonoCard(
                    Modifier.testTag("rDoneNudge").onGloballyPositioned { coords ->
                        doneNudgeCardPositionInRootY = coords.positionInRoot().y
                        doneNudgeCardHeightPx = coords.size.height
                    },
                ) {
                    // TASK-C2-2026-08-05-build23-bg-tuning-and-tour-tap.md W-2: ツアー中(YouTube往復の
                    // 練習)はこの復帰カードの一言を短く「おかえり！」にする(本人指定の文言)。
                    // 通常ユーザーの復帰ナッジは従来どおりの文言を維持。
                    Text(if (fdGuideActive) "おかえり！" else "おかえりなさい！ ストレッチできた？", color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(10.dp))
                    // D(本丸): fdGuide中はその場(結果画面)で記録を完結させる。ホームへは飛ばさない。
                    KyonoPrimaryButton(
                        if (fdGuideActive) "1日目の記録をつけにいく" else "きょうの記録をつけにいく",
                        if (fdGuideActive) { { performPracticeRecord() } } else onDoneFromNudge,
                        Modifier.testTag("rDoneNudgeBtn").scale(doneNudgeScale.value),
                    )
                }
            }
            // TASK-C2-2026-08-05-build28-round6.md R-18: 旧「1日目クリア！ナイスご自愛！」の
            // 労いカード(fdCelebrationVisible)は削除。performPracticeRecordはfdGuideActive時に
            // しか呼ばれず、ツアー中はこの労い演出自体を出さない裁定になったため(詳細は
            // performPracticeRecordのコメント参照)。
            Spacer(Modifier.height(16.dp))
            // index.html:746 #rTourBtn(オンボ→クイズ経由・ツアー未見のときだけ)の1:1移植。
            if (showTourBtn) {
                KyonoGhostButton("つづき：使い方ツアーへ", onStartTour, Modifier.testTag("rTourBtn"))
                Spacer(Modifier.height(10.dp))
            }
            // TASK-C2-2026-07-28-quiz-result-reach-parity.md §1: rGoHomeBtn/rRecheckBtnもガイド中は
            // 隠す(app-quiz.js:291-299の1:1移植。一本道=①をタップ→もどる→記録、以外の分岐を見せない。
            // タブバーからの脱出は常に可能なため、隠しても迷子にはならない)。
            if (!fdGuideActive) {
                KyonoPrimaryButton("きょうの1本へ", onDone, Modifier.testTag("resultDoneBtn"))
                // TASK-C2-2026-08-01-build15-subtraction9.md #1: 「もう一回チェックする」は
                // ホームのckCard(再チェック導線)と完全重複のため削除(5視点監査③④で独立に
                // 指摘・本人GO)。通常時(非ガイド)でも出さない。
            }
        }
        // D: MainActivity.kt:1764-1771のKyonoConfettiと同じ作法(結果画面版)。
        if (confettiTrigger != null) {
            key(confettiTrigger) {
                KyonoConfetti(count = 70, modifier = Modifier.matchParentSize())
            }
        }
        // TASK-C2-2026-08-01-build13-round3.md ③⑦: 見出し「📖 使い方ツアー」をオンボチャットと
        // 同じ見た目でバーの上に常設する(既存のfdGuideActive条件=初回ジャーニー中のみを維持)。
        // 見出し+バーをColumnでまとめ、その合算高さをjourneyBarHeightPxとして計測する
        // (本文側のtop paddingが見出し分も含めて重なりを避けられるようにするため)。
        if (fdGuideActive) {
            Column(
                modifier = Modifier.align(Alignment.TopCenter).onGloballyPositioned { coords ->
                    journeyBarHeightPx = coords.size.height
                },
            ) {
                Text(
                    "使い方ツアー", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 20.dp).padding(top = 20.dp),
                )
                KyonoJourneyBar(labels = KYONO_JOURNEY_STEPS, currentIndex = journeyIndex)
            }
        }
        }
        // D: MainActivity.kt:1774-1854のカードダイアログと同じ作法(結果画面版・節目分岐は日1目には
        // 到達しないため省略)。
        cardResult?.let { result ->
            val onCardClose = {
                cardResult = null
                // TASK-C2-2026-08-05-build28-round6.md R-18(本人動画指摘・裁定GO): showDoneNudgeを
                // 立てたままにしておくと、カードを閉じてからstep5(ツアー)へ遷移するまでの約350msの
                // 間、済んだはずの「おかえり！／1日目の記録をつけにいく」画面が一瞬出戻って見えて
                // いた。このカード表示フロー自体がfdGuideActive時にしか到達しないため、常に
                // リセットしてよい。
                showDoneNudge = false
                tryStartTour(store, resultScope) { onStartTour() }
            }
            AlertDialog(
                onDismissRequest = onCardClose,
                confirmButton = {
                    Button(onClick = onCardClose, modifier = Modifier.testTag("cardCloseBtn")) { Text("とじる") }
                },
                dismissButton = {
                    Button(
                        onClick = { ShareImage.shareBitmap(resultContext, result.bitmap, "kyono-ogatore-$today.png", "#きょうのオガトレ 1日目！") },
                        modifier = Modifier.testTag("cardShareBtn"),
                    ) { Text("保存・シェアする") }
                },
                text = {
                    KyonoInstantDialogAnimations()
                    Column {
                        Image(
                            bitmap = result.bitmap.asImageBitmap(),
                            contentDescription = "記録カード",
                            modifier = Modifier.fillMaxWidth().testTag("cardImage"),
                        )
                        // TASK-C2-2026-08-05-build27-round5.md R-13(本人指示・一字一句このまま):
                        // 「自分用に画像を保存したり SNSでシェアしたりしてね！」をシェアボタン付近に
                        // 追加。このダイアログはfdGuide練習カード専用(通常ユーザーはMainActivity側の
                        // 別ダイアログを使う)ため、ツアー中限定の条件分岐は不要。
                        Text(
                            "自分用に画像を保存したり SNSでシェアしたりしてね！",
                            color = colors.sub, fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                },
            )
        }
    }
}

// TASK-C2-2026-08-04-build20-home-cards-and-tour-tiers.md T-B: KyonoTourMockupを位置(index)では
// なくmock(意味のある固定キー)でswitchする。スライド配列の並べ替えで絵がズレる心配が構造的に
// 無くなる(build19 T-1の再発防止)。
enum class TourMockKind { MAP, VIDEO_DAILY, TODAY_DONE, CARD_DEX, SOUDAN, OBU, MY_RECORD }

data class TourSlideDef(val title: String, val desc: String, val mock: TourMockKind)

// TASK-C2-2026-08-04-build20-home-cards-and-tour-tiers.md T-A/T-B: 「体験一本道＋予告3枚」
// (build19 T-2)をさらに2段構えにする。共通プール7枚から、初回は「地図+まだ見ていない3枚」、
// 再生(使い方タブ)は「地図+全7枚のフルマニュアル」を切り出す(スライド配列の共通プール+
// 初回サブセット方式)。
val OB_TOUR_POOL = listOf(
    // T-A(alan5指定文言・このまま): 初回1枚目に追加する「1日の流れ」地図。
    TourSlideDef(
        "まいにちやることは1つだけ",
        "ホームの「きょうの1本」をみる→おわったら「きょうやった！」をおす\nこれだけで記録カードがたまっていくよ\nつぎの3枚は「こまったとき」の場所あんないだよ",
        TourMockKind.MAP,
    ),
    // T-B「復活3枚」(alan5指定文言・build18までと同一・このまま)。再生の7枚版にのみ含める。
    TourSlideDef("まいにち1本、動画をやる", "ホームの「きょうの1本」をタップ→YouTubeがひらくよ\n見おわったらこのアプリにもどってきてね", TourMockKind.VIDEO_DAILY),
    TourSlideDef("おわったら「きょうやった！」", "アプリにもどったらこのボタンを押すだけ\n連続と通算がのびるよ", TourMockKind.TODAY_DONE),
    TourSlideDef("ためると図鑑がうまる", "記録カードは記念日・季節・レアなど何種類もあるよ\n「保存・シェアする」で写真にのこせて SNSやコメント欄にもどうぞ\n毎日の記録でカード図鑑がすこしずつうまっていく（マイ記録→お楽しみ機能）", TourMockKind.CARD_DEX),
    // build19 T-2の「予告3枚」(文言は変更なし)。初回サブセットにも含まれる。
    TourSlideDef("悩みは相談室で質問", "右下のボタンをタップ→「肩こり」のように打つか、チップを選ぶだけ\nオガトレ監修の答えとおすすめ動画がすぐ届くよ", TourMockKind.SOUDAN),
    // TASK-C2-2026-08-02-build17-feedback-fixes.md P-2: 「尾形さん」→「尾形」(本人指示・改行と同時)。
    TourSlideDef("オガトレ通信をのぞく", "尾形からのお知らせが届くよ\nホームいちばん上の「きょうのひとこと」も毎日かわります", TourMockKind.OBU),
    // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §6: ...(既存コメント維持)...
    TourSlideDef("マイ記録でふりかえる", "やった日に印がつくカレンダーがあるよ（×はつかないよ）\n毎日の合図（通知）はマイ設定からいつでも入れられるよ", TourMockKind.MY_RECORD),
)
// T-A: 初回は「地図(0)+予告3枚(4,5,6)」の4枚。「もう体験したことの再説明」(videoDaily/
// todayDone/cardDex)は初回では引き続き省く(build19 T-2の判断を継承)。
private val OB_TOUR_FIRST_RUN_INDICES = listOf(0, 4, 5, 6)

// T-A/T-B: isFirstRun(初回=tryStartTour/オンボ直後・showClosing:trueの経路とオンボ埋め込み
// 経路の両方)は「地図+予告3枚」の4枚、再生(使い方タブ onReenterTour・isFirstRun:false)は
// プール全7枚のフルマニュアルを返す。
fun obTourSlides(isFirstRun: Boolean): List<TourSlideDef> =
    if (isFirstRun) OB_TOUR_FIRST_RUN_INDICES.map { OB_TOUR_POOL[it] } else OB_TOUR_POOL
const val OB_TOUR_CLOSING_TITLE = "これで準備ばっちり！"
// TASK-C2-2026-08-04-build19-tour-redesign.md T-2(alan5指定文言・このまま): 削除した「忘れても
// だいじょうぶ」枚の内容をこの締めスライドに吸収する。
const val OB_TOUR_CLOSING_DESC = "あしたも待ってるね\nきょうのぶんの動画は ホームの「きょうの1本」からどうぞ\n困ったら使い方タブの「使い方ツアー」でいつでも読み返せるよ"

// index.html:4283-4347 fdTourMaybeStart/obTourStep/obTourEndの1:1移植。3枚(T-2で7枚から予告3枚+
// 締めへ再構成)+条件付き4枚目(closing・自動起動時のみ)。スワイプカルーセルでなく「つぎへ」
// ボタン+ドット進捗のリニアなステップ形式
// (index.html:4297-4324と同じ構造)。T-3以降、進捗バーはツアー独自の点表示ではなく体験
// ジャーニーバー(KYONO_JOURNEY_STEPS)の5段目「みどころ」を共用する。
// ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
// Phase 3: index.html:172-176,313-315 .obt-t/.obt-d/.dots/.dot/.dot.onの1:1移植。
// TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §6: 以前はRecordStoreを受け取らず
// テーマ・文字サイズを"auto"/trueに固定していたため、手動でライト設定にしている本人が
// 夜に再訪するとツアー画面だけダークになる不具合があった。他画面と同じくstoreの実設定を使う。
@Composable
fun TourScreen(store: RecordStore, showClosing: Boolean, isFirstRun: Boolean = false, onDone: () -> Unit) {
    var si by remember { mutableStateOf(0) }
    val slides = remember(isFirstRun) { obTourSlides(isFirstRun) }
    val totalSlides = slides.size + if (showClosing) 1 else 0
    KyonoTheme(store.get("theme", "light"), bigText = store.get("bigtext", true)) {
        val colors = LocalKyonoColors.current
        // TestFlight実機フィードバックD6(2026-07-29、iOS向けだがAndroidにも同じ穴があった):
        // ステップごとに本文の量が違い、その下に置かれた「つぎへ」の位置が毎回上下に動いていた。
        // index.html:524 #obLog{flex:1;max-height:52vh;overflow-y:auto}/528 #obChips(ボタン専用の
        // 別要素)の1:1移植で、内容とボタン列を別のColumnに分ける。ボタン列を外側のColumnに出し
        // 内容側だけweight(1f)+verticalScrollにすることで、内容の長さに関わらずボタンの位置が
        // 1dpも動かなくなる(あふれるステップだけ中でスクロール)。
        val scrollState = rememberScrollState()
        LaunchedEffect(si) { scrollState.scrollTo(0) } // index.html:4308 log.scrollTop=0の1:1移植
        Column(Modifier.fillMaxSize().background(colors.bg)) {
        // TASK-C2-2026-07-31-build11-renshu-journey.md D(本丸): 練習モードと同じKyonoJourneyBarに
        // ドット表示を置き換え、画面上部に固定する(本人の明示要求=デザインの一貫性)。ラベルは
        // 番号だけで十分なため空文字列にする(circle内の数字/✓で進捗は伝わる)。
        // TASK-C2-2026-08-01-build13-round3.md ③⑦: 見出し「📖 使い方ツアー」を初回ジャーニー
        // (isFirstRun)のときだけオンボチャットと同じ見た目でバーの上に常設する。
        if (isFirstRun) {
            Text(
                "使い方ツアー", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 20.dp).padding(top = 20.dp),
            )
        }
        // TASK-C2-2026-08-04-build19-tour-redesign.md T-3: ツアー独自の(番号のみの)進捗バーを
        // 廃止し、体験ジャーニーバーの5段目「みどころ」を共用する(予告3枚+締めの間は常に
        // カレント)。
        // TASK-C2-2026-08-04-build20-home-cards-and-tour-tiers.md T-B: 再生(フル7枚マニュアル・
        // isFirstRun:false)ではジャーニーバーの「チェック✓の残骸」が意味不明になる(発注書の
        // 指摘)ため非表示にし、かわりに「N/7」の小さな頁表示にする。
        if (isFirstRun) {
            KyonoJourneyBar(labels = KYONO_JOURNEY_STEPS, currentIndex = KYONO_JOURNEY_STEPS.size - 1)
        } else {
            Text(
                "${si + 1}/${slides.size}", color = colors.sub, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 12.dp),
                textAlign = TextAlign.End,
            )
        }
        // TASK-C2-2026-08-04-build19-tour-redesign.md T-5: 内容がボタン列より大きく上に寄り、
        // 画面中央がクリーム一色の余白になっていた(本人指摘)。BoxWithConstraintsで可視高さを
        // 取り、内容Columnに heightIn(min=) と Arrangement.Center を与えて縦中央に寄せる。
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val visibleHeight = this.maxHeight
            Column(
                Modifier.fillMaxWidth().verticalScroll(scrollState).heightIn(min = visibleHeight).padding(20.dp),
                verticalArrangement = Arrangement.Center,
            ) {
            if (si < slides.size) {
                val slide = slides[si]
                Text(slide.title, color = colors.ink, fontSize = 17.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("tourTitle"))
                Spacer(Modifier.height(10.dp))
                // index.html:4118-4142 各スライドv フィールド(実際の画面のミニチュアモックアップ)の1:1移植。
                KyonoTourMockup(slide.mock)
                Spacer(Modifier.height(10.dp))
                // TASK-C2-2026-08-04-build19-tour-redesign.md T-6: lineHeight 27sp@14ptだと行が
                // バラけて痩せて見えていた(本人指摘)ため詰める(iOS版lineSpacing 6の等価値)。
                // 1.5dp線枠は外し、colors.card塗り+角丸14のみのシンプルな箱にする。
                Box(
                    Modifier.fillMaxWidth()
                        .background(colors.card, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(slide.desc, color = colors.ink, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.testTag("tourDesc"))
                }
            } else {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    // index.html:4276 OB_TOUR_CLOSING(chara-congrats.png 110x110・中央表示)の1:1移植。
                    KyonoCharaImage("chara_congrats", Modifier.size(110.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(OB_TOUR_CLOSING_TITLE, color = colors.ink, fontSize = 17.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("tourTitle"))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        OB_TOUR_CLOSING_DESC, color = colors.sub, fontSize = 14.sp, lineHeight = 22.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.testTag("tourDesc"),
                    )
                }
            }
            }
        }
        // D6: ボタン列は内容のスクロールに関わらず画面下端に固定。
        // TASK-C2-2026-08-04-build19-tour-redesign.md T-4: 全幅ボタン3段積み(もどる/つぎへ/
        // とばす)が画面下1/3を占有し野暮ったかった(本人指摘)。全幅ボタンは黄色「つぎへ」1本
        // だけにし、「もどる」「ツアーをとばす」は1行に並べる細身のテキストリンクへ格下げする
        // (枠・塗りなし・タップ領域は高さ44dp確保)。締めスライドは「おわる」黄1本のみ
        // (もどるも省く・alan5指定)。
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 10.dp, bottom = 20.dp)) {
            KyonoPrimaryButton(
                if (si < totalSlides - 1) "つぎへ" else "おわる",
                { if (si < totalSlides - 1) si++ else onDone() },
                Modifier.testTag("tourNextBtn"),
            )
            if (si < totalSlides - 1) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (si > 0) {
                        Text(
                            "◀ もどる", color = colors.sub2, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.heightIn(min = 44.dp).wrapContentHeight(Alignment.CenterVertically)
                                .clickable { si-- }.testTag("tourPrevBtn"),
                        )
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    Text(
                        "ツアーをとばす", color = colors.tealInk, fontSize = 15.sp, fontWeight = FontWeight.Black,
                        modifier = Modifier.heightIn(min = 44.dp).wrapContentHeight(Alignment.CenterVertically)
                            .clickable(onClick = onDone).testTag("tourSkipBtn"),
                    )
                }
            }
        }
        }
    }
}
