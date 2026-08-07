package jp.ogatore.kyouno.record

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import java.io.FileOutputStream

// ネイティブ移植 Step 3(マスタープラン§2-3): Web版のlocalStorage(kyono_*プレフィックス・
// キーごとにJSON.stringify済み文字列を保持)を、内部ストレージの単一JSONファイル`kyono-store.json`に
// 1:1対応させたKVストア。buildExportString/importDataの契約(§2-3)をそのまま成立させるため、
// 内部表現も「フルキー("kyono_"つき)→生JSON文字列」のまま保持する(localStorageと同じ形)。
// 起動時に全読み込み・変更のたび全書き戻し(§2-3の方針。サイズが小さいため十分)。
//
// TASK build36 R-58(Fable監査A-1+設計裁定・2026-08-07): 記録全消失シナリオの恒久対策。
// 旧実装は素のwriteText(非アトミック)で、書き込み途中の強制終了/電源断でファイルが途中切れに
// なると、次回load()のdecode失敗をrunCatchingが握りつぶして空で起動し、ユーザーの次の1操作の
// persist()が空同然のマップで全上書き=streak/図鑑/メモの全損に至った。対策の柱は3つ:
//  1) persist()を「一時ファイル+fsync+rename」のアトミック書き込みへ(手書き採用。AtomicFileは
//     android.*依存が入り、素のJVMユニットテストができなくなるため不採用)
//  2) load()のdecode失敗時は寛容ロード(サルベージ)→それも無理なら破損ファイルを
//     .corrupt-<epoch秒>へ隔離して空で継続(唯一のコピーを絶対に上書きで壊さない)。
//     隔離renameすら失敗した場合のみpersistを封印する
//  3) 手編集での生bool/null混入(過去実績あり・MEMORY記載)はサルベージがゼロ損失で自己修復する
//     (二重JSONエンコード規約上、生trueの意図は文字列"true"なので、JSONテキスト表現を
//     そのまま文字列として採用すればよい)
// 前提: 書き込みはMainActivityの長寿命インスタンスのみ(DailyNotifications/KyonoWidgetは
// 読み取り専用の規約=GO-H1§1)。この単一ライター前提を崩すとrenameの後勝ち上書きで
// 更新消失が起きうるため、崩さないこと。
@PublishedApi
internal val recordStoreJson = Json { ignoreUnknownKeys = true }

class RecordStore private constructor(
    private var raw: MutableMap<String, String>,
    private val file: File?,
) {
    companion object {
        // R-58: 隔離ファイルは新しい順に2個まで残す(証拠保全と肥大化防止のバランス)。
        private const val QUARANTINE_KEEP = 2

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

    // R-58: 破損ファイルの隔離(退避rename)にすら失敗したときだけtrue。唯一のコピーを
    // 上書きから守るため、そのセッション中のpersistを封じる(次回起動で再試行される)。
    private var persistDisabled = false

    private fun tmpFile(f: File) = File(f.parentFile, f.name + ".tmp")

    private fun load() {
        val f = file ?: return
        val tmp = tmpFile(f)
        if (f.exists()) {
            val text = runCatching { f.readText() }.getOrNull()
            if (text == null) {
                // 読み取り自体の失敗(権限/一時的I/O障害)。実態不明のファイルを上書きしない。
                persistDisabled = true
                return
            }
            val decoded = runCatching { recordStoreJson.decodeFromString<Map<String, String>>(text) }.getOrNull()
            if (decoded != null) {
                raw = decoded.toMutableMap()
                tmp.delete() // renameまで到達しなかった書きかけ残骸
                trimQuarantine(f)
                return
            }
            val salvaged = salvage(text)
            if (salvaged != null) {
                // 誤修復に備えて原本バイトを.bakに残してから採用(次のpersistで正規形に直る)。
                runCatching { File(f.parentFile, f.name + ".bak").writeText(text) }
                raw = salvaged
                tmp.delete()
                trimQuarantine(f)
                return
            }
            // 完全破損: 証拠を保全して空で継続。全損の本質は「破損データの唯一のコピーを
            // 次のpersistが上書きすること」であり、隔離が成功すれば上書きしても失うものは無い。
            val quarantine = File(f.parentFile, f.name + ".corrupt-" + (System.currentTimeMillis() / 1000))
            if (!f.renameTo(quarantine)) {
                persistDisabled = true
            }
        }
        // 本体が無い(初回起動 or 隔離直後): persist()のフォールバック1(delete成功→rename失敗)の
        // 隙間で落ちたケースに備え、完全にdecodeできる.tmpがあれば昇格して採用する。
        if (!persistDisabled && tmp.exists()) {
            val tmpDecoded = runCatching { recordStoreJson.decodeFromString<Map<String, String>>(tmp.readText()) }.getOrNull()
            if (tmpDecoded != null && tmp.renameTo(f)) {
                raw = tmpDecoded.toMutableMap()
            } else {
                tmp.delete()
            }
        }
        trimQuarantine(f)
    }

    // R-58: 寛容ロード。トップレベルがJSONオブジェクトなら、値が文字列以外(生bool/null/数値/
    // オブジェクト等)でも「そのJSONテキスト表現を文字列として」採用する。二重JSONエンコード
    // 規約上これが元の意図と一致するため、過去実績の手編集事故はゼロ損失で復元される。
    private fun salvage(text: String): MutableMap<String, String>? = runCatching {
        val obj = recordStoreJson.parseToJsonElement(text)
        if (obj !is JsonObject) return@runCatching null
        val out = mutableMapOf<String, String>()
        for ((k, v) in obj) {
            out[k] = if (v is JsonPrimitive && v.isString) v.content else v.toString()
        }
        out
    }.getOrNull()

    private fun trimQuarantine(f: File) {
        runCatching {
            val siblings = f.parentFile?.listFiles() ?: return
            siblings.filter { it.name.startsWith(f.name + ".corrupt-") }
                .sortedByDescending { it.name }
                .drop(QUARANTINE_KEEP)
                .forEach { it.delete() }
        }
    }

    private fun persist() {
        val f = file ?: return
        if (persistDisabled) return
        runCatching {
            // 同一ディレクトリの一時ファイルに書く(cacheDir等の別マウントだとrenameが
            // EXDEVコピーになり非アトミック化するため、構造的に同一ディレクトリを強制)。
            val tmp = tmpFile(f)
            FileOutputStream(tmp).use { out ->
                out.write(recordStoreJson.encodeToString(raw).toByteArray(Charsets.UTF_8))
                // fsync必須: これが無いとext4のrename順序次第で電源断時に0バイトになりうる。
                out.fd.sync()
            }
            if (!tmp.renameTo(f)) {
                // フォールバック1: 上書きrename非対応FSに備え、先に削除してからrename。
                f.delete()
                if (!tmp.renameTo(f)) {
                    // フォールバック2(最終): 旧来の直接書き込み。最悪でも「現状と同じ」に
                    // 落ちるだけで、この変更によって現状より悪化する経路は存在しない。
                    f.writeText(recordStoreJson.encodeToString(raw))
                    tmp.delete()
                }
            }
        }
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
