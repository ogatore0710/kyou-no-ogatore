package jp.ogatore.kyouno.card

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// ネイティブ移植 Step 4(マスタープラン§6 Step4検収基準1): card-golden.json(Step0採取・55件)を
// scripts-native/verify-card-data.mjsと同じ手順で再生し、JS実出力(dateIdx/effTotal/milestone/
// isImgEra/isThemeV2Era/pat/rotAssignPos)と全一致することを確認する。
// note通り: streak2.datesは2026-06-01〜2026-07-25の連続55日(=cases自体)。rotAssignは空から出発
// (ensureRotAssignで一括バックフィル。Step0が明示的に指定した仕様)。

@Serializable
private data class GoldenPattern(val tier: String, val name: String, val key: String? = null)

@Serializable
private data class GoldenCase(
    val ds: String,
    val dateIdx: Int,
    val effTotal: Int,
    val milestone: Boolean,
    val isImgEra: Boolean,
    val isThemeV2Era: Boolean,
    val pat: GoldenPattern? = null,
    val rotAssignPos: Int? = null,
)

@Serializable
private data class GoldenFile(val cases: List<GoldenCase>)

class CardLotteryTest {
    private fun loadGolden(): GoldenFile {
        val stream = javaClass.classLoader!!.getResourceAsStream("card-golden.json")!!
        val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        return Json { ignoreUnknownKeys = true }.decodeFromString(text)
    }

    @Test
    fun cardGolden55CasesMatchJSOutput() {
        val golden = loadGolden()
        assertEquals("card-golden.jsonの件数が55でない", 55, golden.cases.size)

        val dates = golden.cases.map { it.ds }
        val total = dates.size
        val rot = CardLottery.ensureRotAssign(dates, total, emptyMap()).toMutableMap()
        val data = CardDataLoader.shared

        val failures = mutableListOf<String>()
        golden.cases.forEachIndexed { i, c ->
            val effTotal = i + 1
            val di = CardLottery.dateIdx(c.ds)
            val milestone = data.MILESTONES.contains(effTotal)
            val isImgEra = di >= data.CARD_IMG_FROM
            val isThemeV2Era = di >= data.CARD_THEMES_V2_FROM
            val pat = CardLottery.cardPatternFor(c.ds, effTotal, di, rot)
            val rotAssignPos: Int? = if (isImgEra && (pat?.tier == "normal" || pat?.tier == "rare")) rot[c.ds] else null

            if (di != c.dateIdx) failures.add("${c.ds} dateIdx: got=$di want=${c.dateIdx}")
            if (effTotal != c.effTotal) failures.add("${c.ds} effTotal: got=$effTotal want=${c.effTotal}")
            if (milestone != c.milestone) failures.add("${c.ds} milestone: got=$milestone want=${c.milestone}")
            if (isImgEra != c.isImgEra) failures.add("${c.ds} isImgEra: got=$isImgEra want=${c.isImgEra}")
            if (isThemeV2Era != c.isThemeV2Era) failures.add("${c.ds} isThemeV2Era: got=$isThemeV2Era want=${c.isThemeV2Era}")
            val gotPat = pat?.let { GoldenPattern(it.tier, it.name, it.key) }
            if (gotPat != c.pat) failures.add("${c.ds} pat: got=$gotPat want=${c.pat}")
            if (rotAssignPos != c.rotAssignPos) failures.add("${c.ds} rotAssignPos: got=$rotAssignPos want=${c.rotAssignPos}")
        }
        assertTrue("不一致 ${failures.size}/${golden.cases.size} 件:\n" + failures.joinToString("\n"), failures.isEmpty())
        if (failures.isEmpty()) println("card-golden: ${golden.cases.size}/${golden.cases.size} match")
    }
}
