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

// index.html:4108-4111 ONBOARDING_SCRIPT.routesの1:1移植(TASK-C2-2026-07-27-onboarding-routes-closing-message)。
struct ObRouteInfo {
    let say: [String]
    let btn: String
}
let obRoutes: [String: ObRouteInfo] = [
    "quiz": ObRouteInfo(say: ["そしたら30秒で硬さチェックをしよう！下のボタンタップしてね！"], btn: "かたさチェックをはじめる"),
    "today": ObRouteInfo(say: ["じゃあ今日の1本から！むずかしいことはなしだよ👍"], btn: "きょうの1本を見る"),
]

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
// §D(TASK-C2-2026-07-27-behavior-parity-audit.md): index.html:4145 obReducedMotion()/4186
// const wait=obReducedMotion()?0:1500の1:1移植として、reduced-motion時は待機をなくす。
struct OnboardingView: View {
    let store: RecordStore
    let onComplete: (_ route: String, _ presetWorry: String?) -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var bubbles: [ChatBubble] = []
    @State private var activeQuestion: ObQuestionDef?
    @State private var routeCta: ObRouteInfo?
    @State private var answers: [String: String] = [:]
    @State private var pendingPick: CheckedContinuation<ObChip, Never>?
    @State private var pendingCta: CheckedContinuation<Void, Never>?

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
        let wait: UInt64 = reduceMotion ? 0 : 1_500_000_000
        for line in lines {
            bubbles.append(ChatBubble(text: line, fromUser: false))
            // アクセシビリティ対応(スクリーンリーダー無音問題の解消): 吹き出しを配列に積んだ
            // その瞬間だけ、その1件のテキストをVoiceOverへ通知する。post(.announcement,...)は
            // 「このView階層のどこかのregionが変わったので中身を全部読み直す」方式ではなく、
            // 呼び出し側が渡した文字列そのものを1回読み上げるだけの命令的API。したがって
            // 過去に出た吹き出し(bubbles中の既存要素)には一切触れず、会話の再読み上げは起きない。
            UIAccessibility.post(notification: .announcement, argument: line)
            try? await Task.sleep(nanoseconds: wait)
        }
    }

    private func awaitPick() async -> ObChip {
        await withCheckedContinuation { cont in
            pendingPick = cont
        }
    }

    private func awaitCta() async {
        await withCheckedContinuation { cont in
            pendingCta = cont
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
            // アクセシビリティ対応: ユーザーが選んだチップの吹き出しも同様に、追加された瞬間だけ通知する。
            UIAccessibility.post(notification: .announcement, argument: picked.label)
            if q.key == "anchor" {
                await say([obAnchorAck[picked.v] ?? "OK！おぼえたよ📝"])
            } else if q.key == "bigtext" {
                await say([obBigtextAck])
            }
        }
        // index.html:4108-4111 ONBOARDING_SCRIPT.routes: 相槌の後にもう1往復、締めメッセージ+
        // 専用ボタンを表示し、タップされて初めてfinish()(=画面遷移)する(自動遷移しない)。
        let stiff = answers["stiff"] ?? "normal"
        let worry = answers["worry"] ?? "none"
        let routeInfo = obRoutes[obDecideRoute(stiff: stiff, worry: worry)]!
        await say(routeInfo.say)
        routeCta = routeInfo
        await awaitCta()
        routeCta = nil
        finish()
    }

    private var themeSetting: String { store.get("theme", default: "auto") }

    var body: some View {
        KyonoTheme(themeSetting: themeSetting, bigText: store.get("bigtext", default: true)) {
            OnboardingContentView(
                bubbles: bubbles, activeQuestion: activeQuestion, routeCta: routeCta,
                onChipTap: { chip in
                    pendingPick?.resume(returning: chip)
                    pendingPick = nil
                },
                onCtaTap: {
                    pendingCta?.resume(returning: ())
                    pendingCta = nil
                }
            )
        }
        .task { await runFlow() }
    }
}

private struct OnboardingContentView: View {
    @Environment(\.kyonoColors) private var colors
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    let bubbles: [ChatBubble]
    let activeQuestion: ObQuestionDef?
    let routeCta: ObRouteInfo?
    let onChipTap: (ObChip) -> Void
    let onCtaTap: () -> Void

    private var dark: Bool { colors.bg == kyonoDarkColors.bg }

    var body: some View {
        // TASK-C2-2026-07-30-onboarding-scroll-and-copy.md A2: TourContentView(D6)と同じ構造。
        // 選択肢・CTAボタンを本文と同じScrollViewから外し、外側VStackの固定フッターにする。
        // これでCTAは常に画面内の同じ位置にあり、本文の長さに関わらず動かない。
        VStack(spacing: 0) {
        ScrollViewReader { proxy in
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text("🌱 はじめてガイド").kyonoFont(.black900, size: 16).foregroundColor(colors.ink)
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
                        Text(b.text).kyonoFont(.bold700, size: 15).foregroundColor(colors.ink).lineSpacing(11)
                            .padding(.horizontal, 14).padding(.vertical, 10)
                            .background(shape.fill(b.fromUser ? colors.yellowSoft : colors.card))
                            .overlay(shape.stroke(b.fromUser ? Color.clear : colors.line, lineWidth: 1.5))
                        if !b.fromUser { Spacer(minLength: 40) }
                    }
                    .transition(.sdPop)
                }
                Color.clear.frame(height: 1).id("obBottom")
            }
            .padding(20)
            // TASK-C2-2026-07-27-chips-overflow-and-bubble-pop.md §3: index.html:4149 .sd-pop
            // (opacity0→1・translateY(4px)→0・.18s ease-out)の1:1移植。reduced-motion時は無演出即表示。
            .animation(reduceMotion ? nil : .easeOut(duration: 0.18), value: bubbles.count)
        }
        // TASK-C2-2026-07-30-onboarding-scroll-and-copy.md A1: 固定60ms delay後に1回だけ
        // scrollToする実装だと、バブルのポップイン(180ms)と競合し、レイアウト確定前に着地する
        // ことがあった(スクロールが上がりきらない)。SoudanSheetView.swift:500-518の手法を移植:
        // delayを使わず、状態変化のたびに即座に(同じフレーム内で)scrollTo(anchor:)を呼ぶ。
        // SwiftUIはwithAnimationで包まれたレイアウト変化とscrollTo自体を同じアニメーションとして
        // 解決するため、事前に「レイアウトが確定するのを待つ」猶予は不要。オンボは相談室と違って
        // 全部ユーザー操作起点の追加のため、bot発言用の.top/ユーザー発言用.bottomという使い分けは
        // 不要で、常に.bottomでよい。選択肢・CTAはA2でスクロール領域の外(固定フッター)に出た
        // ため、ここでスクロール対象にする必要があるのはbubblesの増減だけになった。
        .onChange(of: bubbles.count) { _, _ in scrollToBottom(proxy) }
        }
        // A2: 選択肢・CTAは固定フッター(スクロールしない)。
        if let q = activeQuestion {
            VStack(alignment: .leading, spacing: 10) {
                Text("👇 タップしてえらんでね").kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)
                let palette = obgColors(dark: dark)
                ForEach(Array(q.chips.enumerated()), id: \.offset) { i, chip in
                    let c = palette[i % 4]
                    // TASK-C2-2026-07-30-icon-system-addendum-chips.md: 部位・時間帯チップの
                    // 生成イラスト。ChipArt/chip-<v>.pngが存在するものだけ表示する
                    // (硬さチェック6タイプ=KyonoTypeArtはこの対象外・触らない)。
                    HStack(spacing: 10) {
                        if let url = Bundle.main.url(forResource: "chip-\(chip.v)", withExtension: "png"),
                           let uiImage = UIImage(contentsOfFile: url.path) {
                            Image(uiImage: uiImage).resizable().scaledToFit().frame(width: 32, height: 32)
                        }
                        Text(chip.label).kyonoFont(.bold700, size: 16).foregroundColor(colors.ink)
                    }
                        .frame(maxWidth: .infinity)
                        .padding(.horizontal, 18).padding(.vertical, 14)
                        .background(RoundedRectangle(cornerRadius: 16).fill(c.bg))
                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(c.border, lineWidth: 2))
                        .onTapGesture { onChipTap(chip) }
                }
            }
            .padding(.horizontal, 20).padding(.bottom, 20)
        }
        if let cta = routeCta {
            KyonoPrimaryButton(cta.btn, action: onCtaTap)
                .padding(.horizontal, 20).padding(.bottom, 20)
        }
        }
        .background(KyonoBackgroundColor().ignoresSafeArea())
    }

    private func scrollToBottom(_ proxy: ScrollViewProxy) {
        if reduceMotion {
            proxy.scrollTo("obBottom", anchor: .bottom)
        } else {
            withAnimation { proxy.scrollTo("obBottom", anchor: .bottom) }
        }
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
    let pt: String
    let area: String
}

