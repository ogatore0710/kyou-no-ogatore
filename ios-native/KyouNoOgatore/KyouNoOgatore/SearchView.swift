//
//  SearchView.swift
//  KyouNoOgatore
//
//  ネイティブ移植 Step 7a(マスタープラン§6 Step 7a・§2-1「app-search.js TAG_CATS」行): 検索(TAG_CATS)・
//  再生リストUI(Android版SearchScreen.kt/CatalogListScreen.ktと同一ロジックのSwiftUI実装)。
//  判定ロジックは存在しない画面(単純な文字列フィルタ)なので、Web版app-search.js currentHits()の
//  1:1移植をこのファイルに直接持つ。
//
//  スコープ解釈の注記(タスクの「再生リスト（catalog.json）」表記について): Web版の「再生リスト」タブは
//  実際にはcatalog.jsonでなくindex.html内の別配列PLAYLISTS(手動キュレーションのYouTubeプレイリストID
//  約20件・機械抽出スクリプト未整備)が情報源で、catalog.json(454件)を情報源とするのは「検索」タブの方。
//  タスク文面がcatalog.jsonを再生リストの情報源として明記しているため、本実装では「再生リスト」を
//  「catalog.jsonの動画をカテゴリ絞り込みなしで一覧できる画面」として実装した(Android版と同じ判断。
//  詳細はSearchScreen.ktの冒頭コメント参照)。

import SwiftUI

struct TagCatDef {
    let key: String
    let name: String
    let tags: [String]
}

// app-search.js:6-11 TAG_CATS の1:1移植。
let tagCats: [TagCatDef] = [
    TagCatDef(key: "b", name: "からだの場所", tags: ["全身", "肩・肩甲骨", "首・肩こり", "姿勢・背中", "股関節", "開脚", "もも裏", "太もも・お尻", "腰", "ひざ・O脚", "足首・足うら"]),
    TagCatDef(key: "a", name: "時間・シーン", tags: ["朝", "夜・寝る前", "座ったまま", "10分以内", "ショート"]),
    TagCatDef(key: "c", name: "目的", tags: ["むくみ", "引き締め", "筋膜・マッサージ", "自律神経", "スポーツ・運動前後", "生活・セルフケア"]),
    TagCatDef(key: "d", name: "その他", tags: ["解説", "水族館ロケ", "古民家ロケ", "その他"]),
]

// app-search.js:40-50 currentHits() の1:1移植。
func searchCatalog(_ catalog: [CatalogVideo], query: String, activeTag: String?, year: Int?) -> [CatalogVideo] {
    let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
    return catalog.filter { v in
        if let activeTag, !(v.tags ?? []).contains(activeTag) { return false }
        if let year, v.y != year { return false }
        if q.isEmpty { return true }
        let hay = (v.t + " " + (v.tags ?? []).joined(separator: " ") + " " + String(v.y) + "年").lowercased()
        return q.lowercased().split(separator: " ").allSatisfy { w in hay.contains(w) }
    }
}

private struct VideoRow: View {
    let v: CatalogVideo
    let openUrl: (String) -> Void

    var body: some View {
        Button {
            openUrl("https://www.youtube.com/watch?v=\(v.id)")
        } label: {
            VStack(alignment: .leading, spacing: 2) {
                if let tag = v.tags?.first {
                    Text(tag).font(.caption).foregroundColor(Color(red: 0.42, green: 0.31, blue: 0.65))
                }
                Text(v.t)
                Text(v.s).font(.caption).foregroundColor(.gray)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(10)
        .background(Color(red: 0.95, green: 0.95, blue: 0.93))
        .cornerRadius(10)
        .buttonStyle(.plain)
    }
}

// index.html #search / app-search.js の1:1移植。カテゴリタブ→タグチップ→自由入力の3段絞り込み。
struct SearchView: View {
    let openUrl: (String) -> Void
    let onBack: () -> Void

    private let catalog = CatalogLoader.shared
    @State private var activeCat = tagCats[0].key
    @State private var activeTag: String?
    @State private var query = ""
    @State private var searchLimit = 24

    private var hits: [CatalogVideo] { searchCatalog(catalog, query: query, activeTag: activeTag, year: nil) }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("動画を探す").font(.title2.bold())
            Button("◀ もどる", action: onBack)
            TextField("肩こり、腰痛など", text: $query)
                .textFieldStyle(.roundedBorder)
                .onChange(of: query) { _, _ in searchLimit = 24 }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack {
                    ForEach(tagCats, id: \.key) { cat in
                        Button(cat.name) { activeCat = cat.key; activeTag = nil }
                            .buttonStyle(.borderedProminent)
                            .tint(cat.key == activeCat ? Color(red: 0.42, green: 0.31, blue: 0.65) : Color(red: 0.91, green: 0.89, blue: 0.96))
                    }
                }
            }
            let activeCatTags = tagCats.first { $0.key == activeCat }?.tags ?? []
            ScrollView(.horizontal, showsIndicators: false) {
                HStack {
                    ForEach(activeCatTags, id: \.self) { tag in
                        Button(tag) { activeTag = (activeTag == tag) ? nil : tag; searchLimit = 24 }
                            .buttonStyle(.borderedProminent)
                            .tint(tag == activeTag ? Color(red: 0.42, green: 0.31, blue: 0.65) : Color(red: 0.95, green: 0.95, blue: 0.93))
                    }
                }
            }
            Text("\(hits.count)件見つかりました").font(.caption)
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 6) {
                    ForEach(Array(hits.prefix(searchLimit)), id: \.id) { v in VideoRow(v: v, openUrl: openUrl) }
                    if hits.count > searchLimit {
                        Button("もっと見る") { searchLimit += 48 }
                            .frame(maxWidth: .infinity)
                    }
                }
            }
        }
        .padding(16)
    }
}

// 「再生リスト」= catalog.jsonの動画一覧をカテゴリ絞り込みなしで年降順にブラウズできる画面
// (スコープ解釈はファイル冒頭コメント参照)。
struct CatalogListView: View {
    let openUrl: (String) -> Void
    let onBack: () -> Void

    private let catalog = CatalogLoader.shared.sorted { a, b in a.y != b.y ? a.y > b.y : a.t < b.t }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("再生リスト").font(.title2.bold())
            Button("◀ もどる", action: onBack)
            Text("\(catalog.count)本の動画").font(.caption)
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 6) {
                    ForEach(catalog, id: \.id) { v in VideoRow(v: v, openUrl: openUrl) }
                }
            }
        }
        .padding(16)
    }
}
