package jp.ogatore.kyouno

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import jp.ogatore.kyouno.catalog.CatalogLoader
import jp.ogatore.kyouno.catalog.CatalogVideo

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

data class TagCatDef(val key: String, val name: String, val tags: List<String>)

// app-search.js:6-11 TAG_CATS の1:1移植。
val TAG_CATS = listOf(
    TagCatDef("b", "からだの場所", listOf("全身", "肩・肩甲骨", "首・肩こり", "姿勢・背中", "股関節", "開脚", "もも裏", "太もも・お尻", "腰", "ひざ・O脚", "足首・足うら")),
    TagCatDef("a", "時間・シーン", listOf("朝", "夜・寝る前", "座ったまま", "10分以内", "ショート")),
    TagCatDef("c", "目的", listOf("むくみ", "引き締め", "筋膜・マッサージ", "自律神経", "スポーツ・運動前後", "生活・セルフケア")),
    TagCatDef("d", "その他", listOf("解説", "水族館ロケ", "古民家ロケ", "その他")),
)

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

@Composable
private fun VideoRow(v: CatalogVideo, openUrl: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable { openUrl("https://www.youtube.com/watch?v=${v.id}") }
            .background(Color(0xFFF3F1EC), RoundedCornerShape(10.dp))
            .padding(10.dp)
            .testTag("video_${v.id}"),
    ) {
        v.tags.firstOrNull()?.let { tag ->
            Text(tag, style = MaterialTheme.typography.labelSmall, color = Color(0xFF6B4EA6))
        }
        Text(v.t, style = MaterialTheme.typography.bodyMedium)
        Text(v.s, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

// index.html #search / app-search.js の1:1移植。カテゴリタブ→タグチップ→自由入力の3段絞り込み。
@Composable
fun SearchScreen(openUrl: (String) -> Unit, onBack: () -> Unit) {
    val catalog = remember { CatalogLoader.shared }
    var activeCat by remember { mutableStateOf(TAG_CATS[0].key) }
    var activeTag by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var searchLimit by remember { mutableStateOf(24) }

    val hits = remember(query, activeTag) { searchCatalog(catalog, query, activeTag, null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row {
            Text("動画を探す", style = MaterialTheme.typography.headlineSmall)
        }
        Button(onClick = onBack, modifier = Modifier.testTag("searchBackBtn")) { Text("◀ もどる") }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; searchLimit = 24 },
            modifier = Modifier.fillMaxWidth().testTag("searchInput"),
            placeholder = { Text("肩こり、腰痛など") },
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(modifier = Modifier.fillMaxWidth().testTag("searchCatRow")) {
            items(TAG_CATS) { cat ->
                Button(
                    onClick = { activeCat = cat.key; activeTag = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (cat.key == activeCat) Color(0xFF6B4EA6) else Color(0xFFE8E3F5),
                        contentColor = if (cat.key == activeCat) Color.White else Color.Black,
                    ),
                    modifier = Modifier.padding(end = 4.dp).testTag("searchCat_${cat.key}"),
                ) { Text(cat.name) }
            }
        }
        Spacer(Modifier.height(4.dp))
        val activeCatTags = TAG_CATS.first { it.key == activeCat }.tags
        LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("searchTagRow")) {
            items(activeCatTags) { tag ->
                Button(
                    onClick = { activeTag = if (activeTag == tag) null else tag; searchLimit = 24 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (tag == activeTag) Color(0xFF6B4EA6) else Color(0xFFF3F1EC),
                        contentColor = if (tag == activeTag) Color.White else Color.Black,
                    ),
                    modifier = Modifier.padding(end = 4.dp).testTag("searchTag_$tag"),
                ) { Text(tag) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("${hits.size}件見つかりました", style = MaterialTheme.typography.labelSmall, modifier = Modifier.testTag("searchHitCount"))
        Spacer(Modifier.height(4.dp))
        LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("searchResults")) {
            items(hits.take(searchLimit)) { v -> VideoRow(v, openUrl) }
            if (hits.size > searchLimit) {
                item {
                    Button(onClick = { searchLimit += 48 }, modifier = Modifier.fillMaxWidth().testTag("searchMoreBtn")) {
                        Text("もっと見る")
                    }
                }
            }
        }
    }
}

// 「再生リスト」= catalog.jsonの動画一覧をカテゴリ絞り込みなしで年降順にブラウズできる画面
// (スコープ解釈はファイル冒頭コメント参照)。LazyColumnがそのまま454件を仮想化するため
// 検索画面のようなsearchLimit方式のページングは不要。
@Composable
fun CatalogListScreen(openUrl: (String) -> Unit, onBack: () -> Unit) {
    val catalog = remember { CatalogLoader.shared.sortedWith(compareByDescending<CatalogVideo> { it.y }.thenBy { it.t }) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("再生リスト", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onBack, modifier = Modifier.testTag("catalogBackBtn")) { Text("◀ もどる") }
        Spacer(Modifier.height(8.dp))
        Text("${catalog.size}本の動画", style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(4.dp))
        LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("catalogList")) {
            items(catalog) { v -> VideoRow(v, openUrl) }
        }
    }
}