// app-quiz.js:45-79 TYPES の1:1移植(name/copy/hope/pt/area。rx/poolは動画選出専用データのため
// typeRxPoolへ別に切り出す)。areaはTASK-C2-2026-07-26-result-video-recommendations.md(#result
// のrxHead文言生成)で追加。
let quizTypes: [String: TypeInfo] = [
    "momo": TypeInfo(
        name: "つっぱりモモンガ",
        copy: "前屈すると、つま先がとても遠い。それはあなたの脚が長い…わけではなく、もも裏がモモンガの滑空ポーズみたいにピンとつっぱっているサイン。",
        hope: "でもモモンガも、着地すればちゃんと脚をゆるめます。もも裏は変化が出やすい場所。2週間後の前屈で、床がぐっと近くなってるはず。",
        pt: "硬いのは<b>ハムストリングス（もも裏の筋肉）</b>。ここが硬いと骨盤が後ろに倒れたまま固定され、前屈で腰だけが無理に曲がります。放っておくと<b>腰痛や座り姿勢の悪化</b>につながる場所。逆に言えば、もも裏をゆるめるだけで前屈も腰もラクになります。",
        area: "もも裏"
    ),
    "koka": TypeInfo(
        name: "開かずのトビラ",
        copy: "あぐらでひざが山になるのは、股関節のとびらが閉まっているから。股関節の封印は解きたいですよね。",
        hope: "とびらは、毎日すこしずつ油をさせば開きます。股関節は9分の習慣がいちばん効く場所。あせらずコツコツ。",
        pt: "硬いのは<b>内もも（内転筋）とお尻（大臀筋・梨状筋）</b>。股関節を外に開く動きが制限されて、あぐら・開脚が苦手になります。股関節は体の土台なので、ここが動くと<b>歩く・座る・立つ全部がラクに</b>。腰への負担も減ります。",
        area: "股関節"
    ),
    "kenko": TypeInfo(
        name: "飛べないダチョウ",
        copy: "ひじをつけたまま上がらないのは、肩甲骨まわりの羽根が飛べないダチョウみたいに、すっかり休眠しているから。デスクワークの勲章です。",
        hope: "ダチョウの羽根だって、バサバサ動かせば血が巡ります。肩甲骨がゆるむと、肩こりも呼吸もぐっとラクに。",
        pt: "硬いのは<b>肩甲骨まわり（僧帽筋・広背筋・大胸筋など）</b>。肩甲骨の動きが小さくなると、首と肩の筋肉が代わりに働き続けて<b>肩こり・巻き肩・浅い呼吸</b>の原因に。肩甲骨を動かす習慣がつくと、背中が軽くなって姿勢も変わります。",
        area: "肩甲骨"
    ),
    "ashi": TypeInfo(
        name: "棒立ちペンギン",
        copy: "しゃがむとかかとがプカッ あるいは後ろにコロン。それは足首がカチッと固まっている証拠。ペンギンは可愛いけど、転ぶと痛い。",
        hope: "足首がゆるむと、歩くのも立つのも軽くなります。つまむだけの簡単ストレッチから始めましょう。",
        pt: "硬いのは<b>足首の背屈（すねに向けて曲げる動き）＝ふくらはぎ・アキレス腱まわり</b>。ここが硬いと、しゃがむ動作でかかとが浮き、<b>つまずき・むくみ・ふくらはぎの張り</b>につながります。足首は毎日使う関節なので、ゆるめた効果を実感しやすい場所です。",
        area: "足首"
    ),
    "robot": TypeInfo(
        name: "ガチガチロボット",
        copy: "全体的に、ガチガチ。でも言いかえれば、どこを伸ばしても効く「伸びしろの宝庫」ということ。",
        hope: "ロボットにも心はあります。全身をやさしくほぐす1本から始めれば、ガチガチの体もちゃんと応えてくれます。",
        pt: "特定の場所というより<b>全身が複合的に硬い状態</b>。この場合は部位を絞るより、全身をまんべんなく動かすルーティンで底上げするのが近道です。<b>どこを伸ばしても効く＝変化を感じやすい</b>ので、実はいちばん楽しいスタート地点だったりします。",
        area: "全身"
    ),
    "yawara": TypeInfo(
        name: "しなやかネコ",
        copy: "おっと、けっこうしなやか！あなたはもう「しなやかネコ」。ここから先は、そのしなやかさを守るステージです。",
        hope: "しなやかさは資産。猫が毎朝伸びをするみたいに、朝と夜の習慣で守っていきましょう。悩みに合わせた1本もどうぞ。",
        pt: "関節の可動域は良好です。次の課題は<b>「維持」と「使い方」</b>。柔らかくても、支える筋力や毎日の習慣が崩れると体は硬さに戻ります。朝晩の軽いルーティンで可動域を守りつつ、悩みのある部位を先回りでケアしましょう。",
        area: "メンテナンス"
    ),
]

