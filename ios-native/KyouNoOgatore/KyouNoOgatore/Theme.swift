//
//  Theme.swift
//  KyouNoOgatore
//
//  ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md
//  §「Web版の正本（デザイントークン」): index.htmlのCSS変数(:root/body.dark)から抽出した値をそのまま
//  定義する(Android版Theme.ktと同一の値)。独自解釈でのアレンジはしない。

import Combine
import CoreText
import SwiftUI

// index.html:75-77(ライト)・118-...(body.dark、上書き値)の1:1移植。
struct KyonoColors {
    let yellow: Color
    let yellowSoft: Color
    let ink: Color
    let sub: Color
    let sub2: Color
    let subFaint: Color
    let teal: Color
    let tealStrong: Color
    let tealSoft: Color
    let tealInk: Color
    let coral: Color
    let coralSoft: Color
    let pink: Color
    let pinkSoft: Color
    let bg: Color
    let card: Color
    let line: Color
    let btnPrimaryShadow: Color
    let tabbarIconOff: Color
    // GO-G1(5視点ワンループ): index.html:392,121 .tabbar button{color:var(--sub)}/
    // body.dark .tabbar button{color:#847D6C}の1:1移植。タブバーのアイコン輪郭線(SVGの
    // stroke="currentColor")はボタンのtext colorを継承しており、ダークモードでは--sub
    // (通常の二次テキスト色)ではなくこの専用の控えめな色に上書きされる。以前はこの輪郭線が
    // 全テーマ固定のColor(hex: 0x3A3A35)になっており、ダーク背景に対しコントラスト比1.0〜1.45
    // (実測)でほぼ見えなくなっていた欠落。
    let tabbarStrokeOff: Color
}

extension Color {
    init(hex: UInt32) {
        let r = Double((hex >> 16) & 0xFF) / 255
        let g = Double((hex >> 8) & 0xFF) / 255
        let b = Double(hex & 0xFF) / 255
        self.init(red: r, green: g, blue: b)
    }
}

let kyonoLightColors = KyonoColors(
    yellow: Color(hex: 0xFFD93B), yellowSoft: Color(hex: 0xFFF3C4), ink: Color(hex: 0x3A3A35),
    sub: Color(hex: 0x6E6B5F), sub2: Color(hex: 0x6B6857),
    // GO-G2(5視点ワンループ): index.html --sub-faint:#827F72 は背景(--bg:#FFFAF3)に対し実測
    // 3.87:1でWCAG AA(4.5:1)未達だった(ダーク側の#8C8676は背景#211E19に対し4.58:1で元々AA達成
    // 済み・変更なし)。#757267へ底上げし4.64:1を確保(色味は保ったまま明度のみ下げた)。
    subFaint: Color(hex: 0x757267),
    teal: Color(hex: 0x2BB3A3), tealStrong: Color(hex: 0x1E7B70), tealSoft: Color(hex: 0xDFF5F2),
    tealInk: Color(hex: 0x177065), coral: Color(hex: 0xFF8A70), coralSoft: Color(hex: 0xFFE8E2),
    pink: Color(hex: 0xE56A9A), pinkSoft: Color(hex: 0xFFEDF3),
    bg: Color(hex: 0xFFFAF3), card: Color(hex: 0xFFFFFF), line: Color(hex: 0xF2EADB),
    btnPrimaryShadow: Color(hex: 0xE8BE1E), tabbarIconOff: Color(hex: 0xC4BDA9),
    tabbarStrokeOff: Color(hex: 0x6E6B5F)
)

let kyonoDarkColors = KyonoColors(
    yellow: Color(hex: 0xFFD93B), yellowSoft: Color(hex: 0x3A3423), ink: Color(hex: 0xF2EDE1),
    sub: Color(hex: 0xB9B2A0), sub2: Color(hex: 0xC6BFAE), subFaint: Color(hex: 0x8C8676),
    teal: Color(hex: 0x2BB3A3), tealStrong: Color(hex: 0x1E7B70), tealSoft: Color(hex: 0x22403B),
    tealInk: Color(hex: 0x7BD0C4), coral: Color(hex: 0xFF8A70), coralSoft: Color(hex: 0x3A2A24),
    pink: Color(hex: 0xE56A9A), pinkSoft: Color(hex: 0x3A2730),
    bg: Color(hex: 0x211E19), card: Color(hex: 0x2C2822), line: Color(hex: 0x3D382F),
    btnPrimaryShadow: Color(hex: 0x8A6D00), tabbarIconOff: Color(hex: 0x3D382F),
    tabbarStrokeOff: Color(hex: 0x847D6C)
)

