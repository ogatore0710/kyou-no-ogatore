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
import CoreGraphics
import ImageIO

struct BragView: View {
    let store: RecordStore
    let onBack: () -> Void

    private let catalog = CatalogLoader.shared
    @State private var daysText: String
    @State private var query = ""
    @State private var picked: CatalogVideo?
    @State private var cardImage: UIImage?

    private let streakCount: Int

    init(store: RecordStore, onBack: @escaping () -> Void) {
        self.store = store
        self.onBack = onBack
        let streak = RecordLogic.loadStreak(store)
        streakCount = streak.count
        // index.html:2724-2730 openBrag()の1:1移植: 「つづいている日数」はcount(いま連続)から
        // プリフィル(total=通算ではない)。全画面完全性監査タスク #brag で、Web版と異なりtotalから
        // プリフィルしていた既存の不一致を修正。
        _daysText = State(initialValue: String(streak.count > 0 ? streak.count : 1))
    }

    private var hits: [CatalogVideo] {
        query.trimmingCharacters(in: .whitespaces).isEmpty ? [] : Array(searchCatalog(catalog, query: query, activeTag: nil).prefix(20))
    }

    private var themeSetting: String { store.get("theme", default: "auto") }

    var body: some View {
        KyonoTheme(themeSetting: themeSetting, bigText: store.get("bigtext", default: true)) {
            BragContentView(
                store: store, daysText: $daysText, query: $query, picked: $picked, cardImage: $cardImage,
                hits: hits, streakCount: streakCount, onBack: onBack
            )
        }
        // iOSスワイプもどり導線追加タスク(EdgeSwipeBack.swift参照): アコーディオン状態を持たない画面。
        .edgeSwipeBack(onBack: onBack)
    }
}

// TASK-C2-2026-07-27-brag-card-thumbnail.md: index.html:2765-2774 loadBragThumb()の1:1移植。
// 3秒でタイムアウトし、取れなければnil扱いで先へ進む(オフライン・遅い回線でカード生成が
// 固まらないようにするための上限。取得失敗・デコード失敗も同じくnil扱い)。CardCoreは決定的
// ロジックの1:1移植先のため、ネットワークI/Oはアプリ層のここで行い、結果のCGImageだけを渡す。
private func fetchBragThumbnail(videoId: String) async -> CGImage? {
    guard let url = URL(string: youtubeThumbUrl(videoId)) else { return nil }
    let config = URLSessionConfiguration.ephemeral
    config.timeoutIntervalForRequest = 3
    guard let (data, _) = try? await URLSession(configuration: config).data(from: url) else { return nil }
    guard let source = CGImageSourceCreateWithData(data as CFData, nil) else { return nil }
    return CGImageSourceCreateImageAtIndex(source, 0, nil)
}

private struct BragContentView: View {
    @Environment(\.kyonoColors) private var colors
    let store: RecordStore
    @Binding var daysText: String
    @Binding var query: String
    @Binding var picked: CatalogVideo?
    @Binding var cardImage: UIImage?
    @State private var makingCard = false
    let hits: [CatalogVideo]
    let streakCount: Int
    let onBack: () -> Void