// 診断結果画面「おすすめ動画3本」欠落修正タスク(TASK-C2-2026-07-26-result-video-recommendations.md):
// app-quiz.js:45-90 TYPES[].rx/poolの1:1移植(動画選出専用データ。§1-2に基づき機械抽出)。
struct TypeRxPool { let rx: [String]; let pool: [String] }
let typeRxPool: [String: TypeRxPool] = [
    "momo": TypeRxPool(rx: ["momo7"], pool: ["kaikyaku", "momoKai", "momoIsho", "zenkutsu15", "hamu10", "kaikyaku2", "kotsuban5", "yotsu12", "yotsu8", "asa10", "nagomi7"]),
    "koka": TypeRxPool(rx: ["koka9"], pool: ["kominka", "kokaSai", "koka22", "koka3cho", "kokaIsho", "kokaPoki", "kaikyaku", "kaikyaku90", "nagomi7", "ashisuki", "yotsu12"]),
    "kenko": TypeRxPool(rx: ["kenko12"], pool: ["asa5", "kenkoIsho", "kenko22", "kenkoIsho2", "kenko3cho", "katakori", "katakori8", "zutsu7", "suwatta8", "nagomi7"]),
    "ashi": TypeRxPool(rx: ["ashi1"], pool: ["ashi2", "ashi10", "ashi3cho", "ashiIsho", "fukura5", "fukuraMassa", "fukura8", "ashi4", "katai8st", "ashisuki"]),
    "robot": TypeRxPool(rx: ["honki9"], pool: ["asa10", "asaBaki9", "yoru9umi", "yoru9ice", "zenshinCho", "yoru12kai", "senaka5", "ofuro10", "nagomi7"]),
    "yawara": TypeRxPool(rx: ["asa10"], pool: ["asa9shi", "asaGachi5", "asa3", "honki9", "yoru9umi", "jukusui9", "jiritsu10", "ofuro6", "choyokin10", "ibuki10", "nagomi7"]),
]

// app-quiz.js:81-85 WORRYの1:1移植(悩みキー→追加のおすすめ1本+ラベル。yawaraka=null相当は
// マップに含めないことで表現)。
struct WorryExtra { let v: String; let label: String }
let worryExtraMap: [String: WorryExtra] = [
    "katakori": WorryExtra(v: "katakori", label: "肩こりさんへ もう1本"),
    "yotsu": WorryExtra(v: "yotsu12", label: "腰痛さんへ もう1本"),
    "tsukare": WorryExtra(v: "jiritsu10", label: "おつかれさんへ もう1本"),
]

// index.html:1458 V(かたさタイプおすすめ動画専用の小規模動画カタログ)のキー→YouTube動画IDの
// 1:1移植(§1-2に基づき機械抽出)。タイトル・サブタイトル等の実体はすでに移植済みの一般カタログ
// (catalog.json/CatalogLoader)に同じ動画IDが含まれているため、そちらを検索して表示に使う
// (V自体のt/s/tagsは重複移植しない)。
let quizVideoKeyToId: [String: String] = [
    "momo7": "CyWthETY73s", "momoKai": "3_z8R2l4CKE", "kaikyaku": "Re5FPU5_37g", "asa10": "2EfFlQev4rg",
    "koka9": "-Y5bOC_ecB0", "kominka": "LMz4DV66bV8", "kenko12": "ZYTlwh_FhoU", "yoru15": "HCVb47eWgqA",
    "asa5": "VTMYfFnkHh4", "ashi1": "6U4fgJu0ZMw", "ashi2": "86u3S-epkRg", "ashi10": "t3C-N5_828k",
    "fukura5": "gdvjMR61Z4k", "honki9": "q8jr0KhoML4", "yaruki22": "oV0Rqt76bhM", "yoru9umi": "NrJIhK_gOXc",
    "yoru9ice": "_2g_qWssAEI", "asa9shi": "H9ctJbhTR0Y", "jiritsu10": "XkgsF39kkRw", "ashisuki": "4SsJx5W8hNQ",
    "katakori": "7FY6SR6cyts", "yotsu12": "vZ4LYE0Ahe8", "nagomi7": "aIIU5R2l-kQ", "momoIsho": "CnxxUFl373A",
    "zenkutsu15": "0-LT6LWLwOQ", "hamu10": "7LgLQuHx-DI", "kaikyaku2": "P6-GHA1AuwE", "kotsuban5": "3F53Us-nwDY",
    "yotsu8": "laNHVUwdxZM", "kokaSai": "0jhnX8BPzes", "koka22": "uG2_e0Y7qkw", "koka3cho": "Imgtayb1v78",
    "kokaIsho": "3br07_9ZbyQ", "kokaPoki": "_ETT9HRUxQE", "kaikyaku90": "2gb2LlmK5XQ", "kenkoIsho": "LdnJXMB2kZs",
    "kenko22": "Qxqcjj_k0WE", "kenkoIsho2": "xhloKtNFgeQ", "kenko3cho": "lUOSasCDvM8", "katakori8": "Sw5MvxmAoGg",
    "zutsu7": "8rOq_AqiNaw", "suwatta8": "bzGMeDoGpeA", "ashi3cho": "cs1A8W_HofI", "ashiIsho": "8vftEiHldF8",
    "fukuraMassa": "uy4loFazBgM", "fukura8": "vVNi7jhGBpU", "ashi4": "nkvn6zyYx08", "katai8st": "B-vdrGt8hlA",
    "zenshinCho": "NWl4iQSpkgw", "yoru12kai": "9mCCZ39Gb5c", "ofuro20": "JdPVMVfmdzc", "zenshin15": "VDy2XlF9EBE",
    "senaka5": "aSrdZ4aNRmg", "ofuro10": "JIOnn1-NSHM", "asaBaki9": "0wZ5nElZaRA", "asaGachi5": "gMIlRS_lbYA",
    "jukusui9": "09C7ti0xY4k", "ofuro6": "WvnX_RsX_jY", "choyokin10": "HCLVdX5esK0", "asa3": "ZVSkWhJVlfk",
    "ibuki10": "mRz5ZZAi9dU", "ogaRadio6": "6jSlocilSYk", "asa10kesen": "Jz7WdjFV5aw", "neochi10": "TfkPz1DNK2Y",
]

// app-quiz.js:238-255 currentRx()の1:1移植(乱数不使用・rotationIndexのみで決定的な動画選出)。
// CardLottery.rotationIndex(既存・CardCoreで移植済み)を再利用するだけで、選出ロジック自体は
// ここで新規実装するが判定/安全ロジックではないため§3-2の対象外(表示用の推薦リスト生成)。
func currentRx(_ typeKey: String, now: Date) -> [String] {
    guard let t = typeRxPool[typeKey] else { return [] }
    let need = 3 - t.rx.count
    guard need > 0, !t.pool.isEmpty else { return Array(t.rx.prefix(3)) }
    let r = CardLottery.rotationIndex(now: now)
    let spacing = t.pool.count / (need == 3 ? 3 : 2)
    var picks: [String] = []
    for i in 0..<need {
        var idx = (r + i * spacing) % t.pool.count
        var tries = 0
        while (t.rx.contains(t.pool[idx]) || picks.contains(t.pool[idx])) && tries < t.pool.count {
            idx = (idx + 1) % t.pool.count
            tries += 1
        }
        picks.append(t.pool[idx])
    }
    return t.rx + picks
}

// 全画面完全性監査タスク #result: index.html:2976 SOUDAN_TYPE_INTENT(タイプ→相談室プリセット
// intentId)の1:1移植。最初の1件のみ使う(Web版と同じ)。
let soudanTypeIntent: [String: String] = [
    "momo": "zenkutsu", "koka": "kokansetsu", "kenko": "katakori", "ashi": "ashikubi", "robot": "zenshin",
]

