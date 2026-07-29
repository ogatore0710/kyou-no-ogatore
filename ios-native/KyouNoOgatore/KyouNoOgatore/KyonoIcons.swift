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
    // アイコン方針(TASK-C2-2026-07-30-icon-system.md)既存アイコン適用バッチ: タブバーの
    // 「動画を探す」(虫眼鏡)を検索欄の目印として再利用するため、KyonoTabBar.swiftの
    // SearchIconと同じ形状をここに複製する(タブバー本体は既存のまま・private structも
    // 変更しない)。「使い方」(開いた本)は`.guideBook`として一度追加したが、実際に見出しとして
    // 使う場所が無く(図鑑自体の見出しは既に`.dexBook`が担当)、未使用のenumケースを残すと
    // 混乱を招くため削除した(alan5判断・2026-07-30)。
    case search
    // アイコン方針(I) 新規描き起こしバッチ1(2026-07-30): 「記録のひっこし」の3ボタン
    // (📦記録をコピーする/📋自動で読みこむ/📥よみこむ)は隣接して並ぶため、判断①(3つとも
    // 別の絵にする)のとおり、意味が近くても見分けがつく3種類を個別に描く。
    case exportBox, clipboardPaste, importTray
    // アイコン方針(I) 新規描き起こしバッチ2(2026-07-30): 「お楽しみ・ごほうび」系3種
    // (👑節目ゴールドカード/🎉お楽しみ機能/🎫おやすみ券)を同じテーマでまとめて描く。
    case crownBadge, confettiBurst, ticketStub
    // アイコン方針(I) 新規描き起こしバッチ3・残り5種(2026-07-30): 🎯は当初「的(同心円)」を
    // 想定していたが、alan5指摘のとおり`.clock`/`.question`と同じ「丸+中身」の構図になり
    // 見分けが怪しくなるため、同心円をやめて旗(ゴールを立てる)に変更した。同じ理由で⏱も
    // 円形の腕時計ではなく砂時計(三角×2)にし、丸い意匠が増えすぎないようにする。
    case goalFlag, sprout, hourglassTime, phoneDevice, paletteArt
}

// UI/UXパリティ監査2巡目A3(2026-07-29): index.html:98 .sec-head svg{width:21px;height:21px}の
// 1:1移植。従来24pt固定だったため常時+14%大きく、図鑑・使い方・検索・マイ記録・ホームの
// 全セクション見出しに波及していた欠落を修正する。
struct KyonoSectionHeader: View {
    @Environment(\.kyonoColors) private var colors
    let icon: KyonoIcon
    let title: String
    let fill: Color
    var accent: Color = Color(hex: 0xE56A9A)

