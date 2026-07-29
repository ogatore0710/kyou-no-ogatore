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

// index.html:91-94,592-598 .logo(chara.png 52x52+タイトル+サブタイトル)の1:1移植。Web版はこの
// 要素がセクション切り替えの外側にある単一のグローバルヘッダーで、home/history/search/guideの
// 4タブすべての先頭に共通で出る。UI/UXパリティ監査GO-5(2026-07-28): ネイティブはホーム画面にしか
// 実装が無く、他3タブ(マイ記録・動画を探す・使い方)には出ていなかった欠落の修正。4画面とも
// このコンポーネント1つを呼ぶことで、以後のズレを構造的に防ぐ(seasonal mark<id="logoMark">は
// ネイティブ側に対応する仕組みが元々無く、このタスクのスコープ外)。
struct KyonoAppHeader: View {
    var body: some View {
        HStack(alignment: .center, spacing: 10) {
            KyonoCharaImage(name: "chara").frame(width: 52, height: 52)
            VStack(alignment: .leading, spacing: 1) {
                // UI/UXパリティ監査GO-11(2026-07-28・前倒し): index.html:88-89 h1{font-size:20px;
                // white-space:nowrap}の1:1移植。Web側は「22px→20pxへ意図的に縮小のうえnowrap」と
                // 明記されたコメントが残っており、マイ記録タブでG6(左右余白統一)前の幅ではこの
                // タイトルが実機で2行に折り返す実害が確認された。22ptのままlineLimit無指定だった
                // 欠落を修正する。
                KyonoSectionTitle("#きょうのオガトレ", size: 20).lineLimit(1)
                // index.html:94 .logosub{...white-space:nowrap}の1:1移植。
                KyonoBodyText("みんなで一緒にストレッチを習慣化").lineLimit(1)
            }
            Spacer()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

// TASK-C2-2026-07-27-fd-guide-ui-branch.md: index.html:216 fdBob(1.4s ease-in-out infinite・
// translateY 0↔5px)の1:1移植。はじめの1本ガイドの指差しヒント「👇 ここを押してみて」用。
struct FdBobText: View {
    @Environment(\.kyonoColors) private var colors
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    let text: String
    @State private var bob = false

    init(_ text: String) { self.text = text }

    var body: some View {
        Text(text)
            .kyonoFont(.black900, size: 15).foregroundColor(colors.pink)
            .frame(maxWidth: .infinity, alignment: .center)
            .offset(y: bob ? 5 : 0)
            .onAppear {
                // §D: index.html:214-220 fd-pointはprefers-reduced-motion:no-preference時のみ発火する。
                guard !reduceMotion else { return }
                withAnimation(.easeInOut(duration: 0.7).repeatForever(autoreverses: true)) { bob = true }
            }
    }
}

// index.html:95-96 .card{...border:1.5px solid var(--line);box-shadow:0 2px 10px
// rgba(160,140,80,.06)} / body.dark .card{box-shadow:none}の1:1移植。
// UI/UXパリティ監査GO-4(2026-07-28): 枠線・影とも欠落していた(ダークモードは
// Web版どおり影を出さず、枠線のみ)。
let kyonoCardShadowColor = Color(hex: 0xA08C50)

struct KyonoCard<Content: View>: View {
    @Environment(\.kyonoColors) private var colors
    // UI/UXパリティ監査GO-3(iOS・2026-07-29): index.html:87 body.bigtext{zoom:1.18}の1:1移植。
    // Android版はCompose LocalDensityの一括変換(density自体を1.18倍)で済むが、SwiftUIには
    // 対応する仕組みが無いため、共有部品ごとに@Environment(\.kyonoBigText)を読んで余白・角丸・
    // 枠線・影を手動で1.18倍する(フォントは既存の.kyonoFont()が既に1.18倍済みなので触らない)。
    @Environment(\.kyonoBigText) private var bigText
    @ViewBuilder let content: () -> Content

    private var dark: Bool { colors.bg == kyonoDarkColors.bg }
    private var zoom: CGFloat { bigText ? kyonoBigTextScale : 1 }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) { content() }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(20 * zoom)
            .background(colors.card)
            .cornerRadius(kyonoRadius * zoom)
            .overlay(RoundedRectangle(cornerRadius: kyonoRadius * zoom).stroke(colors.line, lineWidth: 1.5 * zoom))
            .shadow(color: dark ? .clear : kyonoCardShadowColor.opacity(0.06), radius: 10 * zoom, x: 0, y: 2 * zoom)
    }
}

// index.html:107-110,115-118 .grad-warm/.grad-mint/.grad-pink/.grad-softの1:1移植。診断結果・
// ホームの一部カードなど「白一色ではない」目立たせカードに使う斜めグラデーション背景。
enum KyonoGradient { case warm, mint, pink, soft }

struct KyonoGradientCard<Content: View>: View {
    @Environment(\.kyonoColors) private var colors
    @Environment(\.colorScheme) private var systemColorScheme
    // UI/UXパリティ監査GO-3(iOS・2026-07-29): KyonoCardと同じズーム対応。
    @Environment(\.kyonoBigText) private var bigText
    let gradient: KyonoGradient
    @ViewBuilder let content: () -> Content

