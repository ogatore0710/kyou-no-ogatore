import CoreGraphics
import CoreText
import Foundation
import ImageIO

// ネイティブ移植 Step 4(マスタープラン§2-1・§2-4・§6 Step 4): 記録カード描画(index.html:119-349
// drawCard)の1:1移植。同じ1000x1000座標系を使う。
//
// UIKitではなくCoreGraphics/CoreText/ImageIOを直接使う実装方針(重要): このパッケージのテストは
// SafetyCore/RecordCore(Step2/3)と同じく`swift test`でmacOSホスト上に直接ビルド・実行する運用のため、
// iOS専用のUIKit(UIGraphicsImageRenderer等)に依存すると`swift test`自体がビルドできなくなる
// (Package.swiftのplatforms:はデプロイ対象の宣言であり、`swift test`の実行ホストを変えるものではない)。
// CoreGraphics/CoreText/ImageIOはmacOS/iOS両方で同一APIが使える純粋なApple frameworkなので、
// これらだけで実装すればmacOSホストでのテストとiOS実機/シミュレータでの実行の両方が同じコードで動く。
//
// Step 4時点のスコープ(§6検収基準4「同一日付での再描画が同一出力」を満たす範囲に絞っている):
//   実装済み: 背景グラデーション・日替わり散らし装飾(cardRand駆動・新旧2方式の分岐込み)・
//             白カードパネル+破線ふち・タイトルピル・日付バッジ・通算日数の大数字・節目の王冠+文言。
//   未実装(Step4のスコープ外・検収基準に含まれない。Step7bのパリティ突合で扱う想定):
//     キャラクター立ち絵(chara-*.png)・CARD_IMG_FROM以降のカード柄モチーフ画像(assets/cards/*.webp)・
//     かたさタイプ/メモのタグピル行・フッター吹き出し文言・M PLUS 1p/BananaNumフォント
//     (アセット未同梱のため現時点はHelvetica-Bold(macOS/iOS共通の標準フォント名)で代替。
//     フォント差はビットマップ比較の対象外)。
//
// 現在時刻・乱数を直接読まない設計(§1-1第3項・§2-4末尾の禁止事項。厳守): 装飾の並びはCardLottery.cardRand
// (dateIdxを種にした決定的PRNG)のみで決まり、システム乱数源・現在時刻APIには一切触れない。
// 同じ入力(ds・effTotal・theme・milestone)なら常にビット単位で同じPNGを返す(criterion 4)。
public struct ResolvedTheme {
    public let name: String
    public let bg: [String] // hex 2色[開始,終了]
    public let main: String // hex
    public let deco: [String] // hex 3色
    public init(name: String, bg: [String], main: String, deco: [String]) {
        self.name = name; self.bg = bg; self.main = main; self.deco = deco
    }
}

public enum CardRenderer {
    public static func render(
        ds: String, effTotal: Int, theme: ResolvedTheme, milestone: Bool, milestoneTitle: String?,
        dateIdx: Int, cardThemesV2From: Int
    ) -> Data {
        let width = 1000, height = 1000
        let colorSpace = CGColorSpaceCreateDeviceRGB()
        guard let ctx = CGContext(
            data: nil, width: width, height: height, bitsPerComponent: 8, bytesPerRow: 0,
            space: colorSpace, bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue
        ) else { return Data() }

        // Canvas2D/JS版は原点=左上・y下向き。CGContextのビットマップは原点=左下・y上向きが既定のため、
        // 図形描画(グラデ・パス塗り)はここで一括反転して以後JS版と同じ座標のまま書けるようにする。
        // テキストだけはCoreTextが常にy-upのグリフ空間を仮定するため、drawText側で個別に打ち消す。
        ctx.translateBy(x: 0, y: CGFloat(height))
        ctx.scaleBy(x: 1, y: -1)

        draw(
            in: ctx, ds: ds, effTotal: effTotal, theme: theme, milestone: milestone,
            milestoneTitle: milestoneTitle, dateIdx: dateIdx, cardThemesV2From: cardThemesV2From
        )

        guard let image = ctx.makeImage() else { return Data() }
        return pngData(from: image)
    }

