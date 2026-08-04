//
//  CatalogData.swift
//  KyouNoOgatore
//
//  catalog.json(scripts-native/gen-catalog.mjsがvideos.jsから生成。マスタープラン§2-1
//  "videos.js CATALOG"行・§6 Step 7a)のDecodableモデル。videos.js側のフィールド名(id/t/y/s/tags)を
//  そのまま使う(CardData.swiftと同じ方針)。アプリターゲット直下に同梱(CardCore等の独立SPM
//  パッケージではなくUI層所有の静的コンテンツとして扱う。QUIZ_ART画像のStep5c同梱と同じ判断)。

import Foundation

struct CatalogVideo: Decodable {
    let id: String
    let t: String
    let y: Int
    let s: String
    let tags: [String]?
    // TASK-C2-2026-08-04-build20-home-cards-and-tour-tiers.md H-2: ホームの「きょうの1本」に
    // 出うる動画にだけ付与するねらい型短タイトル。videos.js(Web版)は不可触のためcatalog.json
    // にのみ直接追記しており、全動画には無い(nilあり)。
    var st: String? = nil
}

enum CatalogLoader {
    static let shared: [CatalogVideo] = {
        guard let url = Bundle.main.url(forResource: "catalog", withExtension: "json") else {
            fatalError("catalog.json がアプリバンドルに同梱されていない")
        }
        do {
            return try JSONDecoder().decode([CatalogVideo].self, from: try Data(contentsOf: url))
        } catch {
            fatalError("catalog.json のデコードに失敗: \(error)")
        }
    }()
}
