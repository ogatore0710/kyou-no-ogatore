package jp.ogatore.kyouno

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import jp.ogatore.kyouno.card.QuizEngine
import jp.ogatore.kyouno.card.QuizScores
import jp.ogatore.kyouno.record.RecordLogic
import jp.ogatore.kyouno.record.RecordStore
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

// index.html:4377 obGo()内の条件式の1:1移植(stiff=hard/unknown、またはworry!=noneならquizへ)。
fun obDecideRoute(stiff: String, worry: String): String =
    if (stiff == "hard" || stiff == "unknown" || worry != "none") "quiz" else "today"

// かたさチェックの.opt.g0〜g3(index.html:301-309)と同じ「明→暗」段階色パレット。オンボの回答チップ
// (obg0-3)・診断の選択肢とも、実際の難易度でなくチップの並び順で明→暗を巡回させる(index.html:4211と
// 同じ「obg"+(i%4)」方式)。
private val OBG_COLORS = listOf(Color(0xFFEAF8F1), Color(0xFFFFF3CB), Color(0xFFFBE3C6), Color(0xFFF2D7CD))

data class ChatBubble(val text: String, val fromUser: Boolean)

// index.html:4395-4434 obOpen/obAskQ/obPick/obGoの1:1移植。「welcome」専用画面は無く、この会話UI自体が
// あいさつ(greet)を最初の3吹き出しとして描画することでwelcome相当を兼ねる(index.html:4405)。
@Composable
fun OnboardingScreen(store: RecordStore, onComplete: (route: String, presetWorry: String?) -> Unit) {
    var bubbles by remember { mutableStateOf(OB_GREET.map { ChatBubble(it, false) } + ChatBubble(OB_QUESTIONS[0].q, false)) }
    var qi by remember { mutableStateOf(0) }
    val answers = remember { mutableStateMapOf<String, String>() }

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

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("🌱 はじめてガイド", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("obTitle"))
        Spacer(Modifier.height(12.dp))
        for (b in bubbles) {
            Text(
                b.text,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                textAlign = if (b.fromUser) TextAlign.End else TextAlign.Start,
            )
        }
        val q = OB_QUESTIONS.getOrNull(qi)
        if (q != null) {
            Spacer(Modifier.height(8.dp))
            Text("👇 タップしてえらんでね", style = MaterialTheme.typography.labelSmall)
            q.chips.forEachIndexed { i, chip ->
                Button(
                    onClick = {
                        answers[q.key] = chip.v
                        bubbles = bubbles + ChatBubble(chip.label, true)
                        OB_ANCHOR_ACK[chip.v]?.let { if (q.key == "anchor") bubbles = bubbles + ChatBubble(it, false) }
                        qi++
                        val nextQ = OB_QUESTIONS.getOrNull(qi)
                        if (nextQ == null) finish() else bubbles = bubbles + ChatBubble(nextQ.q, false)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OBG_COLORS[i % 4], contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).testTag("obChip_${q.key}_${chip.v}"),
                ) { Text(chip.label) }
            }
        }
    }
}

data class QuizOptDef(val label: String, val note: String, val score: Int?, val worryKey: String?)
data class QuizQuestionDef(val key: String, val title: String, val note: String, val opts: List<QuizOptDef>, val artRes: Int? = null)

// app-quiz.js:89-91 QUIZ_ART(momo/kokaのみ実写・kenko/ashi/worryは元々手描きSVG/装飾で写真なし)の
// 1:1移植。R.drawable.quiz_q1/quiz_q2はassets/check/q1.jpg・q2.jpgをそのまま同梱したもの
// (§6 Step5c検収基準「QUIZ_ART写真のアセット同梱」)。
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

data class TypeInfo(val name: String, val copy: String, val hope: String)

