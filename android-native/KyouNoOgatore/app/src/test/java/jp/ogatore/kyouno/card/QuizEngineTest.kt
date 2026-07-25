package jp.ogatore.kyouno.card

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

// ネイティブ移植 Step 4(マスタープラン§6 Step4検収基準2): decideTypeの単体テスト。
// scripts/qa.jsのcheckQuizTypeTiebreak(2026-07-20 PO承認「かたさタイプ同点タイブレーク」)と
// 同じ観点をJUnitに移植する。
class QuizEngineTest {
    private fun date(epochDay: Int): Instant {
        // rotationIndex(now) = floor((now_ms + 6h) / 86400000) となるよう、指定の日数ぶんのepochを渡す。
        // r=0..11の各値をピンポイントで作るための最小限のヘルパー(UTC基準で「r日目の正午」を使えば
        // +6hオフセットを跨がずrotationIndex()==epochDayになる)。
        return Instant.ofEpochSecond(epochDay.toLong() * 86400 + 43200)
    }

    // (1) robot/yawaraのゲートは同点処理より優先(qa.js相当)
    @Test
    fun robotYawaraGatesTakePriorityOverTiebreak() {
        assertEquals("robot", QuizEngine.decideType(QuizScores(3, 3, 3, 0), null, date(0)))
        assertEquals("yawara", QuizEngine.decideType(QuizScores(2, 0, 0, 0), null, date(0)))
        assertEquals("yawara", QuizEngine.decideType(QuizScores(1, 1, 1, 1), null, date(0)))
    }

    // (2) 単独最高点は悩み・日付に関係なくその部位
    @Test
    fun singleMaxHolderWinsRegardlessOfWorryOrDate() {
        assertEquals("ashi", QuizEngine.decideType(QuizScores(0, 1, 0, 2), "katakori", date(5)))
    }

    // (3) 悩みタイブレーク: 同点の中に悩み対応部位があればそれを選ぶ
    @Test
    fun worryTiebreakPicksMatchingHolder() {
        assertEquals("kenko", QuizEngine.decideType(QuizScores(2, 2, 2, 2), "katakori", date(5)))
        assertEquals("momo", QuizEngine.decideType(QuizScores(2, 2, 2, 2), "yotsu", date(5)))
        assertEquals("koka", QuizEngine.decideType(QuizScores(1, 2, 1, 2), "yotsu", date(5))) // 第2候補
    }

    // (4) 悩みで決まらない同点は日付ローテーションで決定的に散る(再現性・rでの切り替わり)
    @Test
    fun rotationTiebreakIsDeterministicAndVariesByDate() {
        val r0 = QuizEngine.decideType(QuizScores(2, 1, 1, 2), "yawaraka", date(0))
        assertEquals("momo", r0)
        assertEquals("同一入力・同一rなら同一結果", r0, QuizEngine.decideType(QuizScores(2, 1, 1, 2), "yawaraka", date(0)))
        assertEquals("ashi", QuizEngine.decideType(QuizScores(2, 1, 1, 2), "yawaraka", date(1)))
        assertEquals(
            "肩こりでも同点にkenkoが居なければローテーションへ",
            "koka",
            QuizEngine.decideType(QuizScores(2, 2, 1, 0), "katakori", date(3)),
        )
    }

    // (5) 分布の対称性(§6 Step4検収基準2): 全256通り×r=0..11の合算で4部位の当選数が完全一致=各603
    @Test
    fun distributionOver256CombosTimes12Rotations() {
        val counts = mutableMapOf("momo" to 0, "koka" to 0, "kenko" to 0, "ashi" to 0)
        for (r in 0 until 12) {
            val now = date(r)
            for (a in 0 until 4) for (b in 0 until 4) for (c in 0 until 4) for (d in 0 until 4) {
                val t = QuizEngine.decideType(QuizScores(a, b, c, d), null, now)
                counts[t]?.let { counts[t] = it + 1 }
            }
        }
        assertEquals(mapOf("momo" to 603, "koka" to 603, "kenko" to 603, "ashi" to 603), counts)
    }
}
