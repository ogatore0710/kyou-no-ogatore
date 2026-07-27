import CoreGraphics
import CoreText
import Foundation
import ImageIO

// ネイティブ移植 Step 7b(マスタープラン§2-1「index.html drawBragCard」行・§6 Step 7b)→Step4/7b
// パリティ突合タスク(TASK-C2-2026-07-26-native-migration-card-visual-assets.md): じまんカード描画
// (index.html:2805-2919 drawBragCard)の1:1移植(Android版BragCardRenderer.ktと同一ロジック)。
// CardRendererとは独立の描画器として作業量を見積もる、という§2-1備考どおりファイル自体は分けているが、
// 実際の背景・飾り・白カード・タイトルピル・日付バッジの舞台演出はdrawCard()と全く同じ数値
// (index.html:2814のコメント「記録カードと同じ舞台」)なので、その部分の図形/テキスト/色ヘルパーは
// CardRenderer(internal公開済み・同一パッケージ)を呼んで再利用する。キャラクター立ち絵・
// M PLUS 1p/BananaNumフォントもCardRenderer側の実装(CHARA_FILES/CardFonts)をそのまま再利用する
// (§2-1備考「じまん・声…」行の「同じアセット・フォント」)。
//
// TASK-C2-2026-07-27-brag-card-thumbnail.md: index.html:2876-2889 drawBragCard()のサムネイル分岐の
// 1:1移植。取得(3秒タイムアウト・失敗時null)自体はネットワークI/Oのため、CardCore(決定的ロジックの
// 1:1移植先)の外側=アプリ層(BragView.swift)で行い、ここには結果のCGImageだけを受け取る。
// thumbnailがnilのとき(未選択・オフライン・タイムアウト)は従来どおり動画タイトルの折り返し表示
// (index.html:2883-2889)へフォールバックする(このフォールバック自体は変更しない)。
public enum BragCardRenderer {
    private static let footerPool = [
        "続けてるじぶん、どんどんじまんしてね✨",
        "この1本と続けた日々が、もうじまんです。",
        "続けてるあなたが、いちばんすてきです。",
    ]

    private static let fixedDeco: [(String, CGFloat, CGFloat, CGFloat)] = [
        ("h", 95, 150, 34), ("s", 885, 120, 26), ("k", 120, 860, 30), ("h", 905, 850, 30),
        ("c", 60, 480, 11), ("k", 940, 430, 24), ("s", 180, 70, 18), ("c", 500, 60, 9),
        ("h", 820, 300, 22), ("c", 935, 650, 10), ("s", 70, 690, 16), ("c", 250, 935, 9), ("k", 760, 945, 22),
    ]

    // index.html:2808 の1:1移植(小数入力を弾かないtype=numberの実測バグ修正込み)。
    public static func clampDays(_ raw: Int) -> Int { min(9999, max(1, raw)) }

    public static func render(ds: String, days: Int, theme: ResolvedTheme, favoriteTitle: String?, thumbnail: CGImage? = nil) -> Data {
        let width = 1000, height = 1000
        let colorSpace = CGColorSpaceCreateDeviceRGB()
        guard let ctx = CGContext(
            data: nil, width: width, height: height, bitsPerComponent: 8, bytesPerRow: 0,
            space: colorSpace, bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue
        ) else { return Data() }

        ctx.translateBy(x: 0, y: CGFloat(height))
        ctx.scaleBy(x: 1, y: -1)

        draw(in: ctx, ds: ds, days: clampDays(days), theme: theme, favoriteTitle: favoriteTitle, thumbnail: thumbnail)

        guard let image = ctx.makeImage() else { return Data() }
        return CardRenderer.pngData(from: image)
    }