    var body: some View {
        let dark = colors.bg == kyonoDarkColors.bg
        let zoom: CGFloat = bigText ? kyonoBigTextScale : 1
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
            .padding(20 * zoom)
            .background(LinearGradient(colors: [from, to], startPoint: .topLeading, endPoint: .bottomTrailing))
            .cornerRadius(kyonoRadius * zoom)
            .overlay(RoundedRectangle(cornerRadius: kyonoRadius * zoom).stroke(colors.line, lineWidth: 1.5 * zoom))
            .shadow(color: dark ? .clear : kyonoCardShadowColor.opacity(0.06), radius: 10 * zoom, x: 0, y: 2 * zoom)
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
        Text(text).kyonoFont(.black900, size: size).foregroundColor(colors.ink)
    }
}

struct KyonoBodyText: View {
    @Environment(\.kyonoColors) private var colors
    let text: String
    init(_ text: String) { self.text = text }
    var body: some View {
        Text(text).kyonoFont(.bold700, size: 15).foregroundColor(colors.sub)
    }
}

struct KyonoStreakText: View {
    @Environment(\.kyonoColors) private var colors
    let total: Int
    let streakCount: Int
    // TASK-C2-2026-07-28-myrecord-settings-tour-parity.md §1: app-record.js:48の1:1移植。
    // 数日あいて券でもつなげない時は、古い連続を見せない(押した瞬間に消えたと誤解させない)。
    var brokenNow: Bool = false
    init(_ total: Int, streakCount: Int, brokenNow: Bool = false) {
        self.total = total; self.streakCount = streakCount; self.brokenNow = brokenNow
    }
    var body: some View {
        Text("通算 \(total) 日" + (brokenNow ? "・きょうやると新しい章のスタート🌱" : (streakCount >= 2 ? "・いま\(streakCount)日連続" : "")))
            .kyonoFont(.black900, size: 20)
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
    // UI/UXパリティ監査GO-3(iOS・2026-07-29): KyonoCardと同じズーム対応。
    @Environment(\.kyonoBigText) private var bigText
    let text: String
    // TASK-C2-2026-07-30-icon-system.md(I): 見出し・ボタンの絵文字をKyonoIconGlyph(タブバーと
    // 同じ手描き風Canvas意匠)へ置き換えるための差し込み口。nilなら従来どおりテキストのみ。
    var icon: KyonoIcon? = nil
    let action: () -> Void
    var enabled: Bool = true
    // index.html:382 .done-btn.did{background:var(--line);color:var(--sub);box-shadow:none;
    // font-size:14px}の1:1移植。UI/UXパリティ監査GO-8(2026-07-28): 完了後も黄色+3D影のまま
    // opacity 0.5にするだけで、Webの「フラットな灰色化=もう押せない見た目」になっていなかった
    // 欠落。「きょうやった!」ボタンだけtrueを渡し、完了後はグレー1枚のフラット表示に切り替える
    // (他の呼び出し元=相談室の送信ボタン等は従来どおりの半透明ディムのままでよいため既定false)。
    var flatWhenDisabled = false
    @State private var pressed = false

    init(_ text: String, icon: KyonoIcon? = nil, enabled: Bool = true, flatWhenDisabled: Bool = false, action: @escaping () -> Void) {
        self.text = text; self.icon = icon; self.enabled = enabled; self.flatWhenDisabled = flatWhenDisabled; self.action = action
    }

    private var zoom: CGFloat { bigText ? kyonoBigTextScale : 1 }

    var body: some View {
        if flatWhenDisabled && !enabled {
            Text(text).kyonoFont(.black900, size: 14).foregroundColor(colors.sub)
                .padding(.horizontal, 18 * zoom).padding(.vertical, 16 * zoom)
                .frame(maxWidth: .infinity)
                .background(colors.line)
                .cornerRadius(kyonoButtonRadius * zoom)
        } else {
            let shadowOffset: CGFloat = (pressed ? 1 : 4) * zoom
            let faceOffset: CGFloat = (pressed ? 3 : 0) * zoom
            let alpha: Double = enabled ? 1 : 0.5
            ZStack {
                // TASK-C2-2026-07-27-text-size-accessibility.md 項目4: このTextは見た目上の高さ調整だけの
                // 複製で本文と同一内容のため、.accessibilityHidden(true)で読み上げ対象から外す(無いと
                // VoiceOverが同じラベルを2回読み上げてしまっていた)。
                Text(text).kyonoFont(.black900, size: 20).foregroundColor(.clear)
                    .padding(.horizontal, 18 * zoom).padding(.vertical, 16 * zoom)
                    .frame(maxWidth: .infinity)
                    .background(colors.btnPrimaryShadow.opacity(alpha))
                    .cornerRadius(kyonoButtonRadius * zoom)
                    .offset(y: shadowOffset)
                    .accessibilityHidden(true)
                // B1(2026-07-29): 黄色背景の文字はcolors.inkではなくcolors.yellowInk(ライト値
                // 固定)を使う。ダークモードでcolors.inkが反転しても黄色背景の上では常に濃い文字色のまま。
                HStack(spacing: 6 * zoom) {
                    if let icon {
                        // 黄色背景の上に乗るアイコンなので、塗り(fill)は無し(背景の黄色がそのまま
                        // 透ける)にして、線(stroke・タブバーと同じinkColor固定)とアクセント
                        // (yellowInk)だけで形を見せる。
                        KyonoIconGlyph(icon: icon, fill: .clear, accent: colors.yellowInk)
                            .frame(width: 20 * zoom, height: 20 * zoom)
                    }
                    Text(text)
                }
                .kyonoFont(.black900, size: 20).foregroundColor(colors.yellowInk)
                    .padding(.horizontal, 18 * zoom).padding(.vertical, 16 * zoom)
                    .frame(maxWidth: .infinity)
                    .background(colors.yellow.opacity(alpha))
                    .cornerRadius(kyonoButtonRadius * zoom)
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
}

// index.html:103,105 .btn-ghost{background:var(--teal-soft);color:var(--tealink);font-size:15px}
// / .btn-ghost:active{transform:translateY(1px);opacity:.85}の1:1移植。
// UI/UXパリティ監査GO-2(2026-07-28): .buttonStyle(.plain)がSwiftUI既定の押下ディムを消したまま
// 代替を入れていなかった(タップしても無反応に見える欠落)。KyonoPrimaryButtonと同じ
// DragGesture+@State pressedの手法をここにも展開する。
struct KyonoGhostButton: View {
    @Environment(\.kyonoColors) private var colors
    // UI/UXパリティ監査GO-3(iOS・2026-07-29): KyonoCardと同じズーム対応。
    @Environment(\.kyonoBigText) private var bigText
    let text: String
    let action: () -> Void
    @State private var pressed = false

    init(_ text: String, action: @escaping () -> Void) {
        self.text = text; self.action = action
    }

    private var zoom: CGFloat { bigText ? kyonoBigTextScale : 1 }

    var body: some View {
        Text(text).kyonoFont(.black900, size: 15).foregroundColor(colors.tealInk)
            .padding(.horizontal, 18 * zoom).padding(.vertical, 16 * zoom)
            .frame(maxWidth: .infinity)
            .background(colors.tealSoft)
            .cornerRadius(kyonoButtonRadius * zoom)
            .opacity(pressed ? 0.85 : 1)
            .offset(y: pressed ? 1 * zoom : 0)
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { _ in pressed = true }
                    .onEnded { _ in pressed = false; action() }
            )
    }
}

// KyonoGhostButtonと同じ見た目でNavigationLinkを包む版(遷移先画面があるメニュー項目用)。
struct KyonoGhostNavigationLink<Destination: View>: View {
    @Environment(\.kyonoColors) private var colors
    // UI/UXパリティ監査GO-3(iOS・2026-07-29): KyonoCardと同じズーム対応。
    @Environment(\.kyonoBigText) private var bigText
    let text: String
    @ViewBuilder let destination: () -> Destination

    init(_ text: String, @ViewBuilder destination: @escaping () -> Destination) {
        self.text = text; self.destination = destination
    }

    var body: some View {
        let zoom: CGFloat = bigText ? kyonoBigTextScale : 1
        NavigationLink { destination() } label: {
            Text(text).kyonoFont(.black900, size: 15).foregroundColor(colors.tealInk)
                .padding(.horizontal, 18 * zoom).padding(.vertical, 16 * zoom)
                .frame(maxWidth: .infinity)
                .background(colors.tealSoft)
                .cornerRadius(kyonoButtonRadius * zoom)
        }
        .buttonStyle(.plain)
    }
}

// index.html:104 .btn-line{background:none;border:2px solid #E0D5BE;color:var(--sub2);font-weight:800;font-size:15px}
// index.html:104,143 .btn-line{border:2px solid #E0D5BE}/body.dark .btn-line{border-color:#4A443A}
// の1:1移植。ダークモード再確認タスク(TASK-C2-2026-07-27-darkmode-recheck-and-nudges.md)で発覚:
// 従来ボーダー色がライト固定(0xE0D5BE)のままで、ダークモードでも同じ薄いベージュ色が出ていた
// (Web版はダークモード専用の暗い色に切り替わる)。
struct KyonoLineButton: View {
    @Environment(\.kyonoColors) private var colors
    // UI/UXパリティ監査GO-3(iOS・2026-07-29): KyonoCardと同じズーム対応。
    @Environment(\.kyonoBigText) private var bigText
    let text: String
    var enabled: Bool = true
    let action: () -> Void

    init(_ text: String, enabled: Bool = true, action: @escaping () -> Void) {
        self.text = text; self.enabled = enabled; self.action = action
    }

    private var dark: Bool { colors.bg == kyonoDarkColors.bg }
    private var zoom: CGFloat { bigText ? kyonoBigTextScale : 1 }
    @State private var pressed = false

    // index.html:104,105,143 .btn-line + .btn-line:active{transform:translateY(1px);opacity:.85}の
    // 1:1移植。UI/UXパリティ監査GO-2(2026-07-28): KyonoGhostButtonと同じ欠落・同じ対処。
    var body: some View {
        Text(text).kyonoFont(.extraBold800, size: 15).foregroundColor(colors.sub2)
            .padding(.horizontal, 18 * zoom).padding(.vertical, 16 * zoom)
            .frame(maxWidth: .infinity)
            .overlay(RoundedRectangle(cornerRadius: kyonoButtonRadius * zoom).stroke(Color(hex: dark ? 0x4A443A : 0xE0D5BE), lineWidth: 2 * zoom))
            .opacity(enabled ? (pressed ? 0.85 : 1) : 0.5)
            .offset(y: pressed ? 1 * zoom : 0)
            .contentShape(Rectangle())
            .disabled(!enabled)
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { _ in if enabled { pressed = true } }
                    .onEnded { _ in if enabled { pressed = false; action() } }
            )
    }
}

// index.html:372-376 .seg/.seg button/.seg button.on(セグメントコントロール)の1:1移植。
// 例: 設定画面の「画面のみため」「もじの大きさ」トグル。
struct KyonoSegmentedControl<T: Equatable>: View {
    @Environment(\.kyonoColors) private var colors
    // UI/UXパリティ監査GO-3(iOS・2026-07-29): KyonoCardと同じズーム対応。
    @Environment(\.kyonoBigText) private var bigText
    let options: [(T, String)]
    let selected: T
    let onSelect: (T) -> Void

    var body: some View {
        let zoom: CGFloat = bigText ? kyonoBigTextScale : 1
        HStack(spacing: 0) {
            ForEach(options.indices, id: \.self) { i in
                let (value, label) = options[i]
                let on = value == selected
                // index.html:373,432(相当) .seg button:not(.on):active{opacity:.6}の1:1移植。
                // UI/UXパリティ監査GO-2(2026-07-28): KyonoGhostButton/KyonoLineButtonと同じ欠落。
                // 選択中(on)のセグメントはWeb版でも:active対象外(not(.on))なのでそのまま。
                SegmentedOptionButton(label: label, on: on, colors: colors, zoom: zoom) { onSelect(value) }
            }
        }
        .padding(4 * zoom)
        .background(colors.line)
        .cornerRadius(16 * zoom)
    }
}

private struct SegmentedOptionButton: View {
    let label: String
    let on: Bool
    let colors: KyonoColors
    let zoom: CGFloat
    let action: () -> Void
    @State private var pressed = false

    var body: some View {
        Text(label).kyonoFont(.black900, size: 15).foregroundColor(on ? colors.ink : colors.sub)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 13 * zoom)
            .background(on ? colors.card : Color.clear)
            .cornerRadius(12 * zoom)
            .opacity(!on && pressed ? 0.6 : 1)
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { _ in pressed = true }
                    .onEnded { _ in pressed = false; action() }
            )
    }
}

// TASK-C2-2026-07-27-chips-overflow-and-bubble-pop.md §3: index.html:3079,3085,4149 .sd-pop
// (opacity0→1・translateY(4px)→0)の1:1移植。相談室の吹き出し・タイピング行・オンボのチャット
// 吹き出しで共用する。
private struct SdPopModifier: ViewModifier {
    let opacity: Double
    let offsetY: CGFloat
    func body(content: Content) -> some View {
        content.opacity(opacity).offset(y: offsetY)
    }
}

extension AnyTransition {
    static var sdPop: AnyTransition {
        .modifier(
            active: SdPopModifier(opacity: 0, offsetY: 4),
            identity: SdPopModifier(opacity: 1, offsetY: 0)
        )
    }
}

// TASK-C2-2026-07-27-chips-overflow-and-bubble-pop.md §1: index.html:470-474,3190-3198
// sdChipsFadeUpdate()の1:1移植。横スクロールするチップ列(相談室フッターのチップ行・検索画面の
// カテゴリ行)にだけ、右端にまだ続きがあることを示すフェード+「›」ヒントを重ねる。hasMore判定は
// Web版の「scrollWidth-scrollLeft-clientWidth>8」と同じ考え方をGeometryReaderで再現する。
private struct FadingChipScrollOffsetKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}

