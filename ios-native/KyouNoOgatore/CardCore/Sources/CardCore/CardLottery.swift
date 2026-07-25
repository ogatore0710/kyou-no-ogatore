import Foundation

// ネイティブ移植 Step 4(マスタープラン§2-4・§6 Step 4): 記録カード抽選の決定的ロジック一式。
// index.html の cardRand/legacyRotPos/ensureRotAssign/cardRotPick/cardSeasonPick/cardFromRotPos/
// cardPatternFor / rotationIndex(2985行台〜2674・1675)の1:1移植。
//
// 現在時刻・乱数を直接読まない設計(§1-1第3項・§2-4末尾の禁止事項。厳守):
//   - 「いま」は必ず呼び出し側から `now: Date` として渡す(rotationIndexのみ)。
//   - 過去日カードの再現性を壊すため、このファイルは Date()/Date.now()相当・Int.random/arc4random
//     相当を一切呼ばない。cardRandは「日付から決まる種」を受け取るだけの純粋な疑似乱数生成器で、
//     システム乱数源(SecRandom等)には一切触れない。
public struct CardPattern: Equatable {
    public let tier: String // "toku" | "season" | "rare" | "normal"
    public let name: String
    public let key: String?
    public let bg: [String]?
    public let main: String?
    public let deco: [String]?
}

public enum CardLottery {
    // index.html:2674 cardRand(mulberry32)の忠実移植。
    // JSのMath.imul(32bit符号あり整数乗算の下位32bit)は、値をUInt32のまま掛けた下位32bitと
    // ビットパターンが一致する(乗算のmod 2^32は符号解釈に依存しない)ため、UInt32のラップ乗算(&*)で
    // 再現できる。以降の^・>>>もすべてビット演算でありUInt32のまま計算して差し支えない。
    public static func cardRand(seed: UInt32) -> () -> Double {
        var s = seed
        return {
            s = s &+ 0x6D2B79F5
            var t: UInt32 = (s ^ (s >> 15)) &* (1 | s)
            t = (t &+ (t ^ (t >> 7)) &* (61 | t)) ^ t
            return Double(t ^ (t >> 14)) / 4294967296.0
        }
    }

    // index.html:132(app-card.js) dateIdx = floor((epochMs+9h)/86400000)。JSの`new Date(dateOnlyStr)`は
    // UTC真夜中パースなので、"YYYY-MM-DD"をUTC真夜中のepochとして解釈すれば1:1で再現できる。
    public static func dateIdx(_ ds: String) -> Int {
        guard let epochMs = utcMidnightEpochMs(ds) else { return 0 }
        return Int(floor((epochMs + 9 * 3600 * 1000) / 86400000))
    }

    // index.html:1675 rotationIndex() = floor((Date.now()+6h)/86400000)。dayIndex()(index.html:1708)も
    // 同一式のエイリアス。JSは常に「いま」基準(引数なし)だが、ネイティブ側は呼び出し元からnowを注入する
    // (このファイル自身がDate()を呼ばない・決定的テストを可能にするための設計。RecordLogicと同じ方針)。
    public static func rotationIndex(now: Date) -> Int {
        Int(floor((now.timeIntervalSince1970 * 1000 + 6 * 3600 * 1000) / 86400000))
    }

