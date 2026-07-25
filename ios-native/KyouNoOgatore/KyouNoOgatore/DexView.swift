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

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 8) {
                Button("◀ もどる", action: onBack)
                Text("図鑑").font(.title2.bold())
                Text("\(all.filter { $0.got }.count)/\(all.count)個 あつめました")
                DexSectionView(title: "記念日カード", items: status.toku)
                DexSectionView(title: "季節のカード", items: status.season)
                DexSectionView(title: "レアカード", items: status.rare)
                DexSectionView(title: "ノーマルカード", items: status.normal)
            }
            .padding(16)
        }
    }
}

private struct DexSectionView: View {
    let title: String
    let items: [DexItem]

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(title).font(.headline)
                Spacer()
                Text("\(items.filter { $0.got }.count)/\(items.count)")
            }
            LazyVGrid(columns: dexColumns, spacing: 8) {
                ForEach(Array(items.enumerated()), id: \.offset) { _, item in DexCellView(item: item) }
            }
        }
        .padding(.vertical, 8)
    }
}

private struct DexCellView: View {
    let item: DexItem

    var body: some View {
        VStack(spacing: 2) {
            ZStack {
                RoundedRectangle(cornerRadius: 10).fill(Color(red: 0.93, green: 0.92, blue: 0.89))
                if item.tier == "normal" {
                    if item.got, let nc = CardDataLoader.shared.NORMAL_CARDS.first(where: { $0.name == item.name }) {
                        Circle().fill(Color(hex: nc.main)).frame(width: 24, height: 24)
                    } else {
                        Text("？").font(.title)
                    }
                } else if let key = item.key, let uiImage = loadCardArt(key) {
                    Image(uiImage: uiImage)
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .colorMultiply(item.got ? .white : Color.black.opacity(0.55))
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                }
            }
            .aspectRatio(1, contentMode: .fit)
            Text(item.got ? item.name : "？？？").font(.system(size: 11)).multilineTextAlignment(.center)
            let sub = item.got ? item.flavor : item.hint
            if !sub.isEmpty {
                Text(sub).font(.system(size: 10)).foregroundColor(.gray).multilineTextAlignment(.center)
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

private extension Color {
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
