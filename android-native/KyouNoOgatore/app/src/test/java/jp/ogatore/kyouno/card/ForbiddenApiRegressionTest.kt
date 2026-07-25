package jp.ogatore.kyouno.card

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// ネイティブ移植 Step 4(マスタープラン§2-4末尾): 「禁止API(乱数・現在時刻)がCardRenderer/CardLottery/
// QuizEngine/RecordLogicに存在しない」ことを毎回のテスト実行で機械的に縛る(§6 Step4検収基準3)。
//
// コメント行(`//`以降)は判定対象から除外する。このテスト自身やソース中の説明コメントが
// 「Instant.now()を呼ばない」のように禁止API名そのものを引用すると誤検知するため、各行の`//`より
// 前の部分だけを見る(split(omittingEmptySubsequences=false)必須。既定のtrueだと行頭"//"コメント行で
// 空の「コード部分」が捨てられ、firstOrNull()がコメント本文そのものを返してしまう)。
class ForbiddenApiRegressionTest {
    // 単純な部分文字列一致だと"toLocalDate()"のような無関係な呼び出しが"Date()"に誤爆するため、
    // 識別子文字("A-Za-z0-9_")で始まるパターンだけ、直前が識別子文字でないこと(単語境界)を要求する。
    // "."始まりのパターン(.random(等)は"."自体が識別子文字でないぶん元から誤爆しないため対象外
    // (むしろ単語境界を強制すると「レシーバ式.random(」の正当な呼び出しまで検知できなくなってしまう)。
    private val forbiddenPatterns = listOf(
        "Instant.now(", "System.currentTimeMillis(", "Date()", "kotlin.random", ".random(",
        "java.util.Random", "Math.random(", "Random.nextInt", "Random.nextDouble",
    ).map { pattern ->
        val guard = if (pattern.first().isLetter()) "(?<![A-Za-z0-9_])" else ""
        Regex(guard + Regex.escape(pattern))
    }

    private val targetFiles = listOf(
        "card/CardLottery.kt" to "CardLottery.kt",
        "card/CardRenderer.kt" to "CardRenderer.kt",
        "card/QuizEngine.kt" to "QuizEngine.kt",
        "record/RecordLogic.kt" to "RecordLogic.kt",
    )

    @Test
    fun noForbiddenTimeOrRandomApis() {
        // このテストクラス自身のパッケージ(jp/ogatore/kyouno/card)から app/src/main/java/jp/ogatore/kyouno/ を逆算する
        val thisTestFile = File(
            "src/test/java/${javaClass.packageName.replace('.', '/')}/ForbiddenApiRegressionTest.kt",
        )
        val kyounoRoot = thisTestFile.absoluteFile.parentFile!! // .../src/test/java/jp/ogatore/kyouno/card
            .parentFile!! // .../jp/ogatore/kyouno
        val mainRoot = File(kyounoRoot.path.replace("/src/test/java/", "/src/main/java/"))

        val violations = mutableListOf<String>()
        for ((relPath, label) in targetFiles) {
            val file = File(mainRoot, relPath)
            assertTrue("対象ファイルが見つからない: ${file.path}", file.exists())
            file.readLines().forEachIndexed { i, rawLine ->
                val idx = rawLine.indexOf("//")
                val code = if (idx >= 0) rawLine.substring(0, idx) else rawLine
                for (pattern in forbiddenPatterns) {
                    if (pattern.containsMatchIn(code)) violations.add("$label:${i + 1}: $pattern — ${code.trim()}")
                }
            }
        }
        assertTrue("禁止API使用が見つかった:\n" + violations.joinToString("\n"), violations.isEmpty())
    }
}
