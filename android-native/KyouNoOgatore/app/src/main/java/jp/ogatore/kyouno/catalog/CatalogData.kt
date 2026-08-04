package jp.ogatore.kyouno.catalog

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// catalog.json(scripts-native/gen-catalog.mjsがvideos.jsから生成。マスタープラン§2-1
// "videos.js CATALOG"行・§6 Step 7a)のSerializableモデル。videos.js側のフィールド名(id/t/y/s/tags)を
// そのまま使う(CardData.ktと同じ方針。Web版定数テーブルとの1:1写像を優先)。
@Serializable
data class CatalogVideo(
    val id: String,
    val t: String,
    val y: Int,
    val s: String,
    val tags: List<String> = emptyList(),
    // TASK-C2-2026-08-04-build20-home-cards-and-tour-tiers.md H-2: ホームの「きょうの1本」に
    // 出うる動画にだけ付与するねらい型短タイトル。videos.js(Web版)は不可触のためcatalog.json
    // にのみ直接追記しており、全動画には無い(nullあり)。
    val st: String? = null,
)

object CatalogLoader {
    val shared: List<CatalogVideo> by lazy {
        val stream = CatalogLoader::class.java.classLoader?.getResourceAsStream("catalog.json")
            ?: error("catalog.json がクラスパスに見つからない(app/src/main/resourcesの同梱漏れ)")
        val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        Json { ignoreUnknownKeys = true }.decodeFromString(text)
    }
}
