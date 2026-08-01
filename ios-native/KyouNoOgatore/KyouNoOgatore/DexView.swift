//
//  DexView.swift
//  KyouNoOgatore
//
//  ネイティブ移植 Step 7a(マスタープラン§6 Step 7a・図鑑UI): index.html renderDex()の1:1移植
//  (Android版DexScreen.ktと同一ロジック)。ロック/アンロック判定はDexLogic.getDexStatus
//  (Step4のCardLottery呼び出しのみ)を呼ぶだけで、このファイルは4段(記念日/季節/レア/ノーマル)の
//  グリッド表示だけを持つ。
//
//  SwiftUIのLazyVGridはScrollView内に入れてよい(マスタープラン§1-4の「LazyVerticalGridを
//  verticalScroll内に入れない」はJetpack Compose特有の無限高さ制約クラッシュの回避策であり、
//  SwiftUIのLazyVGridには同種の制約が無いため、Android版のColumn+Row手組みは踏襲しない)。
//
//  カード画像(assets/cards/*.webp)はiOS用にPNG変換のうえCardArt/フォルダ配下に配置(ファイル名=カードkey)。
//  ただしPBXFileSystemSynchronizedRootGroup(Xcode26形式)はビルド時にサブフォルダをバンドルルートへ
//  フラット化するため(実測確認済み)、loadCardArt()はsubdirectory指定なしで探す。
//  ロック中はCSSアルファマスクのシルエット効果の代わりに、同じ画像を暗くティントして表示する簡略版
//  (Android版と同じ判断)。
//
//  ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
//  Phase 3: index.html:225-241 .dex-box/.dex-sec/.dex-seccount/.dex-thumb/.dex-name/.dex-hintの1:1移植。

import SwiftUI
import RecordCore
import CardCore

private let dexColumns = [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())]

struct DexView: View {
    let store: RecordStore
    let onBack: () -> Void

    private let status: DexStatus

    init(store: RecordStore, onBack: @escaping () -> Void) {
        self.store = store
        self.onBack = onBack
        let streak = RecordLogic.loadStreak(store)
        let existing: [String: Int] = store.get("rotAssign", default: [:])
        let rot = CardLottery.ensureRotAssign(dates: streak.dates, total: streak.total, existing: existing)
        if existing.isEmpty && !rot.isEmpty { store.set("rotAssign", rot) }
        self.status = DexLogic.getDexStatus(dates: streak.dates, total: streak.total, rotAssign: rot)
    }

    private var all: [DexItem] { status.toku + status.season + status.rare + status.normal }
    private var themeSetting: String { store.get("theme", default: "auto") }

    var body: some View {
        KyonoTheme(themeSetting: themeSetting, bigText: store.get("bigtext", default: true)) {
            DexContentView(status: status, all: all, onBack: onBack)
        }
        // iOSスワイプもどり導線追加タスク(EdgeSwipeBack.swift参照): アコーディオン状態を持たない画面。
        .edgeSwipeBack(onBack: onBack)
    }
}

private struct DexContentView: View {
    @Environment(\.kyonoColors) private var colors
    let status: DexStatus
    let all: [DexItem]
    let onBack: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 4) {
                KyonoLineButton("◀ もどる", action: onBack)
                KyonoSectionHeader(icon: .dexBook, title: "図鑑", fill: colors.tealSoft)
                Text("\(all.filter { $0.got }.count)/\(all.count)個 あつめました").kyonoFont(.bold700, size: 13).foregroundColor(colors.sub)
                DexSectionView(title: "記念日カード", items: status.toku)
                DexSectionView(title: "季節のカード", items: status.season)
                DexSectionView(title: "レアカード", items: status.rare)
                DexSectionView(title: "ノーマルカード", items: status.normal)
                // GO-G15(5視点ワンループ): 記録系画面に保存先の事実だけを目立たない位置に一言添える。
                // 数字・達成率は書かない(デザイン原則どおり)。
                Spacer().frame(height: 12)
                Text("この記録はこの端末に保存されるよ").kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)
            }
            .padding(16)
        }
        .background(KyonoBackgroundColor().ignoresSafeArea())
    }
}

