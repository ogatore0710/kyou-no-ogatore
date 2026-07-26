//
//  OnboardingViews.swift
//  KyouNoOgatore
//
//  ネイティブ移植 Step 5c(マスタープラン§6 Step 5c): オンボーディング(4問チャット)・使い方ツアー
//  (8枚)・かたさ診断(QuizEngine呼び出しのみ・判定ロジックはStep4で移植済み)のUI一式。
//  Android版(OnboardingScreens.kt)と同一ロジックのSwiftUI実装。index.html:4087-4143
//  (ONBOARDING_SCRIPT/OB_TOUR_SLIDES)・app-quiz.js:10-79(QUESTIONS/TYPES)の1:1移植
//  (文言はWeb版から転記。判定ロジックはQuizEngine.decideTypeを呼ぶだけで再実装しない)。
//
//  A2HS関連UI(a2hsModal/envBanner/脱出バナー・ホーム画面追加の誘い等)はこのファイルにも他の
//  どこにも実装しない(マスタープラン§2-2・§6 Step5c検収基準3のgrep確認対象)。
//
//  ⚠️ Step5c時点の検収基準どおり、iOS側はビルド・シミュレータ確認をせず、Android版(実タップ確認
//  済み)とのロジック同一性のコードレビューのみを行う対象としてこのファイルを置く(HomeView.swiftの
//  Step5a時点の注記と同じ運用)。

import SwiftUI
import RecordCore
import CardCore

// MARK: - オンボーディング

struct ObChip {
    let label: String
    let v: String
}

struct ObQuestionDef {
    let key: String
    let q: String
    let chips: [ObChip]
}

let obGreet = [
    "いつもありがとうございます！理学療法士のオガトレです！",
    "ここは毎日のストレッチを応援する場所だよ！ぜんぶ無料・とうろく不要🆓 あんしんしてね",
    "最初に4つだけ教えてね！あなた用にこのアプリをととのえます☺️",
]

// index.html:4093-4102 ONBOARDING_SCRIPT.questions の1:1移植。かたさチェック本体(QUESTIONS)とは
// 別の、オンボ専用の簡易4問(bigtext/stiff/worry/anchor)。
let obQuestions = [
    ObQuestionDef(key: "bigtext", q: "もじの大きさ、どっちが見やすい？", chips: [
        ObChip(label: "大きめ（いまのまま）", v: "big"), ObChip(label: "ふつう", v: "normal"),
    ]),
    ObQuestionDef(key: "stiff", q: "体、硬いほう？", chips: [
        ObChip(label: "ガチガチかも", v: "hard"), ObChip(label: "ふつう", v: "normal"),
        ObChip(label: "やわらかい", v: "soft"), ObChip(label: "わからない", v: "unknown"),
    ]),
    ObQuestionDef(key: "worry", q: "いちばん気になるのは？", chips: [
        ObChip(label: "肩こり・首", v: "katakori"), ObChip(label: "腰", v: "youtsuu"),
        ObChip(label: "前屈できない", v: "zenkutsu"), ObChip(label: "眠り", v: "nemuri"),
        ObChip(label: "とくにない", v: "none"),
    ]),
    ObQuestionDef(key: "anchor", q: "ストレッチ、いつやる派？", chips: [
        ObChip(label: "朝おきて", v: "asa"), ObChip(label: "おふろ上がり", v: "furo"),
        ObChip(label: "寝るまえ", v: "neru"), ObChip(label: "きめてない", v: "free"),
    ]),
]

let obAnchorAck: [String: String] = [
    "asa": "朝おきてすぐだね☀️ ホームにも覚えさせたよ📝",
    "furo": "おふろ上がりは体もほぐれてて効果的👍 覚えたよ📝",
    "neru": "寝るまえの1本はねむりにも効くよ🌙 覚えたよ📝",
    "free": "きめなくてもOK！そのつどでだいじょうぶ😊",
]

// app-quiz.js:193 WORRY_TIEBREAKと紐づくQ5語彙への対応表(index.html:4370 OB_WORRY_TO_QUIZ)。
// "none"は対応表に含めない(worry!=="none"のときだけquizルートへ行く条件と対になっている)。
let obWorryToQuiz: [String: String] = ["katakori": "katakori", "youtsuu": "yotsu", "zenkutsu": "yawaraka", "nemuri": "tsukare"]

