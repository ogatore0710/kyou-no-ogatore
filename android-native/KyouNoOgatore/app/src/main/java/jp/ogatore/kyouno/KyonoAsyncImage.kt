package jp.ogatore.kyouno

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

// 見た目パリティ第2弾(TASK-C2-2026-07-26-visual-parity-round2.md §2 動画サムネイル): 新規ライブラリ
// 依存(Coil等)を追加する実績がプロジェクト内に無いため、「標準のAsyncImage相当」として
// produceState+HttpURLConnectionによる素朴な非同期読み込みをここに実装する。読み込み失敗時は
// Web版onerror="this.style.visibility='hidden'"と同じく何も表示しない(崩れたアイコンを見せない)。
//
// TASK: オフライン用ディスクキャッシュ(ThumbnailCache)を読み書き経路に挟む。弱電波下で検索/
// 再生リスト画面を開くたびに毎回ネット取得していた問題への対策(§前回コメント参照)。
// context.cacheDir/thumbnails/配下に保存し、context.filesDir(kyono-store.json=記録データ本体)とは
// 完全に分離する。書き込み済みならネットワーク状況に関わらず常にキャッシュを優先してよい
// (サムネイルURLはvideoId+size単位で不変のため鮮度管理/期限切れは不要)。
@Composable
fun KyonoAsyncImage(url: String, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    val context = LocalContext.current
    val cache = remember(context) { ThumbnailCache(File(context.cacheDir, "thumbnails")) }
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = url) {
        value = runCatching {
            withContext(Dispatchers.IO) {
                val key = ThumbnailCache.keyFor(url)
                val bytes = cache.read(key) ?: run {
                    val conn = URL(url).openConnection() as HttpURLConnection
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000
                    conn.doInput = true
                    conn.connect()
                    val fetched = conn.inputStream.use { it.readBytes() }
                    cache.write(key, fetched)
                    fetched
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }.getOrNull()
    }
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier,
        )
    }
}

// index.html videos.js/app-search.js相当: YouTube動画IDからサムネイルURLを組み立てる。
// 一覧(検索・再生リスト)はmqdefault、個別強調(ツアーミニチュア等)はhqdefaultをWeb版で使い分けている。
fun youtubeThumbUrl(videoId: String, size: String = "mqdefault"): String =
    "https://i.ytimg.com/vi/$videoId/$size.jpg"
