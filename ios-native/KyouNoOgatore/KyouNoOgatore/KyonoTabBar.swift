//
//  KyonoTabBar.swift
//  KyouNoOgatore
//
//  ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md
//  §「コンポーネント様式」下部タブバー): index.html:389-397 .tabbar/1158-1164 <nav class="tabbar">の
//  1:1移植(Android版KyonoTabBar.ktと同一ロジック)。5項目(使い方/マイ記録/ホーム/再生リスト/
//  動画を探す)固定・選択時アイコンが黄色に塗りつぶされる。アイコンはSF Symbols/Material Iconsへの
//  置き換え不可(タスク文の明示的な指示)なので、index.htmlのインラインSVG(d属性)をSwiftUIのPathで
//  直接再現する(手描き風の意匠を保つ)。

import SwiftUI
import UIKit

enum KyonoTab {
    case guide, myRecord, home, catalog, search
}

struct KyonoTabBar: View {
    @Environment(\.kyonoColors) private var colors
    // UI/UXパリティ監査GO-3(iOS・2026-07-29): KyonoCardと同じズーム対応。
    @Environment(\.kyonoBigText) private var bigText
    let current: KyonoTab?
    let onSelect: (KyonoTab) -> Void

    private var dark: Bool { colors.bg == kyonoDarkColors.bg }
    private var zoom: CGFloat { bigText ? kyonoBigTextScale : 1 }

    var body: some View {
        HStack(spacing: 0) {
            tabItem(.guide, "使い方") { GuideIcon(fill: $0, stroke: $1) }
            tabItem(.myRecord, "マイ記録") { MyRecordIcon(fill: $0, stroke: $1) }
            tabItem(.home, "ホーム") { HomeIcon(fill: $0, stroke: $1) }
            tabItem(.catalog, "再生リスト") { CatalogIcon(fill: $0, stroke: $1) }
            tabItem(.search, "動画を探す") { SearchIcon(fill: $0, stroke: $1) }
        }
        .padding(.horizontal, 4 * zoom).padding(.vertical, 6 * zoom)
        // index.html:389-391 .tabbar{...background:rgba(255,255,255,.97);backdrop-filter:blur(8px);
        // border-top:1.5px solid var(--line)}/121 body.dark .tabbar{background:rgba(33,30,25,.97);
        // border-top-color:#3D382F}の1:1移植。UI/UXパリティ監査GO-7(2026-07-28): 半透明・
        // backdrop-blur・上部境界線とも欠落し不透明単色だった。SwiftUIのMaterialでbackdrop-filter
        // 相当のぼかしを実現し、その上に.97相当のほぼ不透明なティントを重ねる。
        // TestFlight実機フィードバックC1(2026-07-29): この背景にignoresSafeAreaが無く、
        // ホームインジケータ領域(下の安全領域)まで背景が伸びずに後ろが透けて黒い帯に見えていた
        // (G7で単色の不透明背景からMaterial+ティントへ差し替えた際の副作用)。背景だけ下端まで
        // 伸ばし、ボタン・文字(HStack本体のpadding/内容)は安全領域の内側のまま変えない。
        .background {
            ZStack {
                Rectangle().fill(.regularMaterial)
                Rectangle().fill(dark ? Color(hex: 0x211E19) : Color.white).opacity(0.97)
            }
            .ignoresSafeArea(edges: .bottom)
        }
        .overlay(alignment: .top) {
            Rectangle().fill(colors.line).frame(height: 1.5 * zoom)
        }
    }

