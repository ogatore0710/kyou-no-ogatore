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
import UIKit
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
        // app-search.js:48 q.split(/\s+/)の1:1移植。JSの\sはU+3000(全角スペース)を含むが、
        // Swiftのsplit(separator:" ")は半角スペースのみのため、日本語キーボード既定の全角
        // スペース区切り検索が必ず0件になっていた(TASK-C2-2026-07-28)。isWhitespaceはU+3000を含む。
        return q.lowercased().split(whereSeparator: { $0.isWhitespace }).allSatisfy { w in hay.contains(w) }
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

// 見た目パリティ第2弾(TASK-C2-2026-07-26-visual-parity-round2.md §2 動画サムネイル):
// index.html:328-336 .video(サムネイル112x16:9+タイトル3行clamp+badge)の1:1移植。
// KyonoAsyncImage(§1で新設)を再利用し、読み込み失敗時は何も表示しない(Web版onerrorと同じ)。
// index.html:1680-1685 vHTML/videoCard(badge引数)相当。ResultView(#result rxList)からも
// 使うためnon-privateにする(全画面完全性監査タスクのfollow-up=
// TASK-C2-2026-07-26-result-video-recommendations.md)。badge指定時はvideoCard()と同じく
// タグpillの代わりにbadge文言(「①まずほぐす」等)を表示する。
struct VideoRow: View {
    @Environment(\.kyonoColors) private var colors
    let v: CatalogVideo
    let openUrl: (String) -> Void
    var badge: String? = nil
    // TASK-C2-2026-07-27-fd-guide-ui-branch.md: index.html:337 .fd-hero .video(pink枠+pink-soft地)の
    // 1:1移植。はじめの1本ガイド中の①だけを視覚的に主役化する強調枠。
    var hero: Bool = false

    // index.html:137 body.dark .badge{color:#F0A58E}の1:1移植。ダークモード再確認タスク
    // (TASK-C2-2026-07-27-darkmode-recheck-and-nudges.md)で発覚: ライト固定色(#B4462F)のままだと
    // ダークモードのcoralSoft背景に対してコントラストが低すぎて読みにくかった。
    private var dark: Bool { colors.bg == kyonoDarkColors.bg }
    private var badgeTextColor: Color { dark ? Color(hex: 0xF0A58E) : Color(hex: 0xB4462F) }

