//
//  SoudanSheetView.swift
//  KyouNoOgatore
//
//  ネイティブ移植 Step 6(マスタープラン§6 Step 6・§2-1「相談室エンジン sd*一式」対応): 相談室チャットUI
//  (Android版SoudanSheet.ktと同一ロジックのSwiftUI実装)。判定はStep2で移植済みのSafetyGate、通常応答の
//  内容選定はこのStepで拡張したSoudanEngineを呼ぶだけで、このファイルには判定コードを一切書かない
//  (マスタープラン§3-2・§3-4手順6。grep確認対象)。
//
//  カテゴリタブによる絞り込み(sdActiveCat/sdCatIds)はindex.html:2988-2994の1:1移植だが、これは
//  表示グルーピングのための単純な配列レンジ抽出であり安全判定ではないため、SoudanEngine(SafetyCore)
//  ではなくこのUIファイル側に置く(マスタープラン§2-1のSoudanSheetView/SoudanSheet行の役割分担どおり)。
//
//  未移植(Step6のスコープ外として明示的に見送り。Android版と同じ判断): 雑談(smalltalk 54件)・
//  自由入力でのfollowup同義語マッチ(SD_FU_KW)・動画サムネイル画像の読み込み・
//  タイプ診断との相性演出(sdTypeFlavor)。タイピングドット+吹き出し分割タイミング演出は
//  TASK-C2-2026-07-27-soudan-staged-reveal.mdで追加移植した(下記applyResponse参照)。
//
//  ⚠️ Step6時点の検収基準どおり、iOS側はビルド確認のみでシミュレータ実行確認は必須要件ではない
//  (Android版で実タップ確認済み・同一ロジックのコードレビューで信頼度を補完する運用。マスタープラン§4-2)。

import SwiftUI
import RecordCore
import SafetyCore

struct SdCatDef {
    let key: String
    let label: String
    let from: String
    let to: String
}

// index.html:2981-2987 SOUDAN_CHIP_CATS の1:1移植(悩み一覧を5つの大項目タブに束ねる境界)。
let sdChipCats: [SdCatDef] = [
    SdCatDef(key: "body", label: "からだの部位で", from: "katakori", to: "oshirikori"),
    SdCatDef(key: "ashi", label: "脚・足まわりで", from: "momomae", to: "ashidaru"),
    SdCatDef(key: "scene", label: "状況・シーンで", from: "deskwork", to: "shakitto"),
    SdCatDef(key: "nayami", label: "お悩み・体型で", from: "tsukare", to: "wakibara"),
    SdCatDef(key: "howto", label: "やり方・Q&Aで", from: "mainichi", to: "kubinaru"),
]

// index.html:1827 PLAN_EXCLUDE_INTENTS の1:1移植(「即中止して様子見」が答えのintentは矛盾するため除外)。
private let planExcludeIntent = "itakunatta"

// index.html:1755 kyono_plan の1:1移植(store方式・保存は1本だけ)。
struct SdPlanData: Codable {
    let intentId: String
    let label: String
    let videos: [String]
    let start: String
    var days: Int = 14
}

// index.html:2989-2994 sdCatIds の1:1移植。
func sdCatIntentIds(_ cat: SdCatDef, _ intents: [SafetyKB.Intent]) -> [String] {
    guard let i0 = intents.firstIndex(where: { $0.id == cat.from }),
          let i1 = intents.firstIndex(where: { $0.id == cat.to }) else {
        return intents.map { $0.id }
    }
    return intents[i0...i1].map { $0.id }
}

enum SdBubble {
    case bot(text: String, red: Bool, videoId: String?, fallbackCaution: Bool = false)
    case user(text: String)
    case planConfirm(intentId: String, label: String, replacing: Bool)
    // index.html:3323-3330 sdAnswerFallback内の2通目(逃げ道リンク3つ)の1:1移植。rawUserTextはmailto本文用。
    case fallbackLinks(rawUserText: String)
    // TASK-C2-2026-07-27-soudan-staged-reveal.md: index.html:3084 sdTypingNode()の1:1移植。
    case typing
}

// TASK-C2-2026-07-27-ios-sdbubble-unstable-id.md: SdBubble自体に`var id: String { UUID().uuidString }`
// という計算プロパティでIdentifiable適合させていたが、アクセスのたびに新しいUUIDを返すため
// ForEachの差分更新が壊れ、再描画のたびに全要素のViewが破棄・再生成されていた(段階表示の
// タイピングドットアニメーションが`.onAppear`の再発火で潰され続ける症状として顕在化)。
// 生成時に1回だけ確定するidを持つラッパーに差し替えて安定させる。
struct SdMessage: Identifiable {
    let id = UUID()
    let bubble: SdBubble
}

// index.html:3053 sdMsgLen()の1:1移植(タイピング待ち時間の計算専用。空白を除いた文字数)。
private func sdMsgLen(_ text: String) -> Int {
    text.filter { !$0.isWhitespace }.count
}

// タイピング待ち時間計算のために各吹き出しから代表テキストを取り出す。Web版はHTML文字列全体から
// タグを除いた文字数を使うため、動画注記込みの吹き出しもその見出しテキストで近似する。
private func sdBubbleLen(_ b: SdBubble) -> Int {
    switch b {
    case let .bot(text, _, _, _): return sdMsgLen(text)
    case .fallbackLinks: return sdMsgLen("この悩み、オガトレに届けるメールがひらかない方はアドレスをコピー動画を探すタブでさがしてみる")
    default: return 0
    }
}

