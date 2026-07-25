package jp.ogatore.kyouno.card

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.floor

// ネイティブ移植 Step 4(マスタープラン§2-4・§6 Step 4): 記録カード抽選の決定的ロジック一式。
// index.html の cardRand/legacyRotPos/ensureRotAssign/cardRotPick/cardSeasonPick/cardFromRotPos/
// cardPatternFor / rotationIndex(2985行台〜2674・1675)の1:1移植。
//
// 現在時刻・乱数を直接読まない設計(§1-1第3項・§2-4末尾の禁止事項。厳守):
//   - 「いま」は必ず呼び出し側から now: Instant として渡す(rotationIndexのみ)。
//   - 過去日カードの再現性を壊すため、このファイルは Instant.now()/System.currentTimeMillis()相当・
//     kotlin.random/java.util.Random相当を一切呼ばない。cardRandは「日付から決まる種」を受け取るだけの
//     純粋な疑似乱数生成器で、システム乱数源には一切触れない。
data class CardPattern(
    val tier: String, // "toku" | "season" | "rare" | "normal"
    val name: String,
    val key: String?,
    val bg: List<String>? = null,
    val main: String? = null,
    val deco: List<String>? = null,
)

object CardLottery {
    // index.html:2674 cardRand(mulberry32)の忠実移植。
    // JSのMath.imul(32bit符号あり整数乗算の下位32bit)は、値をUIntのまま掛けた下位32bitと
    // ビットパターンが一致する(乗算のmod 2^32は符号解釈に依存しない)ため、UIntのラップ乗算(*)で
    // 再現できる。以降のxor・shrもすべてビット演算でありUIntのまま計算して差し支えない。
    fun cardRand(seed: UInt): () -> Double {
        var s = seed
        return {
            s += 0x6D2B79F5u
            var t: UInt = (s xor (s shr 15)) * (1u or s)
            t = (t + (t xor (t shr 7)) * (61u or t)) xor t
            (t xor (t shr 14)).toDouble() / 4294967296.0
        }
    }

    // index.html:132(app-card.js) dateIdx = floor((epochMs+9h)/86400000)。JSの`new Date(dateOnlyStr)`は
    // UTC真夜中パースなので、"YYYY-MM-DD"をUTC真夜中のepochとして解釈すれば1:1で再現できる。
    fun dateIdx(ds: String): Int {
        val epochMs = runCatching { LocalDate.parse(ds).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull() ?: return 0
        return floor((epochMs + 9 * 3600 * 1000).toDouble() / 86400000.0).toInt()
    }

    // index.html:1675 rotationIndex() = floor((Date.now()+6h)/86400000)。dayIndex()(index.html:1708)も
    // 同一式のエイリアス。JSは常に「いま」基準(引数なし)だが、ネイティブ側は呼び出し元からnowを注入する
    // (このファイル自身が現在時刻を読まない・決定的テストを可能にするための設計。RecordLogicと同じ方針)。
    fun rotationIndex(now: Instant): Int {
        return floor((now.toEpochMilli() + 6 * 3600 * 1000).toDouble() / 86400000.0).toInt()
    }

    // 旧方式(暦日dateIdx%50固定)。移行前にすでに見せた日を寸分違わず再現するためだけに残す。
    fun legacyRotPos(dateIdx: Int): Int {
        val order = CardDataLoader.shared.CARD_ROT_ORDER
        return order[((dateIdx % 50) + 50) % 50]
    }

    // 抽選プール=ノーマル20+レア30=50。posからカード内容を作る共通処理。
    fun cardFromRotPos(pos: Int): CardPattern {
        val data = CardDataLoader.shared
        if (pos < data.NORMAL_CARDS.size) {
            val t = data.NORMAL_CARDS[pos]
            return CardPattern(tier = "normal", name = t.name, key = null, bg = t.bg, main = t.main, deco = t.deco)
        }
        val r = data.RARE_CARDS[pos - data.NORMAL_CARDS.size]
        return CardPattern(tier = "rare", name = r.name, key = r.key, bg = r.bg)
    }

    // 季節: mmddが期間内の最初の1件(SEASON_CARDSの並び順=特定行事が先)。
    fun cardSeasonPick(ds: String): CardPattern? {
        val parts = ds.split("-")
        if (parts.size != 3) return null
        val mm = parts[1].toIntOrNull() ?: return null
        val dd = parts[2].toIntOrNull() ?: return null
        val mmdd = mm * 100 + dd
        for (s in CardDataLoader.shared.SEASON_CARDS) {
            if (s.ws != null && s.we != null && mmdd in s.ws..s.we) {
                return CardPattern(tier = "season", name = s.name, key = s.key, bg = s.bg)
            }
        }
        return null
    }

    // index.html:ensureRotAssign 相当。永続化された割当(kyono_rotAssign)を引数/戻り値で明示的に
    // やり取りする純粋関数として実装する(RecordStoreへの実配線はStep5以降のUI/データ結線の責務)。
    // 初回(existing が空)だけ、既存の記録日を旧方式(legacyRotPos)でバックフィルする(過去カード再現ルール)。
    fun ensureRotAssign(dates: List<String>, total: Int, existing: Map<String, Int>): Map<String, Int> {
        if (existing.isNotEmpty()) return existing
        val data = CardDataLoader.shared
        val rot = LinkedHashMap<String, Int>()
        val sorted = dates.sorted()
        val off = maxOf(0, total - dates.size)
        sorted.forEachIndexed { i, ds ->
            val di = dateIdx(ds)
            if (di < data.CARD_IMG_FROM) return@forEachIndexed // 画像方式が始まる前は抽選対象外
            val eff = i + 1 + off
            if (data.TOKU_CARDS.containsKey(eff.toString())) return@forEachIndexed // 記念日は抽選枠を消費しない
            if (data.MILESTONES.contains(eff)) return@forEachIndexed
            if (cardSeasonPick(ds) != null) return@forEachIndexed // 季節も抽選枠を消費しない
            rot[ds] = legacyRotPos(di)
        }
        return rot
    }

    // rot は呼び出し側が永続化する想定(戻り値の新しいMapに今回の割当を反映して返す)。
    fun cardRotPick(ds: String, rot: MutableMap<String, Int>): CardPattern {
        val existing = rot[ds]
        if (existing != null) return cardFromRotPos(existing)
        val seq = rot.size // これまでに実際に抽選対象になった回数(休み方に関係なく単調増加)
        val pos = CardDataLoader.shared.CARD_ROT_ORDER[seq % 50]
        rot[ds] = pos
        return cardFromRotPos(pos)
    }

    // その日のカードパターンを決める(CARD_IMG_FROM以降のみ画像方式・記念>季節>抽選)。
    // null = 従来drawCardに任せる(CARD_IMG_FROM未満、または画像のない節目=3日目のみ)。
    fun cardPatternFor(ds: String, effTotal: Int, dateIdx: Int, rot: MutableMap<String, Int>): CardPattern? {
        val data = CardDataLoader.shared
        if (dateIdx < data.CARD_IMG_FROM) return null
        data.TOKU_CARDS[effTotal.toString()]?.let { tk ->
            return CardPattern(tier = "toku", name = tk.name, key = tk.key, bg = tk.bg)
        }
        if (data.MILESTONES.contains(effTotal)) return null // 記念画像のない節目(3日目のみ)は従来のゴールドカードのまま
        cardSeasonPick(ds)?.let { return it }
        return cardRotPick(ds, rot)
    }
}