    private static func pngData(from image: CGImage) -> Data {
        let data = NSMutableData()
        guard let dest = CGImageDestinationCreateWithData(data, "public.png" as CFString, 1, nil) else { return Data() }
        CGImageDestinationAddImage(dest, image, nil)
        guard CGImageDestinationFinalize(dest) else { return Data() }
        return data as Data
    }

    private static func draw(
        in ctx: CGContext, ds: String, effTotal: Int, theme: ResolvedTheme,
        milestone: Bool, milestoneTitle: String?, dateIdx: Int, cardThemesV2From: Int
    ) {
        // 背景グラデ(index.html:144-146)
        let colorSpace = CGColorSpaceCreateDeviceRGB()
        let colors = [color(theme.bg[0]), color(theme.bg[1])] as CFArray
        if let gradient = CGGradient(colorsSpace: colorSpace, colors: colors, locations: [0, 1]) {
            ctx.drawLinearGradient(gradient, start: .zero, end: CGPoint(x: 1000, y: 1000), options: [])
        }

        // 日替わり散らし装飾(index.html:167-203。CARD_THEMES_V2_FROM以降か節目かで新旧いずれかの方式)
        drawDecorations(ctx, theme: theme, dateIdx: dateIdx, cardThemesV2From: cardThemesV2From, milestone: milestone)

        // 白カード(index.html:204-207)
        ctx.setFillColor(gray: 1, alpha: 0.94)
        roundRectPath(ctx, x: 85, y: 175, w: 830, h: 650, r: 52)
        ctx.fillPath()
        ctx.saveGState()
        ctx.setStrokeColor(color(theme.main, alpha: 0.45))
        ctx.setLineWidth(4)
        ctx.setLineDash(phase: 0, lengths: [2, 16])
        ctx.setLineCap(.round)
        roundRectPath(ctx, x: 110, y: 200, w: 780, h: 600, r: 40)
        ctx.strokePath()
        ctx.restoreGState()

        // タイトルピル(index.html:208-211)
        ctx.setFillColor(color(theme.main))
        roundRectPath(ctx, x: 300, y: 145, w: 400, h: 64, r: 32)
        ctx.fillPath()
        drawCenteredText("#きょうのオガトレ", in: ctx, centerX: 500, baselineY: 190, fontSize: 34, color: color("#FFFFFF"))

        // 日付バッジ(index.html:213-220)
        let parts = ds.split(separator: "-").map { Int($0) ?? 0 }
        let dtxt = parts.count == 3 ? "\(parts[0])/\(parts[1])/\(parts[2])" : ds
        let dw = textWidth(dtxt, fontSize: 26)
        ctx.saveGState()
        ctx.setAlpha(0.85)
        ctx.setFillColor(color(theme.main))
        roundRectPath(ctx, x: 868 - dw - 44, y: 212, w: dw + 44, h: 52, r: 26)
        ctx.fillPath()
        ctx.restoreGState()
        drawCenteredText(dtxt, in: ctx, centerX: 868 - (dw + 44) / 2, baselineY: 247, fontSize: 26, color: color("#FFFFFF"))

        // 見出し(節目のときだけ。index.html:222)
        var by: CGFloat = 462
        if milestone {
            drawCrownShape(ctx, x: 500, y: 258, w: 100, color: color("#FFD700"))
            let msTxt = "\(milestoneTitle ?? "節目たっせい")！おめでとうございます！"
            drawCenteredText(msTxt, in: ctx, centerX: 500, baselineY: 330, fontSize: 34, color: color("#8A877D"))
            by = 520
        }

        // 通算日数の大数字(index.html:230-238。M+1p/BananaNumはアセット未同梱のためHelvetica-Boldで代替)
        let numTxt = String(effTotal)
        let numW = textWidth(numTxt, fontSize: 180)
        let dayW = textWidth("日目！", fontSize: 84)
        let bx = 500 - (numW + 16 + dayW) / 2
        drawLeftText(numTxt, in: ctx, x: bx, baselineY: by, fontSize: 180, color: color(theme.main))
        drawLeftText("日目！", in: ctx, x: bx + numW + 16, baselineY: by, fontSize: 84, color: color("#3A3A35"))
    }

