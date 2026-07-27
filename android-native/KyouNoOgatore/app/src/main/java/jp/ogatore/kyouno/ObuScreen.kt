package jp.ogatore.kyouno

import android.media.MediaPlayer
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.time.Instant
import jp.ogatore.kyouno.obu.ObuLoader
import jp.ogatore.kyouno.obu.ObuPost
import jp.ogatore.kyouno.obu.obuFmtDate
import jp.ogatore.kyouno.obu.obuIsStaleDate
import jp.ogatore.kyouno.obu.obuLatestByType
import jp.ogatore.kyouno.record.RecordLogic
import jp.ogatore.kyouno.record.RecordStore
import kotlinx.coroutines.delay

// ネイティブ移植 Step 7b(マスタープラン§6 Step 7b・§2-1「obu-feed.js OBU_FEED」行): オガトレ通信
// (オガトレ部)の全件アーカイブUI。index.html renderObuArchive()の1:1移植(新着順ソート+type別描画)。
// FABタップ時のプレビューポップアップ(renderObuPopup/openObu)は当初簡略化していたが、
// TASK-C2-2026-07-27-obu-fab-preview-popup.mdでObuPreviewPopupとして追加移植した
// (下記ObuPreviewPopup参照)。
//
// ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
// Phase 3: index.html:266-278 .obu-post/.obu-post.obu-text(yellow-soft)/.obu-date/.obu-title/
// .obu-capの1:1移植。
// TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §5: obuFmtDate/obuIsStaleDateの日付整形・
// 30日超の控えめ表示を追加(以前は「見た目のみの範囲を超える」として生ISO表示のまま見送っていた)。
@Composable
fun ObuScreen(store: RecordStore, onBack: () -> Unit) {
    val themeSetting = store.get("theme", "auto")
    KyonoTheme(themeSetting, bigText = store.get("bigtext", true)) {
        val colors = LocalKyonoColors.current
        // TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §8: index.html:1352
        // renderObuArchive()の安定ソート(日付のみ・同日内は元の記載順を保持)の1:1移植。
        // 以前はtimeでの二次ソートがあり、time付きの投稿が同日内で先頭に来てしまっていた。
        val posts = remember { ObuLoader.shared.sortedByDescending { it.date } }
        val today = remember { RecordLogic.todayStr(Instant.now()) }
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
                    items(posts) { post -> ObuPostCard(post, today) }
                    // TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §8: 一覧末尾にも
                    // 「もどる」を追加(Web版・voicesアーカイブと同じく上下どちらからでも脱出できるように)。
                    item {
                        Spacer(Modifier.height(4.dp))
                        KyonoLineButton("◀ もどる", onBack, Modifier.testTag("obuBackBtnBottom"))
                    }
                }
            }
        }
    }
}

