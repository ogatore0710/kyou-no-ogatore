package jp.ogatore.kyouno

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

// 診断結果画面「おすすめ動画3本」欠落修正タスク(TASK-C2-2026-07-26-result-video-recommendations.md):
// currentRx()の決定性・Web版app-quiz.js currentRx()との出力一致を確認する単体テスト。
// 期待値はTYPES/currentRx()と同じロジックをNode.jsで実行して得たWeb版の実出力(§検収基準3)。
class CurrentRxTest {
    @Test
    fun matchesWebOutputAtEpochZero() {
        val now = Instant.ofEpochMilli(0)
        assertEquals(listOf("momo7", "kaikyaku", "kaikyaku2"), currentRx("momo", now))
        assertEquals(listOf("koka9", "kominka", "kokaPoki"), currentRx("koka", now))
        assertEquals(listOf("kenko12", "asa5", "katakori"), currentRx("kenko", now))
        assertEquals(listOf("ashi1", "ashi2", "fukuraMassa"), currentRx("ashi", now))
        assertEquals(listOf("honki9", "asa10", "zenshinCho"), currentRx("robot", now))
        assertEquals(listOf("asa10", "asa9shi", "jukusui9"), currentRx("yawara", now))
    }

    @Test
    fun matchesWebOutputAtArbitraryTimestamp() {
        val now = Instant.ofEpochMilli(1753574400000)
        assertEquals(listOf("momo7", "momoKai", "kotsuban5"), currentRx("momo", now))
        assertEquals(listOf("koka9", "kokaSai", "kaikyaku"), currentRx("koka", now))
        assertEquals(listOf("kenko12", "katakori8", "kenkoIsho"), currentRx("kenko", now))
        assertEquals(listOf("ashi1", "fukura8", "ashi10"), currentRx("ashi", now))
        assertEquals(listOf("honki9", "asaBaki9", "yoru12kai"), currentRx("robot", now))
        assertEquals(listOf("asa10", "asaGachi5", "jiritsu10"), currentRx("yawara", now))
    }

    @Test
    fun isDeterministicForSameDay() {
        val a = Instant.ofEpochMilli(1721692800000)
        val b = Instant.ofEpochMilli(1721692800000 + 3600_000) // 同じ日の1時間後
        assertEquals(currentRx("momo", a), currentRx("momo", b))
    }

    @Test
    fun differsAcrossDaysForAtLeastOneType() {
        val day1 = Instant.ofEpochMilli(1721692800000)
        val day2 = Instant.ofEpochMilli(1753574400000)
        assertEquals(false, currentRx("momo", day1) == currentRx("momo", day2))
    }
}
