//
//  VoicesLogic.swift
//  KyouNoOgatore
//
//  ネイティブ移植 Step 7b(マスタープラン§6 Step 7b・§2-1「じまん/声/...」行): せんぱいの声(VOICES)の
//  日替わり選定ロジック(Android版VoicesLogic.ktと同一ロジック)。index.html:3880-3902
//  pickDailyVoices()の1:1移植。
//
//  pickDailyVoices()自体はcardRand(mulberry32)と全く同じPRNGを使う(シードの作り方だけが違う:
//  日付epochでなく日付文字列のUTF-16コード単位を31進ハッシュしたもの)。Step4で移植済みの
//  CardLottery.cardRandをそのまま呼び、ここでPRNGを再実装しない。

import Foundation
import CardCore

struct Voice: Decodable, Equatable {
    let tag: String
    let front: String
    let vid: String
    let q: String
    let src: String
}

enum VoicesLoader {
    static let shared: [Voice] = {
        guard let url = Bundle.main.url(forResource: "voices", withExtension: "json") else {
            fatalError("voices.json がアプリバンドルに同梱されていない")
        }
        do {
            return try JSONDecoder().decode([Voice].self, from: try Data(contentsOf: url))
        } catch {
            fatalError("voices.json のデコードに失敗: \(error)")
        }
    }()
}

enum VoicesLogic {
    // index.html:3881-3882 の1:1移植。日付文字列("YYYY-MM-DD")のUTF-16コード単位を31進ハッシュする。
    private static func dailyHash(_ ds: String) -> UInt32 {
        var fh: UInt32 = 0
        for u in ds.utf16 { fh = fh &* 31 &+ UInt32(u) }
        return fh
    }

    // index.html:3892-3901 の1:1移植(Fisher-Yatesシャッフル+先頭8件抽出)。乱数源はCardLottery.cardRand
    // (Step4)を呼ぶだけで、PRNGアルゴリズムそのものはここで再実装しない。
    static func pickDaily(today: String, voices: [Voice] = VoicesLoader.shared) -> [Voice] {
        let rnd = CardLottery.cardRand(seed: dailyHash(today))
        var idx = Array(0..<voices.count)
        var i = idx.count - 1
        while i > 0 {
            let j = Int(Foundation.floor(rnd() * Double(i + 1)))
            idx.swapAt(i, j)
            i -= 1
        }
        let n = min(8, voices.count)
        return (0..<n).map { voices[idx[$0]] }
    }
}
