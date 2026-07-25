import Foundation

// ネイティブ移植 Step 3(マスタープラン§2-3): index.html:2054 buildExportString / index.html:2082
// importData の1:1移植(UIのconfirm/alert/location.reloadを除いたデータ変換部分のみ)。
// これがPWA版のエクスポート文字列をそのままネイティブへの引っ越しインポート経路にする土台(§2-3)。
public enum KyonoTransferError: Error { case invalidFormat }

public enum KyonoTransfer {
    private static let prefix = "KYONO1:"
    private static let maxImportLength = 300_000
    private static let maxKeys = 50
    private static let maxValueLength = 200_000

    // "KYONO1:" + Base64(UTF8 bytes of JSON{v:1,data:{kyono_*: 生JSON文字列}})。
    // JS版はbtoaがLatin1専用のため unescape(encodeURIComponent(...)) でUTF8バイト列に変換してから
    // btoaしている。Swiftはbase64EncodedString()がバイト列を直接扱えるので、そのトリックは不要
    // (結果として得られるbase64は同じ「UTF8バイト列のbase64」なのでJS版と相互にデコード可能)。
    public static func buildExportString(_ store: RecordStore) -> String {
        let data = store.allRawKyonoEntries
        let envelope: [String: Any] = ["v": 1, "data": data]
        let jsonData = try! JSONSerialization.data(withJSONObject: envelope, options: [.sortedKeys])
        return prefix + jsonData.base64EncodedString()
    }

    // 検証(プレフィックス→Base64→JSON→中身)が全部通ってから書き込む。壊れた文字列では何も変更しない
    // (index.html:2081のコメントと同じ方針)。呼び出し側のconfirm確認・「よみこみました」表示・
    // location.reload相当はUI層(Step 5c以降)の責務でありここには含めない。
    public static func importString(_ str: String, into store: RecordStore) throws {
        guard str.hasPrefix(prefix), str.count <= maxImportLength else {
            throw KyonoTransferError.invalidFormat
        }
        let b64 = String(str.dropFirst(prefix.count))
        guard let decoded = Data(base64Encoded: b64),
              let obj = try? JSONSerialization.jsonObject(with: decoded)
        else { throw KyonoTransferError.invalidFormat }

        let src: [String: Any]
        if let envelope = obj as? [String: Any],
           let v = envelope["v"] as? NSNumber, v.intValue >= 1,
           let data = envelope["data"] as? [String: Any] {
            src = data
        } else if let flat = obj as? [String: Any] {
            src = flat // v無し(旧形式): トップレベルそのものがdata。index.html:2090と同じ両対応
        } else {
            throw KyonoTransferError.invalidFormat
        }

        var accepted: [String: String] = [:]
        for (k, v) in src {
            if accepted.count >= maxKeys { break } // アプリが使うキーは約20個・50は十分な上限
            guard k.hasPrefix("kyono_") else { continue }
            guard let s = v as? String, s.count <= maxValueLength else { continue }
            accepted[k] = s
        }
        guard !accepted.isEmpty else { throw KyonoTransferError.invalidFormat }

        for (k, v) in accepted { store.setRaw(k, v) }

        // 取り込んだキー＋端末の表示設定 以外の既存kyono_キーを掃除(index.html:2100-2104)。
        // 書込みを先に済ませてから掃除するので、途中で失敗しても既存記録は消えない。
        var keep = Set(accepted.keys)
        keep.insert("kyono_theme")
        keep.insert("kyono_bigtext")
        for k in store.allRawKyonoEntries.keys where !keep.contains(k) {
            store.removeRaw(k)
        }
    }
}
