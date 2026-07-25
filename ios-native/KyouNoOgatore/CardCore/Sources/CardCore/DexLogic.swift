import Foundation

// ネイティブ移植 Step 7a(マスタープラン§6 Step 7a・図鑑UI): index.html:2474 getDexStatus() の1:1移植。
// ロック/アンロック判定はCardLottery.ensureRotAssign/cardSeasonPick(Step4で移植済み)を呼ぶだけで、
// 抽選ロジックそのものはここで一切再実装しない(検収基準2「Step4のCardLottery呼び出しのみ」)。
// このファイルがするのは「呼び出した結果を4段(記念日/季節/レア/ノーマル)に仕分けてヒント/フレーバー
// 文言を添える」という表示用の組み立てだけ。

public struct DexItem: Equatable {
    public let tier: String
    public let key: String?
    public let name: String
    public let got: Bool
    public let hint: String
    public let flavor: String
}

public struct DexStatus: Equatable {
    public let toku: [DexItem]
    public let season: [DexItem]
    public let rare: [DexItem]
    public let normal: [DexItem]
}

public enum DexLogic {
    // index.html:2481 fmtMD の1:1移植。
    private static func fmtMd(_ w: Int) -> String { "\(w / 100)/\(w % 100)" }

    public static func getDexStatus(dates: [String], total: Int, rotAssign: [String: Int]) -> DexStatus {
        let data = CardDataLoader.shared
        let rot = CardLottery.ensureRotAssign(dates: dates, total: total, existing: rotAssign)
        let posSet = Set(rot.values)
        let seasonHitKeys = Set(dates.compactMap { ds in CardLottery.cardSeasonPick(ds)?.key })

        let toku = data.TOKU_CARDS.keys.compactMap { Int($0) }.sorted().map { d -> DexItem in
            let t = data.TOKU_CARDS[String(d)]!
            let got = total >= d
            return DexItem(
                tier: "toku", key: t.key, name: t.name, got: got,
                hint: got ? "" : "あと\(d - total)日続けると出会えます",
                flavor: got ? (data.DEX_FLAVOR[t.key] ?? "") : ""
            )
        }
        let season = data.SEASON_CARDS.map { s -> DexItem in
            let got = seasonHitKeys.contains(s.key)
            return DexItem(
                tier: "season", key: s.key, name: s.name, got: got,
                hint: got ? "" : "\(fmtMd(s.ws ?? 0))〜\(fmtMd(s.we ?? 0))ごろに記録すると出会えるかも",
                flavor: got ? (data.DEX_FLAVOR[s.key] ?? "") : ""
            )
        }
        let rare = data.RARE_CARDS.enumerated().map { i, r -> DexItem in
            let got = posSet.contains(data.NORMAL_CARDS.count + i)
            return DexItem(
                tier: "rare", key: r.key, name: r.name, got: got,
                hint: got ? "" : (data.DEX_TEASE[r.key] ?? "気まぐれに出てくる1枚。記録を続けてみて"),
                flavor: got ? (data.DEX_FLAVOR[r.key] ?? "") : ""
            )
        }
        let normal = data.NORMAL_CARDS.enumerated().map { i, n -> DexItem in
            let got = posSet.contains(i)
            return DexItem(
                tier: "normal", key: nil, name: n.name, got: got,
                hint: got ? "" : data.DEX_NORMAL_TEASE,
                flavor: got ? (data.DEX_FLAVOR_NORMAL[n.name] ?? "") : ""
            )
        }
        return DexStatus(toku: toku, season: season, rare: rare, normal: normal)
    }
}