// アクセシビリティ対応(スクリーンリーダー無音問題の解消): 新しく追加された吹き出し「1件だけ」を
// VoiceOverへ読み上げるための代表テキスト抽出。sdBubbleLen()と同じ「代表テキストで近似する」
// 考え方の踏襲(表示用ではなく読み上げ専用の文字列)。.typingは「…」の点滅ドットのみで読み上げる
// 実体が無いためnilを返す(タイピング演出中に毎回読み上げが発火するのを防ぐ)。
private func sdBubbleAnnounceText(_ b: SdBubble) -> String? {
    switch b {
    case let .user(text):
        return text
    case let .bot(text, _, _, _):
        return text.isEmpty ? nil : text
    case let .planConfirm(_, label, replacing):
        return replacing
            ? "いまのプランと入れ替える？きょうの1本が、あなたの\(label)プランになるよ"
            : "きょうの1本が、あなたの\(label)プランになるよ！2週間いっしょにやってみる？"
    case .fallbackLinks:
        return "この悩み、オガトレに届けるメールがひらかない方はアドレスをコピー動画を探すタブでさがしてみる"
    case .typing:
        return nil
    }
}

// UIAccessibility.post(.announcement, argument:)は「渡した文字列を1回読み上げる」だけの命令的API
// であり、Viewのregionを丸ごと再読み上げする仕組みとは無関係。呼び出し側(=吹き出しをmessagesに
// 積む各箇所)で「いま積んだ1件」だけを渡すことで、過去の吹き出しには一切触れず、追加された
// 吹き出しだけが読まれることを保証する。
private func announceBubble(_ b: SdBubble) {
    guard let text = sdBubbleAnnounceText(b) else { return }
    UIAccessibility.post(notification: .announcement, argument: text)
}

// index.html:2979 SOUDAN_BODY_INTENTS の1:1移植(体の悩み系intent=かたさチェック誘導チップの対象)。
private let soudanBodyIntents: Set<String> = ["katakori", "youtsuu", "zenkutsu", "kokansetsu", "ashikubi", "kaikyaku", "nekoze", "nemuri", "zenshin"]
// index.html:2946 SD_MAIL の1:1移植(検索タブのリクエスト導線と同じ宛先)。
private let sdMail = "kyou-no@ogatore.jp"

enum SdChipsMode: Equatable {
    case none
    case intents(activeCat: String)
    case followups(intentId: String, nextBestId: String?)
    case nearmiss(ids: [String])
}

struct SoudanSheetView: View {
    let store: RecordStore
    let openUrl: (String) -> Void
    let onClose: () -> Void
    var presetIntentId: String?
    var greeted: Bool = false
    var onGreeted: () -> Void = {}
    var onOpenSearch: () -> Void = {}
    var onOpenQuiz: () -> Void = {}

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    private let kb = SafetyKBLoader.shared
    @State private var messages: [SdMessage] = []
    @State private var chipsMode: SdChipsMode = .intents(activeCat: "body")
    @State private var lastIntentId: String?
    @State private var shownVideoIds: [String] = [] // index.html:2999 sdCtx.shownVideoIds相当(セッション内のみ)
    @State private var input = ""
    @State private var plan: SdPlanData?
    // TASK-C2-2026-07-27-soudan-staged-reveal.md: index.html:3096 sdPending(応答演出中は次の
    // チップタップ/送信を受け付けない)の1:1移植。演出中の吹き出し順序が入り乱れるのを防ぐ。
    @State private var sdPending = false
    private var hasType: Bool { store.get("type", default: nil as QuizTypeResult?) != nil } // index.html:3024 sdHasType()の1:1移植

    init(
        store: RecordStore, openUrl: @escaping (String) -> Void, onClose: @escaping () -> Void, presetIntentId: String? = nil,
        greeted: Bool = false, onGreeted: @escaping () -> Void = {}, onOpenSearch: @escaping () -> Void = {}, onOpenQuiz: @escaping () -> Void = {}
    ) {
        self.store = store
        self.openUrl = openUrl
        self.onClose = onClose
        self.presetIntentId = presetIntentId
        self.greeted = greeted
        self.onGreeted = onGreeted
        self.onOpenSearch = onOpenSearch
        self.onOpenQuiz = onOpenQuiz
        _plan = State(initialValue: store.get("plan", default: nil))
    }

    // index.html:3090-3134 sdPush()の1:1移植。bot発言を1つずつ、タイピングドットを挟みながら
    // 段階的に表示する。チップ列(chipsMode)の更新はWeb版のsdRenderChips()と同じく、
    // 全吹き出しの表示が終わったタイミングでまとめて行う(表示中は直前のチップがそのまま残る)。
    // カテゴリタブの早期畳み(index.html:3097-3100)はSwiftUIのレイアウトでは該当する見切れ問題が
    // 起きないため見送り(表示タイミングの本質=段階表示自体は1:1)。
    private func revealBotMessages(_ botMsgs: [SdBubble], _ newChipsMode: SdChipsMode) async {
        // §D: index.html:3051 sdReduced()がtrueの場合、queueNext()はタイピングドット+待機を
        // 挟まず即座に全件を表示する(index.html:3119-3131 if(reduced){showNext();return;})の1:1移植。
        if reduceMotion {
            messages.append(contentsOf: botMsgs.map { SdMessage(bubble: $0) })
            chipsMode = newChipsMode
            // reduced-motion時は演出無しでまとめて積むが、読み上げは1件ずつ順番に発火させる
            // (複数の新規吹き出しがまとめて届いた場合でも、それぞれの新規テキストのみを読む。
            // 過去の吹き出しの再読み上げにはならない)。
            for msg in botMsgs { announceBubble(msg) }
            return
        }
        for (i, msg) in botMsgs.enumerated() {
            messages.append(SdMessage(bubble: .typing))
            let base = min(1600, 500 + sdBubbleLen(msg) * 22)
            let wait = i == 0 ? 400 : (i == 1 ? base : base * 2)
            try? await Task.sleep(nanoseconds: UInt64(wait) * 1_000_000)
            messages.removeLast()
            messages.append(SdMessage(bubble: msg))
            // アクセシビリティ対応: タイピングドットを実際のbot発言に差し替えた直後、その1件だけを通知する。
            announceBubble(msg)
        }
        chipsMode = newChipsMode
    }

