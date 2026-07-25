//
//  VoicesView.swift
//  KyouNoOgatore
//
//  ネイティブ移植 Step 7b(マスタープラン§6 Step 7b・§2-1「じまん/声/...」行): せんぱいの声UI
//  (Android版VoicesScreen.ktと同一ロジック。index.html renderVoices()の1:1移植)。日替わり選定は
//  VoicesLogic.pickDaily(CardLottery.cardRandを呼ぶだけ)に委ね、このファイルはタップでめくる
//  カードUIだけを持つ。

import SwiftUI
import RecordCore

struct VoicesView: View {
    let openUrl: (String) -> Void
    let onBack: () -> Void

    private let todays: [Voice]
    @State private var openIndices: Set<Int> = []

    init(openUrl: @escaping (String) -> Void, onBack: @escaping () -> Void) {
        self.openUrl = openUrl
        self.onBack = onBack
        let today = RecordLogic.todayStr(now: Date())
        self.todays = VoicesLogic.pickDaily(today: today)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button("◀ もどる", action: onBack)
            Text("せんぱいの声").font(.title2.bold())
            Text("タップでめくってね").font(.caption)
            ScrollView {
                LazyVStack(spacing: 8) {
                    ForEach(Array(todays.enumerated()), id: \.offset) { i, v in
                        VoiceCardView(
                            voice: v,
                            open: openIndices.contains(i),
                            onToggle: {
                                if openIndices.contains(i) { openIndices.remove(i) } else { openIndices.insert(i) }
                            },
                            openUrl: openUrl
                        )
                    }
                }
            }
        }
        .padding(16)
    }
}

private struct VoiceCardView: View {
    let voice: Voice
    let open: Bool
    let onToggle: () -> Void
    let openUrl: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(voice.tag).font(.caption).foregroundColor(Color(red: 0.42, green: 0.31, blue: 0.65))
            if open {
                Text(voice.q)
                Text("— せんぱいの声（\(voice.src)）").font(.caption).foregroundColor(.gray)
                Button("せんぱいとおなじ1本をみる ▶") { openUrl("https://www.youtube.com/watch?v=\(voice.vid)") }
            } else {
                Text(voice.front).font(.title3)
                Text("タップでめくる").font(.caption).foregroundColor(.gray)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(Color(red: 0.95, green: 0.95, blue: 0.93))
        .cornerRadius(12)
        .onTapGesture { onToggle() }
    }
}
