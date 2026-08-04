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
// TASK-C2-2026-08-01-build15-subtraction9.md #2: yearパラメータは年セレクタUI削除に伴い
// 除去(ネイティブのみ。呼び出し元だったUIが無くなったため引数ごと不要になった)。
func searchCatalog(_ catalog: [CatalogVideo], query: String, activeTag: String?) -> [CatalogVideo] {
    let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
    return catalog.filter { v in
        if let activeTag, !(v.tags ?? []).contains(activeTag) { return false }
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
    // TASK-C2-2026-08-04-build21-color-system-navy.md D2(選択中チップ): 黄地+白文字は実測1.38:1で
    // 読めなくなる罠のため、onBg/onBorderを#6B5500へ(白文字と実測7.18:1)。b/c/d同様onは
    // テーマ非依存の単一値にする(新しい色トークンは増やさない・既存方針を踏襲)。
    case "a": return dark
        ? ChipColors(bg: Color(hex: 0x37301C), border: Color(hex: 0x5C4F1E), text: Color(hex: 0xE8C74C), onBg: Color(hex: 0x6B5500), onBorder: Color(hex: 0x6B5500), onText: .white)
        : ChipColors(bg: Color(hex: 0xFFF6D8), border: Color(hex: 0xF2DE8A), text: Color(hex: 0x8A6D00), onBg: Color(hex: 0x6B5500), onBorder: Color(hex: 0x6B5500), onText: .white)
    case "b": return dark
        ? ChipColors(bg: Color(hex: 0x1F3532), border: Color(hex: 0x2E5A52), text: Color(hex: 0x7BD0C4), onBg: Color(hex: 0x1E7B70), onBorder: Color(hex: 0x1E7B70), onText: .white)
        : ChipColors(bg: Color(hex: 0xE7F8F1), border: Color(hex: 0xBFE8DC), text: Color(hex: 0x177065), onBg: Color(hex: 0x1E7B70), onBorder: Color(hex: 0x1E7B70), onText: .white)
    // TASK-C2-2026-08-02-build16-polish-and-ia.md P-7: 選択中チップ(on)の白文字コントラスト。
    // pink(#E56A9A)/purple(#8B7BD8)地に白文字は実測3.06:1/3.55:1でWCAG AA(4.5:1)未達だった。
    // teal(case b)が既にonBg/onBorderへ素のteal(#2BB3A3)でなくtealStrong(#1E7B70)という
    // 濃い変種を使っている前例に倣い、pink/purpleも「この画面の未選択時textとして既に使っている
    // 濃い変種」(0xB0366E/0x6A58B5、どちらも実測4.5:1超)へ差し替える(新しい色トークンは増やさない)。
    // onBgは白文字向けの固定色のためlight/dark共通(teal案と同じ設計)。
    case "c": return dark
        ? ChipColors(bg: Color(hex: 0x3A2730), border: Color(hex: 0x5E3A4C), text: Color(hex: 0xF09BC0), onBg: Color(hex: 0xB0366E), onBorder: Color(hex: 0xB0366E), onText: .white)
        : ChipColors(bg: Color(hex: 0xFFEDF3), border: Color(hex: 0xF5C6D8), text: Color(hex: 0xB0366E), onBg: Color(hex: 0xB0366E), onBorder: Color(hex: 0xB0366E), onText: .white)
    default: return dark
        ? ChipColors(bg: Color(hex: 0x2C2740), border: Color(hex: 0x4A4070), text: Color(hex: 0xB8A9F0), onBg: Color(hex: 0x6A58B5), onBorder: Color(hex: 0x6A58B5), onText: .white)
        : ChipColors(bg: Color(hex: 0xF1EDFF), border: Color(hex: 0xD6CCF5), text: Color(hex: 0x6A58B5), onBg: Color(hex: 0x6A58B5), onBorder: Color(hex: 0x6A58B5), onText: .white)
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
    // TASK-C2-2026-08-03-build18-tutorial-quality.md B-7: TASK-C2-2026-08-02-build17-feedback-
    // fixes.md Q-4でfdGuide中はopenUrlをno-opにしたが、見た目は普段どおりタップできそうに
    // 見えており「押せるように見える嘘」になっていた(本人指摘)。no-op裁定自体は維持しつつ、
    // 減光+実際にdisabled(true)にして見た目でも押せないことを明示する。
    var disabledLook: Bool = false

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
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(hero ? colors.pink : colors.borderStrong, lineWidth: hero ? 2.5 : 1.5))
        // TASK-C2-2026-07-27-text-size-accessibility.md 項目4: サムネイル(装飾)+バッジ+タイトル+
        // 補足を1回のVoiceOverスワイプで読める1つの単位にまとめる。
        .accessibilityElement(children: .combine)
        .buttonStyle(.plain)
        .opacity(disabledLook ? 0.5 : 1)
        .disabled(disabledLook)
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
    private var hits: [CatalogVideo] { searchCatalog(catalog, query: debouncedQuery, activeTag: activeTag) }
    private var themeSetting: String { store.get("theme", default: "light") }

    var body: some View {
        KyonoTheme(themeSetting: themeSetting, bigText: store.get("bigtext", default: true)) {
            SearchContentView(
                activeCat: $activeCat, activeTag: $activeTag, query: $query, searchLimit: $searchLimit,
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
        // iOSスワイプもどり導線追加タスク: Android版GuideScreen.ktのBackHandler(D1)対比の一環。
        // このアコーディオン状態を持たない画面ではonBackへ直行するだけでよい(EdgeSwipeBack.swift参照)。
        .edgeSwipeBack(onBack: onBack)
    }
}

private struct SearchContentView: View {
    @Environment(\.kyonoColors) private var colors
    @Environment(\.colorScheme) private var systemColorScheme
    // UI/UXパリティ監査GO-3(iOS・2026-07-29): KyonoCardと同じズーム対応(チップのみ・第1段階)。
    @Environment(\.kyonoBigText) private var bigText
    @Binding var activeCat: String
    @Binding var activeTag: String?
    @Binding var query: String
    @Binding var searchLimit: Int
    let hits: [CatalogVideo]
    let onBack: () -> Void
    let openUrl: (String) -> Void

    private var dark: Bool { colors.bg == kyonoDarkColors.bg }
    private var zoom: CGFloat { bigText ? kyonoBigTextScale : kyonoNormalTextScale }
    // GO-G14(5視点ワンループ): ホームと同じenvBanner(オフライン案内)をこの画面にも出す。
    @StateObject private var networkMonitor = NetworkMonitor()

    // TASK-C2-2026-07-31-feedback-round2.md B-2 → bodypart-art-rollout: 「からだの場所」
    // カテゴリ(key "b")のタグチップにChipArt/chip-<key>.pngのイラストを付ける。日本語ラベル→
    // アセットキーの対応表を持つ(OnboardingViews.swift:295-298のchip-\(chip.v)命名と同じ規則)。
    // 「腰」はB-2時点ではオンボと同じchip-youtsuu.pngを再利用していたが、bodypart-art-rollout
    // (記録カード風・本人選定トーン)でタグ側だけ絵を差し替えるにあたり、オンボのchip-youtsuu.png
    // (かたさチェック「腰」ワーリーチップ)には触れない方針のため、専用キー"koshi"を新設して
    // 分離した。
    // TASK-C2-2026-07-31-build12-journey2-splash-emoji.md W2-10: 残り3カテゴリ(a=時間・シーン・
    // c=目的・d=その他)にも新規生成15種を適用し、全4カテゴリのタグにアイコンが付くようにする。
    private let tagChipKey: [String: [String: String]] = [
        "b": [
            "全身": "zenshin",
            "肩・肩甲骨": "kata",
            "首・肩こり": "kubi",
            "姿勢・背中": "senaka",
            "股関節": "kokansetsu",
            "開脚": "kaikyaku",
            "もも裏": "momoura",
            "太もも・お尻": "futomomo",
            "腰": "koshi",
            "ひざ・O脚": "hiza",
            "足首・足うら": "ashikubi",
        ],
        "a": [
            "朝": "toki_asa",
            "夜・寝る前": "toki_yoru",
            "座ったまま": "toki_suwaru",
            "10分以内": "toki_10pun",
            "ショート": "toki_short",
        ],
        "c": [
            "むくみ": "mokuteki_mukumi",
            "引き締め": "mokuteki_hikishime",
            "筋膜・マッサージ": "mokuteki_massage",
            "自律神経": "mokuteki_jiritsu",
            "スポーツ・運動前後": "mokuteki_sports",
            "生活・セルフケア": "mokuteki_selfcare",
        ],
        "d": [
            "解説": "sonota_kaisetsu",
            "水族館ロケ": "sonota_suizokukan",
            "古民家ロケ": "sonota_kominka",
            "その他": "sonota_sonota",
        ],
    ]

    var body: some View {
        // TestFlight実機フィードバックC2(2026-07-29): 「動画を探す」でhits.countは454件正しく
        // 入っているのに、カードが1枚しか描画されず「もっと見る」も出ない欠陥があった。
        // index.html:945-963を確認すると、Web版の#searchはsearchbox/catrow/chips/vlist/moreBtn/
        // reqBoxが全部同じ<section>の中の兄弟要素で、ページ全体が1つのスクロール領域になっている
        // (検索結果だけを独立スクロール領域にする構造はWeb版に存在しない)。
        // 従来の実装はこれと逆に、「非スクロールのVStack(検索欄・チップ)」の中に
        // 「結果だけを囲むScrollView」をネストし、その外側のVStack全体に
        // kyonoScreenPadding()(padding(.bottom,180*zoom)を含む)を当てていた。ScrollViewが
        // 高さの手がかりを持たないVStackの中に置かれ、かつ外側に大きな下パディングが乗ることで
        // ScrollViewの実効ビューポート高さの計算が壊れ、LazyVStackが最初の1件しか実体化しない
        // 状態になっていた(SwiftUIの既知の落とし穴: ScrollViewをフレーム不定のVStackへ
        // ネストすると、LazyVStackが「表示範囲」を正しく認識できないことがある)。
        // HomeView.swift(既に正しく動いている画面)と同じ「ScrollViewを最外殻にし、内側の
        // VStackへkyonoScreenPadding()を当てる」形に合わせて直す(Web版の1枚スクロールとも一致)。
        ScrollView {
        VStack(alignment: .leading, spacing: 8) {
            // 見た目パリティ移植の仕上げ(TASK-C2-2026-07-26-native-visual-design-parity-cleanup.md):
            // タブバー導入後は「戻る」概念が無いWeb版に合わせ、タブ画面から「◀ もどる」ボタンを削除。
            // UI/UXパリティ監査GO-5(2026-07-28): index.html:91-94,945 Web版の#search節には
            // 独自タイトルが無く、セクション切り替えの外にある共通ヘッダー(.logo)だけで構成されている。
            // 「動画を探す」という単独タイトルはネイティブ独自の代替であり、共通ヘッダーへ置き換える。
            KyonoAppHeader()
            Spacer().frame(height: 8)
            // GO-G14(5視点ワンループ): 文言・スタイルはホーム版(HomeView.swift)と完全に同じにする。
            if networkMonitor.isOffline {
                Text("いま電波がないみたい 動画を見るには電波が必要だよ（「きょうやった！」の記録はつけられるよ）")
                    .kyonoFont(.bold700, size: 15).foregroundColor(colors.ink).lineSpacing(9)
                    .padding(.horizontal, 12).padding(.vertical, 10)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        RoundedRectangle(cornerRadius: 14).fill(colors.yellowSoft)
                            .overlay(RoundedRectangle(cornerRadius: 14).stroke(colors.yellow, lineWidth: 1.5))
                    )
            }
            // index.html:945-949 .searchbox。TASK-C2-2026-07-29-ux-audit-G.md G4(消去ボタン):
            // index.html:430「#qClear: 検索クリア✕: 文字があるときだけ表示。50-60代向けに指で
            // 押せる大きさ・searchboxの高さは増やさない」の1:1移植。WebKit標準の小さい✕(native
            // では最初からTextField標準の消去アイコンが無いため二重化の懸念自体が無い)と違い、
            // これだけが唯一の消去手段になる。
            HStack(spacing: 6) {
                // アイコン方針(TASK-C2-2026-07-30-icon-system.md)判断: 検索欄の虫めがねは
                // この年齢層にとっていちばん通じる目印なので、プレースホルダー内の絵文字を
                // 削除するだけでなく、タブバーと同じ`.search`アイコンを実際に左側へ差し込む
                // (alan5判断・2026-07-30)。右のG4消去ボタンとは反対側なので場所は競合しない。
                KyonoIconGlyph(icon: .search, fill: .clear, accent: colors.sub)
                    .frame(width: 18 * zoom, height: 18 * zoom)
                // TASK-C2-2026-08-04-build20-addendum.md A-1: 文字色未指定バグの棚卸し対象。
                TextField("例: 肩こり／朝／むくみ", text: $query)
                    .foregroundColor(colors.ink)
                    .tint(colors.ink)
                if !query.isEmpty {
                    Button {
                        query = ""
                    } label: {
                        Text("✕").kyonoFont(.black900, size: 14).foregroundColor(colors.ink)
                            .frame(width: 30 * zoom, height: 30 * zoom)
                            .background(Circle().fill(colors.line))
                    }
                    .accessibilityLabel("入力をけす")
                    .accessibilityIdentifier("searchClearBtn")
                }
            }
                .padding(.horizontal, 14).padding(.vertical, 10)
                .background(RoundedRectangle(cornerRadius: 16).fill(colors.card))
                .overlay(RoundedRectangle(cornerRadius: 16).stroke(colors.borderStrong, lineWidth: 2))
                .onChange(of: query) { _, _ in searchLimit = 24 }
            // index.html:436-437 .catbtn/.catbtn.on。TASK-C2-2026-07-27-chips-overflow-and-
            // bubble-pop.md §1: index.html:434 .catrow{overflow-x:auto}(検索画面のカテゴリ行は
            // 元から横スクロール仕様)+右端フェード+「›」ヒントの1:1移植。
            FadingChipRow(spacing: 6) {
                ForEach(tagCats, id: \.key) { cat in
                    let on = cat.key == activeCat
                    // B1(2026-07-29): 選択中(on=true)は黄色背景になるため、colors.inkではなく
                    // colors.yellowInk(ライト値固定)を使う。
                    // TASK-C2-2026-08-04-build22-yellow-return.md Z-1(藍トークン全撤去): build21の
                    // 「黄は面として使わない」拡張判断を撤回し、黄地+yellowInk文字(build21以前の姿)
                    // へ戻す。
                    Text(cat.name).kyonoFont(.black900, size: 14).foregroundColor(on ? colors.yellowInk : colors.sub)
                        .padding(.horizontal, 13 * zoom).padding(.vertical, 10 * zoom)
                        .background(RoundedRectangle(cornerRadius: 12 * zoom).fill(on ? colors.yellow : colors.line))
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
                    HStack(spacing: 4) {
                        // TASK-C2-2026-07-31-feedback-round2.md B-2→build12 W2-10: 4カテゴリ全部の
                        // タグにアイコン付与。対応表に無いタグはifが不成立になりアイコンなしのまま
                        // 素通りする。
                        if let key = tagChipKey[activeCat]?[tag],
                           let url = Bundle.main.url(forResource: "chip-\(key)", withExtension: "png"),
                           let uiImage = UIImage(contentsOfFile: url.path) {
                            // TASK-C2-2026-07-31-bodypart-art-legibility.md: 老眼対応で22pt→28ptへ拡大。
                            Image(uiImage: uiImage).resizable().scaledToFit().frame(width: 28 * zoom, height: 28 * zoom)
                        }
                        Text(tag).kyonoFont(.bold700, size: 14).foregroundColor(on ? cc.onText : cc.text)
                    }
                        .padding(.horizontal, 16 * zoom).padding(.vertical, 10 * zoom)
                        .background(Capsule().fill(on ? cc.onBg : cc.bg))
                        .overlay(Capsule().stroke(on ? cc.onBorder : cc.border, lineWidth: 2 * zoom))
                        .onTapGesture { activeTag = (activeTag == tag) ? nil : tag; searchLimit = 24 }
                }
            }
            // index.html:952 #filterNowの1:1移植。TASK-C2-2026-07-29-ux-audit-G.md G4(ピル・最優先):
            // 監査の指摘「タグで絞り込み中だと分からないまま、なぜか11本しか出ない状態から抜けられ
            // なくなる」に対応。チップ自体の.on表示だけでは絞り込み中であることの手がかりが弱く、
            // 解除手段も「もう一度同じチップを探してタップ」しかなかった。
            if let activeTag {
                HStack(spacing: 8) {
                    Text("\(activeTag) ✕").kyonoFont(.bold700, size: 14).foregroundColor(cc.onText)
                        .padding(.horizontal, 16 * zoom).padding(.vertical, 10 * zoom)
                        .background(Capsule().fill(cc.onBg))
                        .overlay(Capsule().stroke(cc.onBorder, lineWidth: 2 * zoom))
                        .onTapGesture { self.activeTag = nil; searchLimit = 24 }
                        .accessibilityIdentifier("searchFilterPill")
                    Text("タップで解除").kyonoFont(.bold700, size: 13).foregroundColor(colors.sub)
                }
            }
            // TASK-C2-2026-08-01-build15-subtraction9.md #2: 年セレクタを削除(5視点監査
            // ①②③④が独立に指摘。「肩こりの動画がほしい」目的に対し「何年の動画か」で絞る
            // 発想は想定ユーザーに伝わらず、このアプリで唯一のプルダウンUIだった。本人ガイド
            // ライン>Web1:1移植の既定によりネイティブのみ削除・Web正本は据え置き)。
            HStack {
                Spacer()
                // UI/UXパリティ監査GO-10(2026-07-28): app-search.js:61 `${hits.length}本`の1:1移植。
                // 「件見つかりました」は動詞つきの事務的な文体で、Web版のカジュアルな単位表現とは別物だった。
                Text(verbatim: "\(hits.count)本").kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)
            }
            // C2修正(2026-07-29): 以前はここに独立したScrollViewがあり、結果一覧だけを
            // 別スクロール領域に囲っていた。外側を1枚のScrollViewにしたのでここは
            // 素のLazyVStackのままでよい(遅延読み込み自体はScrollViewが親にあれば機能する)。
            LazyVStack(alignment: .leading, spacing: 6) {
                // index.html:958 #vlistの0件時分岐(app-search.js:68)の1:1移植。TASK-C2-2026-07-29-
                // ux-audit-G.md G4: 0件のときにここが完全に空になり、下のReqBox(文言のみ)しか
                // 出ないため「検索結果欄そのものが沈黙する」ように見えていた。
                if hits.isEmpty {
                    VStack(spacing: 6) {
                        KyonoCharaImage(name: "chara-2").frame(width: 84, height: 84)
                        Text("この条件のストレッチはまだないみたい…")
                            .kyonoFont(.bold700, size: 15).foregroundColor(colors.sub)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 20)
                    .background(RoundedRectangle(cornerRadius: 16).fill(colors.card))
                    .accessibilityIdentifier("searchEmptyCard")
                } else {
                    ForEach(Array(hits.prefix(searchLimit)), id: \.id) { v in VideoRow(v: v, openUrl: openUrl) }
                        .accessibilityIdentifier("searchResultRow")
                }
                if hits.count > searchLimit {
                    // index.html:71 app-search.js drawResults()「さらに表示（あと${残り}本）」の1:1移植。
                    KyonoGhostButton("さらに表示（あと\(hits.count - searchLimit)本）") { searchLimit += 48 }
                        .accessibilityIdentifier("searchMoreBtn")
                }
                // 動画を探す画面のリクエスト導線欠落修正タスク(TASK-C2-2026-07-26-search-request-box.md):
                // index.html:960-963 #reqBox(app-search.js drawResults()のreqMsg/reqBtn組み立て・
                // index.html copyMailAddr()の1:1移植)。検索ロジック自体は変更していない。
                let kwText = [query.trimmingCharacters(in: .whitespacesAndNewlines), activeTag].compactMap { $0?.isEmpty == false ? $0 : nil }.joined(separator: " ")
                ReqBox(shown: !hits.isEmpty, kwText: kwText)
            }
        }
        // UI/UXパリティ監査GO-9・G6(2026-07-28): index.html:82 body{padding:20px 18px 180px}の
        // 1:1移植。この画面だけ全辺16ptだった欠落を、共通のkyonoScreenPadding()へ統一する。
        .kyonoScreenPadding()
        }
        .background(KyonoBackgroundColor().ignoresSafeArea())
    }
}

