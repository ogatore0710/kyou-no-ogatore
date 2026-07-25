package jp.ogatore.kyouno.safety

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// soudan-kb.json（scripts-native/gen-safety-kb.mjsがsoudan-kb.jsから生成。マスタープラン§3-2）の
// redFlags/crisisセクションを読み込むSerializableモデル。Step 6(相談室UI通常応答)からintents(122件)/
// commonFollowups(4件)も追加デコードする(smalltalk(54件)はStep6の作業範囲外のため対象外のまま)。
// これらは判定(crisis/redFlag)とは無関係の通常会話コンテンツであり、SafetyGateの4関数(judgment)には
// 一切関与しない(マスタープラン§3-2の隔離対象はnorm/crisisHit/redFlagHit/redFlagKindの4関数のみ)。
@Serializable
data class SafetyKB(
    val redFlags: RedFlags,
    val crisis: Crisis,
    val intents: List<Intent> = emptyList(),
    val commonFollowups: List<CommonFollowup> = emptyList(),
) {
    @Serializable
    data class RedFlags(
        val kw: List<String>,
        val stateKw: List<String>? = null,
        val empathy: String,
        val answer: String,
        val answerState: String,
    )

    @Serializable
    data class Crisis(
        val kw: List<String>,
        val answer: String,
    )

    // soudan-kb.json intents[] の1要素。index.html:3274 sdAnswerIntent等が参照するフィールドのみ。
    @Serializable
    data class Intent(
        val id: String,
        val chip: String,
        val kw: List<String> = emptyList(),
        val empathy: String = "",
        val mitate: String = "",
        val videos: List<Video> = emptyList(),
        val keizoku: String = "",
        val followups: List<String> = emptyList(),
        val safety: Boolean = false,
    )

    @Serializable
    data class Video(
        val v: String,
        val note: String = "",
    )

    // soudan-kb.json commonFollowups[] の1要素。mode: "text"|"shorter"|"more"(index.html:3335 sdAnswerFollowup)。
    @Serializable
    data class CommonFollowup(
        val id: String,
        val chip: String,
        val mode: String = "text",
        val answer: String? = null,
    )
}

object SafetyKBLoader {
    // app/src/main/resources/soudan-kb.json をクラスパス経由で読み込む(プレーンJVM単体テストから見えるように、
    // AndroidのAssetManager/resではなくJava標準のリソースフォルダに配置している)。
    // 失敗時はビルド構成の異常(リソース同梱漏れ)なので、判定ロジック内でのエラーハンドリング濫用とは別物として許容する。
    val shared: SafetyKB by lazy {
        val stream = SafetyKBLoader::class.java.classLoader?.getResourceAsStream("soudan-kb.json")
            ?: error("soudan-kb.json がクラスパスに見つからない(app/src/main/resourcesの同梱漏れ)")
        val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        Json { ignoreUnknownKeys = true }.decodeFromString(SafetyKB.serializer(), text)
    }
}
