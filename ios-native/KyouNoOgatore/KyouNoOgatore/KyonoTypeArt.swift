//
//  KyonoTypeArt.swift
//  KyouNoOgatore
//
//  フォント適用漏れ・キャラ/タイプ画像の欠落修正タスク(TASK-C2-2026-07-26-visual-parity-fonts-characters.md)
//  §3 かたさタイプの画像: app-quiz.js:3-7 TYPE_ART(koka/ashi/robotのインラインSVG)・
//  index.html:1437 TYPE_IMG(momo/kenko/yawaraのPNG)の1:1移植。診断結果画面のタイプ名上部
//  (Web版 .type-illust、104x104)に表示する。既存のKyonoIcons.swiftと同じ要領でSVGはCanvas直書き。
//
//  TestFlight実機フィードバックE1(2026-07-29): 6体ぶんのPNGが揃ったため、koka/ashi/robotも
//  PNG優先の対象に含めた。Canvas描画(下のGroup else節)はコードとしては残し、辞書のキーに無い
//  タイプが来たとき、またはPNG読み込み自体が失敗したとき(index.html:2624 TYPE_IMG[key]?PNG:SVG
//  と同じPNG優先・SVGフォールバックの考え方)の保険として使う。このプロジェクトはウィジェット
//  (B6)・検索サムネイル(D1)と、バンドル画像が実機で静かに読み込めなくなる欠陥を2度経験して
//  いるため、Canvas描画を消して二重管理を無くすより、フォールバックとして残す判断にした
//  (alan5の「消す」提案とは異なる判断・理由はここに明記)。

import SwiftUI

// TestFlight実機フィードバックE1(2026-07-29): alan5の当初の見立て(「koka/ashi/robotは
// 表示側が無い」)は誤りで、実際にはこの下のCanvas描画として既に実装済みだった。本当の作業は
// 「無いものを足す」ではなく「PNGが揃った(assets/type-{koka,ashi,robot}.png、承認済みコミット
// c32bf1f)ので、既存のPNG優先ロジックの対象に6体とも含める」こと。辞書に無いキーはCanvas描画
// にフォールバックする既存の分岐(下のGroup参照)を使うため、ここに追加するだけで済む。
private let typeImgNames: [String: String] = [
    "momo": "type-momo",
    "kenko": "type-kenko",
    "yawara": "type-yawara",
    "koka": "type-koka",
    "ashi": "type-ashi",
    "robot": "type-robot",
]

struct KyonoTypeArt: View {
    let typeKey: String

