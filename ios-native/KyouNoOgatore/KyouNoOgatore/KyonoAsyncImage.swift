//
//  KyonoAsyncImage.swift
//  KyouNoOgatore
//
//  見た目パリティ第2弾(TASK-C2-2026-07-26-visual-parity-round2.md §2 動画サムネイル): iOS標準の
//  AsyncImage(タスク文の推奨どおり・新規ライブラリ依存なし)を薄くラップする。読み込み失敗時は
//  Web版onerror="this.style.visibility='hidden'"と同じく何も表示しない(崩れたアイコンを見せない)。
//
//  TASK: オフライン用ディスクキャッシュ(ThumbnailCache)を読み書き経路に挟むため、SwiftUI標準の
//  AsyncImageから自前のURLSession+.task(id:)実装に切り替えた(AsyncImageはキャッシュ挙動を
//  差し込めないため)。Cachesディレクトリ配下のthumbnails/に保存し、Documentsディレクトリ
//  (kyono-store.json=記録データ本体)とは完全に分離する。書き込み済みならネットワーク状況に
//  関わらず常にキャッシュを優先してよい(サムネイルURLはvideoId+size単位で不変)。

import SwiftUI
import UIKit

struct KyonoAsyncImage: View {
    let url: String
    var contentMode: ContentMode = .fill

    @State private var uiImage: UIImage?

    private static let cache: ThumbnailCache = {
        let base = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0]
        let dir = base.appendingPathComponent("thumbnails", isDirectory: true)
        return ThumbnailCache(cacheDir: dir)
    }()

    var body: some View {
        Group {
            if let uiImage {
                Image(uiImage: uiImage).resizable().aspectRatio(contentMode: contentMode)
            }
        }
        .task(id: url) {
            // produceState(key1=url)相当: urlが変わったら古い画像を即座に消してから読み込み直す
            // (画面再利用時に別動画の古いサムネイルが一瞬見えるのを防ぐ)。
            uiImage = nil
            uiImage = await Self.loadImage(urlString: url)
        }
    }

    private static func loadImage(urlString: String) async -> UIImage? {
        let key = ThumbnailCache.key(forURL: urlString)
        if let cached = cache.read(key: key), let image = UIImage(data: cached) {
            return image
        }
        guard let requestURL = URL(string: urlString) else { return nil }
        guard let (data, _) = try? await URLSession.shared.data(from: requestURL) else { return nil }
        guard let image = UIImage(data: data) else { return nil }
        cache.write(key: key, data: data)
        return image
    }
}

// index.html videos.js/app-search.js相当: YouTube動画IDからサムネイルURLを組み立てる。
func youtubeThumbUrl(_ videoId: String, size: String = "mqdefault") -> String {
    "https://i.ytimg.com/vi/\(videoId)/\(size).jpg"
}