    // index.html:3090 sdPush相当。応答1件をbot/userの吹き出し列へ展開しchipsModeを更新する。
    private func applyResponse(_ userText: String?, _ r: SoudanResponse) async {
        if let userText {
            let userBubble = SdBubble.user(text: userText)
            messages.append(SdMessage(bubble: userBubble))
            announceBubble(userBubble)
        }
        let red: Bool
        switch r.verdict {
        case .crisis, .redFlag: red = true
        case .normal: red = false
        }
        var botMsgs: [SdBubble] = []
        if !r.empathy.isEmpty { botMsgs.append(.bot(text: r.empathy, red: red, videoId: nil)) }
        if !r.message.isEmpty { botMsgs.append(.bot(text: r.message, red: red, videoId: nil, fallbackCaution: r.isFallback)) }
        if let v = r.video {
            botMsgs.append(.bot(text: v.note.isEmpty ? "おすすめの1本" : v.note, red: false, videoId: v.videoId))
            if !shownVideoIds.contains(v.videoId) { shownVideoIds.append(v.videoId) }
        }
        if !r.keizoku.isEmpty { botMsgs.append(.bot(text: r.keizoku, red: false, videoId: nil)) }
        // index.html:3328-3330 sdAnswerFallback2通目(逃げ道リンク3つ)の1:1移植。
        if r.isFallback { botMsgs.append(.fallbackLinks(rawUserText: userText ?? "")) }
        if let intentId = r.intentId { lastIntentId = intentId }
        let newChipsMode: SdChipsMode
        switch r.verdict {
        case .crisis:
            newChipsMode = .none // index.html:3310 チップ・カテゴリタブなし
        case .redFlag:
            newChipsMode = .intents(activeCat: "body") // index.html:3304
        case .normal:
            if r.hasFollowup, let intentId = r.intentId {
                newChipsMode = .followups(intentId: intentId, nextBestId: r.nextBestChip?.id)
            } else if !r.nearmissChips.isEmpty {
                newChipsMode = .nearmiss(ids: r.nearmissChips.map { $0.id })
            } else {
                newChipsMode = .intents(activeCat: "body")
            }
        }
        await revealBotMessages(botMsgs, newChipsMode)
    }

    // index.html:3361-3384 sdChipTap/sdFollowupTap/sdSendの「if(sdPending) return」の1:1移植。
    private func runResponse(_ userText: String?, _ r: SoudanResponse) {
        guard !sdPending else { return }
        sdPending = true
        Task {
            await applyResponse(userText, r)
            sdPending = false
        }
    }

    private func sendText() {
        guard !sdPending else { return }
        let raw = input.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !raw.isEmpty else { return }
        input = ""
        runResponse(raw, SoudanEngine.respond(to: raw))
    }

    private func chipTap(_ id: String) {
        guard !sdPending else { return }
        guard let intent = kb.intents.first(where: { $0.id == id }), let r = SoudanEngine.respondToIntent(id: id) else { return }
        runResponse(intent.chip, r)
    }

    private func followupTap(_ id: String) {
        guard !sdPending else { return }
        guard let f = kb.commonFollowups.first(where: { $0.id == id }),
              let r = SoudanEngine.respondToFollowup(id: id, lastIntentId: lastIntentId, shownVideoIds: shownVideoIds) else { return }
        runResponse(f.chip, r)
    }

    // index.html:1844 planChipTap相当。即開始はせず確認の吹き出しを積む。
    private func planChipTap(_ id: String) {
        guard let intent = kb.intents.first(where: { $0.id == id }) else { return }
        let replacing = plan != nil && plan?.intentId != id
        let userBubble = SdBubble.user(text: "📅 この悩みを2週間プランにする")
        let confirmBubble = SdBubble.planConfirm(intentId: id, label: intent.chip, replacing: replacing)
        messages.append(SdMessage(bubble: userBubble))
        announceBubble(userBubble)
        messages.append(SdMessage(bubble: confirmBubble))
        announceBubble(confirmBubble)
    }

    // index.html:1857 planStart相当。
    private func planStart(_ id: String) {
        guard let intent = kb.intents.first(where: { $0.id == id }) else { return }
        var seen = Set<String>()
        let vids = (intent.videos ?? []).map { $0.v }.filter { seen.insert($0).inserted }
        guard !vids.isEmpty else { return }
        let today = RecordLogic.todayStr(now: Date())
        let newPlan = SdPlanData(intentId: id, label: intent.chip, videos: vids, start: today, days: 14)
        store.set("plan", newPlan)
        plan = newPlan
        let bubble = SdBubble.bot(text: "よし、きょうから14日間いっしょにやろう！ホームの「きょうの1本」が\(intent.chip)用になったよ😊", red: false, videoId: nil)
        messages.append(SdMessage(bubble: bubble))
        announceBubble(bubble)
    }

    private func planDecline() {
        let bubble = SdBubble.bot(text: "OK！1本ずつでも十分えらいよ😊 プランにしたくなったら、いつでもここから組めるからね", red: false, videoId: nil)
        messages.append(SdMessage(bubble: bubble))
        announceBubble(bubble)
    }

    private var themeSetting: String { store.get("theme", default: "auto") }

    // ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
    // Phase 3: index.html:459-489 .sd-sheet/.sd-head/.sd-b/.chip/.catbtnの1:1移植。見た目の変更のみで、
    // 上の判定・状態管理ロジック(applyResponse/chipTap/sendText等)には一切手を入れていない。
    var body: some View {
        KyonoTheme(themeSetting: themeSetting, bigText: store.get("bigtext", default: true)) {
            content
        }
    }