// app-quiz.js:45-79 TYPES の1:1移植(name/copy/hopeのみ。pt/rx/poolは動画カタログ連動でStep7aの範囲)。
val QUIZ_TYPES = mapOf(
    "momo" to TypeInfo(
        "つっぱりモモンガ",
        "前屈すると、つま先がとても遠い。それはあなたの脚が長い…わけではなく、もも裏がモモンガの滑空ポーズみたいにピンとつっぱっているサイン。",
        "でもモモンガも、着地すればちゃんと脚をゆるめます。もも裏は変化が出やすい場所。2週間後の前屈で、床がぐっと近くなってるはず。",
    ),
    "koka" to TypeInfo(
        "開かずのトビラ",
        "あぐらでひざが山になるのは、股関節のとびらが閉まっているから。股関節の封印は解きたいですよね。",
        "とびらは、毎日すこしずつ油をさせば開きます。股関節は9分の習慣がいちばん効く場所。あせらずコツコツ。",
    ),
    "kenko" to TypeInfo(
        "飛べないダチョウ",
        "ひじをつけたまま上がらないのは、肩甲骨まわりの羽根が飛べないダチョウみたいに、すっかり休眠しているから。デスクワークの勲章です。",
        "ダチョウの羽根だって、バサバサ動かせば血が巡ります。肩甲骨がゆるむと、肩こりも呼吸もぐっとラクに。",
    ),
    "ashi" to TypeInfo(
        "棒立ちペンギン",
        "しゃがむとかかとがプカッ あるいは後ろにコロン。それは足首がカチッと固まっている証拠。ペンギンは可愛いけど、転ぶと痛い。",
        "足首がゆるむと、歩くのも立つのも軽くなります。つまむだけの簡単ストレッチから始めましょう。",
    ),
    "robot" to TypeInfo(
        "ガチガチロボット",
        "全体的に、ガチガチ。でも言いかえれば、どこを伸ばしても効く「伸びしろの宝庫」ということ。",
        "ロボットにも心はあります。全身をやさしくほぐす1本から始めれば、ガチガチの体もちゃんと応えてくれます。",
    ),
    "yawara" to TypeInfo(
        "しなやかネコ",
        "おっと、けっこうしなやか！あなたはもう「しなやかネコ」。ここから先は、そのしなやかさを守るステージです。",
        "しなやかさは資産。猫が毎朝伸びをするみたいに、朝と夜の習慣で守っていきましょう。悩みに合わせた1本もどうぞ。",
    ),
)

@Serializable
data class QuizTypeResult(val key: String, val worry: String?, val at: String)

