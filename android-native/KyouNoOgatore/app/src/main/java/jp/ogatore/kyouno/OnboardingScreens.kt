package jp.ogatore.kyouno

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
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

data class ObChip(val label: String, val v: String)
data class ObQuestionDef(val key: String, val q: String, val chips: List<ObChip>)

val OB_GREET = listOf(
    "いつもありがとうございます！理学療法士のオガトレです！",
    "ここは毎日のストレッチを応援する場所だよ！ぜんぶ無料・とうろく不要🆓 あんしんしてね",
    "最初に4つだけ教えてね！あなた用にこのアプリをととのえます☺️",
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
        listOf(ObChip("ガチガチかも", "hard"), ObChip("ふつう", "normal"), ObChip("やわらかい", "soft"), ObChip("わからない", "unknown")),
    ),
    ObQuestionDef(
        "worry", "いちばん気になるのは？",
        listOf(ObChip("肩こり・首", "katakori"), ObChip("腰", "youtsuu"), ObChip("前屈できない", "zenkutsu"), ObChip("眠り", "nemuri"), ObChip("とくにない", "none")),
    ),
    ObQuestionDef(
        "anchor", "ストレッチ、いつやる派？",
        listOf(ObChip("朝おきて", "asa"), ObChip("おふろ上がり", "furo"), ObChip("寝るまえ", "neru"), ObChip("きめてない", "free")),
    ),
)

val OB_ANCHOR_ACK = mapOf(
    "asa" to "朝おきてすぐだね☀️ ホームにも覚えさせたよ📝",
    "furo" to "おふろ上がりは体もほぐれてて効果的👍 覚えたよ📝",
    "neru" to "寝るまえの1本はねむりにも効くよ🌙 覚えたよ📝",
    "free" to "きめなくてもOK！そのつどでだいじょうぶ😊",
)

// app-quiz.js:193 WORRY_TIEBREAKと紐づくQ5語彙(katakori/yotsu/tsukare/yawaraka)への対応表
// (index.html:4370 OB_WORRY_TO_QUIZ)。"none"は対応表に含めない(実質的な悩みではないためQ5を
// スキップする対象外=worry!=="none"のときだけquizルートへ行く条件と対になっている)。
val OB_WORRY_TO_QUIZ = mapOf("katakori" to "katakori", "youtsuu" to "yotsu", "zenkutsu" to "yawaraka", "nemuri" to "tsukare")

// index.html:4108-4111 ONBOARDING_SCRIPT.routesの1:1移植(TASK-C2-2026-07-27-onboarding-routes-closing-message)。
data class ObRouteInfo(val say: List<String>, val btn: String)
val OB_ROUTES = mapOf(
    "quiz" to ObRouteInfo(listOf("そしたら30秒で硬さチェックをしよう！下のボタンタップしてね！"), "かたさチェックをはじめる"),
    "today" to ObRouteInfo(listOf("じゃあ今日の1本から！むずかしいことはなしだよ👍"), "きょうの1本を見る"),
)

// index.html:4377 obGo()内の条件式の1:1移植(stiff=hard/unknown、またはworry!=noneならquizへ)。
fun obDecideRoute(stiff: String, worry: String): String =
    if (stiff == "hard" || stiff == "unknown" || worry != "none") "quiz" else "today"

