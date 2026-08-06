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
                        // TASK-C2-2026-08-05-build23-bg-tuning-and-tour-tap.md W-1(IMG_8780): グレー
                        // 空箱のままだと未完成に見えるため、朝専用の看板動画(asa10=2EfFlQev4rg)の
                        // サムネを仮アセット同梱して表示(GPT生成絵が届くまでの暫定)。
                        ZStack {
                            RoundedRectangle(cornerRadius: 8).fill(colors.line)
                            KyonoTourDrawable(name: "tour-map-thumb").clipShape(RoundedRectangle(cornerRadius: 8))
                        }
                        .frame(width: 46, height: 46 * 9 / 16)
                        Text("きょうの1本").kyonoFont(.bold700, size: 13).foregroundColor(colors.ink)
                        Spacer()
                    }
                    .padding(8).background(RoundedRectangle(cornerRadius: 12).fill(colors.card))
                    Image(systemName: "arrow.down").foregroundColor(colors.sub).font(.system(size: 14))
                    Text("きょうやった！").kyonoFont(.black900, size: 14).foregroundColor(kyonoBtnPrimaryText)
                        .frame(maxWidth: .infinity).padding(.vertical, 8)
                        .background(RoundedRectangle(cornerRadius: 12).fill(colors.yellow))
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
                            RoundedRectangle(cornerRadius: 10).stroke(colors.borderStrong, lineWidth: 1.5)
                                .frame(width: 52, height: 52)
                                .overlay(Text("？").kyonoFont(.black900, size: 16).foregroundColor(colors.sub))
                        }
                    }
                }
                .frame(maxWidth: .infinity)
            }
        // 予告1) 悩みは相談室で質問: TASK-C2-2026-08-05-build29-round7.md R-21(本人指示・IMG_8820)
        // 相談室シート実UIの縮小再現(ヘッダー+吹き出し2つ+チップ行+入力欄+送信)。文字が読める
        // 必要はなく「あの画面だ」と分かる密度でよい(本人指示どおり)。
        case .soudan:
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    KyonoIconGlyph(icon: .soudanBubble, fill: .clear, accent: colors.teal).frame(width: 15, height: 15)
                    Text("オガトレ相談室").kyonoFont(.black900, size: 13).foregroundColor(colors.ink)
                    Spacer()
                    Text("✕").kyonoFont(.black900, size: 11).foregroundColor(colors.ink)
                        .frame(width: 18, height: 18).background(Circle().fill(colors.line))
                }
                .padding(.horizontal, 10).padding(.vertical, 8)
                Divider()
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Spacer()
                        Text("肩こりがつらい").kyonoFont(.bold700, size: 12).foregroundColor(colors.ink)
                            .padding(.horizontal, 10).padding(.vertical, 6)
                            .background(UnevenRoundedRectangle(topLeadingRadius: 12, bottomLeadingRadius: 12, bottomTrailingRadius: 4, topTrailingRadius: 12).fill(colors.yellowSoft))
                    }
                    HStack(alignment: .bottom) {
                        KyonoCharaImage(name: "chara-good").frame(width: 24, height: 24)
                        let shape = UnevenRoundedRectangle(topLeadingRadius: 12, bottomLeadingRadius: 4, bottomTrailingRadius: 12, topTrailingRadius: 12)
                        Text("それはつらいね…！まずはこの1本").kyonoFont(.bold700, size: 12).foregroundColor(colors.ink)
                            .padding(.horizontal, 10).padding(.vertical, 6)
                            .background(shape.fill(colors.card))
                            .overlay(shape.stroke(colors.borderStrong, lineWidth: 1.2))
                    }
                }
                .padding(.horizontal, 10).padding(.top, 8)
                HStack(spacing: 5) {
                    ForEach(["肩こり", "腰", "前屈"], id: \.self) { label in
                        Text(label).kyonoFont(.bold700, size: 10).foregroundColor(colors.tealInk)
                            .padding(.horizontal, 8).padding(.vertical, 4)
                            .background(Capsule().fill(colors.tealSoft))
                    }
                }
                .padding(.horizontal, 10).padding(.top, 8)
                HStack(spacing: 6) {
                    Text("例: 肩がこる").kyonoFont(.bold700, size: 11).foregroundColor(colors.sub)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 10).padding(.vertical, 8)
                        .background(RoundedRectangle(cornerRadius: 10).fill(colors.card))
                        .overlay(RoundedRectangle(cornerRadius: 10).stroke(colors.borderStrong, lineWidth: 1.2))
                    Text("送信").kyonoFont(.black900, size: 12).foregroundColor(kyonoBtnPrimaryText)
                        .padding(.horizontal, 12).padding(.vertical, 8)
                        .background(RoundedRectangle(cornerRadius: 10).fill(colors.yellow))
                }
                .padding(10)
            }
            .background(RoundedRectangle(cornerRadius: 14).fill(colors.card))
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(colors.borderStrong, lineWidth: 1.2))
        // 予告2) オガトレ通信をのぞく: TASK-C2-2026-08-05-build29-round7.md R-21: オガトレ通信画面
        // (ObuView)実UIの縮小再現(ヘッダー+投稿カード2種=文字投稿の黄ボックス/写真投稿)。
        case .obu:
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    KyonoIconGlyph(icon: .obuBubble, fill: .clear, accent: colors.pink).frame(width: 15, height: 15)
                    Text("オガトレ通信").kyonoFont(.black900, size: 13).foregroundColor(colors.ink)
                    Spacer()
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text("8/3").kyonoFont(.black900, size: 10).foregroundColor(colors.sub2)
                    Text("今日もおつかれさま！").kyonoFont(.bold700, size: 12).foregroundColor(colors.ink)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(10)
                .background(RoundedRectangle(cornerRadius: 10).fill(colors.yellowSoft))
                VStack(alignment: .leading, spacing: 4) {
                    Text("8/1").kyonoFont(.black900, size: 10).foregroundColor(colors.sub)
                    RoundedRectangle(cornerRadius: 10).fill(colors.line)
                        .frame(height: 40)
                        .overlay(RoundedRectangle(cornerRadius: 10).stroke(colors.borderStrong, lineWidth: 1.2))
                }
            }
        // 予告3) マイ記録でふりかえる: TASK-C2-2026-08-05-build29-round7.md R-21: マイ記録画面の
        // カレンダー実UIの縮小再現(見出し+月送りナビ+曜日行+日付グリッド)。
        case .myRecord:
            KyonoCard {
                VStack(spacing: 6) {
                    Text("マイ記録").kyonoFont(.black900, size: 15).foregroundColor(colors.ink)
                    HStack {
                        Text("◀").kyonoFont(.black900, size: 11).foregroundColor(colors.sub)
                        Spacer()
                        Text(verbatim: "8月").kyonoFont(.black900, size: 13).foregroundColor(colors.ink)
                        Spacer()
                        Text("▶").kyonoFont(.black900, size: 11).foregroundColor(colors.sub)
                    }
                    HStack(spacing: 4) {
                        ForEach(["日", "月", "火", "水", "木", "金", "土"], id: \.self) { w in
                            Text(w).kyonoFont(.black900, size: 9).foregroundColor(colors.sub)
                                .frame(maxWidth: .infinity)
                        }
                    }
                    ForEach(0..<2, id: \.self) { row in
                        HStack(spacing: 4) {
                            ForEach(0..<7, id: \.self) { col in
                                let n = row * 7 + col + 1
                                let done = [2, 3, 5, 9, 10].contains(n)
                                Circle().fill(done ? colors.tealStrong : Color.clear)
                                    .frame(width: 20, height: 20)
                                    .overlay(Text("\(n)").kyonoFont(.bold700, size: 9).foregroundColor(done ? .white : colors.sub))
                            }
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
    private var ext: String { ["obu-fab-photo", "tour-map-thumb"].contains(name) ? "jpg" : "png" }
    var body: some View {
        if let url = Bundle.main.url(forResource: name, withExtension: ext),
           let uiImage = UIImage(contentsOfFile: url.path) {
            Image(uiImage: uiImage).resizable().aspectRatio(contentMode: .fill)
        }
    }
}
