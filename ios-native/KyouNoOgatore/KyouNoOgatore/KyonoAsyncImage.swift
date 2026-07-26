//
//  KyonoAsyncImage.swift
//  KyouNoOgatore
//
//  見た目パリティ第2弾(TASK-C2-2026-07-26-visual-parity-round2.md §2 動画サムネイル): iOS標準の
//  AsyncImage(タスク文の推奨どおり・新規ライブラリ依存なし)を薄くラップする。読み込み失敗時は
//  Web版onerror="this.style.visibility='hidden'"と同じく何も表示しない(崩れたアイコンを見せない)。

import SwiftUI

struct KyonoAsyncImage: View {
    let url: String
    var contentMode: ContentMode = .fill

    var body: some View {
        AsyncImage(url: URL(string: url)) { phase in
            if let image = phase.image {
                image.resizable().aspectRatio(contentMode: contentMode)
            }
        }
    }
}

// index.html videos.js/app-search.js相当: YouTube動画IDからサムネイルURLを組み立てる。
func youtubeThumbUrl(_ videoId: String, size: String = "mqdefault") -> String {
    "https://i.ytimg.com/vi/\(videoId)/\(size).jpg"
}
