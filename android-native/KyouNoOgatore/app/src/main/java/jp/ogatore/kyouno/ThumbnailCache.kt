package jp.ogatore.kyouno

import java.io.File
import java.security.MessageDigest

// TASK: サムネイル画像のオフライン用ディスクキャッシュ(弱電波下で検索/再生リスト画面のサムネイルが
// 毎回ネット取得になり空グリッドに見える問題への対策)。KyonoAsyncImage(Composable)からロジックだけを
// 分離し、Android Context/BitmapFactoryに依存しないJVM単体テストで検証できるようにする。
// キャッシュ先はcontext.cacheDir配下(呼び出し側でthumbnails/を指定)であり、記録データ本体
// (kyono-store.json、context.filesDir)とは完全に分離する。キー=YouTube動画ID(サムネイルURLは
// videoId+size単位で不変なため、鮮度管理/期限切れは不要。書き込み済みなら常にキャッシュを優先する)。
// 上限は約50MBのハードキャップで、超過分は最終更新日時が古いファイルから順に削除する(LRU相当)。
class ThumbnailCache(
    private val cacheDir: File,
    private val maxBytes: Long = DEFAULT_MAX_BYTES,
) {
    init {
        if (!cacheDir.exists()) cacheDir.mkdirs()
    }

    fun read(key: String): ByteArray? {
        val file = File(cacheDir, fileName(key))
        if (!file.isFile) return null
        return runCatching { file.readBytes() }.getOrNull()
    }

    fun write(key: String, bytes: ByteArray) {
        val file = File(cacheDir, fileName(key))
        runCatching { file.writeBytes(bytes) }
        evictIfNeeded()
    }

    private fun evictIfNeeded() {
        val files = cacheDir.listFiles()?.filter { it.isFile } ?: return
        var total = files.sumOf { it.length() }
        if (total <= maxBytes) return
        // 最終更新日時が古い順(=最も昔にキャッシュされたもの)から削除していく。
        for (file in files.sortedBy { it.lastModified() }) {
            if (total <= maxBytes) break
            total -= file.length()
            file.delete()
        }
    }

    private fun fileName(key: String) = "$key.thumb"

    companion object {
        const val DEFAULT_MAX_BYTES: Long = 50L * 1024 * 1024

        private val VIDEO_ID_REGEX = Regex("""/vi/([^/]+)/""")

        // youtubeThumbUrl()が組み立てる"https://i.ytimg.com/vi/<videoId>/<size>.jpg"からvideoIdを
        // 抜き出す。PlaylistThumbのように任意の"https://..."サムネイルURLが渡ってくる呼び出し口も
        // あるため、パターンに一致しない場合はURL全体のSHA-256でキー化するフォールバックを用意する。
        fun keyFor(url: String): String {
            val match = VIDEO_ID_REGEX.find(url)
            if (match != null) return match.groupValues[1]
            val digest = MessageDigest.getInstance("SHA-256").digest(url.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
