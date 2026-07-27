package jp.ogatore.kyouno.catalog

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// playlists.json(scripts-native/gen-playlists.mjsがindex.html:3498-3521 PLAYLISTSから生成。
// TASK-C2-2026-07-28-search-playlists-and-fullwidth-space.md §1)のSerializableモデル。
// 手動キュレーションのYouTubeプレイリスト16本(3グループ)。CatalogData.ktと同じ方針。
@Serializable
data class PlaylistItem(
    val id: String,
    val title: String,
    val desc: String,
    val thumb: String? = null,
)

@Serializable
data class PlaylistGroup(
    val group: String,
    val items: List<PlaylistItem>,
)

object PlaylistLoader {
    val shared: List<PlaylistGroup> by lazy {
        val stream = PlaylistLoader::class.java.classLoader?.getResourceAsStream("playlists.json")
            ?: error("playlists.json がクラスパスに見つからない(app/src/main/resourcesの同梱漏れ)")
        val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        Json { ignoreUnknownKeys = true }.decodeFromString(text)
    }
}
