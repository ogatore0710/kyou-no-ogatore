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
//
//  ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
//  Phase 3: index.html:433-449 .searchbox/.catbtn/.catbtn.on/.chip-a〜d(カテゴリごとの配色)/.chip.onの1:1移植。

import SwiftUI
import RecordCore

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

// index.html:441-449 .chip-a〜d(カテゴリ色)の1:1移植(ライト/ダーク)。
private struct ChipColors {
    let bg: Color, border: Color, text: Color, onBg: Color, onBorder: Color, onText: Color
}

private func chipColors(for key: String, dark: Bool) -> ChipColors {
    switch key {
    case "a": return dark
        ? ChipColors(bg: Color(hex: 0x37301C), border: Color(hex: 0x5C4F1E), text: Color(hex: 0xE8C74C), onBg: Color(hex: 0xFFD93B), onBorder: Color(hex: 0xFFD93B), onText: Color(hex: 0x211E19))
        : ChipColors(bg: Color(hex: 0xFFF6D8), border: Color(hex: 0xF2DE8A), text: Color(hex: 0x8A6D00), onBg: Color(hex: 0xFFD93B), onBorder: Color(hex: 0xFFD93B), onText: Color(hex: 0x3A3A35))
    case "b": return dark
        ? ChipColors(bg: Color(hex: 0x1F3532), border: Color(hex: 0x2E5A52), text: Color(hex: 0x7BD0C4), onBg: Color(hex: 0x1E7B70), onBorder: Color(hex: 0x1E7B70), onText: .white)
        : ChipColors(bg: Color(hex: 0xE7F8F1), border: Color(hex: 0xBFE8DC), text: Color(hex: 0x177065), onBg: Color(hex: 0x1E7B70), onBorder: Color(hex: 0x1E7B70), onText: .white)
    case "c": return dark
        ? ChipColors(bg: Color(hex: 0x3A2730), border: Color(hex: 0x5E3A4C), text: Color(hex: 0xF09BC0), onBg: Color(hex: 0xE56A9A), onBorder: Color(hex: 0xE56A9A), onText: .white)
        : ChipColors(bg: Color(hex: 0xFFEDF3), border: Color(hex: 0xF5C6D8), text: Color(hex: 0xB0366E), onBg: Color(hex: 0xE56A9A), onBorder: Color(hex: 0xE56A9A), onText: .white)
    default: return dark
        ? ChipColors(bg: Color(hex: 0x2C2740), border: Color(hex: 0x4A4070), text: Color(hex: 0xB8A9F0), onBg: Color(hex: 0x8B7BD8), onBorder: Color(hex: 0x8B7BD8), onText: .white)
        : ChipColors(bg: Color(hex: 0xF1EDFF), border: Color(hex: 0xD6CCF5), text: Color(hex: 0x6A58B5), onBg: Color(hex: 0x8B7BD8), onBorder: Color(hex: 0x8B7BD8), onText: .white)
    }
}

private struct VideoRow: View {
    @Environment(\.kyonoColors) private var colors
    let v: CatalogVideo
    let openUrl: (String) -> Void

    var body: some View {
        Button {
            openUrl("https://www.youtube.com/watch?v=\(v.id)")
        } label: {
            VStack(alignment: .leading, spacing: 2) {
                if let tag = v.tags?.first {
                    Text(tag).font(.system(size: 11, weight: .black)).foregroundColor(colors.tealInk)
                }
                Text(v.t).font(.system(size: 14, weight: .bold)).foregroundColor(colors.ink)
                Text(v.s).font(.system(size: 12)).foregroundColor(colors.sub)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(12)
        .background(RoundedRectangle(cornerRadius: 12).fill(colors.bg))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(colors.line, lineWidth: 1.5))
        .buttonStyle(.plain)
    }
}

// index.html #search / app-search.js の1:1移植。カテゴリタブ→タグチップ→自由入力の3段絞り込み。
struct SearchView: View {
    let store: RecordStore
    let openUrl: (String) -> Void
    let onBack: () -> Void

    private let catalog = CatalogLoader.shared
    @State private var activeCat = tagCats[0].key
    @State private var activeTag: String?
    @State private var query = ""
    @State private var searchLimit = 24

    private var hits: [CatalogVideo] { searchCatalog(catalog, query: query, activeTag: activeTag, year: nil) }
    private var themeSetting: String { store.get("theme", default: "auto") }

    var body: some View {
        KyonoTheme(themeSetting: themeSetting) {
            SearchContentView(
                activeCat: $activeCat, activeTag: $activeTag, query: $query, searchLimit: $searchLimit,
                hits: hits, onBack: onBack, openUrl: openUrl
            )
        }
    }
}

private struct SearchContentView: View {
    @Environment(\.kyonoColors) private var colors
    @Environment(\.colorScheme) private var systemColorScheme
    @Binding var activeCat: String
    @Binding var activeTag: String?
    @Binding var query: String
    @Binding var searchLimit: Int
    let hits: [CatalogVideo]
    let onBack: () -> Void
    let openUrl: (String) -> Void

