//
//  VoicesView.swift
//  KyouNoOgatore
//
//  ネイティブ移植 Step 7b(マスタープラン§6 Step 7b・§2-1「じまん/声/...」行): せんぱいの声UI
//  (Android版VoicesScreen.ktと同一ロジック。index.html renderVoices()の1:1移植)。日替わり選定は
//  VoicesLogic.pickDaily(CardLottery.cardRandを呼ぶだけ)に委ね、このファイルはタップでめくる
//  カードUIだけを持つ。
//
//  ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
//  Phase 3: index.html:350-369 .vcard/.vface/.vfront(yellow-soft→pink-softグラデ)/.vback/.vtag/.vgoの1:1移植。

import SwiftUI
import RecordCore

struct VoicesView: View {
    let store: RecordStore
    let openUrl: (String) -> Void
    let onBack: () -> Void

    private let todays: [Voice]
    @State private var openIndices: Set<Int> = []

    init(store: RecordStore, openUrl: @escaping (String) -> Void, onBack: @escaping () -> Void) {
        self.store = store
        self.openUrl = openUrl
        self.onBack = onBack
        let today = RecordLogic.todayStr(now: Date())
        self.todays = VoicesLogic.pickDaily(today: today)
    }

    private var themeSetting: String { store.get("theme", default: "light") }

    var body: some View {
        KyonoTheme(themeSetting: themeSetting, bigText: store.get("bigtext", default: true)) {
            VoicesContentView(todays: todays, openIndices: $openIndices, onBack: onBack, openUrl: openUrl)
        }
        // iOSスワイプもどり導線追加タスク(EdgeSwipeBack.swift参照): アコーディオン状態を持たない画面。
        .edgeSwipeBack(onBack: onBack)
    }
}

private struct VoicesContentView: View {
    @Environment(\.kyonoColors) private var colors
    let todays: [Voice]
    @Binding var openIndices: Set<Int>
    let onBack: () -> Void
    let openUrl: (String) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 8) {
                KyonoLineButton("◀ もどる", action: onBack)
                KyonoCard {
                    KyonoSectionHeader(icon: .envelope, title: "せんぱいの声", fill: colors.pinkSoft)
                    Spacer().frame(height: 8)
                    Text("まえを歩くせんぱいたちの ほんとうの声です\nカードをタップするとめくれます")
                        .kyonoFont(.bold700, size: 14).foregroundColor(colors.ink)
                    Spacer().frame(height: 4)
                    Text("※YouTubeコメントの原文のまま（お名前は出ません）\n※個人の感想です 症状があるときは医療機関へ")
                        .kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)
                }
                VStack(spacing: 10) {
                    ForEach(Array(todays.enumerated()), id: \.offset) { i, v in
                        VoiceCardView(
                            voice: v,
                            open: openIndices.contains(i),
                            onToggle: {
                                if openIndices.contains(i) { openIndices.remove(i) } else { openIndices.insert(i) }
                            },
                            openUrl: openUrl,
                            index: i
                        )
                    }
                }
            }
            .padding(16)
        }
        .background(KyonoBackgroundColor().ignoresSafeArea())
    }
}

private struct VoiceTag: View {
    @Environment(\.kyonoColors) private var colors
    let text: String
    var body: some View {
        Text(text).kyonoFont(.black900, size: 12).foregroundColor(colors.tealInk)
            .padding(.horizontal, 8).padding(.vertical, 2)
            // 監査C-2(2026-08-10・R2で現存確認): iOSだけCapsule(完全ピル)だった。正本は8px角丸
            // (index.html:364 border-radius:8px・Android VoicesScreen.ktと同じ)。
            .background(RoundedRectangle(cornerRadius: 8).fill(colors.tealSoft))
    }
}

private struct VoiceCardView: View {
    @Environment(\.kyonoColors) private var colors
    let voice: Voice
    let open: Bool
    let onToggle: () -> Void
    let openUrl: (String) -> Void
    let index: Int

    // 挙動パリティ監査タスク(TASK-C2-2026-07-27-behavior-parity-audit.md §A): index.html:351
    // .vin(transition:transform .55s・rotateY(180deg))の1:1移植。タップでめくる瞬間が無演出で
    // 一気に切り替わっていたため3Dフリップを追加。90度地点(半分の時間)でfront/backの表示内容を
    // 切り替える(裏面が鏡像文字になるのを避けるため、切替後はさらに180度分を逆回転で打ち消す)。
    @State private var rotation: Double = 0
    @State private var showBack = false
    // TASK-C2-2026-08-06-build30-round8.md R-29(本人指示「開く前サイズが違うのきになる おなじにして」):
    // 旧実装は表裏を常時composeし「コンテナ高さ=大きい方(=裏の長文)」に固定していたため、閉じた
    // カードの枠が裏文章の長さでバラバラだった。表裏それぞれの自然高さを測り、コンテナ高さは
    // 「表示中の面」の高さに合わせる。初期値150は.vin{min-height:150px}(§8)相当のフォールバック。
    @State private var frontHeight: CGFloat = 150
    @State private var backHeight: CGFloat = 150

