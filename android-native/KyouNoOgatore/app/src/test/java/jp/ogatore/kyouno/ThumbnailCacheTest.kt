package jp.ogatore.kyouno

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

// サムネイルオフラインキャッシュ(ThumbnailCache)のJVM単体テスト。実機Contextを使わず、
// Files.createTempDirectory()で作った一時ディレクトリをcacheDir代わりに渡して検証する。
class ThumbnailCacheTest {
    private fun tempDir() = Files.createTempDirectory("thumb-cache-test").toFile().apply { deleteOnExit() }

    @Test
    fun writeThenReadRoundTrip() {
        val cache = ThumbnailCache(tempDir())
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        cache.write("videoAbc", bytes)

        val read = cache.read("videoAbc")

        assertArrayEquals(bytes, read)
    }

    @Test
    fun readMissReturnsNull() {
        val cache = ThumbnailCache(tempDir())

        assertNull(cache.read("neverWritten"))
    }

    @Test
    fun keyForExtractsVideoIdFromThumbnailUrl() {
        val key = ThumbnailCache.keyFor("https://i.ytimg.com/vi/dQw4w9WgXcQ/mqdefault.jpg")

        assertEquals("dQw4w9WgXcQ", key)
    }

    @Test
    fun keyForFallsBackToStableHashForNonMatchingUrl() {
        val url = "https://example.com/some-thumb.jpg"

        val key1 = ThumbnailCache.keyFor(url)
        val key2 = ThumbnailCache.keyFor(url)

        assertEquals(key1, key2)
        assertTrue(key1.isNotEmpty())
    }

    @Test
    fun evictionRemovesOldestEntriesFirstAndStaysUnderCap() {
        val dir = tempDir()
        // 1エントリ2000バイト、上限5000バイト。3つ書き込むと6000バイトになり超過するので、
        // 最も古い1件目が削除されて4000バイト(<=5000)に収まるはず。
        val cache = ThumbnailCache(dir, maxBytes = 5000)
        val payload = ByteArray(2000) { 0x7 }

        cache.write("oldest", payload)
        Thread.sleep(20)
        cache.write("middle", payload)
        Thread.sleep(20)
        cache.write("newest", payload)

        assertNull("oldest entry should have been evicted", cache.read("oldest"))
        assertArrayEquals(payload, cache.read("middle"))
        assertArrayEquals(payload, cache.read("newest"))

        val totalOnDisk = dir.listFiles()?.sumOf { it.length() } ?: 0L
        assertTrue("total on-disk size ($totalOnDisk) must stay under cap", totalOnDisk <= 5000)
    }

    @Test
    fun evictionKeepsWorkingAcrossManyWrites() {
        val dir = tempDir()
        val cache = ThumbnailCache(dir, maxBytes = 10_000)
        val payload = ByteArray(1000) { 0x1 }

        repeat(30) { i ->
            cache.write("video$i", payload)
            Thread.sleep(2)
        }

        val totalOnDisk = dir.listFiles()?.sumOf { it.length() } ?: 0L
        assertTrue("total on-disk size ($totalOnDisk) must stay under cap after many writes", totalOnDisk <= 10_000)
        // 最初の方に書いたキーは追い出されているはず。
        assertFalse(File(dir, "video0.thumb").exists())
    }
}