// index.html:4377 obGo()内の条件式の1:1移植。
func obDecideRoute(stiff: String, worry: String) -> String {
    (stiff == "hard" || stiff == "unknown" || worry != "none") ? "quiz" : "today"
}

// かたさチェックの.opt.g0〜g3(index.html:301-309)・オンボの#obChips .chip.obg0-3(index.html:537-544)と
// 同じ「明→暗」段階色パレット(bg,border)。並び順で明→暗を巡回させる(index.html:4211と同じ
// 「obg"+(i%4)」方式)。ライト/ダークで別パレット。
struct ObgColor {
    let bg: Color
    let border: Color
}
private let obgLight: [ObgColor] = [
    ObgColor(bg: Color(hex: 0xEAF8F1), border: Color(hex: 0xBFE8DC)), ObgColor(bg: Color(hex: 0xFFF3CB), border: Color(hex: 0xF2DE8A)),
    ObgColor(bg: Color(hex: 0xFBE3C6), border: Color(hex: 0xE5BC85)), ObgColor(bg: Color(hex: 0xF2D7CD), border: Color(hex: 0xDCA894)),
]
private let obgDark: [ObgColor] = [
    ObgColor(bg: Color(hex: 0x2A423B), border: Color(hex: 0x2E5A52)), ObgColor(bg: Color(hex: 0x3B3524), border: Color(hex: 0x5C4F1E)),
    ObgColor(bg: Color(hex: 0x403322), border: Color(hex: 0x6A4A26)), ObgColor(bg: Color(hex: 0x402A28), border: Color(hex: 0x5E3A38)),
]
func obgColors(dark: Bool) -> [ObgColor] { dark ? obgDark : obgLight }

struct ChatBubble: Identifiable {
    let id = UUID()
    let text: String
    let fromUser: Bool
}

// index.html:4395-4434 obOpen/obAskQ/obPick/obGoの1:1移植。「welcome」専用画面は無く、この会話UI
// 自体があいさつ(greet)を最初の3吹き出しとして描画することでwelcome相当を兼ねる(index.html:4405)。
// index.html:4211「今後変えたくなったら…」bigtext回答時の相槌の1:1移植(obPick内)。
private let obBigtextAck = "OK！今後変えたくなったら「マイ記録」タブの「続ける設定」でいつでも変更できるよ！"

// 見た目パリティ第2弾(TASK-C2-2026-07-26-visual-parity-round2.md §1): index.html:4182 obSay()の
// 「1.5秒間隔で吹き出しが1つずつ出る」演出を.task+Task.sleep(1.5秒)のコルーチンで1:1再現する。
// reduced-motion対応(Web版は即時表示に切り替え)はこのタスクの検収基準に明記が無く、システム設定の
// 読み取り経路を新設する判断が必要になるため今回は見送る(常に1.5秒間隔で表示。Android版と同じ判断)。
struct OnboardingView: View {
    let store: RecordStore
    let onComplete: (_ route: String, _ presetWorry: String?) -> Void

    @State private var bubbles: [ChatBubble] = []
    @State private var activeQuestion: ObQuestionDef?
    @State private var answers: [String: String] = [:]
    @State private var pendingPick: CheckedContinuation<ObChip, Never>?

    private func finish() {
        // bigtext/anchorは実際の設定として即時反映(index.html:4218-4235 obPick)
        store.set("bigtext", answers["bigtext"] == "big")
        if let anchor = answers["anchor"] { store.set("anchor", anchor) }
        let stiff = answers["stiff"] ?? "normal"
        let worry = answers["worry"] ?? "none"
        let route = obDecideRoute(stiff: stiff, worry: worry)
        let presetWorry = worry != "none" ? obWorryToQuiz[worry] : nil
        store.set("onboarded", true)
        // index.html:4375-4384 はじめの1本ガイド開始条件(オンボ完走・通算0日・fd未設定のときだけ)
        let fdExisting: String? = store.get("fd", default: nil)
        if RecordLogic.loadStreak(store).total == 0 && fdExisting == nil {
            store.set("fd", "go")
            store.set("fdday", RecordLogic.todayStr(now: Date()))
        }
        onComplete(route, presetWorry)
    }