// かたさチェックの.opt.g0〜g3(index.html:301-309)・オンボの#obChips .chip.obg0-3(index.html:537-544)と
// 同じ「明→暗」段階色パレット(bg,border)。実際の難易度でなくチップの並び順で明→暗を巡回させる
// (index.html:4211と同じ「obg"+(i%4)」方式)。ライト/ダークで別パレット。
private data class ObgColor(val bg: Color, val border: Color)
private val OBG_LIGHT = listOf(
    ObgColor(Color(0xFFEAF8F1), Color(0xFFBFE8DC)), ObgColor(Color(0xFFFFF3CB), Color(0xFFF2DE8A)),
    ObgColor(Color(0xFFFBE3C6), Color(0xFFE5BC85)), ObgColor(Color(0xFFF2D7CD), Color(0xFFDCA894)),
)
private val OBG_DARK = listOf(
    ObgColor(Color(0xFF2A423B), Color(0xFF2E5A52)), ObgColor(Color(0xFF3B3524), Color(0xFF5C4F1E)),
    ObgColor(Color(0xFF403322), Color(0xFF6A4A26)), ObgColor(Color(0xFF402A28), Color(0xFF5E3A38)),
)
private fun obgColors(dark: Boolean) = if (dark) OBG_DARK else OBG_LIGHT

data class ChatBubble(val text: String, val fromUser: Boolean)

// index.html:4211 「今後変えたくなったら…」bigtext回答時の相槌の1:1移植(obPick内)。
private const val OB_BIGTEXT_ACK = "OK！今後変えたくなったら「マイ記録」タブの「続ける設定」でいつでも変更できるよ！"

