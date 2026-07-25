package jp.ogatore.kyouno.card

// ネイティブ移植 Step 7a(マスタープラン§6 Step 7a・図鑑UI): index.html:2474 getDexStatus() の1:1移植。
// ロック/アンロック判定はCardLottery.ensureRotAssign/cardSeasonPick(Step4で移植済み)を呼ぶだけで、
// 抽選ロジックそのものはここで一切再実装しない(検収基準2「Step4のCardLottery呼び出しのみ」)。
// このファイルがするのは「呼び出した結果を4段(記念日/季節/レア/ノーマル)に仕分けてヒント/フレーバー
// 文言を添える」という表示用の組み立てだけ。

data class DexItem(val tier: String, val key: String?, val name: String, val got: Boolean, val hint: String, val flavor: String)
data class DexStatus(val toku: List<DexItem>, val season: List<DexItem>, val rare: List<DexItem>, val normal: List<DexItem>)

object DexLogic {
    // index.html:2481 fmtMD の1:1移植。
    private fun fmtMd(w: Int): String = "${w / 100}/${w % 100}"

    fun getDexStatus(dates: List<String>, total: Int, rotAssign: Map<String, Int>): DexStatus {
        val data = CardDataLoader.shared
        val rot = CardLottery.ensureRotAssign(dates, total, rotAssign)
        val posSet = rot.values.toSet()
        val seasonHitKeys = dates.mapNotNull { ds -> CardLottery.cardSeasonPick(ds)?.key }.toSet()

        val toku = data.TOKU_CARDS.keys.mapNotNull { it.toIntOrNull() }.sorted().map { d ->
            val t = data.TOKU_CARDS.getValue(d.toString())
            val got = total >= d
            DexItem(
                tier = "toku", key = t.key, name = t.name, got = got,
                hint = if (got) "" else "あと${d - total}日続けると出会えます",
                flavor = if (got) data.DEX_FLAVOR[t.key] ?: "" else "",
            )
        }
        val season = data.SEASON_CARDS.map { s ->
            val got = seasonHitKeys.contains(s.key)
            DexItem(
                tier = "season", key = s.key, name = s.name, got = got,
                hint = if (got) "" else "${fmtMd(s.ws ?: 0)}〜${fmtMd(s.we ?: 0)}ごろに記録すると出会えるかも",
                flavor = if (got) data.DEX_FLAVOR[s.key] ?: "" else "",
            )
        }
        val rare = data.RARE_CARDS.mapIndexed { i, r ->
            val got = posSet.contains(data.NORMAL_CARDS.size + i)
            DexItem(
                tier = "rare", key = r.key, name = r.name, got = got,
                hint = if (got) "" else (data.DEX_TEASE[r.key] ?: "気まぐれに出てくる1枚。記録を続けてみて"),
                flavor = if (got) data.DEX_FLAVOR[r.key] ?: "" else "",
            )
        }
        val normal = data.NORMAL_CARDS.mapIndexed { i, n ->
            val got = posSet.contains(i)
            DexItem(
                tier = "normal", key = null, name = n.name, got = got,
                hint = if (got) "" else data.DEX_NORMAL_TEASE,
                flavor = if (got) data.DEX_FLAVOR_NORMAL[n.name] ?: "" else "",
            )
        }
        return DexStatus(toku, season, rare, normal)
    }
}
