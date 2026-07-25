//
//  ObuData.swift
//  KyouNoOgatore
//
//  obu-feed.json(scripts-native/gen-catalog.mjsがobu-feed.jsから生成。マスタープラン§2-1
//  "obu-feed.js OBU_FEED"行・§6 Step 7b)のDecodableモデル。type: "text"|"photo"|"radio"。

import Foundation

struct ObuPost: Decodable {
    let id: String
    let date: String
    let type: String
    let text: String?
    let image: String?
    let audio: String?
    let title: String?
    let time: String?
}

enum ObuLoader {
    static let shared: [ObuPost] = {
        guard let url = Bundle.main.url(forResource: "obu-feed", withExtension: "json") else {
            fatalError("obu-feed.json がアプリバンドルに同梱されていない")
        }
        do {
            return try JSONDecoder().decode([ObuPost].self, from: try Data(contentsOf: url))
        } catch {
            fatalError("obu-feed.json のデコードに失敗: \(error)")
        }
    }()

    // index.html obuValidAssetPath()相当の簡易版+バンドル内画像ファイル名への変換。
    // "assets/obu/post-2026-07-09-01.jpg" → "post-2026-07-09-01"(拡張子なし・ファイル名部分のみ)。
    // ObuArt/フォルダへそのままの名前で同梱している(PBXFileSystemSynchronizedRootGroupがビルド時に
    // バンドルルートへフラット化する。DexView.swiftのCardArt/と同じ実測知見)。
    static func imageFileBaseName(_ imagePath: String) -> String? {
        guard imagePath.range(of: "^assets/obu/[A-Za-z0-9_.\\-/]+$", options: .regularExpression) != nil else { return nil }
        let base = (imagePath as NSString).lastPathComponent
        return (base as NSString).deletingPathExtension
    }
}
