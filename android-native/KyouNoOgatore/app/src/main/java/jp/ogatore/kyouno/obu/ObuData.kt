package jp.ogatore.kyouno.obu

import jp.ogatore.kyouno.record.RecordLogic
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

    // index.html obuValidAssetPath()相当の簡易版+drawable/rawリソース名への変換。
    // "assets/obu/post-2026-07-09-01.jpg" → "obu_post_2026_07_09_01"(拡張子なし・"-"を"_"に置換・obu_接頭辞)。
    private fun assetResourceName(assetPath: String): String? {
        if (!Regex("^assets/obu/[A-Za-z0-9_.\\-/]+$").matches(assetPath)) return null
        val base = assetPath.substringAfterLast("/").substringBeforeLast(".")
        return "obu_" + base.replace(Regex("[^A-Za-z0-9]"), "_")
    }
    fun imageResourceName(imagePath: String): String? = assetResourceName(imagePath)
    // TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §1: audio(type=radioのmp3パス)を
    // res/rawのリソース名に変換する。画像と同じ命名規則(obu_接頭辞)。
    fun audioResourceName(audioPath: String): String? = assetResourceName(audioPath)
}

// TASK-C2-2026-07-27-obu-fab-preview-popup.md: index.html:1275-1289
// obuIsLaterOrEqual/obuLatest/obuLatestByType の1:1移植。
fun obuIsLaterOrEqual(a: ObuPost, b: ObuPost): Boolean {
    if (a.date != b.date) return a.date >= b.date
    if (a.time != null && b.time != null) return a.time >= b.time
    return true
}

fun obuLatest(posts: List<ObuPost>): ObuPost? {
    if (posts.isEmpty()) return null
    var best = posts[0]
    for (i in 1 until posts.size) if (obuIsLaterOrEqual(posts[i], best)) best = posts[i]
    return best
}

fun obuLatestByType(posts: List<ObuPost>, type: String): ObuPost? {
    var best: ObuPost? = null
    for (item in posts) if (item.type == type && (best == null || obuIsLaterOrEqual(item, best))) best = item
    return best
}

// index.html:1298-1306 OBU_STALE_DAYS/obuIsStaleDate/obuHasNew の1:1移植。todayは呼び出し元
// (UI層)がInstant.now()から求めて渡す(§2-4: 純粋ロジック層に現在時刻を直接埋め込まない)。
const val OBU_STALE_DAYS = 30
fun obuIsStaleDate(date: String, today: String): Boolean = RecordLogic.daysBetween(date, today) >= OBU_STALE_DAYS

fun obuHasNew(posts: List<ObuPost>, seenId: String?, today: String): Boolean {
    val latest = obuLatest(posts) ?: return false
    if (obuIsStaleDate(latest.date, today)) return false
    return seenId != latest.id
}

// TASK-C2-2026-07-28-obu-voices-diary-and-navigation.md §5: index.html:1314-1318 obuFmtDate()の
// 1:1移植。"2026-07-09"→"7月9日"(ゼロ埋めなし)。timeがあれば"7月9日 12:30ごろ"。
fun obuFmtDate(date: String, time: String?): String {
    val m = date.substring(5, 7).toInt()
    val d = date.substring(8, 10).toInt()
    val base = "${m}月${d}日"
    return if (time != null) "$base ${time}ごろ" else base
}
