//
//  PlaylistData.swift
//  KyouNoOgatore
//
//  playlists.json(scripts-native/gen-playlists.mjsがindex.html:3498-3521 PLAYLISTSから生成。
//  TASK-C2-2026-07-28-search-playlists-and-fullwidth-space.md §1)のDecodableモデル。
//  手動キュレーションのYouTubeプレイリスト16本(3グループ)。CatalogData.swiftと同じ方針。

import Foundation

struct PlaylistItem: Decodable {
    let id: String
    let title: String
    let desc: String
    let thumb: String?
}

struct PlaylistGroup: Decodable {
    let group: String
    let items: [PlaylistItem]
}

enum PlaylistLoader {
    static let shared: [PlaylistGroup] = {
        guard let url = Bundle.main.url(forResource: "playlists", withExtension: "json") else {
            fatalError("playlists.json がアプリバンドルに同梱されていない")
        }
        do {
            return try JSONDecoder().decode([PlaylistGroup].self, from: try Data(contentsOf: url))
        } catch {
            fatalError("playlists.json のデコードに失敗: \(error)")
        }
    }()
}