// index.html <b>タグの簡易リッチテキスト化(app-quiz.js TYPES[].ptの太字表現)。判定・データ構造では
// なく表示専用の変換のためロジック層には置かない。
private func boldHtmlText(_ raw: String, bold: Color) -> Text {
    var result = Text("")
    var rest = raw
    while true {
        guard let startRange = rest.range(of: "<b>") else { result = result + Text(rest); break }
        result = result + Text(rest[rest.startIndex..<startRange.lowerBound])
        let afterOpen = String(rest[startRange.upperBound...])
        guard let endRange = afterOpen.range(of: "</b>") else { result = result + Text(afterOpen); break }
        // Text連結(+)はText型のみ許容するため、ここだけ.kyonoFont(ViewModifier)ではなく
        // .font(.kyono(...))を直接使う(bigtextの1.18倍はこの1箇所のみ非適用・影響は軽微)。
        result = result + Text(afterOpen[afterOpen.startIndex..<endRange.lowerBound]).font(.kyono(.black900, size: 14)).foregroundColor(bold)
        rest = String(afterOpen[endRange.upperBound...])
    }
    return result
}

struct QuizTypeResult: Codable {
    let key: String
    let worry: String?
    let at: String
}

// app-quiz.js:145-153 activeQuestions()・194+ decideType呼び出し部分の1:1移植。判定そのものは
// QuizEngine.decideType(Step4で移植済み)を呼ぶだけで、ここでは一切再実装しない
// (マスタープラン§6 Step5c検収基準2)。presetWorryがあるときはQ5(worry)を出題しない。
// 全画面完全性監査タスク(TASK-C2-2026-07-26-full-completeness-audit.md #result): app-quiz.js:211
// REACH_FROM_MOMO(Q1の回答index→とどくメーター段位への対応表)の1:1移植。
private let reachFromMomo = [5, 4, 2, 1]

struct QuizView: View {
    let store: RecordStore
    let presetWorry: String?
    let onComplete: (_ typeKey: String, _ autoReachLv: Int?) -> Void
    let onGoHome: () -> Void

    private let activeQuestions: [QuizQuestionDef]
    @State private var qi = 0
    @State private var scores: [String: Int] = [:]
    @State private var worry: String?
    // 全画面完全性監査タスク(TASK-C2-2026-07-26-full-completeness-audit.md #quiz):
    // index.html:1649 quizGoHome()の「回答済みなら確認ダイアログ」の1:1移植。
    @State private var showGoHomeConfirm = false
    // TASK-C2-2026-07-28-quiz-result-reach-parity.md §2: app-quiz.js:180の1:1移植。回答タップ直後に
    // 選択肢を無効化し、次の設問が描画されるまで二度押しで判定の入力が汚れるのを防ぐ。
    @State private var answering = false
    // TASK-C2-2026-07-28-quiz-result-reach-parity.md §5: app-quiz.js:166 state.pickedの1:1移植。
    // 「まえの質問へ」で戻ったとき前回選んだ選択肢が分かるよう、質問key→選択値(scoreまたはworryKey)を覚えておく。
    @State private var picked: [String: String] = [:]

    init(store: RecordStore, presetWorry: String?, onComplete: @escaping (String, Int?) -> Void, onGoHome: @escaping () -> Void) {
        self.store = store
        self.presetWorry = presetWorry
        self.onComplete = onComplete
        self.onGoHome = onGoHome
        self.activeQuestions = presetWorry != nil ? quizQuestions.filter { $0.key != "worry" } : quizQuestions
        _worry = State(initialValue: presetWorry)
    }

    private var themeSetting: String { store.get("theme", default: "auto") }

