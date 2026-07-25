//
//  KyonoIcons.swift
//  KyouNoOgatore
//
//  ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
//  Phase 3(§「やること」4「アイコン移植」): index.html全編の.sec-head内インラインSVG(手描き風アイコン)を
//  SwiftUI Canvas/Pathで1:1移植する(Android版KyonoIcons.ktと同一ロジック)。SF Symbols代替は不可
//  (タスク文の明示的な指示)。全24箇所のsec-headは形状で見ると14種類に集約される。
//  KyonoSectionHeader(icon:title:fill:accent:)がsec-head全体(アイコン+タイトルの横並び)を1:1移植する。

import SwiftUI

private let inkColor = Color(hex: 0x3A3A35)

enum KyonoIcon {
    case clock, question, quizCheck, soudanBubble, obuBubble, play, calendarCheck
    case dexBook, heart, envelope, notes, mountainCheck, shieldCheck, star
}

struct KyonoSectionHeader: View {
    @Environment(\.kyonoColors) private var colors
    let icon: KyonoIcon
    let title: String
    let fill: Color
    var accent: Color = Color(hex: 0xE56A9A)

    var body: some View {
        HStack(spacing: 8) {
            KyonoIconGlyph(icon: icon, fill: fill, accent: accent).frame(width: 24, height: 24)
            Text(title).font(.kyono(.black900, size: 16)).foregroundColor(colors.ink)
        }
    }
}

struct KyonoIconGlyph: View {
    @Environment(\.kyonoColors) private var colors
    let icon: KyonoIcon
    let fill: Color
    var accent: Color = Color(hex: 0xE56A9A)

