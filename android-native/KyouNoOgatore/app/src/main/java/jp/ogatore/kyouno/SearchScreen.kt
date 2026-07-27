package jp.ogatore.kyouno

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import jp.ogatore.kyouno.catalog.CatalogLoader
import jp.ogatore.kyouno.catalog.CatalogVideo
import jp.ogatore.kyouno.record.RecordStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ネイティブ移植 Step 7a(マスタープラン§6 Step 7a・§2-1「app-search.js TAG_CATS」行): 検索(TAG_CATS)・
// 再生リストUI(SearchView.swift/SearchScreen.ktの1:1対応)。判定ロジックは存在しない画面(単純な
// 文字列フィルタ)なので、Web版app-search.js currentHits()の1:1移植をこのファイルに直接持つ。
//
// スコープ解釈の注記(タスクの「再生リスト（catalog.json）」表記について): Web版の「再生リスト」タブは
// 実際にはcatalog.jsonでなくindex.html内の別配列PLAYLISTS(手動キュレーションのYouTubeプレイリストID
// 約20件・機械抽出スクリプト未整備)が情報源で、catalog.json(454件)を情報源とするのは「検索」タブの方
// (index.html/app-search.js確認済み)。タスク文面がcatalog.jsonを再生リストの情報源として明記している
// ため、本実装では「再生リスト」を「catalog.jsonの動画をカテゴリ絞り込みなしで一覧できる画面」として
// 実装した(検索画面の絞り込みUIを持たない単純版)。PLAYLISTS配列の移植(要:抽出スクリプト新設)が
// 別途必要な場合はalan5への報告で選択を仰ぐ(手写し禁止=§1-2のためこの場では実施しない)。
//
// ネイティブ移植「見た目のWeb版パリティ移植」タスク(TASK-C2-2026-07-26-native-visual-design-parity.md)
// Phase 3: index.html:433-449 .searchbox/.catbtn/.catbtn.on/.chip-a〜d(カテゴリごとの配色)/.chip.onの1:1移植。
// 仕上げ(...-cleanup.md): タブバー画面には「戻る」概念が無いWeb版に合わせ「◀ もどる」ボタンは削除済み。

data class TagCatDef(val key: String, val name: String, val tags: List<String>)

// app-search.js:6-11 TAG_CATS の1:1移植。
val TAG_CATS = listOf(
    TagCatDef("b", "からだの場所", listOf("全身", "肩・肩甲骨", "首・肩こり", "姿勢・背中", "股関節", "開脚", "もも裏", "太もも・お尻", "腰", "ひざ・O脚", "足首・足うら")),
    TagCatDef("a", "時間・シーン", listOf("朝", "夜・寝る前", "座ったまま", "10分以内", "ショート")),
    TagCatDef("c", "目的", listOf("むくみ", "引き締め", "筋膜・マッサージ", "自律神経", "スポーツ・運動前後", "生活・セルフケア")),
    TagCatDef("d", "その他", listOf("解説", "水族館ロケ", "古民家ロケ", "その他")),
)

// index.html:441-449 .chip-a〜d(カテゴリ色)の1:1移植(ライト/ダーク)。
private data class ChipColors(val bg: Color, val border: Color, val text: Color, val onBg: Color, val onBorder: Color, val onText: Color)

private fun chipColorsFor(key: String, dark: Boolean): ChipColors = when (key) {
    "a" -> if (dark) ChipColors(Color(0xFF37301C), Color(0xFF5C4F1E), Color(0xFFE8C74C), Color(0xFFFFD93B), Color(0xFFFFD93B), Color(0xFF211E19))
    else ChipColors(Color(0xFFFFF6D8), Color(0xFFF2DE8A), Color(0xFF8A6D00), Color(0xFFFFD93B), Color(0xFFFFD93B), Color(0xFF3A3A35))
    "b" -> if (dark) ChipColors(Color(0xFF1F3532), Color(0xFF2E5A52), Color(0xFF7BD0C4), Color(0xFF1E7B70), Color(0xFF1E7B70), Color.White)
    else ChipColors(Color(0xFFE7F8F1), Color(0xFFBFE8DC), Color(0xFF177065), Color(0xFF1E7B70), Color(0xFF1E7B70), Color.White)
    "c" -> if (dark) ChipColors(Color(0xFF3A2730), Color(0xFF5E3A4C), Color(0xFFF09BC0), Color(0xFFE56A9A), Color(0xFFE56A9A), Color.White)
    else ChipColors(Color(0xFFFFEDF3), Color(0xFFF5C6D8), Color(0xFFB0366E), Color(0xFFE56A9A), Color(0xFFE56A9A), Color.White)
    else -> if (dark) ChipColors(Color(0xFF2C2740), Color(0xFF4A4070), Color(0xFFB8A9F0), Color(0xFF8B7BD8), Color(0xFF8B7BD8), Color.White)
    else ChipColors(Color(0xFFF1EDFF), Color(0xFFD6CCF5), Color(0xFF6A58B5), Color(0xFF8B7BD8), Color(0xFF8B7BD8), Color.White)
}

