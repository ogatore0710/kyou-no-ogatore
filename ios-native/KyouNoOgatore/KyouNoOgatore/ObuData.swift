//
//  ObuData.swift
//  KyouNoOgatore
//
//  obu-feed.json(scripts-native/gen-catalog.mjsがobu-feed.jsから生成。マスタープラン§2-1
//  "obu-feed.js OBU_FEED"行・§6 Step 7b)のDecodableモデル。type: "text"|"photo"|"radio"。

import Foundation
import RecordCore

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

// TASK-C2-2026-07-27-obu-fab-preview-popup.md: index.html:1275-1289
// obuIsLaterOrEqual/obuLatest/obuLatestByType の1:1移植。
func obuIsLaterOrEqual(_ a: ObuPost, _ b: ObuPost) -> Bool {
    if a.date != b.date { return a.date >= b.date }
    if let at = a.time, let bt = b.time { return at >= bt }
    return true
}

func obuLatest(_ posts: [ObuPost]) -> ObuPost? {
    guard var best = posts.first else { return nil }
    for post in posts.dropFirst() where obuIsLaterOrEqual(post, best) { best = post }
    return best
}

func obuLatestByType(_ posts: [ObuPost], _ type: String) -> ObuPost? {
    var best: ObuPost?
    for item in posts where item.type == type {
        if best == nil || obuIsLaterOrEqual(item, best!) { best = item }
    }
    return best
}

// index.html:1298-1306 OBU_STALE_DAYS/obuIsStaleDate/obuHasNew の1:1移植。todayは呼び出し元
// (UI層)がDate()から求めて渡す(§2-4: 純粋ロジック層に現在時刻を直接埋め込まない)。
let obuStaleDays = 30
func obuIsStaleDate(_ date: String, _ today: String) -> Bool {
    RecordLogic.daysBetween(date, today) >= obuStaleDays
}

func obuHasNew(_ posts: [ObuPost], _ seenId: String?, _ today: String) -> Bool {
    guard let latest = obuLatest(posts) else { return false }
    if obuIsStaleDate(latest.date, today) { return false }
    return seenId != latest.id
}
