//
//  EdgeSwipeBack.swift
//  KyouNoOgatore
//
//  Android版パリティ移植: android-native/.../GuideScreen.ktのBackHandler(D1・5視点ワンループG6検収で
//  追加)により、Android側は既にどの子画面からもシステムバック/スワイプで前の画面へ戻れる。iOS側には
//  同等の「左端スワイプでもどる」導線が無く、明示的な「◀ もどる」ボタンのタップしか手段が無かった
//  欠落を埋める。NavigationStack不採用方針(masterplan §1-4・KyouNoOgatoreApp.swiftのコメント参照)
//  のため、UIKitのinteractivePopGestureRecognizerには乗れない。DragGestureで自前実装する。
//
//  設計:
//  - 開始位置が画面左端から一定距離(edgeWidth)以内のドラッグのみを「戻る」候補とする
//    (iOS標準のエッジスワイプ導線と同じ考え方。任意の地点から始まるドラッグでは絶対に発火しない)。
//  - 対象9画面(search/catalog/dex/voices/brag/diary/obu/guide/settings)はいずれも
//    outer content Viewが.padding(16)または.padding(20)(SettingsView)で囲われており、
//    インタラクティブな子要素(カテゴリチップの横スクロール行=SearchViewのFadingChipRow等)は
//    x=16以降にしか存在しない。edgeWidthを16未満(14pt)にしておくことで、このジェスチャーの
//    検出範囲はそのpaddingの余白(何もタップできない領域)に完全に収まり、横スクロールカルーセルや
//    チップ行の操作と物理的にコンフリクトしない(必要以上に慎重を期すため、タスク指示の目安値
//    24ptよりさらに絞った)。
//  - onEndedでのみ判定する(ドラッグ中に前の画面をちらつかせるような割り込みはしない。Web版由来の
//    「画面遷移は即座に切り替わる」設計 KyouNoOgatoreApp.swiftのscreenContent transitionと役割を
//    分離するため)。
//  - 横方向の移動量が縦方向の移動量を十分に上回る場合のみ「スワイプ」と判定し、縦スクロール
//    (ScrollViewの主操作)と誤認しないようにする。
//  - .simultaneousGesture()で付与する。ScrollViewの内部ジェスチャー(UIScrollViewベース)を
//    横取りせず、両方が独立に認識できるようにするため(.gesture()だと排他的になり、スクロールを
//    妨げるおそれがある)。

import SwiftUI

struct EdgeSwipeBack: ViewModifier {
    /// 左端とみなす開始位置のしきい値(pt)。対象9画面の最小outer paddingが16ptのため、それより
    /// 狭い14ptに設定し、インタラクティブな子要素の領域と重ならないようにしている(上記コメント参照)。
    var edgeWidth: CGFloat = 14
    /// 「戻る」と判定するために必要な右方向ドラッグ量(pt)。
    var minTranslation: CGFloat = 60
    let onBack: () -> Void

    func body(content: Content) -> some View {
        content
            .simultaneousGesture(
                DragGesture(minimumDistance: 8, coordinateSpace: .local)
                    .onEnded { value in
                        guard value.startLocation.x < edgeWidth else { return }
                        let dx = value.translation.width
                        let dy = value.translation.height
                        guard dx > minTranslation, dx > abs(dy) * 1.5 else { return }
                        onBack()
                    }
            )
    }
}

extension View {
    /// 左端スワイプで`onBack`を呼ぶジェスチャーを付与する。子画面のトップレベルViewから
    /// `.modifier(EdgeSwipeBack(onBack: onBack))`と同義で使う糖衣。
    func edgeSwipeBack(onBack: @escaping () -> Void) -> some View {
        modifier(EdgeSwipeBack(onBack: onBack))
    }
}