private struct DexSectionView: View {
    @Environment(\.kyonoColors) private var colors
    let title: String
    let items: [DexItem]

    var body: some View {
        KyonoCard {
            HStack {
                Text(title).kyonoFont(.black900, size: 14).foregroundColor(colors.ink)
                Spacer()
                // index.html:233 .dex-seccount(bg丸ピル)
                Text("\(items.filter { $0.got }.count)/\(items.count)")
                    .kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)
                    .padding(.horizontal, 10).padding(.vertical, 2)
                    .background(Capsule().fill(colors.bg))
            }
            Spacer().frame(height: 8)
            LazyVGrid(columns: dexColumns, spacing: 8) {
                ForEach(Array(items.enumerated()), id: \.offset) { _, item in DexCellView(item: item) }
            }
        }
        .padding(.vertical, 6)
    }
}

private struct DexCellView: View {
    @Environment(\.kyonoColors) private var colors
    let item: DexItem

    var body: some View {
        VStack(spacing: 5) {
            ZStack {
                // index.html:236 .dex-thumb(bg/border/radius12)の1:1移植。
                RoundedRectangle(cornerRadius: 12).fill(colors.bg)
                RoundedRectangle(cornerRadius: 12).stroke(colors.line, lineWidth: 1.5)
                if item.tier == "normal" {
                    if item.got, let nc = CardDataLoader.shared.NORMAL_CARDS.first(where: { $0.name == item.name }) {
                        Circle().fill(Color(hex: nc.main)).frame(width: 24, height: 24)
                    } else {
                        Text("？").kyonoFont(.black900, size: 22).foregroundColor(colors.sub)
                    }
                } else if let key = item.key, let uiImage = loadCardArt(key) {
                    Image(uiImage: uiImage)
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .colorMultiply(item.got ? .white : Color.black.opacity(0.55))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
            .aspectRatio(1, contentMode: .fit)
            // index.html:241 .dex-name{font-size:11px;line-height:1.3}の1:1移植。
            // UI/UXパリティ監査2巡目A1(2026-07-29): 前回G2は検索チップのみに適用していた
            // カスタムフォント行送り超過補正をここにも展開する。
            Text(item.got ? item.name : "？？？").kyonoFont(.black900, size: 12).foregroundColor(colors.ink).multilineTextAlignment(.center).lineSpacing(3)
            let sub = item.got ? item.flavor : item.hint
            if !sub.isEmpty {
                // index.html:242 .dex-hint{font-size:10px;line-height:1.35}の1:1移植。
                Text(sub).kyonoFont(.bold700, size: 12).foregroundColor(colors.sub).multilineTextAlignment(.center).lineSpacing(4)
            }
        }
    }
}

// PBXFileSystemSynchronizedRootGroup(Xcode26形式)はCardArt/サブフォルダをバンドルのビルド時に
// フラット化してバンドルルート直下へ配置するため、subdirectory指定なしで探す(実測確認済み)。
private func loadCardArt(_ key: String) -> UIImage? {
    guard let url = Bundle.main.url(forResource: key, withExtension: "png") else { return nil }
    return UIImage(contentsOfFile: url.path)
}

extension Color {
    init(hex: String) {
        var s = hex.trimmingCharacters(in: CharacterSet(charactersIn: "#"))
        if s.count == 3 { s = s.map { "\($0)\($0)" }.joined() }
        var v: UInt64 = 0
        Scanner(string: s).scanHexInt64(&v)
        self.init(
            red: Double((v >> 16) & 0xFF) / 255,
            green: Double((v >> 8) & 0xFF) / 255,
            blue: Double(v & 0xFF) / 255
        )
    }
}
