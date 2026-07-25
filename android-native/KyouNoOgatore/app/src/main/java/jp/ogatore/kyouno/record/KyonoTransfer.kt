package jp.ogatore.kyouno.record

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.util.Base64

// ネイティブ移植 Step 3(マスタープラン§2-3): index.html:2054 buildExportString / index.html:2082
// importData の1:1移植(UIのconfirm/alert/location.reloadを除いたデータ変換部分のみ)。
// これがPWA版のエクスポート文字列をそのままネイティブへの引っ越しインポート経路にする土台(§2-3)。
class KyonoTransferException(message: String) : Exception(message)

object KyonoTransfer {
    private const val PREFIX = "KYONO1:"
    private const val MAX_IMPORT_LENGTH = 300_000
    private const val MAX_KEYS = 50
    private const val MAX_VALUE_LENGTH = 200_000

    // "KYONO1:" + Base64(UTF8 bytes of JSON{v:1,data:{kyono_*: 生JSON文字列}})。
    // JS版はbtoaがLatin1専用のため unescape(encodeURIComponent(...)) でUTF8バイト列に変換してから
    // btoaしている。java.util.Base64はバイト列を直接扱えるので、そのトリックは不要
    // (結果として得られるbase64は同じ「UTF8バイト列のbase64」なのでJS版と相互にデコード可能)。
    fun buildExportString(store: RecordStore): String {
        val data = store.allRawKyonoEntries
        val envelope = buildJsonObject {
            put("v", 1)
            putJsonObject("data") { for ((k, v) in data) put(k, v) }
        }
        return PREFIX + Base64.getEncoder().encodeToString(envelope.toString().toByteArray(Charsets.UTF_8))
    }

    // 検証(プレフィックス→Base64→JSON→中身)が全部通ってから書き込む。壊れた文字列では何も変更しない
    // (index.html:2081のコメントと同じ方針)。呼び出し側のconfirm確認・「よみこみました」表示・
    // 再読込相当はUI層(Step 5c以降)の責務でありここには含めない。
    fun importString(str: String, store: RecordStore) {
        if (!str.startsWith(PREFIX) || str.length > MAX_IMPORT_LENGTH) {
            throw KyonoTransferException("invalid prefix or too long")
        }
        val b64 = str.substring(PREFIX.length)
        val decoded = runCatching { Base64.getDecoder().decode(b64) }.getOrNull()
            ?: throw KyonoTransferException("invalid base64")
        val text = String(decoded, Charsets.UTF_8)
        val elem = runCatching { Json.parseToJsonElement(text) }.getOrNull()
            ?: throw KyonoTransferException("invalid json")
        val obj = elem as? JsonObject ?: throw KyonoTransferException("invalid json")

        val v = (obj["v"] as? JsonPrimitive)?.intOrNull
        val dataObj = obj["data"] as? JsonObject
        val src: JsonObject = if (v != null && v >= 1 && dataObj != null) dataObj else obj // v無し(旧形式)両対応

        val accepted = mutableMapOf<String, String>()
        for ((k, v2) in src) {
            if (accepted.size >= MAX_KEYS) break // アプリが使うキーは約20個・50は十分な上限
            if (!k.startsWith("kyono_")) continue
            val prim = v2 as? JsonPrimitive
            if (prim == null || !prim.isString) continue
            if (prim.content.length > MAX_VALUE_LENGTH) continue
            accepted[k] = prim.content
        }
        if (accepted.isEmpty()) throw KyonoTransferException("no valid kyono_ keys")

        for ((k, v2) in accepted) store.setRaw(k, v2)

        // 取り込んだキー＋端末の表示設定 以外の既存kyono_キーを掃除(index.html:2100-2104)。
        // 書込みを先に済ませてから掃除するので、途中で失敗しても既存記録は消えない。
        val keep = accepted.keys.toMutableSet()
        keep.add("kyono_theme")
        keep.add("kyono_bigtext")
        for (k in store.allRawKyonoEntries.keys.toList()) if (k !in keep) store.removeRaw(k)
    }
}