private struct FadingChipContentWidthKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}

struct FadingChipRow<Content: View>: View {
    @Environment(\.kyonoColors) private var colors
    let spacing: CGFloat
    @ViewBuilder let content: () -> Content

    @State private var offsetX: CGFloat = 0
    @State private var contentWidth: CGFloat = 0
    @State private var containerWidth: CGFloat = 0

    init(spacing: CGFloat = 8, @ViewBuilder content: @escaping () -> Content) {
        self.spacing = spacing
        self.content = content
    }

    private var hasMore: Bool { contentWidth - (-offsetX) - containerWidth > 8 }

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: spacing) { content() }
                .background(
                    GeometryReader { proxy in
                        Color.clear
                            .preference(key: FadingChipScrollOffsetKey.self, value: proxy.frame(in: .named("fadingChipRow")).minX)
                            .preference(key: FadingChipContentWidthKey.self, value: proxy.size.width)
                    }
                )
        }
        .coordinateSpace(name: "fadingChipRow")
        .onPreferenceChange(FadingChipScrollOffsetKey.self) { offsetX = $0 }
        .onPreferenceChange(FadingChipContentWidthKey.self) { contentWidth = $0 }
        .background(
            GeometryReader { proxy in
                Color.clear.onAppear { containerWidth = proxy.size.width }
                    .onChange(of: proxy.size.width) { _, newValue in containerWidth = newValue }
            }
        )
        .overlay(alignment: .trailing) {
            if hasMore {
                ZStack(alignment: .trailing) {
                    LinearGradient(colors: [colors.card.opacity(0), colors.card], startPoint: .leading, endPoint: .trailing)
                        .frame(width: 40)
                    Circle()
                        .fill(colors.card)
                        .overlay(Circle().stroke(colors.line, lineWidth: 1))
                        .frame(width: 22, height: 22)
                        .overlay(Text("›").foregroundColor(colors.sub).font(.system(size: 14, weight: .black)))
                        .padding(.trailing, 2)
                }
                .frame(height: 42)
                .allowsHitTesting(false)
                .transition(.opacity)
            }
        }
        .animation(.easeOut(duration: 0.2), value: hasMore)
    }
}