    private var dark: Bool { colors.bg == kyonoDarkColors.bg }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            KyonoLineButton("◀ もどる", action: onBack)
            Text("動画を探す").font(.kyono(.black900, size: 16)).foregroundColor(colors.ink)
            // index.html:945-949 .searchbox
            TextField("🔍 例: 肩こり／朝／むくみ", text: $query)
                .padding(.horizontal, 14).padding(.vertical, 10)
                .background(RoundedRectangle(cornerRadius: 16).fill(colors.card))
                .overlay(RoundedRectangle(cornerRadius: 16).stroke(colors.line, lineWidth: 2))
                .onChange(of: query) { _, _ in searchLimit = 24 }
            // index.html:436-437 .catbtn/.catbtn.on
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(tagCats, id: \.key) { cat in
                        let on = cat.key == activeCat
                        Text(cat.name).font(.system(size: 14, weight: .black)).foregroundColor(on ? colors.ink : colors.sub)
                            .padding(.horizontal, 13).padding(.vertical, 10)
                            .background(RoundedRectangle(cornerRadius: 12).fill(on ? colors.yellow : colors.line))
                            .onTapGesture { activeCat = cat.key; activeTag = nil }
                    }
                }
            }
            // index.html:440-449 .chip/.chip-a〜d/.chip.on
            let activeCatTags = tagCats.first { $0.key == activeCat }?.tags ?? []
            let cc = chipColors(for: activeCat, dark: dark)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 6) {
                    ForEach(activeCatTags, id: \.self) { tag in
                        let on = tag == activeTag
                        Text(tag).font(.system(size: 14, weight: .bold)).foregroundColor(on ? cc.onText : cc.text)
                            .padding(.horizontal, 16).padding(.vertical, 10)
                            .background(Capsule().fill(on ? cc.onBg : cc.bg))
                            .overlay(Capsule().stroke(on ? cc.onBorder : cc.border, lineWidth: 2))
                            .onTapGesture { activeTag = (activeTag == tag) ? nil : tag; searchLimit = 24 }
                    }
                }
            }
            Text("\(hits.count)件見つかりました").font(.system(size: 12)).foregroundColor(colors.sub)
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 6) {
                    ForEach(Array(hits.prefix(searchLimit)), id: \.id) { v in VideoRow(v: v, openUrl: openUrl) }
                    if hits.count > searchLimit {
                        KyonoGhostButton("もっと見る") { searchLimit += 48 }
                    }
                }
            }
        }
        .padding(16)
        .background(KyonoBackgroundColor().ignoresSafeArea())
    }
}

// 「再生リスト」= catalog.jsonの動画一覧をカテゴリ絞り込みなしで年降順にブラウズできる画面
// (スコープ解釈はファイル冒頭コメント参照)。
struct CatalogListView: View {
    let store: RecordStore
    let openUrl: (String) -> Void
    let onBack: () -> Void

    private let catalog = CatalogLoader.shared.sorted { a, b in a.y != b.y ? a.y > b.y : a.t < b.t }
    private var themeSetting: String { store.get("theme", default: "auto") }

    var body: some View {
        KyonoTheme(themeSetting: themeSetting) {
            CatalogListContentView(catalog: catalog, onBack: onBack, openUrl: openUrl)
        }
    }
}

private struct CatalogListContentView: View {
    @Environment(\.kyonoColors) private var colors
    let catalog: [CatalogVideo]
    let onBack: () -> Void
    let openUrl: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            KyonoLineButton("◀ もどる", action: onBack)
            Text("再生リスト").font(.kyono(.black900, size: 16)).foregroundColor(colors.ink)
            Text("\(catalog.count)本の動画").font(.system(size: 12)).foregroundColor(colors.sub)
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 6) {
                    ForEach(catalog, id: \.id) { v in VideoRow(v: v, openUrl: openUrl) }
                    // index.html:941 .hint(固定表示にするとFAB2段(右下)と重なるバグの再発になる=
                    // とどくメーターの5番目ボタンで既発見済みの教訓と同種のため、リスト末尾項目にする)
                    Text("タップするとYouTubeで開きます！テレビで流すのもおすすめ📺")
                        .font(.system(size: 13)).foregroundColor(colors.sub)
                        .padding(.top, 8).padding(.bottom, 90)
                }
            }
        }
        .padding(16)
        .background(KyonoBackgroundColor().ignoresSafeArea())
    }
}