    private static func drawDecorations(_ ctx: CGContext, theme: ResolvedTheme, dateIdx: Int, cardThemesV2From: Int, milestone: Bool) {
        let isYozora = theme.name == "よぞら"
        var deco: [(String, CGFloat, CGFloat, CGFloat)] = []
        if dateIdx >= cardThemesV2From && !milestone {
            let rnd = CardLottery.cardRand(seed: UInt32(dateIdx))
            let shapes = ["h", "s", "k", "c", "f", "k", "c"]
            let bands: [(CGFloat, CGFloat, CGFloat, CGFloat)] = [(30, 970, 25, 150), (30, 970, 850, 975), (25, 75, 160, 850), (925, 975, 160, 850)]
            let n = 12 + Int(rnd() * 5)
            for _ in 0..<n {
                let b = bands[Int(rnd() * Double(bands.count))]
                let x = (b.0 + rnd() * (b.1 - b.0)).rounded()
                let y = (b.2 + rnd() * (b.3 - b.2)).rounded()
                var sh = shapes[Int(rnd() * Double(shapes.count))]
                if isYozora && (sh == "h" || sh == "f") { sh = rnd() < 0.5 ? "s" : "k" }
                let sz = (10 + rnd() * 24).rounded()
                deco.append((sh, x, y, sz))
            }
        } else {
            // index.html:185-190 固定配置(CARD_THEMES_V2_FROM未満の従来方式・1バイトも変えない)
            deco = [
                ("h", 95, 150, 34), ("s", 885, 120, 26), ("k", 120, 860, 30), ("h", 905, 850, 30),
                ("c", 60, 480, 11), ("k", 940, 430, 24), ("s", 180, 70, 18), ("c", 500, 60, 9),
                ("h", 820, 300, 22), ("c", 935, 650, 10), ("s", 70, 690, 16), ("c", 250, 935, 9), ("k", 760, 945, 22),
            ]
        }
        for (i, d) in deco.enumerated() {
            let col = color(theme.deco[i % theme.deco.count])
            switch d.0 {
            case "h": drawHeartShape(ctx, x: d.1 - d.3 / 2, y: d.2 - d.3 / 2, s: d.3, color: col)
            case "s": drawStarShape(ctx, x: d.1, y: d.2, r: d.3 * 0.6, color: col)
            case "k": drawSparkleShape(ctx, x: d.1, y: d.2, r: d.3 * 0.7, color: col)
            case "f": drawFlowerShape(ctx, x: d.1, y: d.2, r: d.3 * 0.62, color: col)
            default:
                ctx.setFillColor(col)
                ctx.fillEllipse(in: CGRect(x: d.1 - d.3, y: d.2 - d.3, width: d.3 * 2, height: d.3 * 2))
            }
        }
        if isYozora && !milestone {
            drawMoonShape(ctx, x: 152, y: 108, r: 44, color: color("#FFE9A8"))
        }
        if milestone {
            for i in 0..<24 {
                ctx.saveGState()
                let x = CGFloat((i * 173) % 1000), y = CGFloat((i * 257) % 1000)
                ctx.translateBy(x: x, y: y)
                ctx.rotate(by: CGFloat(i) * 1.3)
                ctx.setFillColor(color(theme.deco[i % 3]))
                ctx.fill(CGRect(x: -8, y: -3, width: 16, height: 6))
                ctx.restoreGState()
            }
        }
    }

    // MARK: - 図形ヘルパー(index.html:2624-2690 roundRect/drawHeart/drawStar/drawSparkle/drawFlower/drawMoon/drawCrownの移植)

    private static func roundRectPath(_ ctx: CGContext, x: CGFloat, y: CGFloat, w: CGFloat, h: CGFloat, r: CGFloat) {
        let path = CGMutablePath()
        path.move(to: CGPoint(x: x + r, y: y))
        path.addArc(tangent1End: CGPoint(x: x + w, y: y), tangent2End: CGPoint(x: x + w, y: y + h), radius: r)
        path.addArc(tangent1End: CGPoint(x: x + w, y: y + h), tangent2End: CGPoint(x: x, y: y + h), radius: r)
        path.addArc(tangent1End: CGPoint(x: x, y: y + h), tangent2End: CGPoint(x: x, y: y), radius: r)
        path.addArc(tangent1End: CGPoint(x: x, y: y), tangent2End: CGPoint(x: x + w, y: y), radius: r)
        path.closeSubpath()
        ctx.addPath(path)
    }

