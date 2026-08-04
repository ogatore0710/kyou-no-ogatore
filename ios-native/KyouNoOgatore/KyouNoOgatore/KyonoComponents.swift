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
            .kyonoFont(.black900, size: 15).foregroundColor(colors.pinkInk)
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
    private var zoom: CGFloat { bigText ? kyonoBigTextScale : kyonoNormalTextScale }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) { content() }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(20 * zoom)
            .background(colors.card)
            .cornerRadius(kyonoRadius * zoom)
            .overlay(RoundedRectangle(cornerRadius: kyonoRadius * zoom).stroke(colors.borderStrong, lineWidth: 1.5 * zoom))
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
        let zoom: CGFloat = bigText ? kyonoBigTextScale : kyonoNormalTextScale
        // TASK-C2-2026-08-05-build23-bg-tuning-and-tour-tap.md W-3(本人裁定「案a・彩度を立てる」):
        // ライトの新背景#F7EEDC(Z-5)に対しグラデ両端が同化していたため、色相を保ったまま彩度/明度を
        // 一段濃くした。本人の基準ペア(暖色系#FFF6D8→#FFE9A8・桃色系#FFE9F0→#FFD3E3)から明度比
        // 0.898(暖色)/0.955(桃色)を実測抽出し、平均レート0.9265を全8箇所へ均等適用(彩度はどちらの
        // 基準ペアもS=100%のため、ミント系(元々S=54.8%)も含め全端点をS=100%へ底上げ)。ダークは
        // 対象外(現状維持)。
        let (from, to): (Color, Color) = {
            switch gradient {
            case .warm: return dark ? (Color(hex: 0x37301C), Color(hex: 0x33232B)) : (Color(hex: 0xFFECA3), Color(hex: 0xFFC9DB))
            case .mint: return dark ? (Color(hex: 0x22403B), Color(hex: 0x33301C)) : (Color(hex: 0xBDFFE4), Color(hex: 0xFFF3B9))
            case .pink: return dark ? (Color(hex: 0x33232B), Color(hex: 0x33301C)) : (Color(hex: 0xFFC9DB), Color(hex: 0xFFF3B9))
            case .soft: return dark ? (Color(hex: 0x2C2822), Color(hex: 0x33232B)) : (Color(hex: 0xFFF6D0), Color(hex: 0xFFC9DB))
            }
        }()
        VStack(alignment: .leading, spacing: 0) { content() }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(20 * zoom)
            .background(LinearGradient(colors: [from, to], startPoint: .topLeading, endPoint: .bottomTrailing))
            .cornerRadius(kyonoRadius * zoom)
            .overlay(RoundedRectangle(cornerRadius: kyonoRadius * zoom).stroke(colors.borderStrong, lineWidth: 1.5 * zoom))
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
    // アイコン方針(TASK-C2-2026-07-30-icon-system.md)既存アイコン適用バッチ: KyonoSectionHeaderとは
    // 別に存在するこの軽量見出しにも、既存KyonoIconで意味が一致する呼び出し元だけアイコンを足す
    // (見出しは削除しない・付けられる先だけ付ける)。
    var icon: KyonoIcon? = nil
    init(_ text: String, size: CGFloat = 16, icon: KyonoIcon? = nil) { self.text = text; self.size = size; self.icon = icon }
    var body: some View {
        HStack(spacing: 6) {
            if let icon {
                KyonoIconGlyph(icon: icon, fill: .clear, accent: colors.pink).frame(width: size + 2, height: size + 2)
            }
            Text(text).kyonoFont(.black900, size: size).foregroundColor(colors.ink)
        }
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

// TASK-C2-2026-08-04-build22-yellow-return.md Z-7(本人カード裁定「案1・数字が主役」): 「通算N日」の
// 1行見出しを、記録カードと同じBanananum流儀の大きな数字を中央に主役配置する形へ再設計。
// 「通算」の言葉は本文からも全廃(呼び出し元の見出しごと変更)し、連続記録の付帯情報(いま○日連続/
// 新しい章のスタート)は数字の下の小さい1行へ格下げして情報は落とさない。
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
    private var subtitle: String? {
        brokenNow ? "きょうやると新しい章のスタート" : (streakCount >= 2 ? "いま\(streakCount)日連続" : nil)
    }
    var body: some View {
        VStack(spacing: 2) {
            HStack(alignment: .firstTextBaseline, spacing: 3) {
                Text("\(total)").kyonoFont(.banana, size: 56).foregroundColor(colors.pinkInk)
                Text("日").kyonoFont(.black900, size: 22).foregroundColor(colors.pinkInk)
            }
            if let subtitle {
                Text(subtitle).kyonoFont(.extraBold800, size: 13).foregroundColor(colors.pinkInk)
            }
        }
        .frame(maxWidth: .infinity, alignment: .center)
    }
}

