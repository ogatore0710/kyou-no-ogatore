package jp.ogatore.kyouno.record

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Base64

// ネイティブ移植 Step 3(マスタープラン§6 Step3検収基準1・2): Step 0で採取したexport-fixture.json
// (PWA版buildExportStringの実出力)をインポートし、export-fixture-expected.jsonの期待値と機械照合する。

@Serializable
private data class ExportFixture(val exportString: String)

@Serializable
private data class ExportExpected(
    val streak2_count: Int,
    val streak2_total: Int,
    val daylog_keyCount: Int,
    val keys: List<String>,
    val keyCount: Int,
    val passThroughOnlyKeys: List<String>,
)

class KyonoTransferTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun loadFixture(): ExportFixture {
        val stream = javaClass.classLoader!!.getResourceAsStream("export-fixture.json")!!
        return json.decodeFromString(stream.bufferedReader(Charsets.UTF_8).readText())
    }

    private fun loadExpected(): ExportExpected {
        val stream = javaClass.classLoader!!.getResourceAsStream("export-fixture-expected.json")!!
        return json.decodeFromString(stream.bufferedReader(Charsets.UTF_8).readText())
    }

    // 検収基準1: streak2のcount/total・daylog件数・キー集合が期待値JSONと一致
    @Test
    fun importExportFixtureMatchesExpectedValues() {
        val fixture = loadFixture()
        val expected = loadExpected()
        val store = RecordStore.inMemory()
        KyonoTransfer.importString(fixture.exportString, store)

        val st = RecordLogic.loadStreak(store)
        assertEquals(expected.streak2_count, st.count)
        assertEquals(expected.streak2_total, st.total)

        val dl = RecordLogic.loadDaylog(store)
        assertEquals(expected.daylog_keyCount, dl.size)

        val keys = store.allRawKyonoEntries.keys
        assertEquals(expected.keys.toSet(), keys)
        assertEquals(expected.keyCount, keys.size)

        // 未知キー(a2hs2/homehint_next)もネイティブが使わない値としてパススルー保全されている
        for (k in expected.passThroughOnlyKeys) {
            assertNotNull("パススルーキー$k が保全されていない", store.rawValue(k))
        }
    }

    // 検収基準2: インポート→エクスポートの往復でキー集合が減らない(a2hs2等の未使用キー含む)
    @Test
    fun importExportRoundTripDoesNotShrinkKeySet() {
        val fixture = loadFixture()
        val store = RecordStore.inMemory()
        KyonoTransfer.importString(fixture.exportString, store)
        val keysAfterImport = store.allRawKyonoEntries.keys.toSet()

        val reExported = KyonoTransfer.buildExportString(store)
        val store2 = RecordStore.inMemory()
        KyonoTransfer.importString(reExported, store2)
        val keysAfterRoundTrip = store2.allRawKyonoEntries.keys.toSet()

        assertEquals("往復でキー集合が変化した", keysAfterImport, keysAfterRoundTrip)
    }

    @Test
    fun rejectsInvalidPrefix() {
        val store = RecordStore.inMemory()
        assertThrows(KyonoTransferException::class.java) {
            KyonoTransfer.importString("NOTKYONO:xxxx", store)
        }
    }

    @Test
    fun rejectsMissingKyonoPrefixKeysOnly() {
        // "kyono_"始まりでないキーだけのペイロードは無効(index.html:2097 if(!cnt) throw 0 と同じ)
        val payload = """{"v":1,"data":{"other_key":"1"}}"""
        val b64 = Base64.getEncoder().encodeToString(payload.toByteArray(Charsets.UTF_8))
        val store = RecordStore.inMemory()
        assertThrows(KyonoTransferException::class.java) {
            KyonoTransfer.importString("KYONO1:$b64", store)
        }
    }
}
