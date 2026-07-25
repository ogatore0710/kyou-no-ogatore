package jp.ogatore.kyouno

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import jp.ogatore.kyouno.obu.ObuLoader
import jp.ogatore.kyouno.obu.ObuPost

// ネイティブ移植 Step 7b(マスタープラン§6 Step 7b・§2-1「obu-feed.js OBU_FEED」行): オガトレ通信
// (オガトレ部)の全件アーカイブUI。index.html renderObuArchive()の1:1移植(新着順ソート+type別描画)。
// FABの新着ポップアップ(obuHasNew()・renderObuPopup())は本ステップでは簡略化し、
// アーカイブ一覧のみを実装する(投稿1件のみの現状ではポップアップの実質的な価値が薄いため)。
@Composable
fun ObuScreen(onBack: () -> Unit) {
    val posts = remember {
        ObuLoader.shared.sortedWith(
            compareByDescending<ObuPost> { it.date }.thenByDescending { it.time ?: "" },
        )
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onBack, modifier = Modifier.testTag("obuBackBtn")) { Text("◀ もどる") }
        Spacer(Modifier.height(8.dp))
        Text("オガトレ通信", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("obuArchiveList")) {
            items(posts) { post -> ObuPostCard(post) }
        }
    }
}

@Composable
private fun ObuPostCard(post: ObuPost) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .background(Color(0xFFF3F1EC), RoundedCornerShape(12.dp)).padding(12.dp)
            .testTag("obuPost_${post.id}"),
    ) {
        Text(post.date + (post.time?.let { " $it" } ?: ""), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        when (post.type) {
            "photo" -> {
                post.image?.let { imagePath ->
                    val resName = ObuLoader.imageResourceName(imagePath)
                    val resId = resName?.let { context.resources.getIdentifier(it, "drawable", context.packageName) } ?: 0
                    if (resId != 0) {
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = post.text,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(180.dp).padding(top = 4.dp),
                        )
                    }
                }
                post.text?.let { Text(it, modifier = Modifier.padding(top = 4.dp)) }
            }
            "radio" -> {
                post.title?.let { Text(it, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp)) }
                Text("🎧 音声つき投稿(ネイティブでは再生UI未実装)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            else -> post.text?.let { Text(it, modifier = Modifier.padding(top = 4.dp)) }
        }
    }
}