struct KyonoBackgroundColor: View {
    @Environment(\.kyonoColors) private var colors
    var body: some View { colors.bg }
}

// TASK-C2-2026-08-02-build16-polish-and-ia.md P-3: ステータスバーのスクリム。タブ画面は
// ScrollViewのコンテンツがそのままステータスバー(時計・電波・電池)の裏まで素通しでスクロール
// してしまい、文字色によっては時計と重なって読みにくくなる欠陥の修正。上端にcolors.bg
// (ダークは焦げ茶)から透明への短いグラデーションを敷き、常に一定の読める背景を確保する。
// 全タブ共通の1コンポーネント(RootView.contentから1箇所だけ差し込む・タップは透過)。
struct KyonoStatusBarScrim: View {
    @Environment(\.kyonoColors) private var colors
    var body: some View {
        LinearGradient(colors: [colors.bg, colors.bg.opacity(0)], startPoint: .top, endPoint: .bottom)
            .frame(height: 56)
            .ignoresSafeArea(edges: .top)
            .allowsHitTesting(false)
    }
}

// index.html:99-102 .btn/.btn-primary(黄色背景+太字20px+下方向の立体シャドウ)の1:1移植。
// box-shadow:0 4px 0 #E8BE1E(ぼかし無しのオフセット矩形)をSwiftUI上でZStack二重描画により再現。
// :active時はtranslateY(3px)+shadow 1pxに縮む(押した感触)ため、押下状態を検知する。
// TASK-C2-2026-07-30-button-standard-migration.md(診断の案A・本人GO): DragGesture(minimumDistance:0)は
// 指をボタン外へずらしてから離してもaction()が発火してしまう欠陥(診断2)があったため、標準Button+
// ButtonStyle(configuration.isPressedで押下検知、外して離せば標準どおりキャンセルされる)へ移行。
// 押下時の見た目変化には.easeOut(duration:0.1)を付与(診断3・reduceMotion時は無演出)。
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

    init(_ text: String, icon: KyonoIcon? = nil, enabled: Bool = true, flatWhenDisabled: Bool = false, action: @escaping () -> Void) {
        self.text = text; self.icon = icon; self.enabled = enabled; self.flatWhenDisabled = flatWhenDisabled; self.action = action
    }

    private var zoom: CGFloat { bigText ? kyonoBigTextScale : kyonoNormalTextScale }

    var body: some View {
        if flatWhenDisabled && !enabled {
            // TASK-C2-2026-07-31-feedback-round2.md追加1件: bigtext時にラベルが長いと
            // (「✅ 1日目の記録をつけにいく」等)1行の幅に収まらず"…"で尻切れしていた欠落。
            // 折り返しを許可する(文言は変えない・Web版との1:1パリティを保つ)。
            Text(text).kyonoFont(.black900, size: 14).foregroundColor(colors.sub)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.horizontal, 18 * zoom).padding(.vertical, 16 * zoom)
                .frame(maxWidth: .infinity)
                .background(colors.line)
                .cornerRadius(kyonoButtonRadius * zoom)
        } else {
            let alpha: Double = enabled ? 1 : 0.5
            Button(action: action) {
                HStack(spacing: 6 * zoom) {
                    if let icon {
                        // TASK-C2-2026-08-04-build22-yellow-return.md Z-1: 黄背景の上に乗るアイコンな
                        // ので、塗り(fill)は無し(黄がそのまま透ける)にして、線(stroke)は主ボタン文字
                        // と同じ濃色固定で形を見せる。
                        KyonoIconGlyph(icon: icon, fill: .clear, accent: kyonoBtnPrimaryText)
                            .frame(width: 20 * zoom, height: 20 * zoom)
                    }
                    // 同上: 折り返し許可(文言短縮ではなくレイアウト側で対応)。
                    Text(text).multilineTextAlignment(.center).fixedSize(horizontal: false, vertical: true)
                }
                // Z-1(本人裁定「案B」): 黄背景は常に濃色文字固定(テーマ非依存・実測11.05:1)。
                .kyonoFont(.black900, size: 20).foregroundColor(kyonoBtnPrimaryText)
            }
            .buttonStyle(KyonoPrimaryButtonStyle(colors: colors, zoom: zoom, alpha: alpha))
            .disabled(!enabled)
        }
    }
}

