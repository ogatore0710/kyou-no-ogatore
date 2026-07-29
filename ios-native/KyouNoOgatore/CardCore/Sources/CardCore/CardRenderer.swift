import CoreGraphics
import CoreText
import Foundation
import ImageIO

// ネイティブ移植 Step 4(マスタープラン§2-1・§2-4・§6 Step 4)→Step4/7bパリティ突合タスク
// (TASK-C2-2026-07-26-native-migration-card-visual-assets.md): 記録カード描画(index.html:119-349
// drawCard)の1:1移植。同じ1000x1000座標系を使う。
//
// UIKitではなくCoreGraphics/CoreText/ImageIOを直接使う実装方針(重要): このパッケージのテストは
// SafetyCore/RecordCore(Step2/3)と同じく`swift test`でmacOSホスト上に直接ビルド・実行する運用のため、
// iOS専用のUIKit(UIGraphicsImageRenderer等)に依存すると`swift test`自体がビルドできなくなる
// (Package.swiftのplatforms:はデプロイ対象の宣言であり、`swift test`の実行ホストを変えるものではない)。
// CoreGraphics/CoreText/ImageIOはmacOS/iOS両方で同一APIが使える純粋なApple frameworkなので、
// これらだけで実装すればmacOSホストでのテストとiOS実機/シミュレータでの実行の両方が同じコードで動く。
//
// §6検収基準4「同一日付での再描画が同一出力」を満たす範囲: 全項目実装済み(キャラ立ち絵・カード柄
// モチーフ画像・かたさタイプ/メモのタグピル行・フッター吹き出し文言・M PLUS 1p/BananaNumフォント含む)。
// アセット(キャラ・カード柄・アイコン画像・フォント)はBundle.mainから探す(App target直下。Step7aの
// CardArt/DexView.loadCardArtと同じ「サブディレクトリ指定なし」パターン。Xcode26形式の
// PBXFileSystemSynchronizedRootGroupがビルド時にサブフォルダをバンドルルートへフラット化するため)。
// `swift test`実行時(macOSホスト単体実行・アプリバンドルなし)はBundle.mainがアプリと無関係な
// テストランナーを指すため、これらのlookupは自然にnilを返す→未実装時と同じグレースフルな
// フォールバック(Helvetica-Bold代替・画像省略)に落ちるだけで、テスト実行自体は妨げない。
//
// 現在時刻・乱数を直接読まない設計(§1-1第3項・§2-4末尾の禁止事項。厳守): 装飾・キャラクター立ち絵の
// 選択ロジックはすべてcardRand/dateIdx等の決定的入力のみで決まり、システム乱数源・現在時刻APIには
// 一切触れない。キャラクター立ち絵の日替わりローテがWeb版のdayIndex()=現在時刻依存とは意図的に
// 差をつけている点はCardRenderer.kt(Android)の同趣旨コメントと同じ判断(dateIdxを種にすることで
// 「同じ日付の再描画は常に同じキャラ」を保証する)。
public struct ResolvedTheme {
    public let name: String
    public let bg: [String] // hex 2色[開始,終了]
    public let main: String // hex
    public let deco: [String] // hex 3色
    public init(name: String, bg: [String], main: String, deco: [String]) {
        self.name = name; self.bg = bg; self.main = main; self.deco = deco
    }
}

// TestFlight実機フィードバックE1(2026-07-29): 当初は「アイコン画像があるのはこの3タイプのみ」
// だったが、6体ぶんのPNGが揃った(assets/type-{koka,ashi,robot}.png)ため6体とも対象に含める。
// 記録カードは既存のKyonoTypeArt.swiftと違いCanvas相当のフォールバック描画を持たないため、
// この辞書に無いタイプは今までアイコンなしで描画されていた(koka/ashi/robotがまさにこれで、
// alan5の指摘どおり実際に欠けていた)。
public let TYPE_IMG_NAMES: [String: String] = [
    "momo": "type-momo", "kenko": "type-kenko", "yawara": "type-yawara",
    "koka": "type-koka", "ashi": "type-ashi", "robot": "type-robot",
]
private let CHARA_CROWN = "chara-crown"

