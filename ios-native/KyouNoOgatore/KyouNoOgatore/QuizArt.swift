//
//  QuizArt.swift
//  KyouNoOgatore
//
//  TASK-C2-2026-07-28-quiz-result-reach-parity.md §4: app-quiz.js:92-137 QUIZ_ART[2]/[3]の移植。
//  Web版コメント「あたま/あごの高さ線＝両ひじが上がる目安」「かかとの浮き」は判定基準そのものの
//  可視化であり装飾ではない。SVGの正確な手描き曲線までは再現せず、判定基準(高さの目安線・
//  かかとの浮き)が明確に伝わる簡略化した図として実装する(タスク文が明示的に許容する範囲)。
//  Android版QuizArt.ktと同一ロジック。

import SwiftUI

private let quizArtInk = Color(hex: 0x3A3A35)
private let quizArtSkin = Color(hex: 0xE8B48C)
private let quizArtPink = Color(hex: 0xE56A9A)
private let quizArtHead = Color(hex: 0xFFE3C9)
private let quizArtGround = Color(hex: 0xE0D8C4)
private let quizArtGuide = Color(hex: 0xCFC9B8)
private let quizArtLabel = Color(hex: 0x6E6B5F)

struct QuizArtKenko: View {
    var body: some View {
        Canvas { ctx, size in
            let s = size.width / 300
            func pt(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: x * s, y: y * s) }

            // app-quiz.js:96 M30 150h240(ゆか)
            var ground = Path()
            ground.move(to: pt(30, 150)); ground.addLine(to: pt(270, 150))
            ctx.stroke(ground, with: .color(quizArtGround), style: StrokeStyle(lineWidth: 5 * s, lineCap: .round))

            // app-quiz.js:97-99 はな/あごの高さの目安線(点線)+ラベル。判定基準そのもの。
            var noseLine = Path(); noseLine.move(to: pt(136, 57)); noseLine.addLine(to: pt(210, 57))
            var chinLine = Path(); chinLine.move(to: pt(136, 69)); chinLine.addLine(to: pt(210, 69))
            let dash = StrokeStyle(lineWidth: 2.5 * s, dash: [2 * s, 6 * s])
            ctx.stroke(noseLine, with: .color(quizArtGuide), style: dash)
            ctx.stroke(chinLine, with: .color(quizArtGuide), style: dash)
            ctx.draw(Text("はな").font(.system(size: 11, weight: .black)).foregroundColor(quizArtLabel), at: pt(226, 58), anchor: .leading)
            ctx.draw(Text("あご").font(.system(size: 11, weight: .black)).foregroundColor(quizArtLabel), at: pt(226, 71), anchor: .leading)

            // app-quiz.js:100 legs
            var legL = Path(); legL.move(to: pt(112, 118)); legL.addLine(to: pt(106, 150))
            var legR = Path(); legR.move(to: pt(132, 118)); legR.addLine(to: pt(138, 150))
            let legStyle = StrokeStyle(lineWidth: 9 * s, lineCap: .round)
            ctx.stroke(legL, with: .color(Color(hex: 0x55524A)), style: legStyle)
            ctx.stroke(legR, with: .color(Color(hex: 0x55524A)), style: legStyle)
            // app-quiz.js:101 torso
            var torso = Path(); torso.move(to: pt(122, 118)); torso.addLine(to: pt(122, 80))
            ctx.stroke(torso, with: .color(quizArtInk), style: StrokeStyle(lineWidth: 15 * s, lineCap: .round))
            // app-quiz.js:102 head
            let head = Path(ellipseIn: CGRect(x: pt(122, 58).x - 13 * s, y: pt(122, 58).y - 13 * s, width: 26 * s, height: 26 * s))
            ctx.fill(head, with: .color(quizArtHead))
            ctx.stroke(head, with: .color(quizArtInk), lineWidth: 4 * s)
            // app-quiz.js:105 eyes
            ctx.fill(Path(ellipseIn: CGRect(x: pt(118, 59).x - 2 * s, y: pt(118, 59).y - 2 * s, width: 4 * s, height: 4 * s)), with: .color(quizArtInk))
            ctx.fill(Path(ellipseIn: CGRect(x: pt(126, 59).x - 2 * s, y: pt(126, 59).y - 2 * s, width: 4 * s, height: 4 * s)), with: .color(quizArtInk))

            // app-quiz.js:107-110 両ひじをつけて上げた腕(肩→ひじ)
            var arms = Path()
            arms.move(to: pt(108, 80))
            arms.addQuadCurve(to: pt(118, 42), control: pt(106, 58))
            arms.move(to: pt(136, 80))
            arms.addQuadCurve(to: pt(126, 42), control: pt(138, 58))
            ctx.stroke(arms, with: .color(quizArtSkin), style: StrokeStyle(lineWidth: 8 * s, lineCap: .round))
            // app-quiz.js:112 ひじが合わさる位置(判定の中心点)
            ctx.fill(Path(ellipseIn: CGRect(x: pt(122, 41).x - 7 * s, y: pt(122, 41).y - 7 * s, width: 14 * s, height: 14 * s)), with: .color(quizArtPink))
        }
        .aspectRatio(300.0 / 170.0, contentMode: .fit)
        .frame(maxWidth: .infinity)
    }
}

