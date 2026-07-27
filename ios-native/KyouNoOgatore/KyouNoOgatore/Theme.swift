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
    sub: Color(hex: 0x6E6B5F), sub2: Color(hex: 0x6B6857), subFaint: Color(hex: 0x827F72),
    teal: Color(hex: 0x2BB3A3), tealStrong: Color(hex: 0x1E7B70), tealSoft: Color(hex: 0xDFF5F2),
    tealInk: Color(hex: 0x177065), coral: Color(hex: 0xFF8A70), coralSoft: Color(hex: 0xFFE8E2),
    pink: Color(hex: 0xE56A9A), pinkSoft: Color(hex: 0xFFEDF3),
    bg: Color(hex: 0xFFFAF3), card: Color(hex: 0xFFFFFF), line: Color(hex: 0xF2EADB),
    btnPrimaryShadow: Color(hex: 0xE8BE1E), tabbarIconOff: Color(hex: 0xC4BDA9)
)

let kyonoDarkColors = KyonoColors(
    yellow: Color(hex: 0xFFD93B), yellowSoft: Color(hex: 0x3A3423), ink: Color(hex: 0xF2EDE1),
    sub: Color(hex: 0xB9B2A0), sub2: Color(hex: 0xC6BFAE), subFaint: Color(hex: 0x8C8676),
    teal: Color(hex: 0x2BB3A3), tealStrong: Color(hex: 0x1E7B70), tealSoft: Color(hex: 0x22403B),
    tealInk: Color(hex: 0x7BD0C4), coral: Color(hex: 0xFF8A70), coralSoft: Color(hex: 0x3A2A24),
    pink: Color(hex: 0xE56A9A), pinkSoft: Color(hex: 0x3A2730),
    bg: Color(hex: 0x211E19), card: Color(hex: 0x2C2822), line: Color(hex: 0x3D382F),
    btnPrimaryShadow: Color(hex: 0x8A6D00), tabbarIconOff: Color(hex: 0x3D382F)
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

struct KyonoTheme<Content: View>: View {
    let themeSetting: String
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
        return Font.custom(psName, size: size)
    }
}