    var body: some View {
        Group {
            if let name = typeImgNames[typeKey], let uiImage = loadTypeArt(name) {
                Image(uiImage: uiImage).resizable().scaledToFit()
            } else {
                Canvas { ctx, size in
                    let s = size.width / 96
                    func pt(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: x * s, y: y * s) }
                    let ink = Color(hex: 0x3A3A35)
                    switch typeKey {
                    case "koka":
                        // app-quiz.js TYPE_ART.koka(開かずのトビラ=扉)の1:1移植。
                        let door = Path(roundedRect: CGRect(x: pt(24, 12).x, y: pt(24, 12).y, width: pt(48, 0).x, height: pt(0, 74).y), cornerRadius: 6 * s)
                        ctx.fill(door, with: .color(Color(hex: 0xD9A066)))
                        ctx.stroke(door, with: .color(ink), lineWidth: 3 * s)
                        let panelTop = Path(roundedRect: CGRect(x: pt(31, 20).x, y: pt(31, 20).y, width: pt(34, 0).x, height: pt(0, 26).y), cornerRadius: 4 * s)
                        let panelBottom = Path(roundedRect: CGRect(x: pt(31, 52).x, y: pt(31, 52).y, width: pt(34, 0).x, height: pt(0, 26).y), cornerRadius: 4 * s)
                        let panelFill = Color(hex: 0xE8B87E), panelBorder = Color(hex: 0xB4805A)
                        ctx.fill(panelTop, with: .color(panelFill)); ctx.stroke(panelTop, with: .color(panelBorder), lineWidth: 2 * s)
                        ctx.fill(panelBottom, with: .color(panelFill)); ctx.stroke(panelBottom, with: .color(panelBorder), lineWidth: 2 * s)
                        ctx.fill(Path(ellipseIn: CGRect(x: pt(63, 49).x - 3.5 * s, y: pt(63, 49).y - 3.5 * s, width: 7 * s, height: 7 * s)), with: .color(ink))
                        ctx.fill(Path(ellipseIn: CGRect(x: pt(39, 33).x - 1.8 * s, y: pt(39, 33).y - 1.8 * s, width: 3.6 * s, height: 3.6 * s)), with: .color(ink))
                        ctx.fill(Path(ellipseIn: CGRect(x: pt(53, 33).x - 1.8 * s, y: pt(53, 33).y - 1.8 * s, width: 3.6 * s, height: 3.6 * s)), with: .color(ink))
                        var smile = Path()
                        smile.move(to: pt(42, 39))
                        smile.addQuadCurve(to: pt(50, 39), control: pt(46, 41.5))
                        ctx.stroke(smile, with: .color(ink), style: StrokeStyle(lineWidth: 2.2 * s, lineCap: .round))
                    case "ashi":
                        // app-quiz.js TYPE_ART.ashi(棒立ちペンギン)の1:1移植。
                        let body = Path(ellipseIn: CGRect(x: pt(24, 24).x, y: pt(24, 24).y, width: 48 * s, height: 60 * s))
                        ctx.fill(body, with: .color(Color(hex: 0x4E5A6E)))
                        ctx.stroke(body, with: .color(ink), lineWidth: 3 * s)
                        let belly = Path(ellipseIn: CGRect(x: pt(33, 40).x, y: pt(33, 40).y, width: 30 * s, height: 40 * s))
                        ctx.fill(belly, with: .color(.white))
                        ctx.fill(Path(ellipseIn: CGRect(x: pt(41, 40).x - 2.2 * s, y: pt(41, 40).y - 2.2 * s, width: 4.4 * s, height: 4.4 * s)), with: .color(ink))
                        ctx.fill(Path(ellipseIn: CGRect(x: pt(55, 40).x - 2.2 * s, y: pt(55, 40).y - 2.2 * s, width: 4.4 * s, height: 4.4 * s)), with: .color(ink))
                        var beak = Path()
                        beak.move(to: pt(44, 47)); beak.addLine(to: pt(48, 50)); beak.addLine(to: pt(52, 47)); beak.closeSubpath()
                        let beakColor = Color(hex: 0xF5A25D)
                        ctx.fill(beak, with: .color(beakColor))
                        ctx.stroke(beak, with: .color(ink), style: StrokeStyle(lineWidth: 2 * s, lineJoin: .round))
                        var footL = Path(); footL.move(to: pt(34, 82)); footL.addQuadCurve(to: pt(43, 83), control: pt(38, 85))
                        var footR = Path(); footR.move(to: pt(62, 82)); footR.addQuadCurve(to: pt(53, 83), control: pt(58, 85))
                        ctx.stroke(footL, with: .color(beakColor), style: StrokeStyle(lineWidth: 4 * s, lineCap: .round))
                        ctx.stroke(footR, with: .color(beakColor), style: StrokeStyle(lineWidth: 4 * s, lineCap: .round))
                        var wingL = Path(); wingL.move(to: pt(24, 44)); wingL.addQuadCurve(to: pt(26, 58), control: pt(20, 52))
                        var wingR = Path(); wingR.move(to: pt(72, 44)); wingR.addQuadCurve(to: pt(70, 58), control: pt(76, 52))
                        ctx.stroke(wingL, with: .color(ink), style: StrokeStyle(lineWidth: 3 * s, lineCap: .round))
                        ctx.stroke(wingR, with: .color(ink), style: StrokeStyle(lineWidth: 3 * s, lineCap: .round))
                    case "robot":
                        // app-quiz.js TYPE_ART.robot(ガチガチロボット)の1:1移植。
                        let head = Path(roundedRect: CGRect(x: pt(24, 26).x, y: pt(24, 26).y, width: 48 * s, height: 42 * s), cornerRadius: 10 * s)
                        ctx.fill(head, with: .color(Color(hex: 0xC7D3DE)))
                        ctx.stroke(head, with: .color(ink), lineWidth: 3 * s)
                        var antenna = Path(); antenna.move(to: pt(48, 26)); antenna.addLine(to: pt(48, 14))
                        ctx.stroke(antenna, with: .color(ink), style: StrokeStyle(lineWidth: 3 * s, lineCap: .round))
                        let tip = Path(ellipseIn: CGRect(x: pt(48, 12).x - 4 * s, y: pt(48, 12).y - 4 * s, width: 8 * s, height: 8 * s))
                        ctx.fill(tip, with: .color(Color(hex: 0xFF8A70)))
                        ctx.stroke(tip, with: .color(ink), lineWidth: 2.5 * s)
                        let eyeL = Path(roundedRect: CGRect(x: pt(33, 38).x, y: pt(33, 38).y, width: 10 * s, height: 10 * s), cornerRadius: 3 * s)
                        let eyeR = Path(roundedRect: CGRect(x: pt(53, 38).x, y: pt(53, 38).y, width: 10 * s, height: 10 * s), cornerRadius: 3 * s)
                        ctx.fill(eyeL, with: .color(.white)); ctx.stroke(eyeL, with: .color(ink), lineWidth: 2.5 * s)
                        ctx.fill(eyeR, with: .color(.white)); ctx.stroke(eyeR, with: .color(ink), lineWidth: 2.5 * s)
                        ctx.fill(Path(ellipseIn: CGRect(x: pt(38, 43).x - 1.8 * s, y: pt(38, 43).y - 1.8 * s, width: 3.6 * s, height: 3.6 * s)), with: .color(ink))
                        ctx.fill(Path(ellipseIn: CGRect(x: pt(58, 43).x - 1.8 * s, y: pt(58, 43).y - 1.8 * s, width: 3.6 * s, height: 3.6 * s)), with: .color(ink))
                        var mouth = Path(); mouth.move(to: pt(40, 57)); mouth.addLine(to: pt(56, 57))
                        ctx.stroke(mouth, with: .color(ink), style: StrokeStyle(lineWidth: 2.5 * s, lineCap: .round))
                        let base = Path(roundedRect: CGRect(x: pt(30, 72).x, y: pt(30, 72).y, width: 36 * s, height: 10 * s), cornerRadius: 5 * s)
                        ctx.fill(base, with: .color(Color(hex: 0xE4EAF0)))
                        ctx.stroke(base, with: .color(ink), lineWidth: 2.5 * s)
                    default:
                        break
                    }
                }
            }
        }
        .frame(width: 104, height: 104)
    }
}

// PBXFileSystemSynchronizedRootGroup(Xcode26形式)はTypeArt/サブフォルダをビルド時にフラット化して
// バンドルルート直下へ配置するため、subdirectory指定なしで探す(DexView.swift loadCardArtと同じ考え方)。
private func loadTypeArt(_ name: String) -> UIImage? {
    guard let url = Bundle.main.url(forResource: name, withExtension: "png") else { return nil }
    return UIImage(contentsOfFile: url.path)
}