private struct KyonoPrimaryButtonStyle: ButtonStyle {
    let colors: KyonoColors
    let zoom: CGFloat
    let alpha: Double
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func makeBody(configuration: Configuration) -> some View {
        let pressed = configuration.isPressed
        let shadowOffset: CGFloat = (pressed ? 1 : 4) * zoom
        let faceOffset: CGFloat = (pressed ? 3 : 0) * zoom
        ZStack {
            // TASK-C2-2026-07-27-text-size-accessibility.md 項目4: このラベルは見た目上の高さ調整だけの
            // 複製で本文と同一内容のため、.accessibilityHidden(true)で読み上げ対象から外す(無いと
            // VoiceOverが同じラベルを2回読み上げてしまっていた)。
            configuration.label.foregroundColor(.clear)
                .padding(.horizontal, 18 * zoom).padding(.vertical, 16 * zoom)
                .frame(maxWidth: .infinity)
                .background(colors.btnPrimaryShadow.opacity(alpha))
                .cornerRadius(kyonoButtonRadius * zoom)
                .offset(y: shadowOffset)
                .accessibilityHidden(true)
            configuration.label
                .padding(.horizontal, 18 * zoom).padding(.vertical, 16 * zoom)
                .frame(maxWidth: .infinity)
                .background(colors.yellow.opacity(alpha))
                .cornerRadius(kyonoButtonRadius * zoom)
                // TASK-C2-2026-08-04-build22-yellow-return.md Z-1: 案B新設の縁(2pt・実測vs背景4.74:1・
                // vs黄面3.57:1)。
                .overlay(RoundedRectangle(cornerRadius: kyonoButtonRadius * zoom).stroke(kyonoBtnPrimaryBorder.opacity(alpha), lineWidth: 2 * zoom))
                .offset(y: faceOffset)
        }
        .contentShape(Rectangle())
        .animation(reduceMotion ? nil : .easeOut(duration: 0.1), value: pressed)
    }
}

// index.html:103,105 .btn-ghost{background:var(--teal-soft);color:var(--tealink);font-size:15px}
// / .btn-ghost:active{transform:translateY(1px);opacity:.85}の1:1移植。
// UI/UXパリティ監査GO-2(2026-07-28): .buttonStyle(.plain)がSwiftUI既定の押下ディムを消したまま
// 代替を入れていなかった(タップしても無反応に見える欠落)。
// TASK-C2-2026-07-30-button-standard-migration.md: DragGesture+@State pressed方式は指を外して
// 離してもaction()が発火する欠陥(診断2)があったため、標準Button+ButtonStyleへ移行。
struct KyonoGhostButton: View {
    @Environment(\.kyonoColors) private var colors
    // UI/UXパリティ監査GO-3(iOS・2026-07-29): KyonoCardと同じズーム対応。
    @Environment(\.kyonoBigText) private var bigText
    let text: String
    // UX13案・案8(2026-07-30): KyonoLineButtonと同じ差し込み口。ボタン用途の残存絵文字を
    // Canvasアイコンへ置き換えるための穴。nilなら従来どおりテキストのみ。
    var icon: KyonoIcon? = nil
    let action: () -> Void

