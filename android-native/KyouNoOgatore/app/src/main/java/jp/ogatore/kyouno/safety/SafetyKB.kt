package jp.ogatore.kyouno.safety

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// soudan-kb.json（scripts-native/gen-safety-kb.mjsがsoudan-kb.jsから生成。マスタープラン§3-2）の
// redFlags/crisisセクションだけを読み込む最小Serializableモデル。Step 2時点ではintents(122件)は未使用のため
// デコード対象に含めない(kotlinx.serializationはignoreUnknownKeys=trueで宣言していないキーを無視する)。
@Serializable
data class SafetyKB(
    val redFlags: RedFlags,
    val crisis: Crisis,
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
