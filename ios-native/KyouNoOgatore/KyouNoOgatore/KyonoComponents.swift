//
//  KyonoComponents.swift
//  KyouNoOgatore
//
//  ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md
//  §「やること」2「共通コンポーネント化」): index.html .card/.btn/.btn-primary/.btn-ghostの1:1移植
//  (Android版KyonoComponents.ktと同一ロジック)。

import SwiftUI

// フォント適用漏れ・キャラ/タイプ画像の欠落修正タスク(TASK-C2-2026-07-26-visual-parity-fonts-characters.md)
// §2 キャラクター画像: assets/chara*.pngをCharaArt/へ同梱済みの前提で、複数画面(相談室・オンボ・
// ホーム等)から共通で使えるオガトレくん画像コンポーネント。nameは拡張子なしのファイル名
// (例: "chara-hitokoto")。PBXFileSystemSynchronizedRootGroupがCharaArt/をビルド時にバンドル
// ルートへフラット化するため、subdirectory指定なしで探す(DexView.swift loadCardArtと同じ考え方)。
struct KyonoCharaImage: View {
    let name: String
    var body: some View {
        if let url = Bundle.main.url(forResource: name, withExtension: "png"),
           let uiImage = UIImage(contentsOfFile: url.path) {
            Image(uiImage: uiImage).resizable().scaledToFit()
        }
    }
}

// index.html:95 .card{background:var(--card);border-radius:var(--radius);padding:20px;margin-bottom:16px}
struct KyonoCard<Content: View>: View {
    @Environment(\.kyonoColors) private var colors
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 0) { content() }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(20)
            .background(colors.card)
            .cornerRadius(kyonoRadius)
    }
}

// index.html:107-110,115-118 .grad-warm/.grad-mint/.grad-pink/.grad-softの1:1移植。診断結果・
// ホームの一部カードなど「白一色ではない」目立たせカードに使う斜めグラデーション背景。
enum KyonoGradient { case warm, mint, pink, soft }

struct KyonoGradientCard<Content: View>: View {
    @Environment(\.kyonoColors) private var colors
    @Environment(\.colorScheme) private var systemColorScheme
    let gradient: KyonoGradient
    @ViewBuilder let content: () -> Content

    var body: some View {
        let dark = colors.bg == kyonoDarkColors.bg
        let (from, to): (Color, Color) = {
            switch gradient {
            case .warm: return dark ? (Color(hex: 0x37301C), Color(hex: 0x33232B)) : (Color(hex: 0xFFF3C4), Color(hex: 0xFFEDF3))
            case .mint: return dark ? (Color(hex: 0x22403B), Color(hex: 0x33301C)) : (Color(hex: 0xE7F8F1), Color(hex: 0xFFF9DC))
            case .pink: return dark ? (Color(hex: 0x33232B), Color(hex: 0x33301C)) : (Color(hex: 0xFFEDF3), Color(hex: 0xFFF9DC))
            case .soft: return dark ? (Color(hex: 0x2C2822), Color(hex: 0x33232B)) : (Color(hex: 0xFFFDF5), Color(hex: 0xFFEDF3))
            }
        }()
        VStack(alignment: .leading, spacing: 0) { content() }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(20)
            .background(LinearGradient(colors: [from, to], startPoint: .topLeading, endPoint: .bottomTrailing))
            .cornerRadius(kyonoRadius)
    }
}

// 見出し・本文テキスト・背景色: いずれも@Environment(\.kyonoColors)を自分で読む独立View構造体として
// 定義する(HomeView等の呼び出し側でcolorsをプロパティとして持たせると、KyonoThemeが設定する
// environmentの「子孫」にならず常定義時点の既定値=ライトになってしまうため。SwiftUIのenvironment
// 伝播はビュー階層上の位置で決まり、Swiftのクロージャのレキシカルスコープでは決まらない)。
struct KyonoSectionTitle: View {
    @Environment(\.kyonoColors) private var colors
    let text: String
    let size: CGFloat
    init(_ text: String, size: CGFloat = 16) { self.text = text; self.size = size }
    var body: some View {
        Text(text).font(.kyono(.black900, size: size)).foregroundColor(colors.ink)
    }
}

struct KyonoBodyText: View {
    @Environment(\.kyonoColors) private var colors
    let text: String
    init(_ text: String) { self.text = text }
    var body: some View {
        Text(text).font(.kyono(.bold700, size: 15)).foregroundColor(colors.sub)
    }
}

struct KyonoStreakText: View {
    @Environment(\.kyonoColors) private var colors
    let total: Int
    let streakCount: Int
    init(_ total: Int, streakCount: Int) { self.total = total; self.streakCount = streakCount }
    var body: some View {
        Text("通算 \(total) 日" + (streakCount >= 2 ? "・いま\(streakCount)日連続" : ""))
            .font(.kyono(.black900, size: 20))
            .foregroundColor(colors.pink)
    }
}

struct KyonoBackgroundColor: View {
    @Environment(\.kyonoColors) private var colors
    var body: some View { colors.bg }
}