@Composable
fun ObuPostCard(post: ObuPost, today: String, photoWidthFraction: Float = 1f) {
    val colors = LocalKyonoColors.current
    val context = LocalContext.current
    // index.html:271 .obu-post.obu-text(yellow-softの角丸ボックス)。photo/radioはボックスなしで並べる。
    val isText = post.type != "photo" && post.type != "radio"
    // TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §5: index.html:277-278
    // .obu-date-old(30日超は11px・sub-faintに落とす)の1:1移植。
    val isOld = obuIsStaleDate(post.date, today)
    Column(
        Modifier.fillMaxWidth().padding(bottom = 14.dp)
            .let { if (isText) it.background(colors.yellowSoft, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp) else it }
            .testTag("obuPost_${post.id}"),
    ) {
        Text(
            obuFmtDate(post.date, post.time),
            color = if (isOld) colors.subFaint else if (isText) colors.sub2 else colors.sub,
            fontSize = if (isOld) 11.sp else 12.sp,
            fontWeight = if (isOld) FontWeight.SemiBold else FontWeight.Black,
        )
        when (post.type) {
            "photo" -> {
                post.image?.let { imagePath ->
                    val resName = ObuLoader.imageResourceName(imagePath)
                    val resId = resName?.let { context.resources.getIdentifier(it, "drawable", context.packageName) } ?: 0
                    if (resId != 0) {
                        // TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §7: index.html:266
                        // .obu-post img{width:100%}(アーカイブは自然なアスペクト比・切り抜きなし)/
                        // #obuModal .obu-post img{width:75%}(ポップアップは75%幅中央寄せ)の1:1移植。
                        // 以前はheight固定180dp+Cropで縦長写真の上下が欠けていた。
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = post.text,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth(photoWidthFraction).padding(top = 6.dp)
                                .align(Alignment.CenterHorizontally)
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
                Spacer(Modifier.height(6.dp))
                ObuRadioPlayer(post)
            }
            else -> post.text?.let { Text(it, color = colors.ink, fontSize = 15.sp, lineHeight = 24.sp) }
        }
    }
}

// TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §1: index.html:1334-1336
// <audio controls src=...>相当の1:1移植。MediaPlayerで再生/一時停止・再生位置と長さを表示する
// (最低限の要件。バックグラウンド再生・通知センター連携はスコープ外=作らない)。
@Composable
private fun ObuRadioPlayer(post: ObuPost, modifier: Modifier = Modifier) {
    val colors = LocalKyonoColors.current
    val context = LocalContext.current
    val resId = remember(post.audio) {
        post.audio?.let { path ->
            ObuLoader.audioResourceName(path)?.let { name -> context.resources.getIdentifier(name, "raw", context.packageName) }
        } ?: 0
    }
    if (resId == 0) {
        Text("🎧 音声を読み込めませんでした", color = colors.sub, fontSize = 12.sp, modifier = modifier)
        return
    }
    var mediaPlayer by remember(resId) { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember(resId) { mutableStateOf(false) }
    var position by remember(resId) { mutableStateOf(0) }
    var duration by remember(resId) { mutableStateOf(0) }

    DisposableEffect(resId) {
        val mp = MediaPlayer.create(context, resId)
        mediaPlayer = mp
        duration = mp?.duration ?: 0
        mp?.setOnCompletionListener {
            isPlaying = false
            position = 0
            it.seekTo(0)
        }
        onDispose {
            mp?.release()
            mediaPlayer = null
        }
    }

    LaunchedEffect(isPlaying, resId) {
        while (isPlaying) {
            position = mediaPlayer?.currentPosition ?: position
            delay(300)
        }
    }

    fun fmt(ms: Int): String {
        val totalSec = ms / 1000
        return "${totalSec / 60}:${(totalSec % 60).toString().padStart(2, '0')}"
    }

    Row(
        modifier.fillMaxWidth()
            .background(colors.card, RoundedCornerShape(12.dp))
            .border(1.5.dp, colors.line, RoundedCornerShape(12.dp))
            .padding(10.dp)
            .testTag("obuRadioPlayer_${post.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).background(colors.teal, CircleShape)
                .clickable {
                    val mp = mediaPlayer ?: return@clickable
                    if (isPlaying) mp.pause() else mp.start()
                    isPlaying = !isPlaying
                }
                .testTag("obuRadioPlayBtn_${post.id}"),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (isPlaying) "⏸" else "▶", color = Color.White, fontSize = 14.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Box(Modifier.fillMaxWidth().height(4.dp).background(colors.line, RoundedCornerShape(2.dp))) {
                val frac = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
                Box(Modifier.fillMaxWidth(frac).height(4.dp).background(colors.teal, RoundedCornerShape(2.dp)))
            }
            Spacer(Modifier.height(4.dp))
            Text("${fmt(position)} / ${fmt(duration)}", color = colors.sub, fontSize = 11.sp, modifier = Modifier.testTag("obuRadioTime_${post.id}"))
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
    val today = remember { RecordLogic.todayStr(Instant.now()) }
    // TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §6: index.html:265
    // #obuModal .obu-box{max-height:80vh;overflow-y:auto}の1:1移植。文字サイズ「大きめ」+
    // text/photo/radioの3件が揃うと「もっと見る」リンクと✕ボタンが画面外に出て操作不能になり得た欠落。
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.8f
    Dialog(onDismissRequest = onClose) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = maxHeight)
                .background(colors.card, RoundedCornerShape(20.dp)).testTag("obuModal"),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(18.dp, 18.dp, 18.dp, 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("オガトレ通信", color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Box(
                    Modifier.size(40.dp).background(colors.line, CircleShape).clickable(onClick = onClose).testTag("obuPopupCloseBtn"),
                    contentAlignment = Alignment.Center,
                ) { Text("✕", color = colors.ink, fontWeight = FontWeight.Black) }
            }
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(18.dp, 12.dp, 18.dp, 0.dp)) {
                if (items.isEmpty()) {
                    Text(
                        "まだ投稿がありません また今度のぞいてみてね🌱",
                        color = colors.sub, fontSize = 14.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp).testTag("obuPopupEmpty"),
                    )
                } else {
                    Column(Modifier.testTag("obuPopupBody")) {
                        items.forEach { post -> ObuPostCard(post, today, photoWidthFraction = 0.75f) }
                    }
                }
            }
            Text(
                "もっと見る（過去の投稿もぜんぶ）",
                color = colors.tealInk, fontSize = 14.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(18.dp, 4.dp, 18.dp, 18.dp).clickable(onClick = onViewArchive).testTag("obuMoreLink"),
            )
        }
    }
}
