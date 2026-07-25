package jp.ogatore.kyouno.voices

import org.junit.Assert.assertEquals
import org.junit.Test

// ネイティブ移植 Step 7b: pickDailyVoices()のゴールデン。2026-07-26のJS実出力(node -eで
// index.htmlのpickDailyVoices()と同一アルゴリズムを直接実行して採取)をハードコードした
// 期待indices([66,68,106,12,97,49,91,39])と突合する。CardLottery.cardRand(Step4済み)を
// 呼ぶだけで、シャッフルアルゴリズムそのものはVoicesLogic側で再実装していないことの回帰確認も兼ねる。
class VoicesLogicTest {
    @Test
    fun pickDailyMatchesJSOutputFor20260726() {
        val expectedIdx = listOf(66, 68, 106, 12, 97, 49, 91, 39)
        val voices = VoicesLoader.shared
        val expected = expectedIdx.map { voices[it] }
        val actual = VoicesLogic.pickDaily("2026-07-26", voices)
        assertEquals(expected, actual)
    }

    @Test
    fun pickDailyReturnsEightUniqueVoices() {
        val picked = VoicesLogic.pickDaily("2026-01-01")
        assertEquals(8, picked.size)
        assertEquals(8, picked.map { it.vid }.toSet().size)
    }
}