// index.html:99-102 .btn/.btn-primary(黄色背景+太字20px+下方向の立体シャドウ)の1:1移植。
// box-shadow:0 4px 0 #E8BE1E(ぼかし無しのオフセット矩形)をSwiftUI上でZStack二重描画により再現。
// :active時はtranslateY(3px)+shadow 1pxに縮む(押した感触)ため、DragGesture(minimumDistance:0)で押下検知する。
struct KyonoPrimaryButton: View {
    @Environment(\.kyonoColors) private var colors
    let text: String
    let action: () -> Void
    var enabled: Bool = true
    @State private var pressed = false

    init(_ text: String, enabled: Bool = true, action: @escaping () -> Void) {
        self.text = text; self.enabled = enabled; self.action = action
    }

    var body: some View {
        let shadowOffset: CGFloat = pressed ? 1 : 4
        let faceOffset: CGFloat = pressed ? 3 : 0
        let alpha: Double = enabled ? 1 : 0.5
        ZStack {
            Text(text).font(.kyono(.black900, size: 20)).foregroundColor(.clear)
                .padding(.horizontal, 18).padding(.vertical, 16)
                .frame(maxWidth: .infinity)
                .background(colors.btnPrimaryShadow.opacity(alpha))
                .cornerRadius(kyonoButtonRadius)
                .offset(y: shadowOffset)
            Text(text).font(.kyono(.black900, size: 20)).foregroundColor(colors.ink)
                .padding(.horizontal, 18).padding(.vertical, 16)
                .frame(maxWidth: .infinity)
                .background(colors.yellow.opacity(alpha))
                .cornerRadius(kyonoButtonRadius)
                .offset(y: faceOffset)
        }
        .contentShape(Rectangle())
        .gesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in if enabled { pressed = true } }
                .onEnded { _ in
                    if enabled { pressed = false; action() }
                }
        )
    }
}

// index.html:103 .btn-ghost{background:var(--teal-soft);color:var(--tealink);font-size:15px}
struct KyonoGhostButton: View {
    @Environment(\.kyonoColors) private var colors
    let text: String
    let action: () -> Void

    init(_ text: String, action: @escaping () -> Void) {
        self.text = text; self.action = action
    }

    var body: some View {
        Button(action: action) {
            Text(text).font(.kyono(.black900, size: 15)).foregroundColor(colors.tealInk)
                .padding(.horizontal, 18).padding(.vertical, 16)
                .frame(maxWidth: .infinity)
                .background(colors.tealSoft)
                .cornerRadius(kyonoButtonRadius)
        }
        .buttonStyle(.plain)
    }
}

// KyonoGhostButtonと同じ見た目でNavigationLinkを包む版(遷移先画面があるメニュー項目用)。
struct KyonoGhostNavigationLink<Destination: View>: View {
    @Environment(\.kyonoColors) private var colors
    let text: String
    @ViewBuilder let destination: () -> Destination

    init(_ text: String, @ViewBuilder destination: @escaping () -> Destination) {
        self.text = text; self.destination = destination
    }

    var body: some View {
        NavigationLink { destination() } label: {
            Text(text).font(.kyono(.black900, size: 15)).foregroundColor(colors.tealInk)
                .padding(.horizontal, 18).padding(.vertical, 16)
                .frame(maxWidth: .infinity)
                .background(colors.tealSoft)
                .cornerRadius(kyonoButtonRadius)
        }
        .buttonStyle(.plain)
    }
}

// index.html:104 .btn-line{background:none;border:2px solid #E0D5BE;color:var(--sub2);font-weight:800;font-size:15px}
struct KyonoLineButton: View {
    @Environment(\.kyonoColors) private var colors
    let text: String
    let action: () -> Void

    init(_ text: String, action: @escaping () -> Void) {
        self.text = text; self.action = action
    }

    var body: some View {
        Button(action: action) {
            Text(text).font(.kyono(.extraBold800, size: 15)).foregroundColor(colors.sub2)
                .padding(.horizontal, 18).padding(.vertical, 16)
                .frame(maxWidth: .infinity)
                .overlay(RoundedRectangle(cornerRadius: kyonoButtonRadius).stroke(Color(hex: 0xE0D5BE), lineWidth: 2))
        }
        .buttonStyle(.plain)
    }
}

// index.html:372-376 .seg/.seg button/.seg button.on(セグメントコントロール)の1:1移植。
// 例: 設定画面の「画面のみため」「もじの大きさ」トグル。
struct KyonoSegmentedControl<T: Equatable>: View {
    @Environment(\.kyonoColors) private var colors
    let options: [(T, String)]
    let selected: T
    let onSelect: (T) -> Void

    var body: some View {
        HStack(spacing: 0) {
            ForEach(options.indices, id: \.self) { i in
                let (value, label) = options[i]
                let on = value == selected
                Button(action: { onSelect(value) }) {
                    Text(label).font(.kyono(.black900, size: 15)).foregroundColor(on ? colors.ink : colors.sub)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 13)
                        .background(on ? colors.card : Color.clear)
                        .cornerRadius(12)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(4)
        .background(colors.line)
        .cornerRadius(16)
    }
}