    init(_ text: String, icon: KyonoIcon? = nil, action: @escaping () -> Void) {
        self.text = text; self.icon = icon; self.action = action
    }

    private var zoom: CGFloat { bigText ? kyonoBigTextScale : kyonoNormalTextScale }
    private var dark: Bool { colors.bg == kyonoDarkColors.bg }
    // TASK-C2-2026-08-04-build22-yellow-return.md Z-2(本人裁定「案B」): ライトはミント地#DFF5F2+
    // 文字#0F5A50(実測7.11:1)+枠#177065 2.5pt(実測5.71:1)へ復帰。ダークはbuild21から不変
    // (teal系のまま)。
    private var textColor: Color { dark ? colors.tealInk : Color(hex: 0x0F5A50) }

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6 * zoom) {
                if let icon {
                    KyonoIconGlyph(icon: icon, fill: .clear, accent: textColor).frame(width: 18 * zoom, height: 18 * zoom)
                }
                Text(text).kyonoFont(.black900, size: 15).foregroundColor(textColor)
            }
        }
        .buttonStyle(dark
            ? KyonoGhostButtonStyle(background: colors.tealSoft, borderColor: colors.tealStrong, borderWidth: 2, zoom: zoom)
            : KyonoGhostButtonStyle(background: Color(hex: 0xDFF5F2), borderColor: Color(hex: 0x177065), borderWidth: 2.5, zoom: zoom))
    }
}

private struct KyonoGhostButtonStyle: ButtonStyle {
    let background: Color
    let borderColor: Color
    var borderWidth: CGFloat = 2
    let zoom: CGFloat
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func makeBody(configuration: Configuration) -> some View {
        let pressed = configuration.isPressed
        configuration.label
            .padding(.horizontal, 18 * zoom).padding(.vertical, 16 * zoom)
            .frame(maxWidth: .infinity)
            .background(background)
            .cornerRadius(kyonoButtonRadius * zoom)
            .overlay(RoundedRectangle(cornerRadius: kyonoButtonRadius * zoom).stroke(borderColor, lineWidth: borderWidth))
            .opacity(pressed ? 0.85 : 1)
            .offset(y: pressed ? 1 * zoom : 0)
            .contentShape(Rectangle())
            .animation(reduceMotion ? nil : .easeOut(duration: 0.1), value: pressed)
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
        let zoom: CGFloat = bigText ? kyonoBigTextScale : kyonoNormalTextScale
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
    // TASK-C2-2026-07-30-icon-system.md(I): KyonoPrimaryButtonと同じ差し込み口。nilなら従来どおり
    // テキストのみ。
    var icon: KyonoIcon? = nil
    var enabled: Bool = true
    let action: () -> Void

    init(_ text: String, icon: KyonoIcon? = nil, enabled: Bool = true, action: @escaping () -> Void) {
        self.text = text; self.icon = icon; self.enabled = enabled; self.action = action
    }

    private var dark: Bool { colors.bg == kyonoDarkColors.bg }
    private var zoom: CGFloat { bigText ? kyonoBigTextScale : kyonoNormalTextScale }

    // index.html:104,105,143 .btn-line + .btn-line:active{transform:translateY(1px);opacity:.85}の
    // 1:1移植。UI/UXパリティ監査GO-2(2026-07-28): KyonoGhostButtonと同じ欠落・同じ対処。
    // TASK-C2-2026-07-30-button-standard-migration.md: 標準Button+ButtonStyleへ移行(理由はKyonoPrimaryButton参照)。
    // TASK-C2-2026-08-04-build22-yellow-return.md Z-2(本人裁定「案B」): ライトは文字・枠とも
    // #4A473D(実測8.95:1)に統一。ダークはalan5未指摘のため既存値(sub2文字・0x4A443A枠)を維持。
    private var textColor: Color { dark ? colors.sub2 : Color(hex: 0x4A473D) }

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6 * zoom) {
                if let icon {
                    KyonoIconGlyph(icon: icon, fill: .clear, accent: textColor).frame(width: 20 * zoom, height: 20 * zoom)
                }
                Text(text).kyonoFont(.extraBold800, size: 15).foregroundColor(textColor)
            }
        }
        // TASK-C2-2026-08-04-build22-yellow-return.md Z-6: 既知の枠線コントラスト不足(実測1.72:1)を
        // 根治。ダークはcolors.borderStrong(新背景比7.70:1)へ統一。
        .buttonStyle(KyonoLineButtonStyle(borderColor: dark ? colors.borderStrong : Color(hex: 0x4A473D), zoom: zoom, enabled: enabled))
        .disabled(!enabled)
    }
}