    var body: some View {
        // UI/UXパリティ監査2巡目A5(2026-07-29): この画面だけ他の全画面(Home/MyRecord/Search/
        // Guide/Voices/Dex/Settings/Obu等)と違いページ全体のScrollViewが無く、内側の検索結果
        // 一覧(240pt)だけがスクロール可能だった欠落。Web版の#bragはページ全体がウィンドウ
        // スクロールする通常のドキュメントフローのため、小さい画面やキーボード表示中に
        // ボタン・注意書きへ到達できなくなるリスクがあった(Android版は既にverticalScroll済み)。
        ScrollView {
        VStack(alignment: .leading, spacing: 8) {
            KyonoLineButton("◀ もどる", action: onBack)
            KyonoCard {
                KyonoSectionHeader(icon: .heart, title: "じまんカードをつくる", fill: colors.pinkSoft, accent: colors.pink)
                Spacer().frame(height: 8)
                Text("続けてる日数と すきな1本を 1枚のカードに\nできたカードは保存やSNS投稿ができます")
                    .kyonoFont(.bold700, size: 14).foregroundColor(colors.sub)

                // index.html:340 #brag .lbl
                Spacer().frame(height: 14)
                Text("つづいている日数").kyonoFont(.black900, size: 13).foregroundColor(colors.sub)
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
                // 全画面完全性監査タスク(TASK-C2-2026-07-26-full-completeness-audit.md #brag):
                // index.html:857 #bragDaysNoteの1:1移植。
                Spacer().frame(height: 4)
                Text(streakCount > 0 ? "いまの記録から入れておきました（数字はすきにかえてOK）" : "まだ記録がなくてもだいじょうぶ すきな数字でためせます")
                    .kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)

                Spacer().frame(height: 14)
                Text("すきな1本をさがす").kyonoFont(.black900, size: 13).foregroundColor(colors.sub)
                Spacer().frame(height: 6)
                TextField("例: 肩甲骨／朝／開脚", text: $query)
                    .padding(.horizontal, 14).padding(.vertical, 10)
                    .background(RoundedRectangle(cornerRadius: 16).fill(colors.card))
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(colors.line, lineWidth: 2))

                // UX13案・案11(2026-07-30): A5でページ全体がScrollViewになった結果、検索結果だけが
                // 内側ScrollView(maxHeight:240)の入れ子になっていた(ページを撫でるつもりが内側だけ
                // 動く/その逆というスクロール衝突)。検索タブ(SearchView.swift)と同じく、内側
                // ScrollViewをやめてページフローへ直接並べる(既にhitsは20件で頭打ちのため
                // 「さらに表示」ボタンは不要・引き算の範囲)。
                VStack(alignment: .leading, spacing: 6) {
                    ForEach(hits, id: \.id) { v in
                        Text(v.t)
                            .kyonoFont(.bold700, size: 14).foregroundColor(colors.ink)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(12)
                            .background(RoundedRectangle(cornerRadius: 12).fill(colors.bg))
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(colors.line, lineWidth: 1.5))
                            .contentShape(Rectangle())
                            .onTapGesture { picked = v }
                    }
                }

                Spacer().frame(height: 8)
                Text("えらんだ1本").kyonoFont(.black900, size: 13).foregroundColor(colors.sub)
                Text(picked.map { "選択中: \($0.t)" } ?? "まだえらんでいません 上の検索からどうぞ")
                    .kyonoFont(.bold700, size: 14).foregroundColor(colors.sub)

                Spacer().frame(height: 16)
                KyonoPrimaryButton("カードをつくる", enabled: !makingCard) {
                    // TASK-C2-2026-07-27-brag-card-thumbnail.md: index.html:2765-2774
                    // loadBragThumb()の1:1移植。サムネイル取得はネットワークI/Oのためasync化し、
                    // 取得中もUIをブロックしない(3秒タイムアウトで先へ進むのはfetchBragThumbnail側)。
                    makingCard = true
                    Task {
                        var thumbnail: CGImage?
                        if let picked { thumbnail = await fetchBragThumbnail(videoId: picked.id) }
                        let days = BragCardRenderer.clampDays(Int(daysText) ?? 1)
                        let ds = RecordLogic.todayStr(now: Date())
                        let dateIdx = CardLottery.dateIdx(ds)
                        let data = CardDataLoader.shared
                        let theme = data.CARD_THEMES[dateIdx % data.CARD_THEMES.count]
                        let resolved = ResolvedTheme(name: theme.name, bg: theme.bg, main: theme.main, deco: theme.deco)
                        let png = BragCardRenderer.render(ds: ds, days: days, theme: resolved, favoriteTitle: picked?.t, thumbnail: thumbnail)
                        cardImage = UIImage(data: png)
                        // GO-G7(5視点ワンループ): カード獲得(生成完了)に「きょうやった！」と同じ軽いハプティクスを追加。
                        UIImpactFeedbackGenerator(style: .light).impactOccurred()
                        makingCard = false
                    }
                }
                Spacer().frame(height: 8)
                Text("えらんだ1本は カードにサムネイル画像で入ります").kyonoFont(.bold700, size: 13).foregroundColor(colors.sub)
                // GO-G15(5視点ワンループ): 記録系画面に保存先の事実だけを目立たない位置に一言添える。
                // 数字・達成率は書かない(デザイン原則どおり)。
                Spacer().frame(height: 6)
                Text("この記録はこの端末に保存されるよ").kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)
            }
        }
        .padding(16)
        }
        // UX13案・案13(2026-07-30): 数字キーボード(.numberPad)はiOS標準の「完了」ボタンが無く、
        // キーボード表示中は指を離しても閉じる手段がなかった。ページをドラッグしはじめた時点で
        // 閉じるようにする(.interactivelyだと指の動きに追従する分ゆっくりのドラッグが必要になり、
        // 「閉じられない」という体感が残りうるため、確実さを優先して.immediatelyにした)。
        .scrollDismissesKeyboard(.immediately)
        .background(KyonoBackgroundColor().ignoresSafeArea())
        // GO-G5(5視点ワンループ): ObuPreviewPopupの背景タップで閉じるパターンをこのカードモーダルにも
        // 適用(以前は.sheet()でスワイプでしか閉じられなかった)。
        .overlay {
            KyonoCardModalOverlay(isPresented: cardImage != nil, onClose: { cardImage = nil }) {
                if let cardImage {
                    // TestFlight実機フィードバックD3(2026-07-29): index.html:1197-1199
                    // (btn-primary「保存・シェアする」→btn-line「とじる」の縦積み・各100%幅)の
                    // 1:1移植。以前はHStackで横並びにしていたため、幅を分け合った「保存・シェアする」
                    // (フォントサイズ20)だけが2行に折り返し、1行の「とじる」と高さ・上端が揃わなかった。
                    // 絵文字(Web版📤)は本人の新ガイドライン(ボタン・タブ・見出しにはOS絵文字を
                    // 使わない・アイコンはデザイン生成のものを使う)により持ち込まない。
                    VStack(spacing: 12) {
                        Image(uiImage: cardImage).resizable().scaledToFit()
                        KyonoPrimaryButton("保存・シェアする") {
                            let days = BragCardRenderer.clampDays(Int(daysText) ?? 1)
                            ShareImage.share(uiImage: cardImage, text: "#きょうのオガトレ \(days)日つづいてる！")
                        }
                        KyonoLineButton("とじる") { self.cardImage = nil }
                    }
                }
            }
        }
    }
}
