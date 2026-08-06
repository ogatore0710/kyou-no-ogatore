package jp.ogatore.kyouno

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

// 診断結果画面「おすすめ動画3本」欠落修正タスク(TASK-C2-2026-07-26-result-video-recommendations.md):
// currentRx()の決定性を確認する単体テスト。
// TASK-C2-2026-08-06-build30-round8.md R-26: 「メイン(固定1本)+しあげ(ローテ1本)」の計2本仕様へ
// 変更(Web版は本人ゲートで駐車中のため今回触らない=Web版とは意図的に出力が異なる)。期待値は
// 新ロジック(need=1・spacing=pool全長・重複回避は現行踏襲)の手計算値で、旧Web版実出力の
// 「先頭2本」と一致する(i=0の選出はspacing変更の影響を受けないため)。
class CurrentRxTest {
    @Test
    fun matchesWebOutputAtEpochZero() {
        val now = Instant.ofEpochMilli(0)
        assertEquals(listOf("momo7", "kaikyaku"), currentRx("momo", now))
        assertEquals(listOf("koka9", "kominka"), currentRx("koka", now))
        assertEquals(listOf("kenko12", "asa5"), currentRx("kenko", now))
        assertEquals(listOf("ashi1", "ashi2"), currentRx("ashi", now))
        assertEquals(listOf("honki9", "asa10"), currentRx("robot", now))
        assertEquals(listOf("asa10", "asa9shi"), currentRx("yawara", now))
    }

    @Test
    fun matchesWebOutputAtArbitraryTimestamp() {
        val now = Instant.ofEpochMilli(1753574400000)
        assertEquals(listOf("momo7", "momoKai"), currentRx("momo", now))
        assertEquals(listOf("koka9", "kokaSai"), currentRx("koka", now))
        assertEquals(listOf("kenko12", "katakori8"), currentRx("kenko", now))
        assertEquals(listOf("ashi1", "fukura8"), currentRx("ashi", now))
        assertEquals(listOf("honki9", "asaBaki9"), currentRx("robot", now))
        assertEquals(listOf("asa10", "asaGachi5"), currentRx("yawara", now))
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