// GO-G5(5視点ワンループ): ObuPreviewPopupView(ObuView.swift)のスクリム+タップで閉じるパターンを
// 記録カード各モーダル(HomeView/MyRecordView/BragView)へ横展開するための共通コンテナ。
// 以前は.sheet()(下からのシート・スワイプでしか閉じられない)を使っており、背景タップでは
// 閉じられなかった欠落。isPresentedがtrueの間だけ、暗いスクリム+タップで閉じる領域の上に
// contentを角丸カードとして重ねて表示する。
struct KyonoCardModalOverlay<Content: View>: View {
    @Environment(\.kyonoColors) private var colors
    let isPresented: Bool
    let onClose: () -> Void
    @ViewBuilder let content: () -> Content

    var body: some View {
        if isPresented {
            ZStack {
                Color.black.opacity(0.55).ignoresSafeArea()
                    .onTapGesture { onClose() }
                // TestFlight実機フィードバックD3(2026-07-29): index.html:1191 #cardModalBox
                // (padding:18px・高さ指定なし=内容ぴったり)の1:1移植。以前は.background()を
                // ScrollView自体に付けていたため、ScrollViewが(親のZStackがColor.black...
                // ignoresSafeArea()で画面いっぱいになる影響で)画面ほぼ全高まで広がり、実際の
                // 内容(画像+ボタン)より下に大きな空白ができていた。.background()をcontent()側に
                // 付け替え、カードの見た目の大きさを内容の実寸に戻す(ScrollView自体はオーバー
                // フロー時の保険としてそのまま残す)。
                ScrollView {
                    content()
                        .padding()
                        .background(RoundedRectangle(cornerRadius: 20).fill(colors.card))
                }
                .padding(24)
            }
            .transition(.opacity)
        }
    }
}