enum CardFontWeight: Hashable {
    case w700, w800, w900, banana
}

// フォント(app target Fonts/配下・§7bタスクでMPLUS1p-{Bold,ExtraBold,Black}.ttf/bananaslip.otfを同梱)の
// 遅延読み込み+キャッシュ。Bundle.mainで見つからない場合(swift test実行時含む)はHelvetica-Boldへ
// 安全にフォールバックする。
enum CardFonts {
    private static var descriptorCache: [CardFontWeight: CTFontDescriptor] = [:]
    private static func descriptor(for weight: CardFontWeight) -> CTFontDescriptor? {
        if let cached = descriptorCache[weight] { return cached }
        let filename: String
        let ext: String
        switch weight {
        case .w700: filename = "mplus1p-700"; ext = "ttf"
        case .w800: filename = "mplus1p-800"; ext = "ttf"
        case .w900: filename = "mplus1p-900"; ext = "ttf"
        case .banana: filename = "banananum"; ext = "otf"
        }
        guard let url = Bundle.main.url(forResource: filename, withExtension: ext),
              let descriptors = CTFontManagerCreateFontDescriptorsFromURL(url as CFURL) as? [CTFontDescriptor],
              let d = descriptors.first else { return nil }
        descriptorCache[weight] = d
        return d
    }
    static func font(_ weight: CardFontWeight, size: CGFloat) -> CTFont {
        guard let d = descriptor(for: weight) else {
            return CTFontCreateWithName("Helvetica-Bold" as CFString, size, nil)
        }
        return CTFontCreateWithFontDescriptor(d, size, nil)
    }
}

public enum CardRenderer {
    public static func render(
        ds: String, effTotal: Int, theme: ResolvedTheme, milestone: Bool, milestoneTitle: String?,
        dateIdx: Int, cardThemesV2From: Int,
        pat: CardPattern? = nil, typeName: String? = nil, typeIconKey: String? = nil,
        memoText: String? = nil, streakCount: Int = 0
    ) -> Data {
        let width = 1000, height = 1000
        let colorSpace = CGColorSpaceCreateDeviceRGB()
        guard let ctx = CGContext(
            data: nil, width: width, height: height, bitsPerComponent: 8, bytesPerRow: 0,
            space: colorSpace, bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue
        ) else { return Data() }

        // Canvas2D/JS版は原点=左上・y下向き。CGContextのビットマップは原点=左下・y上向きが既定のため、
        // 図形描画(グラデ・パス塗り)はここで一括反転して以後JS版と同じ座標のまま書けるようにする。
        // テキスト・画像だけはそれぞれの描画APIがy-upの空間を仮定するため、drawText/drawImage側で
        // 個別に打ち消す。
        ctx.translateBy(x: 0, y: CGFloat(height))
        ctx.scaleBy(x: 1, y: -1)

        draw(
            in: ctx, ds: ds, effTotal: effTotal, theme: theme, milestone: milestone,
            milestoneTitle: milestoneTitle, dateIdx: dateIdx, cardThemesV2From: cardThemesV2From,
            pat: pat, typeName: typeName, typeIconKey: typeIconKey, memoText: memoText, streakCount: streakCount
        )

        guard let image = ctx.makeImage() else { return Data() }
        return pngData(from: image)
    }

    static func pngData(from image: CGImage) -> Data {
        let data = NSMutableData()
        guard let dest = CGImageDestinationCreateWithData(data, "public.png" as CFString, 1, nil) else { return Data() }
        CGImageDestinationAddImage(dest, image, nil)
        guard CGImageDestinationFinalize(dest) else { return Data() }
        return data as Data
    }