    private static func drawHeartShape(_ ctx: CGContext, x: CGFloat, y: CGFloat, s: CGFloat, color: CGColor) {
        ctx.saveGState()
        ctx.translateBy(x: x, y: y)
        ctx.scaleBy(x: s / 24, y: s / 24)
        let path = CGMutablePath()
        path.move(to: CGPoint(x: 12, y: 21))
        path.addCurve(to: CGPoint(x: 3.5, y: 6.5), control1: CGPoint(x: 4, y: 15), control2: CGPoint(x: 1, y: 10))
        path.addCurve(to: CGPoint(x: 12, y: 8), control1: CGPoint(x: 6, y: 3.5), control2: CGPoint(x: 10, y: 4.5))
        path.addCurve(to: CGPoint(x: 20.5, y: 6.5), control1: CGPoint(x: 14, y: 4.5), control2: CGPoint(x: 18, y: 3.5))
        path.addCurve(to: CGPoint(x: 12, y: 21), control1: CGPoint(x: 23, y: 10), control2: CGPoint(x: 20, y: 15))
        path.closeSubpath()
        ctx.addPath(path)
        ctx.setFillColor(color)
        ctx.fillPath()
        ctx.restoreGState()
    }

    private static func drawStarShape(_ ctx: CGContext, x: CGFloat, y: CGFloat, r: CGFloat, color: CGColor) {
        let path = CGMutablePath()
        for i in 0..<10 {
            let a = CGFloat.pi / 5 * CGFloat(i) - CGFloat.pi / 2
            let rr = i % 2 == 0 ? r : r * 0.45
            let p = CGPoint(x: x + rr * cos(a), y: y + rr * sin(a))
            if i == 0 { path.move(to: p) } else { path.addLine(to: p) }
        }
        path.closeSubpath()
        ctx.addPath(path)
        ctx.setFillColor(color)
        ctx.fillPath()
    }

    private static func drawSparkleShape(_ ctx: CGContext, x: CGFloat, y: CGFloat, r: CGFloat, color: CGColor) {
        let path = CGMutablePath()
        path.move(to: CGPoint(x: x, y: y - r))
        path.addQuadCurve(to: CGPoint(x: x + r, y: y), control: CGPoint(x: x + r * 0.15, y: y - r * 0.15))
        path.addQuadCurve(to: CGPoint(x: x, y: y + r), control: CGPoint(x: x + r * 0.15, y: y + r * 0.15))
        path.addQuadCurve(to: CGPoint(x: x - r, y: y), control: CGPoint(x: x - r * 0.15, y: y + r * 0.15))
        path.addQuadCurve(to: CGPoint(x: x, y: y - r), control: CGPoint(x: x - r * 0.15, y: y - r * 0.15))
        path.closeSubpath()
        ctx.addPath(path)
        ctx.setFillColor(color)
        ctx.fillPath()
    }

    private static func drawFlowerShape(_ ctx: CGContext, x: CGFloat, y: CGFloat, r: CGFloat, color: CGColor) {
        for i in 0..<5 {
            let a = CGFloat.pi * 2 / 5 * CGFloat(i) - CGFloat.pi / 2
            let cx = x + r * 0.6 * cos(a), cy = y + r * 0.6 * sin(a)
            ctx.setFillColor(color)
            ctx.fillEllipse(in: CGRect(x: cx - r * 0.42, y: cy - r * 0.42, width: r * 0.84, height: r * 0.84))
        }
        ctx.setFillColor(gray: 1, alpha: 0.9)
        ctx.fillEllipse(in: CGRect(x: x - r * 0.3, y: y - r * 0.3, width: r * 0.6, height: r * 0.6))
    }

    private static func drawMoonShape(_ ctx: CGContext, x: CGFloat, y: CGFloat, r: CGFloat, color: CGColor) {
        let path = CGMutablePath()
        path.addArc(center: CGPoint(x: x, y: y), radius: r, startAngle: -.pi / 2, endAngle: .pi / 2, clockwise: false)
        path.addQuadCurve(to: CGPoint(x: x, y: y - r), control: CGPoint(x: x - r * 0.7, y: y))
        path.closeSubpath()
        ctx.addPath(path)
        ctx.setFillColor(color)
        ctx.fillPath()
    }

