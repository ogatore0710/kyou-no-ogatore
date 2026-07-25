package jp.ogatore.kyouno.safety

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

// ネイティブ移植 Step 2(マスタープラン§3-4手順2): norm-golden.json(16件)をJUnit4 Parameterizedで
// 1件=1テストケースとして全件アサートする。NFC/NFD合成濁点差・半角カナ・絵文字混在・「寝転」除去の
// 連結マッチ敵対ケースを含む。normOutputはJS実出力(soudan-ai-poc/norm.mjs)を正として固定した値。

@Serializable
data class NormGoldenCase(
    val system: String,
    val input: String,
    val normOutput: String,
    val redFlagHit: Boolean? = null,
)

private fun loadNormGolden(): List<NormGoldenCase> {
    val stream = NormGoldenCase::class.java.classLoader!!.getResourceAsStream("norm-golden.json")!!
    val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    return Json { ignoreUnknownKeys = true }.decodeFromString(text)
}

class NormGoldenCountTest {
    @Test
    fun goldenCountIs16() {
        assertEquals("norm-golden.jsonの件数が16でない", 16, loadNormGolden().size)
    }
}

@RunWith(Parameterized::class)
class NormGoldenFixtureTest(private val case: NormGoldenCase) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: [{0}]")
        fun data(): List<NormGoldenCase> = loadNormGolden()
    }

    @Test
    fun normMatchesGoldenOutput() {
        assertEquals("[${case.system}] ${case.input}", case.normOutput, SafetyGate.norm(case.input))
    }

    @Test
    fun redFlagHitMatchesGoldenWhenSpecified() {
        val want = case.redFlagHit ?: return // このケースはredFlagHitを検証対象外(normのみ)
        val n = SafetyGate.norm(case.input)
        assertEquals("[${case.system}/redFlagHit] ${case.input}", want, SafetyGate.redFlagHit(n))
    }
}
