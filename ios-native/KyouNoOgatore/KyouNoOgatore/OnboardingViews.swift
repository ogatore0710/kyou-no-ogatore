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
import WidgetKit

// MARK: - オンボーディング

struct ObChip {
    let label: String
    let v: String
    // TASK-C2-2026-08-05-build27-round5.md R-15(本人裁定・案①硬い=赤): 設問の意味と色を
    // リンクさせるための色キー(obgNamedColors/obgNamedColorsDarkのキーに対応)。nilなら従来どおり
    // 並び順の巡回(obgColors(dark:)[i % count])にフォールバックする(もじの大きさ設問はこちら)。
    var colorKey: String? = nil
}

struct ObQuestionDef {
    let key: String
    let q: String
    let chips: [ObChip]
}

let obGreet = [
    "いつもありがとうございます！理学療法士のオガトレです！",
    "ここは毎日のストレッチを応援する場所だよ！ぜんぶ無料・とうろく不要 あんしんしてね",
    "最初に4つだけ教えてね！あなた用にこのアプリをととのえます",
]

// index.html:4093-4102 ONBOARDING_SCRIPT.questions の1:1移植。かたさチェック本体(QUESTIONS)とは
// 別の、オンボ専用の簡易4問(bigtext/stiff/worry/anchor)。
// TASK-C2-2026-08-05-build27-round5.md R-15(本人裁定・案①硬い=赤): 「文字の内容と色をリンク
// させてほしい」という本人の生指摘を受け、stiff/worry/anchorの3設問だけ色キーを明示する
// (もじの大きさ設問は対象外・従来どおり巡回)。
let obQuestions = [
    ObQuestionDef(key: "bigtext", q: "もじの大きさ、どっちが見やすい？", chips: [
        ObChip(label: "大きめ（いまのまま）", v: "big"), ObChip(label: "ふつう", v: "normal"),
    ]),
    ObQuestionDef(key: "stiff", q: "体、硬いほう？", chips: [
        ObChip(label: "ガチガチかも", v: "hard", colorKey: "rose"), ObChip(label: "ふつう", v: "normal", colorKey: "yellow"),
        ObChip(label: "やわらかい", v: "soft", colorKey: "green"), ObChip(label: "わからない", v: "unknown", colorKey: "neutral"),
    ]),
    ObQuestionDef(key: "worry", q: "いちばん気になるのは？", chips: [
        ObChip(label: "肩こり・首", v: "katakori", colorKey: "orange"), ObChip(label: "腰", v: "youtsuu", colorKey: "rose"),
        ObChip(label: "前屈できない", v: "zenkutsu", colorKey: "blue"), ObChip(label: "眠り", v: "nemuri", colorKey: "purple"),
        ObChip(label: "とくにない", v: "none", colorKey: "green"),
    ]),
    ObQuestionDef(key: "anchor", q: "ストレッチ、いつやる派？", chips: [
        ObChip(label: "朝おきて", v: "asa", colorKey: "yellow"), ObChip(label: "おふろ上がり", v: "furo", colorKey: "blue"),
        ObChip(label: "寝るまえ", v: "neru", colorKey: "purple"), ObChip(label: "きめてない", v: "free", colorKey: "neutral"),
    ]),
]

let obAnchorAck: [String: String] = [
    "asa": "朝おきてすぐだね ホームにも覚えさせたよ",
    "furo": "おふろ上がりは体もほぐれてて効果的 覚えたよ",
    "neru": "寝るまえの1本はねむりにも効くよ 覚えたよ",
    "free": "きめなくてもOK！そのつどでだいじょうぶ",
]

// app-quiz.js:193 WORRY_TIEBREAKと紐づくQ5語彙への対応表(index.html:4370 OB_WORRY_TO_QUIZ)。
// "none"は対応表に含めない(worry!=="none"のときだけquizルートへ行く条件と対になっている)。
let obWorryToQuiz: [String: String] = ["katakori": "katakori", "youtsuu": "yotsu", "zenkutsu": "yawaraka", "nemuri": "tsukare"]

// TASK-C2-2026-08-01-build14-fixes-and-5lens-audit.md A-2: 固定フッターCTA(かたさチェックを
// はじめる/きょうの1本を見る)のおおよその高さ(ボタン本体+外側padding)。オンボチャットの
// 自動スクロール着地位置に、この分の余白を確保するために使う。
let kyonoOnboardingCtaInset: CGFloat = 100

// index.html:4108-4111 ONBOARDING_SCRIPT.routesの1:1移植(TASK-C2-2026-07-27-onboarding-routes-closing-message)。
struct ObRouteInfo {
    let say: [String]
    let btn: String
}
let obRoutes: [String: ObRouteInfo] = [
    "quiz": ObRouteInfo(say: ["そしたら30秒で硬さチェックをしよう！下のボタンタップしてね！"], btn: "かたさチェックをはじめる"),
    "today": ObRouteInfo(say: ["じゃあ今日の1本から！むずかしいことはなしだよ"], btn: "きょうの1本を見る"),
]

// index.html:4377 obGo()内の条件式の1:1移植。
func obDecideRoute(stiff: String, worry: String) -> String {
    (stiff == "hard" || stiff == "unknown" || worry != "none") ? "quiz" : "today"
}

// かたさチェックの.opt.g0〜g3(index.html:301-309)・オンボの#obChips .chip.obg0-3(index.html:537-544)と
// 同じ「明→暗」段階色パレット(bg,border)。並び順で明→暗を巡回させる(index.html:4211と同じ
// 「obg"+(i%4)」方式)。ライト/ダークで別パレット。
// TASK-C2-2026-08-01-build14-fixes-and-5lens-audit.md A-1: 5択の質問(部位選択など)で
// i%4のため1番目と5番目が同色になっていた欠落。5色目(青系・色相約200)を追加し5色パレットにした。
struct ObgColor {
    let bg: Color
    let border: Color
    let text: Color
}
// TASK-C2-2026-08-05-build24-chip-clarity.md(案A'・本人GO): ビルド23実機で選択肢チップが
// 「見にくい」との指摘。黄CTA(#FFD93B・ink文字・濃縁)と同じ「高彩度の塗り+ink文字+カテゴリ濃縁」の
// 文法へ、地色を淡パステルから高彩度へ刷新。縁は据え置き、文字はカテゴリ濃色ではなくink固定に統一。
// ライトのみの変更(ダークのobgDarkは不変)。
private let obgLight: [ObgColor] = [
    ObgColor(bg: Color(hex: 0x6FCDA6), border: Color(hex: 0x177065), text: Color(hex: 0x3A3A35)),
    ObgColor(bg: Color(hex: 0xFFDB4D), border: Color(hex: 0x7A5E00), text: Color(hex: 0x3A3A35)),
    ObgColor(bg: Color(hex: 0xFFB558), border: Color(hex: 0x995400), text: Color(hex: 0x3A3A35)),
    ObgColor(bg: Color(hex: 0xEE9B82), border: Color(hex: 0x863213), text: Color(hex: 0x3A3A35)),
    ObgColor(bg: Color(hex: 0x7BC2E8), border: Color(hex: 0x006199), text: Color(hex: 0x3A3A35)),
]
// TASK-C2-2026-08-01-build13-round3.md ②: 旧配色は4色の色相が29〜40度に密集し、
// ダークでは「全部こげ茶」に潰れて見えた。4色目を茶系からローズ/マゼンタ(色相約320度)へ
// 大きく振り、緑(154)・黄(48)・橙(28)・薔薇(320)へ色相を広く分散させた。
private let obgDark: [ObgColor] = [
    ObgColor(bg: Color(hex: 0x223D33), border: Color(hex: 0x2E5A48), text: Color(hex: 0xF2EDE1)),
    ObgColor(bg: Color(hex: 0x4A3D14), border: Color(hex: 0x6B5A1C), text: Color(hex: 0xF2EDE1)),
    ObgColor(bg: Color(hex: 0x4D3018), border: Color(hex: 0x704620), text: Color(hex: 0xF2EDE1)),
    ObgColor(bg: Color(hex: 0x4A1F35), border: Color(hex: 0x6B2C4C), text: Color(hex: 0xF2EDE1)),
    ObgColor(bg: Color(hex: 0x1F3A4D), border: Color(hex: 0x2B5570), text: Color(hex: 0xF2EDE1)),
]
func obgColors(dark: Bool) -> [ObgColor] { dark ? obgDark : obgLight }

