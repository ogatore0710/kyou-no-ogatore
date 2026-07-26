//
//  KyonoTourMockups.swift
//  KyouNoOgatore
//
//  見た目パリティ第2弾(TASK-C2-2026-07-26-visual-parity-round2.md §1・最優先): 使い方ツアーの
//  各スライドが「実際のアプリ画面のミニチュア再現」であるWeb版(index.html:4117-4143 OB_TOUR_SLIDES
//  のv フィールド)を1:1移植する(Android版KyonoTourMockups.ktと同一ロジック)。タップ不可の
//  静止モックアップ(Web版の「gmock」)であり、ここで描く内容はロジック・状態には一切影響しない。

import SwiftUI

struct KyonoTourMockup: View {
    @Environment(\.kyonoColors) private var colors
    let slideIndex: Int

    var body: some View {
        switch slideIndex {
        // 1) 📺まいにち1本: 「きょうの1本」カードのミニチュア(動画サムネイル+タイトル+案内文)
        case 0:
            KyonoCard {
                Text("きょうの1本").font(.kyono(.black900, size: 15)).foregroundColor(colors.ink)
                Spacer().frame(height: 8)
                HStack(alignment: .center) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 8).fill(colors.line)
                        KyonoAsyncImage(url: youtubeThumbUrl("Re5FPU5_37g"))
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                    .frame(width: 120, height: 120 * 9 / 16)
                    Spacer().frame(width: 10)
                    VStack(alignment: .leading, spacing: 4) {
                        Text("開脚できるようになる2週間ストレッチ").font(.kyono(.bold700, size: 13)).foregroundColor(colors.ink)
                        Text("▶ タップでYouTubeがひらきます").font(.kyono(.bold700, size: 12)).foregroundColor(colors.sub)
                    }
                }
            }
        // 2) ✅きょうやった！: 「続けた日数」カードのミニチュア(大きい数字「8日目」+done-btn)
        case 1:
            KyonoCard {
                VStack(spacing: 2) {
                    Text("続けた日数（通算）").font(.kyono(.black900, size: 15)).foregroundColor(colors.ink)
                    HStack(alignment: .bottom, spacing: 0) {
                        Text("8").font(.kyono(.black900, size: 38)).foregroundColor(colors.pink)
                        Text("日目").font(.kyono(.black900, size: 16)).foregroundColor(colors.ink).padding(.bottom, 6)
                    }
                    Spacer().frame(height: 6)
                    // index.html:380-381 .done-btn(teal-strong塗り+立体シャドウ。gmockのため押下は無し)
                    ZStack {
                        RoundedRectangle(cornerRadius: 18).fill(Color(hex: 0x1E8A7D)).offset(y: 4)
                        RoundedRectangle(cornerRadius: 18).fill(colors.tealStrong)
                        Text("きょうやった！").font(.kyono(.black900, size: 16)).foregroundColor(.white).padding(.vertical, 14)
                    }
                }
                .frame(maxWidth: .infinity)
            }
        // 3) 記録カードをつくる: card-sample.pngを180x180角丸で中央表示
        case 2:
            HStack {
                Spacer()
                KyonoTourDrawable(name: "card-sample").frame(width: 180, height: 180).clipShape(RoundedRectangle(cornerRadius: 20))
                Spacer()
            }
        // 4) ためると図鑑がうまる: card-sample.pngの隣に「？」の点線枠3つ
        case 3:
            KyonoCard {
                VStack {
                    Text("カード図鑑").font(.kyono(.black900, size: 15)).foregroundColor(colors.ink)
                    Spacer().frame(height: 8)
                    HStack(spacing: 8) {
                        KyonoTourDrawable(name: "card-sample").frame(width: 52, height: 52).clipShape(RoundedRectangle(cornerRadius: 10))
                        ForEach(0..<3, id: \.self) { _ in
                            RoundedRectangle(cornerRadius: 10).stroke(colors.line, lineWidth: 1.5)
                                .frame(width: 52, height: 52)
                                .overlay(Text("？").font(.kyono(.black900, size: 16)).foregroundColor(colors.sub))
                        }
                    }
                }
                .frame(maxWidth: .infinity)
            }
        // 5) 悩みは相談室で質問: 実際のチャット吹き出し2つ(ユーザー発言→オガトレくんの返答、アバター付き)
        case 4:
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Spacer()
                    Text("肩こりがつらい").font(.kyono(.bold700, size: 15)).foregroundColor(colors.ink)
                        .padding(.horizontal, 14).padding(.vertical, 10)
                        .background(UnevenRoundedRectangle(topLeadingRadius: 16, bottomLeadingRadius: 16, bottomTrailingRadius: 6, topTrailingRadius: 16).fill(colors.yellowSoft))
                }
                HStack(alignment: .bottom) {
                    KyonoCharaImage(name: "chara-hitokoto").frame(width: 34, height: 34)
                    let shape = UnevenRoundedRectangle(topLeadingRadius: 16, bottomLeadingRadius: 6, bottomTrailingRadius: 16, topTrailingRadius: 16)
                    Text("それはつらいね…！まずはこの1本からやってみよう😊").font(.kyono(.bold700, size: 15)).foregroundColor(colors.ink)
                        .padding(.horizontal, 14).padding(.vertical, 10)
                        .background(shape.fill(colors.card))
                        .overlay(shape.stroke(colors.line, lineWidth: 1.5))
                }
            }
        // 6) オガトレ通信をのぞく: 丸い写真アイコン+説明
        case 5:
            HStack {
                Spacer()
                KyonoTourDrawable(name: "obu-fab-photo").frame(width: 56, height: 56).clipShape(Circle())
                    .overlay(Circle().stroke(colors.yellow, lineWidth: 3))
                Spacer().frame(width: 12)
                VStack(alignment: .leading, spacing: 2) {
                    Text("右下のこの写真ボタン").font(.kyono(.black900, size: 13)).foregroundColor(colors.ink)
                    Text("ひとこと・写真・ラジオ📻").font(.kyono(.bold700, size: 13)).foregroundColor(colors.sub)
                }
                Spacer()
            }
        // 7) マイ記録でふりかえる: カレンダーのミニチュア(5個の丸、3個が塗りつぶし=やった日)
        case 6:
            KyonoCard {
                VStack {
                    Text("カレンダー").font(.kyono(.black900, size: 15)).foregroundColor(colors.ink)
                    Spacer().frame(height: 8)
                    HStack(spacing: 7) {
                        ForEach(1...5, id: \.self) { n in
                            let done = n <= 3
                            Circle().fill(done ? colors.tealStrong : Color.clear)
                                .frame(width: 34, height: 34)
                                .overlay(Text("\(n)").font(.kyono(.black900, size: 14)).foregroundColor(done ? .white : colors.sub))
                        }
                    }
                }
                .frame(maxWidth: .infinity)
            }
        // 8) 忘れてもだいじょうぶ: シンプルな案内カード
        case 7:
            KyonoCard {
                Text("下の「使い方」タブに\nぜんぶ書いてあります")
                    .font(.kyono(.bold700, size: 14)).foregroundColor(colors.ink).lineSpacing(10)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .multilineTextAlignment(.center)
            }
        default:
            EmptyView()
        }
    }
}

private struct KyonoTourDrawable: View {
    let name: String
    var body: some View {
        if let url = Bundle.main.url(forResource: name, withExtension: name == "obu-fab-photo" ? "jpg" : "png"),
           let uiImage = UIImage(contentsOfFile: url.path) {
            Image(uiImage: uiImage).resizable().aspectRatio(contentMode: .fill)
        }
    }
}
