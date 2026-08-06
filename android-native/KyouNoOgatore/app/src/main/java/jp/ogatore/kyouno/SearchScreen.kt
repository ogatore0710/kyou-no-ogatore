@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package jp.ogatore.kyouno

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.alpha
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

// TASK-C2-2026-07-31-feedback-round2.md B-2 → bodypart-art-rollout: 「からだの場所」
// (key "b")タグチップの部位アイコン対応表。日本語タグ名→drawable-nodpi/chip_<key>.pngの
// キー対応。「腰」はB-2時点ではオンボ(OnboardingScreens.kt obChipIconRes)と同じ
// chip_youtsuu.pngを再利用していたが、bodypart-art-rollout(記録カード風・本人選定トーン)
// でタグ側だけ絵を差し替えるにあたり、オンボのchip_youtsuu.png(かたさチェック「腰」
// ワーリーチップ)には触れない方針のため、専用キー"koshi"を新設して分離した。
// TASK-C2-2026-07-31-build12-journey2-splash-emoji.md W2-10: 残り3カテゴリ(a=時間・シーン・
// c=目的・d=その他)にも新規生成15種を適用し、全4カテゴリのタグにアイコンが付くようにする。
private val tagChipKey: Map<String, Map<String, String>> = mapOf(
    "b" to mapOf(
        "全身" to "zenshin",
        "肩・肩甲骨" to "kata",
        "首・肩こり" to "kubi",
        "姿勢・背中" to "senaka",
        "股関節" to "kokansetsu",
        "開脚" to "kaikyaku",
        "もも裏" to "momoura",
        "太もも・お尻" to "futomomo",
        "腰" to "koshi",
        "ひざ・O脚" to "hiza",
        "足首・足うら" to "ashikubi",
    ),
    "a" to mapOf(
        "朝" to "toki_asa",
        "夜・寝る前" to "toki_yoru",
        "座ったまま" to "toki_suwaru",
        "10分以内" to "toki_10pun",
        "ショート" to "toki_short",
    ),
    "c" to mapOf(
        "むくみ" to "mokuteki_mukumi",
        "引き締め" to "mokuteki_hikishime",
        "筋膜・マッサージ" to "mokuteki_massage",
        "自律神経" to "mokuteki_jiritsu",
        "スポーツ・運動前後" to "mokuteki_sports",
        "生活・セルフケア" to "mokuteki_selfcare",
    ),
    "d" to mapOf(
        "解説" to "sonota_kaisetsu",
        "水族館ロケ" to "sonota_suizokukan",
        "古民家ロケ" to "sonota_kominka",
        "その他" to "sonota_sonota",
    ),
)

// index.html:441-449 .chip-a〜d(カテゴリ色)の1:1移植(ライト/ダーク)。
private data class ChipColors(val bg: Color, val border: Color, val text: Color, val onBg: Color, val onBorder: Color, val onText: Color)

// TASK-C2-2026-08-02-build16-polish-and-ia.md P-7: 選択中チップ(on)の白文字コントラスト。
// pink(#E56A9A)/purple(#8B7BD8)地に白文字は実測3.06:1/3.55:1でWCAG AA(4.5:1)未達だった。
// teal("b")が既にonBg/onBorderへ素のteal(#2BB3A3)でなくtealStrong(#1E7B70)という濃い変種を
// 使っている前例に倣い、pink/purpleも「この画面の未選択時textとして既に使っている濃い変種」
// (0xB0366E/0x6A58B5、どちらも実測4.5:1超)へ差し替える(新しい色トークンは増やさない)。
// onBgは白文字向けの固定色のためlight/dark共通(teal案と同じ設計)。
// TASK-C2-2026-08-04-build21-color-system-navy.md D2(選択中チップ): 黄地+白文字は実測1.38:1で
// 読めなくなる罠のため、onBg/onBorderを#6B5500へ(白文字と実測7.18:1)。b/c/d同様onはテーマ
// 非依存の単一値にする(新しい色トークンは増やさない・既存方針を踏襲)。
private fun chipColorsFor(key: String, dark: Boolean): ChipColors = when (key) {
    "a" -> if (dark) ChipColors(Color(0xFF37301C), Color(0xFF5C4F1E), Color(0xFFE8C74C), Color(0xFF6B5500), Color(0xFF6B5500), Color.White)
    else ChipColors(Color(0xFFFFF6D8), Color(0xFFF2DE8A), Color(0xFF8A6D00), Color(0xFF6B5500), Color(0xFF6B5500), Color.White)
    "b" -> if (dark) ChipColors(Color(0xFF1F3532), Color(0xFF2E5A52), Color(0xFF7BD0C4), Color(0xFF1E7B70), Color(0xFF1E7B70), Color.White)
    else ChipColors(Color(0xFFE7F8F1), Color(0xFFBFE8DC), Color(0xFF177065), Color(0xFF1E7B70), Color(0xFF1E7B70), Color.White)
    "c" -> if (dark) ChipColors(Color(0xFF3A2730), Color(0xFF5E3A4C), Color(0xFFF09BC0), Color(0xFFB0366E), Color(0xFFB0366E), Color.White)
    else ChipColors(Color(0xFFFFEDF3), Color(0xFFF5C6D8), Color(0xFFB0366E), Color(0xFFB0366E), Color(0xFFB0366E), Color.White)
    else -> if (dark) ChipColors(Color(0xFF2C2740), Color(0xFF4A4070), Color(0xFFB8A9F0), Color(0xFF6A58B5), Color(0xFF6A58B5), Color.White)
    else ChipColors(Color(0xFFF1EDFF), Color(0xFFD6CCF5), Color(0xFF6A58B5), Color(0xFF6A58B5), Color(0xFF6A58B5), Color.White)
}

