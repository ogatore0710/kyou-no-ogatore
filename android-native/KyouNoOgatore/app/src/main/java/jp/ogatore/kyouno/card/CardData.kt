package jp.ogatore.kyouno.card

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// card-data.json(scripts-native/gen-card-data.mjsがindex.html/app-card.jsから生成。マスタープラン§2-4)の
// Serializableモデル。フィールド名はJS側の定数名(SCREAMING_SNAKE_CASE)を意図的にそのまま使う——
// このデータは「人が読み書きするコード」ではなく「Web版の定数テーブルの1:1写像」であり、9月の
// 差分同期(§5-2)でJS側の変更を機械的に追いやすくすることを、Kotlinの命名規約より優先している。

@Serializable
data class CardTheme(val name: String, val bg: List<String>, val main: String, val deco: List<String>)

@Serializable
data class MilestoneInfo(val d: Int, val t: String)

@Serializable
data class CharaFile(val file: String, val w: Int)

@Serializable
data class SeasonCard(val key: String, val name: String, val bg: List<String>, val ws: Int? = null, val we: Int? = null)

@Serializable
data class RareCard(val key: String, val name: String, val bg: List<String>)

@Serializable
data class NormalCard(val name: String, val bg: List<String>, val main: String, val deco: List<String>)

@Serializable
data class TokuCard(val key: String, val name: String, val bg: List<String>)

@Serializable
data class CardData(
    val CARD_THEMES: List<CardTheme>,
    val GOLD: CardTheme,
    val MS: List<MilestoneInfo>,
    val CHARA_FILES: List<CharaFile>,
    val CARD_THEMES_V1_COUNT: Int,
    val CARD_THEMES_V2_FROM: Int,
    val MILESTONES: List<Int>,
    val CARD_IMG_FROM: Int,
    val SEASON_CARDS: List<SeasonCard>,
    val RARE_CARDS: List<RareCard>,
    val NORMAL_CARDS: List<NormalCard>,
    val TOKU_CARDS: Map<String, TokuCard>, // キーはeffTotalの文字列("4".."1900")
    val CARD_ROT_ORDER: List<Int>,
    // Step 7a(図鑑UI): index.html getDexStatus()が参照するヒント/フレーバー文言。
    val DEX_TEASE: Map<String, String> = emptyMap(), // rareのkey→ロック中ヒント
    val DEX_FLAVOR: Map<String, String> = emptyMap(), // toku/season/rareのkey→アンロック後フレーバー
    val DEX_FLAVOR_NORMAL: Map<String, String> = emptyMap(), // normalのname→アンロック後フレーバー
    val DEX_NORMAL_TEASE: String = "",
)

object CardDataLoader {
    val shared: CardData by lazy {
        val stream = CardDataLoader::class.java.classLoader?.getResourceAsStream("card-data.json")
            ?: error("card-data.json がクラスパスに見つからない(app/src/main/resourcesの同梱漏れ)")
        val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        Json { ignoreUnknownKeys = true }.decodeFromString(CardData.serializer(), text)
    }
}
