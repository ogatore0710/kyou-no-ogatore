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
        // TestFlight実機フィードバックB4(2026-07-29): [uiImage, text]を素のまま渡すと、
        // 「写真に保存」等の保存系アクティビティは項目が全部画像でないと候補から外れるため、
        // 共有シートから消えていた。テキストをUIActivityItemSource(ShareTextItem)に包み、
        // 保存系アクティビティにだけitemForActivityTypeでnilを返して除外する(SNS投稿・
        // メッセージ等には従来どおりテキストも渡すので、ハッシュタグが消えることはない)。
        let activityVC = UIActivityViewController(activityItems: [uiImage, ShareTextItem(text: text)], applicationActivities: nil)
        var top = root
        while let presented = top.presentedViewController { top = presented }
        if let popover = activityVC.popoverPresentationController {
            popover.sourceView = top.view
            popover.sourceRect = CGRect(x: top.view.bounds.midX, y: top.view.bounds.midY, width: 0, height: 0)
        }
        top.present(activityVC, animated: true)
    }
}

// B4: 保存系(写真に保存・プリント・連絡先に割り当て・リーディングリスト)には渡さず、
// それ以外(SNS投稿・メッセージ・メール等)には渡す、というactivityType別の出し分けを行う
// UIActivityItemSource実装。
private final class ShareTextItem: NSObject, UIActivityItemSource {
    private let text: String
    init(text: String) { self.text = text }

    func activityViewControllerPlaceholderItem(_ activityViewController: UIActivityViewController) -> Any {
        text
    }

    func activityViewController(_ activityViewController: UIActivityViewController, itemForActivityType activityType: UIActivity.ActivityType?) -> Any? {
        guard let activityType else { return text }
        let excluded: Set<UIActivity.ActivityType> = [.saveToCameraRoll, .print, .assignToContact, .addToReadingList]
        return excluded.contains(activityType) ? nil : text
    }
}
