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

enum KyonoTab {
    case guide, myRecord, home, catalog, search
}

private let strokeColor = Color(hex: 0x3A3A35)

struct KyonoTabBar: View {
    @Environment(\.kyonoColors) private var colors
    let current: KyonoTab
    let onSelect: (KyonoTab) -> Void

    var body: some View {
        HStack(spacing: 0) {
            tabItem(.guide, "使い方") { GuideIcon(fill: $0) }
            tabItem(.myRecord, "マイ記録") { MyRecordIcon(fill: $0) }
            tabItem(.home, "ホーム") { HomeIcon(fill: $0) }
            tabItem(.catalog, "再生リスト") { CatalogIcon(fill: $0) }
            tabItem(.search, "動画を探す") { SearchIcon(fill: $0) }
        }
        .padding(.horizontal, 4).padding(.vertical, 6)
        .background(colors.card)
    }

    private func tabItem(_ tab: KyonoTab, _ label: String, @ViewBuilder icon: @escaping (Color) -> some View) -> some View {
        let selected = current == tab
        return Button(action: { onSelect(tab) }) {
            VStack(spacing: 2) {
                icon(selected ? colors.yellow : colors.tabbarIconOff).frame(width: 24, height: 24)
                Text(label).font(.kyono(.black900, size: 12)).foregroundColor(selected ? colors.ink : colors.sub)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 4)
        }
        .buttonStyle(.plain)
    }
}

// index.html:1159 使い方(開いた本)
private struct GuideIcon: View {
    let fill: Color
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
            ctx.stroke(path, with: .color(strokeColor), lineWidth: 1.6 * s)
            var spine = Path()
            spine.move(to: CGPoint(x: 12 * s, y: 5.5 * s))
            spine.addLine(to: CGPoint(x: 12 * s, y: 19 * s))
            ctx.stroke(spine, with: .color(strokeColor), lineWidth: 1.6 * s)
        }
    }
}

// index.html:1160 マイ記録(カレンダー+チェック)
private struct MyRecordIcon: View {
    let fill: Color
    var body: some View {
        Canvas { ctx, size in
            let s = size.width / 24
            let rect = CGRect(x: 3 * s, y: 5 * s, width: 18 * s, height: 16 * s)
            let rounded = Path(roundedRect: rect, cornerRadius: 3.5 * s)
            ctx.fill(rounded, with: .color(fill))
            ctx.stroke(rounded, with: .color(strokeColor), lineWidth: 1.6 * s)
            var lines = Path()
            lines.move(to: CGPoint(x: 3 * s, y: 9.5 * s)); lines.addLine(to: CGPoint(x: 21 * s, y: 9.5 * s))
            lines.move(to: CGPoint(x: 8 * s, y: 3 * s)); lines.addLine(to: CGPoint(x: 8 * s, y: 7 * s))
            lines.move(to: CGPoint(x: 16 * s, y: 3 * s)); lines.addLine(to: CGPoint(x: 16 * s, y: 7 * s))
            ctx.stroke(lines, with: .color(strokeColor), lineWidth: 1.6 * s)
            var check = Path()
            check.move(to: CGPoint(x: 8.5 * s, y: 14.5 * s))
            check.addLine(to: CGPoint(x: 11 * s, y: 17 * s))
            check.addLine(to: CGPoint(x: 15.5 * s, y: 12 * s))
            ctx.stroke(check, with: .color(strokeColor), style: StrokeStyle(lineWidth: 1.8 * s, lineCap: .round, lineJoin: .round))
        }
    }
}

// index.html:1161 ホーム(家)
private struct HomeIcon: View {
    let fill: Color
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
            ctx.stroke(path, with: .color(strokeColor), style: StrokeStyle(lineWidth: 1.6 * s, lineJoin: .round))
            let door = CGRect(x: 9.5 * s, y: 15 * s, width: 5 * s, height: 6 * s)
            ctx.fill(Path(door), with: .color(colors.card))
            ctx.stroke(Path(door), with: .color(strokeColor), lineWidth: 1.4 * s)
        }
    }
}

// index.html:1162 再生リスト
private struct CatalogIcon: View {
    let fill: Color
    var body: some View {
        Canvas { ctx, size in
            let s = size.width / 24
            var handle = Path()
            handle.move(to: CGPoint(x: 6.5 * s, y: 4.5 * s)); handle.addLine(to: CGPoint(x: 17.5 * s, y: 4.5 * s))
            ctx.stroke(handle, with: .color(strokeColor), style: StrokeStyle(lineWidth: 1.6 * s, lineCap: .round))
            let rect = Path(roundedRect: CGRect(x: 3 * s, y: 7 * s, width: 14 * s, height: 13 * s), cornerRadius: 3 * s)
            ctx.fill(rect, with: .color(fill))
            ctx.stroke(rect, with: .color(strokeColor), lineWidth: 1.6 * s)
            var play = Path()
            play.move(to: CGPoint(x: 8.5 * s, y: 12 * s))
            play.addLine(to: CGPoint(x: 8.5 * s, y: 17 * s))
            play.addLine(to: CGPoint(x: 12.5 * s, y: 14.5 * s))
            play.closeSubpath()
            ctx.fill(play, with: .color(strokeColor))
        }
    }
}

// index.html:1163 動画を探す(虫眼鏡)
private struct SearchIcon: View {
    let fill: Color
    var body: some View {
        Canvas { ctx, size in
            let s = size.width / 24
            let circle = Path(ellipseIn: CGRect(x: 4 * s, y: 4 * s, width: 13 * s, height: 13 * s))
            ctx.fill(circle, with: .color(fill))
            ctx.stroke(circle, with: .color(strokeColor), lineWidth: 1.7 * s)
            var handle = Path()
            handle.move(to: CGPoint(x: 15.5 * s, y: 15.5 * s)); handle.addLine(to: CGPoint(x: 20 * s, y: 20 * s))
            ctx.stroke(handle, with: .color(strokeColor), style: StrokeStyle(lineWidth: 1.9 * s, lineCap: .round))
        }
    }
}

// index.html:1166-1175 obuFab/soudanFabの1:1移植。円形・カラーボーダー3px・影付き。
struct KyonoFab: View {
    @Environment(\.kyonoColors) private var colors
    let emoji: String
    let borderColor: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(emoji).font(.system(size: 22))
                .frame(width: 56, height: 56)
                .background(Circle().fill(colors.card))
                .overlay(Circle().stroke(borderColor, lineWidth: 3))
                .shadow(color: .black.opacity(0.2), radius: 4, y: 2)
        }
        .buttonStyle(.plain)
    }
}