    var body: some View {
        KyonoTheme(themeSetting: themeSetting, bigText: store.get("bigtext", default: true)) {
            QuizContentView(
                activeQuestions: activeQuestions, qi: qi, answering: answering, picked: picked,
                onOptTap: { q, opt in
                    guard !answering else { return }
                    answering = true
                    picked[q.key] = opt.score.map { String($0) } ?? opt.worryKey
                    if let score = opt.score { scores[q.key] = score }
                    if let worryKey = opt.worryKey { worry = worryKey }
                    qi += 1
                    if qi >= activeQuestions.count {
                        let s = QuizEngine.Scores(momo: scores["momo"] ?? 0, koka: scores["koka"] ?? 0, kenko: scores["kenko"] ?? 0, ashi: scores["ashi"] ?? 0)
                        let typeKey = QuizEngine.decideType(s, worry: worry, now: Date())
                        store.set("type", QuizTypeResult(key: typeKey, worry: worry, at: RecordLogic.todayStr(now: Date())))
                        // app-quiz.js:223-234 finishQuiz()の自動転記(A案)の1:1移植: とどくメーターが
                        // まだ1件も無ければ、Q1(momo)の回答を初回記録として自動で書きこむ
                        // (ユーザーが自分で測った値があるときは上書きしない)。
                        var autoReachLv: Int?
                        if RecordLogic.getReach(store).isEmpty, let momoIdx = scores["momo"], reachFromMomo.indices.contains(momoIdx) {
                            autoReachLv = reachFromMomo[momoIdx]
                        }
                        if let lv = autoReachLv {
                            RecordLogic.setReach(store, lv: lv, now: Date())
                        }
                        onComplete(typeKey, autoReachLv)
                    }
                },
                onBack: { if qi > 0 { qi -= 1 } },
                onGoHomeTap: { if qi > 0 { showGoHomeConfirm = true } else { onGoHome() } }
            )
        }
        .onChange(of: qi) { _, _ in answering = false }
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
    let answering: Bool
    let picked: [String: String]
    let onOptTap: (QuizQuestionDef, QuizOptDef) -> Void
    let onBack: () -> Void
    let onGoHomeTap: () -> Void

    private var dark: Bool { colors.bg == kyonoDarkColors.bg }

    var body: some View {
        // TASK-C2-2026-07-30-onboarding-scroll-and-copy.md A2: TourContentView(D6)と同じ構造。
        // 「まえの質問へ」「ホームにもどる」を本文と同じScrollViewから外し、外側VStackの
        // 固定フッターにする。これでCTAは常に画面内の同じ位置にあり、本文の長さ(選択肢の
        // note文の折返し行数など)に関わらず動かない。
        VStack(spacing: 0) {
        ScrollView {
            VStack(alignment: .leading, spacing: 8) {
                Text("かたさチェック").kyonoFont(.black900, size: 16).foregroundColor(colors.ink)
                Text("Q\(qi + 1) / \(activeQuestions.count)").kyonoFont(.black900, size: 12).foregroundColor(colors.sub)
                // TASK-C2-2026-07-28-quiz-result-reach-parity.md §5: index.html:719 .dots+
                // app-quiz.js:175-176の1:1移植。ツアー画面にはドットがあるのにクイズには無かった欠落。
                HStack(spacing: 6) {
                    ForEach(activeQuestions.indices, id: \.self) { i in
                        Circle().fill(i <= qi ? colors.pink : colors.line).frame(width: 9, height: 9)
                    }
                }
                if qi < activeQuestions.count {
                    let q = activeQuestions[qi]
                    Spacer().frame(height: 4)
                    Text(q.title).kyonoFont(.black900, size: 18).foregroundColor(colors.ink)
                    Text(q.note).kyonoFont(.bold700, size: 13).foregroundColor(colors.sub)
                    if let artResName = q.artResName {
                        Image(artResName).resizable().scaledToFit()
                            .background(colors.bg).cornerRadius(16)
                    } else if q.key == "kenko" {
                        Spacer().frame(height: 10)
                        QuizArtKenko()
                    } else if q.key == "ashi" {
                        Spacer().frame(height: 10)
                        QuizArtAshi()
                    }
                    // 全画面完全性監査タスク #quiz: index.html:717 .tap-hint(タップ誘導文言)の1:1移植。
                    Spacer().frame(height: 6)
                    Text("👇 タップしてえらんでね").kyonoFont(.black900, size: 13).foregroundColor(colors.ink)
                    Spacer().frame(height: 4)
                    // index.html:293-309 .opt/.opt.g0〜g3(明→暗の段階色カード)の1:1移植。
                    // TASK-C2-2026-07-28-quiz-result-reach-parity.md §5: app-quiz.js:168-169
                    // 「段階色は数値スコアの設問(Q1-Q4)だけ」の1:1移植。Q5(worry)はscore==nilのため
                    // 段階色を付けず、通常のカード色(colors.card/colors.line)にする。
                    let palette = obgColors(dark: dark)
                    ForEach(Array(q.opts.enumerated()), id: \.offset) { i, opt in
                        let c = opt.score != nil ? palette[i % 4] : nil
                        let pickedVal = opt.score.map { String($0) } ?? opt.worryKey
                        // app-quiz.js:171 .opt.on(前回選んだ選択肢に枠色)の1:1移植。
                        let isPicked = picked[q.key] == pickedVal
                        // UI/UXパリティ監査GO-2(2026-07-28)・視点D確信度CONFIRMED: 素の
                        // VStack{...}.onTapGestureのみで押下時の見た目変化が一切無く、新規ユーザーが
                        // 最初に触る5問クイズがタップしても無反応に見えていた欠落。index.html:295
                        // .opt:active{background:var(--yellow-soft);border-color:var(--yellow)}の
                        // 1:1移植(KyonoPrimaryButtonと同じDragGesture+@State pressedの手法を展開)。
                        QuizOptionCard(
                            label: opt.label, note: opt.note,
                            background: c?.bg ?? colors.card,
                            borderColor: isPicked ? colors.teal : (c?.border ?? colors.line),
                            pressedBackground: colors.yellowSoft, pressedBorderColor: colors.yellow,
                            colors: colors
                        ) { if !answering { onOptTap(q, opt) } }
                    }
                }
            }
            .padding(20)
        }
        // TASK-C2-2026-07-30-onboarding-scroll-and-copy.md A3: 「まえの質問へ」(前の設問に戻る)と
        // 「ホームにもどる」(診断を中断してホームに戻る=破壊的操作)が同じKyonoLineButton(枠線のみ)
        // スタイルで並んでいて見分けにくかった。「まえの質問へ」は枠線ボタンのまま残し、
        // 「ホームにもどる」はSettingsView.swift:157-160の「変える」リンクと同じ見た目(文字だけ・
        // 控えめな色)に格下げして、機能差を視覚的に出す。
        VStack(spacing: 8) {
            // 全画面完全性監査タスク #quiz: index.html:720 #qBackBtn(Q1以外で表示)の1:1移植。
            if qi > 0 {
                KyonoLineButton("← まえの質問へ", action: onBack)
            }
            // 全画面完全性監査タスク #quiz: index.html:721 「ホームにもどる」ボタンの1:1移植。
            Text("ホームにもどる")
                .kyonoFont(.bold700, size: 14).foregroundColor(colors.sub)
                .padding(.vertical, 10)
                .onTapGesture { onGoHomeTap() }
        }
        .padding(.horizontal, 20).padding(.bottom, 20)
        }
        .background(KyonoBackgroundColor().ignoresSafeArea())
    }
}

// UI/UXパリティ監査GO-2(2026-07-28): index.html:293-295 .opt/.opt:activeの1:1移植。
// KyonoPrimaryButtonと同じDragGesture+@State pressedの手法で、押した瞬間だけ
// background/borderをyellow-soft/yellowへ切り替える(transitionなし=CSS同様の瞬時切り替え)。
private struct QuizOptionCard: View {
    let label: String
    let note: String
    let background: Color
    let borderColor: Color
    let pressedBackground: Color
    let pressedBorderColor: Color
    let colors: KyonoColors
    let action: () -> Void
    // UI/UXパリティ監査GO-3(iOS・2026-07-29): KyonoCardと同じズーム対応。
    @Environment(\.kyonoBigText) private var bigText
    @State private var pressed = false

    private var zoom: CGFloat { bigText ? kyonoBigTextScale : 1 }

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            // UI/UXパリティ監査2巡目A4(2026-07-29): index.html:294 .opt{font-size:18px}の1:1移植。
            // 従来15ptで-16.7%小さく値がズレていた欠落を修正する。
            Text(label).kyonoFont(.black900, size: 18).foregroundColor(colors.ink)
            // UI/UXパリティ監査2巡目A1(2026-07-29): index.html:297 .opt .crit{line-height:1.5}の
            // 1:1移植。前回G2は検索チップのみに適用していたカスタムフォント行送り超過補正をここにも展開する。
            Text(note).kyonoFont(.bold700, size: 13).foregroundColor(colors.sub).lineSpacing(7)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16 * zoom).padding(.vertical, 14 * zoom)
        .background(RoundedRectangle(cornerRadius: 16 * zoom).fill(pressed ? pressedBackground : background))
        .overlay(RoundedRectangle(cornerRadius: 16 * zoom).stroke(pressed ? pressedBorderColor : borderColor, lineWidth: 2 * zoom))
        .contentShape(Rectangle())
        .gesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in pressed = true }
                .onEnded { _ in pressed = false; action() }
        )
        // TASK-C2-2026-07-27-text-size-accessibility.md 項目4: 選択肢の見出し+補足説明を
        // 1回のVoiceOverスワイプで読める1つの単位にまとめる。
        .accessibilityElement(children: .combine)
    }
}

// ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
// Phase 3: index.html:726-735 #result .card.grad-soft/.type-name/.type-copy/.type-hopeの1:1移植。
// 全画面完全性監査タスク(TASK-C2-2026-07-26-full-completeness-audit.md #result)で
// rPT/rReachNote/rPace/hint/rRecheckBtn/rSoudanLinkを追加。rxList/rDoneNudge/rTourBtn(動画レコメンド
// 連動・オンボ→ツアー導線)は動画カタログ非依存ではないため別タスクのfollow-upとして切り出す。
struct ResultView: View {
    let store: RecordStore
    let typeKey: String
    let autoReachLv: Int?
    let showTourBtn: Bool
    let openUrl: (String) -> Void
    let onDone: () -> Void
    // TASK-C2-2026-07-27-scroll-parity-and-reduced-motion-gaps.md §C補足(alan5指摘): index.html:3991
    // rDoneNudgeBtn経由の1:1移植。呼び出し元(KyouNoOgatoreApp)からはHome側のshowDoneNudgeも
    // 立てる版を渡してもらう。
    var onDoneFromNudge: (() -> Void)? = nil
    let onStartQuiz: () -> Void
    let onOpenSoudan: (String?) -> Void
    let onStartTour: () -> Void

