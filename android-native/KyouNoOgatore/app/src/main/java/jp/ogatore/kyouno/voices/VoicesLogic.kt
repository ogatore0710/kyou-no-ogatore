package jp.ogatore.kyouno.voices

import jp.ogatore.kyouno.card.CardLottery
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ネイティブ移植 Step 7b(マスタープラン§6 Step 7b・§2-1「じまん/声/...」行): せんぱいの声(VOICES)の
// 日替わり選定ロジック。index.html:3880-3902 pickDailyVoices()の1:1移植。
//
// pickDailyVoices()自体はcardRand(mulberry32)と全く同じPRNGを使う(シードの作り方だけが違う:
// 日付epochでなく日付文字列のUTF-16コード単位を31進ハッシュしたもの)。Step4で移植済みの
// CardLottery.cardRandをそのまま呼び、ここでPRNGを再実装しない。
@Serializable
data class Voice(val tag: String, val front: String, val vid: String, val q: String, val src: String)

object VoicesLoader {
    val shared: List<Voice> by lazy {
        val stream = VoicesLoader::class.java.classLoader?.getResourceAsStream("voices.json")
            ?: error("voices.json がクラスパスに見つからない(app/src/main/resourcesの同梱漏れ)")
        val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        Json { ignoreUnknownKeys = true }.decodeFromString(text)
    }
}

object VoicesLogic {
    // index.html:3881-3882 の1:1移植。日付文字列("YYYY-MM-DD")のUTF-16コード単位を31進ハッシュする。
    private fun dailyHash(ds: String): UInt {
        var fh = 0u
        for (c in ds) fh = fh * 31u + c.code.toUInt()
        return fh
    }

    // index.html:3892-3901 の1:1移植(Fisher-Yatesシャッフル+先頭8件抽出)。乱数源はCardLottery.cardRand
    // (Step4)を呼ぶだけで、PRNGアルゴリズムそのものはここで再実装しない。
    fun pickDaily(today: String, voices: List<Voice> = VoicesLoader.shared): List<Voice> {
        val rnd = CardLottery.cardRand(dailyHash(today))
        val idx = MutableList(voices.size) { it }
        for (i in idx.size - 1 downTo 1) {
            val j = kotlin.math.floor(rnd() * (i + 1)).toInt()
            val tmp = idx[i]; idx[i] = idx[j]; idx[j] = tmp
        }
        val n = minOf(8, voices.size)
        return (0 until n).map { voices[idx[it]] }
    }
}
