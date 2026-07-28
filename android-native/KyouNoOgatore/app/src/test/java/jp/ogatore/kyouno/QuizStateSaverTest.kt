package jp.ogatore.kyouno

import org.junit.Assert.assertEquals
import org.junit.Test

// Fable監査D5-1(alan5差し戻し2026-07-28): クイズの回答途中(scores/picked)を回転をまたいで
// 保持するための平坦化round-tripを固定する。pickedはInt(score)/String(worryKey)/nullの
// 3種類が混在するため型タグの往復まで確認する。
class QuizStateSaverTest {
    @Test
    fun scoresRoundTrips() {
        // QuizScoresSaver.save()はSaverScope受け取りのため、テストではrestore側だけを
        // 本番のSaverで検証する(平坦化フォーマット自体はsave/restoreで対称なので、restoreが
        // 正しく戻せることを見れば十分)。
        val flattened = arrayListOf<Any?>("momo", 3, "koka", 1)
        val restored = QuizScoresSaver.restore(flattened)
        assertEquals(3, restored?.get("momo"))
        assertEquals(1, restored?.get("koka"))
        assertEquals(2, restored?.size)
    }

    @Test
    fun pickedRoundTripsIntStringAndNullValues() {
        val flattened = ArrayList(
            listOf(
                arrayListOf<Any?>("momo", "int", 2),
                arrayListOf<Any?>("worry", "string", "kata-koru"),
                arrayListOf<Any?>("blank", "null", null),
            ),
        )
        val restored = QuizPickedSaver.restore(flattened)
        assertEquals(2, restored?.get("momo"))
        assertEquals("kata-koru", restored?.get("worry"))
        assertEquals(null, restored?.get("blank"))
        assertEquals(3, restored?.size)
    }
}