    private static func draw(
        in ctx: CGContext, ds: String, effTotal: Int, theme: ResolvedTheme,
        milestone: Bool, milestoneTitle: String?, dateIdx: Int, cardThemesV2From: Int,
        pat: CardPattern?, typeName: String?, typeIconKey: String?, memoText: String?, streakCount: Int
    ) {
        // 背景グラデ(index.html:144-146)
        let colorSpace = CGColorSpaceCreateDeviceRGB()
        let colors = [color(theme.bg[0]), color(theme.bg[1])] as CFArray
        if let gradient = CGGradient(colorsSpace: colorSpace, colors: colors, locations: [0, 1]) {
            ctx.drawLinearGradient(gradient, start: .zero, end: CGPoint(x: 1000, y: 1000), options: [])
        }

        // カード柄モチーフ画像 or 日替わり散らし装飾(index.html:147-203)
        let motifImage = pat?.key.flatMap { loadBundleImage($0) }
        if let motifImage {
            drawImageJS(motifImage, x: 0, y: 0, w: 1000, h: 1000, in: ctx)
        } else if let pat {
            drawScatterSimple(ctx, theme: theme, dateIdx: dateIdx)
            _ = pat // pat!=nullであることの利用のみ(milestoneはこの分岐で常にfalse・cardPatternFor仕様)
        } else {
            drawDecorationsLegacy(ctx, theme: theme, dateIdx: dateIdx, cardThemesV2From: cardThemesV2From, milestone: milestone)
        }

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
        drawCenteredText("#きょうのオガトレ", in: ctx, centerX: 500, baselineY: 190, fontSize: 34, weight: .w900, color: color("#FFFFFF"))

        // 日付バッジ(index.html:213-220)
        let parts = ds.split(separator: "-").map { Int($0) ?? 0 }
        let dtxt = parts.count == 3 ? "\(parts[0])/\(parts[1])/\(parts[2])" : ds
        let dw = textWidth(dtxt, fontSize: 26, weight: .w800)
        ctx.saveGState()
        ctx.setAlpha(0.85)
        ctx.setFillColor(color(theme.main))
        roundRectPath(ctx, x: 868 - dw - 44, y: 212, w: dw + 44, h: 52, r: 26)
        ctx.fillPath()
        ctx.restoreGState()
        drawCenteredText(dtxt, in: ctx, centerX: 868 - (dw + 44) / 2, baselineY: 247, fontSize: 26, weight: .w800, color: color("#FFFFFF"))

        // 見出し(節目のときだけ。index.html:222)
        if milestone {
            drawCrownShape(ctx, x: 500, y: 258, w: 100, color: color("#FFD700"))
            let msTxt = "\(milestoneTitle ?? "節目たっせい")！おめでとうございます！"
            drawCenteredText(msTxt, in: ctx, centerX: 500, baselineY: 330, fontSize: 34, weight: .w800, color: color("#8A877D"))
        }

        // 山本さん案レイアウト: 数字+日目！を1行 → タグピル行(index.html:223-296)。
        // rowsの有無で大数字のY位置がずれる(shift)ため、rowsを先に確定させる。
        var rows: [(String, String)] = []
        if let typeName { rows.append(("かたさタイプ", typeName)) }
        if let memoText { rows.append(("メモ", memoText)) }
        let shift: CGFloat = milestone ? 0 : (rows.isEmpty ? 60 : 0)
        let by: CGFloat = (milestone ? 520 : 462) + shift

        // 通算日数の大数字(index.html:230-238)
        let numTxt = String(effTotal)
        let numW = textWidth(numTxt, fontSize: 180, weight: .w900)
        let dayW = textWidth("日目！", fontSize: 84, weight: .w900)
        let bx = 500 - (numW + 16 + dayW) / 2
        drawLeftText(numTxt, in: ctx, x: bx, baselineY: by, fontSize: 180, weight: .w900, color: color(theme.main))
        drawLeftText("日目！", in: ctx, x: bx + numW + 16, baselineY: by, fontSize: 84, weight: .w900, color: color("#3A3A35"))

        // 連続記録N日(index.html:240)
        if streakCount >= 2 {
            drawCenteredText("連続記録\(streakCount)日", in: ctx, centerX: 500, baselineY: by + 52, fontSize: 30, weight: .w700, color: color("#8A877D"))
        }

        // キャラ選定(dateIdx駆動・決定的。上部コメント参照)
        let charaFiles = CardDataLoader.shared.CHARA_FILES
        let charaPick = charaFiles[((dateIdx % charaFiles.count) + charaFiles.count) % charaFiles.count]
        let charaImage = loadBundleImage(charaDrawableName(charaPick.file))
        let crownImage = loadBundleImage(CHARA_CROWN)
        let charaW = CGFloat(charaPick.w)

        // タグピル行(index.html:241-296)
        let rowY: [CGFloat] = milestone ? [648, 764] : [614, 738]
        let chW: CGFloat = milestone ? 280 : charaW
        let chActive = milestone ? (crownImage != nil || charaImage != nil) : charaImage != nil
        let row1RightEdge: CGFloat = chActive ? 985 - chW - 24 : 860
        for (i, row) in rows.prefix(2).enumerated() {
            let (label, value) = row
            let yc = rowY[i]
            let lw = textWidth(label, fontSize: 28, weight: .banana)
            let pw = lw + 48
            ctx.setFillColor(color(theme.main))
            roundRectPath(ctx, x: 130, y: yc - 30, w: pw, h: 60, r: 30)
            ctx.fillPath()
            drawLeftText(label, in: ctx, x: 130 + 24, baselineY: yc + 10, fontSize: 28, weight: .banana, color: color("#FFFFFF"))
            let vx = 130 + pw + 26
            let maxW = (i == 1 ? row1RightEdge : 860) - vx
            var fs: CGFloat = 46
            while textWidth(value, fontSize: fs, weight: .w800) > maxW && fs > 32 { fs -= 2 }
            if textWidth(value, fontSize: fs, weight: .w800) <= maxW {
                drawLeftText(value, in: ctx, x: vx, baselineY: yc + (fs * 0.36).rounded(), fontSize: fs, weight: .w800, color: color("#3A3A35"))
                if label == "かたさタイプ", let typeIconKey, let iconName = TYPE_IMG_NAMES[typeIconKey], let iconImage = loadBundleImage(iconName) {
                    let ih = fs + 10
                    let ar = CGFloat(iconImage.width) / CGFloat(iconImage.height)
                    let iw = ih * ar
                    let ix = vx + textWidth(value, fontSize: fs, weight: .w800) + 14
                    let iy = yc - ih / 2
                    if ix + iw <= maxW + vx {
                        drawImageJS(iconImage, x: ix, y: iy, w: iw, h: ih, in: ctx)
                    }
                }
            } else if i == 1 {
                // メモ行はキャラ回避で幅が狭くなりうるため3行まで許容(index.html:274-278)
                fs = 28
                let lines = wrapLines(value, maxW: maxW, maxLines: 3, fontSize: fs, weight: .w800)
                let ly0 = yc - CGFloat(lines.count - 1) * 19
                for (li, l) in lines.enumerated() {
                    drawLeftText(l, in: ctx, x: vx, baselineY: ly0 + CGFloat(li) * 38, fontSize: fs, weight: .w800, color: color("#3A3A35"))
                }
            } else {
                // 2行に分割(index.html:280-287)
                fs = 30
                var l1 = ""; var i2 = value.startIndex
                while i2 < value.endIndex, textWidth(l1 + String(value[i2]), fontSize: fs, weight: .w800) <= maxW {
                    l1.append(value[i2]); i2 = value.index(after: i2)
                }
                var l2 = String(value[i2...])
                while textWidth(l2, fontSize: fs, weight: .w800) > maxW && l2.count > 2 {
                    l2 = String(l2.dropLast(2)) + "…"
                }
                drawLeftText(l1, in: ctx, x: vx, baselineY: yc - 6, fontSize: fs, weight: .w800, color: color("#3A3A35"))
                drawLeftText(l2, in: ctx, x: vx, baselineY: yc + 32, fontSize: fs, weight: .w800, color: color("#3A3A35"))
            }
        }
        // 点線区切り(index.html:291-296)
        if rows.count >= 2 {
            let sy = (rowY[0] + rowY[1]) / 2
            ctx.saveGState()
            ctx.setStrokeColor(color(theme.main, alpha: 0.4))
            ctx.setLineWidth(3)
            ctx.setLineDash(phase: 0, lengths: [3, 11])
            ctx.setLineCap(.round)
            ctx.move(to: CGPoint(x: 150, y: sy))
            ctx.addLine(to: CGPoint(x: 850, y: sy))
            ctx.strokePath()
            ctx.restoreGState()
        }

        // キャラ本体(index.html:297-302)
        let chFig = milestone ? (crownImage ?? charaImage) : charaImage
        if let chFig {
            let w: CGFloat = milestone ? 280 : charaW
            let h = w * CGFloat(chFig.height) / CGFloat(chFig.width)
            drawImageJS(chFig, x: 985 - w, y: 998 - h, w: w, h: h, in: ctx)
        }

        // フッター=キャラの吹き出し(index.html:303-341)
        drawFooterBubble(ctx, ds: ds, theme: theme)
    }