struct QuizArtAshi: View {
    var body: some View {
        Canvas { ctx, size in
            let s = size.width / 300
            func pt(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: x * s, y: y * s) }

            // app-quiz.js:117 ゆか
            var ground = Path(); ground.move(to: pt(30, 150)); ground.addLine(to: pt(270, 150))
            ctx.stroke(ground, with: .color(quizArtGround), style: StrokeStyle(lineWidth: 5 * s, lineCap: .round))

            // app-quiz.js:118-120 しゃがんだ脚(太もも・すね・かかとが浮いた足)
            var thigh = Path(); thigh.move(to: pt(120, 112)); thigh.addLine(to: pt(152, 110))
            var shin = Path(); shin.move(to: pt(120, 112)); shin.addLine(to: pt(134, 142))
            var foot = Path(); foot.move(to: pt(120, 148)); foot.addLine(to: pt(142, 144))
            ctx.stroke(thigh, with: .color(Color(hex: 0x55524A)), style: StrokeStyle(lineWidth: 13 * s, lineCap: .round))
            ctx.stroke(shin, with: .color(Color(hex: 0x55524A)), style: StrokeStyle(lineWidth: 11 * s, lineCap: .round))
            ctx.stroke(foot, with: .color(quizArtInk), style: StrokeStyle(lineWidth: 6 * s, lineCap: .round))
            // app-quiz.js:122 背中
            var back = Path()
            back.move(to: pt(152, 112))
            back.addQuadCurve(to: pt(158, 76), control: pt(150, 88))
            ctx.stroke(back, with: .color(quizArtInk), style: StrokeStyle(lineWidth: 15 * s, lineCap: .round))
            // app-quiz.js:123 head
            let head = Path(ellipseIn: CGRect(x: pt(152, 60).x - 15 * s, y: pt(152, 60).y - 15 * s, width: 30 * s, height: 30 * s))
            ctx.fill(head, with: .color(quizArtHead))
            ctx.stroke(head, with: .color(quizArtInk), lineWidth: 4 * s)
            ctx.fill(Path(ellipseIn: CGRect(x: pt(146, 62).x - 2.2 * s, y: pt(146, 62).y - 2.2 * s, width: 4.4 * s, height: 4.4 * s)), with: .color(quizArtInk))
            // app-quiz.js:126-127 前に伸ばした腕
            var arm = Path(); arm.move(to: pt(156, 80)); arm.addLine(to: pt(118, 86))
            ctx.stroke(arm, with: .color(quizArtSkin), style: StrokeStyle(lineWidth: 8 * s, lineCap: .round))

            // app-quiz.js:128-134 かかとの浮きをズームする円+点線の引き出し線+ラベル(判定基準そのもの)
            var leader = Path(); leader.move(to: pt(158, 140)); leader.addLine(to: pt(190, 144))
            ctx.stroke(leader, with: .color(quizArtGuide), style: StrokeStyle(lineWidth: 2.5 * s, dash: [3 * s, 4 * s]))
            let zoom = Path(ellipseIn: CGRect(x: pt(224, 110).x - 34 * s, y: pt(224, 110).y - 34 * s, width: 68 * s, height: 68 * s))
            ctx.fill(zoom, with: .color(.white))
            ctx.stroke(zoom, with: .color(quizArtInk), lineWidth: 3.5 * s)
            var zoomGround = Path(); zoomGround.move(to: pt(204, 128)); zoomGround.addLine(to: pt(244, 128))
            ctx.stroke(zoomGround, with: .color(quizArtGround), lineWidth: 4 * s)
            var heel = Path(); heel.move(to: pt(209, 124)); heel.addLine(to: pt(235, 118))
            ctx.stroke(heel, with: .color(quizArtInk), style: StrokeStyle(lineWidth: 6 * s, lineCap: .round))
            // かかとが浮いている隙間を示す矢印
            var arrowShaft = Path(); arrowShaft.move(to: pt(234, 120)); arrowShaft.addLine(to: pt(234, 128))
            ctx.stroke(arrowShaft, with: .color(quizArtPink), style: StrokeStyle(lineWidth: 3.5 * s, dash: [2 * s, 3 * s]))
            var arrowHead = Path()
            arrowHead.move(to: pt(229, 122)); arrowHead.addLine(to: pt(234, 128)); arrowHead.addLine(to: pt(239, 122))
            ctx.stroke(arrowHead, with: .color(quizArtPink), style: StrokeStyle(lineWidth: 3 * s, lineCap: .round))
            ctx.draw(
                Text("かかと").font(.system(size: 12, weight: .black)).foregroundColor(quizArtPink),
                at: pt(224, 88)
            )
        }
        .aspectRatio(300.0 / 170.0, contentMode: .fit)
        .frame(maxWidth: .infinity)
    }
}
