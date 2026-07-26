package jp.ogatore.kyouno

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

// 見た目パリティ第2弾(TASK-C2-2026-07-26-visual-parity-round2.md §2 動画サムネイル): 新規ライブラリ
// 依存(Coil等)を追加する実績がプロジェクト内に無いため、「標準のAsyncImage相当」として
// produceState+HttpURLConnectionによる素朴な非同期読み込みをここに実装する。読み込み失敗時は
// Web版onerror="this.style.visibility='hidden'"と同じく何も表示しない(崩れたアイコンを見せない)。
@Composable
fun KyonoAsyncImage(url: String, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = url) {
        value = runCatching {
            withContext(Dispatchers.IO) {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.doInput = true
                conn.connect()
                conn.inputStream.use { BitmapFactory.decodeStream(it) }
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