    private var content: some View {
        SoudanContentView(
            messages: messages, chipsMode: chipsMode, input: $input, plan: plan,
            kb: kb, hasType: hasType, sdPending: sdPending, onClose: onClose, openUrl: openUrl,
            onSend: sendText, onChip: chipTap, onFollowup: followupTap,
            onPlanChip: planChipTap, onPlanStart: planStart, onPlanDecline: planDecline,
            onCatSelect: { key in chipsMode = .intents(activeCat: key) },
            onOpenSearch: onOpenSearch, onOpenQuiz: onOpenQuiz
        )
        // TASK-C2-2026-07-27-soudan-safety-copy-and-links: index.html:3459 sdEnsureGreeting()の1:1移植。
        // 相談室をアプリのこのセッションで初めて開いたときだけ、吹き出し形式であいさつを1通出す
        // (greetedはWeb版sdGreetedと同じくセッション内のみ・store非永続。呼び出し元がルート階層で保持する)。
        // ホーム構造修正タスクのpresetIntentId自動応答(index.html:3464-3467)より先に出す(Web版と同順)。
        .onAppear {
            if !greeted {
                let greetBubble = SdBubble.bot(text: "こんにちは、オガトレです！からだの悩み、なんでも聞かせて😊\n下のチップか、ことばで入力してね", red: false, videoId: nil)
                messages.append(SdMessage(bubble: greetBubble))
                announceBubble(greetBubble)
                onGreeted()
            }
            if let presetIntentId { chipTap(presetIntentId) }
        }
    }
}

// KyonoColors解決の都合上(HomeView.swift冒頭コメント参照: @Environmentは自分を包むKyonoThemeの
// 子孫でなければ既定値のまま)、実際の描画は別のView構造体に切り出す。ロジックは一切持たず、
// 親から渡された状態・コールバックをそのまま描画するだけ。
private struct SoudanContentView: View {
    @Environment(\.kyonoColors) private var colors
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    let messages: [SdMessage]
    let chipsMode: SdChipsMode
    @Binding var input: String
    let plan: SdPlanData?
    let kb: SafetyKB
    let hasType: Bool
    let sdPending: Bool
    let onClose: () -> Void
    let openUrl: (String) -> Void
    let onSend: () -> Void
    let onChip: (String) -> Void
    let onFollowup: (String) -> Void
    let onPlanChip: (String) -> Void
    let onPlanStart: (String) -> Void
    let onPlanDecline: () -> Void
    let onCatSelect: (String) -> Void
    let onOpenSearch: () -> Void
    let onOpenQuiz: () -> Void

    // TestFlight実機フィードバックB5(2026-07-29): 新しい発言が増えても自動で下へスクロール
    // しなかった(index.html:3056 sdScrollEnd()/3061 sdScrollToTop()相当の実装が丸ごと欠落)。
    // チップを押しても画面が動かず「押せたか分からない」体験になっていた。
    private static let bottomAnchorID = "sdBottomAnchor"

