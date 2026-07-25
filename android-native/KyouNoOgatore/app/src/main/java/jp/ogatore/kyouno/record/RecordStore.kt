package jp.ogatore.kyouno.record

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

// ネイティブ移植 Step 3(マスタープラン§2-3): Web版のlocalStorage(kyono_*プレフィックス・
// キーごとにJSON.stringify済み文字列を保持)を、内部ストレージの単一JSONファイル`kyono-store.json`に
// 1:1対応させたKVストア。buildExportString/importDataの契約(§2-3)をそのまま成立させるため、
// 内部表現も「フルキー("kyono_"つき)→生JSON文字列」のまま保持する(localStorageと同じ形)。
// 起動時に全読み込み・変更のたび全書き戻し(§2-3の方針。サイズが小さいため十分)。
@PublishedApi
internal val recordStoreJson = Json { ignoreUnknownKeys = true }

class RecordStore private constructor(
    private var raw: MutableMap<String, String>,
    private val file: File?,
) {
    companion object {
        // 実機用: ファイルに永続化する
        fun forFile(file: File): RecordStore {
            val store = RecordStore(mutableMapOf(), file)
            store.load()
            return store
        }

        // テスト用: メモリ上のみで完結させる(ファイルI/Oを一切しない)
        fun inMemory(initial: Map<String, String> = emptyMap()): RecordStore =
            RecordStore(initial.toMutableMap(), null)
    }

    private fun load() {
        val f = file ?: return
        if (!f.exists()) return
        runCatching {
            val decoded: Map<String, String> = recordStoreJson.decodeFromString(f.readText())
            raw = decoded.toMutableMap()
        }
    }

    private fun persist() {
        val f = file ?: return
        runCatching { f.writeText(recordStoreJson.encodeToString(raw)) }
    }

    // Web版 store.get(k,d) 相当: "kyono_"+kの値をJSON.parseして返す。欠落/型不一致時はdefaultValue
    // internal: publicなinline関数はprivateメンバ(raw/persist)へ触れられないため、モジュール内利用に絞る
    internal inline fun <reified T> get(key: String, default: T): T {
        val s = raw["kyono_$key"] ?: return default
        return runCatching { recordStoreJson.decodeFromString<T>(s) }.getOrDefault(default)
    }

    // Web版 store.set(k,v) 相当: "kyono_"+kにJSON.stringify(v)を保存。成功可否を返す(Web版のtry/catch踏襲)。
    internal inline fun <reified T> set(key: String, value: T): Boolean {
        return runCatching {
            raw["kyono_$key"] = recordStoreJson.encodeToString(value)
            persist()
            true
        }.getOrDefault(false)
    }

    // ---- インポート/エクスポート・未知キーpassthrough用の生アクセス ----

    val allRawKyonoEntries: Map<String, String>
        get() = raw.filterKeys { it.startsWith("kyono_") }

    fun rawValue(fullKey: String): String? = raw[fullKey]

    fun setRaw(fullKey: String, jsonString: String) {
        raw[fullKey] = jsonString
        persist()
    }

    fun removeRaw(fullKey: String) {
        raw.remove(fullKey)
        persist()
    }
}