// app-search.js:40-50 currentHits() の1:1移植。
// TASK-C2-2026-08-01-build15-subtraction9.md #2: yearパラメータは年セレクタUI削除に伴い
// 除去(ネイティブのみ。呼び出し元だったUIが無くなったため引数ごと不要になった)。
fun searchCatalog(catalog: List<CatalogVideo>, query: String, activeTag: String?): List<CatalogVideo> {
    val q = query.trim()
    return catalog.filter { v ->
        if (activeTag != null && activeTag !in v.tags) return@filter false
        if (q.isEmpty()) return@filter true
        val hay = (v.t + " " + v.tags.joinToString(" ") + " " + v.y + "年").lowercase()
        // app-search.js:48 q.split(/\s+/)の1:1移植。JSの\sはU+3000(全角スペース)を含むが、
        // KotlinのRegex("\\s+")はASCII空白のみ(UNICODE_CHARACTER_CLASS未指定)のため、日本語
        // キーボード既定の全角スペース区切り検索が必ず0件になっていた(TASK-C2-2026-07-28)。
        q.lowercase().split(Regex("[\\s　]+")).all { w -> w.isEmpty() || hay.contains(w) }
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
fun VideoRow(v: CatalogVideo, openUrl: (String) -> Unit, badge: String? = null, hero: Boolean = false, disabledLook: Boolean = false, useShortTitle: Boolean = false) {
    val colors = LocalKyonoColors.current
    // index.html:137 body.dark .badge{color:#F0A58E}の1:1移植。ダークモード再確認タスク
    // (TASK-C2-2026-07-27-darkmode-recheck-and-nudges.md)で発覚: ライト固定色(#B4462F)のままだと
    // ダークモードのcoralSoft背景に対してコントラストが低すぎて読みにくかった。
    val dark = colors.bg == KyonoDarkColors.bg
    val badgeTextColor = if (dark) Color(0xFFF0A58E) else Color(0xFFB4462F)
    // TASK-C2-2026-07-27-fd-guide-ui-branch.md: index.html:337 .fd-hero .video(pink枠+pink-soft地)の
    // 1:1移植。はじめの1本ガイド中の①だけを視覚的に主役化する強調枠。
    // TASK-C2-2026-08-03-build18-tutorial-quality.md B-7: fdGuide中はopenUrlをno-opにしている
    // (Q-4)が、見た目は普段どおりタップできそうに見えており「押せるように見える嘘」になって
    // いた。no-op裁定は維持したまま、減光+clickable自体をenabled=falseにして見た目でも
    // 押せないことを明示する。
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .alpha(if (disabledLook) 0.5f else 1f)
            .clickable(enabled = !disabledLook) { openUrl("https://www.youtube.com/watch?v=${v.id}") }
            // TASK build33 R-49展開: VideoRowにもB3押し出し影。
            .kyonoDropShadow(if (dark) Color(0xFF110F0C) else Color(0xFFE4D0BD), 3.dp, 16.dp)
            .background(if (hero) colors.pinkSoft else colors.card, RoundedCornerShape(16.dp))
            .border(if (hero) 2.5.dp else 1.5.dp, if (hero) colors.pink else colors.borderStrong, RoundedCornerShape(16.dp))
            .padding(10.dp)
            // TASK-C2-2026-07-27-text-size-accessibility.md 項目4: サムネイル(装飾)+バッジ+タイトル+
            // 補足を1回のTalkBackスワイプで読める1つの単位にまとめる。
            .semantics(mergeDescendants = true) {}
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
                // TASK build32 R-46(本人指示・2026-08-06): 「余裕があったら追加の一本」等の
                // 長いバッジがおおきめ設定で2行に折り返す。文言は変えず1行固定+自動縮小。
                KyonoAutoShrinkText(
                    label, color = badgeTextColor, baseFontSize = 12.sp, fontWeight = FontWeight.Black,
                    // R-52(本人指示・細かい直し③): ピルの上下余白+2dp。
                    modifier = Modifier.background(colors.coralSoft, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(if (useShortTitle) (v.st ?: v.t) else v.t, color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(v.s, color = colors.sub, fontSize = 14.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

// index.html #search / app-search.js の1:1移植。カテゴリタブ→タグチップ→自由入力の3段絞り込み。
@Composable
fun SearchScreen(store: RecordStore, openUrl: (String) -> Unit, onBack: () -> Unit) {
    // Fable監査GO-2(視点B): 下タブ5枚のうち動画を探す/再生リスト/マイ記録にBackHandlerが
    // 無く、システム「もどる」が即アプリ終了していた。onBack自体は呼び出し元で既に
    // screen=Home配線済みなので、他の副画面(DexScreen等)と同じ単純な形で拾うだけでよい。
    BackHandler(onBack = onBack)
    val themeSetting = store.get("theme", "light")
    KyonoTheme(themeSetting, bigText = store.get("bigtext", true)) {
        val colors = LocalKyonoColors.current
        val context = LocalContext.current
        val dark = colors.bg == KyonoDarkColors.bg
        val catalog = remember { CatalogLoader.shared }
        var activeCat by remember { mutableStateOf(TAG_CATS[0].key) }
        var activeTag by remember { mutableStateOf<String?>(null) }
        var query by remember { mutableStateOf("") }
        // 挙動パリティ監査タスク(TASK-C2-2026-07-27-behavior-parity-audit.md §B): app-search.js:55
        // onSearchInput()の180msデバウンス(IME変換中の毎打鍵フル再描画を間引く)の1:1移植。
        // TextField自体は即時反映するが、実際のフィルタ(hits)はdebouncedQueryを使う。
        var debouncedQuery by remember { mutableStateOf("") }
        LaunchedEffect(query) {
            delay(180)
            debouncedQuery = query
        }
        var searchLimit by remember { mutableStateOf(24) }
        // TASK-C2-2026-08-01-build15-subtraction9.md #2: 年セレクタ(旧#ySel)を削除(5視点監査
        // ①②③④が独立に指摘。「肩こりの動画がほしい」目的に対し「何年の動画か」で絞る発想は
        // 想定ユーザーに伝わらず、このアプリで唯一のプルダウンUIだった。本人ガイドライン>
        // Web1:1移植の既定によりネイティブのみ削除・Web正本は据え置き)。

        val hits = remember(debouncedQuery, activeTag) { searchCatalog(catalog, debouncedQuery, activeTag) }

        // TestFlight実機フィードバックC2(2026-07-29): 「動画を探す」でhits.sizeは正しく計算されて
        // いるのに(catalog.size=454を確認済み)、結果が1件も画面に出ない欠陥があった(logcatで
        // hits.size自体は正しい値になっているのに描画されないことを確認・検索ロジックは無罪)。
        // 原因: 従来は非スクロールのColumn(Modifier.fillMaxSize())の中にLazyColumn(weight(1f))を
        // 直接ネストしていた。「からだの場所」のように4行に折り返すタグ行が多い状態だと、
        // bigtext 1.18倍込みでヘッダー・検索欄・チップ行だけで画面の残り高さを使い切ってしまい、
        // weight(1f)のLazyColumnに割り当てられる高さが0になって結果が丸ごと描画されなかった
        // (それより上の「N本」表示すら画面外に押し出されていた)。
        // 直し方: ヘッダー・検索欄・チップ行・年セレクタもすべて1つのLazyColumnのitemとして
        // 積み、画面全体を1枚のスクロール領域にする(Web版index.html:945-963の#search、および
        // 既に正しく動いているMainActivity.kt HomeScreen・SearchView.swift修正後と同じ構造)。
        // 動画行(hits.take(searchLimit)、最大でも数十件)はitems()で引き続き遅延読み込みのまま。
        LazyColumn(
            Modifier.fillMaxSize().background(colors.bg).testTag("searchResults"),
            contentPadding = KyonoScreenPadding,
        ) {
        item {
        Column {
            // 見た目パリティ移植の仕上げ(TASK-C2-2026-07-26-native-visual-design-parity-cleanup.md):
            // タブバー導入後は「戻る」概念が無いWeb版に合わせ、タブ画面から「◀ もどる」ボタンを削除。
            // UI/UXパリティ監査GO-5(2026-07-28): index.html:91-94,945 Web版の#search節には
            // 独自タイトルが無く、セクション切り替えの外にある共通ヘッダー(.logo)だけで構成されている。
            // 「動画を探す」という単独タイトルはネイティブ独自の代替であり、共通ヘッダーへ置き換える。
            KyonoAppHeader()
            Spacer(Modifier.height(16.dp))
            // GO-G14(5視点ワンループ): ホームと同じenvBanner(オフライン案内)をこの画面にも出す。
            // 文言・スタイルはホーム版(MainActivity.kt)と完全に同じにする。
            if (rememberIsOffline()) {
                Text(
                    "いま電波がないみたい 動画を見るには電波が必要だよ（「きょうやった！」の記録はつけられるよ）",
                    color = colors.ink, fontSize = 15.sp, lineHeight = 25.sp,
                    modifier = Modifier.fillMaxWidth()
                        .background(colors.yellowSoft, RoundedCornerShape(14.dp))
                        .border(1.5.dp, colors.yellow, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("envBanner"),
                )
                Spacer(Modifier.height(10.dp))
            }
            // index.html:945-949 .searchbox。TASK-C2-2026-07-29-ux-audit-G.md G4(消去ボタン):
            // index.html:430「#qClear: 検索クリア✕: 文字があるときだけ表示。50-60代向けに指で
            // 押せる大きさ」の1:1移植。
            TextField(
                value = query,
                onValueChange = { query = it; searchLimit = 24 },
                modifier = Modifier.fillMaxWidth().testTag("searchInput"),
                placeholder = { Text("例: 肩こり／朝／むくみ", color = colors.sub) },
                // アイコン方針(TASK-C2-2026-07-30-icon-system.md)判断: 検索欄の虫めがねは
                // この年齢層にとっていちばん通じる目印なので、プレースホルダー内の絵文字を
                // 削除するだけでなく、タブバーと同じ.searchアイコンを実際に左側(leadingIcon)へ
                // 差し込む(alan5判断・2026-07-30)。右のG4消去ボタン(trailingIcon)とは反対側
                // なので場所は競合しない。
                leadingIcon = {
                    KyonoIconGlyph(KyonoIcon.Search, fill = Color.Transparent, accent = colors.sub, modifier = Modifier.size(18.dp))
                },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(colors.line, androidx.compose.foundation.shape.CircleShape)
                                .clickable { query = ""; searchLimit = 24 }
                                .semantics { contentDescription = "入力をけす" }
                                .testTag("searchClearBtn"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("✕", color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
                } else null,
                shape = RoundedCornerShape(16.dp),
                // TASK-C2-2026-08-04-build20-addendum.md A-1: 文字色未指定バグの棚卸し対象。
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.card, unfocusedContainerColor = colors.card,
                    focusedIndicatorColor = colors.borderStrong, unfocusedIndicatorColor = colors.borderStrong,
                    focusedTextColor = colors.ink, unfocusedTextColor = colors.ink, cursorColor = colors.ink,
                ),
            )
            Spacer(Modifier.height(10.dp))
            // index.html:436-437 .catbtn/.catbtn.on。TASK-C2-2026-07-27-chips-overflow-and-
            // bubble-pop.md §1: index.html:434 .catrow{overflow-x:auto}(検索画面のカテゴリ行は
            // 元から横スクロール仕様)+右端フェード+「›」ヒントの1:1移植。
            FadingChipRow(modifier = Modifier.fillMaxWidth(), testTag = "searchCatRow") {
                items(TAG_CATS) { cat ->
                    val on = cat.key == activeCat
                    // B1(2026-07-29): 選択中(on=true)は黄色背景になるため、colors.inkではなく
                    // colors.yellowInk(ライト値固定)を使う。
                    // TASK-C2-2026-08-04-build22-yellow-return.md Z-1(藍トークン全撤去): build21の
                    // 「黄は面として使わない」拡張判断を撤回し、黄地+yellowInk文字(build21以前の姿)
                    // へ戻す。
                    Text(
                        cat.name, color = if (on) colors.yellowInk else colors.sub, fontSize = 14.sp, fontWeight = FontWeight.Black,
                        lineHeight = 14.sp, style = KyonoTightLineTextStyle,
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
            // index.html:440-449,952 .chip/.chip-a〜d/.chip.on。TASK-C2-2026-07-27-chips-overflow-
            // and-bubble-pop.md §5: .chipsは既定でflex-wrap:wrap(横スクロールは相談室フッターのみの
            // 例外)のため、LazyRowからFlowRowへ変更(3つ目以降が見えなくなっていた欠落の修正)。
            val activeCatTags = TAG_CATS.first { it.key == activeCat }.tags
            val cc = chipColorsFor(activeCat, dark)
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("searchTagRow"),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                activeCatTags.forEach { tag ->
                    val on = tag == activeTag
                    // TASK-C2-2026-07-31-feedback-round2.md B-2→build12 W2-10: 4カテゴリ全部の
                    // タグに左アイコンを添える。OnboardingScreens.kt obChipIconResと同じ
                    // 「resources.getIdentifier実行時解決」方式(KyonoTypeArt.kt/PlaylistThumb等
                    // 本ファイル内でも既出のパターン)を使い、生成イラストがまだ届いていない
                    // (drawable-nodpi/chip_*.pngが無い)キーはresId==0で単に描画しないだけにして
                    // ビルドも実行時も落とさない。
                    val chipIconKey = tagChipKey[activeCat]?.get(tag)
                    val chipIconResId = if (chipIconKey != null) {
                        remember(chipIconKey) { context.resources.getIdentifier("chip_$chipIconKey", "drawable", context.packageName) }
                    } else 0
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(if (on) cc.onBg else cc.bg, RoundedCornerShape(50))
                            .border(2.dp, if (on) cc.onBorder else cc.border, RoundedCornerShape(50))
                            .clickable { activeTag = if (activeTag == tag) null else tag; searchLimit = 24 }
                            // alan5差し戻し(2026-07-29): 「からだの場所」1行目が全身｜肩・肩甲骨の
                            // 2つで折り返し、Web版(3つ)と揃わない実機所見。padding値自体はCSS
                            // (.chip{padding:10px 16px})と数値上一致していたが、カスタムフォントの
                            // 実測グリフ幅がブラウザと完全一致しないため、「3つ入るか」を合格条件と
                            // する指示に基づき左右パディングを詰める。14dpでは実測1px未満の差で
                            // ギリギリ入らなかったため、余裕を持って12dpにする。
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("searchTag_$tag"),
                    ) {
                        if (chipIconResId != 0) {
                            // TASK-C2-2026-07-31-bodypart-art-legibility.md: 老眼対応で22dp→28dpへ拡大。
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(chipIconResId),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            tag, color = if (on) cc.onText else cc.text, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            lineHeight = 14.sp, style = KyonoTightLineTextStyle,
                        )
                    }
                }
            }
            // index.html:952 #filterNowの1:1移植。TASK-C2-2026-07-29-ux-audit-G.md G4(ピル・最優先):
            // 監査の指摘「タグで絞り込み中だと分からないまま、なぜか11本しか出ない状態から抜けられ
            // なくなる」に対応。
            activeTag?.let { tag ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        "$tag ✕", color = cc.onText, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(cc.onBg, RoundedCornerShape(50))
                            .border(2.dp, cc.onBorder, RoundedCornerShape(50))
                            .clickable { activeTag = null; searchLimit = 24 }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("searchFilterPill"),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("タップで解除", color = colors.sub, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // UI/UXパリティ監査GO-10(2026-07-28): app-search.js:61 `${hits.length}本`の1:1移植。
                // 「件見つかりました」は動詞つきの事務的な文体で、Web版のカジュアルな単位表現とは別物だった。
                Text("${hits.size}本", color = colors.sub, fontSize = 12.sp, modifier = Modifier.testTag("searchHitCount"))
            }
            Spacer(Modifier.height(6.dp))
        }
        }
        // index.html:958 #vlistの0件時分岐(app-search.js:68)の1:1移植。TASK-C2-2026-07-29-
        // ux-audit-G.md G4: 0件のときにここが完全に空になり、下のReqBox(文言のみ)しか
        // 出ないため「検索結果欄そのものが沈黙する」ように見えていた。
        if (hits.isEmpty()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(colors.card, RoundedCornerShape(16.dp))
                        .padding(vertical = 20.dp)
                        .testTag("searchEmptyCard"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    KyonoCharaImage("chara_kaikyaku", Modifier.size(84.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("この条件のストレッチはまだないみたい…", color = colors.sub, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        // C2修正: 動画行だけを引き続きitems()で遅延読み込みする(外側LazyColumnの直下)。
        items(hits.take(searchLimit)) { v -> VideoRow(v, openUrl) }
        if (hits.size > searchLimit) {
            item {
                Spacer(Modifier.height(6.dp))
                // index.html:71 app-search.js drawResults()「さらに表示（あと${残り}本）」の1:1移植。
                KyonoGhostButton("さらに表示（あと${hits.size - searchLimit}本）", { searchLimit += 48 }, Modifier.testTag("searchMoreBtn"))
            }
        }
        // 動画を探す画面のリクエスト導線欠落修正タスク(TASK-C2-2026-07-26-search-request-box.md):
        // index.html:960-963 #reqBox(app-search.js drawResults()のreqMsg/reqBtn組み立て・
        // index.html copyMailAddr()の1:1移植)。検索ロジック自体は変更していない。
        item {
            Spacer(Modifier.height(10.dp))
            val kwText = listOfNotNull(query.trim().ifBlank { null }, activeTag).joinToString(" ")
            ReqBox(context = context, store = store, shown = hits.isNotEmpty(), kwText = kwText)
        }
        }
    }
}

// index.html:961 reqMsg/reqBtnの表示切り替え。offlineCat分岐(Web版はCATALOG未ロード時の対応)は
// ネイティブではcatalog.jsonを同梱リソースとして常に同期ロードするため該当せず、常時表示でよい。
@Composable
private fun ReqBox(context: Context, store: RecordStore, shown: Boolean, kwText: String) {
    val colors = LocalKyonoColors.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    KyonoGradientCard(KyonoGradient.Warm, Modifier.testTag("reqBox")) {
        Text(
            if (shown) "やりたいストレッチが見つからない？\nオガトレに直接リクエストを送れます"
            else "ごめんなさい まだなかったみたい\nリクエストを送ってもらえたら動画づくりの参考にします",
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
        // UX13案・案8(2026-07-30): ボタン用途の残存絵文字をCanvasアイコンへ(ClipboardPaste)。
        Row(
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .clickable {
                    clipboard.setText(AnnotatedString("kyou-no@ogatore.jp"))
                    copied = true
                    scope.launch { delay(kyonoTransientMessageMillis(store)); copied = false }
                }
                .testTag("copyMailAddrBtn"),
        ) {
            if (!copied) {
                KyonoIconGlyph(KyonoIcon.ClipboardPaste, fill = Color.Transparent, accent = colors.tealInk, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(
                if (copied) "コピーしました" else "アドレスをコピー",
                color = colors.tealInk, fontSize = 12.sp, fontWeight = FontWeight.Black,
            )
        }
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

// index.html:328-336相当のサムネイル枠。thumbが"assets/pl-*.jpg"のときはネイティブに同梱した
// ローカル画像(drawable-nodpi/pl_*.jpg)を、https://のときはKyonoAsyncImageで都度取得する
// (TASK-C2-2026-07-28: index.html:3498-3521 PLAYLISTSはグループによって同梱ローカル画像と
// 既存i.ytimg.comサムネURLが混在しているため、両対応にする)。
@Composable
private fun PlaylistThumb(thumb: String?, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    if (thumb == null) return
    if (thumb.startsWith("http")) {
        KyonoAsyncImage(thumb, modifier)
    } else {
        // "assets/pl-asa30.jpg" -> "pl_asa30"(drawableはハイフン不可のためアンダースコアへ変換済み)
        val name = thumb.substringAfterLast("/").substringBeforeLast(".").replace("-", "_")
        val resId = remember(name) { context.resources.getIdentifier(name, "drawable", context.packageName) }
        if (resId != 0) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(resId),
                contentDescription = null,
                modifier = modifier,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        }
    }
}

// index.html:3517 renderPlaylists()の1:1移植。プレイリスト1件=角丸サムネ+タイトル+説明、
// タップでYouTubeプレイリストを開く(タブでの動画個別再生と違い、連続再生キューがそのまま続く)。
@Composable
private fun PlaylistRow(item: jp.ogatore.kyouno.catalog.PlaylistItem, openUrl: (String) -> Unit) {
    val colors = LocalKyonoColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable { openUrl("https://www.youtube.com/playlist?list=${item.id}") }
            .background(colors.card, RoundedCornerShape(16.dp))
            .border(1.5.dp, colors.borderStrong, RoundedCornerShape(16.dp))
            .padding(10.dp)
            .semantics(mergeDescendants = true) {}
            .testTag("playlist_${item.id}"),
    ) {
        Box(
            Modifier.width(112.dp).aspectRatio(16f / 9f)
                .background(colors.line, RoundedCornerShape(12.dp)),
        ) {
            PlaylistThumb(item.thumb, Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(item.desc, color = colors.sub, fontSize = 14.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

// 「再生リスト」タブ本体。TASK-C2-2026-07-28-search-playlists-and-fullwidth-space.md §1の判断:
// index.html:3498-3521 PLAYLISTS(手動キュレーション16本・3グループ)を主役として上に置く。
// TASK-C2-2026-08-06-build30-round8.md R-25(本人指示): 以前は下に「全部の動画を見たい」需要向けの
// 平坦一覧(catalog.json 454本)を従えていたが、これを廃止しプレイリストのみの画面にする
// (「動画を探す」タブが検索・全動画一覧の役目を引き続き担う)。
// LazyColumnがそのまま仮想化するため検索画面のようなsearchLimit方式のページングは不要。
@Composable
fun CatalogListScreen(store: RecordStore, openUrl: (String) -> Unit, onBack: () -> Unit) {
    // Fable監査GO-2(視点B): SearchScreenと同じ理由でBackHandler不在だった。
    BackHandler(onBack = onBack)
    val themeSetting = store.get("theme", "light")
    KyonoTheme(themeSetting, bigText = store.get("bigtext", true)) {
        val colors = LocalKyonoColors.current
        val playlistGroups = remember { jp.ogatore.kyouno.catalog.PlaylistLoader.shared }
        Column(Modifier.fillMaxSize().background(colors.bg).padding(16.dp)) {
            Text("再生リスト", color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            // GO-G14(5視点ワンループ): ホームと同じenvBanner(オフライン案内)をこの画面にも出す。
            // 文言・スタイルはホーム版(MainActivity.kt)と完全に同じにする。
            if (rememberIsOffline()) {
                Text(
                    "いま電波がないみたい 動画を見るには電波が必要だよ（「きょうやった！」の記録はつけられるよ）",
                    color = colors.ink, fontSize = 15.sp, lineHeight = 25.sp,
                    modifier = Modifier.fillMaxWidth()
                        .background(colors.yellowSoft, RoundedCornerShape(14.dp))
                        .border(1.5.dp, colors.yellow, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("envBanner"),
                )
                Spacer(Modifier.height(10.dp))
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("catalogList")) {
                playlistGroups.forEach { group ->
                    item {
                        Text(
                            group.group, color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                        )
                    }
                    items(group.items) { p -> PlaylistRow(p, openUrl) }
                }
                // index.html:941 .hint(リストの一番下に流れる注記のため、固定表示ではなくリスト末尾項目にする。
                // 固定表示にするとFAB2段(右下)と重なるバグの再発になる=とどくメーターの5番目ボタンで
                // 既発見済みの教訓と同種)
                item {
                    Text(
                        "タップするとYouTubeで開きます！テレビで流すのもおすすめ",
                        color = colors.sub, fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 90.dp),
                    )
                }
            }
        }
    }
}
