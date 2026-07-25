package jp.ogatore.kyouno.obu

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// obu-feed.json(scripts-native/gen-catalog.mjsがobu-feed.jsから生成。マスタープラン§2-1
// "obu-feed.js OBU_FEED"行・§6 Step 7b)のSerializableモデル。type: "text"|"photo"|"radio"。
@Serializable
data class ObuPost(
    val id: String,
    val date: String,
    val type: String,
    val text: String? = null,
    val image: String? = null,
    val audio: String? = null,
    val title: String? = null,
    val time: String? = null,
)

object ObuLoader {
    val shared: List<ObuPost> by lazy {
        val stream = ObuLoader::class.java.classLoader?.getResourceAsStream("obu-feed.json")
            ?: error("obu-feed.json がクラスパスに見つからない(app/src/main/resourcesの同梱漏れ)")
        val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        Json { ignoreUnknownKeys = true }.decodeFromString(text)
    }

    // index.html obuValidAssetPath()相当の簡易版+drawableリソース名への変換。
    // "assets/obu/post-2026-07-09-01.jpg" → "obu_post_2026_07_09_01"(拡張子なし・"-"を"_"に置換・obu_接頭辞)。
    fun imageResourceName(imagePath: String): String? {
        if (!Regex("^assets/obu/[A-Za-z0-9_.\\-/]+$").matches(imagePath)) return null
        val base = imagePath.substringAfterLast("/").substringBeforeLast(".")
        return "obu_" + base.replace(Regex("[^A-Za-z0-9]"), "_")
    }
}
