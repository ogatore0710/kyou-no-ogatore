package jp.ogatore.kyouno.record

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// GO-G9(5視点ワンループ)・alan5差し戻し対応: 「よみこむ」実行前の自動退避+取り消しの
// 中核ロジック(RecordSnapshot.capture/restore)のユニットテスト。UI項目(他のGO15件)は
// alan5指示により対象外・このロジックだけ追加する。
class RecordSnapshotTest {
    @Test
    fun restoreBringsBackOverwrittenValue() {
        val store = RecordStore.inMemory()
        store.set("theme", "light")
        store.set("bigtext", true)

        val snapshot = RecordSnapshot.capture(store)

        // よみこむ相当: 別の値で上書き
        store.set("theme", "dark")
        assertEquals("\"dark\"", store.rawValue("kyono_theme"))

        RecordSnapshot.restore(store, snapshot)
        assertEquals("\"light\"", store.rawValue("kyono_theme"))
        assertEquals("true", store.rawValue("kyono_bigtext"))
    }

    @Test
    fun restoreRemovesKeysAddedAfterCapture() {
        val store = RecordStore.inMemory()
        store.set("theme", "auto")
        val snapshot = RecordSnapshot.capture(store)

        // よみこむ相当: capture後に新しいキーが増える(インポートしたデータにしか無いキー)
        store.setRaw("kyono_streak2", """{"count":5,"total":5,"dates":[]}""")
        assertEquals("""{"count":5,"total":5,"dates":[]}""", store.rawValue("kyono_streak2"))

        RecordSnapshot.restore(store, snapshot)
        assertNull("capture後に増えたキーはrestoreで消えるべき", store.rawValue("kyono_streak2"))
        assertEquals(snapshot.keys, store.allRawKyonoEntries.keys)
    }

    @Test
    fun restoreAfterKyonoTransferImportRoundTrips() {
        // 実際の「よみこむ」経路(KyonoTransfer.importString)で別デバイスのデータを取り込んだ後
        // でも、restoreでcapture時点のキー集合・値の両方に戻せることを確認する。
        val store = RecordStore.inMemory()
        store.set("theme", "light")
        store.set("bigtext", false)
        store.setRaw("kyono_streak2", """{"count":3,"total":3,"dates":["2026-07-01"]}""")
        val snapshot = RecordSnapshot.capture(store)

        // 別デバイス(別のstreak2値)からの「よみこむ」を模擬する
        val otherDevice = RecordStore.inMemory()
        otherDevice.set("theme", "dark")
        otherDevice.setRaw("kyono_streak2", """{"count":0,"total":0,"dates":[]}""")
        val incoming = KyonoTransfer.buildExportString(otherDevice)
        KyonoTransfer.importString(incoming, store)
        assertEquals("""{"count":0,"total":0,"dates":[]}""", store.rawValue("kyono_streak2"))

        RecordSnapshot.restore(store, snapshot)
        assertEquals(snapshot, store.allRawKyonoEntries)
    }
}