    // ダークモード再確認+rDoneNudge/rTourBtn実装タスク(TASK-C2-2026-07-27-darkmode-recheck-and-
    // nudges.md): index.html:3958-3969 rDoneNudge用タップ検知(#result内のa.videoクリックで
    // pendingNudgeを立てる)+index.html:3970-4001 checkDoneNudge()の「結果画面表示中」分岐の1:1移植。
    // HomeViewの既存の同種ロジック(pendingNudgeDate/showDoneNudge)とは独立させ、既存の
    // 壊れやすい仕組みには一切触れない。
    @State private var pendingNudgeDate: String?
    @State private var showDoneNudge = false
    @Environment(\.scenePhase) private var scenePhase

    private var info: TypeInfo { quizTypes[typeKey] ?? TypeInfo(name: typeKey, copy: "", hope: "", pt: "", area: "") }
    // 診断結果画面「おすすめ動画3本」欠落修正タスク(TASK-C2-2026-07-26-result-video-recommendations.md):
    // app-quiz.js:238-255 currentRx()の1:1移植呼び出し。日付のみで決まる(乱数不使用)。
    private var rx: [String] { currentRx(typeKey, now: Date()) }
    private var worry: String? {
        let result: QuizTypeResult? = store.get("type", default: nil)
        return result?.worry
    }
    private var fdGuideActive: Bool {
        let fd: String? = store.get("fd", default: nil)
        return HomeLogic.fdActive(fd: fd, streakTotal: RecordLogic.loadStreak(store).total)
    }

    var body: some View {
        // ResultViewはRecordStoreを従来受け取らなかったが、rSoudanLinkの遷移先(onOpenSoudan)や
        // worry(WORRY_EXTRA用)を読むために保持する。テーマ設定はシステムのダークモードに委ねる("auto"扱い)。
        KyonoTheme(themeSetting: "auto", bigText: store.get("bigtext", default: true)) {
            content
        }
        .onChange(of: scenePhase) { _, newPhase in
            guard newPhase == .active else { return }
            let today = RecordLogic.todayStr(now: Date())
            let dates = RecordLogic.loadStreak(store).dates
            if HomeLogic.shouldShowDoneNudge(pendingNudgeDate: pendingNudgeDate, today: today, streakDates: dates) {
                showDoneNudge = true
            }
            pendingNudgeDate = nil
        }
    }

    private var content: some View {
        ResultContentView(
            info: info, typeKey: typeKey, autoReachLv: autoReachLv, rx: rx, worry: worry,
            showDoneNudge: showDoneNudge, fdGuideActive: fdGuideActive, showTourBtn: showTourBtn,
            onVideoTap: { url in pendingNudgeDate = RecordLogic.todayStr(now: Date()); openUrl(url) },
            openUrl: openUrl, onDone: onDone, onDoneFromNudge: onDoneFromNudge ?? onDone,
            onStartQuiz: onStartQuiz, onOpenSoudan: onOpenSoudan, onStartTour: onStartTour
        )
    }
}

private struct ResultContentView: View {
    @Environment(\.kyonoColors) private var colors
    let info: TypeInfo
    let typeKey: String
    let autoReachLv: Int?
    let rx: [String]
    let worry: String?
    let showDoneNudge: Bool
    let fdGuideActive: Bool
    let showTourBtn: Bool
    let onVideoTap: (String) -> Void
    let openUrl: (String) -> Void
    let onDone: () -> Void
    let onDoneFromNudge: () -> Void
    let onStartQuiz: () -> Void
    let onOpenSoudan: (String?) -> Void
    let onStartTour: () -> Void

