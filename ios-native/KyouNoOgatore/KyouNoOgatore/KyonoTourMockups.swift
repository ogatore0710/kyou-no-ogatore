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
//
//  TASK-C2-2026-08-04-build20-home-cards-and-tour-tiers.md T-A/T-B: スライド配列が「初回4枚/
//  再生7枚」の2構成に分かれ、同じ絵が異なるインデックスで出ることになったため、位置(index)では
//  なくTourMockKind(意味のある固定キー)でswitchするよう変更。これにより配列の並び替えで絵が
//  ズレる心配が構造的に無くなる(T-1の再発防止)。

import SwiftUI

struct KyonoTourMockup: View {
    @Environment(\.kyonoColors) private var colors
    let kind: TourMockKind

    var body: some View {
        switch kind {
        // T-A: まいにちやることは1つだけ。動画カード→黄色「きょうやった！」ボタン→記録カードの
        // 3コマ縦並び簡略図(alan5指定)。
        // TASK-C2-2026-08-04-build21-addendum.md Y-3: 本人がGPT生成イラストを`tour-map-illust.png`
        // として用意し次第、1ファイル追加だけで差し替わるフォールバック構造(KyonoFabの
        // photoResName方式と同じ考え方)。バンドルに無い間(今回のビルド時点)は現行モックのまま。
        case .map:
            if let url = Bundle.main.url(forResource: "tour-map-illust", withExtension: "png"),
               let uiImage = UIImage(contentsOfFile: url.path) {
                Image(uiImage: uiImage).resizable().scaledToFit()
            } else {
                VStack(spacing: 6) {
                    HStack(spacing: 8) {
                        RoundedRectangle(cornerRadius: 8).fill(colors.line).frame(width: 46, height: 46 * 9 / 16)
                        Text("きょうの1本").kyonoFont(.bold700, size: 13).foregroundColor(colors.ink)
                        Spacer()
                    }
                    .padding(8).background(RoundedRectangle(cornerRadius: 12).fill(colors.card))
                    Image(systemName: "arrow.down").foregroundColor(colors.sub).font(.system(size: 14))
                    Text("きょうやった！").kyonoFont(.black900, size: 14).foregroundColor(.white)
                        .frame(maxWidth: .infinity).padding(.vertical, 8)
                        .background(RoundedRectangle(cornerRadius: 12).fill(colors.btnPrimaryBg))
                    Image(systemName: "arrow.down").foregroundColor(colors.sub).font(.system(size: 14))
                    HStack {
                        Spacer()
                        KyonoCharaImage(name: "card-sample").frame(width: 60, height: 60)
                        Spacer()
                    }
                }
            }
        // 復活枚1) まいにち1本、動画をやる: 「きょうの1本」カードのミニチュア(動画サムネイル+タイトル+案内文)
        case .videoDaily:
            KyonoCard {
                Text("きょうの1本").kyonoFont(.black900, size: 15).foregroundColor(colors.ink)
                Spacer().frame(height: 8)
                HStack(alignment: .center) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 8).fill(colors.line)
                        KyonoAsyncImage(url: youtubeThumbUrl("Re5FPU5_37g")).clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                    .frame(width: 120, height: 120 * 9 / 16)
                    Spacer().frame(width: 10)
                    VStack(alignment: .leading, spacing: 4) {
                        Text("開脚できるようになる2週間ストレッチ").kyonoFont(.bold700, size: 13).foregroundColor(colors.ink)
                        Text("▶ タップでYouTubeがひらきます").kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)
                    }
                    Spacer()
                }
            }
        // 復活枚2) おわったら「きょうやった！」: 「続けた日数」カードのミニチュア(大きい数字「8日目」+done-btn)
        case .todayDone:
            KyonoCard {
                VStack(alignment: .center, spacing: 2) {
                    Text("続けた日数（通算）").kyonoFont(.black900, size: 15).foregroundColor(colors.ink)
                    HStack(alignment: .bottom, spacing: 2) {
                        Text("8").kyonoFont(.black900, size: 38).foregroundColor(colors.pink)
                        Text("日目").kyonoFont(.black900, size: 16).foregroundColor(colors.ink).padding(.bottom, 6)
                    }
                    Spacer().frame(height: 6)
                    Text("きょうやった！").kyonoFont(.black900, size: 16).foregroundColor(.white)
                        .frame(maxWidth: .infinity).padding(.vertical, 14)
                        .background(RoundedRectangle(cornerRadius: 18).fill(colors.tealStrong))
                }
                .frame(maxWidth: .infinity)
            }
        // 復活枚3) ためると図鑑がうまる: card-sample.pngの隣に「？」の点線枠3つ
        case .cardDex:
            KyonoCard {
                VStack(alignment: .center, spacing: 8) {
                    Text("カード図鑑").kyonoFont(.black900, size: 15).foregroundColor(colors.ink)
                    HStack(spacing: 8) {
                        KyonoCharaImage(name: "card-sample").frame(width: 52, height: 52)
                        ForEach(0..<3, id: \.self) { _ in
                            RoundedRectangle(cornerRadius: 10).stroke(colors.line, lineWidth: 1.5)
                                .frame(width: 52, height: 52)
                                .overlay(Text("？").kyonoFont(.black900, size: 16).foregroundColor(colors.sub))
                        }
                    }
                }
                .frame(maxWidth: .infinity)
            }
        // 予告1) 悩みは相談室で質問: 実際のチャット吹き出し2つ(ユーザー発言→オガトレくんの返答、アバター付き)
        case .soudan:
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
        // 予告2) オガトレ通信をのぞく: 丸い写真アイコン+説明
        case .obu:
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
        // 予告3) マイ記録でふりかえる: カレンダーのミニチュア(5個の丸、3個が塗りつぶし=やった日)
        case .myRecord:
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