    // index.html:151-166「else if(pat)」分岐の1:1移植: pat!=null(トク/季節/レア/ノーマルいずれか確定済み)
    // だが画像未解決の場合の日替わり散らし。CARD_THEMES_V2_FROM分岐・yozora特別扱い・月は無い
    // (この分岐に来る時点でtheme.nameはpatのbg/main/decoから来るためよぞらになり得ない=Web版と同仕様)。
    private static func drawScatterSimple(_ ctx: CGContext, theme: ResolvedTheme, dateIdx: Int) {
        let rnd = CardLottery.cardRand(seed: UInt32(dateIdx))
        let shapes = ["h", "s", "k", "c", "f", "k", "c"]
        let bands: [(CGFloat, CGFloat, CGFloat, CGFloat)] = [(30, 970, 25, 150), (30, 970, 850, 975), (25, 75, 160, 850), (925, 975, 160, 850)]
        let n = 12 + Int(rnd() * 5)
        for i in 0..<n {
            let b = bands[Int(rnd() * Double(bands.count))]
            let x = (b.0 + rnd() * (b.1 - b.0)).rounded()
            let y = (b.2 + rnd() * (b.3 - b.2)).rounded()
            let sh = shapes[Int(rnd() * Double(shapes.count))]
            let sz = (10 + rnd() * 24).rounded()
            drawShape(ctx, shape: sh, x: x, y: y, sz: sz, color: color(theme.deco[i % theme.deco.count]))
        }
    }