    private static func utcMidnightEpochMs(_ ds: String) -> Double? {
        let parts = ds.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return nil }
        var utc = Calendar(identifier: .gregorian)
        utc.timeZone = TimeZone(identifier: "UTC")!
        var c = DateComponents()
        c.year = parts[0]; c.month = parts[1]; c.day = parts[2]
        guard let d = utc.date(from: c) else { return nil }
        return d.timeIntervalSince1970 * 1000
    }

    // 旧方式(暦日dateIdx%50固定)。移行前にすでに見せた日を寸分違わず再現するためだけに残す。
    public static func legacyRotPos(_ dateIdx: Int) -> Int {
        let order = CardDataLoader.shared.CARD_ROT_ORDER
        return order[((dateIdx % 50) + 50) % 50]
    }

    // 抽選プール=ノーマル20+レア30=50。posからカード内容を作る共通処理。
    public static func cardFromRotPos(_ pos: Int) -> CardPattern {
        let data = CardDataLoader.shared
        if pos < data.NORMAL_CARDS.count {
            let t = data.NORMAL_CARDS[pos]
            return CardPattern(tier: "normal", name: t.name, key: nil, bg: t.bg, main: t.main, deco: t.deco)
        }
        let r = data.RARE_CARDS[pos - data.NORMAL_CARDS.count]
        return CardPattern(tier: "rare", name: r.name, key: r.key, bg: r.bg, main: nil, deco: nil)
    }

    // 季節: mmddが期間内の最初の1件(SEASON_CARDSの並び順=特定行事が先)。
    public static func cardSeasonPick(_ ds: String) -> CardPattern? {
        let parts = ds.split(separator: "-")
        guard parts.count == 3, let mm = Int(parts[1]), let dd = Int(parts[2]) else { return nil }
        let mmdd = mm * 100 + dd
        for s in CardDataLoader.shared.SEASON_CARDS {
            if let ws = s.ws, let we = s.we, mmdd >= ws, mmdd <= we {
                return CardPattern(tier: "season", name: s.name, key: s.key, bg: s.bg, main: nil, deco: nil)
            }
        }
        return nil
    }

    // index.html:ensureRotAssign 相当。永続化された割当(kyono_rotAssign)を引数/戻り値で明示的に
    // やり取りする純粋関数として実装する(RecordStoreへの実配線はStep5以降のUI/データ結線の責務)。
    // 初回(rot が空)だけ、既存の記録日を旧方式(legacyRotPos)でバックフィルする(過去カード再現ルール)。
    public static func ensureRotAssign(dates: [String], total: Int, existing: [String: Int]) -> [String: Int] {
        if !existing.isEmpty { return existing }
        let data = CardDataLoader.shared
        var rot: [String: Int] = [:]
        let sorted = dates.sorted()
        let off = max(0, total - dates.count)
        for (i, ds) in sorted.enumerated() {
            let di = dateIdx(ds)
            if di < data.CARD_IMG_FROM { continue } // 画像方式が始まる前は抽選対象外
            let eff = i + 1 + off
            if data.TOKU_CARDS[String(eff)] != nil { continue } // 記念日は抽選枠を消費しない
            if data.MILESTONES.contains(eff) { continue }
            if cardSeasonPick(ds) != nil { continue } // 季節も抽選枠を消費しない
            rot[ds] = legacyRotPos(di)
        }
        return rot
    }

    // rot は呼び出し側が永続化する想定(inout で今回の割当を反映して返す)。
    public static func cardRotPick(_ ds: String, rot: inout [String: Int]) -> CardPattern {
        if let pos = rot[ds] { return cardFromRotPos(pos) }
        let seq = rot.count // これまでに実際に抽選対象になった回数(休み方に関係なく単調増加)
        let pos = CardDataLoader.shared.CARD_ROT_ORDER[seq % 50]
        rot[ds] = pos
        return cardFromRotPos(pos)
    }

    // その日のカードパターンを決める(CARD_IMG_FROM以降のみ画像方式・記念>季節>抽選)。
    // nil = 従来drawCardに任せる(CARD_IMG_FROM未満、または画像のない節目=3日目のみ)。
    public static func cardPatternFor(ds: String, effTotal: Int, dateIdx: Int, rot: inout [String: Int]) -> CardPattern? {
        let data = CardDataLoader.shared
        if dateIdx < data.CARD_IMG_FROM { return nil }
        if let tk = data.TOKU_CARDS[String(effTotal)] {
            return CardPattern(tier: "toku", name: tk.name, key: tk.key, bg: tk.bg, main: nil, deco: nil)
        }
        if data.MILESTONES.contains(effTotal) { return nil } // 記念画像のない節目(3日目のみ)は従来のゴールドカードのまま
        if let sp = cardSeasonPick(ds) { return sp }
        return cardRotPick(ds, rot: &rot)
    }
}