// app-quiz.js:145-153 activeQuestions()・194+ decideType呼び出し部分の1:1移植。判定そのものは
// QuizEngine.decideType(Step4で移植済み)を呼ぶだけで、ここでは一切再実装しない
// (マスタープラン§6 Step5c検収基準2)。presetWorryがあるときはQ5(worry)を出題しない。
@Composable
fun QuizScreen(store: RecordStore, presetWorry: String?, onComplete: (typeKey: String) -> Unit) {
    val activeQuestions = remember(presetWorry) {
        if (presetWorry != null) QUIZ_QUESTIONS.filter { it.key != "worry" } else QUIZ_QUESTIONS
    }
    var qi by remember { mutableStateOf(0) }
    val scores = remember { mutableStateMapOf<String, Int>() }
    var worry by remember { mutableStateOf(presetWorry) }

    val q = activeQuestions.getOrNull(qi)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("かたさチェック", style = MaterialTheme.typography.headlineSmall)
        Text("Q${qi + 1} / ${activeQuestions.size}", modifier = Modifier.testTag("quizProgress"))
        if (q != null) {
            Spacer(Modifier.height(8.dp))
            Text(q.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("quizTitle"))
            Text(q.note, style = MaterialTheme.typography.bodySmall)
            q.artRes?.let { res ->
                Spacer(Modifier.height(8.dp))
                Image(
                    painter = painterResource(id = res),
                    contentDescription = "${q.title}のお手本",
                    modifier = Modifier.fillMaxWidth().testTag("quizArt_${q.key}"),
                )
            }
            Spacer(Modifier.height(8.dp))
            q.opts.forEachIndexed { i, opt ->
                Button(
                    onClick = {
                        opt.score?.let { scores[q.key] = it }
                        opt.worryKey?.let { worry = it }
                        qi++
                        if (qi >= activeQuestions.size) {
                            val s = QuizScores(scores["momo"] ?: 0, scores["koka"] ?: 0, scores["kenko"] ?: 0, scores["ashi"] ?: 0)
                            val typeKey = QuizEngine.decideType(s, worry, Instant.now())
                            store.set("type", QuizTypeResult(typeKey, worry, RecordLogic.todayStr(Instant.now())))
                            onComplete(typeKey)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OBG_COLORS[i % 4], contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("quizOpt_${q.key}_$i"),
                ) {
                    Column {
                        Text(opt.label, fontWeight = FontWeight.Bold)
                        Text(opt.note, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
// ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
// Phase 3: index.html:726-735 #result .card.grad-soft/.type-name/.type-copy/.type-hopeの1:1移植。
fun ResultScreen(typeKey: String, onDone: () -> Unit) {
    val info = QUIZ_TYPES[typeKey] ?: TypeInfo(typeKey, "", "")
    // ResultScreenはRecordStoreを受け取らないため、テーマ設定はシステムのダークモードに委ねる("auto"扱い)。
    KyonoTheme("auto") {
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
                    modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    info.name, color = colors.ink, fontSize = 29.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.fillMaxWidth().testTag("resultTypeName"),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    info.copy, color = colors.sub, fontSize = 15.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().background(colors.yellowSoft, RoundedCornerShape(14.dp)).padding(14.dp)) {
                    Text("🌱 " + info.hope, color = colors.ink, fontSize = 15.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            KyonoPrimaryButton("ホームへ", onDone, Modifier.testTag("resultDoneBtn"))
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
    TourSlideDef("📅 マイ記録でふりかえる", "やった日に印がつくカレンダーがあるよ（×はつかないよ） 📏とどくメーターと🎉お楽しみ機能（じまんカード・せんぱいの声・ひとことにっき）もこのタブの「見てみる」から見られるよ 毎日の合図（カレンダー通知）は続ける設定からいつでも入れられるよ📅"),
    TourSlideDef("📖 忘れてもだいじょうぶ", "このツアーも使い方タブの「📖 使い方ツアー」から いつでももう一度見られるよ"),
)
const val OB_TOUR_CLOSING_TITLE = "🌱 これで準備ばっちり！"

// index.html:4283-4347 fdTourMaybeStart/obTourStep/obTourEndの1:1移植。8枚+条件付き9枚目(closing・
// 自動起動時のみ)。スワイプカルーセルでなく「つぎへ」ボタン+ドット進捗のリニアなステップ形式
// (index.html:4297-4324と同じ構造)。
@Composable
fun TourScreen(showClosing: Boolean, onDone: () -> Unit) {
    var si by remember { mutableStateOf(0) }
    val totalSlides = OB_TOUR_SLIDES.size + if (showClosing) 1 else 0
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(modifier = Modifier.testTag("tourDots")) {
            for (i in 0 until totalSlides) {
                Text(if (i == si) "●" else "○", modifier = Modifier.padding(2.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        if (si < OB_TOUR_SLIDES.size) {
            val slide = OB_TOUR_SLIDES[si]
            Text(slide.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("tourTitle"))
            Spacer(Modifier.height(12.dp))
            Text(slide.desc, modifier = Modifier.testTag("tourDesc"))
        } else {
            Text(OB_TOUR_CLOSING_TITLE, style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("tourTitle"))
        }
        Spacer(Modifier.height(24.dp))
        Row {
            if (si > 0) {
                Button(onClick = { si-- }, modifier = Modifier.testTag("tourPrevBtn")) { Text("◀ もどる") }
                Spacer(Modifier.width(12.dp))
            }
            Button(
                onClick = { if (si < totalSlides - 1) si++ else onDone() },
                modifier = Modifier.testTag("tourNextBtn"),
            ) { Text(if (si < totalSlides - 1) "つぎへ" else "とじる") }
        }
        if (si < totalSlides - 1) {
            Spacer(Modifier.height(12.dp))
            Text("ツアーをとばす", modifier = Modifier.clickable { onDone() }.testTag("tourSkipBtn"))
        }
    }
}
