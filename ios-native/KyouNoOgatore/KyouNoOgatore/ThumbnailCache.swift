//
//  ThumbnailCache.swift
//  KyouNoOgatore
//
//  TASK: サムネイル画像のオフライン用ディスクキャッシュ(弱電波下で検索/再生リスト画面の
//  サムネイルが毎回ネット取得になり空グリッドに見える問題への対策)。KyonoAsyncImage(View)から
//  ロジックだけを分離し、SwiftUI/UIKitに依存しない(Foundationのみ)ことで standalone swiftc
//  ドライバでも検証できるようにする。
//
//  保存先はCachesディレクトリ配下(呼び出し側でthumbnails/を指定)であり、記録データ本体
//  (kyono-store.json、Documentsディレクトリ)とは完全に分離する。キー=YouTube動画ID(サムネイル
//  URLはvideoId+size単位で不変なため、鮮度管理/期限切れは不要。書き込み済みなら常にキャッシュを
//  優先してよい)。上限は約50MBのハードキャップで、超過分は最終更新日時が古いファイルから順に
//  削除する(LRU相当)。

import Foundation

final class ThumbnailCache {
    static let defaultMaxBytes = 50 * 1024 * 1024

    private let cacheDir: URL
    private let maxBytes: Int
    private let fileManager: FileManager

    init(cacheDir: URL, maxBytes: Int = ThumbnailCache.defaultMaxBytes, fileManager: FileManager = .default) {
        self.cacheDir = cacheDir
        self.maxBytes = maxBytes
        self.fileManager = fileManager
        try? fileManager.createDirectory(at: cacheDir, withIntermediateDirectories: true)
    }

    func read(key: String) -> Data? {
        try? Data(contentsOf: fileURL(for: key))
    }

    func write(key: String, data: Data) {
        try? data.write(to: fileURL(for: key), options: .atomic)
        evictIfNeeded()
    }

    private func fileURL(for key: String) -> URL {
        cacheDir.appendingPathComponent(key + ".thumb")
    }

    private func evictIfNeeded() {
        guard let urls = try? fileManager.contentsOfDirectory(
            at: cacheDir,
            includingPropertiesForKeys: [.contentModificationDateKey, .fileSizeKey]
        ) else { return }

        var entries: [(url: URL, date: Date, size: Int)] = urls.compactMap { url in
            guard let values = try? url.resourceValues(forKeys: [.contentModificationDateKey, .fileSizeKey]),
                  let date = values.contentModificationDate,
                  let size = values.fileSize
            else { return nil }
            return (url, date, size)
        }

        var total = entries.reduce(0) { $0 + $1.size }
        guard total > maxBytes else { return }

        // 最終更新日時が古い順(=最も昔にキャッシュされたもの)から削除していく。
        entries.sort { $0.date < $1.date }
        for entry in entries {
            if total <= maxBytes { break }
            try? fileManager.removeItem(at: entry.url)
            total -= entry.size
        }
    }

    // youtubeThumbUrl()が組み立てる"https://i.ytimg.com/vi/<videoId>/<size>.jpg"からvideoIdを
    // 抜き出す。PlaylistThumbのように任意の"https://..."サムネイルURLが渡ってくる呼び出し口も
    // あるため、パターンに一致しない場合はURL全体の安定ハッシュ(FNV-1a)でキー化するフォールバック
    // を用意する(SwiftのString.hashValueはプロセスごとに乱数シード化されディスク間で不安定なため
    // 使えない)。
    static func key(forURL urlString: String) -> String {
        if let range = urlString.range(of: "/vi/([^/]+)/", options: .regularExpression) {
            let matched = urlString[range] // "/vi/xxxx/"
            let trimmed = matched.dropFirst("/vi/".count).dropLast(1)
            if !trimmed.isEmpty { return String(trimmed) }
        }
        return fnv1aHex(urlString)
    }

    private static func fnv1aHex(_ s: String) -> String {
        var hash: UInt64 = 0xcbf2_9ce4_8422_2325
        let prime: UInt64 = 0x0000_0100_0000_01B3
        for byte in s.utf8 {
            hash ^= UInt64(byte)
            hash = hash &* prime
        }
        return String(format: "%016llx", hash)
    }
}