    private var catalogById: [String: CatalogVideo] {
        Dictionary(uniqueKeysWithValues: CatalogLoader.shared.map { ($0.id, $0) })
    }
    private func lookupVideo(_ key: String) -> CatalogVideo? {
        quizVideoKeyToId[key].flatMap { catalogById[$0] }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                KyonoGradientCard(gradient: .soft) {
                    Text("あなたのかたさタイプは…").kyonoFont(.black900, size: 14).foregroundColor(colors.sub)
                        .frame(maxWidth: .infinity, alignment: .center)
                    Spacer().frame(height: 10)
                    // index.html:317-318,729 .type-illust(104x104・中央寄せ)の1:1移植。
                    KyonoTypeArt(typeKey: typeKey).frame(maxWidth: .infinity, alignment: .center)
                    Spacer().frame(height: 4)
                    Text(info.name).kyonoFont(.black900, size: 29).foregroundColor(colors.ink)
                        .frame(maxWidth: .infinity, alignment: .center)
                    Spacer().frame(height: 8)
                    Text(info.copy).kyonoFont(.bold700, size: 15).foregroundColor(colors.sub)
                        .frame(maxWidth: .infinity, alignment: .center)
                    // TASK-C2-2026-07-28-quiz-result-reach-parity.md §1: app-quiz.js:291-299の1:1移植。
                    // ガイド中(fdGuideActive)は結果画面を「タイプ+①」に削ぎ落とす(2026-07-21 5視点
                    // 検証C・PO承認済み)。長文解説(rHope/rPT)・ペース目安(rPace)・②③・相談室リンクは
                    // 翌日以降(通常表示)に回す(一本道=①をタップ→もどる→記録、以外の分岐を見せない)。
                    if !fdGuideActive {
                        Spacer().frame(height: 12)
                        Text("🌱 " + info.hope).kyonoFont(.bold700, size: 15).foregroundColor(colors.ink)
                            .padding(14)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(colors.yellowSoft)
                            .cornerRadius(14)
                        // 全画面完全性監査タスク #result: index.html:733 #rPT(理学療法士のひとくち解説)の1:1移植。
                        Spacer().frame(height: 12)
                        VStack(alignment: .leading, spacing: 4) {
                            Text("🩺 理学療法士のひとくち解説").kyonoFont(.black900, size: 13).foregroundColor(colors.ink)
                            boldHtmlText(info.pt, bold: colors.ink)
                                .kyonoFont(.bold700, size: 14).foregroundColor(colors.sub)
                        }
                    }
                    // 全画面完全性監査タスク #result: index.html:734 #rReachNote(Q1自動転記の一言)の1:1移植。
                    if let lv = autoReachLv {
                        Spacer().frame(height: 8)
                        Text("📏 いまの前屈「\(reachLv[lv])」を とどくメーターにも記録したよ")
                            .kyonoFont(.black900, size: 13).foregroundColor(colors.tealInk)
                            .frame(maxWidth: .infinity, alignment: .center)
                    }
                }
                if fdGuideActive {
                    // TASK-C2-2026-07-27-fd-guide-ui-branch.md: app-quiz.js:300-322の1:1移植。ガイド中は
                    // 通常の3本リストではなく、①だけを練習させる専用UIに差し替える(2026-07-21 5視点
                    // 検証D・PO承認済み)。「タスクスイッチできない層がYouTubeから戻れないのが最初の
                    // 脱落点」という動機のため、OS別のもどりかた案内を必ず添える。
                    KyonoCard {
                        Text("きょうはこの1本だけでOK！")
                            .kyonoFont(.black900, size: 15).foregroundColor(colors.ink)
                        Spacer().frame(height: 10)
                        // app-quiz.js:316-320 sd-row.oga相当の練習宣言吹き出し(相談室botバブルと同じ見た目)。
                        HStack(alignment: .bottom, spacing: 8) {
                            KyonoCharaImage(name: "chara-hitokoto").frame(width: 38, height: 38)
                            VStack(alignment: .leading, spacing: 4) {
                                // TASK-C2-2026-07-30-onboarding-scroll-and-copy.md B1: 直前の見出し
                                // 「きょうはこの1本だけでOK！」で"練習/1本だけ"の意図は既に伝わっている。
                                // 「ぜんぶ見るのはあとでOK」は下の「あと2本〜あしたから見られるよ」と
                                // 重複するため削る。
                                Text("①をタップ！ YouTubeが開くよ🏫")
                                    .kyonoFont(.bold700, size: 15).foregroundColor(colors.ink)
                                Text("🔙 見おわったら 画面ひだり上に出る「◀」か YouTubeをとじると この画面にもどれるよ")
                                    .kyonoFont(.bold700, size: 13).foregroundColor(colors.sub)
                            }
                            .padding(.horizontal, 14).padding(.vertical, 10)
                            .background(RoundedRectangle(cornerRadius: 16).fill(colors.card))
                            .overlay(RoundedRectangle(cornerRadius: 16).stroke(colors.line, lineWidth: 1.5))
                        }
                        Spacer().frame(height: 8)
                        // index.html:216 fdBob(1.4s ease-in-out infinite・translateY 0↔5px)の1:1移植。
                        FdBobText("👇 ここを押してみて")
                        Spacer().frame(height: 4)
                        if let vk = rx.first, let v = lookupVideo(vk) {
                            // TASK-C2-2026-07-28-quiz-result-reach-parity.md §5: app-quiz.js:320
                            // videoCard(rx[0], "きょうはこれ1本でOK!")の1:1移植。badge:nilで欠落していた。
                            VideoRow(v: v, openUrl: onVideoTap, badge: "きょうはこれ1本でOK！", hero: true)
                        }
                        Spacer().frame(height: 6)
                        Text("あと2本とくわしい解説は あしたから見られるよ🌱")
                            .kyonoFont(.bold700, size: 13).foregroundColor(colors.sub)
                            .frame(maxWidth: .infinity, alignment: .center)
                    }
                } else {
                    // 診断結果画面「おすすめ動画3本」欠落修正タスク(TASK-C2-2026-07-26-result-video-recommendations.md):
                    // index.html:736-744 rxHead/rxList/worryExtra/rRotateNoteの1:1移植。
                    KyonoCard {
                        Text("おすすめの3本: まずは「\(info.area)」から！2週間続けてみて")
                            .kyonoFont(.black900, size: 15).foregroundColor(colors.ink)
                        Spacer().frame(height: 10)
                        let badges = ["①まずほぐす", "②メインの1本", "③しあげ"]
                        ForEach(Array(rx.enumerated()), id: \.offset) { i, vk in
                            if let v = lookupVideo(vk) {
                                VideoRow(v: v, openUrl: onVideoTap, badge: badges.indices.contains(i) ? badges[i] : nil)
                            }
                        }
                        if !rx.isEmpty {
                            Spacer().frame(height: 8)
                            KyonoGhostButton("▶ 3本続けて再生する") {
                                let ids = rx.compactMap { quizVideoKeyToId[$0] }.joined(separator: ",")
                                openUrl("https://www.youtube.com/watch_videos?video_ids=\(ids)")
                            }
                        }
                        // index.html:81-85,327-328 WORRY[saved.worry](悩み別の追加1本。3本と重複しない場合のみ)の1:1移植。
                        if let worry, let extra = worryExtraMap[worry], !rx.contains(extra.v), let v = lookupVideo(extra.v) {
                            Spacer().frame(height: 4)
                            VideoRow(v: v, openUrl: onVideoTap, badge: "＋ \(extra.label)")
                        }
                        // index.html:740 #rRotateNoteの1:1移植。
                        Spacer().frame(height: 4)
                        // GO-G2(5視点ワンループ): index.html:740 .rotate-note{color:var(--sub)}の1:1移植。
                        // subFaintは実測コントラスト不足(3.87:1)で、Web版でもここはvar(--sub)であり
                        // 元々subFaintの用途ではなかった(subFaintの正しい用途はオガトレ通信の
                        // 30日超の古い投稿日付のみ・index.html:277-278)。
                        Text("おすすめは3日ごとに自動で入れ替わります")
                            .kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)
                    }
                }
                // TASK-C2-2026-07-28-quiz-result-reach-parity.md §1: rPace/rSoudanLinkもガイド中は隠す
                // (app-quiz.js:291-299 rPace + app-quiz.js:332 !guide && sdKb()の1:1移植)。
                if !fdGuideActive {
                    // 全画面完全性監査タスク #result: index.html:741-742 #rPace/hint(ペースの目安・免責注意書き)の1:1移植。
                    KyonoCard {
                        Text("🩺 ペースの目安").kyonoFont(.black900, size: 14).foregroundColor(colors.ink)
                        Spacer().frame(height: 6)
                        Text("・毎日が理想！週3でも効きます\n・1日1回で十分\n・痛い日は休むのが正解\n・痛みは「イタ気持ちいい」まで")
                            .kyonoFont(.bold700, size: 14).foregroundColor(colors.sub)
                        Spacer().frame(height: 8)
                        // GO-G2: index.html:742 .hint{color:var(--sub)}の1:1移植。
                        Text("※効果には個人差があります 痛みが強いときは中止して医療機関へ")
                            .kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)
                        // 全画面完全性監査タスク #result: index.html:743 #rSoudanLink(タイプ別の相談室逆導線)の1:1移植。
                        if let intentId = soudanTypeIntent[typeKey] {
                            Spacer().frame(height: 10)
                            // GO-G3(5視点ワンループ): 最小タップ領域44pt/48ptの確保(見た目は変えず当たり判定のみ拡張)。
                            // UX13案・案8(2026-07-30): ボタン用途の残存絵文字をCanvasアイコンへ(.soudanBubble)。
                            HStack(spacing: 4) {
                                KyonoIconGlyph(icon: .soudanBubble, fill: .clear, accent: colors.tealInk).frame(width: 16, height: 16)
                                Text("この悩み、相談室で聞いてみる")
                                    .kyonoFont(.black900, size: 14).foregroundColor(colors.tealInk)
                            }
                                .frame(maxWidth: .infinity, alignment: .center)
                                .padding(.vertical, 12)
                                .onTapGesture { onOpenSoudan(intentId) }
                        }
                    }
                }
                // ダークモード再確認+rDoneNudge/rTourBtn実装タスク: index.html:745 #rDoneNudgeの1:1移植。
                // はじめの1本ガイド中、結果画面を表示したまま動画を見に行って戻ってきたときに、
                // ホームのcheerの代わりに結果画面内へ「やった？」の復帰案内を出す。
                if showDoneNudge {
                    KyonoCard {
                        Text("おかえりなさい！✨ ストレッチできた？").kyonoFont(.black900, size: 15).foregroundColor(colors.ink)
                        Spacer().frame(height: 10)
                        KyonoPrimaryButton(fdGuideActive ? "✅ 1日目の記録をつけにいく" : "✅ きょうの記録をつけにいく", action: onDoneFromNudge)
                    }
                }
                // index.html:746 #rTourBtn(オンボ→クイズ経由・ツアー未見のときだけ)の1:1移植。
                if showTourBtn {
                    KyonoGhostButton("📖 つづき：使い方ツアーへ", action: onStartTour)
                }
                // TASK-C2-2026-07-28-quiz-result-reach-parity.md §1: rGoHomeBtn/rRecheckBtnもガイド中は
                // 隠す(app-quiz.js:291-299の1:1移植。タブバーからの脱出は常に可能なため迷子にはならない)。
                if !fdGuideActive {
                    KyonoPrimaryButton("きょうの1本へ", action: onDone)
                    // 全画面完全性監査タスク #result: index.html:748 #rRecheckBtn(もう一回チェックする)の1:1移植。
                    KyonoGhostButton("もう一回チェックする", action: onStartQuiz)
                }
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
    // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §6: Web版の「見てみる」ボタンはネイティブの
    // マイ記録に存在しない(お楽しみは🎉じまんカード/💬せんぱいの声/📔ひとことにっきの個別3ボタン)ため、
    // その3ボタンを直接指す文言に書きかえる(以前はWeb版UI前提の文言のまま移植されていた)。
    TourSlideDef(title: "📅 マイ記録でふりかえる", desc: "やった日に印がつくカレンダーがあるよ（×はつかないよ） 📏とどくメーターと🎉じまんカード・💬せんぱいの声・📔ひとことにっき もこのタブから見られるよ 毎日の合図（カレンダー通知）は続ける設定からいつでも入れられるよ📅"),
    TourSlideDef(title: "📖 忘れてもだいじょうぶ", desc: "困ったらいつでも読み返せるよ！ 使い方タブの「📖 使い方ツアー」からね"),
]
let obTourClosingTitle = "🌱 これで準備ばっちり！"
// TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §5: index.html:4275-4277
// OB_TOUR_CLOSING.dの1:1移植(2026-07-21 PO承認案(b))。以前はタイトルのみで説明文が欠落していた。
let obTourClosingDesc = "あしたも待ってるね🌱 きょうのぶんの動画をちゃんとやるなら ホームの「きょうの1本」からどうぞ💪"