    var body: some View {
        ScrollViewReader { proxy in
        VStack(spacing: 0) {
            // index.html:461-465 .sd-head(ヘッダー・円形×クローズボタン)
            HStack {
                KyonoSectionHeader(icon: .soudanBubble, title: "オガトレ相談室", fill: colors.tealSoft, accent: colors.teal)
                Spacer()
                // GO-G3(5視点ワンループ): index.html:464の視覚サイズ(40px)は変えず、タップ領域だけ
                // 44pt相当に広げる(透明な44x44の外枠の中に従来どおりの見た目の40x40円を重ねる)。
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
            }
            .padding(.horizontal, 16).padding(.vertical, 12)
            .background(colors.card)
            // index.html:1223 .sd-disc(2行・2行目後半は太字強調)。Text連結(+)はText型のみ許容する
            // ため、ここだけ.kyonoFont(ViewModifier)ではなく.font(.kyono(...))を直接使う
            // (bigtextの1.18倍はこの1箇所のみ非適用・影響は軽微)。
            (Text("※回答はオガトレ監修のパターン集から選んでいます\n※目安をつかむ相談室です ")
                .font(.kyono(.bold700, size: 13))
                + Text("強い痛み・しびれがあるときは医療機関へ").font(.kyono(.black900, size: 13)))
                .foregroundColor(colors.sub).multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 16).padding(.vertical, 6)
                .background(colors.card)
            Divider()

            ScrollView {
                // TASK-C2-2026-07-27-chips-overflow-and-bubble-pop.md §3: index.html:3079 .sd-pop
                // (opacity0→1・translateY(4px)→0・.18s ease-out)の1:1移植。reduced-motion時は
                // 無演出即表示。messages.countの変化にanimation(value:)を効かせ、追加されたぶんの
                // ForEach挿入トランジションとして再生させる(各appendを個別にwithAnimationで
                // 囲む代わりに、ここ1箇所でまとめて対応)。
                VStack(alignment: .leading, spacing: 10) {
                    ForEach(messages) { m in bubbleView(m.bubble).transition(.sdPop) }
                    // B5: scrollTo(anchor:.bottom)の着地点。見えない1ptのマーカーで、
                    // 最後の吹き出しがチップ行の直上ぎりぎりで止まらず余白を持って収まる。
                    Color.clear.frame(height: 1).id(Self.bottomAnchorID)
                }
                .padding(16)
                .animation(reduceMotion ? nil : .easeOut(duration: 0.18), value: messages.count)
            }
            .onChange(of: messages.count) { _, _ in
                // index.html:3056 sdScrollEnd()/3061 sdScrollToTop()相当。Web版は新規bot行ごとに
                // 「行の頭」へ、ユーザー発言時は最下部へ、と細かく使い分けているが、ここでは
                // 「新しい発言が増えたら最下部へ」という指示どおりの単純な形にする。
                if reduceMotion {
                    proxy.scrollTo(Self.bottomAnchorID, anchor: .bottom)
                } else {
                    withAnimation { proxy.scrollTo(Self.bottomAnchorID, anchor: .bottom) }
                }
            }
            .onAppear {
                // index.html:3460 「再オープン時もログ最下部から」の1:1移植。
                proxy.scrollTo(Self.bottomAnchorID, anchor: .bottom)
            }

            chipsView
        }
        .background(colors.bg)
        }
    }

    @ViewBuilder
    private func bubbleView(_ m: SdBubble) -> some View {
        switch m {
        // index.html:482-483 .sd-row.user .sd-b(黄色系吹き出し・右寄せ)
        case let .user(text):
            HStack {
                Spacer()
                // UI/UXパリティ監査GO-12(2026-07-28): index.html:481 .sd-b{font-size:15px;
                // line-height:1.75}の1:1移植。lineSpacing指定なしでSwiftUI既定にフォールバックし、
                // Web版より行間が詰まっていた欠落を修正する。
                Text(text).kyonoFont(.bold700, size: 15).foregroundColor(colors.ink).lineSpacing(11)
                    .padding(.horizontal, 14).padding(.vertical, 10)
                    .background(RoundedCorner(radius: 16, corners: [.topLeft, .topRight, .bottomLeft]).fill(colors.yellowSoft))
            }
        // index.html:481,488,3080 .sd-b/.sd-row.sd-red .sd-b(通常=card+line枠・赤旗=coral-soft+coral枠)/
        // .sd-ava(chara-hitokoto.pngアバター・botメッセージのみ)の1:1移植。
        case let .bot(text, red, videoId, fallbackCaution):
            let bg = red ? colors.coralSoft : colors.card
            let border = red ? colors.coral : colors.line
            HStack(alignment: .bottom) {
                KyonoCharaImage(name: "chara-hitokoto").frame(width: 38, height: 38)
                VStack(alignment: .leading, spacing: 6) {
                    // UI/UXパリティ監査GO-12(2026-07-28): index.html:481 .sd-b{font-size:15px;
                    // line-height:1.75}の1:1移植。
                    if !text.isEmpty { Text(text).kyonoFont(.bold700, size: 15).foregroundColor(colors.ink).lineSpacing(11) }
                    // index.html:3330 sdAnswerFallback1通目の末尾<span>(小さめ注意書き)の1:1移植。
                    if fallbackCaution {
                        Text("※つらい症状（強い痛み・胸の苦しさ・熱など）があるときは、メールより先に医療機関に相談してね")
                            .kyonoFont(.bold700, size: 13).foregroundColor(colors.sub)
                    }
                    if let videoId {
                        KyonoGhostButton("▶ 動画を見る") { openUrl("https://www.youtube.com/watch?v=\(videoId)") }
                    }
                }
                .padding(.horizontal, 14).padding(.vertical, 10)
                .background(
                    RoundedCorner(radius: 16, corners: [.topLeft, .topRight, .bottomRight]).fill(bg)
                        .overlay(RoundedCorner(radius: 16, corners: [.topLeft, .topRight, .bottomRight]).stroke(border, lineWidth: 1.5))
                )
                .frame(maxWidth: 320, alignment: .leading)
                Spacer()
            }
        // TASK-C2-2026-07-27-soudan-staged-reveal.md: index.html:3084 sdTypingNode()の1:1移植。
        // 「…」の3点を位相をずらして明滅させるタイピングドット。
        case .typing:
            HStack(alignment: .bottom) {
                KyonoCharaImage(name: "chara-hitokoto").frame(width: 38, height: 38)
                SdTypingDots()
                    .padding(.horizontal, 16).padding(.vertical, 14)
                    .background(
                        RoundedCorner(radius: 16, corners: [.topLeft, .topRight, .bottomRight]).fill(colors.card)
                            .overlay(RoundedCorner(radius: 16, corners: [.topLeft, .topRight, .bottomRight]).stroke(colors.line, lineWidth: 1.5))
                    )
                Spacer()
            }
        // index.html:3323-3330 sdAnswerFallback2通目(逃げ道リンク3つ)の1:1移植。
        // ①mailto(検索タブと同じopenMailTo流用)②クリップボードコピー③検索タブへ遷移。
        case let .fallbackLinks(rawUserText):
            HStack(alignment: .bottom) {
                KyonoCharaImage(name: "chara-hitokoto").frame(width: 38, height: 38)
                FallbackLinksView(rawUserText: rawUserText, onOpenSearch: onOpenSearch)
                    .padding(.horizontal, 14).padding(.vertical, 10)
                    .background(
                        RoundedCorner(radius: 16, corners: [.topLeft, .topRight, .bottomRight]).fill(colors.card)
                            .overlay(RoundedCorner(radius: 16, corners: [.topLeft, .topRight, .bottomRight]).stroke(colors.line, lineWidth: 1.5))
                    )
                    .frame(maxWidth: 320, alignment: .leading)
                Spacer()
            }
        case let .planConfirm(intentId, label, replacing):
            HStack(alignment: .bottom) {
                KyonoCharaImage(name: "chara-hitokoto").frame(width: 38, height: 38)
                VStack(alignment: .leading, spacing: 8) {
                    Text(replacing
                        ? "いまのプランと入れ替える？きょうの1本が、あなたの\(label)プランになるよ"
                        : "きょうの1本が、あなたの\(label)プランになるよ！2週間いっしょにやってみる？")
                        .kyonoFont(.bold700, size: 15).foregroundColor(colors.ink)
                    KyonoPrimaryButton(replacing ? "入れ替えてはじめる！" : "はじめる！") { onPlanStart(intentId) }
                    KyonoGhostButton("まずは1本だけ") { onPlanDecline() }
                }
                .padding(.horizontal, 14).padding(.vertical, 10)
                .background(
                    RoundedCorner(radius: 16, corners: [.topLeft, .topRight, .bottomRight]).fill(colors.card)
                        .overlay(RoundedCorner(radius: 16, corners: [.topLeft, .topRight, .bottomRight]).stroke(colors.line, lineWidth: 1.5))
                )
                .frame(maxWidth: 320, alignment: .leading)
                Spacer()
            }
        }
    }

    // ---- チップ列(index.html:3139 sdRenderChips相当・.chip/.catbtnの1:1移植) ----
    @ViewBuilder
    private var chipsView: some View {
        VStack(alignment: .leading, spacing: 6) {
            switch chipsMode {
            case .none:
                EmptyView() // crisis直後: チップ・カテゴリタブなし(index.html:3143-3145)
            case let .intents(activeCat):
                // TASK-C2-2026-07-27-chips-overflow-and-bubble-pop.md §2/§5: index.html:471
                // .sd-foot .sd-catrow{flex-wrap:wrap}の1:1移植。相談室フッターのチップ行は
                // 例外的に横スクロールだが、カテゴリ行だけは折り返しに戻る指定。
                FlowLayout(spacing: 8, lineSpacing: 6, alignment: .leading) {
                    ForEach(sdChipCats, id: \.key) { cat in
                        KyonoCatButton(label: cat.label, selected: cat.key == activeCat) { onCatSelect(cat.key) }
                    }
                }
                let cat = sdChipCats.first { $0.key == activeCat } ?? sdChipCats[0]
                let ids = Set(sdCatIntentIds(cat, kb.intents))
                // TASK-C2-2026-07-27-chips-overflow-and-bubble-pop.md §1: index.html:470
                // .sd-foot .chips{flex-wrap:nowrap;overflow-x:auto}(相談室フッターのチップ行
                // だけの例外的な横スクロール)+472 .fade-r(右端フェード+「›」ヒント)の1:1移植。
                FadingChipRow {
                    ForEach(kb.intents.filter { ids.contains($0.id) }, id: \.id) { intent in
                        KyonoChip(label: intent.chip) { onChip(intent.id) }
                    }
                }
            case let .followups(intentId, nextBestId):
                let intent = kb.intents.first { $0.id == intentId }
                // index.html:1828 planInjectChip相当: 動画2本以上・除外intentでない・実行中プランと同一でないときだけ出す
                let showPlanChip = intent != nil && (intent?.videos?.count ?? 0) >= 2 &&
                    intent?.id != planExcludeIntent && plan?.intentId != intent?.id
                FadingChipRow {
                        if showPlanChip, let intent {
                            KyonoChip(label: "📅 この悩みを2週間プランにする") { onPlanChip(intent.id) }
                        }
                        ForEach(intent?.followups ?? [], id: \.self) { fid in
                            if let f = kb.commonFollowups.first(where: { $0.id == fid }) {
                                KyonoChip(label: f.chip) { onFollowup(fid) }
                            } else if let li = kb.intents.first(where: { $0.id == fid }) {
                                KyonoChip(label: li.chip) { onChip(fid) }
                            }
                        }
                        if let nextBestId, let nb = kb.intents.first(where: { $0.id == nextBestId }) {
                            KyonoChip(label: "\(nb.chip)の話も") { onChip(nb.id) }
                        }
                        // index.html:3160-3163 未チェックの人への導線(体の悩み系回答にだけ出す)の1:1移植。
                        if let intent, !(intent.safety ?? false), !hasType, soudanBodyIntents.contains(intent.id) {
                            KyonoChip(label: "30秒のかたさチェックやってみる?") { onOpenQuiz() }
                        }
                        KyonoChip(label: "べつの悩みをそうだん") { onCatSelect("body") }
                }
            case let .nearmiss(ids):
                FadingChipRow {
                        ForEach(ids, id: \.self) { id in
                            if let intent = kb.intents.first(where: { $0.id == id }) {
                                KyonoChip(label: intent.chip) { onChip(id) }
                            }
                        }
                        KyonoChip(label: "べつの悩みをそうだん") { onCatSelect("body") }
                }
            }

            HStack {
                TextField("気になることを入力", text: $input).textFieldStyle(.roundedBorder)
                KyonoPrimaryButton("送信", enabled: !sdPending, action: onSend).frame(width: 90)
            }
            // GO-G16(5視点ワンループ): 高さ92%のシートで✕が最上部右端40×40だけだったため、
            // 入力欄の下にも「とじる」を1つ追加する。
            Spacer().frame(height: 8)
            KyonoLineButton("とじる", action: onClose)
        }
        .padding(.horizontal, 14).padding(.vertical, 8)
        .background(colors.card)
    }
}

