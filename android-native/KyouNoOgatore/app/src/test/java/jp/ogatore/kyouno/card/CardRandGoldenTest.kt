package jp.ogatore.kyouno.card

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

// ネイティブ移植 Step 4: cardRand(mulberry32)の生出力ゴールデン(scripts-native/gen-card-rand-golden.mjs
// で採取。card-golden.jsonが検証しないビット単位のUInt32折り返し挙動を固定する)。

@Serializable
private data class RandGoldenCase(val seed: Long, val values: List<Double>)

class CardRandGoldenTest {
    @Test
    fun cardRandMatchesJSOutput() {
        val stream = javaClass.classLoader!!.getResourceAsStream("card-rand-golden.json")!!
        val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val cases: List<RandGoldenCase> = Json { ignoreUnknownKeys = true }.decodeFromString(text)
        assertFalse(cases.isEmpty())

        for (c in cases) {
            val rnd = CardLottery.cardRand(c.seed.toUInt())
            c.values.forEachIndexed { i, want ->
                val got = rnd()
                assertEquals("seed=${c.seed} index=$i", want, got, 1e-12)
            }
        }
    }
}
