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
        // iOSスワイプもどり導線追加タスク(EdgeSwipeBack.swift参照): アコーディオン状態を持たない画面。
        .edgeSwipeBack(onBack: onBack)
    }
}

private struct DiaryContentView: View {
    @Environment(\.kyonoColors) private var colors
    // iOS画面まるごとズーム第2段階(2026-07-29): 第1段階(共有部品)に続き、この画面自身が持つ
    // リテラルpadding/frame/lineWidthにも1.18倍を適用する(フォントサイズは対象外・理由は
    // HomeView.swiftの同種コメント参照)。
    @Environment(\.kyonoBigText) private var bigText
    private var zoom: CGFloat { bigText ? kyonoBigTextScale : 1 }
    let entries: [(date: String, memo: String)]
    let onBack: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12 * zoom) {
            KyonoLineButton("◀ もどる", action: onBack)
            KyonoCard {
                KyonoSectionHeader(icon: .notes, title: "ひとことにっき", fill: colors.pinkSoft)
                Spacer().frame(height: 10 * zoom)
                if entries.isEmpty {
                    Text("「きょうやった！」のあとにメモをのこせます")
                        .kyonoFont(.bold700, size: 14).foregroundColor(colors.sub)
                } else {
                    VStack(spacing: 0) {
                        ForEach(entries, id: \.date) { entry in
                            HStack(alignment: .top, spacing: 10 * zoom) {
                                // TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §8:
                                // index.html:271 flex-shrink:0の1:1移植。長いメモが隣にあっても
                                // 日付ラベルが潰れて折り返されないようにする(以前は保護なし)。
                                Text(diaryDateLabel(entry.date))
                                    .kyonoFont(.black900, size: 15).foregroundColor(colors.sub)
                                    .fixedSize()
                                Text(entry.memo)
                                    .kyonoFont(.bold700, size: 15).foregroundColor(colors.ink)
                            }
                            .padding(.vertical, 7 * zoom)
                            // TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §8: index.html:271
                            // border-bottom:1px dashed var(--line)の1:1移植(以前は実線で近似としていた)。
                            // Web版は全行(最終行含む)に付くため、除外条件は付けない。
                            DashedDividerShape()
                                .stroke(colors.line, style: StrokeStyle(lineWidth: 1 * zoom, dash: [4, 3]))
                                .frame(height: 1 * zoom)
                        }
                    }
                }
            }
            Spacer()
        }
        .padding(16 * zoom)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(colors.bg.ignoresSafeArea())
    }
}

// index.html:271 k.slice(5).replace("-","/")の1:1移植("YYYY-MM-DD" → "MM/DD")。
private func diaryDateLabel(_ ymd: String) -> String {
    let mmdd = ymd.dropFirst(5)
    return mmdd.replacingOccurrences(of: "-", with: "/")
}

// index.html:271 border-bottom:1px dashed var(--line)の1:1移植。
private struct DashedDividerShape: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: 0, y: rect.midY))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.midY))
        return path
    }
}
