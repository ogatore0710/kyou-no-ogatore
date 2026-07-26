//
//  ObuView.swift
//  KyouNoOgatore
//
//  ネイティブ移植 Step 7b(マスタープラン§6 Step 7b・§2-1「obu-feed.js OBU_FEED」行): オガトレ通信
//  (オガトレ部)の全件アーカイブUI(Android版ObuScreen.ktと同一ロジック。index.html
//  renderObuArchive()の1:1移植=新着順ソート+type別描画)。FABの新着ポップアップは本ステップでは
//  簡略化し、アーカイブ一覧のみを実装する(Android版と同じ判断)。
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
        KyonoTheme(themeSetting: themeSetting) {
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
            if posts.isEmpty {
                KyonoCard {
                    Text("まだ投稿がありません また今度のぞいてみてね🌱")
                        .font(.kyono(.bold700, size: 14)).foregroundColor(colors.sub)
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

private struct ObuPostCardView: View {
    @Environment(\.kyonoColors) private var colors
    let post: ObuPost

    // index.html:271 .obu-post.obu-text(yellow-softの角丸ボックス)。photo/radioはボックスなしで並べる。
    private var isText: Bool { post.type != "photo" && post.type != "radio" }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(post.date + (post.time.map { " " + $0 } ?? ""))
                .font(.kyono(.black900, size: 12)).foregroundColor(isText ? colors.sub2 : colors.sub)
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
                if let text = post.text { Text(text).font(.kyono(.bold700, size: 14)).foregroundColor(colors.ink).lineSpacing(6) }
            case "radio":
                if let title = post.title { Text("📻 \(title)").font(.kyono(.black900, size: 14)).foregroundColor(colors.tealInk) }
                Text("🎧 音声つき投稿(ネイティブでは再生UI未実装)").font(.kyono(.bold700, size: 12)).foregroundColor(colors.sub)
            default:
                if let text = post.text { Text(text).font(.kyono(.bold700, size: 15)).foregroundColor(colors.ink).lineSpacing(9) }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(isText ? 14 : 0)
        .background(isText ? colors.yellowSoft : Color.clear)
        .cornerRadius(isText ? 14 : 0)
        .padding(.bottom, 14)
    }
}
