//
//  BragView.swift
//  KyouNoOgatore
//
//  ネイティブ移植 Step 7b(マスタープラン§6 Step 7b・§2-1「index.html drawBragCard」行): じまんカード
//  作成UI(Android版BragScreen.ktと同一ロジック。index.html openBrag()〜makeBragCard()の1:1移植)。
//  日数入力(1〜9999の整数のみ・自由入力なし)+動画検索(Step7aのsearchCatalogを再利用)+作成ボタン。
//  判定/描画ロジックはBragCardRenderer(Step7b新設)を呼ぶだけ。

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

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button("◀ もどる", action: onBack)
            Text("じまんカードをつくる").font(.title2.bold())

            Text("つづけてる日数")
            TextField("", text: $daysText)
                .keyboardType(.numberPad)
                .textFieldStyle(.roundedBorder)
                .onChange(of: daysText) { _, newValue in
                    let filtered = newValue.filter { $0.isNumber }
                    daysText = String(filtered.prefix(4))
                }

            Text("すきな1本をさがす")
            TextField("動画のタイトルやタグで検索", text: $query).textFieldStyle(.roundedBorder)
            if let picked { Text("選択中: \(picked.t)") }
            ScrollView {
                LazyVStack(alignment: .leading) {
                    ForEach(hits, id: \.id) { v in
                        Text(v.t).onTapGesture { picked = v }.padding(.vertical, 4)
                    }
                }
            }

            Button("カードをつくる✨") {
                let days = BragCardRenderer.clampDays(Int(daysText) ?? 1)
                let ds = RecordLogic.todayStr(now: Date())
                let dateIdx = CardLottery.dateIdx(ds)
                let data = CardDataLoader.shared
                let theme = data.CARD_THEMES[dateIdx % data.CARD_THEMES.count]
                let resolved = ResolvedTheme(name: theme.name, bg: theme.bg, main: theme.main, deco: theme.deco)
                let png = BragCardRenderer.render(ds: ds, days: days, theme: resolved, favoriteTitle: picked?.t)
                cardImage = UIImage(data: png)
            }
            .buttonStyle(.borderedProminent)
            .frame(maxWidth: .infinity)
        }
        .padding(16)
        .sheet(isPresented: Binding(get: { cardImage != nil }, set: { if !$0 { cardImage = nil } })) {
            if let cardImage {
                VStack {
                    Image(uiImage: cardImage).resizable().scaledToFit()
                    HStack {
                        Button("とじる") { self.cardImage = nil }
                        Button("保存・シェアする") {
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
