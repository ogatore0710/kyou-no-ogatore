//
//  ObuView.swift
//  KyouNoOgatore
//
//  ネイティブ移植 Step 7b(マスタープラン§6 Step 7b・§2-1「obu-feed.js OBU_FEED」行): オガトレ通信
//  (オガトレ部)の全件アーカイブUI(Android版ObuScreen.ktと同一ロジック。index.html
//  renderObuArchive()の1:1移植=新着順ソート+type別描画)。FABタップ時のプレビューポップアップ
//  (renderObuPopup/openObu)は当初簡略化していたが、TASK-C2-2026-07-27-obu-fab-preview-popup.mdで
//  ObuPreviewPopupView(下記)として追加移植した。
//
//  ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
//  Phase 3: index.html:266-278 .obu-post/.obu-post.obu-text(yellow-soft)/.obu-date/.obu-title/
//  .obu-capの1:1移植。obuIsStaleDate/obuFmtDateの日付整形ロジックは新規追加であり「見た目のみ」の
//  スコープを超えるため、このステップでは移植しない(既存の生日付表示を維持)。

import SwiftUI
import RecordCore

struct ObuView: View {
    let store: RecordStore
    let onBack: () -> Void

    private let posts: [ObuPost]

    init(store: RecordStore, onBack: @escaping () -> Void) {
        self.store = store
        self.onBack = onBack
        self.posts = ObuLoader.shared.sorted { a, b in
            if a.date != b.date { return a.date > b.date }
            return (a.time ?? "") > (b.time ?? "")
        }
    }

    private var themeSetting: String { store.get("theme", default: "auto") }

    var body: some View {
        KyonoTheme(themeSetting: themeSetting, bigText: store.get("bigtext", default: true)) {
            ObuContentView(posts: posts, onBack: onBack)
        }
    }
}

private struct ObuContentView: View {
    @Environment(\.kyonoColors) private var colors
    let posts: [ObuPost]
    let onBack: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            KyonoLineButton("◀ もどる", action: onBack)
            KyonoSectionHeader(icon: .obuBubble, title: "オガトレ通信", fill: colors.pinkSoft)
            // 全画面完全性監査タスク(TASK-C2-2026-07-26-full-completeness-audit.md #obu):
            // index.html:932 説明文の1:1移植。
            Text("尾形さんからの ひとこと・写真・ラジオを ぜんぶまとめて見られます🌱")
                .kyonoFont(.bold700, size: 14).foregroundColor(colors.ink)
            if posts.isEmpty {
                KyonoCard {
                    Text("まだ投稿がありません また今度のぞいてみてね🌱")
                        .kyonoFont(.bold700, size: 14).foregroundColor(colors.sub)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .multilineTextAlignment(.center)
                }
            } else {
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 0) {
                        ForEach(posts, id: \.id) { post in ObuPostCardView(post: post) }
                    }
                }
            }
        }
        .padding(16)
        .background(KyonoBackgroundColor().ignoresSafeArea())
    }
}

struct ObuPostCardView: View {
    @Environment(\.kyonoColors) private var colors
    let post: ObuPost

    // index.html:271 .obu-post.obu-text(yellow-softの角丸ボックス)。photo/radioはボックスなしで並べる。
    private var isText: Bool { post.type != "photo" && post.type != "radio" }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(post.date + (post.time.map { " " + $0 } ?? ""))
                .kyonoFont(.black900, size: 12).foregroundColor(isText ? colors.sub2 : colors.sub)
            switch post.type {
            case "photo":
                if let imagePath = post.image, let base = ObuLoader.imageFileBaseName(imagePath),
                   let url = Bundle.main.url(forResource: base, withExtension: "jpg"),
                   let uiImage = UIImage(contentsOfFile: url.path) {
                    Image(uiImage: uiImage).resizable().aspectRatio(contentMode: .fill).frame(height: 180).clipped()
                        .background(colors.card)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                        .overlay(RoundedRectangle(cornerRadius: 14).stroke(colors.line, lineWidth: 1.5))
                }
                if let text = post.text { Text(text).kyonoFont(.bold700, size: 14).foregroundColor(colors.ink).lineSpacing(6) }
            case "radio":
                if let title = post.title { Text("📻 \(title)").kyonoFont(.black900, size: 14).foregroundColor(colors.tealInk) }
                Text("🎧 音声つき投稿(ネイティブでは再生UI未実装)").kyonoFont(.bold700, size: 12).foregroundColor(colors.sub)
            default:
                if let text = post.text { Text(text).kyonoFont(.bold700, size: 15).foregroundColor(colors.ink).lineSpacing(9) }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(isText ? 14 : 0)
        .background(isText ? colors.yellowSoft : Color.clear)
        .cornerRadius(isText ? 14 : 0)
        .padding(.bottom, 14)
    }
}

// TASK-C2-2026-07-27-obu-fab-preview-popup.md: index.html:1344-1358 renderObuPopup/openObuの1:1移植。
// FABタップで直接全アーカイブへ遷移していたのをやめ、まずtext/photo/radio最新1件ずつ(最大3件)だけを
// 見せるプレビューにする。既読記録(obu_seen)・バッジ更新は呼び出し元(FABのaction)がポップアップを
// 開く時点で行う(index.html:1345-1348と同じ「開いた瞬間に既読」のタイミング)。
struct ObuPreviewPopupView: View {
    @Environment(\.kyonoColors) private var colors
    let onClose: () -> Void
    let onViewArchive: () -> Void

    private let items: [ObuPost]

    init(onClose: @escaping () -> Void, onViewArchive: @escaping () -> Void) {
        self.onClose = onClose
        self.onViewArchive = onViewArchive
        let posts = ObuLoader.shared
        self.items = ["text", "photo", "radio"].compactMap { obuLatestByType(posts, $0) }
    }

    var body: some View {
        ZStack {
            Color.black.opacity(0.55).ignoresSafeArea()
                .onTapGesture { onClose() }
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text("オガトレ通信").kyonoFont(.black900, size: 15).foregroundColor(colors.ink)
                    Spacer()
                    Button(action: onClose) {
                        Text("✕").kyonoFont(.black900, size: 18).foregroundColor(colors.ink)
                            .frame(width: 40, height: 40)
                            .background(Circle().fill(colors.line))
                    }
                    .buttonStyle(.plain)
                }
                if items.isEmpty {
                    Text("まだ投稿がありません また今度のぞいてみてね🌱")
                        .kyonoFont(.bold700, size: 14).foregroundColor(colors.sub)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .multilineTextAlignment(.center)
                        .padding(.vertical, 20)
                } else {
                    VStack(alignment: .leading, spacing: 0) {
                        ForEach(items, id: \.id) { post in ObuPostCardView(post: post) }
                    }
                }
                Text("もっと見る（過去の投稿もぜんぶ）")
                    .kyonoFont(.black900, size: 14).foregroundColor(colors.tealInk)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .onTapGesture { onViewArchive() }
            }
            .padding(18)
            .background(RoundedRectangle(cornerRadius: 20).fill(colors.card))
            .padding(24)
        }
    }
}