// app-search.js:40-50 currentHits() の1:1移植。
fun searchCatalog(catalog: List<CatalogVideo>, query: String, activeTag: String?, year: Int?): List<CatalogVideo> {
    val q = query.trim()
    return catalog.filter { v ->
        if (activeTag != null && activeTag !in v.tags) return@filter false
        if (year != null && v.y != year) return@filter false
        if (q.isEmpty()) return@filter true
        val hay = (v.t + " " + v.tags.joinToString(" ") + " " + v.y + "年").lowercase()
        q.lowercase().split(Regex("\\s+")).all { w -> w.isEmpty() || hay.contains(w) }
    }
}

// 見た目パリティ第2弾(TASK-C2-2026-07-26-visual-parity-round2.md §2 動画サムネイル):
// index.html:328-336 .video(サムネイル112x16:9+タイトル3行clamp+badge)の1:1移植。
// KyonoAsyncImage(§1で新設)を再利用し、読み込み失敗時は何も表示しない(Web版onerrorと同じ)。
// index.html:1680-1685 vHTML/videoCard(badge引数)相当。ResultScreen(#result rxList)からも
// 使うためnon-privateにする(全画面完全性監査タスク #result のfollow-up=
// TASK-C2-2026-07-26-result-video-recommendations.md)。badge指定時はvideoCard()と同じく
// タグpillの代わりにbadge文言(「①まずほぐす」等)を表示する。
@Composable
fun VideoRow(v: CatalogVideo, openUrl: (String) -> Unit, badge: String? = null, hero: Boolean = false) {
    val colors = LocalKyonoColors.current
    // index.html:137 body.dark .badge{color:#F0A58E}の1:1移植。ダークモード再確認タスク
    // (TASK-C2-2026-07-27-darkmode-recheck-and-nudges.md)で発覚: ライト固定色(#B4462F)のままだと
    // ダークモードのcoralSoft背景に対してコントラストが低すぎて読みにくかった。
    val dark = colors.bg == KyonoDarkColors.bg
    val badgeTextColor = if (dark) Color(0xFFF0A58E) else Color(0xFFB4462F)
    // TASK-C2-2026-07-27-fd-guide-ui-branch.md: index.html:337 .fd-hero .video(pink枠+pink-soft地)の
    // 1:1移植。はじめの1本ガイド中の①だけを視覚的に主役化する強調枠。
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable { openUrl("https://www.youtube.com/watch?v=${v.id}") }
            .background(if (hero) colors.pinkSoft else colors.card, RoundedCornerShape(16.dp))
            .border(if (hero) 2.5.dp else 1.5.dp, if (hero) colors.pink else colors.line, RoundedCornerShape(16.dp))
            .padding(10.dp)
            .testTag("video_${v.id}"),
    ) {
        androidx.compose.foundation.layout.Box(
            Modifier.width(112.dp).aspectRatio(16f / 9f)
                .background(colors.line, RoundedCornerShape(12.dp)),
        ) {
            KyonoAsyncImage(
                youtubeThumbUrl(v.id),
                Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            (badge ?: v.tags.firstOrNull())?.let { label ->
                Text(
                    label, color = badgeTextColor, fontSize = 12.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.background(colors.coralSoft, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 1.dp),
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(v.t, color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(v.s, color = colors.sub, fontSize = 14.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

// index.html #search / app-search.js の1:1移植。カテゴリタブ→タグチップ→自由入力の3段絞り込み。
@Composable
fun SearchScreen(store: RecordStore, openUrl: (String) -> Unit, onBack: () -> Unit) {
    val themeSetting = store.get("theme", "auto")
    KyonoTheme(themeSetting, bigText = store.get("bigtext", true)) {
        val colors = LocalKyonoColors.current
        val context = LocalContext.current
        val dark = colors.bg == KyonoDarkColors.bg
        val catalog = remember { CatalogLoader.shared }
        var activeCat by remember { mutableStateOf(TAG_CATS[0].key) }
        var activeTag by remember { mutableStateOf<String?>(null) }
        var query by remember { mutableStateOf("") }
        var searchLimit by remember { mutableStateOf(24) }
        // 全画面完全性監査タスク(TASK-C2-2026-07-26-full-completeness-audit.md #search):
        // index.html:955 #ySel(年フィルタ)の1:1移植。searchCatalogのyearパラメータ自体は既存で
        // あったが、選択UIが無く常にnullで呼ばれていた(=年フィルタが機能していなかった)欠落。
        val years = remember { catalog.map { it.y }.toSortedSet(compareByDescending { it }).toList() }
        var selectedYear by remember { mutableStateOf<Int?>(null) }
        var yearMenuOpen by remember { mutableStateOf(false) }

        val hits = remember(query, activeTag, selectedYear) { searchCatalog(catalog, query, activeTag, selectedYear) }

        Column(Modifier.fillMaxSize().background(colors.bg).padding(16.dp)) {
            // 見た目パリティ移植の仕上げ(TASK-C2-2026-07-26-native-visual-design-parity-cleanup.md):
            // タブバー導入後は「戻る」概念が無いWeb版に合わせ、タブ画面から「◀ もどる」ボタンを削除。
            Text("動画を探す", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            // index.html:945-949 .searchbox
            TextField(
                value = query,
                onValueChange = { query = it; searchLimit = 24 },
                modifier = Modifier.fillMaxWidth().testTag("searchInput"),
                placeholder = { Text("🔍 例: 肩こり／朝／むくみ", color = colors.subFaint) },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.card, unfocusedContainerColor = colors.card,
                    focusedIndicatorColor = colors.line, unfocusedIndicatorColor = colors.line,
                ),
            )
            Spacer(Modifier.height(10.dp))
            // index.html:436-437 .catbtn/.catbtn.on
            LazyRow(modifier = Modifier.fillMaxWidth().testTag("searchCatRow")) {
                items(TAG_CATS) { cat ->
                    val on = cat.key == activeCat
                    Text(
                        cat.name, color = if (on) colors.ink else colors.sub, fontSize = 14.sp, fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .background(if (on) colors.yellow else colors.line, RoundedCornerShape(12.dp))
                            .clickable { activeCat = cat.key; activeTag = null }
                            .padding(horizontal = 13.dp, vertical = 10.dp)
                            .testTag("searchCat_${cat.key}"),
                    )
                    Spacer(Modifier.width(6.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            // index.html:440-449 .chip/.chip-a〜d/.chip.on
            val activeCatTags = TAG_CATS.first { it.key == activeCat }.tags
            val cc = chipColorsFor(activeCat, dark)
            LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("searchTagRow")) {
                items(activeCatTags) { tag ->
                    val on = tag == activeTag
                    Text(
                        tag, color = if (on) cc.onText else cc.text, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(if (on) cc.onBg else cc.bg, RoundedCornerShape(50))
                            .border(2.dp, if (on) cc.onBorder else cc.border, RoundedCornerShape(50))
                            .clickable { activeTag = if (activeTag == tag) null else tag; searchLimit = 24 }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("searchTag_$tag"),
                    )
                    Spacer(Modifier.width(6.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    Text(
                        (selectedYear?.let { "${it}年" } ?: "すべての年") + " ▾",
                        color = colors.sub, fontSize = 14.sp, fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .background(colors.card, RoundedCornerShape(12.dp))
                            .border(2.dp, colors.line, RoundedCornerShape(12.dp))
                            .clickable { yearMenuOpen = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("searchYearSelect"),
                    )
                    // ダークモード再確認タスク(TASK-C2-2026-07-27-darkmode-recheck-and-nudges.md)で発覚:
                    // 素のDropdownMenu/DropdownMenuItemはMaterialTheme既定の配色(ライト固定)で描画され、
                    // アプリのダークモードと無関係にライト色のポップアップが出ていた。他の箇所(設定画面の
                    // やるタイミング変更ピッカー等)と同じくPopup+自前スタイルのColumnに置き換える。
                    if (yearMenuOpen) {
                        Popup(alignment = Alignment.TopStart, offset = androidx.compose.ui.unit.IntOffset(0, 130), onDismissRequest = { yearMenuOpen = false }) {
                            Column(
                                Modifier
                                    .background(colors.card, RoundedCornerShape(12.dp))
                                    .border(2.dp, colors.line, RoundedCornerShape(12.dp))
                                    .padding(vertical = 4.dp)
                                    .heightIn(max = 320.dp)
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                Text(
                                    "すべての年", color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth().clickable { selectedYear = null; yearMenuOpen = false; searchLimit = 24 }.padding(horizontal = 16.dp, vertical = 12.dp),
                                )
                                years.forEach { y ->
                                    Text(
                                        "${y}年", color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.fillMaxWidth().clickable { selectedYear = y; yearMenuOpen = false; searchLimit = 24 }.padding(horizontal = 16.dp, vertical = 12.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                Text("${hits.size}件見つかりました", color = colors.sub, fontSize = 12.sp, modifier = Modifier.testTag("searchHitCount"))
            }
            Spacer(Modifier.height(6.dp))
            LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("searchResults")) {
                items(hits.take(searchLimit)) { v -> VideoRow(v, openUrl) }
                if (hits.size > searchLimit) {
                    item {
                        Spacer(Modifier.height(6.dp))
                        KyonoGhostButton("もっと見る", { searchLimit += 48 }, Modifier.testTag("searchMoreBtn"))
                    }
                }
                // 動画を探す画面のリクエスト導線欠落修正タスク(TASK-C2-2026-07-26-search-request-box.md):
                // index.html:960-963 #reqBox(app-search.js drawResults()のreqMsg/reqBtn組み立て・
                // index.html copyMailAddr()の1:1移植)。検索ロジック自体は変更していない。
                item {
                    Spacer(Modifier.height(10.dp))
                    val kwText = listOfNotNull(query.trim().ifBlank { null }, activeTag).joinToString(" ")
                    ReqBox(context = context, shown = hits.isNotEmpty(), kwText = kwText)
                }
            }
        }
    }
}

// index.html:961 reqMsg/reqBtnの表示切り替え。offlineCat分岐(Web版はCATALOG未ロード時の対応)は
// ネイティブではcatalog.jsonを同梱リソースとして常に同期ロードするため該当せず、常時表示でよい。
@Composable
private fun ReqBox(context: Context, shown: Boolean, kwText: String) {
    val colors = LocalKyonoColors.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    KyonoGradientCard(KyonoGradient.Warm, Modifier.testTag("reqBox")) {
        Text(
            if (shown) "やりたいストレッチが見つからない？\nオガトレに直接リクエストを送れます📮"
            else "ごめんなさい まだなかったみたい💦\nリクエストを送ってもらえたら動画づくりの参考にします📮",
            color = colors.ink, fontSize = 15.sp, modifier = Modifier.testTag("reqMsg"),
        )
        Spacer(Modifier.height(12.dp))
        KyonoGhostButton(
            if (kwText.isNotBlank()) "「$kwText」をリクエストする" else "リクエストを送る",
            {
                val subject = "ストレッチのリクエスト（きょうのオガトレ）"
                val body = "こんなストレッチの動画が欲しいです：\n${kwText.ifBlank { "（ここに書いてね）" }}\n\n--\nきょうのオガトレ「動画を探す」から送信"
                openMailIntent(context, "kyou-no@ogatore.jp", subject, body)
            },
            Modifier.testTag("reqBtn"),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "メールがひらかない方は kyou-no@ogatore.jp へ直接どうぞ",
            color = colors.sub, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (copied) "コピーしました✅" else "📋 アドレスをコピー",
            color = colors.tealInk, fontSize = 12.sp, fontWeight = FontWeight.Black,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
                .clickable {
                    clipboard.setText(AnnotatedString("kyou-no@ogatore.jp"))
                    copied = true
                    scope.launch { delay(2000); copied = false }
                }
                .testTag("copyMailAddrBtn"),
        )
    }
}

// index.html:2001系のカレンダーIntentと同じ設計判断(§2-1準拠): ACTION_SENDTOでメールAppにだけ
// 解決させる(mailto: URI+ACTION_VIEWだと非メールAppにも解決されうるため)。
// TASK-C2-2026-07-27-soudan-safety-copy-and-links: 相談室のフォールバック逃げ道リンクからも
// 同じmailto導線を再利用するためprivateを外す(package-private)。
fun openMailIntent(context: Context, to: String, subject: String, body: String): Boolean {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    return try {
        context.startActivity(intent)
        true
    } catch (e: android.content.ActivityNotFoundException) {
        false
    }
}

// 「再生リスト」= catalog.jsonの動画一覧をカテゴリ絞り込みなしで年降順にブラウズできる画面
// (スコープ解釈はファイル冒頭コメント参照)。LazyColumnがそのまま454件を仮想化するため
// 検索画面のようなsearchLimit方式のページングは不要。
@Composable
fun CatalogListScreen(store: RecordStore, openUrl: (String) -> Unit, onBack: () -> Unit) {
    val themeSetting = store.get("theme", "auto")
    KyonoTheme(themeSetting, bigText = store.get("bigtext", true)) {
        val colors = LocalKyonoColors.current
        val catalog = remember { CatalogLoader.shared.sortedWith(compareByDescending<CatalogVideo> { it.y }.thenBy { it.t }) }
        Column(Modifier.fillMaxSize().background(colors.bg).padding(16.dp)) {
            Text("再生リスト", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text("${catalog.size}本の動画", color = colors.sub, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("catalogList")) {
                items(catalog) { v -> VideoRow(v, openUrl) }
                // index.html:941 .hint(リストの一番下に流れる注記のため、固定表示ではなくリスト末尾項目にする。
                // 固定表示にするとFAB2段(右下)と重なるバグの再発になる=とどくメーターの5番目ボタンで
                // 既発見済みの教訓と同種)
                item {
                    Text(
                        "タップするとYouTubeで開きます！テレビで流すのもおすすめ📺",
                        color = colors.sub, fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 90.dp),
                    )
                }
            }
        }
    }
}