// TASK-C2-2026-07-27-soudan-staged-reveal.md: index.html:3084 sdTypingNode()の中身
// (「…」3点を位相をずらして明滅させるドット)。
private struct SdTypingDots: View {
    @Environment(\.kyonoColors) private var colors
    @State private var animate = false

    var body: some View {
        HStack(spacing: 4) {
            ForEach(0..<3, id: \.self) { i in
                Circle()
                    .fill(colors.sub.opacity(animate ? 1 : 0.3))
                    .frame(width: 7, height: 7)
                    .animation(
                        .easeInOut(duration: 0.6).repeatForever(autoreverses: true).delay(Double(i) * 0.15),
                        value: animate
                    )
            }
        }
        .onAppear { animate = true }
    }
}

// index.html:3323-3330 sdAnswerFallback2通目(逃げ道リンク3つ)の中身。envのkyonoColors解決の都合上
// (HomeView.swift冒頭コメント参照)、独立したView構造体に切り出す。
private struct FallbackLinksView: View {
    @Environment(\.kyonoColors) private var colors
    // GO-G11(5視点ワンループ): 「コピーしました」等の一時メッセージの自動消滅時間を、既存の
    // bigtext環境値に連動させる(ONのときは読み切れるよう4秒へ延ばす)。
    @Environment(\.kyonoBigText) private var bigText
    let rawUserText: String
    let onOpenSearch: () -> Void
    @State private var copied = false

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            KyonoGhostButton("📮 この悩み、オガトレに届ける") {
                let subject = "【相談室リクエスト】きょうのオガトレ"
                let body = "相談した内容:\n\(rawUserText)\n---\n送信元: きょうのオガトレ「オガトレ相談室」"
                openMailTo(to: sdMail, subject: subject, body: body)
            }
            Text(copied ? "コピーしました✅" : "📋 メールがひらかない方はアドレスをコピー")
                .kyonoFont(.black900, size: 14).foregroundColor(colors.tealInk)
                .onTapGesture {
                    UIPasteboard.general.string = sdMail
                    copied = true
                    Task {
                        try? await Task.sleep(nanoseconds: bigText ? 4_000_000_000 : 2_000_000_000)
                        copied = false
                    }
                }
            Text("🔍 動画を探すタブでさがしてみる")
                .kyonoFont(.black900, size: 14).foregroundColor(colors.tealInk)
                .onTapGesture { onOpenSearch() }
        }
    }
}