// index.html:961 reqMsg/reqBtnの表示切り替え。offlineCat分岐(Web版はCATALOG未ロード時の対応)は
// ネイティブではcatalog.jsonをバンドルリソースとして常に同期ロードするため該当せず、常時表示でよい。
private struct ReqBox: View {
    @Environment(\.kyonoColors) private var colors
    // GO-G11(5視点ワンループ): 「コピーしました」等の一時メッセージの自動消滅時間を、既存の
    // bigtext環境値に連動させる(ONのときは読み切れるよう4秒へ延ばす)。
    @Environment(\.kyonoBigText) private var bigText
    let shown: Bool
    let kwText: String
    @State private var copied = false

    var body: some View {
        KyonoGradientCard(gradient: .warm) {
            Text(shown
                ? "やりたいストレッチが見つからない？\nオガトレに直接リクエストを送れます"
                : "ごめんなさい まだなかったみたい\nリクエストを送ってもらえたら動画づくりの参考にします")
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
            // UX13案・案8(2026-07-30): ボタン用途の残存絵文字をCanvasアイコンへ(.clipboardPaste)。
            HStack(spacing: 4) {
                if !copied {
                    KyonoIconGlyph(icon: .clipboardPaste, fill: .clear, accent: colors.tealInk).frame(width: 14, height: 14)
                }
                Text(copied ? "コピーしました" : "アドレスをコピー")
                    .kyonoFont(.black900, size: 12).foregroundColor(colors.tealInk)
            }
                .frame(maxWidth: .infinity, alignment: .center)
                .onTapGesture {
                    UIPasteboard.general.string = "kyou-no@ogatore.jp"
                    copied = true
                    Task {
                        try? await Task.sleep(nanoseconds: bigText ? 4_000_000_000 : 2_000_000_000)
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

// index.html:328-336相当のサムネイル枠。thumbが"assets/pl-*.jpg"のときはネイティブに同梱した
// ローカル画像(PlaylistArt/pl-*.jpg)を、https://のときはKyonoAsyncImageで都度取得する
// (TASK-C2-2026-07-28: index.html:3498-3521 PLAYLISTSはグループによって同梱ローカル画像と
// 既存i.ytimg.comサムネURLが混在しているため、両対応にする。KyonoCharaImageと同じ
// PBXFileSystemSynchronizedRootGroup前提でsubdirectory指定なしで探す)。
private struct PlaylistThumb: View {
    let thumb: String?

    var body: some View {
        if let thumb {
            if thumb.hasPrefix("http") {
                KyonoAsyncImage(url: thumb)
            } else if let name = thumb.split(separator: "/").last.map({ String($0.dropLast(4)) }),
                      let url = Bundle.main.url(forResource: name, withExtension: "jpg"),
                      let uiImage = UIImage(contentsOfFile: url.path) {
                Image(uiImage: uiImage).resizable().aspectRatio(contentMode: .fill)
            }
        }
    }
}

// index.html:3517 renderPlaylists()の1:1移植。プレイリスト1件=角丸サムネ+タイトル+説明、
// タップでYouTubeプレイリストを開く(タブでの動画個別再生と違い、連続再生キューがそのまま続く)。
private struct PlaylistRow: View {
    @Environment(\.kyonoColors) private var colors
    let item: PlaylistItem
    let openUrl: (String) -> Void

    var body: some View {
        Button {
            openUrl("https://www.youtube.com/playlist?list=\(item.id)")
        } label: {
            HStack(alignment: .top, spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12).fill(colors.line)
                    PlaylistThumb(thumb: item.thumb).clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .frame(width: 112, height: 112 * 9 / 16)
                VStack(alignment: .leading, spacing: 2) {
                    Text(item.title).kyonoFont(.bold700, size: 15).foregroundColor(colors.ink).lineLimit(2)
                    Text(item.desc).kyonoFont(.bold700, size: 14).foregroundColor(colors.sub).lineLimit(1)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .padding(10)
        .background(RoundedRectangle(cornerRadius: 16).fill(colors.card))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(colors.borderStrong, lineWidth: 1.5))
        .accessibilityElement(children: .combine)
        .buttonStyle(.plain)
    }
}

// 「再生リスト」タブ本体。TASK-C2-2026-07-28-search-playlists-and-fullwidth-space.md §1の判断:
// index.html:3498-3521 PLAYLISTS(手動キュレーション16本・3グループ)を主役として上に置き、
// 「全部の動画を見たい」需要向けの平坦一覧(catalog.json 454本)は残しつつ下に従える。
struct CatalogListView: View {
    let store: RecordStore
    let openUrl: (String) -> Void
    let onBack: () -> Void

    private let playlistGroups = PlaylistLoader.shared
    private let catalog = CatalogLoader.shared.sorted { a, b in a.y != b.y ? a.y > b.y : a.t < b.t }
    private var themeSetting: String { store.get("theme", default: "light") }

    var body: some View {
        KyonoTheme(themeSetting: themeSetting, bigText: store.get("bigtext", default: true)) {
            CatalogListContentView(playlistGroups: playlistGroups, catalog: catalog, onBack: onBack, openUrl: openUrl)
        }
        // iOSスワイプもどり導線追加タスク(EdgeSwipeBack.swift参照): アコーディオン状態を持たない画面。
        .edgeSwipeBack(onBack: onBack)
    }
}

private struct CatalogListContentView: View {
    @Environment(\.kyonoColors) private var colors
    let playlistGroups: [PlaylistGroup]
    let catalog: [CatalogVideo]
    let onBack: () -> Void
    let openUrl: (String) -> Void
    // GO-G14(5視点ワンループ): ホームと同じenvBanner(オフライン案内)をこの画面にも出す。
    @StateObject private var networkMonitor = NetworkMonitor()

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("再生リスト").kyonoFont(.black900, size: 16).foregroundColor(colors.ink)
            // GO-G14(5視点ワンループ): 文言・スタイルはホーム版(HomeView.swift)と完全に同じにする。
            if networkMonitor.isOffline {
                Text("いま電波がないみたい 動画を見るには電波が必要だよ（「きょうやった！」の記録はつけられるよ）")
                    .kyonoFont(.bold700, size: 15).foregroundColor(colors.ink).lineSpacing(9)
                    .padding(.horizontal, 12).padding(.vertical, 10)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        RoundedRectangle(cornerRadius: 14).fill(colors.yellowSoft)
                            .overlay(RoundedRectangle(cornerRadius: 14).stroke(colors.yellow, lineWidth: 1.5))
                    )
            }
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 6) {
                    ForEach(playlistGroups, id: \.group) { gr in
                        Text(gr.group).kyonoFont(.black900, size: 14).foregroundColor(colors.ink)
                            .padding(.top, 10).padding(.bottom, 4)
                        ForEach(gr.items, id: \.id) { p in PlaylistRow(item: p, openUrl: openUrl) }
                    }
                    Text("\(catalog.count)本の動画すべてから探す").kyonoFont(.black900, size: 14).foregroundColor(colors.ink)
                        .padding(.top, 18).padding(.bottom, 4)
                    ForEach(catalog, id: \.id) { v in VideoRow(v: v, openUrl: openUrl) }
                    // index.html:941 .hint(固定表示にするとFAB2段(右下)と重なるバグの再発になる=
                    // とどくメーターの5番目ボタンで既発見済みの教訓と同種のため、リスト末尾項目にする)
                    Text("タップするとYouTubeで開きます！テレビで流すのもおすすめ")
                        .kyonoFont(.bold700, size: 13).foregroundColor(colors.sub)
                        .padding(.top, 8).padding(.bottom, 90)
                }
            }
        }
        .padding(16)
        .background(KyonoBackgroundColor().ignoresSafeArea())
    }
}