    private func say(_ lines: [String]) async {
        for line in lines {
            bubbles.append(ChatBubble(text: line, fromUser: false))
            try? await Task.sleep(nanoseconds: 1_500_000_000)
        }
    }

    private func awaitPick() async -> ObChip {
        await withCheckedContinuation { cont in
            pendingPick = cont
        }
    }

    private func runFlow() async {
        await say(obGreet)
        for q in obQuestions {
            // index.html:4197 obAskQ(): 質問文もobSay経由(1行)なので表示後に1.5秒待ってからチップを出す。
            await say([q.q])
            activeQuestion = q
            let picked = await awaitPick()
            activeQuestion = nil
            answers[q.key] = picked.v
            bubbles.append(ChatBubble(text: picked.label, fromUser: true)) // index.html:4221 obPick内obBubble("user",...)は即時
            if q.key == "anchor" {
                await say([obAnchorAck[picked.v] ?? "OK！おぼえたよ📝"])
            } else if q.key == "bigtext" {
                await say([obBigtextAck])
            }
        }
        finish()
    }

    private var themeSetting: String { store.get("theme", default: "auto") }

    var body: some View {
        KyonoTheme(themeSetting: themeSetting) {
            OnboardingContentView(
                bubbles: bubbles, activeQuestion: activeQuestion,
                onChipTap: { chip in
                    pendingPick?.resume(returning: chip)
                    pendingPick = nil
                }
            )
        }
        .task { await runFlow() }
    }
}

private struct OnboardingContentView: View {
    @Environment(\.kyonoColors) private var colors
    let bubbles: [ChatBubble]
    let activeQuestion: ObQuestionDef?
    let onChipTap: (ObChip) -> Void

    private var dark: Bool { colors.bg == kyonoDarkColors.bg }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text("🌱 はじめてガイド").font(.kyono(.black900, size: 16)).foregroundColor(colors.ink)
                // index.html:478-483 .sd-row/.sd-b(相談室と共用の吹き出しCSSをオンボでも流用)の1:1移植。
                // border-bottom-right-radius:6px(user)/border-bottom-left-radius:6px(bot)をUnevenRoundedRectangleで再現。
                ForEach(bubbles) { b in
                    // index.html:478-483,4150 .sd-row/.sd-b/.sd-ava(相談室と共用の吹き出しCSS・
                    // chara-hitokotoアバターをオンボでも流用)の1:1移植。
                    HStack(alignment: .bottom) {
                        if b.fromUser { Spacer(minLength: 40) }
                        if !b.fromUser { KyonoCharaImage(name: "chara-hitokoto").frame(width: 38, height: 38) }
                        let shape = UnevenRoundedRectangle(
                            topLeadingRadius: 16, bottomLeadingRadius: b.fromUser ? 16 : 6,
                            bottomTrailingRadius: b.fromUser ? 6 : 16, topTrailingRadius: 16
                        )
                        Text(b.text).font(.kyono(.bold700, size: 15)).foregroundColor(colors.ink).lineSpacing(11)
                            .padding(.horizontal, 14).padding(.vertical, 10)
                            .background(shape.fill(b.fromUser ? colors.yellowSoft : colors.card))
                            .overlay(shape.stroke(b.fromUser ? Color.clear : colors.line, lineWidth: 1.5))
                        if !b.fromUser { Spacer(minLength: 40) }
                    }
                }
                if let q = activeQuestion {
                    Text("👇 タップしてえらんでね").font(.kyono(.bold700, size: 12)).foregroundColor(colors.sub)
                    let palette = obgColors(dark: dark)
                    ForEach(Array(q.chips.enumerated()), id: \.offset) { i, chip in
                        let c = palette[i % 4]
                        Text(chip.label).font(.kyono(.bold700, size: 16)).foregroundColor(colors.ink)
                            .frame(maxWidth: .infinity)
                            .padding(.horizontal, 18).padding(.vertical, 14)
                            .background(RoundedRectangle(cornerRadius: 16).fill(c.bg))
                            .overlay(RoundedRectangle(cornerRadius: 16).stroke(c.border, lineWidth: 2))
                            .onTapGesture { onChipTap(chip) }
                    }
                }
            }
            .padding(20)
        }
        .background(KyonoBackgroundColor().ignoresSafeArea())
    }
}