private struct KyonoLineButtonStyle: ButtonStyle {
    let borderColor: Color
    let zoom: CGFloat
    let enabled: Bool
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func makeBody(configuration: Configuration) -> some View {
        let pressed = configuration.isPressed
        configuration.label
            .padding(.horizontal, 18 * zoom).padding(.vertical, 16 * zoom)
            .frame(maxWidth: .infinity)
            .overlay(RoundedRectangle(cornerRadius: kyonoButtonRadius * zoom).stroke(borderColor, lineWidth: 2 * zoom))
            .opacity(enabled ? (pressed ? 0.85 : 1) : 0.5)
            .offset(y: pressed ? 1 * zoom : 0)
            .contentShape(Rectangle())
            .animation(reduceMotion ? nil : .easeOut(duration: 0.1), value: pressed)
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
    // UX13案・案6(2026-07-30): index.html:656-658 segMine/segAsa/segYoruのようにラベルの左に
    // アイコンが付く呼び出し元向けの差し込み口。既定は無指定(既存の画面のみため/もじの大きさ等は
    // 呼び出し側を変えずに済む)。
    var icon: (T) -> KyonoIcon? = { _ in nil }

    var body: some View {
        let zoom: CGFloat = bigText ? kyonoBigTextScale : kyonoNormalTextScale
        HStack(spacing: 0) {
            ForEach(options.indices, id: \.self) { i in
                let (value, label) = options[i]
                let on = value == selected
                // index.html:373,432(相当) .seg button:not(.on):active{opacity:.6}の1:1移植。
                // UI/UXパリティ監査GO-2(2026-07-28): KyonoGhostButton/KyonoLineButtonと同じ欠落。
                // 選択中(on)のセグメントはWeb版でも:active対象外(not(.on))なのでそのまま。
                SegmentedOptionButton(label: label, icon: icon(value), on: on, colors: colors, zoom: zoom) { onSelect(value) }
            }
        }
        .padding(4 * zoom)
        .background(colors.line)
        .cornerRadius(16 * zoom)
    }
}

// TASK-C2-2026-07-30-button-standard-migration.md: 標準Button+ButtonStyleへ移行(理由はKyonoPrimaryButton参照)。
private struct SegmentedOptionButton: View {
    let label: String
    var icon: KyonoIcon? = nil
    let on: Bool
    let colors: KyonoColors
    let zoom: CGFloat
    let action: () -> Void

    private var dark: Bool { colors.bg == kyonoDarkColors.bg }
    // TASK-C2-2026-08-04-build22-yellow-return.md Z-2(本人裁定「案B」): ライトの選択中は白ノブ+
    // 枠#6B6857 2pt+文字#26261F。ダークはbuild21から不変(card地+ink文字/sub未選択、枠なし)。
    private var onBg: Color { dark ? colors.card : .white }
    private var onText: Color { dark ? colors.ink : Color(hex: 0x26261F) }
    private var onBorder: Color? { dark ? nil : Color(hex: 0x6B6857) }
    private var offText: Color { dark ? colors.sub : Color(hex: 0x57544A) }

