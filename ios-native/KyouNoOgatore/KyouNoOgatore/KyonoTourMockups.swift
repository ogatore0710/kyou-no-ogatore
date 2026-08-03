//
//  KyonoTourMockups.swift
//  KyouNoOgatore
//
//  見た目パリティ第2弾(TASK-C2-2026-07-26-visual-parity-round2.md §1・最優先): 使い方ツアーの
//  各スライドが「実際のアプリ画面のミニチュア再現」であるWeb版(index.html:4117-4143 OB_TOUR_SLIDES
//  のv フィールド)を1:1移植する(Android版KyonoTourMockups.ktと同一ロジック)。タップ不可の
//  静止モックアップ(Web版の「gmock」)であり、ここで描く内容はロジック・状態には一切影響しない。
//
//  TASK-C2-2026-08-04-build19-tour-redesign.md T-1(実バグ修正): B-10でobTourSlidesを8→7枚に
//  詰めた際、このswitchのcase番号(8枚時代のcase 0〜7)を詰め忘れ、3枚目以降が1つ前の話題の絵に
//  なっていた(alan5が実描画で確認・報告)。T-2で3枚+締めへ再構成したのに合わせ、caseをゼロから
//  書き直す(以後、スライド文言の変更時は必ずこのswitchも同時に見直すこと・検収基準「見出し⇔絵の
//  一致」を新設)。

import SwiftUI

struct KyonoTourMockup: View {
    @Environment(\.kyonoColors) private var colors
    let slideIndex: Int

    var body: some View {
        switch slideIndex {
        // 1) 悩みは相談室で質問: 実際のチャット吹き出し2つ(ユーザー発言→オガトレくんの返答、アバター付き)
        case 0:
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Spacer()
                    Text("肩こりがつらい").kyonoFont(.bold700, size: 15).foregroundColor(colors.ink)
                        .padding(.horizontal, 14).padding(.vertical, 10)
                        .background(UnevenRoundedRectangle(topLeadingRadius: 16, bottomLeadingRadius: 16, bottomTrailingRadius: 6, topTrailingRadius: 16).fill(colors.yellowSoft))
                }
                HStack(alignment: .bottom) {
                    KyonoCharaImage(name: "chara-hitokoto").frame(width: 34, height: 34)
                    let shape = UnevenRoundedRectangle(topLeadingRadius: 16, bottomLeadingRadius: 6, bottomTrailingRadius: 16, topTrailingRadius: 16)
                    Text("それはつらいね…！まずはこの1本からやってみよう").kyonoFont(.bold700, size: 15).foregroundColor(colors.ink)
                        .padding(.horizontal, 14).padding(.vertical, 10)
                        .background(shape.fill(colors.card))
                        .overlay(shape.stroke(colors.line, lineWidth: 1.5))
                }
            }
        // 2) オガトレ通信をのぞく: 丸い写真アイコン+説明
        case 1:
            HStack {
                Spacer()
                KyonoTourDrawable(name: "obu-fab-photo").frame(width: 56, height: 56).clipShape(Circle())
                    .overlay(Circle().stroke(colors.yellow, lineWidth: 3))
                Spacer().frame(width: 12)
                VStack(alignment: .leading, spacing: 2) {
                    Text("右下のこの写真ボタン").kyonoFont(.black900, size: 13).foregroundColor(colors.ink)
                    Text("ひとこと・写真・ラジオ").kyonoFont(.bold700, size: 13).foregroundColor(colors.sub)
                }
                Spacer()
            }
        // 3) マイ記録でふりかえる: カレンダーのミニチュア(5個の丸、3個が塗りつぶし=やった日)
        case 2:
            KyonoCard {
                VStack {
                    Text("カレンダー").kyonoFont(.black900, size: 15).foregroundColor(colors.ink)
                    Spacer().frame(height: 8)
                    HStack(spacing: 7) {
                        ForEach(1...5, id: \.self) { n in
                            let done = n <= 3
                            Circle().fill(done ? colors.tealStrong : Color.clear)
                                .frame(width: 34, height: 34)
                                .overlay(Text("\(n)").kyonoFont(.black900, size: 14).foregroundColor(done ? .white : colors.sub))
                        }
                    }
                }
                .frame(maxWidth: .infinity)
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
