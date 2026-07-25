package jp.ogatore.kyouno.card

import java.time.Instant

// ネイティブ移植 Step 4(マスタープラン§2-4・§6 Step 4): かたさ診断のタイプ判定(app-quiz.js:194
// decideType)の1:1移植。同点は①Q5「いちばんの悩み」対応部位→②日付ローテーション(rotationIndex)の
// 2段で決める(app-quiz.js:191-207)。
//
// 現在時刻を直接読まない設計(§1-1第3項・§2-4末尾の禁止事項。厳守): rotationIndexが必要とする「いま」は
// 呼び出し側から now: Instant として注入する(CardLottery.rotationIndexと同じ方針)。
data class QuizScores(val momo: Int, val koka: Int, val kenko: Int, val ashi: Int)

object QuizEngine {
    // app-quiz.js:193 WORRY_TIEBREAK の忠実移植。悩みキー→タイブレーク優先部位リスト。
    private val worryTiebreak: Map<String, List<String>> = mapOf(
        "katakori" to listOf("kenko"),
        "yotsu" to listOf("momo", "koka"),
        "tsukare" to emptyList(),
        "yawaraka" to emptyList(),
    )

    // app-quiz.js:194 decideType の忠実移植。
    fun decideType(s: QuizScores, worry: String?, now: Instant): String {
        val total = s.momo + s.koka + s.kenko + s.ashi
        if (total >= 9) return "robot"
        if (total <= 2) return "yawara"
        val maxScore = maxOf(maxOf(s.momo, s.koka), maxOf(s.kenko, s.ashi))
        if (maxScore <= 1) return "yawara"
        val all = listOf("momo" to s.momo, "koka" to s.koka, "kenko" to s.kenko, "ashi" to s.ashi)
        val holders = all.filter { it.second == maxScore }.map { it.first }
        if (holders.size == 1) return holders[0]
        val pref = worryTiebreak[worry] ?: emptyList()
        for (k in pref) if (k in holders) return k
        val r = CardLottery.rotationIndex(now)
        return holders[r % holders.size]
    }
}
