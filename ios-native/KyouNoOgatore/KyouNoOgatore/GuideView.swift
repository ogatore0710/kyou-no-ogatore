//
//  GuideView.swift
//  KyouNoOgatore
//
//  ネイティブ移植 Step 7b(マスタープラン§6 Step 7b): 使い方タブ=よくあるしつもんUI(Android版
//  GuideScreen.ktと同一ロジック。index.html filterFaq()/toggleFaqGroup()の1:1移植)。検索の正規化は
//  Web版と同じくSafetyGate.norm(Step2で移植済み)を再利用する(判定ロジックではなく正規化ユーティリティ
//  としての再利用。マスタープラン§3-2の隔離対象=crisisHit/redFlagHit/redFlagKindの3関数のみ)。

import SwiftUI
import SafetyCore

struct GuideView: View {
    let onBack: () -> Void

    @State private var query = ""
    @State private var openGroups: Set<String> = [faqGroups[0].title]
    @State private var openItems: Set<String> = []

    private var nq: String { SafetyGate.norm(query) }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button("◀ もどる", action: onBack)
            Text("よくあるしつもん").font(.title2.bold())
            Text("しつもんをタップすると こたえがひらきます").font(.caption)
            TextField("🔍 キーワードでさがす（例: 記録 / 機種変更 / 痛い）", text: $query).textFieldStyle(.roundedBorder)

            ScrollView {
                LazyVStack(alignment: .leading, spacing: 4) {
                    ForEach(faqGroups, id: \.title) { group in
                        let visible = group.items.filter { item in
                            if item.hidden { return false }
                            if nq.isEmpty { return true }
                            return SafetyGate.norm(item.q).contains(nq) || SafetyGate.norm(item.a).contains(nq)
                        }
                        if !visible.isEmpty {
                            Text(group.title)
                                .font(.headline)
                                .padding(.top, 8)
                                .onTapGesture { toggleGroup(group.title) }
                            if openGroups.contains(group.title) || !nq.isEmpty {
                                ForEach(visible, id: \.q) { item in
                                    let key = group.title + "|" + item.q
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(item.q)
                                        if openItems.contains(key) {
                                            Text(item.a).font(.caption)
                                        }
                                    }
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .padding(10)
                                    .background(Color(red: 0.95, green: 0.95, blue: 0.93))
                                    .cornerRadius(10)
                                    .onTapGesture { toggleItem(key) }
                                }
                            }
                        }
                    }
                }
            }
        }
        .padding(16)
    }

    private func toggleGroup(_ title: String) {
        if openGroups.contains(title) { openGroups.remove(title) } else { openGroups.insert(title) }
    }

    private func toggleItem(_ key: String) {
        if openItems.contains(key) { openItems.remove(key) } else { openItems.insert(key) }
    }
}