    private static func drawDecorationsLegacy(_ ctx: CGContext, theme: ResolvedTheme, dateIdx: Int, cardThemesV2From: Int, milestone: Bool) {
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
            drawShape(ctx, shape: d.0, x: d.1, y: d.2, sz: d.3, color: col)
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

    private static func drawShape(_ ctx: CGContext, shape: String, x: CGFloat, y: CGFloat, sz: CGFloat, color: CGColor) {
        switch shape {
        case "h": drawHeartShape(ctx, x: x - sz / 2, y: y - sz / 2, s: sz, color: color)
        case "s": drawStarShape(ctx, x: x, y: y, r: sz * 0.6, color: color)
        case "k": drawSparkleShape(ctx, x: x, y: y, r: sz * 0.7, color: color)
        case "f": drawFlowerShape(ctx, x: x, y: y, r: sz * 0.62, color: color)
        default:
            ctx.setFillColor(color)
            ctx.fillEllipse(in: CGRect(x: x - sz, y: y - sz, width: sz * 2, height: sz * 2))
        }
    }

    // index.html:303-329 フッター(キャラの吹き出し)の1:1移植。日付文字列の31進ハッシュで
    // プールから選ぶだけの決定的ロジック(VoicesLogic/BragCardRendererと同じ手法)。
    private static func drawFooterBubble(_ ctx: CGContext, ds: String, theme: ResolvedTheme) {
        let parts = ds.split(separator: "-")
        let fm = parts.count == 3 ? (Int(parts[1]) ?? 1) : 1
        let fd = parts.count == 3 ? (Int(parts[2]) ?? 1) : 1
        let common = [
            "今日も一日、じぶんを大切に。ご自愛くださいね。",
            "がんばりすぎず、ほどよく。ご自愛くださいね。",
            "深呼吸ひとつぶん、じぶんをいたわる時間を。",
            "今日のあなたに、おつかれさまとご自愛を。",
            "体の声をきいて、むりせずご自愛くださいね。",
            "つづけてる自分を、ちゃんと褒めてあげてね。",
        ]
        let pool: [String]
        if fm == 1 && fd <= 7 {
            pool = ["今年もじぶんのペースで。ご自愛くださいね。", "新年も一日一本、ゆるっといきましょう。"]
        } else if fm == 12 && fd >= 28 {
            pool = ["一年おつかれさま。ゆっくりご自愛くださいね。", "今年もよくがんばりました。よいお年を。"]
        } else if fm == 6 {
            pool = common + ["じめじめの季節も心は軽く。ご自愛くださいね。", "雨の日は、おうちストレッチ日和です。"]
        } else if fm == 7 || fm == 8 {
            pool = common + ["暑い毎日、水分とご自愛を忘れずに。", "夏バテ予防も、ストレッチとご自愛から。"]
        } else if fm >= 9 && fm <= 11 {
            pool = common + ["季節の変わり目、ゆっくりご自愛くださいね。", "実りの秋。体にもいいことを少しずつ。"]
        } else if fm == 12 || fm <= 2 {
            pool = common + ["寒い季節も、あたたかくご自愛くださいね。", "湯船にゆっくり。それもご自愛のうち。"]
        } else {
            pool = common + ["新しい季節も、マイペースにご自愛くださいね。", "春の陽気と一緒に、体もゆるめてあげてね。"]
        }
        var fh: UInt32 = 0
        for c in ds.utf16 { fh = fh &* 31 &+ UInt32(c) }
        let fmsg = pool[Int(fh % UInt32(pool.count))]
        var ffs: CGFloat = 27
        while textWidth(fmsg, fontSize: ffs, weight: .w800) > 560 && ffs > 21 { ffs -= 1 }
        let bw = textWidth(fmsg, fontSize: ffs, weight: .w800) + 56
        let bx1 = max(70, 690 - bw)
        ctx.setFillColor(gray: 1, alpha: 0.95)
        roundRectPath(ctx, x: bx1, y: 900, w: bw, h: 74, r: 37)
        ctx.fillPath()
        let tailPath = CGMutablePath()
        tailPath.move(to: CGPoint(x: bx1 + bw - 6, y: 918))
        tailPath.addLine(to: CGPoint(x: bx1 + bw + 26, y: 930))
        tailPath.addLine(to: CGPoint(x: bx1 + bw - 6, y: 950))
        tailPath.closeSubpath()
        ctx.setFillColor(gray: 1, alpha: 0.95)
        ctx.addPath(tailPath)
        ctx.fillPath()
        drawLeftText(fmsg, in: ctx, x: bx1 + 28, baselineY: 900 + 37 + (ffs * 0.36).rounded(), fontSize: ffs, weight: .w800, color: color(theme.main))
    }

    // F1(TASK-C2-2026-07-29-inspection-upgrade.md): テストから画像解決を差し替えられる入口。
    // 実行時はBundle.main経由の実装(下)を使うが、swift test環境ではBundle.mainがアプリ本体を
    // 指さないため常にnilになる(=タイプアイコンの有無を画像到達性だけでは検査できない)。
    // テストはこのimageLoaderを差し替えて「アイコンが実際に描画結果へ反映されるか」を確認する。
    static var imageLoader: (String) -> CGImage? = { name in
        // DexView.loadCardArt(Step7a)と同じBundle.mainパターン(サブディレクトリ指定なし。ヘッダコメント参照)。
        guard let url = Bundle.main.url(forResource: name, withExtension: "png"),
              let src = CGImageSourceCreateWithURL(url as CFURL, nil) else { return nil }
        return CGImageSourceCreateImageAtIndex(src, 0, nil)
    }

    static func loadBundleImage(_ name: String) -> CGImage? { imageLoader(name) }

    // card-data.json(手写しではなくgen-card-data.mjsでJSから機械抽出済み・§1-2「手写し禁止」原則どおり)の
    // file値("assets/chara-good.png"形式のWeb側パス)を、バンドル内画像名("chara-good")へ変換する純関数。
    static func charaDrawableName(_ webPath: String) -> String {
        var s = webPath
        if s.hasPrefix("assets/") { s.removeFirst("assets/".count) }
        if s.hasSuffix(".png") { s.removeLast(".png".count) }
        return s
    }

    // 985,998起点で右下に配置される画像をJS座標系(x,y=左上・y下向き)のまま描画するヘルパー。
    // 外側のグローバルy反転を局所的に打ち消す(drawLeftTextと同じ標準手法。ヘッダコメント参照)。
    static func drawImageJS(_ image: CGImage, x: CGFloat, y: CGFloat, w: CGFloat, h: CGFloat, in ctx: CGContext) {
        ctx.saveGState()
        ctx.translateBy(x: x, y: y)
        ctx.scaleBy(x: 1, y: -1)
        ctx.draw(image, in: CGRect(x: 0, y: -h, width: w, height: h))
        ctx.restoreGState()
    }

    // index.html:2625-2635 wrapLinesの1:1移植(タグピルのメモ行3行折り返し用)。
    private static func wrapLines(_ text: String, maxW: CGFloat, maxLines: Int, fontSize: CGFloat, weight: CardFontWeight) -> [String] {
        var lines: [String] = []
        var rest = Substring(text)
        while !rest.isEmpty && lines.count < maxLines {
            var l = ""
            var i = rest.startIndex
            while i < rest.endIndex, textWidth(l + String(rest[i]), fontSize: fontSize, weight: weight) <= maxW {
                l.append(rest[i]); i = rest.index(after: i)
            }
            if i == rest.startIndex { l = String(rest[rest.startIndex]); i = rest.index(after: rest.startIndex) }
            lines.append(l)
            rest = rest[i...]
        }
        if !rest.isEmpty, !lines.isEmpty {
            var last = lines[lines.count - 1]
            while textWidth(last + "…", fontSize: fontSize, weight: weight) > maxW && !last.isEmpty { last.removeLast() }
            lines[lines.count - 1] = last + "…"
        }
        return lines
    }

    // MARK: - 図形ヘルパー(index.html:2624-2690 roundRect/drawHeart/drawStar/drawSparkle/drawFlower/drawMoon/drawCrownの移植)

    static func roundRectPath(_ ctx: CGContext, x: CGFloat, y: CGFloat, w: CGFloat, h: CGFloat, r: CGFloat) {
        let path = CGMutablePath()
        path.move(to: CGPoint(x: x + r, y: y))
        path.addArc(tangent1End: CGPoint(x: x + w, y: y), tangent2End: CGPoint(x: x + w, y: y + h), radius: r)
        path.addArc(tangent1End: CGPoint(x: x + w, y: y + h), tangent2End: CGPoint(x: x, y: y + h), radius: r)
        path.addArc(tangent1End: CGPoint(x: x, y: y + h), tangent2End: CGPoint(x: x, y: y), radius: r)
        path.addArc(tangent1End: CGPoint(x: x, y: y), tangent2End: CGPoint(x: x + w, y: y), radius: r)
        path.closeSubpath()
        ctx.addPath(path)
    }

    static func drawHeartShape(_ ctx: CGContext, x: CGFloat, y: CGFloat, s: CGFloat, color: CGColor) {
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

    static func drawStarShape(_ ctx: CGContext, x: CGFloat, y: CGFloat, r: CGFloat, color: CGColor) {
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

    static func drawSparkleShape(_ ctx: CGContext, x: CGFloat, y: CGFloat, r: CGFloat, color: CGColor) {
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

    static func drawFlowerShape(_ ctx: CGContext, x: CGFloat, y: CGFloat, r: CGFloat, color: CGColor) {
        for i in 0..<5 {
            let a = CGFloat.pi * 2 / 5 * CGFloat(i) - CGFloat.pi / 2
            let cx = x + r * 0.6 * cos(a), cy = y + r * 0.6 * sin(a)
            ctx.setFillColor(color)
            ctx.fillEllipse(in: CGRect(x: cx - r * 0.42, y: cy - r * 0.42, width: r * 0.84, height: r * 0.84))
        }
        ctx.setFillColor(gray: 1, alpha: 0.9)
        ctx.fillEllipse(in: CGRect(x: x - r * 0.3, y: y - r * 0.3, width: r * 0.6, height: r * 0.6))
    }

    static func drawMoonShape(_ ctx: CGContext, x: CGFloat, y: CGFloat, r: CGFloat, color: CGColor) {
        let path = CGMutablePath()
        path.addArc(center: CGPoint(x: x, y: y), radius: r, startAngle: -.pi / 2, endAngle: .pi / 2, clockwise: false)
        path.addQuadCurve(to: CGPoint(x: x, y: y - r), control: CGPoint(x: x - r * 0.7, y: y))
        path.closeSubpath()
        ctx.addPath(path)
        ctx.setFillColor(color)
        ctx.fillPath()
    }

    static func drawCrownShape(_ ctx: CGContext, x: CGFloat, y: CGFloat, w: CGFloat, color: CGColor) {
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

    private static func attributedLine(_ text: String, fontSize: CGFloat, weight: CardFontWeight, color: CGColor) -> CTLine {
        let attrs: [CFString: Any] = [kCTFontAttributeName: CardFonts.font(weight, size: fontSize), kCTForegroundColorAttributeName: color]
        let attrStr = CFAttributedStringCreate(nil, text as CFString, attrs as CFDictionary)!
        return CTLineCreateWithAttributedString(attrStr)
    }

    static func textWidth(_ text: String, fontSize: CGFloat, weight: CardFontWeight) -> CGFloat {
        let line = attributedLine(text, fontSize: fontSize, weight: weight, color: color("#000000"))
        return CGFloat(CTLineGetTypographicBounds(line, nil, nil, nil))
    }

    static func drawCenteredText(_ text: String, in ctx: CGContext, centerX: CGFloat, baselineY: CGFloat, fontSize: CGFloat, weight: CardFontWeight, color: CGColor) {
        let w = textWidth(text, fontSize: fontSize, weight: weight)
        drawLeftText(text, in: ctx, x: centerX - w / 2, baselineY: baselineY, fontSize: fontSize, weight: weight, color: color)
    }

    static func drawLeftText(_ text: String, in ctx: CGContext, x: CGFloat, baselineY: CGFloat, fontSize: CGFloat, weight: CardFontWeight, color: CGColor) {
        let line = attributedLine(text, fontSize: fontSize, weight: weight, color: color)
        ctx.saveGState()
        ctx.translateBy(x: x, y: baselineY)
        ctx.scaleBy(x: 1, y: -1) // CoreTextはy-upのグリフ空間を仮定するため、外側の反転をここだけ打ち消す
        ctx.textPosition = .zero
        CTLineDraw(line, ctx)
        ctx.restoreGState()
    }

    // MARK: - 色

    static func color(_ hex: String, alpha: CGFloat = 1) -> CGColor {
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