// index.html:95 .card{border-radius:var(--radius)}・--radius:22px の1:1移植。
let kyonoRadius: CGFloat = 22
let kyonoButtonRadius: CGFloat = 18

// TASK-C2-2026-07-27-auto-theme-time-rule.md: app-env.js applyTheme()の時刻判定
// (h>=19||h<5)の1:1移植。端末のローカル時刻(Web版のnew Date().getHours()と同じ)で判定する。
private func isAutoThemeDarkByTime() -> Bool {
    let hour = Calendar.current.component(.hour, from: Date())
    return hour >= 19 || hour < 5
}

// index.html:1157周辺 storeのtheme値("auto"/"light"/"dark")をシステムのダークモードと合成する。
// "auto"はOSのダーク設定 OR 時刻判定(19時〜朝5時)のどちらかで暗くなる(Web版と同じ)。
func resolveKyonoColors(themeSetting: String, systemColorScheme: ColorScheme) -> KyonoColors {
    let dark: Bool
    switch themeSetting {
    case "dark": dark = true
    case "light": dark = false
    default: dark = systemColorScheme == .dark || isAutoThemeDarkByTime()
    }
    return dark ? kyonoDarkColors : kyonoLightColors
}

private struct KyonoColorsKey: EnvironmentKey {
    static let defaultValue = kyonoLightColors
}

extension EnvironmentValues {
    var kyonoColors: KyonoColors {
        get { self[KyonoColorsKey.self] }
        set { self[KyonoColorsKey.self] = newValue }
    }
}

// TASK-C2-2026-07-27-text-size-accessibility.md: index.html:87 body.bigtext{zoom:1.18}の1:1移植。
// Web版は既定でbigtext=true(2026-07-12本人フィードバック・対象ユーザー50-60代)。
private struct KyonoBigTextKey: EnvironmentKey {
    static let defaultValue = true
}

extension EnvironmentValues {
    var kyonoBigText: Bool {
        get { self[KyonoBigTextKey.self] }
        set { self[KyonoBigTextKey.self] = newValue }
    }
}

let kyonoBigTextScale: CGFloat = 1.18

struct KyonoTheme<Content: View>: View {
    let themeSetting: String
    var bigText: Bool = true
    @ViewBuilder let content: () -> Content
    @Environment(\.colorScheme) private var systemColorScheme
    // TASK-C2-2026-07-27-auto-theme-time-rule.md: index.html:4017 setInterval(refreshDay,60000)の
    // 1:1移植。開いたまま19時/5時の境界をまたいでもテーマが追従するよう、フォアグラウンド中は
    // 60秒ごとに再評価する(tickを変更するとbodyが再評価され、resolveKyonoColorsが現在時刻を
    // 素通しで見直す)。
    @State private var tick = 0
    private let ticker = Timer.publish(every: 60, on: .main, in: .common).autoconnect()

    var body: some View {
        // SwiftUIは@Stateが変化するとそれを保持するView(このKyonoTheme)のbodyを再評価する
        // (Composeのようなプロパティ単位の読み取り追跡ではない)。tick自体をbody内で参照しなくても、
        // .onReceiveでのtick更新だけでresolveKyonoColors()の再評価(=現在時刻の再取得)が起きる。
        content()
            .environment(\.kyonoColors, resolveKyonoColors(themeSetting: themeSetting, systemColorScheme: systemColorScheme))
            .environment(\.kyonoBigText, bigText)
            // TASK-C2-2026-07-27-text-size-accessibility.md 項目3: アプリ側1.18倍+端末側Dynamic
            // Typeが両方効くと極端に大きくなりうるため、OS側の拡大に上限を設ける(検収基準の
            // 「アプリ大きめ+端末最大でもレイアウトが破綻しない」への対応)。
            .dynamicTypeSize(...DynamicTypeSize.accessibility2)
            .onReceive(ticker) { _ in tick += 1 }
    }
}

