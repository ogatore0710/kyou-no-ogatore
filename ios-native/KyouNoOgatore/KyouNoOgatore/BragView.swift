//
//  BragView.swift
//  KyouNoOgatore
//
//  ネイティブ移植 Step 7b(マスタープラン§6 Step 7b・§2-1「index.html drawBragCard」行): じまんカード
//  作成UI(Android版BragScreen.ktと同一ロジック。index.html openBrag()〜makeBragCard()の1:1移植)。
//  日数入力(1〜9999の整数のみ・自由入力なし)+動画検索(Step7aのsearchCatalogを再利用)+作成ボタン。
//  判定/描画ロジックはBragCardRenderer(Step7b新設)を呼ぶだけ。
//
//  ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
//  Phase 3: index.html:850-866,340 #brag .card/.lbl/.hint/sec-head(Heartアイコン)の1:1移植。

import SwiftUI
import RecordCore
import CardCore

struct BragView: View {
    let store: RecordStore
    let onBack: () -> Void

    private let catalog = CatalogLoader.shared
    @State private var daysText: String
    @State private var query = ""
    @State private var picked: CatalogVideo?
    @State private var cardImage: UIImage?

    init(store: RecordStore, onBack: @escaping () -> Void) {
        self.store = store
        self.onBack = onBack
        let streak = RecordLogic.loadStreak(store)
        _daysText = State(initialValue: String(streak.total > 0 ? streak.total : 1))
    }

    private var hits: [CatalogVideo] {
        query.trimmingCharacters(in: .whitespaces).isEmpty ? [] : Array(searchCatalog(catalog, query: query, activeTag: nil, year: nil).prefix(20))
    }

    private var themeSetting: String { store.get("theme", default: "auto") }

    var body: some View {
        KyonoTheme(themeSetting: themeSetting) {
            BragContentView(
                store: store, daysText: $daysText, query: $query, picked: $picked, cardImage: $cardImage,
                hits: hits, onBack: onBack
            )
        }
    }
}

private struct BragContentView: View {
    @Environment(\.kyonoColors) private var colors
    let store: RecordStore
    @Binding var daysText: String
    @Binding var query: String
    @Binding var picked: CatalogVideo?
    @Binding var cardImage: UIImage?
    let hits: [CatalogVideo]
    let onBack: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            KyonoLineButton("◀ もどる", action: onBack)
            KyonoCard {
                KyonoSectionHeader(icon: .heart, title: "じまんカードをつくる", fill: colors.pinkSoft, accent: colors.pink)
                Spacer().frame(height: 8)
                Text("続けてる日数と すきな1本を 1枚のカードに✨\nできたカードは保存やSNS投稿ができます")
                    .font(.kyono(.bold700, size: 14)).foregroundColor(colors.sub)

                // index.html:340 #brag .lbl
                Spacer().frame(height: 14)
                Text("つづいている日数").font(.kyono(.black900, size: 13)).foregroundColor(colors.sub)
                Spacer().frame(height: 6)
                TextField("", text: $daysText)
                    .keyboardType(.numberPad)
                    .padding(.horizontal, 14).padding(.vertical, 10)
                    .background(RoundedRectangle(cornerRadius: 16).fill(colors.card))
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(colors.line, lineWidth: 2))
                    .onChange(of: daysText) { _, newValue in
                        let filtered = newValue.filter { $0.isNumber }
                        daysText = String(filtered.prefix(4))
                    }

                Spacer().frame(height: 14)
                Text("すきな1本をさがす🎬").font(.kyono(.black900, size: 13)).foregroundColor(colors.sub)
                Spacer().frame(height: 6)
                TextField("例: 肩甲骨／朝／開脚", text: $query)
                    .padding(.horizontal, 14).padding(.vertical, 10)
                    .background(RoundedRectangle(cornerRadius: 16).fill(colors.card))
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(colors.line, lineWidth: 2))

                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 6) {
                        ForEach(hits, id: \.id) { v in
                            Text(v.t)
                                .font(.kyono(.bold700, size: 14)).foregroundColor(colors.ink)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(12)
                                .background(RoundedRectangle(cornerRadius: 12).fill(colors.bg))
                                .overlay(RoundedRectangle(cornerRadius: 12).stroke(colors.line, lineWidth: 1.5))
                                .contentShape(Rectangle())
                                .onTapGesture { picked = v }
                        }
                    }
                }
                .frame(maxHeight: 240)

                Spacer().frame(height: 8)
                Text("えらんだ1本").font(.kyono(.black900, size: 13)).foregroundColor(colors.sub)
                Text(picked.map { "選択中: \($0.t)" } ?? "まだえらんでいません 上の検索からどうぞ")
                    .font(.kyono(.bold700, size: 14)).foregroundColor(colors.sub)

                Spacer().frame(height: 16)
                KyonoPrimaryButton("カードをつくる✨") {
                    let days = BragCardRenderer.clampDays(Int(daysText) ?? 1)
                    let ds = RecordLogic.todayStr(now: Date())
                    let dateIdx = CardLottery.dateIdx(ds)
                    let data = CardDataLoader.shared
                    let theme = data.CARD_THEMES[dateIdx % data.CARD_THEMES.count]
                    let resolved = ResolvedTheme(name: theme.name, bg: theme.bg, main: theme.main, deco: theme.deco)
                    let png = BragCardRenderer.render(ds: ds, days: days, theme: resolved, favoriteTitle: picked?.t)
                    cardImage = UIImage(data: png)
                }
                Spacer().frame(height: 8)
                Text("えらんだ1本は カードにサムネイル画像で入ります").font(.kyono(.bold700, size: 13)).foregroundColor(colors.sub)
            }
        }
        .padding(16)
        .background(KyonoBackgroundColor().ignoresSafeArea())
        .sheet(isPresented: Binding(get: { cardImage != nil }, set: { if !$0 { cardImage = nil } })) {
            if let cardImage {
                VStack {
                    Image(uiImage: cardImage).resizable().scaledToFit()
                    HStack {
                        KyonoGhostButton("とじる") { self.cardImage = nil }
                        KyonoPrimaryButton("保存・シェアする") {
                            let days = BragCardRenderer.clampDays(Int(daysText) ?? 1)
                            ShareImage.share(uiImage: cardImage, text: "#きょうのオガトレ \(days)日つづいてる！")
                        }
                    }
                }
                .padding()
            }
        }
    }
}
