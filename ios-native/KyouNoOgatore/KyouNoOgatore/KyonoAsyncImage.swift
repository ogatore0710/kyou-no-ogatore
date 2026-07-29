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
//
//  TASK-C2-2026-07-29-testflight-feedback-d.md D1: 実機でサムネイルが1枚も出ない不具合。
//  NSLogでの実測(検索結果24行・全行 `.task` 発火ゼロ)で確定: bodyが`Group { if let uiImage {...} }`
//  のとき、uiImage==nilの間はGroupの中身が完全に空になり、その空Groupに付けた`.task`が
//  一度も発火しない(SwiftUIが中身の無いGroupをレンダーツリーにmountしない)。`Color.clear`を
//  常に実体のあるアンカーにして`.overlay`で条件付き内容を載せる形に変えたところ、同じ実測で
//  全行の`.task`発火・画像取得(200 OK)を確認した。

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
        Color.clear
            .overlay {
                if let uiImage {
                    // F2(TASK-C2-2026-07-29-inspection-upgrade.md): D1(サムネイル全滅)は行数だけを
                    // 見るテストでは素通りしていた(画像が1枚も無くても行は描画されるため)。画像が
                    // 実際に読み込めて描画されたときにだけ付く識別子を用意し、UIテスト側で
                    // 「行が存在する」だけでなく「その中に画像が実在する」ことまで確認できるようにする。
                    Image(uiImage: uiImage).resizable().aspectRatio(contentMode: contentMode)
                        .accessibilityIdentifier("kyonoThumbnailLoaded")
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