// index.html:440 .chip(丸ピル・line枠・card背景)の1:1移植。
private struct KyonoChip: View {
    @Environment(\.kyonoColors) private var colors
    let label: String
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(label).kyonoFont(.black900, size: 14).foregroundColor(colors.sub)
                .padding(.horizontal, 16).padding(.vertical, 10)
                .overlay(Capsule().stroke(colors.line, lineWidth: 2))
                .background(Capsule().fill(colors.card))
        }
        .buttonStyle(.plain)
    }
}

// index.html:436-437 .catbtn/.catbtn.on(カテゴリタブ・選択時=yellow背景)の1:1移植。
private struct KyonoCatButton: View {
    @Environment(\.kyonoColors) private var colors
    let label: String
    let selected: Bool
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(label).kyonoFont(.black900, size: 14).foregroundColor(selected ? Color(hex: 0x3A3A35) : colors.sub)
                .padding(.horizontal, 13).padding(.vertical, 10)
                .background(RoundedRectangle(cornerRadius: 12).fill(selected ? colors.yellow : colors.line))
        }
        .buttonStyle(.plain)
    }
}

// 吹き出しの「しっぽ」角(左上/右上/片方の下角だけ丸める)用の汎用Shape。
private struct RoundedCorner: Shape {
    var radius: CGFloat = 16
    var corners: UIRectCorner = .allCorners
    func path(in rect: CGRect) -> Path {
        Path(UIBezierPath(roundedRect: rect, byRoundingCorners: corners, cornerRadii: CGSize(width: radius, height: radius)).cgPath)
    }
}

// 2週間プラン完走お祝いカード欠落修正タスク(TASK-C2-2026-07-27-plan-completion-celebration.md):
// app-record.js/index.html:1795 planFinishedCache相当(完走直後の「もう2週間続ける」用に
// intentId/label/videos/daysだけ退避する)。
struct PlanFinishedCache {
    let intentId: String
    let label: String
    let videos: [String]
    let days: Int
}

// index.html:1781 renderPlanCard相当(進捗バー・「やめる」は下線付きテキストリンクでボタンではない)。
// ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md):
// index.html:667-676 #planCard の1:1移植。KyonoCard化(ホーム画面スクショで唯一浮いて見えていた箇所)。
//
// 完走(finished)時はこのカード自体を描画しない(呼び出し側がonFinishedを受けてPlanDoneCardViewを
// 表示する設計に変更 — TASK-C2-2026-07-27-plan-completion-celebration.mdで発見: 従来はfinished時に
// onClearedを即座に呼んでいたため、呼び出し側でplan=nilになり「finishedの表示」自体が
// 次の再描画で消えてしまうバグがあった)。
struct PlanProgressCardView: View {
    @Environment(\.kyonoColors) private var colors
    let store: RecordStore
    let plan: SdPlanData
    let onCleared: () -> Void
    let onFinished: (PlanFinishedCache) -> Void

    var body: some View {
        let today = RecordLogic.todayStr(now: Date())
        let dayNum = max(1, RecordLogic.daysBetween(plan.start, today) + 1)
        let finished = dayNum > plan.days

        Group {
            if finished {
                Color.clear.frame(width: 0, height: 0)
                    .onAppear {
                        // index.html:1798 planFinished時のstore.set("plan",null)相当。
                        onFinished(PlanFinishedCache(intentId: plan.intentId, label: plan.label, videos: plan.videos, days: plan.days))
                        store.set("plan", nil as SdPlanData?)
                        onCleared()
                    }
            } else {
                KyonoCard {
                    HStack(alignment: .firstTextBaseline) {
                        Text("📅 \(plan.label)プラン \(dayNum)/\(plan.days)日")
                            .kyonoFont(.black900, size: 15).foregroundColor(colors.ink)
                        Spacer()
                        Text("やめる")
                            .kyonoFont(.black900, size: 13).foregroundColor(colors.sub)
                            .underline()
                            .onTapGesture {
                                store.set("plan", nil as SdPlanData?)
                                onCleared()
                            }
                    }
                    // index.html:414-415 .bar/.bar>div(teal系グラデーションの進捗バー・transition:width
                    // .4s)の1:1移植。挙動パリティ監査タスク §A: 幅の変化が無アニメーションだったため
                    // .animationを追加。
                    let progress = min(1, max(0, Double(dayNum) / Double(plan.days)))
                    GeometryReader { geo in
                        ZStack(alignment: .leading) {
                            RoundedRectangle(cornerRadius: 99).fill(colors.line)
                            RoundedRectangle(cornerRadius: 99).fill(colors.teal)
                                .frame(width: geo.size.width * progress)
                                .animation(.easeOut(duration: 0.4), value: progress)
                        }
                    }
                    .frame(height: 14)
                    .padding(.top, 8)
                }
            }
        }
    }
}