    var body: some View {
        HStack(spacing: 8) {
            KyonoIconGlyph(icon: icon, fill: fill, accent: accent).frame(width: 21, height: 21)
            Text(title).kyonoFont(.black900, size: 16).foregroundColor(colors.ink)
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
            case .search:
                let circle = Path(ellipseIn: CGRect(x: 4 * s, y: 4 * s, width: 13 * s, height: 13 * s))
                ctx.fill(circle, with: .color(fill))
                ctx.stroke(circle, with: .color(inkColor), lineWidth: 2.2 * s)
                var handle = Path()
                handle.move(to: pt(15.5, 15.5)); handle.addLine(to: pt(20, 20))
                ctx.stroke(handle, with: .color(inkColor), style: StrokeStyle(lineWidth: 2.2 * s, lineCap: .round))
            case .exportBox:
                let rect = Path(roundedRect: CGRect(x: 4 * s, y: 9 * s, width: 16 * s, height: 12 * s), cornerRadius: 1.5 * s)
                ctx.fill(rect, with: .color(fill))
                ctx.stroke(rect, with: .color(inkColor), lineWidth: 2.2 * s)
                var flap = Path()
                flap.move(to: pt(4, 9)); flap.addLine(to: pt(12, 4.5)); flap.addLine(to: pt(20, 9))
                ctx.stroke(flap, with: .color(inkColor), style: StrokeStyle(lineWidth: 2.2 * s, lineCap: .round, lineJoin: .round))
                var tape = Path()
                tape.move(to: pt(12, 9)); tape.addLine(to: pt(12, 21))
                tape.move(to: pt(4, 15)); tape.addLine(to: pt(20, 15))
                ctx.stroke(tape, with: .color(accent), style: StrokeStyle(lineWidth: 2.2 * s, lineCap: .round))
            case .clipboardPaste:
                let board = Path(roundedRect: CGRect(x: 4 * s, y: 5.5 * s, width: 16 * s, height: 15.5 * s), cornerRadius: 2 * s)
                ctx.fill(board, with: .color(fill))
                ctx.stroke(board, with: .color(inkColor), lineWidth: 2.2 * s)
                let clip = Path(roundedRect: CGRect(x: 9 * s, y: 3 * s, width: 6 * s, height: 3 * s), cornerRadius: 1.2 * s)
                ctx.fill(clip, with: .color(inkColor))
                var lines = Path()
                lines.move(to: pt(7, 11)); lines.addLine(to: pt(17, 11))
                lines.move(to: pt(7, 14.5)); lines.addLine(to: pt(15, 14.5))
                ctx.stroke(lines, with: .color(accent), style: StrokeStyle(lineWidth: 2.2 * s, lineCap: .round))
            case .importTray:
                var tray = Path()
                tray.move(to: pt(3, 14)); tray.addLine(to: pt(21, 14)); tray.addLine(to: pt(17.5, 20)); tray.addLine(to: pt(6.5, 20)); tray.closeSubpath()
                ctx.fill(tray, with: .color(fill))
                ctx.stroke(tray, with: .color(inkColor), style: StrokeStyle(lineWidth: 2.2 * s, lineJoin: .round))
                var shaft = Path()
                shaft.move(to: pt(12, 3)); shaft.addLine(to: pt(12, 12))
                ctx.stroke(shaft, with: .color(inkColor), style: StrokeStyle(lineWidth: 2.2 * s, lineCap: .round))
                var head = Path()
                head.move(to: pt(8, 8)); head.addLine(to: pt(12, 12)); head.addLine(to: pt(16, 8))
                ctx.stroke(head, with: .color(accent), style: StrokeStyle(lineWidth: 2.2 * s, lineCap: .round, lineJoin: .round))
            case .crownBadge:
                var crown = Path()
                crown.move(to: pt(4, 17)); crown.addLine(to: pt(4, 8)); crown.addLine(to: pt(8, 13))
                crown.addLine(to: pt(12, 6)); crown.addLine(to: pt(16, 13)); crown.addLine(to: pt(20, 8))
                crown.addLine(to: pt(20, 17)); crown.closeSubpath()
                ctx.fill(crown, with: .color(fill))
                ctx.stroke(crown, with: .color(inkColor), style: StrokeStyle(lineWidth: 2.2 * s, lineJoin: .round))
                var band = Path()
                band.move(to: pt(4, 17)); band.addLine(to: pt(20, 17))
                ctx.stroke(band, with: .color(inkColor), style: StrokeStyle(lineWidth: 2.2 * s, lineCap: .round))
                ctx.fill(Path(ellipseIn: CGRect(x: 10.7 * s, y: 4.7 * s, width: 2.6 * s, height: 2.6 * s)), with: .color(accent))
            case .confettiBurst:
                // alan5差し戻し(2026-07-30): 閉じた三角+点3つは「再生ボタン」に読める
                // (.playの三角と衝突)ため、筒の口を閉じずに開けた形に描き直す。
                // fill用は閉じたPath、stroke用は口側(遠い辺)を結ばない2本の別Pathにすることで、
                // 塗りは筒の形のまま・線は開いた口に見えるようにする。
                var hornFill = Path()
                hornFill.move(to: pt(5, 19)); hornFill.addLine(to: pt(9, 8)); hornFill.addLine(to: pt(17, 12)); hornFill.closeSubpath()
                ctx.fill(hornFill, with: .color(fill))
                var hornStroke = Path()
                hornStroke.move(to: pt(5, 19)); hornStroke.addLine(to: pt(9, 8))
                hornStroke.move(to: pt(5, 19)); hornStroke.addLine(to: pt(17, 12))
                ctx.stroke(hornStroke, with: .color(inkColor), style: StrokeStyle(lineWidth: 2.2 * s, lineCap: .round, lineJoin: .round))
                var confettiDots = Path()
                confettiDots.addEllipse(in: CGRect(x: 10 * s, y: 2 * s, width: 2 * s, height: 2 * s))
                confettiDots.addEllipse(in: CGRect(x: 20 * s, y: 7 * s, width: 2 * s, height: 2 * s))
                ctx.fill(confettiDots, with: .color(accent))
                var confettiLine = Path()
                confettiLine.move(to: pt(17, 3)); confettiLine.addLine(to: pt(19, 5))
                ctx.stroke(confettiLine, with: .color(accent), style: StrokeStyle(lineWidth: 1.8 * s, lineCap: .round))
            case .ticketStub:
                // alan5相談(2026-07-30): 左右のヘコミ(切り取り線)を試す。単純な丸角長方形+
                // 円の別パスによる打ち抜きではなく、外周パス自体に凹アーチを組み込む
                // (addQuadCurveで中心へ引き寄せる)ことで、通常のfill/strokeだけで
                // 背景色に依存せず正しく描ける。
                var ticket = Path()
                ticket.move(to: pt(3, 6))
                ticket.addLine(to: pt(21, 6))
                ticket.addLine(to: pt(21, 10))
                ticket.addQuadCurve(to: pt(21, 14), control: pt(18.5, 12))
                ticket.addLine(to: pt(21, 18))
                ticket.addLine(to: pt(3, 18))
                ticket.addLine(to: pt(3, 14))
                ticket.addQuadCurve(to: pt(3, 10), control: pt(5.5, 12))
                ticket.closeSubpath()
                ctx.fill(ticket, with: .color(fill))
                ctx.stroke(ticket, with: .color(inkColor), style: StrokeStyle(lineWidth: 2.2 * s, lineJoin: .round))
                var perf = Path()
                perf.move(to: pt(12, 7)); perf.addLine(to: pt(12, 17))
                ctx.stroke(perf, with: .color(accent), style: StrokeStyle(lineWidth: 2.2 * s, lineCap: .round, dash: [2.2 * s, 2.4 * s]))
            case .goalFlag:
                var pole = Path()
                pole.move(to: pt(6, 20)); pole.addLine(to: pt(6, 4))
                ctx.stroke(pole, with: .color(inkColor), style: StrokeStyle(lineWidth: 2.2 * s, lineCap: .round))
                var flag = Path()
                flag.move(to: pt(6, 4)); flag.addLine(to: pt(18, 7.5)); flag.addLine(to: pt(6, 11)); flag.closeSubpath()
                ctx.fill(flag, with: .color(accent))
                ctx.stroke(flag, with: .color(inkColor), style: StrokeStyle(lineWidth: 2.2 * s, lineJoin: .round))
            case .sprout:
                var stem = Path()
                stem.move(to: pt(12, 20)); stem.addLine(to: pt(12, 12))
                ctx.stroke(stem, with: .color(inkColor), style: StrokeStyle(lineWidth: 2.2 * s, lineCap: .round))
                var leftLeaf = Path()
                leftLeaf.move(to: pt(12, 14))
                leftLeaf.addQuadCurve(to: pt(6, 10), control: pt(6, 14))
                leftLeaf.addQuadCurve(to: pt(12, 12), control: pt(9, 9))
                leftLeaf.closeSubpath()
                ctx.fill(leftLeaf, with: .color(fill))
                ctx.stroke(leftLeaf, with: .color(inkColor), style: StrokeStyle(lineWidth: 2.2 * s, lineJoin: .round))
                var rightLeaf = Path()
                rightLeaf.move(to: pt(12, 12))
                rightLeaf.addQuadCurve(to: pt(18, 8), control: pt(18, 12))
                rightLeaf.addQuadCurve(to: pt(12, 10), control: pt(15, 7))
                rightLeaf.closeSubpath()
                ctx.fill(rightLeaf, with: .color(accent))
                ctx.stroke(rightLeaf, with: .color(inkColor), style: StrokeStyle(lineWidth: 2.2 * s, lineJoin: .round))
            case .hourglassTime:
                var top = Path()
                top.move(to: pt(6, 4)); top.addLine(to: pt(18, 4)); top.addLine(to: pt(12, 11)); top.closeSubpath()
                ctx.fill(top, with: .color(fill))
                ctx.stroke(top, with: .color(inkColor), style: StrokeStyle(lineWidth: 2.2 * s, lineJoin: .round))
                var bottom = Path()
                bottom.move(to: pt(6, 20)); bottom.addLine(to: pt(18, 20)); bottom.addLine(to: pt(12, 13)); bottom.closeSubpath()
                ctx.fill(bottom, with: .color(fill))
                ctx.stroke(bottom, with: .color(inkColor), style: StrokeStyle(lineWidth: 2.2 * s, lineJoin: .round))
                ctx.fill(Path(ellipseIn: CGRect(x: 10.8 * s, y: 11.2 * s, width: 2.4 * s, height: 2.4 * s)), with: .color(accent))
            case .phoneDevice:
                let body = Path(roundedRect: CGRect(x: 7 * s, y: 3 * s, width: 10 * s, height: 18 * s), cornerRadius: 2 * s)
                ctx.fill(body, with: .color(fill))
                ctx.stroke(body, with: .color(inkColor), lineWidth: 2.2 * s)
                var speaker = Path()
                speaker.move(to: pt(10, 5.5)); speaker.addLine(to: pt(14, 5.5))
                ctx.stroke(speaker, with: .color(inkColor), style: StrokeStyle(lineWidth: 2.2 * s, lineCap: .round))
                var homeLine = Path()
                homeLine.move(to: pt(10, 18.5)); homeLine.addLine(to: pt(14, 18.5))
                ctx.stroke(homeLine, with: .color(accent), style: StrokeStyle(lineWidth: 2.2 * s, lineCap: .round))
            case .paletteArt:
                var punch = Path()
                punch.addEllipse(in: CGRect(x: 3 * s, y: 6 * s, width: 16 * s, height: 12 * s))
                punch.addEllipse(in: CGRect(x: 5.2 * s, y: 13.2 * s, width: 3.6 * s, height: 3.6 * s))
                ctx.fill(punch, with: .color(fill), style: FillStyle(eoFill: true))
                ctx.stroke(Path(ellipseIn: CGRect(x: 3 * s, y: 6 * s, width: 16 * s, height: 12 * s)), with: .color(inkColor), lineWidth: 2.2 * s)
                ctx.stroke(Path(ellipseIn: CGRect(x: 5.2 * s, y: 13.2 * s, width: 3.6 * s, height: 3.6 * s)), with: .color(inkColor), lineWidth: 2.2 * s)
                var dots = Path()
                dots.addEllipse(in: CGRect(x: 15 * s, y: 7.5 * s, width: 2.6 * s, height: 2.6 * s))
                dots.addEllipse(in: CGRect(x: 17.5 * s, y: 11.5 * s, width: 2.6 * s, height: 2.6 * s))
                dots.addEllipse(in: CGRect(x: 12.5 * s, y: 14.5 * s, width: 2.6 * s, height: 2.6 * s))
                ctx.fill(dots, with: .color(accent))
            }
        }
    }
}