    var body: some View {
        Button {
            openUrl("https://www.youtube.com/watch?v=\(v.id)")
        } label: {
            HStack(alignment: .top, spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12).fill(colors.line)
                    KyonoAsyncImage(url: youtubeThumbUrl(v.id))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .frame(width: 112, height: 112 * 9 / 16)
                VStack(alignment: .leading, spacing: 2) {
                    if let label = badge ?? v.tags?.first {
                        Text(label).kyonoFont(.black900, size: 12).foregroundColor(badgeTextColor)
                            .padding(.horizontal, 8).padding(.vertical, 1)
                            .background(Capsule().fill(colors.coralSoft))
                    }
                    Text(v.t).kyonoFont(.bold700, size: 15).foregroundColor(colors.ink).lineLimit(3)
                    Text(v.s).kyonoFont(.bold700, size: 14).foregroundColor(colors.sub).lineLimit(1)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .padding(10)
        .background(RoundedRectangle(cornerRadius: 16).fill(hero ? colors.pinkSoft : colors.card))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(hero ? colors.pink : colors.line, lineWidth: hero ? 2.5 : 1.5))
        // TASK-C2-2026-07-27-text-size-accessibility.md 項目4: サムネイル(装飾)+バッジ+タイトル+
        // 補足を1回のVoiceOverスワイプで読める1つの単位にまとめる。
        .accessibilityElement(children: .combine)
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
    // 挙動パリティ監査タスク(TASK-C2-2026-07-27-behavior-parity-audit.md §B): app-search.js:55
    // onSearchInput()の180msデバウンス(IME変換中の毎打鍵フル再描画を間引く)の1:1移植。
    // TextField自体は即時反映するが、実際のフィルタ(hits)はdebouncedQueryを使う。
    @State private var debouncedQuery = ""
    @State private var debounceTask: Task<Void, Never>?
    @State private var searchLimit = 24
    // 全画面完全性監査タスク(TASK-C2-2026-07-26-full-completeness-audit.md #search):
    // index.html:955 #ySel(年フィルタ)の1:1移植。searchCatalogのyearパラメータ自体は既存で
    // あったが、選択UIが無く常にnilで呼ばれていた(=年フィルタが機能していなかった)欠落。
    @State private var selectedYear: Int?

    private var years: [Int] { Array(Set(catalog.map { $0.y })).sorted(by: >) }
    private var hits: [CatalogVideo] { searchCatalog(catalog, query: debouncedQuery, activeTag: activeTag, year: selectedYear) }
    private var themeSetting: String { store.get("theme", default: "auto") }

    var body: some View {
        KyonoTheme(themeSetting: themeSetting, bigText: store.get("bigtext", default: true)) {
            SearchContentView(
                activeCat: $activeCat, activeTag: $activeTag, query: $query, searchLimit: $searchLimit,
                selectedYear: $selectedYear, years: years,
                hits: hits, onBack: onBack, openUrl: openUrl
            )
        }
        .onChange(of: query) { _, newValue in
            debounceTask?.cancel()
            debounceTask = Task {
                try? await Task.sleep(nanoseconds: 180_000_000)
                if !Task.isCancelled { debouncedQuery = newValue }
            }
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
    @Binding var selectedYear: Int?
    let years: [Int]
    let hits: [CatalogVideo]
    let onBack: () -> Void
    let openUrl: (String) -> Void

    private var dark: Bool { colors.bg == kyonoDarkColors.bg }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // 見た目パリティ移植の仕上げ(TASK-C2-2026-07-26-native-visual-design-parity-cleanup.md):
            // タブバー導入後は「戻る」概念が無いWeb版に合わせ、タブ画面から「◀ もどる」ボタンを削除。
            Text("動画を探す").kyonoFont(.black900, size: 16).foregroundColor(colors.ink)
            // index.html:945-949 .searchbox
            TextField("🔍 例: 肩こり／朝／むくみ", text: $query)
                .padding(.horizontal, 14).padding(.vertical, 10)
                .background(RoundedRectangle(cornerRadius: 16).fill(colors.card))
                .overlay(RoundedRectangle(cornerRadius: 16).stroke(colors.line, lineWidth: 2))
                .onChange(of: query) { _, _ in searchLimit = 24 }
            // index.html:436-437 .catbtn/.catbtn.on。TASK-C2-2026-07-27-chips-overflow-and-
            // bubble-pop.md §1: index.html:434 .catrow{overflow-x:auto}(検索画面のカテゴリ行は
            // 元から横スクロール仕様)+右端フェード+「›」ヒントの1:1移植。
            FadingChipRow(spacing: 6) {
                ForEach(tagCats, id: \.key) { cat in
                    let on = cat.key == activeCat
                    Text(cat.name).kyonoFont(.black900, size: 14).foregroundColor(on ? colors.ink : colors.sub)
                        .padding(.horizontal, 13).padding(.vertical, 10)
                        .background(RoundedRectangle(cornerRadius: 12).fill(on ? colors.yellow : colors.line))
                        .onTapGesture { activeCat = cat.key; activeTag = nil }
                }
            }
            // index.html:440-449,952 .chip/.chip-a〜d/.chip.on。TASK-C2-2026-07-27-chips-overflow-
            // and-bubble-pop.md §5: .chipsは既定でflex-wrap:wrap(横スクロールは相談室フッターのみの
            // 例外)のため、折り返しレイアウトにする(3つ目以降が見えなくなっていた欠落の修正)。
            let activeCatTags = tagCats.first { $0.key == activeCat }?.tags ?? []
            let cc = chipColors(for: activeCat, dark: dark)
            FlowLayout(spacing: 6, lineSpacing: 6, alignment: .leading) {
                ForEach(activeCatTags, id: \.self) { tag in
                    let on = tag == activeTag
                    Text(tag).kyonoFont(.bold700, size: 14).foregroundColor(on ? cc.onText : cc.text)
                        .padding(.horizontal, 16).padding(.vertical, 10)
                        .background(Capsule().fill(on ? cc.onBg : cc.bg))
                        .overlay(Capsule().stroke(on ? cc.onBorder : cc.border, lineWidth: 2))
                        .onTapGesture { activeTag = (activeTag == tag) ? nil : tag; searchLimit = 24 }
                }
            }
            HStack {
                Menu {
                    Button("すべての年") { selectedYear = nil; searchLimit = 24 }
                    ForEach(years, id: \.self) { y in
                        Button("\(y)年") { selectedYear = y; searchLimit = 24 }
                    }
                } label: {
                    Text((selectedYear.map { "\($0)年" } ?? "すべての年") + " ▾")
                        .kyonoFont(.black900, size: 14).foregroundColor(colors.sub)
                        .padding(.horizontal, 10).padding(.vertical, 6)
                        .background(RoundedRectangle(cornerRadius: 12).fill(colors.card))
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(colors.line, lineWidth: 2))
                }
                Spacer()
                Text("\(hits.count)件見つかりました").kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)
            }
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 6) {
                    ForEach(Array(hits.prefix(searchLimit)), id: \.id) { v in VideoRow(v: v, openUrl: openUrl) }
                    if hits.count > searchLimit {
                        KyonoGhostButton("もっと見る") { searchLimit += 48 }
                    }
                    // 動画を探す画面のリクエスト導線欠落修正タスク(TASK-C2-2026-07-26-search-request-box.md):
                    // index.html:960-963 #reqBox(app-search.js drawResults()のreqMsg/reqBtn組み立て・
                    // index.html copyMailAddr()の1:1移植)。検索ロジック自体は変更していない。
                    let kwText = [query.trimmingCharacters(in: .whitespacesAndNewlines), activeTag].compactMap { $0?.isEmpty == false ? $0 : nil }.joined(separator: " ")
                    ReqBox(shown: !hits.isEmpty, kwText: kwText)
                }
            }
        }
        .padding(16)
        .background(KyonoBackgroundColor().ignoresSafeArea())
    }
}