// MARK: - かたさチェック(診断)

struct QuizOptDef {
    let label: String
    let note: String
    let score: Int?
    let worryKey: String?
}

struct QuizQuestionDef {
    let key: String
    let title: String
    let note: String
    let opts: [QuizOptDef]
    let artResName: String? // Assets.xcassetsの画像名。momo/kokaのみ実写ありnil以外
}

// app-quiz.js:10-42 QUESTIONS の1:1移植(worry=Q5のみscore=nil/worryKey採用・他4問はscore採用)。
// app-quiz.js:89-91 QUIZ_ART(momo/kokaのみ実写)の1:1移植。quiz_q1/quiz_q2はassets/check/q1.jpg・
// q2.jpgをAssets.xcassetsへそのまま同梱したもの(§6 Step5c検収基準「QUIZ_ART写真のアセット同梱」)。
let quizQuestions = [
    QuizQuestionDef(
        key: "momo", title: "立って前屈 手はどこまでいく？", note: "ひざを曲げずに ゆっくり倒れてみて",
        opts: [
            QuizOptDef(label: "床にペタッとつく", note: "手のひら全体がゆかにつく（ゆかタッチ）", score: 0, worryKey: nil),
            QuizOptDef(label: "つま先にさわれる", note: "指先が足先〜床すれすれ（目安 0〜10cm）", score: 1, worryKey: nil),
            QuizOptDef(label: "すねの途中まで", note: "指先がすねの中ほどで止まる（目安 10〜25cm）", score: 2, worryKey: nil),
            QuizOptDef(label: "ひざから下に行かない", note: "指先がひざ上で止まる（目安 25cm以上）", score: 3, worryKey: nil),
        ],
        artResName: "quiz_q1"
    ),
    QuizQuestionDef(
        key: "koka", title: "あぐらで座ると ひざは？", note: "床に座って 足の裏どうしを合わせてみて",
        opts: [
            QuizOptDef(label: "床にペタッと近い", note: "ひざと床のすき間が こぶし1個未満", score: 0, worryKey: nil),
            QuizOptDef(label: "ちょっと浮く", note: "すき間 こぶし1〜2個ぶん", score: 1, worryKey: nil),
            QuizOptDef(label: "山みたいに浮く", note: "すき間 こぶし3個以上", score: 2, worryKey: nil),
            QuizOptDef(label: "そもそもあぐらがつらい", note: "骨盤が立たず 体が後ろに倒れてしまう", score: 3, worryKey: nil),
        ],
        artResName: "quiz_q2"
    ),
    QuizQuestionDef(
        key: "kenko", title: "胸の前で両ひじをつけて上げると どこまで上がる？", note: "手のひらを合わせて 胸の前でひじをくっつけたまま ゆっくり上げてみて",
        opts: [
            QuizOptDef(label: "鼻より上まで上がる", note: "ひじをつけたまま鼻の高さをこえる", score: 0, worryKey: nil),
            QuizOptDef(label: "あごより上まで上がる", note: "ひじをつけたままあごの高さをこえる", score: 1, worryKey: nil),
            QuizOptDef(label: "ひじはつくけど あまり上がらない", note: "ひじはくっつくが胸〜肩の高さまでしか上がらない", score: 2, worryKey: nil),
            QuizOptDef(label: "そもそもひじがつかない", note: "胸の前でひじをくっつけることができない", score: 3, worryKey: nil),
        ],
        artResName: nil
    ),
    QuizQuestionDef(
        key: "ashi", title: "かかとを付けたまま しゃがめる？", note: "和式トイレのポーズ 無理はしないでね",
        opts: [
            QuizOptDef(label: "余裕でしゃがめる", note: "かかとを付けたまま深くしゃがみ 保持できる", score: 0, worryKey: nil),
            QuizOptDef(label: "しゃがめるけど ぐらぐら", note: "しゃがめるが姿勢を保てない", score: 1, worryKey: nil),
            QuizOptDef(label: "かかとが浮いちゃう", note: "足首の曲がり（背屈）が足りないサイン", score: 2, worryKey: nil),
            QuizOptDef(label: "後ろにコロンと転がる", note: "足首＋股関節の複合的な硬さのサイン", score: 3, worryKey: nil),
        ],
        artResName: nil
    ),
    QuizQuestionDef(
        key: "worry", title: "いちばんの悩みは？", note: "あなたに合うおすすめの仕上げに使います",
        opts: [
            QuizOptDef(label: "肩こり・首こり", note: "デスクワーク・スマホ首のお供に", score: nil, worryKey: "katakori"),
            QuizOptDef(label: "腰痛", note: "骨盤まわりからケアします", score: nil, worryKey: "yotsu"),
            QuizOptDef(label: "疲れ・眠りの浅さ", note: "自律神経をととのえます", score: nil, worryKey: "tsukare"),
            QuizOptDef(label: "とにかく柔らかくなりたい", note: "王道の柔軟コースへ", score: nil, worryKey: "yawaraka"),
        ],
        artResName: nil
    ),
]

