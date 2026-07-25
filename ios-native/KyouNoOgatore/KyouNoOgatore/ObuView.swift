//
//  ObuView.swift
//  KyouNoOgatore
//
//  ネイティブ移植 Step 7b(マスタープラン§6 Step 7b・§2-1「obu-feed.js OBU_FEED」行): オガトレ通信
//  (オガトレ部)の全件アーカイブUI(Android版ObuScreen.ktと同一ロジック。index.html
//  renderObuArchive()の1:1移植=新着順ソート+type別描画)。FABの新着ポップアップは本ステップでは
//  簡略化し、アーカイブ一覧のみを実装する(Android版と同じ判断)。

import SwiftUI

struct ObuView: View {
    let onBack: () -> Void

    private let posts: [ObuPost]

    init(onBack: @escaping () -> Void) {
        self.onBack = onBack
        self.posts = ObuLoader.shared.sorted { a, b in
            if a.date != b.date { return a.date > b.date }
            return (a.time ?? "") > (b.time ?? "")
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Button("◀ もどる", action: onBack)
            Text("オガトレ通信").font(.title2.bold())
            ScrollView {
                LazyVStack(spacing: 8) {
                    ForEach(posts, id: \.id) { post in ObuPostCardView(post: post) }
                }
            }
        }
        .padding(16)
    }
}

private struct ObuPostCardView: View {
    let post: ObuPost

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(post.date + (post.time.map { " " + $0 } ?? "")).font(.caption).foregroundColor(.gray)
            switch post.type {
            case "photo":
                if let imagePath = post.image, let base = ObuLoader.imageFileBaseName(imagePath),
                   let url = Bundle.main.url(forResource: base, withExtension: "jpg"),
                   let uiImage = UIImage(contentsOfFile: url.path) {
                    Image(uiImage: uiImage).resizable().aspectRatio(contentMode: .fill).frame(height: 180).clipped().cornerRadius(8)
                }
                if let text = post.text { Text(text) }
            case "radio":
                if let title = post.title { Text(title).font(.headline) }
                Text("🎧 音声つき投稿(ネイティブでは再生UI未実装)").font(.caption).foregroundColor(.gray)
            default:
                if let text = post.text { Text(text) }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(Color(red: 0.95, green: 0.95, blue: 0.93))
        .cornerRadius(12)
    }
}
