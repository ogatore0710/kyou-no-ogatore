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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import jp.ogatore.kyouno.card.QuizEngine
import jp.ogatore.kyouno.card.QuizScores
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
// reduced-motion設定への対応(Web版は即時表示に切り替え)はこのタスクの検収基準に明記が無く、
// システム設定の読み取り経路を新設する判断が必要になるため今回は見送る(常に1.5秒間隔で表示)。
@Composable
fun OnboardingScreen(store: RecordStore, onComplete: (route: String, presetWorry: String?) -> Unit) {
    var bubbles by remember { mutableStateOf(listOf<ChatBubble>()) }
    var activeQuestion by remember { mutableStateOf<ObQuestionDef?>(null) }
    val answers = remember { mutableStateMapOf<String, String>() }
    val pickChannel = remember { Channel<ObChip>(Channel.CONFLATED) }

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

    LaunchedEffect(Unit) {
        // index.html:4182 obSay()の1:1移植: 1行ごとに表示→1.5秒待つ、を繰り返す。
        suspend fun say(lines: List<String>) {
            for (line in lines) {
                bubbles = bubbles + ChatBubble(line, false)
                delay(1500)
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
        finish()
    }

    val themeSetting = store.get("theme", "auto")
    KyonoTheme(themeSetting) {
        val colors = LocalKyonoColors.current
        val dark = colors.bg == KyonoDarkColors.bg
        Column(Modifier.fillMaxSize().background(colors.bg).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text("🌱 はじめてガイド", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("obTitle"))
            Spacer(Modifier.height(12.dp))
            for (b in bubbles) {
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
                        Text(b.text, color = colors.ink, fontSize = 15.sp, lineHeight = 26.sp)
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

data class TypeInfo(val name: String, val copy: String, val hope: String, val pt: String)

// app-quiz.js:45-79 TYPES の1:1移植(name/copy/hope/pt。rx/poolは動画カタログ連動でStep7aの範囲=
// 全画面完全性監査タスク(TASK-C2-2026-07-26-full-completeness-audit.md #result)で個別の follow-up
// タスクとして切り出す方針。ptは動画非依存の解説文なのでここで追加する)。
val QUIZ_TYPES = mapOf(
    "momo" to TypeInfo(
        "つっぱりモモンガ",
        "前屈すると、つま先がとても遠い。それはあなたの脚が長い…わけではなく、もも裏がモモンガの滑空ポーズみたいにピンとつっぱっているサイン。",
        "でもモモンガも、着地すればちゃんと脚をゆるめます。もも裏は変化が出やすい場所。2週間後の前屈で、床がぐっと近くなってるはず。",
        "硬いのは<b>ハムストリングス（もも裏の筋肉）</b>。ここが硬いと骨盤が後ろに倒れたまま固定され、前屈で腰だけが無理に曲がります。放っておくと<b>腰痛や座り姿勢の悪化</b>につながる場所。逆に言えば、もも裏をゆるめるだけで前屈も腰もラクになります。",
    ),
    "koka" to TypeInfo(
        "開かずのトビラ",
        "あぐらでひざが山になるのは、股関節のとびらが閉まっているから。股関節の封印は解きたいですよね。",
        "とびらは、毎日すこしずつ油をさせば開きます。股関節は9分の習慣がいちばん効く場所。あせらずコツコツ。",
        "硬いのは<b>内もも（内転筋）とお尻（大臀筋・梨状筋）</b>。股関節を外に開く動きが制限されて、あぐら・開脚が苦手になります。股関節は体の土台なので、ここが動くと<b>歩く・座る・立つ全部がラクに</b>。腰への負担も減ります。",
    ),
    "kenko" to TypeInfo(
        "飛べないダチョウ",
        "ひじをつけたまま上がらないのは、肩甲骨まわりの羽根が飛べないダチョウみたいに、すっかり休眠しているから。デスクワークの勲章です。",
        "ダチョウの羽根だって、バサバサ動かせば血が巡ります。肩甲骨がゆるむと、肩こりも呼吸もぐっとラクに。",
        "硬いのは<b>肩甲骨まわり（僧帽筋・広背筋・大胸筋など）</b>。肩甲骨の動きが小さくなると、首と肩の筋肉が代わりに働き続けて<b>肩こり・巻き肩・浅い呼吸</b>の原因に。肩甲骨を動かす習慣がつくと、背中が軽くなって姿勢も変わります。",
    ),
    "ashi" to TypeInfo(
        "棒立ちペンギン",
        "しゃがむとかかとがプカッ あるいは後ろにコロン。それは足首がカチッと固まっている証拠。ペンギンは可愛いけど、転ぶと痛い。",
        "足首がゆるむと、歩くのも立つのも軽くなります。つまむだけの簡単ストレッチから始めましょう。",
        "硬いのは<b>足首の背屈（すねに向けて曲げる動き）＝ふくらはぎ・アキレス腱まわり</b>。ここが硬いと、しゃがむ動作でかかとが浮き、<b>つまずき・むくみ・ふくらはぎの張り</b>につながります。足首は毎日使う関節なので、ゆるめた効果を実感しやすい場所です。",
    ),
    "robot" to TypeInfo(
        "ガチガチロボット",
        "全体的に、ガチガチ。でも言いかえれば、どこを伸ばしても効く「伸びしろの宝庫」ということ。",
        "ロボットにも心はあります。全身をやさしくほぐす1本から始めれば、ガチガチの体もちゃんと応えてくれます。",
        "特定の場所というより<b>全身が複合的に硬い状態</b>。この場合は部位を絞るより、全身をまんべんなく動かすルーティンで底上げするのが近道です。<b>どこを伸ばしても効く＝変化を感じやすい</b>ので、実はいちばん楽しいスタート地点だったりします。",
    ),
    "yawara" to TypeInfo(
        "しなやかネコ",
        "おっと、けっこうしなやか！あなたはもう「しなやかネコ」。ここから先は、そのしなやかさを守るステージです。",
        "しなやかさは資産。猫が毎朝伸びをするみたいに、朝と夜の習慣で守っていきましょう。悩みに合わせた1本もどうぞ。",
        "関節の可動域は良好です。次の課題は<b>「維持」と「使い方」</b>。柔らかくても、支える筋力や毎日の習慣が崩れると体は硬さに戻ります。朝晩の軽いルーティンで可動域を守りつつ、悩みのある部位を先回りでケアしましょう。",
    ),
)

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

@Composable
fun QuizScreen(store: RecordStore, presetWorry: String?, onComplete: (typeKey: String, autoReachLv: Int?) -> Unit, onGoHome: () -> Unit) {
    val activeQuestions = remember(presetWorry) {
        if (presetWorry != null) QUIZ_QUESTIONS.filter { it.key != "worry" } else QUIZ_QUESTIONS
    }
    var qi by remember { mutableStateOf(0) }
    val scores = remember { mutableStateMapOf<String, Int>() }
    var worry by remember { mutableStateOf(presetWorry) }
    // 全画面完全性監査タスク(TASK-C2-2026-07-26-full-completeness-audit.md #quiz):
    // index.html:1649 quizGoHome()の「回答済みなら確認ダイアログ」の1:1移植。
    var showGoHomeConfirm by remember { mutableStateOf(false) }

    val themeSetting = store.get("theme", "auto")
    KyonoTheme(themeSetting) {
        val colors = LocalKyonoColors.current
        val dark = colors.bg == KyonoDarkColors.bg
        val q = activeQuestions.getOrNull(qi)
        Column(Modifier.fillMaxSize().background(colors.bg).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text("かたさチェック", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text("Q${qi + 1} / ${activeQuestions.size}", color = colors.sub, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("quizProgress"))
            if (q != null) {
                Spacer(Modifier.height(10.dp))
                Text(q.title, color = colors.ink, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("quizTitle"))
                Spacer(Modifier.height(4.dp))
                Text(q.note, color = colors.sub, fontSize = 13.sp)
                // 全画面完全性監査タスク #quiz: index.html:717 .tap-hint(タップ誘導文言)の1:1移植。
                Spacer(Modifier.height(6.dp))
                Text("👇 タップしてえらんでね", color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.Black)
                q.artRes?.let { res ->
                    Spacer(Modifier.height(10.dp))
                    Image(
                        painter = painterResource(id = res),
                        contentDescription = "${q.title}のお手本",
                        modifier = Modifier.fillMaxWidth().background(colors.bg, RoundedCornerShape(16.dp)).testTag("quizArt_${q.key}"),
                    )
                }
                Spacer(Modifier.height(10.dp))
                // index.html:293-309 .opt/.opt.g0〜g3(明→暗の段階色カード)の1:1移植。
                val palette = obgColors(dark)
                q.opts.forEachIndexed { i, opt ->
                    val c = palette[i % 4]
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .background(c.bg, RoundedCornerShape(16.dp))
                            .border(2.dp, c.border, RoundedCornerShape(16.dp))
                            .clickable {
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
                        Text(opt.label, color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        Text(opt.note, color = colors.sub, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
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
// rPT/rReachNote/rPace/hint/rRecheckBtn/rSoudanLinkを追加。rxList/rDoneNudge/rTourBtn(動画レコメンド
// 連動・オンボ→ツアー導線)は動画カタログ非依存ではないため別タスクの follow-up として切り出す。
fun ResultScreen(
    store: RecordStore,
    typeKey: String,
    autoReachLv: Int?,
    onDone: () -> Unit,
    onStartQuiz: () -> Unit,
    onOpenSoudan: (String?) -> Unit,
) {
    val info = QUIZ_TYPES[typeKey] ?: TypeInfo(typeKey, "", "", "")
    // ResultScreenはRecordStoreを従来受け取らなかったが、rSoudanLink表示条件(既存のSafetyKBLoader
    // 読み込み有無)には依存しない前提で常時表示にする(ネイティブはKBを起動時に同期読み込み済み)。
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
            // 全画面完全性監査タスク #result: index.html:741-742 #rPace/hint(ペースの目安・免責注意書き)の1:1移植。
            KyonoCard {
                Text("🩺 ペースの目安", color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(6.dp))
                Text(
                    "・毎日が理想！週3でも効きます\n・1日1回で十分\n・痛い日は休むのが正解\n・痛みは「イタ気持ちいい」まで",
                    color = colors.sub, fontSize = 14.sp, lineHeight = 24.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "※効果には個人差があります 痛みが強いときは中止して医療機関へ",
                    color = colors.subFaint, fontSize = 12.sp,
                )
                // 全画面完全性監査タスク #result: index.html:743 #rSoudanLink(タイプ別の相談室逆導線)の1:1移植。
                SOUDAN_TYPE_INTENT[typeKey]?.let { intentId ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "💬 この悩み、相談室で聞いてみる", color = colors.tealInk, fontSize = 14.sp, fontWeight = FontWeight.Black,
                        modifier = Modifier.fillMaxWidth().clickable { onOpenSoudan(intentId) }.testTag("resultSoudanLink"),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            KyonoPrimaryButton("きょうの1本へ", onDone, Modifier.testTag("resultDoneBtn"))
            Spacer(Modifier.height(10.dp))
            // 全画面完全性監査タスク #result: index.html:748 #rRecheckBtn(もう一回チェックする)の1:1移植。
            KyonoGhostButton("もう一回チェックする", onStartQuiz, Modifier.testTag("resultRecheckBtn"))
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
// ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
// Phase 3: index.html:172-176,313-315 .obt-t/.obt-d/.dots/.dot/.dot.onの1:1移植。TourScreenはRecordStoreを
// 受け取らないため、テーマ設定はシステムのダークモードに委ねる("auto"扱い。ResultScreenと同じ判断)。
@Composable
fun TourScreen(showClosing: Boolean, onDone: () -> Unit) {
    var si by remember { mutableStateOf(0) }
    val totalSlides = OB_TOUR_SLIDES.size + if (showClosing) 1 else 0
    KyonoTheme("auto") {
        val colors = LocalKyonoColors.current
        Column(Modifier.fillMaxSize().background(colors.bg).verticalScroll(rememberScrollState()).padding(20.dp)) {
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
            Spacer(Modifier.height(20.dp))
            if (si > 0) {
                KyonoLineButton("◀ もどる", { si-- }, Modifier.testTag("tourPrevBtn"))
                Spacer(Modifier.height(8.dp))
            }
            KyonoPrimaryButton(
                if (si < totalSlides - 1) "つぎへ ➡️（${si + 1}/${totalSlides}）" else "おわる",
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
