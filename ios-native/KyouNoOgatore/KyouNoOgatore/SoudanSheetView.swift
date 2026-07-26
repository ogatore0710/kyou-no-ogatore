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
//  未移植(Step6のスコープ外として明示的に見送り。Android版と同じ判断): タイピングアニメーション・
//  吹き出し分割タイミング演出・雑談(smalltalk 54件)・自由入力でのfollowup同義語マッチ(SD_FU_KW)・
//  動画サムネイル画像の読み込み・タイプ診断との相性演出(sdTypeFlavor)。
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

enum SdBubble: Identifiable {
    case bot(text: String, red: Bool, videoId: String?)
    case user(text: String)
    case planConfirm(intentId: String, label: String, replacing: Bool)

    var id: String { UUID().uuidString }
}

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

    private let kb = SafetyKBLoader.shared
    @State private var messages: [SdBubble] = []
    @State private var chipsMode: SdChipsMode = .intents(activeCat: "body")
    @State private var lastIntentId: String?
    @State private var shownVideoIds: [String] = [] // index.html:2999 sdCtx.shownVideoIds相当(セッション内のみ)
    @State private var input = ""
    @State private var plan: SdPlanData?

    init(store: RecordStore, openUrl: @escaping (String) -> Void, onClose: @escaping () -> Void, presetIntentId: String? = nil) {
        self.store = store
        self.openUrl = openUrl
        self.onClose = onClose
        self.presetIntentId = presetIntentId
        _plan = State(initialValue: store.get("plan", default: nil))
    }

    // index.html:3090 sdPush相当。応答1件をbot/userの吹き出し列へ展開しchipsModeを更新する。
    private func applyResponse(_ userText: String?, _ r: SoudanResponse) {
        var newMsgs: [SdBubble] = []
        if let userText { newMsgs.append(.user(text: userText)) }
        let red: Bool
        switch r.verdict {
        case .crisis, .redFlag: red = true
        case .normal: red = false
        }
        if !r.empathy.isEmpty { newMsgs.append(.bot(text: r.empathy, red: red, videoId: nil)) }
        if !r.message.isEmpty { newMsgs.append(.bot(text: r.message, red: red, videoId: nil)) }
        if let v = r.video {
            newMsgs.append(.bot(text: v.note.isEmpty ? "おすすめの1本" : v.note, red: false, videoId: v.videoId))
            if !shownVideoIds.contains(v.videoId) { shownVideoIds.append(v.videoId) }
        }
        if !r.keizoku.isEmpty { newMsgs.append(.bot(text: r.keizoku, red: false, videoId: nil)) }
        messages += newMsgs
        if let intentId = r.intentId { lastIntentId = intentId }
        switch r.verdict {
        case .crisis:
            chipsMode = .none // index.html:3310 チップ・カテゴリタブなし
        case .redFlag:
            chipsMode = .intents(activeCat: "body") // index.html:3304
        case .normal:
            if r.hasFollowup, let intentId = r.intentId {
                chipsMode = .followups(intentId: intentId, nextBestId: r.nextBestChip?.id)
            } else if !r.nearmissChips.isEmpty {
                chipsMode = .nearmiss(ids: r.nearmissChips.map { $0.id })
            } else {
                chipsMode = .intents(activeCat: "body")
            }
        }
    }

    private func sendText() {
        let raw = input.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !raw.isEmpty else { return }
        input = ""
        applyResponse(raw, SoudanEngine.respond(to: raw))
    }

    private func chipTap(_ id: String) {
        guard let intent = kb.intents.first(where: { $0.id == id }), let r = SoudanEngine.respondToIntent(id: id) else { return }
        applyResponse(intent.chip, r)
    }

    private func followupTap(_ id: String) {
        guard let f = kb.commonFollowups.first(where: { $0.id == id }),
              let r = SoudanEngine.respondToFollowup(id: id, lastIntentId: lastIntentId, shownVideoIds: shownVideoIds) else { return }
        applyResponse(f.chip, r)
    }

    // index.html:1844 planChipTap相当。即開始はせず確認の吹き出しを積む。
    private func planChipTap(_ id: String) {
        guard let intent = kb.intents.first(where: { $0.id == id }) else { return }
        let replacing = plan != nil && plan?.intentId != id
        messages.append(.user(text: "📅 この悩みを2週間プランにする"))
        messages.append(.planConfirm(intentId: id, label: intent.chip, replacing: replacing))
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
        messages.append(.bot(text: "よし、きょうから14日間いっしょにやろう！ホームの「きょうの1本」が\(intent.chip)用になったよ😊", red: false, videoId: nil))
    }

    private func planDecline() {
        messages.append(.bot(text: "OK！1本ずつでも十分えらいよ😊 プランにしたくなったら、いつでもここから組めるからね", red: false, videoId: nil))
    }

    private var themeSetting: String { store.get("theme", default: "auto") }

    // ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
    // Phase 3: index.html:459-489 .sd-sheet/.sd-head/.sd-b/.chip/.catbtnの1:1移植。見た目の変更のみで、
    // 上の判定・状態管理ロジック(applyResponse/chipTap/sendText等)には一切手を入れていない。
    var body: some View {
        KyonoTheme(themeSetting: themeSetting) {
            content
        }
    }

    private var content: some View {
        SoudanContentView(
            messages: messages, chipsMode: chipsMode, input: $input, plan: plan,
            kb: kb, onClose: onClose, openUrl: openUrl,
            onSend: sendText, onChip: chipTap, onFollowup: followupTap,
            onPlanChip: planChipTap, onPlanStart: planStart, onPlanDecline: planDecline,
            onCatSelect: { key in chipsMode = .intents(activeCat: key) }
        )
        // ホーム構造修正タスク(TASK-C2-2026-07-26-home-structure-fix.md): index.html:3409
        // soudanCardChips「タップでそのまま聞けるよ」チップ→openSoudan(intentId)相当。ホームの
        // オガトレ相談室カードのおすすめチップから開いたときだけ、開いた瞬間にそのintentへ自動応答する。
        .onAppear {
            if let presetIntentId { chipTap(presetIntentId) }
        }
    }
}