// TASK-C2-2026-08-05-build27-round5.md R-15: 意味リンク配色用の色キー辞書。既存obgLight/obgDarkの
// 5色(green/yellow/orange/rose/blue)はそのまま名前引きできるようにし、新色2つ(purple/neutral・
// 本人指定のダーク値込み)を追加する。obgLight/obgDark配列自体は変更しない(かたさチェック本体
// Q1-Q4の並び順巡回・obgColors(dark:)[i % count]がそのまま使い続けるため)。
private let obgNamedColorsLight: [String: ObgColor] = [
    "green": obgLight[0], "yellow": obgLight[1], "orange": obgLight[2], "rose": obgLight[3], "blue": obgLight[4],
    "purple": ObgColor(bg: Color(hex: 0xB1A6E6), border: Color(hex: 0x463B8C), text: Color(hex: 0x3A3A35)),
    "neutral": ObgColor(bg: Color(hex: 0xE7E0D2), border: Color(hex: 0x6B6557), text: Color(hex: 0x3A3A35)),
]
private let obgNamedColorsDark: [String: ObgColor] = [
    "green": obgDark[0], "yellow": obgDark[1], "orange": obgDark[2], "rose": obgDark[3], "blue": obgDark[4],
    "purple": ObgColor(bg: Color(hex: 0x2E2847), border: Color(hex: 0x453C6B), text: Color(hex: 0xF2EDE1)),
    "neutral": ObgColor(bg: Color(hex: 0x2F2C26), border: Color(hex: 0x4A463E), text: Color(hex: 0xF2EDE1)),
]
func obgNamedColor(_ key: String, dark: Bool) -> ObgColor? { (dark ? obgNamedColorsDark : obgNamedColorsLight)[key] }

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
    // TASK-C2-2026-07-31-build12-journey2-splash-emoji.md W1-a: 初回起動(まだonboarded==falseの
    // タイミング)だけ、見出しを「📖 使い方ツアー」+4点バーに差し替える。使い方タブ経由の再入場
    // (onboarded==true済み)は既存の「🌱 はじめてガイド」・バーなしのまま。
    let isFirstRun: Bool

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var bubbles: [ChatBubble] = []
    @State private var activeQuestion: ObQuestionDef?
    @State private var routeCta: ObRouteInfo?
    @State private var answers: [String: String] = [:]
    @State private var pendingPick: CheckedContinuation<ObChip, Never>?
    @State private var pendingCta: CheckedContinuation<Void, Never>?

    init(store: RecordStore, onComplete: @escaping (String, String?) -> Void) {
        self.store = store
        self.onComplete = onComplete
        self.isFirstRun = !store.get("onboarded", default: false)
    }

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
                await say([obAnchorAck[picked.v] ?? "OK！おぼえたよ"])
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

    private var themeSetting: String { store.get("theme", default: "light") }

    var body: some View {
        KyonoTheme(themeSetting: themeSetting, bigText: store.get("bigtext", default: true)) {
            OnboardingContentView(
                bubbles: bubbles, activeQuestion: activeQuestion, routeCta: routeCta,
                isFirstRun: isFirstRun,
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
    let isFirstRun: Bool
    let onChipTap: (ObChip) -> Void
    let onCtaTap: () -> Void

    private var dark: Bool { colors.bg == kyonoDarkColors.bg }

    var body: some View {
        // TASK-C2-2026-07-30-onboarding-scroll-and-copy.md A2: TourContentView(D6)と同じ構造。
        // 選択肢・CTAボタンを本文と同じScrollViewから外し、外側VStackの固定フッターにする。
        // これでCTAは常に画面内の同じ位置にあり、本文の長さに関わらず動かない。
        VStack(spacing: 0) {
        // TASK-C2-2026-07-31-build12-journey2-splash-emoji.md W1-a: 初回起動だけ見出しを
        // ScrollView外の固定上部へ移し「📖 使い方ツアー」を出す。
        // 再入場(使い方タブ経由)は既存どおり本文内に「🌱 はじめてガイド」を出すのでここには出さない。
        // TASK-C2-2026-08-03-build18-tutorial-quality.md B-9(本人GO): この見出し下で
        // 「4点バー(この画面)→チェック4/5段(Quiz/Result)→ツアー7/8点(Tour)」と3種類の
        // 進捗バーが連続して出ていた引き算。質問4つ(もじの大きさ/かたさ/悩み/いつやる)は
        // チャットの吹き出しの流れそのもので十分伝わるため、この4点バーだけを消す
        // (チェック・ツアーの2種は残す)。
        if isFirstRun {
            Text("使い方ツアー").kyonoFont(.black900, size: 16).foregroundColor(colors.ink)
                .padding(.horizontal, 20).padding(.top, 20)
        }
        ScrollViewReader { proxy in
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                if !isFirstRun {
                    Text("はじめてガイド").kyonoFont(.black900, size: 16).foregroundColor(colors.ink)
                }
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
                        // TASK-C2-2026-08-04-build19-tour-redesign.md T-7: lineSpacing 11@15ptだと
                        // 行がバラけて痩せて見えていた → 7へ詰める。
                        Text(b.text).kyonoFont(.bold700, size: 15).foregroundColor(colors.ink).lineSpacing(7)
                            .padding(.horizontal, 14).padding(.vertical, 10)
                            .background(shape.fill(b.fromUser ? colors.yellowSoft : colors.card))
                            .overlay(shape.stroke(b.fromUser ? Color.clear : colors.borderStrong, lineWidth: 1.5))
                        if !b.fromUser { Spacer(minLength: 40) }
                    }
                    .transition(.sdPop)
                }
                // TASK-C2-2026-08-01-build14-fixes-and-5lens-audit.md A-2: 固定フッターCTA
                // 「かたさチェックをはじめる」の高さぶん、スクロール末尾にインセットを確保する。
                // obBottomアンカー自体を高さ分だけ確保することで、scrollTo(anchor:.bottom)の
                // 着地位置もこの分だけ上にずれ、最後の吹き出しがCTAに隠れず全文読めるようになる。
                Color.clear.frame(height: kyonoOnboardingCtaInset).id("obBottom")
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
                Text("タップしてえらんでね").kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)
                let palette = obgColors(dark: dark)
                ForEach(Array(q.chips.enumerated()), id: \.offset) { i, chip in
                    // TASK-C2-2026-08-05-build27-round5.md R-15: colorKeyがあれば意味リンク配色を
                    // 使う(かたさ/悩み/時間帯)。無ければ従来どおり並び順の巡回(もじの大きさ)。
                    let c = chip.colorKey.flatMap { obgNamedColor($0, dark: dark) } ?? palette[i % palette.count]
                    // TASK-C2-2026-07-30-icon-system-addendum-chips.md: 部位・時間帯チップの
                    // 生成イラスト。ChipArt/chip-<v>.pngが存在するものだけ表示する
                    // (硬さチェック6タイプ=KyonoTypeArtはこの対象外・触らない)。
                    // TASK-C2-2026-08-01-build13-round3.md ①: 「もじの大きさ」設問(key=="bigtext")は
                    // 絵を一切付けない(バグ修正: chip.v="normal"がかたさ設問と衝突し、かたさ用の
                    // 前屈絵が誤って出ていた欠落の根本対策)。代わりにボタン文字自身のサイズで
                    // 「大きめ/ふつう」を実演する(自己実演型)。
                    HStack(spacing: 10) {
                        if q.key != "bigtext",
                           let url = Bundle.main.url(forResource: "chip-\(chip.v)", withExtension: "png"),
                           let uiImage = UIImage(contentsOfFile: url.path) {
                            Image(uiImage: uiImage).resizable().scaledToFit().frame(width: 32, height: 32)
                        }
                        // TASK-C2-2026-08-04-build22-yellow-return.md Z-3: 文字もカテゴリの濃色
                        // (c.text)にして淡色地への沈みを解消。
                        Text(chip.label).kyonoFont(.bold700, size: (q.key == "bigtext" && chip.v == "big") ? 20 : 16).foregroundColor(c.text)
                    }
                        .frame(maxWidth: .infinity)
                        .padding(.horizontal, 18).padding(.vertical, 14)
                        .background(RoundedRectangle(cornerRadius: 16).fill(c.bg))
                        // Z-3: 縁を2pt→2.5ptへ太く(視認性強化)。
                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(c.border, lineWidth: 2.5))
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

// TASK-C2-2026-07-31-build11-renshu-journey.md D(本丸): 練習モード(かたさチェック開始〜初回
// 記録カード表示まで)の共通ラベル。QuizView/ResultViewの両方から参照する。
// TASK-C2-2026-08-03-build18-tutorial-quality.md B-2: fdGuide中は動画サムネがno-op(Q-4)の
// ためタップされることがなく、「どうが」段が実際には体験されないままバーだけ進んで見えていた
// (本人指摘)。5段から「どうが」を外し4段にする。KyonoJourneyBarはQuizView(:709)・
// ResultContentView(:1066頃)・TourView(:1354頃)の3箇所で使われるが、QuizView/TourViewは
// この配列の「要素数」にだけ依存し(currentIndexは別ロジック)、意味的な段名には依存しない
// ため実害はない。ResultContentViewのjourneyIndex(この下)は必ず同時に直す
// (段の位置がズレる=alan5の警告どおり)。
// TASK-C2-2026-08-04-build19-tour-redesign.md T-3: 使い方ツアー独自の7点バー(番号のみのドット
// 表示)が、この体験ジャーニーバーとは別に画面上部にもう1本出ており「二重のツアー感」になって
// いた。ツアー専用バーを廃止し、この共通バーを5段目「みどころ」まで拡張してツアーからも
// 共用する(予告3枚+締めの間は常に「みどころ」がカレント)。QuizView/ResultContentViewは
// 5段目を一切参照しない(currentIndexの最大値はカードの3のまま)ため実害はない。
let kyonoJourneySteps = ["チェック", "けっか", "きろく", "カード", "みどころ"]

struct QuizView: View {
    let store: RecordStore
    let presetWorry: String?
    let onComplete: (_ typeKey: String, _ autoReachLv: Int?) -> Void
    // TASK-C2-2026-07-31-build11-renshu-journey.md 出荷前小修正(alan5 2026-07-31): fdGuide外
    // (再チェック)で入ったときだけ、途中離脱できる✕を出す(「もう一回チェックする」→気が変わった→
    // 出られない、という閉じ込めの解消)。fdGuide中(初回練習)は前進のみのまま変更しない。
    let onClose: () -> Void

    private let activeQuestions: [QuizQuestionDef]
    @State private var qi = 0
    @State private var scores: [String: Int] = [:]
    @State private var worry: String?
    // TASK-C2-2026-07-28-quiz-result-reach-parity.md §2: app-quiz.js:180の1:1移植。回答タップ直後に
    // 選択肢を無効化し、次の設問が描画されるまで二度押しで判定の入力が汚れるのを防ぐ。
    @State private var answering = false
    // TASK-C2-2026-07-28-quiz-result-reach-parity.md §5: app-quiz.js:166 state.pickedの1:1移植。
    // 「まえの質問へ」で戻ったとき前回選んだ選択肢が分かるよう、質問key→選択値(scoreまたはworryKey)を覚えておく。
    @State private var picked: [String: String] = [:]

    init(store: RecordStore, presetWorry: String?, onComplete: @escaping (String, Int?) -> Void, onClose: @escaping () -> Void) {
        self.store = store
        self.presetWorry = presetWorry
        self.onComplete = onComplete
        self.onClose = onClose
        self.activeQuestions = presetWorry != nil ? quizQuestions.filter { $0.key != "worry" } : quizQuestions
        _worry = State(initialValue: presetWorry)
    }

    private var themeSetting: String { store.get("theme", default: "light") }
    // TASK-C2-2026-07-31-build11-renshu-journey.md D: 練習モードジャーニーバーはfdGuide中
    // (はじめの1本ガイド・streakTotal==0)だけに出す。既存ユーザーの再チェックには一切出さない。
    private var fdGuideActive: Bool {
        let fd: String? = store.get("fd", default: nil)
        return HomeLogic.fdActive(fd: fd, streakTotal: RecordLogic.loadStreak(store).total)
    }

    var body: some View {
        KyonoTheme(themeSetting: themeSetting, bigText: store.get("bigtext", default: true)) {
            QuizContentView(
                activeQuestions: activeQuestions, qi: qi, answering: answering, picked: picked,
                fdGuideActive: fdGuideActive,
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
                onClose: onClose
            )
        }
        .onChange(of: qi) { _, _ in answering = false }
    }
}

private struct QuizContentView: View {
    @Environment(\.kyonoColors) private var colors
    let activeQuestions: [QuizQuestionDef]
    let qi: Int
    let answering: Bool
    let picked: [String: String]
    let fdGuideActive: Bool
    let onOptTap: (QuizQuestionDef, QuizOptDef) -> Void
    let onBack: () -> Void
    let onClose: () -> Void

    private var dark: Bool { colors.bg == kyonoDarkColors.bg }

    var body: some View {
        // TASK-C2-2026-07-30-onboarding-scroll-and-copy.md A2: TourContentView(D6)と同じ構造。
        // 「まえの質問へ」を本文と同じScrollViewから外し、外側VStackの固定フッターにする。
        // これでCTAは常に画面内の同じ位置にあり、本文の長さ(選択肢のnote文の折返し行数など)に
        // 関わらず動かない。TASK-C2-2026-07-31-build11-renshu-journey.md C: 「ホームにもどる」は
        // 削除(練習モードの一貫ジャーニーの一部として、出口を設けない設計に統一)。
        ZStack(alignment: .topTrailing) {
        VStack(spacing: 0) {
        // D(本丸): 練習モードジャーニーバー。fdGuide中だけ画面上部に固定表示(ScrollViewの外)。
        // TASK-C2-2026-08-01-build13-round3.md ③⑦: 見出し「📖 使い方ツアー」をオンボチャットと
        // 同じ見た目でバーの上に常設する(既存のfdGuideActive条件=初回ジャーニー中のみを維持)。
        if fdGuideActive {
            Text("使い方ツアー").kyonoFont(.black900, size: 16).foregroundColor(colors.ink)
                .padding(.horizontal, 20).padding(.top, 20)
            KyonoJourneyBar(labels: kyonoJourneySteps, currentIndex: 0)
        }
        ScrollViewReader { proxy in
        ScrollView {
            VStack(alignment: .leading, spacing: 8) {
                Color.clear.frame(height: 0).id("quizTop")
                Text("かたさチェック").kyonoFont(.black900, size: 16).foregroundColor(colors.ink)
                Text("Q\(qi + 1) / \(activeQuestions.count)").kyonoFont(.black900, size: 12).foregroundColor(colors.sub)
                // TASK-C2-2026-08-01-build15-subtraction9.md #6: 通常時(非fdGuide)は直上の「Qn/N」
                // テキストと9pxドット行が同じ進捗を二重表示していた(5視点監査指摘)ため、ドット行を
                // 削除(引き算)。fdGuide中はジャーニーバー(①チェック)が進捗を示すため、この画面の
                // ドットはfdGuide中ももとから非表示だった(元コード: TASK-C2-2026-07-28-quiz-result-
                // reach-parity.md §5・index.html:719 .dots+app-quiz.js:175-176の1:1移植)。
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
                    Text("タップしてえらんでね").kyonoFont(.black900, size: 13).foregroundColor(colors.ink)
                    Spacer().frame(height: 4)
                    // index.html:293-309 .opt/.opt.g0〜g3(明→暗の段階色カード)の1:1移植。
                    // TASK-C2-2026-07-28-quiz-result-reach-parity.md §5: app-quiz.js:168-169
                    // 「段階色は数値スコアの設問(Q1-Q4)だけ」の1:1移植。Q5(worry)はscore==nilのため
                    // 段階色を付けず、通常のカード色(colors.card/colors.line)にする。
                    let palette = obgColors(dark: dark)
                    // TASK-C2-2026-08-02-build17-feedback-fixes.md P-6: 前段の.transaction{
                    // $0.animation=nil}(build13-round3⑤)だけでは残像を消しきれなかった欠陥の修正。
                    // QuizOptionCardStyleは押下ハイライト用に.animation(_, value: pressed)を持つ
                    // (KyonoComponents側の一般的な作法)が、これは祖先の.transaction{}では上書き
                    // できないSwiftUIの仕様。タップ直後の「押した→離した」がちょうど次の設問への
                    // 切り替えと同時に起きるため、離した瞬間のアニメーションが「旧設問の押下色」から
                    // 「新設問の背景色」への値の変化までまとめて0.1sで補間してしまい、色付き背景の
                    // クロスフェード+せり上がりに見えていた。Group全体に設問キーで.id()を振り、
                    // 設問が変わるたびに選択肢ボタン一式をSwiftUIに「別物」として作り直させることで、
                    // 補間の起きようがない即時差し替えにする。
                    Group {
                        ForEach(Array(q.opts.enumerated()), id: \.offset) { i, opt in
                            let c = opt.score != nil ? palette[i % palette.count] : nil
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
                                borderColor: isPicked ? colors.teal : (c?.border ?? colors.borderStrong),
                                // TASK-C2-2026-08-04-build22-yellow-return.md Z-3(棚卸し対象): このQ1-Q4
                                // 段階色カードもobgColorsパレットを共有するため、同基準で文字も濃色化。
                                labelColor: c?.text ?? colors.ink,
                                // TASK-C2-2026-08-05-build24-chip-clarity.md: 段階色カード(bgが高彩度化)
                                // ではsub(#6E6B5F)がコントラスト不足(alan5実測2.45〜3.94:1)になるため、
                                // noteもinkにする。通常カード(Q5 worry・c==nil)はcolors.subのまま。
                                noteColor: c != nil ? colors.ink : nil,
                                pressedBackground: colors.yellowSoft, pressedBorderColor: colors.yellow,
                                colors: colors
                            ) { if !answering { onOptTap(q, opt) } }
                        }
                    }
                    .id(q.key)
                }
            }
            .padding(20)
            // TASK-C2-2026-08-01-build13-round3.md ⑤: 設問切替時に新旧の選択肢文字が重なって
            // 見える(クロスフェード)対策。祖先(画面遷移全体)のアニメーションがこのサブツリーへ
            // 伝播しないよう明示的に無効化し、即時差し替えにする(reduceMotion時と同じ挙動に統一)。
            .transaction { $0.animation = nil }
        }
        .onChange(of: qi) { _, _ in proxy.scrollTo("quizTop", anchor: .top) }
        }
        // 全画面完全性監査タスク #quiz: index.html:720 #qBackBtn(Q1以外で表示)の1:1移植。
        if qi > 0 {
            KyonoLineButton("← まえの質問へ", action: onBack)
                .padding(.horizontal, 20).padding(.bottom, 20)
        }
        }
        .background(KyonoBackgroundColor().ignoresSafeArea())
        // 出荷前小修正(alan5 2026-07-31): fdGuide外(再チェック)で入ったときだけ、途中離脱できる
        // ✕を出す。SoudanSheetView.swift:470-481の✕(44x44タップ域+40x40円)と同じ見た目。
        // fdGuide中(初回練習)は前進のみのまま出さない。
        if !fdGuideActive {
            Button(action: onClose) {
                Color.clear
                    .frame(width: 44, height: 44)
                    .overlay(
                        Text("✕").kyonoFont(.black900, size: 18).foregroundColor(colors.ink)
                            .frame(width: 40, height: 40)
                            .background(Circle().fill(colors.line))
                    )
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("とじる")
            .padding(.top, 4).padding(.trailing, 12)
        }
        }
    }
}

// UI/UXパリティ監査GO-2(2026-07-28): index.html:293-295 .opt/.opt:activeの1:1移植。
// TASK-C2-2026-07-31-feedback-round2.md A-1: DragGesture(minimumDistance:0)+無条件onEndedは
// 指をボタン外へずらしてから離してもaction()が発火してしまう欠陥(KyonoPrimaryButton等と同じ
// 診断2)が残存していた唯一の箇所。標準Button+ButtonStyle(configuration.isPressedで押下検知、
// 外して離せば標準どおりキャンセルされる)へ移行。
private struct QuizOptionCard: View {
    let label: String
    let note: String
    let background: Color
    let borderColor: Color
    // TASK-C2-2026-08-04-build22-yellow-return.md Z-3(棚卸し対象): obgColorsパレット共有に伴い、
    // 濃色文字も選択可能に。未指定時はcolors.inkのまま(既存呼び出し元との後方互換)。
    var labelColor: Color? = nil
    var noteColor: Color? = nil
    let pressedBackground: Color
    let pressedBorderColor: Color
    let colors: KyonoColors
    let action: () -> Void
    // UI/UXパリティ監査GO-3(iOS・2026-07-29): KyonoCardと同じズーム対応。
    @Environment(\.kyonoBigText) private var bigText

    private var zoom: CGFloat { bigText ? kyonoBigTextScale : kyonoNormalTextScale }

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 2) {
                // UI/UXパリティ監査2巡目A4(2026-07-29): index.html:294 .opt{font-size:18px}の1:1移植。
                // 従来15ptで-16.7%小さく値がズレていた欠落を修正する。
                Text(label).kyonoFont(.black900, size: 18).foregroundColor(labelColor ?? colors.ink)
                // UI/UXパリティ監査2巡目A1(2026-07-29): index.html:297 .opt .crit{line-height:1.5}の
                // 1:1移植。前回G2は検索チップのみに適用していたカスタムフォント行送り超過補正をここにも展開する。
                Text(note).kyonoFont(.bold700, size: 13).foregroundColor(noteColor ?? colors.sub).lineSpacing(7)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .buttonStyle(QuizOptionCardStyle(
            background: background, borderColor: borderColor,
            pressedBackground: pressedBackground, pressedBorderColor: pressedBorderColor,
            zoom: zoom
        ))
        // TASK-C2-2026-07-27-text-size-accessibility.md 項目4: 選択肢の見出し+補足説明を
        // 1回のVoiceOverスワイプで読める1つの単位にまとめる。
        .accessibilityElement(children: .combine)
    }
}

private struct QuizOptionCardStyle: ButtonStyle {
    @Environment(\.kyonoColors) private var colors
    let background: Color
    let borderColor: Color
    let pressedBackground: Color
    let pressedBorderColor: Color
    let zoom: CGFloat
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func makeBody(configuration: Configuration) -> some View {
        let pressed = configuration.isPressed
        configuration.label
            .padding(.horizontal, 16 * zoom).padding(.vertical, 14 * zoom)
            .background(RoundedRectangle(cornerRadius: 16 * zoom).fill(pressed ? pressedBackground : background))
            .overlay(RoundedRectangle(cornerRadius: 16 * zoom).stroke(pressed ? pressedBorderColor : borderColor, lineWidth: 2 * zoom))
            .contentShape(Rectangle())
            .animation(reduceMotion ? nil : .easeOut(duration: 0.1), value: pressed)
            // TASK-C2-2026-08-05-build23-bg-tuning-and-tour-tap.md W-6: かたさチェック選択肢の
            // 押下ハロー(相談室チップと同じ意図的実装)。
            .background(KyonoPressHaloBackground(pressed: pressed, color: colors.teal))
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

    // TASK-C2-2026-08-03-build17-hotfix-result-theme.md: themeSettingを"auto"に固定していたため、
    // アプリ内テーマ(kyono_theme)が「明るい」でもシステム側がダークだと結果画面だけダーク描画
    // されていた欠陥。build16まではデフォルト値も"auto"だったため気づかれなかったが、build17の
    // P-3(デフォルトを"light"へ変更)により、他画面(QuizView/TourView等)は正しく追従する一方
    // ResultViewだけ食い違うようになった(alan5実機報告・IMG_8728/8729)。他画面と同じ
    // store.get("theme", default: "light")に揃える。
    private var themeSetting: String { store.get("theme", default: "light") }

    var body: some View {
        // ResultViewはRecordStoreを従来受け取らなかったが、rSoudanLinkの遷移先(onOpenSoudan)や
        // worry(WORRY_EXTRA用)を読むために保持する。
        KyonoTheme(themeSetting: themeSetting, bigText: store.get("bigtext", default: true)) {
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
            store: store,
            info: info, typeKey: typeKey, autoReachLv: autoReachLv, rx: rx, worry: worry,
            showDoneNudge: $showDoneNudge, fdGuideActive: fdGuideActive, showTourBtn: showTourBtn,
            onVideoTap: { url in pendingNudgeDate = RecordLogic.todayStr(now: Date()); openUrl(url) },
            openUrl: openUrl, onDone: onDone, onDoneFromNudge: onDoneFromNudge ?? onDone,
            onStartQuiz: onStartQuiz, onOpenSoudan: onOpenSoudan, onStartTour: onStartTour
        )
    }
}

private struct ResultContentView: View {
    @Environment(\.kyonoColors) private var colors
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    let store: RecordStore
    let info: TypeInfo
    let typeKey: String
    let autoReachLv: Int?
    let rx: [String]
    let worry: String?
    // TASK-C2-2026-08-05-build28-round6.md R-18: 親(ResultView)の@Stateを直接リセットできるよう
    // let→Bindingへ変更(カードを閉じた瞬間にshowDoneNudgeを消し、出戻り描画を防ぐため)。
    @Binding var showDoneNudge: Bool
    let fdGuideActive: Bool
    let showTourBtn: Bool
    let onVideoTap: (String) -> Void
    let openUrl: (String) -> Void
    let onDone: () -> Void
    let onDoneFromNudge: () -> Void
    let onStartQuiz: () -> Void
    let onOpenSoudan: (String?) -> Void
    let onStartTour: () -> Void

    // TASK-C2-2026-07-31-build12-journey2-splash-emoji.md W1-a: 練習開始ポップ(showPracticePop)は
    // 削除(初回チャット画面に④点バーが出るようになり、結果画面での二重の「ここからは練習」案内が
    // 冗長になったため)。代わりに「動画タップまで」タイプカードを見せ続け、タップした瞬間に
    // どうが(③)へ進段させる。
    @State private var videoTapped = false
    // TASK-C2-2026-08-05-build26-round4.md R-7: 「動画をひらく練習」ピルのふわふわ演出用トグル。
    @State private var pillFloatUp = false
    // A-3: YouTubeから戻ったあと「おかえりなさい」ブロックが画面外で気づけなかった。
    // HomeView.swift:600-621のパルス+スクロール作法をそのまま流用。
    @State private var doneNudgeScale: CGFloat = 1
    // TASK-C2-2026-07-31-build11-renshu-journey.md D(本丸): fdGuide中は「おかえりなさい」の
    // 記録ボタンをその場(結果画面)で完結させる(ホームへ回り道させない)。HomeView.swiftの
    // wasGuide分岐(markDone→労い→confetti→カード入場→tourpend遷移)をこの画面専用に再現する。
    // 通常ユーザー(!fdGuideActive)の「おかえりなさい」は従来どおりonDoneFromNudge(ホームへ)を使う。
    @State private var cardResult: TodayCardResult?
    @State private var confettiTrigger: Int?
    // TASK-C2-2026-08-05-build23-bg-tuning-and-tour-tap.md W-2: 1本目だけYouTube往復の練習に
    // 使えるようにする。案内を一拍見せてからopenUrlする間の再タップ二重発火を防ぐガード。
    @State private var youtubeNoticeVisible = false

    private var catalogById: [String: CatalogVideo] {
        Dictionary(uniqueKeysWithValues: CatalogLoader.shared.map { ($0.id, $0) })
    }
    private func lookupVideo(_ key: String) -> CatalogVideo? {
        quizVideoKeyToId[key].flatMap { catalogById[$0] }
    }

    // D: 練習モードジャーニーバーの現在地(0-based)。①チェックはQuizViewが担当するため
    // ここでは②〜④(index 1〜3)のみ動く。
    // TASK-C2-2026-08-03-build18-tutorial-quality.md B-2: kyonoJourneySteps側で「どうが」を
    // 外し4段(チェック/けっか/きろく/カード)にしたのに合わせ、こちらの添字も詰める
    // (旧: けっか1・どうが2・きろく3・カード4 → 新: けっか1・きろく2・カード3)。
    // videoTapped(「きょうやった!」タップ済み)とshowDoneNudge(動画から復帰済み)は、どちらも
    // 「けっかの次=きろく」段に該当するため同じ2にまとめる。
    private var journeyIndex: Int {
        if cardResult != nil { return 3 }
        if showDoneNudge || videoTapped { return 2 }
        return 1
    }

    // HomeView.swift:316-331 closeCardAndMaybeStartTourの1:1移植(結果画面版)。
    private func closeCardAndMaybeStartTour() {
        cardResult = nil
        // TASK-C2-2026-08-05-build28-round6.md R-18(本人動画指摘・裁定GO): showDoneNudgeを
        // 立てたままにしておくと、カードを閉じてからstep5(ツアー)へ遷移するまでの約350msの間、
        // 済んだはずの「おかえり！／1日目の記録をつけにいく」画面が一瞬出戻って見えていた。
        // このカード表示フロー自体がfdGuideActive時にしか到達しないため、常にリセットしてよい。
        showDoneNudge = false
        let tourpend: Bool = store.get("tourpend", default: false)
        let tourseen: Bool = store.get("tourseen", default: false)
        if tourpend && !tourseen {
            store.set("tourpend", false)
            store.set("tourseen", true)
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                onStartTour()
            }
        }
    }

    // HomeView.swift:505-599のwasGuide分岐だけを抜き出した版(日1目は必ずこの分岐を通る。
    // 節目/通常cheerの分岐はfdGuide初日には到達しないため移植不要)。
    // TASK-C2-2026-08-05-build28-round6.md R-18(本人動画指摘・裁定GO): この関数はfdGuideActive時
    // にしか呼ばれない(呼び出し元2箇所とも`fdGuideActive ?`ガード済み。通常ユーザーの記録演出=
    // 労い→0.7秒→カードはHomeView側の別ロジックで別途担当・ここには一切触れていない)ため、
    // ツアー中は労い演出(旧fdCelebrationVisible「1日目クリア！ナイスご自愛！」)と0.7秒の
    // 待ち時間を省き、即カードモーダルを表示する。旧実装ではKyonoCardModalOverlayの
    // .transition(.opacity)がwithAnimationで包まれて0.35秒かけてフェードインしていたため、
    // 完全に不透明になるまでの間、背後の労いテキスト(0日目カードと矛盾する「1日目クリア！」)が
    // 透けて見えていた。withAnimationを使わず即座に代入することでこのフェード自体を無くす。
    private func performPracticeRecord() {
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
        _ = RecordLogic.markDone(store, now: Date())
        let streak = RecordLogic.loadStreak(store)
        DailyNotifications.resync(store: store)
        WidgetSummaryWriter.write(store: store)
        WidgetCenter.shared.reloadAllTimelines()
        let today = RecordLogic.todayStr(now: Date())
        // 練習モードは「きょうはこれ1本でOK！」で示した動画がそのまま今日の1本なので、Home側の
        // todayVideoIdAndTitle()より確実に特定できる。
        if let vk = rx.first, let v = lookupVideo(vk) {
            RecordLogic.recordDaylog(store, today: today, videoId: v.id, videoTitle: v.t, count: streak.count)
        }
        store.set("fd", "1")
        store.set("tourpend", true)
        // TASK-C2-2026-08-05-build27-round5.md R-13(本人指示「この画面は0日って表示させて。
        // テストだから」): このView自体がfdGuide中の練習専用(通常ユーザーはHomeView側の
        // renderTodayCard呼び出しを使う)なので、大数字表示だけ常に0にする。markDone/
        // recordDaylogは通常どおり実行済みで実カウントには一切影響しない(表示だけの変更)。
        let newCard = renderTodayCard(store: store, streak: streak, ds: today, displayTotalOverride: 0)
        cardResult = newCard
        confettiTrigger = (confettiTrigger ?? 0) + 1
    }

    var body: some View {
        ScrollViewReader { proxy in
        VStack(spacing: 0) {
        // D(本丸): 練習モードジャーニーバー。fdGuide中だけ画面上部に固定表示(ScrollViewの外)。
        // TASK-C2-2026-08-01-build13-round3.md ③⑦: 見出し「📖 使い方ツアー」をオンボチャットと
        // 同じ見た目でバーの上に常設する(既存のfdGuideActive条件=初回ジャーニー中のみを維持)。
        if fdGuideActive {
            Text("使い方ツアー").kyonoFont(.black900, size: 16).foregroundColor(colors.ink)
                .padding(.horizontal, 20).padding(.top, 20)
            KyonoJourneyBar(labels: kyonoJourneySteps, currentIndex: journeyIndex)
        }
        ZStack {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // TASK-C2-2026-08-02-build17-feedback-fixes.md Q-1: ガイド中(fdGuideActive)だけ
                // 「タイプ+①」に削ぎ落としていた結果画面を廃止し、通常のかたさチェックと同じ
                // フル版(タイプカード+解説+動画3本+ペース目安+相談室リンク)を常に表示する
                // (「一度正確な自分の結果がきちんと出る」という本人の狙いどおり)。
                KyonoGradientCard(gradient: .soft) {
                    // TASK-C2-2026-08-04-build20-addendum.md A-3(最小セット置換)。
                    Text("\(kyonoDisplayName(store))のかたさタイプは…").kyonoFont(.black900, size: 14).foregroundColor(colors.sub)
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
                    Spacer().frame(height: 12)
                    Text("" + info.hope).kyonoFont(.bold700, size: 15).foregroundColor(colors.ink)
                        .padding(14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(colors.yellowSoft)
                        .cornerRadius(14)
                    // 全画面完全性監査タスク #result: index.html:733 #rPT(理学療法士のひとくち解説)の1:1移植。
                    Spacer().frame(height: 12)
                    VStack(alignment: .leading, spacing: 4) {
                        Text("理学療法士のひとくち解説").kyonoFont(.black900, size: 13).foregroundColor(colors.ink)
                        boldHtmlText(info.pt, bold: colors.ink)
                            .kyonoFont(.bold700, size: 14).foregroundColor(colors.sub)
                    }
                    // TASK-C2-2026-08-01-build13-round3.md ④: 「とどくメーターにも記録したよ」の
                    // 表示行を削除(自動転記=setReach(lv, silent: true)自体は既存どおり継続、
                    // 表示だけを消す。alan5指摘: 結果画面が説明過多だった)。
                }
                // 診断結果画面「おすすめ動画3本」欠落修正タスク(TASK-C2-2026-07-26-result-video-recommendations.md):
                // index.html:736-744 rxHead/rxList/worryExtra/rRotateNoteの1:1移植。
                // Q-1: ガイド中専用の「①だけ練習」カードは廃止し、常にこの通常版を表示する。
                KyonoCard {
                    Text("おすすめの3本: まずは「\(info.area)」から！2週間続けてみて")
                        .kyonoFont(.black900, size: 15).foregroundColor(colors.ink)
                    // TASK-C2-2026-08-05-build25-tour-round3.md R-4(本人生指摘「1本目タップが
                    // 練習だと伝わらない」): ツアー中(fdGuideActive)だけ、見出しと1本目カードの間に
                    // R-2と同じ視覚言語の練習ピル+案内1行を挟む。タップ時notice・復帰フローには
                    // 一切触れない(表示を1つ足すだけ)。文言は本人校正済み(2026-08-05・2回の校正を
                    // 経た最終版・一字一句このまま)。
                    if fdGuideActive {
                        Spacer().frame(height: 10)
                        VStack(spacing: 6) {
                            // TASK-C2-2026-08-05-build26-round4.md R-7(本人モック確認済み・
                            // mock-pink-highlight-v3.png/pill-float-preview.gifが見た目の正解):
                            // ピンク化+16pt拡大+ふわふわ(±4pt・周期1.6s・easeInOut・reduceMotion時静止)。
                            Text("＼ 動画をひらく練習 ／")
                                .kyonoFont(.black900, size: 16).foregroundColor(colors.pinkInk)
                                .padding(.horizontal, 18).padding(.vertical, 6)
                                .background(Capsule().fill(colors.pinkSoft))
                                .offset(y: reduceMotion ? 0 : (pillFloatUp ? -4 : 4))
                                .onAppear {
                                    guard !reduceMotion else { return }
                                    withAnimation(.easeInOut(duration: 0.8).repeatForever(autoreverses: true)) {
                                        pillFloatUp = true
                                    }
                                }
                            // R-7: 案内行を自動折返しに任せず明示的に2行にする。
                            Text("今は1本目だけタップできるよ！\n動画をひらいてもどってきてね！")
                                .kyonoFont(.bold700, size: 13).foregroundColor(colors.sub)
                                .multilineTextAlignment(.center)
                        }
                        .frame(maxWidth: .infinity, alignment: .center)
                    }
                    Spacer().frame(height: 10)
                    // TASK-C2-2026-08-02-build17-feedback-fixes.md Q-4: ガイド中(fdGuideActive)は
                    // 動画サムネをタップ不可のままにする(本人裁定・離脱回避)。onVideoTapを差し替えず
                    // no-opにする(見た目はQ-1どおり通常の3本リストのまま・タップだけ無効化)。
                    // TASK-C2-2026-08-05-build23-bg-tuning-and-tour-tap.md W-2(本人発案・YouTube往復の
                    // 練習): build17 Q-4を部分改訂し、1本目だけはタップ可にする(2・3本目は従来どおり
                    // no-op+減光)。タップ時は一拍だけ案内を見せてからonVideoTap(既存のpendingNudgeDate
                    // 記録込み)を呼ぶ。performPracticeRecordやtourpend配線には一切触れない
                    // (既存のscenePhase復帰検知→showDoneNudge→「1日目の記録をつけにいく」ボタンの
                    // 練習合流フローをそのまま再利用するだけ)。
                    let videoTapHandler: (String) -> Void = fdGuideActive ? { _ in } : onVideoTap
                    let firstVideoTapHandler: (String) -> Void = { url in
                        guard !youtubeNoticeVisible else { return }
                        withAnimation(.easeOut(duration: 0.2)) { youtubeNoticeVisible = true }
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.9) {
                            youtubeNoticeVisible = false
                            onVideoTap(url)
                        }
                    }
                    let badges = ["①まずほぐす", "②メインの1本", "③しあげ"]
                    ForEach(Array(rx.enumerated()), id: \.offset) { i, vk in
                        if let v = lookupVideo(vk) {
                            // TASK-C2-2026-08-03-build18-tutorial-quality.md B-7: no-op裁定は維持
                            // したまま、見た目でも押せないことを明示する。
                            let isFirst = fdGuideActive && i == 0
                            VideoRow(
                                v: v,
                                openUrl: isFirst ? firstVideoTapHandler : videoTapHandler,
                                badge: badges.indices.contains(i) ? badges[i] : nil,
                                // TASK-C2-2026-08-05-build26-round4.md R-7: ツアー中の1本目カードを
                                // 既存のhero強調枠(pink 2.5pt+pinkSoft地)で目立たせる。
                                hero: isFirst,
                                disabledLook: fdGuideActive && !isFirst,
                                useShortTitle: true
                            )
                        }
                    }
                    if fdGuideActive && youtubeNoticeVisible {
                        Text("YouTubeがひらくよ。見おわったら〈きょうのオガトレ〉にもどってきてね")
                            .kyonoFont(.bold700, size: 13).foregroundColor(colors.tealInk)
                            .frame(maxWidth: .infinity, alignment: .center).multilineTextAlignment(.center)
                            .padding(.top, 6)
                            .transition(.opacity)
                    }
                    if !rx.isEmpty && !fdGuideActive {
                        Spacer().frame(height: 8)
                        KyonoGhostButton("▶ 3本続けて再生する") {
                            let ids = rx.compactMap { quizVideoKeyToId[$0] }.joined(separator: ",")
                            openUrl("https://www.youtube.com/watch_videos?video_ids=\(ids)")
                        }
                    }
                    // index.html:81-85,327-328 WORRY[saved.worry](悩み別の追加1本。3本と重複しない場合のみ)の1:1移植。
                    if let worry, let extra = worryExtraMap[worry], !rx.contains(extra.v), let v = lookupVideo(extra.v) {
                        Spacer().frame(height: 4)
                        VideoRow(v: v, openUrl: videoTapHandler, badge: "＋ \(extra.label)", disabledLook: fdGuideActive, useShortTitle: true)
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
                // TASK-C2-2026-08-02-build17-feedback-fixes.md Q-1: rPace/rSoudanLinkもガイド中だけ
                // 隠していたが、フル版統一のため常に表示する。
                // TASK-C2-2026-08-05-build27-round5.md R-12(本人赤ペン指摘): ツアー中はこのカード
                // 丸ごと非表示にする。通常の結果画面(!fdGuideActive)では従来どおり表示。
                if !fdGuideActive {
                KyonoCard {
                    Text("ペースの目安").kyonoFont(.black900, size: 14).foregroundColor(colors.ink)
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
                // TASK-C2-2026-08-02-build17-feedback-fixes.md Q-3: 結果表示と同時/直後に出していた
                // 「練習モード」ポップアップ的な専用ブロック(旧「きょうは練習してみよう」カード)を廃止し、
                // 静かな一行+ボタンに差し替える。読み終わったら自分のタイミングで進む設計。
                // TASK-C2-2026-08-03-build18-tutorial-quality.md B-6: 文言をalan5指定どおりに変更
                // (練習ボタンを本番と同じ「きょうやった！」に)。B-1: タップと同時にこのブロック
                // 自体を消し、カードモーダル出現までの0.7秒間、背後にボタンが残って半透明スクリム
                // 越しに二重に見えることを防ぐ(videoTappedをそのまま表示条件に使う)。B-8:
                // QuizView.onOptTapのansweringガードと同じ考え方で、videoTapped自体を
                // 「既に処理済みか」の判定にも使い、モーダル出現までの0.7秒間の再タップで
                // performPracticeRecordが二重発火しないようにする。
                // TASK-C2-2026-08-05-build26-round4.md R-6(本人赤ペン指摘): 動画タップ→YouTube→
                // アプリ復帰の経路ではvideoTapped=trueにならないため、復帰カード(showDoneNudge)と
                // この練習ブロックが同時に表示され「記録の入り口が二重」になっていた。復帰カードが
                // 出ている間は練習ブロックを隠す(!showDoneNudgeガード追加)。復帰カードを閉じた後に
                // 未記録なら練習ブロックが復活するのは許容(本人裁定どおり)。
                if fdGuideActive && !videoTapped && !showDoneNudge {
                    // TASK-C2-2026-08-05-build24-chip-clarity.md 追加項目R-2(本人生指摘「優しくない・
                    // 練習だと分かるようにして」): 一行の言い切りをやめ、練習ピル+優しい2行構成にする。
                    // ボタン自体(本番と同じ「きょうやった！」)と挙動は不変(Q-3/B-6の設計意図を維持)。
                    VStack(alignment: .center, spacing: 10) {
                        Text("＼ きろくの れんしゅう ／")
                            .kyonoFont(.black900, size: 12).foregroundColor(colors.tealInk)
                            .padding(.horizontal, 12).padding(.vertical, 4)
                            .background(Capsule().fill(colors.tealSoft))
                        VStack(alignment: .center, spacing: 4) {
                            Text("けっかはほんもの！つぎは、ストレッチのあとにおすボタンをためしてみよう")
                                .kyonoFont(.bold700, size: 14).foregroundColor(colors.ink)
                                .multilineTextAlignment(.center)
                            Text("まだやってなくても だいじょうぶ。ためしに1回おしてみて！")
                                .kyonoFont(.bold700, size: 13).foregroundColor(colors.sub)
                                .multilineTextAlignment(.center)
                        }
                        .frame(maxWidth: .infinity, alignment: .center)
                        KyonoPrimaryButton("きょうやった！") {
                            guard !videoTapped else { return }
                            videoTapped = true
                            performPracticeRecord()
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .center)
                    .id("practiceBlock")
                }
                // ダークモード再確認+rDoneNudge/rTourBtn実装タスク: index.html:745 #rDoneNudgeの1:1移植。
                // はじめの1本ガイド中、結果画面を表示したまま動画を見に行って戻ってきたときに、
                // ホームのcheerの代わりに結果画面内へ「やった？」の復帰案内を出す。
                if showDoneNudge && cardResult == nil {
                    KyonoCard {
                        // TASK-C2-2026-08-05-build23-bg-tuning-and-tour-tap.md W-2: ツアー中(YouTube
                        // 往復の練習)はこの復帰カードの一言を短く「おかえり！」にする(本人指定の文言)。
                        // 通常ユーザーの復帰ナッジは従来どおりの文言を維持。
                        Text(fdGuideActive ? "おかえり！" : "おかえりなさい！ ストレッチできた？").kyonoFont(.black900, size: 15).foregroundColor(colors.ink)
                        Spacer().frame(height: 10)
                        // D(本丸): fdGuide中はその場(結果画面)で記録を完結させる。ホームへは飛ばさない。
                        KyonoPrimaryButton(
                            fdGuideActive ? "1日目の記録をつけにいく" : "きょうの記録をつけにいく",
                            action: fdGuideActive ? performPracticeRecord : onDoneFromNudge
                        )
                        .scaleEffect(doneNudgeScale)
                    }
                    .id("doneNudgeCard")
                }
                // TASK-C2-2026-08-05-build28-round6.md R-18: 旧「1日目クリア！ナイスご自愛！」の
                // 労いカード(fdCelebrationVisible)は削除。performPracticeRecordはfdGuideActive時
                // にしか呼ばれず、ツアー中はこの労い演出自体を出さない裁定になったため(詳細は
                // performPracticeRecordのコメント参照)。
                // index.html:746 #rTourBtn(オンボ→クイズ経由・ツアー未見のときだけ)の1:1移植。
                if showTourBtn {
                    KyonoGhostButton("つづき：使い方ツアーへ", action: onStartTour)
                }
                // TASK-C2-2026-07-28-quiz-result-reach-parity.md §1: rGoHomeBtn/rRecheckBtnもガイド中は
                // 隠す(app-quiz.js:291-299の1:1移植。タブバーからの脱出は常に可能なため迷子にはならない)。
                if !fdGuideActive {
                    KyonoPrimaryButton("きょうの1本へ", action: onDone)
                    // TASK-C2-2026-08-01-build15-subtraction9.md #1: 「もう一回チェックする」は
                    // ホームのckCard.mini(再チェック導線)と完全重複のため削除(5視点監査③④で
                    // 独立に指摘・本人GO)。通常時(非ガイド)でも出さない。
                }
            }
            .padding(20)
        }
        .background(KyonoBackgroundColor().ignoresSafeArea())
        // D: HomeView.swift:342-346のKyonoConfettiと同じ作法(結果画面版)。
        if let confettiTrigger, !reduceMotion {
            KyonoConfetti(count: 70)
                .id(confettiTrigger)
                .allowsHitTesting(false)
        }
        }
        // D: HomeView.swift:830-872のカードモーダルと同じ作法(結果画面版・節目分岐は日1目には
        // 到達しないため省略)。
        .overlay {
            KyonoCardModalOverlay(isPresented: cardResult != nil, onClose: closeCardAndMaybeStartTour, scrimOpaque: true) {
                if let cardResult {
                    VStack {
                        Image(uiImage: cardResult.image).resizable().scaledToFit()
                            .accessibilityIdentifier("cardImage")
                        // TASK-C2-2026-08-05-build27-round5.md R-13(本人指示・一字一句このまま):
                        // 「自分用に画像を保存したり SNSでシェアしたりしてね！」をシェアボタン付近に追加。
                        // このモーダルはfdGuide練習カード専用(通常ユーザーはHomeView側の別モーダルを使う)
                        // ため、ツアー中限定の条件分岐は不要。
                        Text("自分用に画像を保存したり SNSでシェアしたりしてね！")
                            .kyonoFont(.bold700, size: 13).foregroundColor(colors.sub)
                            .multilineTextAlignment(.center)
                            .padding(.top, 8)
                        VStack(spacing: 12) {
                            KyonoPrimaryButton("保存・シェアする") {
                                ShareImage.share(uiImage: cardResult.image, text: "#きょうのオガトレ 1日目！")
                            }
                            KyonoLineButton("とじる", action: closeCardAndMaybeStartTour)
                        }
                    }
                }
            }
        }
        // A-3: HomeView.swift:600-621と同じ作法(パルス+0.15s後にscrollTo)。復帰後に
        // 「おかえりなさい」ブロックが画面外で気づけなかった欠落を解消する。
        .onChange(of: showDoneNudge) { _, newValue in
            guard newValue else { return }
            withAnimation(.easeInOut(duration: 0.35)) { doneNudgeScale = 1.045 }
            withAnimation(.easeInOut(duration: 0.35).delay(0.35)) { doneNudgeScale = 1 }
            withAnimation(.easeInOut(duration: 0.35).delay(0.7)) { doneNudgeScale = 1.045 }
            withAnimation(.easeInOut(duration: 0.35).delay(1.05)) { doneNudgeScale = 1 }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                if reduceMotion {
                    proxy.scrollTo("doneNudgeCard", anchor: .center)
                } else {
                    withAnimation { proxy.scrollTo("doneNudgeCard", anchor: .center) }
                }
            }
        }
        }
        }
    }
}

// MARK: - 使い方ツアー

// TASK-C2-2026-08-04-build20-home-cards-and-tour-tiers.md T-B: KyonoTourMockupを位置(index)では
// なくこのkeyで切り替える。スライド配列を並べ替えても絵がズレない設計にする(build19 T-1で
// 実際に起きた「見出し⇔絵のズレ」の再発防止)。
enum TourMockKind {
    case map, videoDaily, todayDone, cardDex, soudan, obu, myRecord
}

struct TourSlideDef {
    let title: String
    let desc: String
    let mock: TourMockKind
}

// TASK-C2-2026-08-04-build20-home-cards-and-tour-tiers.md T-A/T-B: 「体験一本道＋予告3枚」
// (build19 T-2)をさらに2段構えにする。共通プール7枚から、初回は「地図+まだ見ていない3枚」、
// 再生(使い方タブ)は「地図+全7枚のフルマニュアル」を切り出す(スライド配列の共通プール+
// 初回サブセット方式)。
let obTourPool: [TourSlideDef] = [
    // T-A(alan5指定文言・このまま): 初回1枚目に追加する「1日の流れ」地図。
    TourSlideDef(
        title: "まいにちやることは1つだけ",
        desc: "ホームの「きょうの1本」をみる→おわったら「きょうやった！」をおす\nこれだけで記録カードがたまっていくよ\nつぎの3枚は「こまったとき」の場所あんないだよ",
        mock: .map
    ),
    // T-B「復活3枚」(alan5指定文言・build18までと同一・このまま)。再生の7枚版にのみ含める。
    TourSlideDef(title: "まいにち1本、動画をやる", desc: "ホームの「きょうの1本」をタップ→YouTubeがひらくよ\n見おわったらこのアプリにもどってきてね", mock: .videoDaily),
    TourSlideDef(title: "おわったら「きょうやった！」", desc: "アプリにもどったらこのボタンを押すだけ\n連続と通算がのびるよ", mock: .todayDone),
    TourSlideDef(title: "ためると図鑑がうまる", desc: "記録カードは記念日・季節・レアなど何種類もあるよ\n「保存・シェアする」で写真にのこせて SNSやコメント欄にもどうぞ\n毎日の記録でカード図鑑がすこしずつうまっていく（マイ記録→お楽しみ機能）", mock: .cardDex),
    // build19 T-2の「予告3枚」(文言は変更なし)。初回サブセットにも含まれる。
    TourSlideDef(title: "悩みは相談室で質問", desc: "右下のボタンをタップ→「肩こり」のように打つか、チップを選ぶだけ\nオガトレ監修の答えとおすすめ動画がすぐ届くよ", mock: .soudan),
    // TASK-C2-2026-08-02-build17-feedback-fixes.md P-2: 「尾形さん」→「尾形」(本人指示・改行と同時)。
    TourSlideDef(title: "オガトレ通信をのぞく", desc: "尾形からのお知らせが届くよ\nホームいちばん上の「きょうのひとこと」も毎日かわります", mock: .obu),
    // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §6: Web版の「見てみる」ボタンはネイティブの
    // マイ記録に存在しない(お楽しみは🎉じまんカード/💬せんぱいの声/📔ひとことにっきの個別3ボタン)ため、
    // その3ボタンを直接指す文言に書きかえる(以前はWeb版UI前提の文言のまま移植されていた)。
    // B-10: 6機能列挙をやめて簡略化。
    TourSlideDef(title: "マイ記録でふりかえる", desc: "やった日に印がつくカレンダーがあるよ（×はつかないよ）\n毎日の合図（通知）は続ける設定からいつでも入れられるよ", mock: .myRecord),
]
// T-A: 初回は「地図(0)+予告3枚(4,5,6)」の4枚。「もう体験したことの再説明」(videoDaily/
// todayDone/cardDex)は初回では引き続き省く(build19 T-2の判断を継承)。
private let obTourFirstRunIndices = [0, 4, 5, 6]

// T-A/T-B: isFirstRun(初回=tryStartTour/オンボ直後・showClosing:trueの経路とオンボ埋め込み
// 経路の両方)は「地図+予告3枚」の4枚、再生(使い方タブ onReenterTour・isFirstRun:false)は
// プール全7枚のフルマニュアルを返す。
func obTourSlides(isFirstRun: Bool) -> [TourSlideDef] {
    isFirstRun ? obTourFirstRunIndices.map { obTourPool[$0] } : obTourPool
}
let obTourClosingTitle = "これで準備ばっちり！"
// TASK-C2-2026-08-04-build19-tour-redesign.md T-2(alan5指定文言・このまま): 削除した「忘れても
// だいじょうぶ」枚の内容をこの締めスライドに吸収する。
let obTourClosingDesc = "あしたも待ってるね\nきょうのぶんの動画は ホームの「きょうの1本」からどうぞ\n困ったら使い方タブの「使い方ツアー」でいつでも読み返せるよ"

// index.html:4283-4347 fdTourMaybeStart/obTourStep/obTourEndの1:1移植。3枚(T-2で7枚から
// 予告3枚+締めへ再構成)+条件付き4枚目(closing・自動起動時のみ)。「つぎへ」ボタン+進捗バーの
// リニアなステップ形式(スワイプ不使用)。T-3以降、進捗バーはツアー独自の点表示ではなく
// 体験ジャーニーバー(kyonoJourneySteps)の5段目「みどころ」を共用する。
// TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §6: 以前はRecordStoreを受け取らず
// テーマ・文字サイズを"auto"/trueに固定していたため、手動でライト設定にしている本人が夜に
// 再訪するとツアー画面だけダークになる不具合があった。他画面と同じくstoreの実設定を使う。
struct TourView: View {
    let store: RecordStore
    let showClosing: Bool
    // TASK-C2-2026-08-01-build13-round3.md ③⑦: 「📖 使い方ツアー」見出しを初回ジャーニー中
    // (tryStartTour経由/オンボ直後のクイズ経由)だけ表示するためのフラグ。デフォルトfalseは
    // 使い方タブ・ホームタブからの再入場を意図している。
    var isFirstRun: Bool = false
    let onDone: () -> Void

    @State private var si = 0

    private var slides: [TourSlideDef] { obTourSlides(isFirstRun: isFirstRun) }
    private var totalSlides: Int { slides.count + (showClosing ? 1 : 0) }

    var body: some View {
        KyonoTheme(themeSetting: store.get("theme", default: "light"), bigText: store.get("bigtext", default: true)) {
            TourContentView(si: $si, slides: slides, totalSlides: totalSlides, showClosing: showClosing, isFirstRun: isFirstRun, onDone: onDone)
        }
    }
}

private struct TourContentView: View {
    @Environment(\.kyonoColors) private var colors
    @Binding var si: Int
    let slides: [TourSlideDef]
    let totalSlides: Int
    let showClosing: Bool
    let isFirstRun: Bool
    let onDone: () -> Void

    var body: some View {
        // TestFlight実機フィードバックD6(2026-07-29): ステップごとに本文の量が違い、その下に
        // 置かれた「つぎへ」の位置が毎回上下に動いていた(押した指を置き直す必要があった)。
        // index.html:524 #obLog{flex:1;max-height:52vh;overflow-y:auto}/528 #obChips(ボタン専用の
        // 別要素)の1:1移植で、内容(#obLog相当)とボタン列(#obChips相当)を別の非スクロール領域に
        // 分ける。ボタン列を外側のVStackに出し、内容側だけをScrollViewにすることで、内容の
        // 長さに関わらずボタンの位置が1ptも動かなくなる(あふれるステップだけ中でスクロール)。
        VStack(spacing: 0) {
            // TASK-C2-2026-07-31-build11-renshu-journey.md D(本丸): 練習モードと同じ
            // KyonoJourneyBarにドット表示を置き換え、画面上部に固定する(本人の明示要求=
            // デザインの一貫性)。ラベルは番号だけで十分なため空文字列にする(circle内の
            // 数字/✓で進捗は伝わる)。
            // TASK-C2-2026-08-01-build13-round3.md ③⑦: 見出し「📖 使い方ツアー」を初回ジャーニー
            // (isFirstRun)のときだけオンボチャットと同じ見た目でバーの上に常設する。
            if isFirstRun {
                Text("使い方ツアー").kyonoFont(.black900, size: 16).foregroundColor(colors.ink)
                    .padding(.horizontal, 20).padding(.top, 20)
            }
            // TASK-C2-2026-08-04-build19-tour-redesign.md T-3: ツアー独自の(番号のみの)進捗バーを
            // 廃止し、体験ジャーニーバーの5段目「みどころ」を共用する(予告3枚+締めの間は常に
            // カレント)。
            // TASK-C2-2026-08-04-build20-home-cards-and-tour-tiers.md T-B: 再生(フル7枚マニュアル・
            // isFirstRun:false)ではジャーニーバーの「チェック✓の残骸」が意味不明になる(発注書の
            // 指摘)ため非表示にし、かわりに「N/7」の小さな頁表示にする。
            if isFirstRun {
                KyonoJourneyBar(labels: kyonoJourneySteps, currentIndex: kyonoJourneySteps.count - 1)
            } else {
                Text("\(si + 1)/\(slides.count)")
                    .kyonoFont(.bold700, size: 13).foregroundColor(colors.sub)
                    .frame(maxWidth: .infinity, alignment: .trailing)
                    .padding(.horizontal, 20).padding(.top, 12)
            }
            ScrollViewReader { proxy in
            // TASK-C2-2026-08-04-build19-tour-redesign.md T-5: 内容がボタン列より大きく上に寄り、
            // 画面中央がクリーム一色の余白になっていた。GeometryReaderで可視高さを取り、
            // 内容VStackにminHeight+alignment:.centerを与えて縦中央に寄せる。
            GeometryReader { outerGeo in
            ScrollView {
                VStack(alignment: .leading, spacing: 10) {
                    Color.clear.frame(height: 0).id("obTop")
                    if si < slides.count {
                        let slide = slides[si]
                        Text(slide.title).kyonoFont(.black900, size: 17).foregroundColor(colors.ink)
                        // index.html:4118-4142 各スライドv フィールド(実際の画面のミニチュアモックアップ)の1:1移植。
                        KyonoTourMockup(kind: slide.mock)
                        // TASK-C2-2026-08-04-build19-tour-redesign.md T-6: lineSpacing 11@14ptだと
                        // 行がバラけて痩せて見えていた(本人指摘)ため6へ詰める。1.5pt線枠は外し、
                        // colors.card塗り+角丸14のみのシンプルな箱にする。
                        Text(slide.desc).kyonoFont(.bold700, size: 14).foregroundColor(colors.ink).lineSpacing(6)
                            .padding(.horizontal, 14).padding(.vertical, 10)
                            .background(RoundedRectangle(cornerRadius: 14).fill(colors.card))
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
                }
                .padding(20)
                .frame(minHeight: outerGeo.size.height, alignment: .center)
            }
            // index.html:4308 log.scrollTop=0の1:1移植。内容側だけが独立してスクロールする
            // ようになったため、ステップが変わるたびに先頭へ戻さないと前のステップのスクロール
            // 位置が持ち越されて見える(Web版はそれが起きないようステップ描画のたびに毎回0へ戻す)。
            .onChange(of: si) { _, _ in proxy.scrollTo("obTop", anchor: .top) }
            }
            }
            // D6: ボタン列は内容のスクロールに関わらず画面下端に固定。
            // TASK-C2-2026-08-04-build19-tour-redesign.md T-4: 全幅ボタン3段積み(もどる/つぎへ/
            // とばす)が画面下1/3を占有し野暮ったかった(本人指摘)。全幅ボタンは黄色「つぎへ」
            // 1本だけにし、「もどる」「ツアーをとばす」は1行に並べる細身のテキストリンクへ格下げ
            // する(枠・塗りなし・タップ領域は高さ44pt確保)。締めスライドは「おわる」黄1本のみ
            // (もどるも省く・alan5指定)。
            VStack(spacing: 10) {
                KyonoPrimaryButton(si < totalSlides - 1 ? "つぎへ" : "おわる") {
                    if si < totalSlides - 1 { si += 1 } else { onDone() }
                }
                if si < totalSlides - 1 {
                    HStack {
                        if si > 0 {
                            Button { si -= 1 } label: {
                                Text("◀ もどる").kyonoFont(.extraBold800, size: 15).foregroundColor(colors.sub2)
                                    .frame(minHeight: 44)
                            }
                            .buttonStyle(.plain)
                        }
                        Spacer()
                        Button(action: onDone) {
                            Text("ツアーをとばす").kyonoFont(.black900, size: 15).foregroundColor(colors.tealInk)
                                .frame(minHeight: 44)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .padding(.horizontal, 20).padding(.top, 10).padding(.bottom, 20)
        }
        .background(KyonoBackgroundColor().ignoresSafeArea())
    }
}