// index.html:678-684 #planDoneCard(プラン完走のお祝い。chara-congrats+紙吹雪+3ボタン)の1:1移植
// (TASK-C2-2026-07-27-plan-completion-celebration.md)。
struct PlanDoneCardView: View {
    @Environment(\.kyonoColors) private var colors
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    let cache: PlanFinishedCache
    let alreadyCelebrated: Bool
    let onCelebrate: () -> Void
    let onPlanAgain: () -> Void
    let onStartQuiz: () -> Void
    let onClose: () -> Void

    // index.html:1759 planCelebrated(セッション内で紙吹雪を重ね撃ちしない)の1:1移植。@Stateの初期値は
    // 初回描画時にのみ評価されるため、onCelebrate()でalreadyCelebratedがtrueに変わっても
    // 紙吹雪アニメーションの途中で消えない。
    @State private var showConfettiOnce: Bool

    init(
        cache: PlanFinishedCache, alreadyCelebrated: Bool, onCelebrate: @escaping () -> Void,
        onPlanAgain: @escaping () -> Void, onStartQuiz: @escaping () -> Void, onClose: @escaping () -> Void
    ) {
        self.cache = cache
        self.alreadyCelebrated = alreadyCelebrated
        self.onCelebrate = onCelebrate
        self.onPlanAgain = onPlanAgain
        self.onStartQuiz = onStartQuiz
        self.onClose = onClose
        _showConfettiOnce = State(initialValue: !alreadyCelebrated)
    }

    var body: some View {
        ZStack {
            KyonoGradientCard(gradient: .mint) {
                // フォント適用漏れ・キャラ/タイプ画像の欠落修正タスク: index.html:679
                // #planDoneCard(chara-congrats.png 84x84・中央寄せ)の1:1移植。
                KyonoCharaImage(name: "chara-congrats").frame(width: 84, height: 84)
                    .frame(maxWidth: .infinity, alignment: .center)
                Spacer().frame(height: 8)
                // Text連結(+)はText型のみ許容するため、ここだけ.font(.kyono(...))を直接使う
                // (bigtextの1.18倍はこの1箇所のみ非適用・影響は軽微)。
                (Text("🎉 \(cache.label)プラン完走！すごい！").foregroundColor(colors.pink).font(.kyono(.black900, size: 15))
                    + Text("\n\(cache.days)日間続けたの、ほんとにえらい👏\n体はちゃんと応えてくれてるよ").font(.kyono(.bold700, size: 15)))
                    .foregroundColor(colors.ink)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity, alignment: .center)
                Spacer().frame(height: 12)
                KyonoPrimaryButton("💪 もう2週間続ける", action: onPlanAgain)
                Spacer().frame(height: 8)
                KyonoGhostButton("📏 かたさチェックで変化をみる", action: onStartQuiz)
                Spacer().frame(height: 8)
                KyonoLineButton("とじる", action: onClose)
            }
            // §D: index.html:1921 launchConfetti()冒頭のif(matchMedia(prefers-reduced-motion:reduce).matches) return;の1:1移植。
            if showConfettiOnce && !reduceMotion {
                KyonoConfetti(count: 105)
                    .allowsHitTesting(false)
            }
        }
        .onAppear { if !alreadyCelebrated { onCelebrate() } }
    }
}

// index.html:1919 launchConfetti()の見た目の1:1移植(紙吹雪演出。DUR=1500ms・4色パレット・
// 落下+左右のsway+回転)。§2-4許容箇所(markDoneのcheer選択と同じくUI装飾のみの乱数使用。
// 判定/安全ロジックではない)。
private struct ConfettiParticle {
    let x0: Double, y0: Double, vx: Double, vy: Double
    let w: Double, h: Double, rot0: Double, vr: Double
    let sway: Double, phase: Double, color: Color
}

struct KyonoConfetti: View {
    let count: Int

    private let particles: [ConfettiParticle]
    private let palette = [Color(hex: 0xFFD93B), Color(hex: 0x2BB3A3), Color(hex: 0xE56A9A), Color(hex: 0xFF8A70)]
    private let startDate = Date()

    init(count: Int) {
        self.count = count
        var particles: [ConfettiParticle] = []
        let palette = self.palette
        for i in 0..<count {
            particles.append(ConfettiParticle(
                x0: Double.random(in: 0...1),
                y0: -0.05 - Double.random(in: 0...0.35),
                vx: (Double.random(in: 0...1) - 0.5) * 80,
                vy: 200 + Double.random(in: 0...260),
                w: 6 + Double.random(in: 0...5),
                h: 9 + Double.random(in: 0...6),
                rot0: Double.random(in: 0...360),
                vr: (Double.random(in: 0...1) - 0.5) * 720,
                sway: 20 + Double.random(in: 0...26),
                phase: Double.random(in: 0...(2 * .pi)),
                color: palette[i % palette.count]
            ))
        }
        self.particles = particles
    }

    var body: some View {
        TimelineView(.animation) { timeline in
            Canvas { context, size in
                let elapsed = timeline.date.timeIntervalSince(startDate)
                guard elapsed < 1.5 else { return }
                for p in particles {
                    let x = p.x0 * size.width + p.vx * elapsed + p.sway * sin(elapsed * 3 + p.phase)
                    let y = p.y0 * size.height + p.vy * elapsed
                    guard y > -40 && y < size.height + 40 else { continue }
                    var rectContext = context
                    rectContext.translateBy(x: x, y: y)
                    rectContext.rotate(by: .degrees(p.rot0 + p.vr * elapsed))
                    let rect = CGRect(x: -p.w / 2, y: -p.h / 2, width: p.w, height: p.h)
                    rectContext.fill(Path(rect), with: .color(p.color))
                }
            }
        }
    }
}