// KyonoColors解決の都合上(HomeView.swift冒頭コメント参照: @Environmentは自分を包むKyonoThemeの
// 子孫でなければ既定値のまま)、実際の描画は別のView構造体に切り出す。ロジックは一切持たず、
// 親から渡された状態・コールバックをそのまま描画するだけ。
private struct SoudanContentView: View {
    @Environment(\.kyonoColors) private var colors
    let messages: [SdBubble]
    let chipsMode: SdChipsMode
    @Binding var input: String
    let plan: SdPlanData?
    let kb: SafetyKB
    let onClose: () -> Void
    let openUrl: (String) -> Void
    let onSend: () -> Void
    let onChip: (String) -> Void
    let onFollowup: (String) -> Void
    let onPlanChip: (String) -> Void
    let onPlanStart: (String) -> Void
    let onPlanDecline: () -> Void
    let onCatSelect: (String) -> Void

    var body: some View {
        VStack(spacing: 0) {
            // index.html:461-465 .sd-head(ヘッダー・円形×クローズボタン)
            HStack {
                KyonoSectionHeader(icon: .soudanBubble, title: "オガトレ相談室", fill: colors.tealSoft, accent: colors.teal)
                Spacer()
                Button(action: onClose) {
                    Text("✕").font(.kyono(.black900, size: 18)).foregroundColor(colors.ink)
                        .frame(width: 40, height: 40)
                        .background(Circle().fill(colors.line))
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 16).padding(.vertical, 12)
            .background(colors.card)
            // index.html:466-467 .sd-disc
            Text("※目安をつかむ相談室です 強い痛み・しびれがあるときは医療機関へ")
                .font(.kyono(.bold700, size: 13)).foregroundColor(colors.sub).multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 16).padding(.vertical, 6)
                .background(colors.card)
            Divider()

            ScrollView {
                VStack(alignment: .leading, spacing: 10) {
                    Text("肩こりや腰痛など、気になることを教えてね。下のチップから選んでもいいよ😊")
                        .font(.kyono(.bold700, size: 15)).foregroundColor(colors.sub)
                    ForEach(messages) { m in bubbleView(m) }
                }
                .padding(16)
            }

            chipsView
        }
        .background(colors.bg)
    }

