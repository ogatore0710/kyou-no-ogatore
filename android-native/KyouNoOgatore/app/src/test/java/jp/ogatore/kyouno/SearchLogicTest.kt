package jp.ogatore.kyouno

import jp.ogatore.kyouno.catalog.CatalogVideo
import org.junit.Assert.assertTrue
import org.junit.Test

// TASK-C2-2026-07-28-search-playlists-and-fullwidth-space.md §2: 全角スペース(U+3000)区切りの
// 複数語検索が必ず0件になっていたバグの回帰テスト。app-search.js:48 q.split(/\s+/)はJSの\sが
// U+3000を含むため通るが、Kotlinのsplit(Regex("\\s+"))はASCII空白のみで壊れていた。
class SearchLogicTest {
    private val sample = listOf(
        CatalogVideo(id = "v1", t = "肩こり朝ストレッチ", y = 2026, s = "PT00M30S", tags = listOf("肩こり", "朝")),
    )

    @Test
    fun fullWidthSpaceSeparatedQueryMatches() {
        val hits = searchCatalog(sample, "肩こり　朝", null, null)
        assertTrue(hits.isNotEmpty())
    }

    @Test
    fun halfWidthSpaceSeparatedQueryStillMatches() {
        val hits = searchCatalog(sample, "肩こり 朝", null, null)
        assertTrue(hits.isNotEmpty())
    }

    @Test
    fun mixedWhitespaceQueryStillMatches() {
        val hits = searchCatalog(sample, "肩こり\t朝\n", null, null)
        assertTrue(hits.isNotEmpty())
    }
}