    private func tabItem(_ tab: KyonoTab, _ label: String, @ViewBuilder icon: @escaping (Color, Color) -> some View) -> some View {
        let selected = current == tab
        // GO-G1: index.html:397,123 .tabbar button.on{color:var(--ink)}の1:1移植。輪郭線(stroke)は
        // 選択時=ink・非選択時=tabbarStrokeOff(テーマ別)。
        let stroke = selected ? colors.ink : colors.tabbarStrokeOff
        return Button(action: { onSelect(tab) }) {
            VStack(spacing: 2 * zoom) {
                icon(selected ? colors.yellow : colors.tabbarIconOff, stroke).frame(width: 24 * zoom, height: 24 * zoom)
                Text(label).kyonoFont(.black900, size: 12).foregroundColor(selected ? colors.ink : colors.sub)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 4 * zoom)
        }
        .buttonStyle(.plain)
        // TASK-C2-2026-07-27-text-size-accessibility.md 項目4: アイコン(装飾)とラベルを1つの
        // 読み上げ単位にまとめ、選択状態もあわせて伝える(Android版TabItemの対応と同じ)。
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}

// GO-G1(5視点ワンループ): 輪郭線(stroke)を呼び出し元(tabItem)から渡されたテーマ別の色にする
// (以前はColor(hex: 0x3A3A35)固定でダーク背景に対しコントラスト比1.0〜1.45だった欠落。詳細は
// Theme.swift tabbarStrokeOffのコメント参照)。

// index.html:1159 使い方(開いた本)
private struct GuideIcon: View {
    let fill: Color
    let stroke: Color
    var body: some View {
        Canvas { ctx, size in
            let s = size.width / 24
            var path = Path()
            path.move(to: CGPoint(x: 4 * s, y: 5.5 * s))
            path.addQuadCurve(to: CGPoint(x: 12 * s, y: 5.5 * s), control: CGPoint(x: 8 * s, y: 3.5 * s))
            path.addQuadCurve(to: CGPoint(x: 20 * s, y: 5.5 * s), control: CGPoint(x: 16 * s, y: 3.5 * s))
            path.addLine(to: CGPoint(x: 20 * s, y: 19 * s))
            path.addQuadCurve(to: CGPoint(x: 12 * s, y: 19 * s), control: CGPoint(x: 16 * s, y: 17 * s))
            path.addQuadCurve(to: CGPoint(x: 4 * s, y: 19 * s), control: CGPoint(x: 8 * s, y: 17 * s))
            path.closeSubpath()
            ctx.fill(path, with: .color(fill))
            ctx.stroke(path, with: .color(stroke), lineWidth: 1.6 * s)
            var spine = Path()
            spine.move(to: CGPoint(x: 12 * s, y: 5.5 * s))
            spine.addLine(to: CGPoint(x: 12 * s, y: 19 * s))
            ctx.stroke(spine, with: .color(stroke), lineWidth: 1.6 * s)
        }
    }
}

// index.html:1160 マイ記録(カレンダー+チェック)
private struct MyRecordIcon: View {
    let fill: Color
    let stroke: Color
    var body: some View {
        Canvas { ctx, size in
            let s = size.width / 24
            let rect = CGRect(x: 3 * s, y: 5 * s, width: 18 * s, height: 16 * s)
            let rounded = Path(roundedRect: rect, cornerRadius: 3.5 * s)
            ctx.fill(rounded, with: .color(fill))
            ctx.stroke(rounded, with: .color(stroke), lineWidth: 1.6 * s)
            var lines = Path()
            lines.move(to: CGPoint(x: 3 * s, y: 9.5 * s)); lines.addLine(to: CGPoint(x: 21 * s, y: 9.5 * s))
            lines.move(to: CGPoint(x: 8 * s, y: 3 * s)); lines.addLine(to: CGPoint(x: 8 * s, y: 7 * s))
            lines.move(to: CGPoint(x: 16 * s, y: 3 * s)); lines.addLine(to: CGPoint(x: 16 * s, y: 7 * s))
            ctx.stroke(lines, with: .color(stroke), lineWidth: 1.6 * s)
            var check = Path()
            check.move(to: CGPoint(x: 8.5 * s, y: 14.5 * s))
            check.addLine(to: CGPoint(x: 11 * s, y: 17 * s))
            check.addLine(to: CGPoint(x: 15.5 * s, y: 12 * s))
            ctx.stroke(check, with: .color(stroke), style: StrokeStyle(lineWidth: 1.8 * s, lineCap: .round, lineJoin: .round))
        }
    }
}

// index.html:1161 ホーム(家)
private struct HomeIcon: View {
    let fill: Color
    let stroke: Color
    @Environment(\.kyonoColors) private var colors
    var body: some View {
        Canvas { ctx, size in
            let s = size.width / 24
            var path = Path()
            path.move(to: CGPoint(x: 4 * s, y: 10.5 * s))
            path.addLine(to: CGPoint(x: 12 * s, y: 4 * s))
            path.addLine(to: CGPoint(x: 20 * s, y: 10.5 * s))
            path.addLine(to: CGPoint(x: 20 * s, y: 20 * s))
            path.addLine(to: CGPoint(x: 4 * s, y: 20 * s))
            path.closeSubpath()
            ctx.fill(path, with: .color(fill))
            ctx.stroke(path, with: .color(stroke), style: StrokeStyle(lineWidth: 1.6 * s, lineJoin: .round))
            let door = CGRect(x: 9.5 * s, y: 15 * s, width: 5 * s, height: 6 * s)
            ctx.fill(Path(door), with: .color(colors.card))
            ctx.stroke(Path(door), with: .color(stroke), lineWidth: 1.4 * s)
        }
    }
}

// index.html:1162 再生リスト
private struct CatalogIcon: View {
    let fill: Color
    let stroke: Color
    var body: some View {
        Canvas { ctx, size in
            let s = size.width / 24
            var handle = Path()
            handle.move(to: CGPoint(x: 6.5 * s, y: 4.5 * s)); handle.addLine(to: CGPoint(x: 17.5 * s, y: 4.5 * s))
            ctx.stroke(handle, with: .color(stroke), style: StrokeStyle(lineWidth: 1.6 * s, lineCap: .round))
            let rect = Path(roundedRect: CGRect(x: 3 * s, y: 7 * s, width: 14 * s, height: 13 * s), cornerRadius: 3 * s)
            ctx.fill(rect, with: .color(fill))
            ctx.stroke(rect, with: .color(stroke), lineWidth: 1.6 * s)
            var play = Path()
            play.move(to: CGPoint(x: 8.5 * s, y: 12 * s))
            play.addLine(to: CGPoint(x: 8.5 * s, y: 17 * s))
            play.addLine(to: CGPoint(x: 12.5 * s, y: 14.5 * s))
            play.closeSubpath()
            ctx.fill(play, with: .color(stroke))
        }
    }
}

// index.html:1163 動画を探す(虫眼鏡)
private struct SearchIcon: View {
    let fill: Color
    let stroke: Color
    var body: some View {
        Canvas { ctx, size in
            let s = size.width / 24
            let circle = Path(ellipseIn: CGRect(x: 4 * s, y: 4 * s, width: 13 * s, height: 13 * s))
            ctx.fill(circle, with: .color(fill))
            ctx.stroke(circle, with: .color(stroke), lineWidth: 1.7 * s)
            var handle = Path()
            handle.move(to: CGPoint(x: 15.5 * s, y: 15.5 * s)); handle.addLine(to: CGPoint(x: 20 * s, y: 20 * s))
            ctx.stroke(handle, with: .color(stroke), style: StrokeStyle(lineWidth: 1.9 * s, lineCap: .round))
        }
    }
}

// index.html:1166-1175 obuFab/soudanFabの1:1移植。円形・カラーボーダー3px・影付き。
// 見た目パリティ第2弾(TASK-C2-2026-07-26-visual-parity-round2.md §3): 絵文字だけではVoiceOverに
// 内容が伝わらないため、accessibilityLabelを追加(Web版には無いネイティブならではの上乗せ。
// 見た目・配色・タップ挙動は変更しない)。
//
// オガトレ通信FAB実写真化タスク(TASK-C2-2026-07-26-obu-fab-photo.md): index.html:1166-1167
// #obuFab(絵文字ではなくassets/obu-fab-photo.jpgの実写真・円形・黄色ボーダー)の1:1移植。
// photoResNameを渡したときだけ絵文字の代わりに写真を表示する(相談室FABは従来どおり絵文字のまま)。
struct KyonoFab: View {
    @Environment(\.kyonoColors) private var colors
    let emoji: String
    let borderColor: Color
    var accessibilityLabelText: String = ""
    var photoResName: String? = nil
    // TASK-C2-2026-07-27-obu-fab-preview-popup.md: index.html:255-256 .obu-dot(15px・pink・bg色2pxボーダー・
    // 右上にはみ出す配置)の1:1移植。trueのときだけ描画(obuFabのみで使用・soudanFabは常にfalse)。
    // Web版にはこの他「NEW📣」の吹き出しチップ(.obu-bubbletip)もあるが、位置指定が複雑な割に
    // 視覚的な付加情報がドット(未読の有無)と重複するため今回は見送り(Android版と同じ判断)。
    var badgeDot: Bool = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Group {
                if let photoResName {
                    // index.html:248 .obu-fab img{object-fit:cover}の1:1移植。
                    if let url = Bundle.main.url(forResource: photoResName, withExtension: "jpg"),
                       let uiImage = UIImage(contentsOfFile: url.path) {
                        Image(uiImage: uiImage).resizable().scaledToFill()
                    }
                } else {
                    Text(emoji).kyonoFont(.bold700, size: 22)
                }
            }
            .frame(width: 56, height: 56)
            .background(Circle().fill(colors.card))
            .clipShape(Circle())
            .overlay(Circle().stroke(borderColor, lineWidth: 3))
            .overlay(alignment: .topTrailing) {
                if badgeDot {
                    Circle().fill(colors.pink)
                        .frame(width: 15, height: 15)
                        .overlay(Circle().stroke(colors.bg, lineWidth: 2))
                        .offset(x: 1, y: -1)
                }
            }
            .shadow(color: .black.opacity(0.2), radius: 4, y: 2)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(accessibilityLabelText)
    }
}