    var body: some View {
        Canvas { ctx, size in
            let s = size.width / 24
            func pt(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: x * s, y: y * s) }
            switch icon {
            case .clock:
                let circle = Path(ellipseIn: CGRect(x: 3.5 * s, y: 3.5 * s, width: 17 * s, height: 17 * s))
                ctx.fill(circle, with: .color(fill))
                ctx.stroke(circle, with: .color(inkColor), lineWidth: 2.2 * s)
                var hand = Path()
                hand.move(to: pt(12, 7.5)); hand.addLine(to: pt(12, 12)); hand.addLine(to: pt(15, 14.5))
                ctx.stroke(hand, with: .color(inkColor), style: StrokeStyle(lineWidth: 2.2 * s, lineCap: .round))
            case .question:
                let circle = Path(ellipseIn: CGRect(x: 3.5 * s, y: 3.5 * s, width: 17 * s, height: 17 * s))
                ctx.fill(circle, with: .color(fill))
                ctx.stroke(circle, with: .color(inkColor), lineWidth: 2.2 * s)
                var q = Path()
                q.move(to: pt(9, 10))
                q.addQuadCurve(to: pt(12, 7), control: pt(9, 7))
                q.addQuadCurve(to: pt(15, 10), control: pt(15, 7))
                q.addQuadCurve(to: pt(12, 13), control: pt(15, 12))
                q.addLine(to: pt(12, 14))
                ctx.stroke(q, with: .color(inkColor), style: StrokeStyle(lineWidth: 2.2 * s, lineCap: .round))
                ctx.fill(Path(ellipseIn: CGRect(x: 11.7 * s, y: 16.7 * s, width: 0.6 * s, height: 0.6 * s)), with: .color(inkColor))
            case .quizCheck:
                let circle = Path(ellipseIn: CGRect(x: 4 * s, y: 4 * s, width: 13 * s, height: 13 * s))
                ctx.stroke(circle, with: .color(inkColor), lineWidth: 2.2 * s)
                var handle = Path()
                handle.move(to: pt(15.5, 15.5)); handle.addLine(to: pt(20, 20))
                ctx.stroke(handle, with: .color(inkColor), style: StrokeStyle(lineWidth: 2.2 * s, lineCap: .round))
                var cross = Path()
                cross.move(to: pt(8, 10.5)); cross.addLine(to: pt(13, 10.5))
                cross.move(to: pt(10.5, 8)); cross.addLine(to: pt(10.5, 13))
                ctx.stroke(cross, with: .color(accent), style: StrokeStyle(lineWidth: 2.2 * s, lineCap: .round))
            case .soudanBubble, .obuBubble:
                var bubble = Path()
                bubble.move(to: pt(4, 5.5)); bubble.addLine(to: pt(20, 5.5)); bubble.addLine(to: pt(20, 15.5))
                bubble.addLine(to: pt(10, 15.5)); bubble.addLine(to: pt(6, 19)); bubble.addLine(to: pt(6, 15.5))
                bubble.addLine(to: pt(4, 15.5)); bubble.closeSubpath()
                ctx.fill(bubble, with: .color(fill))
                ctx.stroke(bubble, with: .color(inkColor), style: StrokeStyle(lineWidth: 2.2 * s, lineJoin: .round))
                if case .soudanBubble = icon {
                    var lines = Path()
                    lines.move(to: pt(8.5, 9)); lines.addLine(to: pt(15.5, 9))
                    lines.move(to: pt(8.5, 12)); lines.addLine(to: pt(13, 12))
                    ctx.stroke(lines, with: .color(accent), style: StrokeStyle(lineWidth: 2.2 * s, lineCap: .round))
                }
            case .play:
                let rect = Path(roundedRect: CGRect(x: 2.5 * s, y: 4.5 * s, width: 19 * s, height: 15 * s), cornerRadius: 4 * s)
                ctx.fill(rect, with: .color(fill))
                ctx.stroke(rect, with: .color(inkColor), lineWidth: 2.2 * s)
                var tri = Path()
                tri.move(to: pt(10, 9.2)); tri.addLine(to: pt(10, 14.8)); tri.addLine(to: pt(14.8, 12)); tri.closeSubpath()
                ctx.fill(tri, with: .color(accent))
            case .calendarCheck:
                let rect = Path(roundedRect: CGRect(x: 3 * s, y: 5 * s, width: 18 * s, height: 16 * s), cornerRadius: 3.5 * s)
                ctx.fill(rect, with: .color(fill))
                ctx.stroke(rect, with: .color(inkColor), lineWidth: 2.2 * s)
                var lines = Path()
                lines.move(to: pt(3, 9.5)); lines.addLine(to: pt(21, 9.5))
                lines.move(to: pt(8, 3)); lines.addLine(to: pt(8, 7))
                lines.move(to: pt(16, 3)); lines.addLine(to: pt(16, 7))
                ctx.stroke(lines, with: .color(inkColor), style: StrokeStyle(lineWidth: 2.2 * s, lineCap: .round))
                var check = Path()
                check.move(to: pt(8.5, 14.5)); check.addLine(to: pt(11, 17)); check.addLine(to: pt(15.5, 12))
                ctx.stroke(check, with: .color(accent), style: StrokeStyle(lineWidth: 2.2 * s, lineCap: .round, lineJoin: .round))
            case .dexBook:
                let rect = Path(roundedRect: CGRect(x: 3.5 * s, y: 4 * s, width: 17 * s, height: 16 * s), cornerRadius: 3 * s)
                ctx.fill(rect, with: .color(fill))
                ctx.stroke(rect, with: .color(inkColor), lineWidth: 2.2 * s)
                var lines = Path()
                lines.move(to: pt(12, 4)); lines.addLine(to: pt(12, 20))
                lines.move(to: pt(8, 8)); lines.addLine(to: pt(9.5, 8))
                lines.move(to: pt(8, 12)); lines.addLine(to: pt(9.5, 12))
                lines.move(to: pt(14.5, 8)); lines.addLine(to: pt(16, 8))
                lines.move(to: pt(14.5, 12)); lines.addLine(to: pt(16, 12))
                ctx.stroke(lines, with: .color(inkColor), style: StrokeStyle(lineWidth: 2.2 * s, lineCap: .round))
            case .heart:
                let rect = Path(roundedRect: CGRect(x: 3 * s, y: 5 * s, width: 18 * s, height: 15 * s), cornerRadius: 3.5 * s)
                ctx.fill(rect, with: .color(fill))
                ctx.stroke(rect, with: .color(inkColor), lineWidth: 2.2 * s)
                var heart = Path()
                heart.move(to: pt(12, 17))
                heart.addCurve(to: pt(7.9, 13.3), control1: pt(9.4, 15), control2: pt(7.9, 13.3))
                heart.addCurve(to: pt(8.4, 11.5), control1: pt(7.9, 13.3), control2: pt(8.4, 11.5))
                heart.addCurve(to: pt(11.65, 10.9), control1: pt(8.9, 9.5), control2: pt(10.9, 9.9))
                heart.addCurve(to: pt(14.5, 11.5), control1: pt(12, 9.9), control2: pt(14, 9.5))
                heart.addCurve(to: pt(15, 13.3), control1: pt(15, 13.3), control2: pt(15, 13.3))
                heart.addCurve(to: pt(12, 17), control1: pt(15, 13.3), control2: pt(13.5, 15))
                heart.closeSubpath()
                ctx.fill(heart, with: .color(accent))
            case .envelope:
                let rect = Path(roundedRect: CGRect(x: 3 * s, y: 5 * s, width: 18 * s, height: 15 * s), cornerRadius: 3.5 * s)
                ctx.fill(rect, with: .color(fill))
                ctx.stroke(rect, with: .color(inkColor), lineWidth: 2.2 * s)
                var chevron = Path()
                chevron.move(to: pt(3.5, 8)); chevron.addLine(to: pt(12, 13.5)); chevron.addLine(to: pt(20.5, 8))
                ctx.stroke(chevron, with: .color(inkColor), style: StrokeStyle(lineWidth: 2.2 * s, lineCap: .round, lineJoin: .round))
            case .notes:
                let rect = Path(roundedRect: CGRect(x: 4 * s, y: 3.5 * s, width: 16 * s, height: 17 * s), cornerRadius: 3 * s)
                ctx.fill(rect, with: .color(fill))
                ctx.stroke(rect, with: .color(inkColor), lineWidth: 2.2 * s)
                var lines = Path()
                lines.move(to: pt(8, 8.5)); lines.addLine(to: pt(16, 8.5))
                lines.move(to: pt(8, 12)); lines.addLine(to: pt(16, 12))
                lines.move(to: pt(8, 15.5)); lines.addLine(to: pt(13, 15.5))
                ctx.stroke(lines, with: .color(inkColor), style: StrokeStyle(lineWidth: 2.2 * s, lineCap: .round))
            case .mountainCheck:
                var mountain = Path()
                mountain.move(to: pt(3, 17)); mountain.addLine(to: pt(17, 3)); mountain.addLine(to: pt(21, 7)); mountain.addLine(to: pt(7, 21)); mountain.closeSubpath()
                ctx.fill(mountain, with: .color(fill))
                ctx.stroke(mountain, with: .color(inkColor), style: StrokeStyle(lineWidth: 2.2 * s, lineJoin: .round))
                var check = Path()
                check.move(to: pt(13, 7)); check.addLine(to: pt(17, 11))
                check.move(to: pt(7, 13)); check.addLine(to: pt(9, 15))
                check.move(to: pt(10, 10)); check.addLine(to: pt(12, 12))
                ctx.stroke(check, with: .color(inkColor), style: StrokeStyle(lineWidth: 2.2 * s, lineCap: .round))
            case .shieldCheck:
                var shield = Path()
                shield.move(to: pt(12, 3))
                shield.addLine(to: pt(19, 6))
                shield.addLine(to: pt(19, 12))
                shield.addCurve(to: pt(12, 21), control1: pt(19, 16.5), control2: pt(16, 19.5))
                shield.addCurve(to: pt(5, 12), control1: pt(8, 19.5), control2: pt(5, 16.5))
                shield.addLine(to: pt(5, 6))
                shield.closeSubpath()
                ctx.fill(shield, with: .color(fill))
                ctx.stroke(shield, with: .color(inkColor), style: StrokeStyle(lineWidth: 2.2 * s, lineJoin: .round))
                var check = Path()
                check.move(to: pt(8.5, 12)); check.addLine(to: pt(11, 14.5)); check.addLine(to: pt(15.5, 9.5))
                ctx.stroke(check, with: .color(accent), style: StrokeStyle(lineWidth: 2.2 * s, lineCap: .round, lineJoin: .round))
            case .star:
                var star = Path()
                star.move(to: pt(12, 3))
                star.addLine(to: pt(14.5, 8.5)); star.addLine(to: pt(20, 9)); star.addLine(to: pt(16, 13))
                star.addLine(to: pt(17, 19)); star.addLine(to: pt(12, 16)); star.addLine(to: pt(7, 19))
                star.addLine(to: pt(8, 13)); star.addLine(to: pt(4, 9)); star.addLine(to: pt(9.5, 8.5))
                star.closeSubpath()
                ctx.fill(star, with: .color(fill))
                ctx.stroke(star, with: .color(inkColor), style: StrokeStyle(lineWidth: 1.6 * s, lineJoin: .round))
            }
        }
    }
}