    private static func draw(in ctx: CGContext, ds: String, days: Int, theme: ResolvedTheme, favoriteTitle: String?, thumbnail: CGImage?) {
        // 背景グラデ+固定飾り(index.html:2815-2827。記録カードと同じ舞台)
        let colorSpace = CGColorSpaceCreateDeviceRGB()
        let colors = [CardRenderer.color(theme.bg[0]), CardRenderer.color(theme.bg[1])] as CFArray
        if let gradient = CGGradient(colorsSpace: colorSpace, colors: colors, locations: [0, 1]) {
            ctx.drawLinearGradient(gradient, start: .zero, end: CGPoint(x: 1000, y: 1000), options: [])
        }
        for (i, d) in fixedDeco.enumerated() {
            let col = CardRenderer.color(theme.deco[i % theme.deco.count])
            switch d.0 {
            case "h": CardRenderer.drawHeartShape(ctx, x: d.1 - d.3 / 2, y: d.2 - d.3 / 2, s: d.3, color: col)
            case "s": CardRenderer.drawStarShape(ctx, x: d.1, y: d.2, r: d.3 * 0.6, color: col)
            case "k": CardRenderer.drawSparkleShape(ctx, x: d.1, y: d.2, r: d.3 * 0.7, color: col)
            default:
                ctx.setFillColor(col)
                ctx.fillEllipse(in: CGRect(x: d.1 - d.3, y: d.2 - d.3, width: d.3 * 2, height: d.3 * 2))
            }
        }

        // 白カード+破線ふち(index.html:2828-2830。drawCardと同じ数値)
        ctx.setFillColor(gray: 1, alpha: 0.94)
        CardRenderer.roundRectPath(ctx, x: 85, y: 175, w: 830, h: 650, r: 52)
        ctx.fillPath()
        ctx.saveGState()
        ctx.setStrokeColor(CardRenderer.color(theme.main, alpha: 0.45))
        ctx.setLineWidth(4)
        ctx.setLineDash(phase: 0, lengths: [2, 16])
        ctx.setLineCap(.round)
        CardRenderer.roundRectPath(ctx, x: 110, y: 200, w: 780, h: 600, r: 40)
        ctx.strokePath()
        ctx.restoreGState()

        // タイトルピル(index.html:2832-2834。drawCardと同じ数値)
        ctx.setFillColor(CardRenderer.color(theme.main))
        CardRenderer.roundRectPath(ctx, x: 300, y: 145, w: 400, h: 64, r: 32)
        ctx.fillPath()
        CardRenderer.drawCenteredText("#きょうのオガトレ", in: ctx, centerX: 500, baselineY: 190, fontSize: 34, weight: .w900, color: CardRenderer.color("#FFFFFF"))

        // 日付バッジ(index.html:2836-2841。drawCardと同じ数値)
        let parts = ds.split(separator: "-").map { Int($0) ?? 0 }
        let dtxt = parts.count == 3 ? "\(parts[0])/\(parts[1])/\(parts[2])" : ds
        let dw = CardRenderer.textWidth(dtxt, fontSize: 26, weight: .w800)
        ctx.saveGState()
        ctx.setAlpha(0.85)
        ctx.setFillColor(CardRenderer.color(theme.main))
        CardRenderer.roundRectPath(ctx, x: 868 - dw - 44, y: 212, w: dw + 44, h: 52, r: 26)
        ctx.fillPath()
        ctx.restoreGState()
        CardRenderer.drawCenteredText(dtxt, in: ctx, centerX: 868 - (dw + 44) / 2, baselineY: 247, fontSize: 26, weight: .w800, color: CardRenderer.color("#FFFFFF"))

        // つづけてる日数(index.html:2843-2856。桁数でフォントサイズを変える。数字はBananaNum)
        let numTxt = String(days)
        let numSize: CGFloat = numTxt.count <= 2 ? 200 : (numTxt.count == 3 ? 170 : 140)
        let numW = CardRenderer.textWidth(numTxt, fontSize: numSize, weight: .banana)
        let sw = CardRenderer.textWidth("日つづいてる！", fontSize: 52, weight: .w900)
        let totalW = numW + 18 + sw
        let startX = 500 - totalW / 2
        CardRenderer.drawLeftText(numTxt, in: ctx, x: startX, baselineY: 438, fontSize: numSize, weight: .banana, color: CardRenderer.color(theme.main))
        CardRenderer.drawLeftText("日つづいてる！", in: ctx, x: startX + numW + 18, baselineY: 428, fontSize: 52, weight: .w900, color: CardRenderer.color("#3A3A35"))

        // 区切り線(index.html:2859-2860)
        ctx.saveGState()
        ctx.setStrokeColor(CardRenderer.color(theme.main, alpha: 0.3))
        ctx.setLineWidth(3)
        ctx.setLineDash(phase: 0, lengths: [4, 12])
        ctx.move(to: CGPoint(x: 170, y: 488))
        ctx.addLine(to: CGPoint(x: 830, y: 488))
        ctx.strokePath()
        ctx.restoreGState()

        // 「すきな1本」タグピル(index.html:2862-2875)
        do {
            let label = "すきな1本"
            let lw = CardRenderer.textWidth(label, fontSize: 28, weight: .banana)
            let pw = lw + 48
            let yc: CGFloat = 525
            ctx.setFillColor(CardRenderer.color(theme.main))
            CardRenderer.roundRectPath(ctx, x: 500 - pw / 2, y: yc - 30, w: pw, h: 60, r: 30)
            ctx.fillPath()
            CardRenderer.drawLeftText(label, in: ctx, x: 500 - pw / 2 + 24, baselineY: yc + 10, fontSize: 28, weight: .banana, color: CardRenderer.color("#FFFFFF"))
        }

        // サムネイル(index.html:2876-2883)。取得できなかったとき(オフライン・タイムアウト・
        // 動画未選択)は従来どおり動画タイトルの折り返し表示(index.html:2883-2889)にフォールバックする。
        if let thumbnail {
            let tw: CGFloat = 416, thh: CGFloat = 234, tx: CGFloat = 500 - 416 / 2, ty: CGFloat = 562
            ctx.saveGState()
            CardRenderer.roundRectPath(ctx, x: tx, y: ty, w: tw, h: thh, r: 18)
            ctx.clip()
            CardRenderer.drawImageJS(thumbnail, x: tx, y: ty, w: tw, h: thh, in: ctx)
            ctx.restoreGState()
            ctx.saveGState()
            ctx.setStrokeColor(CardRenderer.color(theme.main, alpha: 0.5))
            ctx.setLineWidth(3)
            CardRenderer.roundRectPath(ctx, x: tx, y: ty, w: tw, h: thh, r: 18)
            ctx.strokePath()
            ctx.restoreGState()
        } else {
            let favT = favoriteTitle ?? "まだえらんでません（これから見つけます！）"
            let lines = wrapLines(favT, maxW: 540, maxLines: 2)
            for (i, ln) in lines.enumerated() {
                CardRenderer.drawCenteredText(ln, in: ctx, centerX: 500, baselineY: 645 + CGFloat(i) * 52, fontSize: 34, weight: .w800, color: CardRenderer.color("#3A3A35"))
            }
        }

        // キャラ(index.html:2891-2894。日替わりローテ・CardRenderer.CHARA_FILESを共用。§2-1備考どおり
        // 「同じアセット・フォント」を使う。dateIdx駆動の選定理由はCardRenderer.swift冒頭コメント参照)。
        let dateIdx = CardLottery.dateIdx(ds)
        let charaFiles = CardDataLoader.shared.CHARA_FILES
        if !charaFiles.isEmpty {
            let pick = charaFiles[((dateIdx % charaFiles.count) + charaFiles.count) % charaFiles.count]
            if let charaImage = CardRenderer.loadBundleImage(CardRenderer.charaDrawableName(pick.file)) {
                let w: CGFloat = 255
                let h = w * CGFloat(charaImage.height) / CGFloat(charaImage.width)
                CardRenderer.drawImageJS(charaImage, x: 965 - w, y: 985 - h, w: w, h: h, in: ctx)
            }
        }

        // フッター=キャラの吹き出し(index.html:2896-2914)
        var fh: UInt32 = 0
        for u in ds.utf16 { fh = fh &* 31 &+ UInt32(u) }
        let fmsg = footerPool[Int(fh % UInt32(footerPool.count))]
        var ffs: CGFloat = 27
        while CardRenderer.textWidth(fmsg, fontSize: ffs, weight: .w800) > 560 && ffs > 21 { ffs -= 1 }
        let bw = CardRenderer.textWidth(fmsg, fontSize: ffs, weight: .w800) + 56
        let bx1 = max(70, 690 - bw)
        ctx.setFillColor(gray: 1, alpha: 0.95)
        CardRenderer.roundRectPath(ctx, x: bx1, y: 900, w: bw, h: 74, r: 37)
        ctx.fillPath()
        CardRenderer.drawLeftText(fmsg, in: ctx, x: bx1 + 28, baselineY: 900 + 37 + (ffs * 0.36), fontSize: ffs, weight: .w800, color: CardRenderer.color(theme.main))
    }

    // index.html:2792-2804(drawBragCard直前の後勝ち定義)の1:1移植。1文字ずつ幅を測って折り返し、
    // maxLines到達時は末尾を"…"に置き換える。
    private static func wrapLines(_ text: String, maxW: CGFloat, maxLines: Int) -> [String] {
        var lines: [String] = []
        var cur = ""
        for ch in text {
            let test = cur + String(ch)
            if CardRenderer.textWidth(test, fontSize: 34, weight: .w800) > maxW && !cur.isEmpty {
                lines.append(cur)
                cur = String(ch)
                if lines.count == maxLines { break }
            } else {
                cur = test
            }
        }
        if lines.count < maxLines && !cur.isEmpty {
            lines.append(cur)
        } else if lines.count == maxLines && !cur.isEmpty {
            lines[maxLines - 1] = String(lines[maxLines - 1].dropLast()) + "…"
        }
        return lines
    }
}