// フォント(CardRendererと同じmplus1p-700/800/900.ttf・banananum.otfをUI全体にも適用。
// §7bカード視覚アセットタスクで既にFonts/へ同梱済みのものを再利用)。CTFontManagerで都度登録すると
// 重複登録エラーになりうるため、プロセス内で一度だけ登録するフラグを持つ。
enum KyonoFontRegistration {
    private static var registered = false
    static func ensureRegistered() {
        guard !registered else { return }
        registered = true
        for name in ["mplus1p-700", "mplus1p-800", "mplus1p-900"] {
            registerFont(name: name, ext: "ttf")
        }
        registerFont(name: "banananum", ext: "otf")
    }
    private static func registerFont(name: String, ext: String) {
        guard let url = Bundle.main.url(forResource: name, withExtension: ext) else { return }
        CTFontManagerRegisterFontsForURL(url as CFURL, .process, nil)
    }
}

enum KyonoFontWeight {
    case bold700, extraBold800, black900, banana
}

extension Font {
    // TASK-C2-2026-07-27-text-size-accessibility.md 項目2: 固定サイズ版のFont.custom(_:size:)は
    // Dynamic Type(端末の文字サイズ設定)に追従しないため、relativeTo:付きの版に変更する。
    // サイズ→TextStyleの対応は「20以上→title2・15前後→body・12-13→caption」の妥当な範囲でよいと
    // 指示されている(タスク文面どおり)。
    private static func relativeStyle(for size: CGFloat) -> Font.TextStyle {
        switch size {
        case 20...: return .title2
        case 14..<20: return .body
        default: return .caption
        }
    }

    // CTFontManagerRegisterFontsForURLで登録した実ファイルのPostScript名はビルドごとに調べる手間があるため、
    // Bundle.main.url経由でCTFontDescriptorから直接名前を取得して使う(CardRenderer.swift CardFontsと同じ考え方)。
    static func kyono(_ weight: KyonoFontWeight, size: CGFloat) -> Font {
        KyonoFontRegistration.ensureRegistered()
        let name: String
        switch weight {
        case .bold700: name = "mplus1p-700"
        case .extraBold800: name = "mplus1p-800"
        case .black900: name = "mplus1p-900"
        case .banana: name = "banananum"
        }
        let ext = weight == .banana ? "otf" : "ttf"
        guard let url = Bundle.main.url(forResource: name, withExtension: ext),
              let descriptors = CTFontManagerCreateFontDescriptorsFromURL(url as CFURL) as? [CTFontDescriptor],
              let descriptor = descriptors.first else {
            return .system(size: size, weight: .bold)
        }
        let ctFont = CTFontCreateWithFontDescriptor(descriptor, size, nil)
        let psName = CTFontCopyPostScriptName(ctFont) as String
        return Font.custom(psName, size: size, relativeTo: relativeStyle(for: size))
    }
}

// TASK-C2-2026-07-27-text-size-accessibility.md 項目1: もじの大きさ(bigtext)設定を実際のフォント
// サイズへ反映する。既存の`.font(.kyono(weight, size:))`呼び出し(200箇所超)を1つずつ書き換える
// 代わりに、環境値`kyonoBigText`を読むViewModifierを新設し機械的に置換する
// (`.font(.kyono(w, size: n))` → `.kyonoFont(w, size: n)`)。
private struct KyonoFontModifier: ViewModifier {
    @Environment(\.kyonoBigText) private var bigText
    let weight: KyonoFontWeight
    let size: CGFloat

    func body(content: Content) -> some View {
        // GO-G12(5視点ワンループ): bigtext ON時に最小文字階層へフロアを入れる(既存の1.18倍
        // スケールでも14ptに届かない階層のみ)。非線形スケールの全面導入は保留(縮小版の指示どおり)。
        content.font(.kyono(weight, size: bigText ? max(size * kyonoBigTextScale, 14) : size))
    }
}

extension View {
    func kyonoFont(_ weight: KyonoFontWeight, size: CGFloat) -> some View {
        modifier(KyonoFontModifier(weight: weight, size: size))
    }
}