struct TypeInfo {
    let name: String
    let copy: String
    let hope: String
}

// app-quiz.js:45-79 TYPES の1:1移植(name/copy/hopeのみ。pt/rx/poolは動画カタログ連動でStep7aの範囲)。
let quizTypes: [String: TypeInfo] = [
    "momo": TypeInfo(
        name: "つっぱりモモンガ",
        copy: "前屈すると、つま先がとても遠い。それはあなたの脚が長い…わけではなく、もも裏がモモンガの滑空ポーズみたいにピンとつっぱっているサイン。",
        hope: "でもモモンガも、着地すればちゃんと脚をゆるめます。もも裏は変化が出やすい場所。2週間後の前屈で、床がぐっと近くなってるはず。"
    ),
    "koka": TypeInfo(
        name: "開かずのトビラ",
        copy: "あぐらでひざが山になるのは、股関節のとびらが閉まっているから。股関節の封印は解きたいですよね。",
        hope: "とびらは、毎日すこしずつ油をさせば開きます。股関節は9分の習慣がいちばん効く場所。あせらずコツコツ。"
    ),
    "kenko": TypeInfo(
        name: "飛べないダチョウ",
        copy: "ひじをつけたまま上がらないのは、肩甲骨まわりの羽根が飛べないダチョウみたいに、すっかり休眠しているから。デスクワークの勲章です。",
        hope: "ダチョウの羽根だって、バサバサ動かせば血が巡ります。肩甲骨がゆるむと、肩こりも呼吸もぐっとラクに。"
    ),
    "ashi": TypeInfo(
        name: "棒立ちペンギン",
        copy: "しゃがむとかかとがプカッ あるいは後ろにコロン。それは足首がカチッと固まっている証拠。ペンギンは可愛いけど、転ぶと痛い。",
        hope: "足首がゆるむと、歩くのも立つのも軽くなります。つまむだけの簡単ストレッチから始めましょう。"
    ),
    "robot": TypeInfo(
        name: "ガチガチロボット",
        copy: "全体的に、ガチガチ。でも言いかえれば、どこを伸ばしても効く「伸びしろの宝庫」ということ。",
        hope: "ロボットにも心はあります。全身をやさしくほぐす1本から始めれば、ガチガチの体もちゃんと応えてくれます。"
    ),
    "yawara": TypeInfo(
        name: "しなやかネコ",
        copy: "おっと、けっこうしなやか！あなたはもう「しなやかネコ」。ここから先は、そのしなやかさを守るステージです。",
        hope: "しなやかさは資産。猫が毎朝伸びをするみたいに、朝と夜の習慣で守っていきましょう。悩みに合わせた1本もどうぞ。"
    ),
]

struct QuizTypeResult: Codable {
    let key: String
    let worry: String?
    let at: String
}

// app-quiz.js:145-153 activeQuestions()・194+ decideType呼び出し部分の1:1移植。判定そのものは
// QuizEngine.decideType(Step4で移植済み)を呼ぶだけで、ここでは一切再実装しない
// (マスタープラン§6 Step5c検収基準2)。presetWorryがあるときはQ5(worry)を出題しない。
struct QuizView: View {
    let store: RecordStore
    let presetWorry: String?
    let onComplete: (_ typeKey: String) -> Void
    let onGoHome: () -> Void