    @ViewBuilder
    private func bubbleView(_ m: SdBubble) -> some View {
        switch m {
        // index.html:482-483 .sd-row.user .sd-b(黄色系吹き出し・右寄せ)
        case let .user(text):
            HStack {
                Spacer()
                Text(text).font(.kyono(.bold700, size: 15)).foregroundColor(colors.ink)
                    .padding(.horizontal, 14).padding(.vertical, 10)
                    .background(RoundedCorner(radius: 16, corners: [.topLeft, .topRight, .bottomLeft]).fill(colors.yellowSoft))
            }
        // index.html:481,488,3080 .sd-b/.sd-row.sd-red .sd-b(通常=card+line枠・赤旗=coral-soft+coral枠)/
        // .sd-ava(chara-hitokoto.pngアバター・botメッセージのみ)の1:1移植。
        case let .bot(text, red, videoId):
            let bg = red ? colors.coralSoft : colors.card
            let border = red ? colors.coral : colors.line
            HStack(alignment: .bottom) {
                KyonoCharaImage(name: "chara-hitokoto").frame(width: 38, height: 38)
                VStack(alignment: .leading, spacing: 6) {
                    if !text.isEmpty { Text(text).font(.kyono(.bold700, size: 15)).foregroundColor(colors.ink) }
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
        case let .planConfirm(intentId, label, replacing):
            HStack(alignment: .bottom) {
                KyonoCharaImage(name: "chara-hitokoto").frame(width: 38, height: 38)
                VStack(alignment: .leading, spacing: 8) {
                    Text(replacing
                        ? "いまのプランと入れ替える？きょうの1本が、あなたの\(label)プランになるよ"
                        : "きょうの1本が、あなたの\(label)プランになるよ！2週間いっしょにやってみる？")
                        .font(.kyono(.bold700, size: 15)).foregroundColor(colors.ink)
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
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack {
                        ForEach(sdChipCats, id: \.key) { cat in
                            KyonoCatButton(label: cat.label, selected: cat.key == activeCat) { onCatSelect(cat.key) }
                        }
                    }
                }
                let cat = sdChipCats.first { $0.key == activeCat } ?? sdChipCats[0]
                let ids = Set(sdCatIntentIds(cat, kb.intents))
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack {
                        ForEach(kb.intents.filter { ids.contains($0.id) }, id: \.id) { intent in
                            KyonoChip(label: intent.chip) { onChip(intent.id) }
                        }
                    }
                }
            case let .followups(intentId, nextBestId):
                let intent = kb.intents.first { $0.id == intentId }
                // index.html:1828 planInjectChip相当: 動画2本以上・除外intentでない・実行中プランと同一でないときだけ出す
                let showPlanChip = intent != nil && (intent?.videos?.count ?? 0) >= 2 &&
                    intent?.id != planExcludeIntent && plan?.intentId != intent?.id
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack {
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
                        KyonoChip(label: "べつの悩みをそうだん") { onCatSelect("body") }
                    }
                }
            case let .nearmiss(ids):
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack {
                        ForEach(ids, id: \.self) { id in
                            if let intent = kb.intents.first(where: { $0.id == id }) {
                                KyonoChip(label: intent.chip) { onChip(id) }
                            }
                        }
                        KyonoChip(label: "べつの悩みをそうだん") { onCatSelect("body") }
                    }
                }
            }

            HStack {
                TextField("気になることを入力", text: $input).textFieldStyle(.roundedBorder)
                KyonoPrimaryButton("送信", action: onSend).frame(width: 90)
            }
        }
        .padding(.horizontal, 14).padding(.vertical, 8)
        .background(colors.card)
    }
}

