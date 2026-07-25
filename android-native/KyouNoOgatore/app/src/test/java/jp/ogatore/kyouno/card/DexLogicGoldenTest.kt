package jp.ogatore.kyouno.card

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

// ネイティブ移植 Step 7a: getDexStatus()のゴールデン(scripts-native/gen-dex-golden.mjsでWeb版実行結果を
// 採取)。card-golden.jsonと同一断面(rotAssign空初期化+2026-06-01〜2026-07-25の連続55日)で、
// DexLogic.getDexStatusが返すtoku/season/rare/normalの各tier・key・name・got状態をWeb版と1件ずつ突合する。

@Serializable
private data class GoldenItem(val tier: String, val key: String?, val name: String, val got: Boolean)

@Serializable
private data class DexGolden(
    val seedDateRangeStart: String,
    val seedDateRangeEnd: String,
    val toku: List<GoldenItem>,
    val season: List<GoldenItem>,
    val rare: List<GoldenItem>,
    val normal: List<GoldenItem>,
)

class DexLogicGoldenTest {
    @Test
    fun getDexStatusMatchesWebGolden() {
        val stream = javaClass.classLoader!!.getResourceAsStream("dex-golden.json")!!
        val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val golden = Json { ignoreUnknownKeys = true }.decodeFromString<DexGolden>(text)

        val dates = mutableListOf<String>()
        var d = java.time.LocalDate.parse(golden.seedDateRangeStart)
        val end = java.time.LocalDate.parse(golden.seedDateRangeEnd)
        while (!d.isAfter(end)) { dates.add(d.toString()); d = d.plusDays(1) }

        val status = DexLogic.getDexStatus(dates, dates.size, emptyMap())

        fun assertTier(name: String, want: List<GoldenItem>, got: List<DexItem>) {
            assertEquals("${name}件数", want.size, got.size)
            want.forEachIndexed { i, w ->
                val g = got[i]
                assertEquals("$name[$i].tier", w.tier, g.tier)
                assertEquals("$name[$i].key", w.key, g.key)
                assertEquals("$name[$i].name", w.name, g.name)
                assertEquals("$name[$i].got", w.got, g.got)
            }
        }
        assertTier("toku", golden.toku, status.toku)
        assertTier("season", golden.season, status.season)
        assertTier("rare", golden.rare, status.rare)
        assertTier("normal", golden.normal, status.normal)
    }
}
