//
//  KyonoComponents.swift
//  KyouNoOgatore
//
//  ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md
//  §「やること」2「共通コンポーネント化」): index.html .card/.btn/.btn-primary/.btn-ghostの1:1移植
//  (Android版KyonoComponents.ktと同一ロジック)。

import SwiftUI

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

// index.html:99-102 .btn/.btn-primary(黄色背景+太字20px+下方向の立体シャドウ)の1:1移植。
// box-shadow:0 4px 0 #E8BE1E(ぼかし無しのオフセット矩形)をSwiftUI上でZStack二重描画により再現。
// :active時はtranslateY(3px)+shadow 1pxに縮む(押した感触)ため、DragGesture(minimumDistance:0)で押下検知する。
struct KyonoPrimaryButton: View {
    @Environment(\.kyonoColors) private var colors
    let text: String
    let action: () -> Void
    var enabled: Bool = true
    @State private var pressed = false

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

// index.html:104 .btn-line{background:none;border:2px solid #E0D5BE;color:var(--sub2);font-weight:800;font-size:15px}
struct KyonoLineButton: View {
    @Environment(\.kyonoColors) private var colors
    let text: String
    let action: () -> Void

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