    private let activeQuestions: [QuizQuestionDef]
    @State private var qi = 0
    @State private var scores: [String: Int] = [:]
    @State private var worry: String?
    // 全画面完全性監査タスク(TASK-C2-2026-07-26-full-completeness-audit.md #quiz):
    // index.html:1649 quizGoHome()の「回答済みなら確認ダイアログ」の1:1移植。
    @State private var showGoHomeConfirm = false

    init(store: RecordStore, presetWorry: String?, onComplete: @escaping (String) -> Void, onGoHome: @escaping () -> Void) {
        self.store = store
        self.presetWorry = presetWorry
        self.onComplete = onComplete
        self.onGoHome = onGoHome
        self.activeQuestions = presetWorry != nil ? quizQuestions.filter { $0.key != "worry" } : quizQuestions
        _worry = State(initialValue: presetWorry)
    }

    private var themeSetting: String { store.get("theme", default: "auto") }

    var body: some View {
        KyonoTheme(themeSetting: themeSetting) {
            QuizContentView(
                activeQuestions: activeQuestions, qi: qi,
                onOptTap: { q, opt in
                    if let score = opt.score { scores[q.key] = score }
                    if let worryKey = opt.worryKey { worry = worryKey }
                    qi += 1
                    if qi >= activeQuestions.count {
                        let s = QuizEngine.Scores(momo: scores["momo"] ?? 0, koka: scores["koka"] ?? 0, kenko: scores["kenko"] ?? 0, ashi: scores["ashi"] ?? 0)
                        let typeKey = QuizEngine.decideType(s, worry: worry, now: Date())
                        store.set("type", QuizTypeResult(key: typeKey, worry: worry, at: RecordLogic.todayStr(now: Date())))
                        onComplete(typeKey)
                    }
                },
                onBack: { if qi > 0 { qi -= 1 } },
                onGoHomeTap: { if qi > 0 { showGoHomeConfirm = true } else { onGoHome() } }
            )
        }
        .alert("回答を消してホームにもどる？", isPresented: $showGoHomeConfirm) {
            Button("もどる", role: .destructive) { onGoHome() }
            Button("キャンセル", role: .cancel) {}
        }
    }
}

private struct QuizContentView: View {
    @Environment(\.kyonoColors) private var colors
    let activeQuestions: [QuizQuestionDef]
    let qi: Int
    let onOptTap: (QuizQuestionDef, QuizOptDef) -> Void
    let onBack: () -> Void
    let onGoHomeTap: () -> Void

    private var dark: Bool { colors.bg == kyonoDarkColors.bg }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 8) {
                Text("かたさチェック").font(.kyono(.black900, size: 16)).foregroundColor(colors.ink)
                Text("Q\(qi + 1) / \(activeQuestions.count)").font(.kyono(.black900, size: 12)).foregroundColor(colors.sub)
                if qi < activeQuestions.count {
                    let q = activeQuestions[qi]
                    Spacer().frame(height: 4)
                    Text(q.title).font(.kyono(.black900, size: 18)).foregroundColor(colors.ink)
                    Text(q.note).font(.kyono(.bold700, size: 13)).foregroundColor(colors.sub)
                    if let artResName = q.artResName {
                        Image(artResName).resizable().scaledToFit()
                            .background(colors.bg).cornerRadius(16)
                    }
                    // 全画面完全性監査タスク #quiz: index.html:717 .tap-hint(タップ誘導文言)の1:1移植。
                    Spacer().frame(height: 6)
                    Text("👇 タップしてえらんでね").font(.kyono(.black900, size: 13)).foregroundColor(colors.ink)
                    Spacer().frame(height: 4)
                    // index.html:293-309 .opt/.opt.g0〜g3(明→暗の段階色カード)の1:1移植。
                    let palette = obgColors(dark: dark)
                    ForEach(Array(q.opts.enumerated()), id: \.offset) { i, opt in
                        let c = palette[i % 4]
                        VStack(alignment: .leading, spacing: 2) {
                            Text(opt.label).font(.kyono(.black900, size: 15)).foregroundColor(colors.ink)
                            Text(opt.note).font(.kyono(.bold700, size: 13)).foregroundColor(colors.sub)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 16).padding(.vertical, 14)
                        .background(RoundedRectangle(cornerRadius: 16).fill(c.bg))
                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(c.border, lineWidth: 2))
                        .contentShape(Rectangle())
                        .onTapGesture { onOptTap(q, opt) }
                    }
                }
                // 全画面完全性監査タスク #quiz: index.html:720 #qBackBtn(Q1以外で表示)の1:1移植。
                if qi > 0 {
                    Spacer().frame(height: 2)
                    KyonoLineButton("← まえの質問へ", action: onBack)
                }
                // 全画面完全性監査タスク #quiz: index.html:721 「ホームにもどる」ボタンの1:1移植。
                Spacer().frame(height: 2)
                KyonoLineButton("ホームにもどる", action: onGoHomeTap)
            }
            .padding(20)
        }
        .background(KyonoBackgroundColor().ignoresSafeArea())
    }
}

// ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
// Phase 3: index.html:726-735 #result .card.grad-soft/.type-name/.type-copy/.type-hopeの1:1移植。
struct ResultView: View {
    let typeKey: String
    let onDone: () -> Void

    private var info: TypeInfo { quizTypes[typeKey] ?? TypeInfo(name: typeKey, copy: "", hope: "") }

    var body: some View {
        // ResultViewはRecordStoreを受け取らないため、テーマ設定はシステムのダークモードに委ねる("auto"扱い)。
        KyonoTheme(themeSetting: "auto") {
            content
        }
    }

    private var content: some View {
        ResultContentView(info: info, typeKey: typeKey, onDone: onDone)
    }
}

private struct ResultContentView: View {
    @Environment(\.kyonoColors) private var colors
    let info: TypeInfo
    let typeKey: String
    let onDone: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                KyonoGradientCard(gradient: .soft) {
                    Text("あなたのかたさタイプは…").font(.kyono(.black900, size: 14)).foregroundColor(colors.sub)
                        .frame(maxWidth: .infinity, alignment: .center)
                    Spacer().frame(height: 10)
                    // index.html:317-318,729 .type-illust(104x104・中央寄せ)の1:1移植。
                    KyonoTypeArt(typeKey: typeKey).frame(maxWidth: .infinity, alignment: .center)
                    Spacer().frame(height: 4)
                    Text(info.name).font(.kyono(.black900, size: 29)).foregroundColor(colors.ink)
                        .frame(maxWidth: .infinity, alignment: .center)
                    Spacer().frame(height: 8)
                    Text(info.copy).font(.kyono(.bold700, size: 15)).foregroundColor(colors.sub)
                        .frame(maxWidth: .infinity, alignment: .center)
                    Spacer().frame(height: 12)
                    Text("🌱 " + info.hope).font(.kyono(.bold700, size: 15)).foregroundColor(colors.ink)
                        .padding(14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(colors.yellowSoft)
                        .cornerRadius(14)
                }
                KyonoPrimaryButton("ホームへ", action: onDone)
            }
            .padding(20)
        }
        .background(KyonoBackgroundColor().ignoresSafeArea())
    }
}

// MARK: - 使い方ツアー

struct TourSlideDef {
    let title: String
    let desc: String
}

// index.html:4117-4143 OB_TOUR_SLIDES の1:1移植(タイトル・説明文のみ)。A2HS関連の内容は1枚も無い
// (§6 Step5c検収基準3のgrep確認対象と対応)。
let obTourSlides = [
    TourSlideDef(title: "📺 まいにち1本、動画をやる", desc: "ホームの「きょうの1本」をタップ→YouTubeがひらくよ 見おわったらこのアプリにもどってきてね"),
    TourSlideDef(title: "✅ おわったら「きょうやった！」", desc: "アプリにもどったらこのボタンを押すだけ 連続と通算がのびるよ 休んでも毎月3枚の🎫おやすみ券が自動で連続を守ってくれるよ"),
    TourSlideDef(title: "📇 記録カードをつくる", desc: "「きょうやった！」のあと「記録カードを画像でのこす」を押す→「保存・シェアする」で写真に保存📷 SNSやコメント欄にもどうぞ"),
    TourSlideDef(title: "📖 ためると図鑑がうまる", desc: "記録カードは記念日・季節・レアなど何種類もあるよ 毎日の記録でカード図鑑がすこしずつうまっていく（マイ記録→🎉お楽しみ機能）"),
    TourSlideDef(title: "💬 悩みは相談室で質問", desc: "右下の💬ボタンをタップ→「肩こり」のように打つか、チップを選ぶだけ オガトレ監修の答えとおすすめ動画がすぐ届くよ"),
    TourSlideDef(title: "📣 オガトレ通信をのぞく", desc: "尾形さんからのお知らせが届くよ ホームいちばん上の「きょうのひとこと」も毎日かわります✅"),
    TourSlideDef(title: "📅 マイ記録でふりかえる", desc: "やった日に印がつくカレンダーがあるよ（×はつかないよ） 📏とどくメーターと🎉お楽しみ機能（じまんカード・せんぱいの声・ひとことにっき）もこのタブの「見てみる」から見られるよ 毎日の合図（カレンダー通知）は続ける設定からいつでも入れられるよ📅"),
    TourSlideDef(title: "📖 忘れてもだいじょうぶ", desc: "このツアーも使い方タブの「📖 使い方ツアー」から いつでももう一度見られるよ"),
]
let obTourClosingTitle = "🌱 これで準備ばっちり！"