// index.html:961 reqMsg/reqBtnの表示切り替え。offlineCat分岐(Web版はCATALOG未ロード時の対応)は
// ネイティブではcatalog.jsonをバンドルリソースとして常に同期ロードするため該当せず、常時表示でよい。
private struct ReqBox: View {
    @Environment(\.kyonoColors) private var colors
    let shown: Bool
    let kwText: String
    @State private var copied = false

    var body: some View {
        KyonoGradientCard(gradient: .warm) {
            Text(shown
                ? "やりたいストレッチが見つからない？\nオガトレに直接リクエストを送れます📮"
                : "ごめんなさい まだなかったみたい💦\nリクエストを送ってもらえたら動画づくりの参考にします📮")
                .kyonoFont(.bold700, size: 15).foregroundColor(colors.ink)
            Spacer().frame(height: 12)
            KyonoGhostButton(kwText.isEmpty ? "リクエストを送る" : "「\(kwText)」をリクエストする") {
                openRequestMail(kwText: kwText)
            }
            Spacer().frame(height: 6)
            Text("メールがひらかない方は kyou-no@ogatore.jp へ直接どうぞ")
                .kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)
                .frame(maxWidth: .infinity, alignment: .center)
                .multilineTextAlignment(.center)
            Spacer().frame(height: 4)
            Text(copied ? "コピーしました✅" : "📋 アドレスをコピー")
                .kyonoFont(.black900, size: 12).foregroundColor(colors.tealInk)
                .frame(maxWidth: .infinity, alignment: .center)
                .onTapGesture {
                    UIPasteboard.general.string = "kyou-no@ogatore.jp"
                    copied = true
                    Task {
                        try? await Task.sleep(nanoseconds: 2_000_000_000)
                        copied = false
                    }
                }
        }
    }
}

// index.html copyMailAddr()と同じ宛先。mailto:はUIApplication.shared.openで既定のメールAppへ委譲
// (Web版のreqBtn.hrefと同じ発想。他の外部リンク(YouTube等)と同じopenUrlパターンに揃える)。
private func openRequestMail(kwText: String) {
    let subject = "ストレッチのリクエスト（きょうのオガトレ）"
    let body = "こんなストレッチの動画が欲しいです：\n\(kwText.isEmpty ? "（ここに書いてね）" : kwText)\n\n--\nきょうのオガトレ「動画を探す」から送信"
    openMailTo(to: "kyou-no@ogatore.jp", subject: subject, body: body)
}

// TASK-C2-2026-07-27-soudan-safety-copy-and-links: 相談室のフォールバック逃げ道リンクからも
// 同じmailto導線を再利用するため、宛先/件名/本文を汎用パラメータ化してprivateを外す。
func openMailTo(to: String, subject: String, body: String) {
    var comps = URLComponents()
    comps.scheme = "mailto"
    comps.path = to
    comps.queryItems = [URLQueryItem(name: "subject", value: subject), URLQueryItem(name: "body", value: body)]
    if let url = comps.url {
        UIApplication.shared.open(url)
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
        KyonoTheme(themeSetting: themeSetting, bigText: store.get("bigtext", default: true)) {
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
            Text("再生リスト").kyonoFont(.black900, size: 16).foregroundColor(colors.ink)
            Text("\(catalog.count)本の動画").kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 6) {
                    ForEach(catalog, id: \.id) { v in VideoRow(v: v, openUrl: openUrl) }
                    // index.html:941 .hint(固定表示にするとFAB2段(右下)と重なるバグの再発になる=
                    // とどくメーターの5番目ボタンで既発見済みの教訓と同種のため、リスト末尾項目にする)
                    Text("タップするとYouTubeで開きます！テレビで流すのもおすすめ📺")
                        .kyonoFont(.bold700, size: 13).foregroundColor(colors.sub)
                        .padding(.top, 8).padding(.bottom, 90)
                }
            }
        }
        .padding(16)
        .background(KyonoBackgroundColor().ignoresSafeArea())
    }
}
