package jp.ogatore.kyouno.record

// GO-G9(5視点ワンループ)・alan5差し戻しD2対応: 「よみこむ」実行前の状態を1件だけ退避し、
// 実行直後の一定時間だけ「さっきの状態にもどす」を出す機能の中核ロジック。UI(SettingsScreen.kt)
// から切り離した純粋関数にして、Composeの再生成(rememberSaveable)やユニットテストから
// 同じロジックを使い回せるようにする。
object RecordSnapshot {
    // captureした時点のキー→生JSON文字列の全件コピー(store.allRawKyonoEntriesはその場の
    // フィルタ済みコピーを返すため、以降のstore変更の影響を受けない)。
    fun capture(store: RecordStore): Map<String, String> = store.allRawKyonoEntries

    // captureしたsnapshotの状態へ全キーを戻す。snapshotに無いキー(=capture後にimportで
    // 追加されたキー)は削除し、snapshotにあるキーは値を上書きする(KyonoTransfer.importString
    // 自体の「一致しないkyono_*キーを削除」ロジックと同じ考え方)。
    fun restore(store: RecordStore, snapshot: Map<String, String>) {
        val current = store.allRawKyonoEntries
        for (k in current.keys) if (k !in snapshot) store.removeRaw(k)
        for ((k, v) in snapshot) store.setRaw(k, v)
    }
}
