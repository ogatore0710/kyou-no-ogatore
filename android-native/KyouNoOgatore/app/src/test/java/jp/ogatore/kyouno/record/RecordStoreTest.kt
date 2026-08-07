package jp.ogatore.kyouno.record

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

// TASK build36 R-58(Fable監査A-1+設計裁定・2026-08-07): 記録全消失シナリオの再発防止テスト。
// 「破損しても唯一のコピーが保全される」ことの直接証明(corruptedFileIsQuarantined...)が本丸。
// RecordStore.ktはpure JVM(java.io+kotlinx.serializationのみ)なので素のJVMユニットテストで
// persist/loadを直接検証できる(この性質を守るためAtomicFileは採用していない・本体コメント参照)。
class RecordStoreTest {
    @get:Rule
    val tmpDir = TemporaryFolder()

    private fun storeFile(): File = File(tmpDir.root, "kyono-store.json")

    private fun corruptFiles(): List<File> =
        tmpDir.root.listFiles()!!.filter { it.name.startsWith("kyono-store.json.corrupt-") }

    // 1) 基本の往復: set→別インスタンスでload→一致
    @Test
    fun roundtripPersistsAcrossInstances() {
        val f = storeFile()
        val store = RecordStore.forFile(f)
        assertTrue(store.set("onboarded", true))
        assertTrue(store.set("anchor", "furo"))
        val reloaded = RecordStore.forFile(f)
        assertEquals(true, reloaded.get("onboarded", false))
        assertEquals("furo", reloaded.get("anchor", ""))
        // アトミック書き込みの後始末: .tmpが残っていない
        assertFalse(File(tmpDir.root, "kyono-store.json.tmp").exists())
    }

    // 2) 全消失シナリオの直接証明: 途中切れファイル→隔離され空で起動→次のsetでも隔離は無傷
    @Test
    fun corruptedFileIsQuarantinedAndNeverOverwritten() {
        val f = storeFile()
        val store = RecordStore.forFile(f)
        store.set("streak2", mapOf("total" to "5"))
        // 書き込み途中killの再現: バイト列を前半だけにtruncate
        val fullBytes = f.readBytes()
        f.writeBytes(fullBytes.copyOfRange(0, fullBytes.size / 2))
        val brokenBytes = f.readBytes()

        val reloaded = RecordStore.forFile(f)
        // (a) 空で起動(デフォルト値に落ちる)
        assertEquals(emptyMap<String, String>(), reloaded.get("streak2", emptyMap<String, String>()))
        // (b) 破損データは.corrupt-*へ原文のまま保全されている
        val quarantined = corruptFiles()
        assertEquals(1, quarantined.size)
        assertTrue(brokenBytes.contentEquals(quarantined[0].readBytes()))
        // (c) その後の操作は新しい本体に書かれ、隔離ファイルは無傷のまま
        assertTrue(reloaded.set("onboarded", true))
        assertEquals(true, RecordStore.forFile(f).get("onboarded", false))
        assertTrue(brokenBytes.contentEquals(quarantined[0].readBytes()))
    }

    // 3) サルベージ: 手編集で生bool/null/数値が混入した過去実績の事故がゼロ損失で復元される
    @Test
    fun salvageRecoversRawJsonValues() {
        val f = storeFile()
        // 二重エンコード規約違反のファイル(値が生のtrue/null/数値/オブジェクト)
        f.writeText("""{"kyono_onboarded": true, "kyono_anchor": "\"furo\"", "kyono_broken": null, "kyono_n": 3}""")
        val store = RecordStore.forFile(f)
        assertEquals(true, store.get("onboarded", false))
        assertEquals("furo", store.get("anchor", ""))
        assertEquals(3, store.get("n", 0))
        // 隔離はされない(復元できたので)・原本は.bakに残る
        assertTrue(corruptFiles().isEmpty())
        assertTrue(File(tmpDir.root, "kyono-store.json.bak").exists())
        // 次のpersistで正規形(二重エンコード)に直っている
        store.set("theme", "dark")
        val reloaded = RecordStore.forFile(f)
        assertEquals(true, reloaded.get("onboarded", false))
        assertEquals("dark", reloaded.get("theme", ""))
    }

    // 4a) 正常本体+ゴミ.tmp → 本体採用・.tmp削除
    @Test
    fun garbageTmpIsDeletedWhenMainIsHealthy() {
        val f = storeFile()
        RecordStore.forFile(f).set("onboarded", true)
        val tmp = File(tmpDir.root, "kyono-store.json.tmp")
        tmp.writeText("garbage{{{")
        val store = RecordStore.forFile(f)
        assertEquals(true, store.get("onboarded", false))
        assertFalse(tmp.exists())
    }

    // 4b) 本体欠落+完全な.tmp → 昇格採用(rename失敗の隙間ケース救済)
    @Test
    fun completeTmpIsPromotedWhenMainIsMissing() {
        val f = storeFile()
        RecordStore.forFile(f).set("anchor", "neru")
        val tmp = File(tmpDir.root, "kyono-store.json.tmp")
        f.copyTo(tmp)
        assertTrue(f.delete())
        val store = RecordStore.forFile(f)
        assertEquals("neru", store.get("anchor", ""))
        assertTrue(f.exists())
        assertFalse(tmp.exists())
    }

    // 5) persist失敗時の無害性: 保存先が消えていてもクラッシュしない
    @Test
    fun persistFailureDoesNotThrow() {
        val f = storeFile()
        val store = RecordStore.forFile(f)
        store.set("onboarded", true)
        tmpDir.root.deleteRecursively()
        // ディレクトリ消失下でもrunCatchingで握られ、falseかtrueかに関わらず例外は出ない
        store.set("anchor", "asa")
    }

    // 6) 隔離ファイルの上限: 新しい順に2個までへトリムされる
    @Test
    fun quarantineFilesAreTrimmedToNewestTwo() {
        val f = storeFile()
        File(tmpDir.root, "kyono-store.json.corrupt-100").writeText("a")
        File(tmpDir.root, "kyono-store.json.corrupt-200").writeText("b")
        File(tmpDir.root, "kyono-store.json.corrupt-300").writeText("c")
        RecordStore.forFile(f)
        val names = corruptFiles().map { it.name }.sorted()
        assertEquals(listOf("kyono-store.json.corrupt-200", "kyono-store.json.corrupt-300"), names)
    }
}