// index.html:4283-4347 fdTourMaybeStart/obTourStep/obTourEndの1:1移植。8枚+条件付き9枚目
// (closing・自動起動時のみ)。「つぎへ」ボタン+ドット進捗のリニアなステップ形式(スワイプ不使用)。
struct TourView: View {
    let showClosing: Bool
    let onDone: () -> Void

    @State private var si = 0

    private var totalSlides: Int { obTourSlides.count + (showClosing ? 1 : 0) }

    // TourViewはRecordStoreを受け取らないため、テーマ設定はシステムのダークモードに委ねる("auto"扱い。
    // ResultViewと同じ判断)。
    var body: some View {
        KyonoTheme(themeSetting: "auto") {
            TourContentView(si: $si, totalSlides: totalSlides, showClosing: showClosing, onDone: onDone)
        }
    }
}

private struct TourContentView: View {
    @Environment(\.kyonoColors) private var colors
    @Binding var si: Int
    let totalSlides: Int
    let showClosing: Bool
    let onDone: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 10) {
                if si < obTourSlides.count {
                    let slide = obTourSlides[si]
                    Text(slide.title).font(.kyono(.black900, size: 17)).foregroundColor(colors.ink)
                    // index.html:4118-4142 各スライドv フィールド(実際の画面のミニチュアモックアップ)の1:1移植。
                    KyonoTourMockup(slideIndex: si)
                    Text(slide.desc).font(.kyono(.bold700, size: 14)).foregroundColor(colors.ink).lineSpacing(11)
                        .padding(.horizontal, 14).padding(.vertical, 10)
                        .background(RoundedRectangle(cornerRadius: 14).fill(colors.card))
                        .overlay(RoundedRectangle(cornerRadius: 14).stroke(colors.line, lineWidth: 1.5))
                } else {
                    // index.html:4276 OB_TOUR_CLOSING(chara-congrats.png 110x110・中央表示)の1:1移植。
                    VStack(spacing: 8) {
                        KyonoCharaImage(name: "chara-congrats").frame(width: 110, height: 110)
                        Text(obTourClosingTitle).font(.kyono(.black900, size: 17)).foregroundColor(colors.ink)
                    }
                    .frame(maxWidth: .infinity)
                }
                // index.html:313-315 .dots/.dot/.dot.on
                HStack(spacing: 6) {
                    ForEach(0..<totalSlides, id: \.self) { i in
                        Circle().fill(i <= si ? colors.pink : colors.line).frame(width: 9, height: 9)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .center)
                .padding(.top, 4)
                Spacer().frame(height: 10)
                if si > 0 {
                    KyonoLineButton("◀ もどる") { si -= 1 }
                }
                KyonoPrimaryButton(si < totalSlides - 1 ? "つぎへ ➡️（\(si + 1)/\(totalSlides)）" : "おわる") {
                    if si < totalSlides - 1 { si += 1 } else { onDone() }
                }
                if si < totalSlides - 1 {
                    KyonoGhostButton("ツアーをとばす", action: onDone)
                }
            }
            .padding(20)
        }
        .background(KyonoBackgroundColor().ignoresSafeArea())
    }
}