    var body: some View {
        Button(action: action) {
            HStack(spacing: 4 * zoom) {
                if let icon {
                    // segHeartのピンク薄塗り(fill #FFEDF3相当)以外は内部で色を固定描画するため、
                    // ここで渡すfillはsegHeart用の値でよい。
                    KyonoIconGlyph(icon: icon, fill: Color(hex: 0xFFEDF3)).frame(width: 16 * zoom, height: 16 * zoom)
                }
                // TASK-C2-2026-08-04-build20-addendum.md F-2②(検収差し戻し): よびな置換で
                // ラベルが長くなる「あなた用」タブが3行に折り返っていた。1行固定+自動縮小
                // (minimumScaleFactor)にして、短いラベル(あさ/よる等)には影響を与えず長い
                // ラベルだけ縮んで収まるようにする。
                Text(label).kyonoFont(.black900, size: 15).foregroundColor(on ? onText : offText)
                    .lineLimit(1).minimumScaleFactor(0.55)
            }
                .frame(maxWidth: .infinity)
                // TASK-C2-2026-08-04-build21-addendum.md Y-1(検収差し戻し): 横パディングが無く、
                // よびな6文字時に文字がピル右端へ接触していた。左右にも余白を持たせる。
                .padding(.horizontal, 10 * zoom)
                .padding(.vertical, 13 * zoom)
                .background(on ? onBg : Color.clear)
                .cornerRadius(12 * zoom)
                .overlay {
                    if on, let onBorder {
                        RoundedRectangle(cornerRadius: 12 * zoom).stroke(onBorder, lineWidth: 2 * zoom)
                    }
                }
        }
        .buttonStyle(KyonoSegmentedOptionButtonStyle(on: on))
    }
}

private struct KyonoSegmentedOptionButtonStyle: ButtonStyle {
    let on: Bool
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func makeBody(configuration: Configuration) -> some View {
        let pressed = configuration.isPressed
        configuration.label
            .opacity(!on && pressed ? 0.6 : 1)
            .contentShape(Rectangle())
            .animation(reduceMotion ? nil : .easeOut(duration: 0.1), value: pressed)
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
//
// TASK-C2-2026-08-02-build17-feedback-fixes.md P-5: 旧実装は.preference()+onPreferenceChange
// でcontentWidth/offsetXを伝えていたが、実機/シミュレータ検証でonPreferenceChangeが初回レイアウト
// パス(まだ最終サイズが確定していない時点)の値0で1度だけ発火した後、GeometryReaderのonAppearが
// 後から正しい値(実測5143pt等)を報告してもonPreferenceChangeが再発火しないSwiftUIの挙動を実測で
// 確認した(containerWidth側は元から.preference()を使わずonAppear/onChangeで直接@Stateへ書いて
// いたため無関係でこの欠陥を免れていた)。同じ「GeometryReaderのonAppear/onChangeで直接@Stateへ
// 書く」方式にoffsetX/contentWidthも揃えることで解消する。
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
    // TASK-C2-2026-08-02-build17-feedback-fixes.md P-5: 「左右どちらにもスクロール可能な状態が
    // 伝わること」の指示どおり、右端のフェード+矢印(既存)と対になる左端版を追加する。offsetXは
    // 左スクロール前は0、スクロールすると負の値になるので符号を反転してhasMoreと揃える。
    private var hasPrevious: Bool { -offsetX > 8 }

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: spacing) { content() }
                .background(
                    GeometryReader { proxy in
                        Color.clear
                            .onAppear {
                                offsetX = proxy.frame(in: .named("fadingChipRow")).minX
                                contentWidth = proxy.size.width
                            }
                            .onChange(of: proxy.frame(in: .named("fadingChipRow")).minX) { _, newValue in offsetX = newValue }
                            .onChange(of: proxy.size.width) { _, newValue in contentWidth = newValue }
                    }
                )
        }
        .coordinateSpace(name: "fadingChipRow")
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
                        .overlay(Circle().stroke(colors.borderStrong, lineWidth: 1))
                        .frame(width: 22, height: 22)
                        .overlay(Text("›").foregroundColor(colors.sub).font(.system(size: 14, weight: .black)))
                        .padding(.trailing, 2)
                }
                .frame(height: 42)
                .allowsHitTesting(false)
                .transition(.opacity)
            }
        }
        .overlay(alignment: .leading) {
            if hasPrevious {
                ZStack(alignment: .leading) {
                    LinearGradient(colors: [colors.card.opacity(0), colors.card], startPoint: .trailing, endPoint: .leading)
                        .frame(width: 40)
                    Circle()
                        .fill(colors.card)
                        .overlay(Circle().stroke(colors.borderStrong, lineWidth: 1))
                        .frame(width: 22, height: 22)
                        .overlay(Text("‹").foregroundColor(colors.sub).font(.system(size: 14, weight: .black)))
                        .padding(.leading, 2)
                }
                .frame(height: 42)
                .allowsHitTesting(false)
                .transition(.opacity)
            }
        }
        .animation(.easeOut(duration: 0.2), value: hasMore)
        .animation(.easeOut(duration: 0.2), value: hasPrevious)
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
    // TASK-C2-2026-08-03-build18-tutorial-quality.md B-1: 結果画面(ResultContentView)だけ、
    // 半透明スクリム(0.55)越しに背後の「静かな一行+ボタン」や各カードが透けて見え、モーダルの
    // カードと二重に見える欠陥があった(alan5実機報告IMG_8729)。P-1のOnboardingScrimと同じ
    // 「不透明色で完全に隠す」方針をここにも展開できるよう、既定値falseで既存呼び出し元
    // (HomeView/MyRecordView/BragView)の見た目は変えず、ResultContentViewの呼び出しだけ
    // trueにして不透明にする。
    var scrimOpaque: Bool = false
    @ViewBuilder let content: () -> Content

    var body: some View {
        if isPresented {
            ZStack {
                Group {
                    if scrimOpaque {
                        colors.bg
                    } else {
                        Color.black.opacity(0.55)
                    }
                }
                .ignoresSafeArea()
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

// TASK-C2-2026-07-31-build11-renshu-journey.md D(本丸): 練習モード(かたさチェック→けっか→
// どうが→きろく→カード)と使い方ツアーが共通で使う進捗バー。画面上部に固定表示する前提
// (呼び出し側がScrollViewの外・VStackの先頭に置く)。既存のクイズ/ツアーの「ドット」表示を
// この1部品に統一し、練習モードとツアーで見た目の一貫性を持たせる(本人の明示要求)。
struct KyonoJourneyBar: View {
    let labels: [String]
    let currentIndex: Int
    @Environment(\.kyonoColors) private var colors
    @Environment(\.kyonoBigText) private var bigText
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private var zoom: CGFloat { bigText ? kyonoBigTextScale : kyonoNormalTextScale }

    var body: some View {
        HStack(alignment: .top, spacing: 0) {
            ForEach(labels.indices, id: \.self) { i in
                let done = i < currentIndex
                let current = i == currentIndex
                VStack(spacing: 3 * zoom) {
                    ZStack {
                        Circle()
                            .fill(done || current ? colors.pink : colors.line)
                            .frame(width: 20 * zoom, height: 20 * zoom)
                        if done {
                            Text("✓").kyonoFont(.black900, size: 11).foregroundColor(.white)
                        } else {
                            Text("\(i + 1)").kyonoFont(.black900, size: 11)
                                .foregroundColor(current ? .white : colors.sub)
                        }
                    }
                    Text(labels[i]).kyonoFont(.black900, size: 12)
                        .foregroundColor(current ? colors.ink : colors.sub)
                        .lineLimit(1).minimumScaleFactor(0.7)
                }
                .frame(maxWidth: i == labels.count - 1 ? nil : .infinity)
                if i < labels.count - 1 {
                    Rectangle().fill(done ? colors.pink : colors.line)
                        .frame(height: 2 * zoom)
                        .frame(maxWidth: .infinity)
                        .padding(.top, 9 * zoom)
                }
            }
        }
        .padding(.horizontal, 16 * zoom).padding(.vertical, 10 * zoom)
        .background(colors.bg)
        .animation(reduceMotion ? nil : .easeOut(duration: 0.25), value: currentIndex)
    }
}