// index.html:4283-4347 fdTourMaybeStart/obTourStep/obTourEndの1:1移植。8枚+条件付き9枚目
// (closing・自動起動時のみ)。「つぎへ」ボタン+ドット進捗のリニアなステップ形式(スワイプ不使用)。
// TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §6: 以前はRecordStoreを受け取らず
// テーマ・文字サイズを"auto"/trueに固定していたため、手動でライト設定にしている本人が夜に
// 再訪するとツアー画面だけダークになる不具合があった。他画面と同じくstoreの実設定を使う。
struct TourView: View {
    let store: RecordStore
    let showClosing: Bool
    let onDone: () -> Void

    @State private var si = 0

    private var totalSlides: Int { obTourSlides.count + (showClosing ? 1 : 0) }

    var body: some View {
        KyonoTheme(themeSetting: store.get("theme", default: "auto"), bigText: store.get("bigtext", default: true)) {
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
        // TestFlight実機フィードバックD6(2026-07-29): ステップごとに本文の量が違い、その下に
        // 置かれた「つぎへ」の位置が毎回上下に動いていた(押した指を置き直す必要があった)。
        // index.html:524 #obLog{flex:1;max-height:52vh;overflow-y:auto}/528 #obChips(ボタン専用の
        // 別要素)の1:1移植で、内容(#obLog相当)とボタン列(#obChips相当)を別の非スクロール領域に
        // 分ける。ボタン列を外側のVStackに出し、内容側だけをScrollViewにすることで、内容の
        // 長さに関わらずボタンの位置が1ptも動かなくなる(あふれるステップだけ中でスクロール)。
        VStack(spacing: 0) {
            ScrollViewReader { proxy in
            ScrollView {
                VStack(alignment: .leading, spacing: 10) {
                    Color.clear.frame(height: 0).id("obTop")
                    if si < obTourSlides.count {
                        let slide = obTourSlides[si]
                        Text(slide.title).kyonoFont(.black900, size: 17).foregroundColor(colors.ink)
                        // index.html:4118-4142 各スライドv フィールド(実際の画面のミニチュアモックアップ)の1:1移植。
                        KyonoTourMockup(slideIndex: si)
                        Text(slide.desc).kyonoFont(.bold700, size: 14).foregroundColor(colors.ink).lineSpacing(11)
                            .padding(.horizontal, 14).padding(.vertical, 10)
                            .background(RoundedRectangle(cornerRadius: 14).fill(colors.card))
                            .overlay(RoundedRectangle(cornerRadius: 14).stroke(colors.line, lineWidth: 1.5))
                    } else {
                        // index.html:4276 OB_TOUR_CLOSING(chara-congrats.png 110x110・中央表示)の1:1移植。
                        VStack(spacing: 8) {
                            KyonoCharaImage(name: "chara-congrats").frame(width: 110, height: 110)
                            Text(obTourClosingTitle).kyonoFont(.black900, size: 17).foregroundColor(colors.ink)
                            Text(obTourClosingDesc).kyonoFont(.bold700, size: 14).foregroundColor(colors.sub)
                                .multilineTextAlignment(.center)
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
                }
                .padding(20)
            }
            // index.html:4308 log.scrollTop=0の1:1移植。内容側だけが独立してスクロールする
            // ようになったため、ステップが変わるたびに先頭へ戻さないと前のステップのスクロール
            // 位置が持ち越されて見える(Web版はそれが起きないようステップ描画のたびに毎回0へ戻す)。
            .onChange(of: si) { _, _ in proxy.scrollTo("obTop", anchor: .top) }
            }
            // D6: ボタン列は内容のスクロールに関わらず画面下端に固定。
            VStack(spacing: 10) {
                if si > 0 {
                    KyonoLineButton("◀ もどる") { si -= 1 }
                }
                // D6: ボタン文言から絵文字を外す(D7と同じ理由)。ドット進捗が既に上にあるため、
                // 文字側の「(N/9)」は重複と判断して落とす(ドットのほうを残す)。
                KyonoPrimaryButton(si < totalSlides - 1 ? "つぎへ" : "おわる") {
                    if si < totalSlides - 1 { si += 1 } else { onDone() }
                }
                if si < totalSlides - 1 {
                    KyonoGhostButton("ツアーをとばす", action: onDone)
                }
            }
            .padding(.horizontal, 20).padding(.top, 10).padding(.bottom, 20)
        }
        .background(KyonoBackgroundColor().ignoresSafeArea())
    }
}
