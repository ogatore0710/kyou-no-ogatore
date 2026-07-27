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

    private var themeSetting: String { store.get("theme", default: "auto") }

    var body: some View {
        KyonoTheme(themeSetting: themeSetting, bigText: store.get("bigtext", default: true)) {
            VoicesContentView(todays: todays, openIndices: $openIndices, onBack: onBack, openUrl: openUrl)
        }
    }
}

private struct VoicesContentView: View {
    @Environment(\.kyonoColors) private var colors
    let todays: [Voice]
    @Binding var openIndices: Set<Int>
    let onBack: () -> Void
    let openUrl: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            KyonoLineButton("◀ もどる", action: onBack)
            KyonoCard {
                KyonoSectionHeader(icon: .envelope, title: "せんぱいの声", fill: colors.pinkSoft)
                Spacer().frame(height: 8)
                Text("まえを歩くせんぱいたちの ほんとうの声です🌱\nカードをタップするとめくれます")
                    .kyonoFont(.bold700, size: 14).foregroundColor(colors.ink)
                Spacer().frame(height: 4)
                Text("※YouTubeコメントの原文のまま（お名前は出ません）\n※個人の感想です 症状があるときは医療機関へ")
                    .kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)
            }
            ScrollView {
                LazyVStack(spacing: 10) {
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
        }
        .padding(16)
        .background(KyonoBackgroundColor().ignoresSafeArea())
    }
}

private struct VoiceTag: View {
    @Environment(\.kyonoColors) private var colors
    let text: String
    var body: some View {
        Text(text).kyonoFont(.black900, size: 11).foregroundColor(colors.tealInk)
            .padding(.horizontal, 8).padding(.vertical, 2)
            .background(Capsule().fill(colors.tealSoft))
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

    var body: some View {
        Group {
            if !showBack {
                frontView
            } else {
                backView.rotation3DEffect(.degrees(180), axis: (x: 0, y: 1, z: 0))
            }
        }
        .rotation3DEffect(.degrees(rotation), axis: (x: 0, y: 1, z: 0))
        .contentShape(Rectangle())
        .onTapGesture { onToggle() }
        .onChange(of: open) { _, newValue in
            withAnimation(.easeInOut(duration: 0.275)) { rotation += 90 }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.275) {
                showBack = newValue
                withAnimation(.easeInOut(duration: 0.275)) { rotation += 90 }
            }
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
        .overlay(RoundedRectangle(cornerRadius: 22).stroke(colors.line, lineWidth: 1.5))
    }
}
