package jp.ogatore.kyouno

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.ogatore.kyouno.obu.ObuLoader
import jp.ogatore.kyouno.obu.ObuPost
import jp.ogatore.kyouno.record.RecordStore

// ネイティブ移植 Step 7b(マスタープラン§6 Step 7b・§2-1「obu-feed.js OBU_FEED」行): オガトレ通信
// (オガトレ部)の全件アーカイブUI。index.html renderObuArchive()の1:1移植(新着順ソート+type別描画)。
// FABの新着ポップアップ(obuHasNew()・renderObuPopup())は本ステップでは簡略化し、
// アーカイブ一覧のみを実装する(投稿1件のみの現状ではポップアップの実質的な価値が薄いため)。
//
// ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
// Phase 3: index.html:266-278 .obu-post/.obu-post.obu-text(yellow-soft)/.obu-date/.obu-title/
// .obu-capの1:1移植。obuIsStaleDate/obuFmtDateの日付整形ロジックは新規追加であり「見た目のみ」の
// スコープを超えるため、このステップでは移植しない(既存の生日付表示を維持)。
@Composable
fun ObuScreen(store: RecordStore, onBack: () -> Unit) {
    val themeSetting = store.get("theme", "auto")
    KyonoTheme(themeSetting) {
        val colors = LocalKyonoColors.current
        val posts = remember {
            ObuLoader.shared.sortedWith(
                compareByDescending<ObuPost> { it.date }.thenByDescending { it.time ?: "" },
            )
        }
        Column(Modifier.fillMaxSize().background(colors.bg).padding(16.dp)) {
            KyonoLineButton("◀ もどる", onBack, Modifier.testTag("obuBackBtn"))
            Spacer(Modifier.height(12.dp))
            KyonoSectionHeader(KyonoIcon.ObuBubble, "オガトレ通信", fill = colors.pinkSoft)
            // 全画面完全性監査タスク(TASK-C2-2026-07-26-full-completeness-audit.md #obu):
            // index.html:932 説明文の1:1移植。
            Spacer(Modifier.height(6.dp))
            Text("尾形さんからの ひとこと・写真・ラジオを ぜんぶまとめて見られます🌱", color = colors.ink, fontSize = 14.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(12.dp))
            if (posts.isEmpty()) {
                KyonoCard {
                    Text(
                        "まだ投稿がありません また今度のぞいてみてね🌱",
                        color = colors.sub, fontSize = 14.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("obuArchiveList")) {
                    items(posts) { post -> ObuPostCard(post) }
                }
            }
        }
    }
}

@Composable
private fun ObuPostCard(post: ObuPost) {
    val colors = LocalKyonoColors.current
    val context = LocalContext.current
    // index.html:271 .obu-post.obu-text(yellow-softの角丸ボックス)。photo/radioはボックスなしで並べる。
    val isText = post.type != "photo" && post.type != "radio"
    Column(
        Modifier.fillMaxWidth().padding(bottom = 14.dp)
            .let { if (isText) it.background(colors.yellowSoft, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp) else it }
            .testTag("obuPost_${post.id}"),
    ) {
        Text(
            post.date + (post.time?.let { " $it" } ?: ""),
            color = if (isText) colors.sub2 else colors.sub, fontSize = 12.sp, fontWeight = FontWeight.Black,
        )
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
                            modifier = Modifier.fillMaxWidth().height(180.dp).padding(top = 6.dp)
                                .background(colors.card, RoundedCornerShape(14.dp))
                                .border(1.5.dp, colors.line, RoundedCornerShape(14.dp)),
                        )
                    }
                }
                post.text?.let { Text(it, color = colors.ink, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp)) }
            }
            "radio" -> {
                post.title?.let {
                    Text("📻 $it", color = colors.tealInk, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 6.dp))
                }
                Text("🎧 音声つき投稿(ネイティブでは再生UI未実装)", color = colors.sub, fontSize = 12.sp)
            }
            else -> post.text?.let { Text(it, color = colors.ink, fontSize = 15.sp, lineHeight = 24.sp) }
        }
    }
}
