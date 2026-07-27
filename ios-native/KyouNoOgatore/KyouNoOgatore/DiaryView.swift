//
//  DiaryView.swift
//  KyouNoOgatore
//
//  ひとことにっき機能欠落修正タスク(TASK-C2-2026-07-26-diary-list-missing.md): app-record.js:267-273
//  renderDiary()の1:1移植(保存済みkyono_memosの一覧表示のみ・保存/編集/削除ロジックには触れない)。
//  index.html:884 #fun内「ひとことにっき」カードの1:1移植(見出しアイコンは既存KyonoIcon.notesを流用)。
//  Android版DiaryScreen.ktと同一ロジック。

import SwiftUI
import RecordCore

struct DiaryView: View {
    let store: RecordStore
    let onBack: () -> Void

    private let entries: [(date: String, memo: String)]

    init(store: RecordStore, onBack: @escaping () -> Void) {
        self.store = store
        self.onBack = onBack
        // index.html:269 keys=Object.keys(memos).sort().reverse().slice(0,7)の1:1移植(新しい順に最大7件)。
        let memos = RecordLogic.loadMemos(store)
        self.entries = memos.sorted { $0.key > $1.key }.prefix(7).map { (date: $0.key, memo: $0.value) }
    }

    private var themeSetting: String { store.get("theme", default: "auto") }

    var body: some View {
        KyonoTheme(themeSetting: themeSetting, bigText: store.get("bigtext", default: true)) {
            DiaryContentView(entries: entries, onBack: onBack)
        }
    }
}

private struct DiaryContentView: View {
    @Environment(\.kyonoColors) private var colors
    let entries: [(date: String, memo: String)]
    let onBack: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            KyonoLineButton("◀ もどる", action: onBack)
            KyonoCard {
                KyonoSectionHeader(icon: .notes, title: "ひとことにっき", fill: colors.pinkSoft)
                Spacer().frame(height: 10)
                if entries.isEmpty {
                    Text("「きょうやった！」のあとにメモをのこせます")
                        .kyonoFont(.bold700, size: 14).foregroundColor(colors.sub)
                } else {
                    VStack(spacing: 0) {
                        ForEach(Array(entries.enumerated()), id: \.offset) { i, entry in
                            HStack(alignment: .top, spacing: 10) {
                                Text(diaryDateLabel(entry.date))
                                    .kyonoFont(.black900, size: 15).foregroundColor(colors.sub)
                                Text(entry.memo)
                                    .kyonoFont(.bold700, size: 15).foregroundColor(colors.ink)
                            }
                            .padding(.vertical, 7)
                            // index.html:271 border-bottom:1px dashed var(--line)の簡略化(実線)。
                            // SwiftUI標準に破線divider相当が無いため、区切り線としての機能を優先し実線で近似。
                            if i < entries.count - 1 {
                                Rectangle().fill(colors.line).frame(height: 1)
                            }
                        }
                    }
                }
            }
            Spacer()
        }
        .padding(16)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(colors.bg.ignoresSafeArea())
    }
}

// index.html:271 k.slice(5).replace("-","/")の1:1移植("YYYY-MM-DD" → "MM/DD")。
private func diaryDateLabel(_ ymd: String) -> String {
    let mmdd = ymd.dropFirst(5)
    return mmdd.replacingOccurrences(of: "-", with: "/")
}