// index.html:4395-4434 obOpen/obAskQ/obPick/obGoの1:1移植。「welcome」専用画面は無く、この会話UI自体が
// あいさつ(greet)を最初の3吹き出しとして描画することでwelcome相当を兼ねる(index.html:4405)。
//
// 見た目パリティ第2弾(TASK-C2-2026-07-26-visual-parity-round2.md §1): index.html:4182 obSay()の
// 「1.5秒間隔で吹き出しが1つずつ出る」演出をLaunchedEffect+delay(1500)のコルーチンで1:1再現する。
// §D(TASK-C2-2026-07-27-behavior-parity-audit.md): index.html:4145 obReducedMotion()/4186
// const wait=obReducedMotion()?0:1500の1:1移植として、reduced-motion時は待機をなくす。
@Composable
fun OnboardingScreen(store: RecordStore, onComplete: (route: String, presetWorry: String?) -> Unit) {
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
                "anchor" -> say(listOf(OB_ANCHOR_ACK[picked.v] ?: "OK！おぼえたよ📝"))
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

    val themeSetting = store.get("theme", "auto")
    KyonoTheme(themeSetting, bigText = store.get("bigtext", true)) {
        val colors = LocalKyonoColors.current
        val dark = colors.bg == KyonoDarkColors.bg
        // TASK-C2-2026-07-28-onboarding-sheet-tap-stolen.md: 新しい設問/選択肢が追加されても
        // スクロール位置が追従しておらず、最新の選択肢がシートの可視領域の端(タップ判定が
        // 効かない位置)ぎりぎりに描画される欠落があった。追加のたびに最下部へ自動スクロールし、
        // 選択肢が常に見える・押せる位置に来るようにする。
        val obScrollState = rememberScrollState()
        LaunchedEffect(bubbles.size, activeQuestion, routeCta) {
            delay(60) // 直前のレイアウト確定(新しい吹き出し/選択肢の高さ反映)を待つ猶予
            obScrollState.animateScrollTo(obScrollState.maxValue)
        }
        Column(Modifier.fillMaxSize().background(colors.bg).verticalScroll(obScrollState).padding(20.dp)) {
            Text("🌱 はじめてガイド", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("obTitle"))
            Spacer(Modifier.height(12.dp))
            // TASK-C2-2026-07-27-chips-overflow-and-bubble-pop.md §3: index.html:4149 .sd-pop
            // (opacity0→1・translateY(4px)→0・.18s ease-out)の1:1移植。reduced-motion時は無演出即表示。
            val obBubblePopDensity = LocalDensity.current
            for (b in bubbles) {
                AnimatedVisibility(
                    visible = true,
                    enter = if (obReducedMotion) {
                        fadeIn(tween(0))
                    } else {
                        fadeIn(tween(180)) + slideInVertically(tween(180)) { with(obBubblePopDensity) { 4.dp.roundToPx() } }
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
                        KyonoCharaImage("chara_hitokoto", Modifier.size(38.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Box(
                        Modifier.fillMaxWidth(0.82f)
                            .let {
                                if (b.fromUser) it.background(colors.yellowSoft, RoundedCornerShape(16.dp, 16.dp, 6.dp, 16.dp))
                                else it.background(colors.card, RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp)).border(1.5.dp, colors.line, RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp))
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
                        Text(
                            b.text, color = colors.ink, fontSize = 15.sp, lineHeight = 26.sp,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                }
                }
            }
            val q = activeQuestion
            if (q != null) {
                Spacer(Modifier.height(8.dp))
                Text("👇 タップしてえらんでね", color = colors.sub, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                val palette = obgColors(dark)
                q.chips.forEachIndexed { i, chip ->
                    val c = palette[i % 4]
                    Text(
                        chip.label, color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                            .background(c.bg, RoundedCornerShape(16.dp))
                            .border(2.dp, c.border, RoundedCornerShape(16.dp))
                            .clickable { pickChannel.trySend(chip) }
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                            .testTag("obChip_${q.key}_${chip.v}"),
                    )
                }
            }
            val cta = routeCta
            if (cta != null) {
                Spacer(Modifier.height(8.dp))
                KyonoPrimaryButton(cta.btn, { ctaChannel.trySend(Unit) }, Modifier.fillMaxWidth().testTag("obRouteCtaBtn"))
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
private data class WorryExtra(val v: String, val label: String)
private val WORRY_EXTRA = mapOf(
    "katakori" to WorryExtra("katakori", "肩こりさんへ もう1本"),
    "yotsu" to WorryExtra("yotsu12", "腰痛さんへ もう1本"),
    "tsukare" to WorryExtra("jiritsu10", "おつかれさんへ もう1本"),
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
fun currentRx(typeKey: String, now: Instant): List<String> {
    val t = TYPE_RX_POOL[typeKey] ?: return emptyList()
    val need = 3 - t.rx.size
    if (need <= 0 || t.pool.isEmpty()) return t.rx.take(3)
    val r = jp.ogatore.kyouno.card.CardLottery.rotationIndex(now)
    val spacing = t.pool.size / (if (need == 3) 3 else 2)
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

@Composable
fun QuizScreen(store: RecordStore, presetWorry: String?, onComplete: (typeKey: String, autoReachLv: Int?) -> Unit, onGoHome: () -> Unit) {
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
    // 全画面完全性監査タスク(TASK-C2-2026-07-26-full-completeness-audit.md #quiz):
    // index.html:1649 quizGoHome()の「回答済みなら確認ダイアログ」の1:1移植。
    var showGoHomeConfirm by remember { mutableStateOf(false) }
    // TASK-C2-2026-07-28-quiz-result-reach-parity.md §6(Android限定): app-quiz.js:156-158の
    // history.pushState設計(戻るで1問ずつ遡れる)の1:1移植。BackHandlerが1つも無く、ハードウェア/
    // ジェスチャーの「もどる」を押すと確認なしに回答が消えていた欠落。qi==0では既存の確認
    // ダイアログ(showGoHomeConfirm)を通す。
    BackHandler {
        if (qi > 0) qi-- else showGoHomeConfirm = true
    }

    val themeSetting = store.get("theme", "auto")
    KyonoTheme(themeSetting, bigText = store.get("bigtext", true)) {
        val colors = LocalKyonoColors.current
        val dark = colors.bg == KyonoDarkColors.bg
        val q = activeQuestions.getOrNull(qi)
        Column(Modifier.fillMaxSize().background(colors.bg).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text("かたさチェック", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text("Q${qi + 1} / ${activeQuestions.size}", color = colors.sub, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("quizProgress"))
            // TASK-C2-2026-07-28-quiz-result-reach-parity.md §5: index.html:719 .dots+app-quiz.js:175-176の
            // 1:1移植。ツアー画面にはドットがあるのにクイズには無かった欠落。
            Row(modifier = Modifier.padding(top = 6.dp).testTag("quizDots"), horizontalArrangement = Arrangement.Center) {
                for (i in activeQuestions.indices) {
                    Box(
                        Modifier.padding(horizontal = 3.dp).size(9.dp)
                            .background(if (i <= qi) colors.pink else colors.line, RoundedCornerShape(50)),
                    )
                }
            }
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
                Text("👇 タップしてえらんでね", color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(10.dp))
                // index.html:293-309 .opt/.opt.g0〜g3(明→暗の段階色カード)の1:1移植。
                // TASK-C2-2026-07-28-quiz-result-reach-parity.md §5: app-quiz.js:168-169
                // 「段階色は数値スコアの設問(Q1-Q4)だけ」の1:1移植。Q5(worry)はscore==nullのため
                // 段階色を付けず、通常のカード色(colors.card/colors.line)にする。
                val palette = obgColors(dark)
                q.opts.forEachIndexed { i, opt ->
                    val c = if (opt.score != null) palette[i % 4] else null
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
                            opt.label, color = colors.ink, fontSize = 18.sp, lineHeight = 18.sp,
                            style = KyonoTightLineTextStyle, fontWeight = FontWeight.Black,
                        )
                        Text(
                            opt.note, color = colors.sub, fontSize = 13.sp, lineHeight = 19.5.sp,
                            style = KyonoTightLineTextStyle, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                // 全画面完全性監査タスク #quiz: index.html:720 #qBackBtn(Q1以外で表示・まえの質問へ戻る)の1:1移植。
                if (qi > 0) {
                    Spacer(Modifier.height(10.dp))
                    KyonoLineButton("← まえの質問へ", { qi-- }, Modifier.testTag("qBackBtn"))
                }
                // 全画面完全性監査タスク #quiz: index.html:721 「ホームにもどる」ボタンの1:1移植。
                // index.html:1649 quizGoHome(): 回答済み(qi>0)のときだけ確認ダイアログを出す。
                Spacer(Modifier.height(10.dp))
                KyonoLineButton(
                    "ホームにもどる",
                    { if (qi > 0) showGoHomeConfirm = true else onGoHome() },
                    Modifier.testTag("quizGoHomeBtn"),
                )
            }
        }
        if (showGoHomeConfirm) {
            AlertDialog(
                onDismissRequest = { showGoHomeConfirm = false },
                title = { Text("回答を消してホームにもどる？") },
                confirmButton = { Button(onClick = { showGoHomeConfirm = false; onGoHome() }) { Text("もどる") } },
                dismissButton = { TextButton(onClick = { showGoHomeConfirm = false }) { Text("キャンセル") } },
            )
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
    KyonoTheme("auto", bigText = store.get("bigtext", true)) {
        val colors = LocalKyonoColors.current
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.bg)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            KyonoGradientCard(KyonoGradient.Soft, Modifier.testTag("resultCard")) {
                Text(
                    "あなたのかたさタイプは…", color = colors.sub, fontSize = 14.sp, fontWeight = FontWeight.Black,
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
                // TASK-C2-2026-07-28-quiz-result-reach-parity.md §1: app-quiz.js:291-299の1:1移植。
                // ガイド中(fdGuideActive)は結果画面を「タイプ+①」に削ぎ落とす(2026-07-21 5視点
                // 検証C・PO承認済み)。長文解説(rHope/rPT)・ペース目安(rPace)・②③・相談室リンクは
                // 翌日以降(通常表示)に回す(一本道=①をタップ→もどる→記録、以外の分岐を見せない)。
                if (!fdGuideActive) {
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth().background(colors.yellowSoft, RoundedCornerShape(14.dp)).padding(14.dp)) {
                        Text("🌱 " + info.hope, color = colors.ink, fontSize = 15.sp)
                    }
                    // 全画面完全性監査タスク #result: index.html:733 #rPT(理学療法士のひとくち解説)の1:1移植。
                    Spacer(Modifier.height(12.dp))
                    Column {
                        Text("🩺 理学療法士のひとくち解説", color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            annotatedBoldHtml(info.pt, colors.ink), color = colors.sub, fontSize = 14.sp, lineHeight = 21.sp,
                            modifier = Modifier.testTag("resultPT"),
                        )
                    }
                }
                // 全画面完全性監査タスク #result: index.html:734 #rReachNote(Q1自動転記の一言)の1:1移植。
                autoReachLv?.let { lv ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "📏 いまの前屈「${REACH_LV[lv]}」を とどくメーターにも記録したよ",
                        color = colors.tealInk, fontSize = 13.sp, fontWeight = FontWeight.Black,
                        modifier = Modifier.fillMaxWidth().testTag("resultReachNote"), textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            if (fdGuideActive) {
                // TASK-C2-2026-07-27-fd-guide-ui-branch.md: app-quiz.js:300-322の1:1移植。ガイド中は
                // 通常の3本リストではなく、①だけを練習させる専用UIに差し替える(2026-07-21 5視点
                // 検証D・PO承認済み)。「タスクスイッチできない層がYouTubeから戻れないのが最初の
                // 脱落点」という動機のため、OS別のもどりかた案内を必ず添える。
                KyonoCard {
                    Text(
                        "きょうはこの1本だけでOK！", color = colors.ink, fontSize = 15.sp,
                        fontWeight = FontWeight.Black, modifier = Modifier.testTag("rxHead"),
                    )
                    Spacer(Modifier.height(10.dp))
                    Column(modifier = Modifier.testTag("rxList")) {
                        // app-quiz.js:316-320 sd-row.oga相当の練習宣言吹き出し(相談室botバブルと同じ見た目)。
                        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.testTag("fdPracticeBubble")) {
                            KyonoCharaImage("chara_hitokoto", Modifier.size(38.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(
                                Modifier.fillMaxWidth()
                                    .background(colors.card, RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp))
                                    .border(1.5.dp, colors.line, RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                            ) {
                                Text(
                                    "ここからは練習だよ🏫 ①を試しにタップ→YouTubeがひらいたら すぐ戻ってきてね" +
                                        "（ぜんぶ見るのは あとでゆっくりでOK）",
                                    color = colors.ink,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "🔙 見おわったら スマホの「もどる」ボタン（◀）で この画面にもどれるよ",
                                    color = colors.sub, fontSize = 13.sp,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        // index.html:214,216 fdBob(1.4s ease-in-out infinite・translateY 0↔5px)の1:1移植。
                        // index.html:214 @media(prefers-reduced-motion:no-preference)の1:1移植:
                        // 減速設定オンでは静止させる(TASK-C2-2026-07-27-behavior-parity-audit.md §D)。
                        val fdBobOffset by if (rememberReducedMotion()) {
                            remember { mutableStateOf(0f) }
                        } else {
                            val fdBobInfinite = rememberInfiniteTransition(label = "fdBob")
                            fdBobInfinite.animateFloat(
                                initialValue = 0f, targetValue = 5f,
                                animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                label = "fdBobOffset",
                            )
                        }
                        Text(
                            "👇 ここを押してみて", color = colors.pink, fontSize = 15.sp, fontWeight = FontWeight.Black,
                            modifier = Modifier.fillMaxWidth().offset(y = fdBobOffset.dp).testTag("fdPoint"),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(4.dp))
                        rx.firstOrNull()?.let { vk ->
                            // TASK-C2-2026-07-28-quiz-result-reach-parity.md §5: app-quiz.js:320
                            // videoCard(rx[0], "きょうはこれ1本でOK!")の1:1移植。badge=nullで欠落していた。
                            lookupVideo(vk)?.let { v -> VideoRow(v, onVideoTap, badge = "きょうはこれ1本でOK！", hero = true) }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "あと2本とくわしい解説は あしたから見られるよ🌱", color = colors.sub, fontSize = 13.sp,
                        fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth().testTag("fdRotateNote"),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                // 診断結果画面「おすすめ動画3本」欠落修正タスク(TASK-C2-2026-07-26-result-video-recommendations.md):
                // index.html:736-744 rxHead/rxList/worryExtra/rRotateNoteの1:1移植。
                KyonoCard {
                    Text(
                        "おすすめの3本: まずは「${info.area}」から！2週間続けてみて", color = colors.ink,
                        fontSize = 15.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("rxHead"),
                    )
                    Spacer(Modifier.height(10.dp))
                    val badges = listOf("①まずほぐす", "②メインの1本", "③しあげ")
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.testTag("rxList")) {
                        rx.forEachIndexed { i, vk ->
                            lookupVideo(vk)?.let { v -> VideoRow(v, onVideoTap, badge = badges.getOrNull(i)) }
                        }
                    }
                    if (rx.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        KyonoGhostButton(
                            "▶ 3本続けて再生する",
                            { openUrl("https://www.youtube.com/watch_videos?video_ids=" + rx.mapNotNull { QUIZ_VIDEO_KEY_TO_ID[it] }.joinToString(",")) },
                            Modifier.testTag("rxPlayAllBtn"),
                        )
                    }
                    // index.html:81-85,327-328 WORRY[saved.worry](悩み別の追加1本。3本と重複しない場合のみ)の1:1移植。
                    worry?.let { w ->
                        WORRY_EXTRA[w]?.let { extra ->
                            if (extra.v !in rx) {
                                lookupVideo(extra.v)?.let { v ->
                                    Spacer(Modifier.height(4.dp))
                                    VideoRow(v, onVideoTap, badge = "＋ ${extra.label}")
                                }
                            }
                        }
                    }
                    // index.html:740 #rRotateNoteの1:1移植。
                    Spacer(Modifier.height(4.dp))
                    // GO-G2(5視点ワンループ): index.html:740 .rotate-note{color:var(--sub)}の1:1移植。
                    // subFaintは実測コントラスト不足(3.87:1)で、Web版でもここはvar(--sub)であり
                    // 元々subFaintの用途ではなかった(subFaintの正しい用途はオガトレ通信の
                    // 30日超の古い投稿日付のみ・index.html:277-278)。
                    Text(
                        "おすすめは3日ごとに自動で入れ替わります", color = colors.sub, fontSize = 12.sp,
                        modifier = Modifier.testTag("rRotateNote"),
                    )
                }
            }
            // TASK-C2-2026-07-28-quiz-result-reach-parity.md §1: rPace/rSoudanLinkもガイド中は隠す
            // (app-quiz.js:291-299 rPace + app-quiz.js:332 !guide && sdKb()の1:1移植)。
            if (!fdGuideActive) {
                Spacer(Modifier.height(16.dp))
                // 全画面完全性監査タスク #result: index.html:741-742 #rPace/hint(ペースの目安・免責注意書き)の1:1移植。
                KyonoCard {
                    Text("🩺 ペースの目安", color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.Black)
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
                        Text(
                            "💬 この悩み、相談室で聞いてみる", color = colors.tealInk, fontSize = 14.sp, fontWeight = FontWeight.Black,
                            modifier = Modifier.fillMaxWidth().clickable { onOpenSoudan(intentId) }.padding(vertical = 12.dp).testTag("resultSoudanLink"),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            // ダークモード再確認+rDoneNudge/rTourBtn実装タスク: index.html:745 #rDoneNudgeの1:1移植。
            // はじめの1本ガイド中、結果画面を表示したまま動画を見に行って戻ってきたときに、
            // ホームのcheerの代わりに結果画面内へ「やった？」の復帰案内を出す。
            if (showDoneNudge) {
                Spacer(Modifier.height(16.dp))
                KyonoCard(Modifier.testTag("rDoneNudge")) {
                    Text("おかえりなさい！✨ ストレッチできた？", color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(10.dp))
                    KyonoPrimaryButton(
                        if (fdGuideActive) "✅ 1日目の記録をつけにいく" else "✅ きょうの記録をつけにいく",
                        onDoneFromNudge,
                        Modifier.testTag("rDoneNudgeBtn"),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            // index.html:746 #rTourBtn(オンボ→クイズ経由・ツアー未見のときだけ)の1:1移植。
            if (showTourBtn) {
                KyonoGhostButton("📖 つづき：使い方ツアーへ", onStartTour, Modifier.testTag("rTourBtn"))
                Spacer(Modifier.height(10.dp))
            }
            // TASK-C2-2026-07-28-quiz-result-reach-parity.md §1: rGoHomeBtn/rRecheckBtnもガイド中は
            // 隠す(app-quiz.js:291-299の1:1移植。一本道=①をタップ→もどる→記録、以外の分岐を見せない。
            // タブバーからの脱出は常に可能なため、隠しても迷子にはならない)。
            if (!fdGuideActive) {
                KyonoPrimaryButton("きょうの1本へ", onDone, Modifier.testTag("resultDoneBtn"))
                Spacer(Modifier.height(10.dp))
                // 全画面完全性監査タスク #result: index.html:748 #rRecheckBtn(もう一回チェックする)の1:1移植。
                KyonoGhostButton("もう一回チェックする", onStartQuiz, Modifier.testTag("resultRecheckBtn"))
            }
        }
    }
}

data class TourSlideDef(val title: String, val desc: String)

// index.html:4117-4143 OB_TOUR_SLIDES の1:1移植(タイトル・説明文のみ。実画面モックHTML(v)は
// 移植対象外=ネイティブでは実UI自体がその場にあるため不要)。A2HS関連の内容は1枚も無い
// (§6 Step5c検収基準3のgrep確認対象と対応)。
val OB_TOUR_SLIDES = listOf(
    TourSlideDef("📺 まいにち1本、動画をやる", "ホームの「きょうの1本」をタップ→YouTubeがひらくよ 見おわったらこのアプリにもどってきてね"),
    TourSlideDef("✅ おわったら「きょうやった！」", "アプリにもどったらこのボタンを押すだけ 連続と通算がのびるよ 休んでも毎月3枚の🎫おやすみ券が自動で連続を守ってくれるよ"),
    TourSlideDef("📇 記録カードをつくる", "「きょうやった！」のあと「記録カードを画像でのこす」を押す→「保存・シェアする」で写真に保存📷 SNSやコメント欄にもどうぞ"),
    TourSlideDef("📖 ためると図鑑がうまる", "記録カードは記念日・季節・レアなど何種類もあるよ 毎日の記録でカード図鑑がすこしずつうまっていく（マイ記録→🎉お楽しみ機能）"),
    TourSlideDef("💬 悩みは相談室で質問", "右下の💬ボタンをタップ→「肩こり」のように打つか、チップを選ぶだけ オガトレ監修の答えとおすすめ動画がすぐ届くよ"),
    TourSlideDef("📣 オガトレ通信をのぞく", "尾形さんからのお知らせが届くよ ホームいちばん上の「きょうのひとこと」も毎日かわります✅"),
    // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §6: Web版の「見てみる」ボタンはネイティブの
    // マイ記録に存在しない(お楽しみは🎉じまんカード/💬せんぱいの声/📔ひとことにっきの個別3ボタン)ため、
    // その3ボタンを直接指す文言に書きかえる(以前はWeb版UI前提の文言のまま移植されていた)。
    TourSlideDef("📅 マイ記録でふりかえる", "やった日に印がつくカレンダーがあるよ（×はつかないよ） 📏とどくメーターと🎉じまんカード・💬せんぱいの声・📔ひとことにっき もこのタブから見られるよ 毎日の合図（カレンダー通知）は続ける設定からいつでも入れられるよ📅"),
    TourSlideDef("📖 忘れてもだいじょうぶ", "このツアーも使い方タブの「📖 使い方ツアー」から いつでももう一度見られるよ"),
)
const val OB_TOUR_CLOSING_TITLE = "🌱 これで準備ばっちり！"
// TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §5: index.html:4275-4277
// OB_TOUR_CLOSING.dの1:1移植(2026-07-21 PO承認案(b))。以前はタイトルのみで説明文が欠落していた。
const val OB_TOUR_CLOSING_DESC = "あしたも待ってるね🌱 きょうのぶんの動画をちゃんとやるなら ホームの「きょうの1本」からどうぞ💪"

// index.html:4283-4347 fdTourMaybeStart/obTourStep/obTourEndの1:1移植。8枚+条件付き9枚目(closing・
// 自動起動時のみ)。スワイプカルーセルでなく「つぎへ」ボタン+ドット進捗のリニアなステップ形式
// (index.html:4297-4324と同じ構造)。
// ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
// Phase 3: index.html:172-176,313-315 .obt-t/.obt-d/.dots/.dot/.dot.onの1:1移植。
// TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §6: 以前はRecordStoreを受け取らず
// テーマ・文字サイズを"auto"/trueに固定していたため、手動でライト設定にしている本人が
// 夜に再訪するとツアー画面だけダークになる不具合があった。他画面と同じくstoreの実設定を使う。
@Composable
fun TourScreen(store: RecordStore, showClosing: Boolean, onDone: () -> Unit) {
    var si by remember { mutableStateOf(0) }
    val totalSlides = OB_TOUR_SLIDES.size + if (showClosing) 1 else 0
    KyonoTheme(store.get("theme", "auto"), bigText = store.get("bigtext", true)) {
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
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState).padding(20.dp),
        ) {
            if (si < OB_TOUR_SLIDES.size) {
                val slide = OB_TOUR_SLIDES[si]
                Text(slide.title, color = colors.ink, fontSize = 17.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("tourTitle"))
                Spacer(Modifier.height(10.dp))
                // index.html:4118-4142 各スライドv フィールド(実際の画面のミニチュアモックアップ)の1:1移植。
                KyonoTourMockup(si)
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.fillMaxWidth()
                        .background(colors.card, RoundedCornerShape(14.dp))
                        .border(1.5.dp, colors.line, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(slide.desc, color = colors.ink, fontSize = 14.sp, lineHeight = 27.sp, modifier = Modifier.testTag("tourDesc"))
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
            // index.html:313-315 .dots/.dot/.dot.on
            Row(modifier = Modifier.padding(top = 14.dp).testTag("tourDots"), horizontalArrangement = Arrangement.Center) {
                for (i in 0 until totalSlides) {
                    Box(
                        Modifier.padding(horizontal = 3.dp).size(9.dp)
                            .background(if (i <= si) colors.pink else colors.line, RoundedCornerShape(50)),
                    )
                }
            }
        }
        // D6: ボタン列は内容のスクロールに関わらず画面下端に固定。
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 10.dp, bottom = 20.dp)) {
            if (si > 0) {
                KyonoLineButton("◀ もどる", { si-- }, Modifier.testTag("tourPrevBtn"))
                Spacer(Modifier.height(8.dp))
            }
            // D6: ボタン文言から絵文字を外す(D7と同じ理由)。ドット進捗が既に上にあるため、
            // 文字側の「(N/9)」は重複と判断して落とす(ドットのほうを残す・iOSと同じ判断)。
            KyonoPrimaryButton(
                if (si < totalSlides - 1) "つぎへ" else "おわる",
                { if (si < totalSlides - 1) si++ else onDone() },
                Modifier.testTag("tourNextBtn"),
            )
            if (si < totalSlides - 1) {
                Spacer(Modifier.height(8.dp))
                KyonoGhostButton("ツアーをとばす", onDone, Modifier.testTag("tourSkipBtn"))
            }
        }
        }
    }
}