    private static func drawCrownShape(_ ctx: CGContext, x: CGFloat, y: CGFloat, w: CGFloat, color: CGColor) {
        let h = w * 0.62
        let path = CGMutablePath()
        path.move(to: CGPoint(x: x - w / 2, y: y + h / 2))
        path.addLine(to: CGPoint(x: x - w / 2, y: y - h * 0.15))
        path.addLine(to: CGPoint(x: x - w * 0.22, y: y + h * 0.05))
        path.addLine(to: CGPoint(x: x, y: y - h / 2))
        path.addLine(to: CGPoint(x: x + w * 0.22, y: y + h * 0.05))
        path.addLine(to: CGPoint(x: x + w / 2, y: y - h * 0.15))
        path.addLine(to: CGPoint(x: x + w / 2, y: y + h / 2))
        path.closeSubpath()
        ctx.addPath(path)
        ctx.setFillColor(color)
        ctx.setStrokeColor(gray: 0.227, alpha: 1) // #3A3A35相当
        ctx.setLineWidth(5)
        ctx.setLineJoin(.round)
        ctx.drawPath(using: .fillStroke)
    }

    // MARK: - テキスト(CoreText。外側でかけた上下反転をここだけ局所的に打ち消す。標準的な手法)

    private static func makeCTFont(_ fontSize: CGFloat) -> CTFont {
        CTFontCreateWithName("Helvetica-Bold" as CFString, fontSize, nil)
    }

    private static func attributedLine(_ text: String, fontSize: CGFloat, color: CGColor) -> CTLine {
        let attrs: [CFString: Any] = [kCTFontAttributeName: makeCTFont(fontSize), kCTForegroundColorAttributeName: color]
        let attrStr = CFAttributedStringCreate(nil, text as CFString, attrs as CFDictionary)!
        return CTLineCreateWithAttributedString(attrStr)
    }

    private static func textWidth(_ text: String, fontSize: CGFloat) -> CGFloat {
        let line = attributedLine(text, fontSize: fontSize, color: color("#000000"))
        return CGFloat(CTLineGetTypographicBounds(line, nil, nil, nil))
    }

    private static func drawCenteredText(_ text: String, in ctx: CGContext, centerX: CGFloat, baselineY: CGFloat, fontSize: CGFloat, color: CGColor) {
        let w = textWidth(text, fontSize: fontSize)
        drawLeftText(text, in: ctx, x: centerX - w / 2, baselineY: baselineY, fontSize: fontSize, color: color)
    }

    private static func drawLeftText(_ text: String, in ctx: CGContext, x: CGFloat, baselineY: CGFloat, fontSize: CGFloat, color: CGColor) {
        let line = attributedLine(text, fontSize: fontSize, color: color)
        ctx.saveGState()
        ctx.translateBy(x: x, y: baselineY)
        ctx.scaleBy(x: 1, y: -1) // CoreTextはy-upのグリフ空間を仮定するため、外側の反転をここだけ打ち消す
        ctx.textPosition = .zero
        CTLineDraw(line, ctx)
        ctx.restoreGState()
    }

    // MARK: - 色

    private static func color(_ hex: String, alpha: CGFloat = 1) -> CGColor {
        var s = hex
        if s.hasPrefix("#") { s.removeFirst() }
        guard s.count == 6, let v = UInt32(s, radix: 16) else { return CGColor(red: 0, green: 0, blue: 0, alpha: alpha) }
        let r = CGFloat((v >> 16) & 0xFF) / 255
        let g = CGFloat((v >> 8) & 0xFF) / 255
        let b = CGFloat(v & 0xFF) / 255
        return CGColor(red: r, green: g, blue: b, alpha: alpha)
    }
}

private extension CGContext {
    func setFillColor(gray: CGFloat, alpha: CGFloat) {
        setFillColor(CGColor(gray: gray, alpha: alpha))
    }
    func setStrokeColor(gray: CGFloat, alpha: CGFloat) {
        setStrokeColor(CGColor(gray: gray, alpha: alpha))
    }
}