// index.html:440 .chip(丸ピル・line枠・card背景)の1:1移植。
private struct KyonoChip: View {
    @Environment(\.kyonoColors) private var colors
    let label: String
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(label).font(.kyono(.black900, size: 14)).foregroundColor(colors.sub)
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
            Text(label).font(.kyono(.black900, size: 14)).foregroundColor(selected ? Color(hex: 0x3A3A35) : colors.sub)
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

// index.html:1781 renderPlanCard相当の簡略版(進捗バー・完走時の卒業表示・解除ボタン)。
// 紙吹雪演出(launchConfetti)・章システムとの連携(mode_manual)等の見た目演出は移植対象外(Step6の
// 検収基準に含まれないため。安全性に無関係)。
// ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md):
// index.html:667-676 #planCard(.bar進捗バー・「やめる」は下線付きテキストリンクでボタンではない)の
// 1:1移植。KyonoCard化(Android版PlanProgressCardと同一ロジック。ホーム画面スクショで唯一浮いて
// 見えていた箇所)。
struct PlanProgressCardView: View {
    @Environment(\.kyonoColors) private var colors
    let store: RecordStore
    let plan: SdPlanData
    let onCleared: () -> Void

    var body: some View {
        let today = RecordLogic.todayStr(now: Date())
        let dayNum = max(1, RecordLogic.daysBetween(plan.start, today) + 1)
        let finished = dayNum > plan.days

        KyonoCard {
            if finished {
                // フォント適用漏れ・キャラ/タイプ画像の欠落修正タスク(TASK-C2-2026-07-26-visual-parity-fonts-characters.md)
                // §2 キャラクター画像: index.html:679 #planDoneCard(chara-congrats.png 84x84・中央寄せ)の1:1移植。
                KyonoCharaImage(name: "chara-congrats").frame(width: 84, height: 84)
                    .frame(maxWidth: .infinity, alignment: .center)
                Text("🎉 \(plan.label)プラン完走！すごい！").font(.kyono(.black900, size: 15)).foregroundColor(colors.ink)
                    .frame(maxWidth: .infinity, alignment: .center)
                Text("\(plan.days)日間続けたの、ほんとにえらい👏").font(.kyono(.bold700, size: 14)).foregroundColor(colors.sub)
                    .frame(maxWidth: .infinity, alignment: .center)
            } else {
                HStack(alignment: .firstTextBaseline) {
                    Text("📅 \(plan.label)プラン \(dayNum)/\(plan.days)日")
                        .font(.kyono(.black900, size: 15)).foregroundColor(colors.ink)
                    Spacer()
                    Text("やめる")
                        .font(.kyono(.black900, size: 13)).foregroundColor(colors.sub)
                        .underline()
                        .onTapGesture {
                            store.set("plan", nil as SdPlanData?)
                            onCleared()
                        }
                }
                // index.html:414-415 .bar/.bar>div(teal系グラデーションの進捗バー)の1:1移植。
                let progress = min(1, max(0, Double(dayNum) / Double(plan.days)))
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 99).fill(colors.line)
                        RoundedRectangle(cornerRadius: 99).fill(colors.teal)
                            .frame(width: geo.size.width * progress)
                    }
                }
                .frame(height: 14)
                .padding(.top, 8)
            }
        }
        .onAppear {
            // index.html:1798 planFinished時のstore.set("plan",null)相当。
            if finished {
                store.set("plan", nil as SdPlanData?)
                onCleared()
            }
        }
    }
}
