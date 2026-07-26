//
//  GuideView.swift
//  KyouNoOgatore
//
//  ネイティブ移植 Step 7b(マスタープラン§6 Step 7b): 使い方タブ=よくあるしつもんUI(Android版
//  GuideScreen.ktと同一ロジック。index.html filterFaq()/toggleFaqGroup()の1:1移植)。検索の正規化は
//  Web版と同じくSafetyGate.norm(Step2で移植済み)を再利用する(判定ロジックではなく正規化ユーティリティ
//  としての再利用。マスタープラン§3-2の隔離対象=crisisHit/redFlagHit/redFlagKindの3関数のみ)。
//
//  ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
//  Phase 3: index.html:180-197,426-429 .faq-g/.faq details/.searchboxの1:1移植。

import SwiftUI
import SafetyCore
import RecordCore

struct GuideView: View {
    let store: RecordStore
    let onBack: () -> Void

    @State private var query = ""
    @State private var openGroups: Set<String> = [faqGroups[0].title]
    @State private var openItems: Set<String> = []

    private var nq: String { SafetyGate.norm(query) }
    private var themeSetting: String { store.get("theme", default: "auto") }

    var body: some View {
        KyonoTheme(themeSetting: themeSetting) {
            content
        }
    }

    private var content: some View {
        GuideContentView(
            query: $query, openGroups: $openGroups, openItems: $openItems, nq: nq, onBack: onBack
        )
    }
}

private struct GuideContentView: View {
    @Environment(\.kyonoColors) private var colors
    @Binding var query: String
    @Binding var openGroups: Set<String>
    @Binding var openItems: Set<String>
    let nq: String
    let onBack: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // 見た目パリティ移植の仕上げ(TASK-C2-2026-07-26-native-visual-design-parity-cleanup.md):
            // タブバー導入後は「戻る」概念が無いWeb版に合わせ、タブ画面から「◀ もどる」ボタンを削除。
            KyonoSectionHeader(icon: .question, title: "よくあるしつもん", fill: colors.coralSoft)
            Text("しつもんをタップすると こたえがひらきます").font(.kyono(.bold700, size: 13)).foregroundColor(colors.sub)
            // index.html:426-429 .searchbox
            TextField("🔍 キーワードでさがす（例: 記録 / 機種変更 / 痛い）", text: $query)
                .padding(.horizontal, 14).padding(.vertical, 10)
                .background(RoundedRectangle(cornerRadius: 16).fill(colors.card))
                .overlay(RoundedRectangle(cornerRadius: 16).stroke(colors.line, lineWidth: 2))

            ScrollView {
                LazyVStack(alignment: .leading, spacing: 6) {
                    ForEach(faqGroups, id: \.title) { group in
                        let visible = group.items.filter { item in
                            if item.hidden { return false }
                            if nq.isEmpty { return true }
                            return SafetyGate.norm(item.q).contains(nq) || SafetyGate.norm(item.a).contains(nq)
                        }
                        if !visible.isEmpty {
                            let isOpen = openGroups.contains(group.title) || !nq.isEmpty
                            // index.html:180-183 .faq-g(グループ見出し・開閉矢印)
                            HStack {
                                Text(group.title).font(.kyono(.black900, size: 14)).foregroundColor(colors.sub)
                                Spacer()
                                Text(isOpen ? "▴" : "▾").font(.kyono(.bold700, size: 14)).foregroundColor(colors.sub)
                            }
                            .padding(.top, 12)
                            .contentShape(Rectangle())
                            .onTapGesture { toggleGroup(group.title) }
                            if isOpen {
                                ForEach(visible, id: \.q) { item in
                                    let key = group.title + "|" + item.q
                                    let open = openItems.contains(key)
                                    // index.html:190-196 .faq details/summary(枠線ボックス・"Q"プレフィックス)
                                    VStack(alignment: .leading, spacing: 8) {
                                        HStack(alignment: .top) {
                                            Text("Q").font(.kyono(.black900, size: 15)).foregroundColor(colors.pink)
                                            Text(item.q).font(.kyono(.extraBold800, size: 14)).foregroundColor(colors.ink)
                                            Spacer()
                                            Text(open ? "▴" : "▾").font(.kyono(.bold700, size: 14)).foregroundColor(colors.sub)
                                        }
                                        if open {
                                            Text(item.a).font(.kyono(.bold700, size: 14)).foregroundColor(colors.sub).padding(.leading, 18)
                                        }
                                    }
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .padding(13)
                                    .background(RoundedRectangle(cornerRadius: 14).fill(colors.bg))
                                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(colors.line, lineWidth: 1.5))
                                    .contentShape(Rectangle())
                                    .onTapGesture { toggleItem(key) }
                                }
                            }
                        }
                    }
                }
            }
        }
        .padding(16)
        .background(KyonoBackgroundColor().ignoresSafeArea())
    }

    private func toggleGroup(_ title: String) {
        if openGroups.contains(title) { openGroups.remove(title) } else { openGroups.insert(title) }
    }

    private func toggleItem(_ key: String) {
        if openItems.contains(key) { openItems.remove(key) } else { openItems.insert(key) }
    }
}
