//
//  ShareImage.swift
//  KyouNoOgatore
//
//  ネイティブ移植 Step 7b(マスタープラン§6 Step 7b・index.html shareCard()/downloadCard()相当):
//  記録カード・じまんカードの「保存・シェアする」を初めて実装する(Step5a/5bまでは「とじる」だけの
//  シートだった)。Web版のWeb Share APIの代わりにiOS標準のUIActivityViewControllerを使う。

import SwiftUI
import UIKit

enum ShareImage {
    // SwiftUIから直接UIActivityViewControllerを開くには、現在のUIWindowのrootViewControllerを
    // 経由する必要がある(SwiftUIにOS標準の共有シートAPIが無いため)。
    static func share(uiImage: UIImage, text: String) {
        guard let windowScene = UIApplication.shared.connectedScenes.first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene,
              let root = windowScene.windows.first(where: { $0.isKeyWindow })?.rootViewController else { return }
        let activityVC = UIActivityViewController(activityItems: [uiImage, text], applicationActivities: nil)
        var top = root
        while let presented = top.presentedViewController { top = presented }
        if let popover = activityVC.popoverPresentationController {
            popover.sourceView = top.view
            popover.sourceRect = CGRect(x: top.view.bounds.midX, y: top.view.bounds.midY, width: 0, height: 0)
        }
        top.present(activityVC, animated: true)
    }
}
