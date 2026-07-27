package jp.ogatore.kyouno

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.window.Dialog
import jp.ogatore.kyouno.obu.ObuLoader
import jp.ogatore.kyouno.obu.ObuPost
import jp.ogatore.kyouno.obu.obuLatestByType
import jp.ogatore.kyouno.record.RecordStore

// ネイティブ移植 Step 7b(マスタープラン§6 Step 7b・§2-1「obu-feed.js OBU_FEED」行): オガトレ通信
// (オガトレ部)の全件アーカイブUI。index.html renderObuArchive()の1:1移植(新着順ソート+type別描画)。
// FABタップ時のプレビューポップアップ(renderObuPopup/openObu)は当初簡略化していたが、
// TASK-C2-2026-07-27-obu-fab-preview-popup.mdでObuPreviewPopupとして追加移植した
// (下記ObuPreviewPopup参照)。
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
fun ObuPostCard(post: ObuPost) {
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

// TASK-C2-2026-07-27-obu-fab-preview-popup.md: index.html:1344-1358 renderObuPopup/openObuの1:1移植。
// FABタップで直接全アーカイブへ遷移していたのをやめ、まずtext/photo/radio最新1件ずつ(最大3件)だけを
// 見せるプレビューにする。既読記録(obu_seen)・バッジ更新は呼び出し元(FABのonClick)がポップアップを
// 開く時点で行う(index.html:1345-1348と同じ「開いた瞬間に既読」のタイミング)。
@Composable
fun ObuPreviewPopup(onClose: () -> Unit, onViewArchive: () -> Unit) {
    val colors = LocalKyonoColors.current
    val items = remember {
        val posts = ObuLoader.shared
        listOf("text", "photo", "radio").mapNotNull { type -> obuLatestByType(posts, type) }
    }
    Dialog(onDismissRequest = onClose) {
        Column(
            Modifier.fillMaxWidth().background(colors.card, RoundedCornerShape(20.dp)).padding(18.dp).testTag("obuModal"),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("オガトレ通信", color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Box(
                    Modifier.size(40.dp).background(colors.line, CircleShape).clickable(onClick = onClose).testTag("obuPopupCloseBtn"),
                    contentAlignment = Alignment.Center,
                ) { Text("✕", color = colors.ink, fontWeight = FontWeight.Black) }
            }
            Spacer(Modifier.height(12.dp))
            if (items.isEmpty()) {
                Text(
                    "まだ投稿がありません また今度のぞいてみてね🌱",
                    color = colors.sub, fontSize = 14.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp).testTag("obuPopupEmpty"),
                )
            } else {
                Column(Modifier.testTag("obuPopupBody")) { items.forEach { post -> ObuPostCard(post) } }
            }
            Text(
                "もっと見る（過去の投稿もぜんぶ）",
                color = colors.tealInk, fontSize = 14.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).clickable(onClick = onViewArchive).testTag("obuMoreLink"),
            )
        }
    }
}