    var body: some View {
        // TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §8→R-29改訂: 表裏の常時composeは
        // 維持(めくり途中で面が消えない)しつつ、コンテナ高さは大きい方固定→「表示中の面の高さ+
        // 明示アニメーション」に変更。閉じた状態は全カード表面基準(min 150pt)の同一コンパクト高さに
        // なり、めくると0.275秒でなめらかに伸びる(高さ変化を明示アニメーションにすることで、以前の
        // 「めくった瞬間に一覧全体がガタつく」不具合は再発しない)。
        // Fable監査GO-3(視点B・Android版VoicesScreen.ktと同じ指摘): opacity(0)はSwiftUIでも
        // ヒットテストを止めない。backViewの中のKyonoGhostButtonが、表向き(front)の間も
        // ZStack内に重なったまま存在し続けるため、表面のタップが裏側のボタンに奪われうる。
        // allowsHitTestingで、見えていない面を構造的にタップ不可能にする(外側の.onTapGesture
        // だけがその領域で反応するようになる)。
        ZStack(alignment: .top) {
            frontView
                .background(heightReader($frontHeight))
                .opacity(showBack ? 0 : 1).allowsHitTesting(!showBack)
            backView
                // TASK build34 R-56(本人指示「せんぱいの声、コメントが全部見れるように戻して」・
                // 2026-08-07): 外側.frame(height: backHeight)がZStack経由でbackViewへも高さの
                // 「提案」として伝わり、直後のheightReader(GeometryReader)がその提案値をそのまま
                // 計測結果として送り返す自己参照ループに陥っていた(=長文コメントほど1行+「…」に
                // 切り詰められたまま高さが更新されない実バグ)。fixedSizeでbackViewに常に自分の
                // 理想の高さで描画・計測させ、外側の提案とは無関係に正しい高さへ収束させる。
                .fixedSize(horizontal: false, vertical: true)
                .rotation3DEffect(.degrees(180), axis: (x: 0, y: 1, z: 0))
                .background(heightReader($backHeight))
                .opacity(showBack ? 1 : 0).allowsHitTesting(showBack)
        }
        .frame(height: showBack ? backHeight : frontHeight, alignment: .top)
        .rotation3DEffect(.degrees(rotation), axis: (x: 0, y: 1, z: 0))
        .contentShape(Rectangle())
        .onTapGesture { onToggle() }
        .onChange(of: open) { _, newValue in
            withAnimation(.easeInOut(duration: 0.275)) { rotation += 90 }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.275) {
                // R-29: showBack切替(=コンテナ高さの切替)もwithAnimationに含め、90度地点からの
                // 後半回転と同時に高さがなめらかに伸縮するようにする。
                withAnimation(.easeInOut(duration: 0.275)) {
                    showBack = newValue
                    rotation += 90
                }
            }
        }
    }

    private func heightReader(_ binding: Binding<CGFloat>) -> some View {
        GeometryReader { geo in
            Color.clear
                .onAppear { binding.wrappedValue = max(150, geo.size.height) }
                .onChange(of: geo.size.height) { _, h in binding.wrappedValue = max(150, h) }
        }
    }

    // index.html:355-357 .vfront(yellow-soft→pink-soft斜めグラデ)
    private var frontView: some View {
        KyonoGradientCard(gradient: .warm) {
            VStack(spacing: 6) {
                VoiceTag(text: voice.tag)
                Text(voice.front).kyonoFont(.black900, size: 18).foregroundColor(colors.ink).multilineTextAlignment(.center)
                Text("タップでめくる").kyonoFont(.black900, size: 12).foregroundColor(colors.sub)
            }
            .frame(maxWidth: .infinity)
        }
        // R-29: 閉じた状態の全カード同一コンパクト高さの基準(.vin{min-height:150px}相当)。
        .frame(minHeight: 150)
    }

    // index.html:358-359,362-363 .vback(card地・枠線)
    private var backView: some View {
        VStack(alignment: .leading, spacing: 8) {
            VoiceTag(text: voice.tag)
            Text(voice.q).kyonoFont(.bold700, size: 14).foregroundColor(colors.ink).lineSpacing(6)
            Text("— せんぱいの声（\(voice.src)）")
                .kyonoFont(.black900, size: 12).foregroundColor(colors.sub)
                .frame(maxWidth: .infinity, alignment: .trailing)
            KyonoGhostButton("せんぱいとおなじ1本をみる ▶") { openUrl("https://www.youtube.com/watch?v=\(voice.vid)") }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(18)
        .background(RoundedRectangle(cornerRadius: 22).fill(colors.card))
        .overlay(RoundedRectangle(cornerRadius: 22).stroke(colors.borderStrong, lineWidth: 1.5))
    }
}
